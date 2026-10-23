package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ArgEdge;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;
import hu.bme.mit.theta.xta.learning.compositional.common.ZoneSemantics;

import java.util.*;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public class XtaInclusionOracle implements EquivalenceOracle.DFAEquivalenceOracle<String> {
    private static final int ARG_LOG_INTERVAL = 500;

    private final ZoneSemantics semantics;
    private final TimedStepper stepper;
    private final MembershipOracle.DFAMembershipOracle<String> mqOracle;
    private final SearchStrategy searchStrategy;
    private final Logger logger;

    private XtaInclusionOracle(ZoneSemantics semantics,
                               TimedStepper stepper,
                               MembershipOracle.DFAMembershipOracle<String> mqOracle,
                               SearchStrategy searchStrategy,
                               Logger logger) {
        this.semantics = checkNotNull(semantics);
        this.stepper = checkNotNull(stepper);
        this.mqOracle = checkNotNull(mqOracle);
        this.searchStrategy = checkNotNull(searchStrategy);
        this.logger = checkNotNull(logger);
    }

    public static XtaInclusionOracle create(ZoneSemantics semantics,
                                            TimedStepper stepper,
                                            MembershipOracle.DFAMembershipOracle<String> mqOracle) {
        return new XtaInclusionOracle(semantics, stepper, mqOracle, SearchStrategy.BFS,
                NullLogger.getInstance());
    }

    public static XtaInclusionOracle create(ZoneSemantics semantics,
                                            TimedStepper stepper,
                                            MembershipOracle.DFAMembershipOracle<String> mqOracle,
                                            SearchStrategy searchStrategy,
                                            Logger logger) {
        return new XtaInclusionOracle(semantics, stepper, mqOracle, searchStrategy, logger);
    }

    @Override
    public DefaultQuery<String, Boolean> findCounterExample(DFA<?, String> hypothesis, Collection<? extends String> inputs) {
        return doFindCounterExample(hypothesis, inputs);
    }

    private <S> DefaultQuery<String, Boolean> doFindCounterExample(DFA<S, String> hypothesis, Collection<? extends String> inputs) {
        InitFunc<ZoneDfaState<S>, ZonePrec> initFunc =
                ZoneDfaInitFunc.create(hypothesis, stepper);
        TransFunc<ZoneDfaState<S>, LearnLibAction<String>, ZonePrec> transFunc =
                ZoneDfaTransFunc.create(hypothesis, stepper);
        ZoneDfaOrd<S> ord = ZoneDfaOrd.create();
        ZoneDfaLts<S> lts = ZoneDfaLts.create(hypothesis, inputs);

        Analysis<ZoneDfaState<S>, LearnLibAction<String>, ZonePrec> analysis = ZoneDfaAnalysis.create(initFunc, transFunc, ord);
        Predicate<ZoneDfaState<S>> targetPred = state -> !hypothesis.isAccepting(state.getDfaState());
        ArgBuilder<ZoneDfaState<S>, LearnLibAction<String>, ZonePrec> argBuilder = ArgBuilder.create(lts, analysis, targetPred);
        ZonePrec prec = ZonePrec.of(semantics.ceilings().keySet());

        ARG<ZoneDfaState<S>, LearnLibAction<String>> arg = argBuilder.createArg();
        argBuilder.init(arg, prec);

        Waitlist<ArgNode<ZoneDfaState<S>, LearnLibAction<String>>> waitlist =
                searchStrategy.createWaitlist();
        waitlist.addAll(arg.getInitNodes());

        final Map<ZoneDfaState<S>, ArgNode<ZoneDfaState<S>, LearnLibAction<String>>> passed =
                new HashMap<>();
        long startTime = System.currentTimeMillis();
        int nodeCount = 0;

        final List<String> symbols = List.copyOf(inputs);
        final int symbolCount = symbols.size();
        @SuppressWarnings("unchecked")
        final Collection<? extends ZoneDfaState<S>>[] succBySymbol = new Collection[symbolCount];
        @SuppressWarnings("unchecked")
        final LearnLibAction<String>[] actionBySymbol = new LearnLibAction[symbolCount];

        logger.write(Logger.Level.SUBSTEP,
                "  [InclusionOracle] starting ARG exploration (hypothesis states: %d, alphabet size: %d)%n",
                hypothesis.size(), inputs.size());

        while (!waitlist.isEmpty()) {
            ArgNode<ZoneDfaState<S>, LearnLibAction<String>> node = waitlist.remove();
            S currentDfaState = node.getState().getDfaState();
            nodeCount++;

            if (nodeCount % ARG_LOG_INTERVAL == 0) {
                logger.write(Logger.Level.SUBSTEP,
                        "  [InclusionOracle] %d nodes processed, passed=%d, elapsed=%d ms%n",
                        nodeCount, passed.size(),
                        System.currentTimeMillis() - startTime);
            }

            for (int i = 0; i < symbolCount; i++) {
                final String symbol = symbols.get(i);
                S nextDfaState = hypothesis.getTransition(currentDfaState, symbol);

                if (nextDfaState == null || !hypothesis.isAccepting(nextDfaState)) {
                    actionBySymbol[i] = null;
                    succBySymbol[i] = null;
                    continue;
                }

                LearnLibAction<String> action = LearnLibAction.create(symbol);
                actionBySymbol[i] = action;
                Collection<? extends ZoneDfaState<S>> succStates =
                        transFunc.getSuccStates(node.getState(), action, prec);
                succBySymbol[i] = succStates;

                if (allBottom(succStates)) {
                    Word<String> candidate = extractWord(node).append(symbol);

                    if (!sulAccepts(candidate)) {
                        return new DefaultQuery<>(candidate, false);
                    }
                }
            }

            if (node.getState().isBottom()) {
                continue;
            }

            final ArgNode<ZoneDfaState<S>, LearnLibAction<String>> coverer =
                    passed.get(node.getState());
            if (coverer != null) {
                node.setCoveringNode(coverer);
                continue;
            }

            if (node.isTarget()) {
                return new DefaultQuery<>(extractWord(node), true);
            }
            passed.put(node.getState(), node);

            for (int i = 0; i < symbolCount; i++) {
                final String symbol = symbols.get(i);
                if (hypothesis.getTransition(currentDfaState, symbol) == null) {
                    continue;
                }
                LearnLibAction<String> cached = actionBySymbol[i];
                Collection<? extends ZoneDfaState<S>> succStates;
                if (cached == null) {
                    cached = LearnLibAction.create(symbol);
                    succStates = transFunc.getSuccStates(node.getState(), cached, prec);
                } else {
                    succStates = succBySymbol[i];
                }
                final LearnLibAction<String> action = cached;

                final boolean needsSiblingScan = succStates.size() > 1;
                for (final ZoneDfaState<S> succState : succStates) {
                    if (needsSiblingScan && node.getSuccNodes().anyMatch(n ->
                            n.getInEdge().get().getAction().equals(action)
                                    && ord.isLeq(succState, n.getState()))) {
                        continue;
                    }
                    arg.createSuccNode(node, action, succState, targetPred.test(succState));
                }
            }
            node.expanded = true;
            waitlist.addAll(node.getSuccNodes());
        }

        logger.write(Logger.Level.SUBSTEP,
                "  [InclusionOracle] converged - %d nodes processed in %d ms, no counterexample%n",
                nodeCount, System.currentTimeMillis() - startTime);
        return null;
    }

    private static <S> boolean allBottom(final Collection<? extends ZoneDfaState<S>> succStates) {
        for (final ZoneDfaState<S> succState : succStates) {
            if (!succState.isBottom()) {
                return false;
            }
        }
        return true;
    }

    private <S> Word<String> extractWord(ArgNode<ZoneDfaState<S>, LearnLibAction<String>> node) {
        List<String> symbols = new ArrayList<>();
        ArgNode<ZoneDfaState<S>, LearnLibAction<String>> current = node;
        while (current.getInEdge().isPresent()) {
            ArgEdge<ZoneDfaState<S>, LearnLibAction<String>> edge = current.getInEdge().get();
            symbols.add(edge.getAction().getSymbol());
            current = edge.getSource();
        }
        Collections.reverse(symbols);
        return Word.fromList(symbols);
    }

    private boolean sulAccepts(Word<String> word) {
        DefaultQuery<String, Boolean> q = new DefaultQuery<>(word);
        mqOracle.processQuery(q);
        return Boolean.TRUE.equals(q.getOutput());
    }
}
