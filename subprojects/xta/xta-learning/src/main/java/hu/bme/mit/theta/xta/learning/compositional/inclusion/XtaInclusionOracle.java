package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ArgEdge;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.analysis.zone.BoundFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;


import java.util.*;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;


public class XtaInclusionOracle implements EquivalenceOracle.DFAEquivalenceOracle<String> {
    private final Map<VarDecl<RatType>, Integer> ceilings;
    private final XtaTimingMapper<Boolean, Boolean> mapper;
    private final BoundFunc luBounds;
    private final MembershipOracle.DFAMembershipOracle<String> mqOracle;

    private XtaInclusionOracle(Map<VarDecl<RatType>, Integer> ceilings,
                               XtaTimingMapper<Boolean, Boolean> mapper,
                               BoundFunc luBounds,
                               MembershipOracle.DFAMembershipOracle<String> mqOracle) {
        this.ceilings = checkNotNull(ceilings);
        this.mapper = checkNotNull(mapper);
        this.luBounds = checkNotNull(luBounds);
        this.mqOracle = checkNotNull(mqOracle);
    }

    public static XtaInclusionOracle create(Map<VarDecl<RatType>, Integer> ceilings,
                                            XtaTimingMapper<Boolean, Boolean> mapper,
                                            BoundFunc luBounds,
                                            MembershipOracle.DFAMembershipOracle<String> mqOracle) {
        return new XtaInclusionOracle(ceilings, mapper, luBounds, mqOracle);
    }

    @Override
    public DefaultQuery<String, Boolean> findCounterExample(DFA<?, String> hypothesis, Collection<? extends String> inputs) {
        return doFindCounterExample(hypothesis, inputs);
    }


    private <S> DefaultQuery<String, Boolean> doFindCounterExample(DFA<S, String> hypothesis, Collection<? extends String> inputs) {
        ZoneDfaInitFunc<S, ZonePrec> initFunc = ZoneDfaInitFunc.create(hypothesis, ceilings);
        ZoneDfaTransFunc<S, ZonePrec> transFunc =  ZoneDfaTransFunc.create(hypothesis, ceilings, mapper);
        ZoneDfaOrd<S> ord = ZoneDfaOrd.create(luBounds);
        ZoneDfaLts<S> lts = ZoneDfaLts.create(hypothesis, inputs);

        Analysis<ZoneDfaState<S>, LearnLibAction<String>, ZonePrec> analysis = ZoneDfaAnalysis.create(initFunc, transFunc, ord);
        Predicate<ZoneDfaState<S>> targetPred = state -> !hypothesis.isAccepting(state.getDfaState());
        ArgBuilder<ZoneDfaState<S>, LearnLibAction<String>, ZonePrec> argBuilder = ArgBuilder.create(lts, analysis, targetPred);
        ZonePrec prec = ZonePrec.of(ceilings.keySet());

        ARG<ZoneDfaState<S>, LearnLibAction<String>> arg = argBuilder.createArg();
        argBuilder.init(arg, prec);

        Waitlist<ArgNode<ZoneDfaState<S>, LearnLibAction<String>>> waitlist = FifoWaitlist.create();
        waitlist.addAll(arg.getInitNodes());

        Map<S, List<ArgNode<ZoneDfaState<S>, LearnLibAction<String>>>> passed = new HashMap<>();

        while (!waitlist.isEmpty()) {
            ArgNode<ZoneDfaState<S>, LearnLibAction<String>> node = waitlist.remove();
            S currentDfaState = node.getState().getDfaState();


            for (String symbol : inputs) {
                S nextDfaState = hypothesis.getTransition(currentDfaState, symbol);

                if (hypothesis.isAccepting(nextDfaState)) {
                    LearnLibAction<String> action = LearnLibAction.create(symbol);
                    Collection<? extends ZoneDfaState<S>> succStates =
                            transFunc.getSuccStates(node.getState(), action, prec);


                    if (succStates.isEmpty() || succStates.stream().allMatch(ZoneDfaState::isBottom)) {
                        Word<String> candidate = extractWord(node).append(symbol);

                        if (!sulAccepts(candidate)) {
                            return new DefaultQuery<>(candidate, false);
                        }
                    }
                }
            }

            if (node.getState().isBottom()) {
                continue;
            }

            List<ArgNode<ZoneDfaState<S>, LearnLibAction<String>>> potentialCoverers =
                    passed.getOrDefault(currentDfaState, Collections.emptyList());

            boolean subsumed = false;
            for (var passedNode : potentialCoverers) {
                if (ord.isLeq(node.getState(), passedNode.getState())) {
                    subsumed = true;
                    node.setCoveringNode(passedNode);
                    break;
                }
            }
            if (subsumed) continue;

            if (node.isTarget()) {
                return new DefaultQuery<>(extractWord(node), true);
            }
            passed.computeIfAbsent(currentDfaState, k -> new ArrayList<>()).add(node);
            argBuilder.expand(node, prec);
            waitlist.addAll(node.getSuccNodes());
        }

        return null;
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