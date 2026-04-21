package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import de.learnlib.oracle.EquivalenceOracle;
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

    private XtaInclusionOracle(Map<VarDecl<RatType>, Integer> ceilings, XtaTimingMapper<Boolean, Boolean> mapper) {
        this.ceilings = checkNotNull(ceilings);
        this.mapper = checkNotNull(mapper);
    }

    public static XtaInclusionOracle create(Map<VarDecl<RatType>, Integer> ceilings, XtaTimingMapper<Boolean, Boolean> mapper) {
        return new XtaInclusionOracle(ceilings, mapper);
    }

    @Override
    public DefaultQuery<String, Boolean> findCounterExample(DFA<?, String> hypothesis, Collection<? extends String> inputs) {
        return doFindCounterExample(hypothesis, inputs);
    }


    private <S> DefaultQuery<String, Boolean> doFindCounterExample(DFA<S, String> hypothesis, Collection<? extends String> inputs) {
        ZoneDfaInitFunc<S, ZonePrec> initFunc = ZoneDfaInitFunc.create(hypothesis, ceilings);
        ZoneDfaTransFunc<S, ZonePrec> transFunc =  ZoneDfaTransFunc.create(hypothesis, ceilings, mapper);
        ZoneDfaOrd<S> ord = ZoneDfaOrd.create();
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

            if (node.getState().isBottom()) {
                continue;
            }

            S currentDfaState = node.getState().getDfaState();
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
                List<String> counterExampleList = new ArrayList<>();
                ArgNode<ZoneDfaState<S>, LearnLibAction<String>> currentNode = node;

                while (currentNode.getInEdge().isPresent()) {
                    ArgEdge<ZoneDfaState<S>, LearnLibAction<String>> inEdge = currentNode.getInEdge().get();
                    counterExampleList.add(inEdge.getAction().getSymbol());

                    currentNode = inEdge.getSource();
                }
                Collections.reverse(counterExampleList);
                Word<String> counterExampleWord = Word.fromList(counterExampleList);

                return new DefaultQuery<>(counterExampleWord, true);
            }
            passed.computeIfAbsent(currentDfaState, k -> new ArrayList<>()).add(node);
            argBuilder.expand(node, prec);
            waitlist.addAll(node.getSuccNodes());
        }

        return null;
    }

}