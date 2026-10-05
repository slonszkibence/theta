package hu.bme.mit.theta.xsts.learning.inclusion;

import hu.bme.mit.theta.xsts.learning.common.LearningCancellation;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ArgEdge;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.analysis.zone.BoundFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xsts.learning.common.LearnLibAction;
import hu.bme.mit.theta.xsts.learning.common.XstsTimingMapper;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimedXstsInclusionOracle<I> implements EquivalenceOracle.DFAEquivalenceOracle<I> {
    private static final int ARG_LOG_INTERVAL = 500;

    private final Map<VarDecl<ClockType>, Integer> ceilings;
    private final XstsTimingMapper<I, ?, ?> mapper;
    private final BoundFunc luBounds;
    private final MembershipOracle.DFAMembershipOracle<I> membershipOracle;
    private final SearchStrategy searchStrategy;
    private final Logger logger;

    private TimedXstsInclusionOracle (
            Map<VarDecl<ClockType>, Integer> ceilings,
            XstsTimingMapper<I, ?, ?> mapper,
            BoundFunc luBounds,
            MembershipOracle.DFAMembershipOracle<I> membershipOracle,
            SearchStrategy searchStrategy,
            Logger logger
    ) {
        this.ceilings = checkNotNull(ceilings);
        this.mapper = checkNotNull(mapper);
        this.luBounds = checkNotNull(luBounds);
        this.membershipOracle = checkNotNull(membershipOracle);
        this.searchStrategy = checkNotNull(searchStrategy);
        this.logger = checkNotNull(logger);
    }

    public static<I> TimedXstsInclusionOracle<I> create(
            Map<VarDecl<ClockType>, Integer> ceilings,
            XstsTimingMapper<I, ?, ?> mapper,
            BoundFunc luBounds,
            MembershipOracle.DFAMembershipOracle<I> membershipOracle,
            SearchStrategy searchStrategy,
            Logger logger
    ) {
        return new TimedXstsInclusionOracle<>(ceilings, mapper, luBounds, membershipOracle, searchStrategy, logger);
    }

    @Override
    public DefaultQuery<I, Boolean> findCounterExample(DFA<?, I> hypothesis, Collection<? extends I> inputs) {
        return doFindCounterExample(hypothesis, inputs);
    }

    private <S> DefaultQuery<I, Boolean> doFindCounterExample(DFA<S, I> hypothesis, Collection<? extends I> inputs) {
        ZoneDfaInitFunc<S, I> initFunc = ZoneDfaInitFunc.create(hypothesis);
        ZoneDfaTransFunc<S, I> transFunc = ZoneDfaTransFunc.create(hypothesis, ceilings, mapper);
        ZoneDfaOrd<S> ord = ZoneDfaOrd.create(luBounds);
        ZoneDfaLts<S, I> lts = ZoneDfaLts.create(hypothesis, inputs);

        Analysis<ZoneDfaState<S>, LearnLibAction<I>, ZonePrec> analysis = ZoneDfaAnalysis.create(initFunc, transFunc, ord);
        Predicate<ZoneDfaState<S>> targetPred = state -> !hypothesis.isAccepting(state.getDfaState());
        ArgBuilder<ZoneDfaState<S>, LearnLibAction<I>, ZonePrec> argBuilder = ArgBuilder.create(lts, analysis, targetPred);
        ZonePrec prec = ZonePrec.of(ceilings.keySet());

        ARG<ZoneDfaState<S>, LearnLibAction<I>> arg = argBuilder.createArg();
        argBuilder.init(arg, prec);

        Waitlist<ArgNode<ZoneDfaState<S>, LearnLibAction<I>>> waitlist = searchStrategy.createWaitlist();
        waitlist.addAll(arg.getInitNodes());

        Map<S, List<ArgNode<ZoneDfaState<S>, LearnLibAction<I>>>> passed = new HashMap<>();
        long startTime = System.currentTimeMillis();

        int nodeCount = 0;
        int bottomCount = 0;
        int subsumedCount = 0;
        int expandedCount = 0;
        logger.write(Logger.Level.SUBSTEP,
                "    [ARG] start: |H|=%d, |alphabet|=%d%n",
                hypothesis.size(), inputs.size());

        while (!waitlist.isEmpty()) {
            LearningCancellation.check("inclusion oracle ARG expansion");
            ArgNode<ZoneDfaState<S>, LearnLibAction<I>> node = waitlist.remove();
            S currentDfaState = node.getState().getDfaState();
            nodeCount++;

            if (nodeCount % ARG_LOG_INTERVAL == 0) {
                logger.write(Logger.Level.DETAIL,
                        "    [ARG] %d processed (%d expanded, %d subsumed, %d bottom), %d ms%n",
                        nodeCount, expandedCount, subsumedCount, bottomCount,
                        System.currentTimeMillis() - startTime);
            }

            for (I symbol : inputs) {
                S nextDfaState = hypothesis.getTransition(currentDfaState, symbol);
                if (nextDfaState == null || !hypothesis.isAccepting(nextDfaState)) {
                    continue;
                }

                LearnLibAction<I> action = LearnLibAction.create(symbol);
                Collection<? extends ZoneDfaState<S>> succStates =
                        transFunc.getSuccStates(node.getState(), action, prec);

                if (succStates.isEmpty() || succStates.stream().allMatch(ZoneDfaState::isBottom)) {
                    Word<I> candidate = extractWord(node).append(symbol);

                    if (!sulAccepts(candidate)) {
                        logger.write(Logger.Level.SUBSTEP,
                                "    [ARG] SPURIOUS word (H accepts, zone dies), length %d "
                                        + "after %d processed / %d expanded, %d ms%n",
                                candidate.size(), nodeCount, expandedCount,
                                System.currentTimeMillis() - startTime);
                        return new DefaultQuery<>(candidate, false);
                    }
                }
            }

            if (node.getState().isBottom()) {
                bottomCount++;
                logger.write(Logger.Level.VERBOSE, "      [ARG] node #%d: bottom%n", nodeCount);
                continue;
            }

            List<ArgNode<ZoneDfaState<S>, LearnLibAction<I>>> potentialCoverers =
                    passed.getOrDefault(currentDfaState, Collections.emptyList());
            boolean subsumed = false;
            for (var passedNode : potentialCoverers) {
                if (ord.isLeq(node.getState(), passedNode.getState())) {
                    subsumed = true;
                    node.setCoveringNode(passedNode);
                    break;
                }
            }
            if (subsumed) {
                subsumedCount++;
                logger.write(Logger.Level.VERBOSE, "      [ARG] node #%d: subsumed%n", nodeCount);
                continue;
            }

            if (node.isTarget()) {
                final Word<I> cex = extractWord(node);
                logger.write(Logger.Level.SUBSTEP,
                        "    [ARG] MISSING word (H rejects a feasible one), length %d "
                                + "after %d processed / %d expanded, %d ms%n",
                        cex.size(), nodeCount, expandedCount,
                        System.currentTimeMillis() - startTime);
                return new DefaultQuery<>(cex, true);
            }

            passed.computeIfAbsent(currentDfaState, k -> new ArrayList<>()).add(node);

            expandedCount++;
            logger.write(Logger.Level.VERBOSE, "      [ARG] node #%d: expanded%n", nodeCount);
            argBuilder.expand(node, prec);
            waitlist.addAll(node.getSuccNodes());
        }

        logger.write(Logger.Level.SUBSTEP,
                "    [ARG] converged, no counterexample: %d processed (%d expanded, %d subsumed, "
                        + "%d bottom), %d ms%n",
                nodeCount, expandedCount, subsumedCount, bottomCount,
                System.currentTimeMillis() - startTime);
        return null;
    }

    private <S> Word<I> extractWord(ArgNode<ZoneDfaState<S>, LearnLibAction<I>> node) {
        List<I> symbols = new ArrayList<>();
        ArgNode<ZoneDfaState<S>, LearnLibAction<I>> current = node;
        while (current.getInEdge().isPresent()) {
            ArgEdge<ZoneDfaState<S>, LearnLibAction<I>> edge = current.getInEdge().get();
            symbols.add(edge.getAction().getSymbol());
            current = edge.getSource();
        }
        Collections.reverse(symbols);
        return Word.fromList(symbols);
    }


    private boolean sulAccepts(Word<I> word) {
        DefaultQuery<I, Boolean> query = new DefaultQuery<>(word);
        membershipOracle.processQuery(query);
        return Boolean.TRUE.equals(query.getOutput());
    }
}
