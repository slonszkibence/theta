package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionTransFunc<S, P extends Prec>
        implements TransFunc<DiscreteCompositionState<S>, XtaAction, P> {
    private final TransFunc<XtaState<ExplState>, XtaAction, P> innerTransFunc;
    private final DFA<S, String> productDfa;
    private final Alphabet<String> alphabet;

    private DiscreteCompositionTransFunc(TransFunc<XtaState<ExplState>, XtaAction, P> innerTransFunc,
                                         DFA<S, String> productDfa,
                                         Alphabet<String> alphabet) {
        this.innerTransFunc = checkNotNull(innerTransFunc);
        this.productDfa = checkNotNull(productDfa);
        this.alphabet = checkNotNull(alphabet);
    }

    public static <S, P extends Prec> DiscreteCompositionTransFunc<S, P> create(
            TransFunc<XtaState<ExplState>, XtaAction, P> innerTransFunc,
            DFA<S, String> productDfa,
            Alphabet<String> alphabet) {
        return new DiscreteCompositionTransFunc<>(innerTransFunc, productDfa, alphabet);
    }


    @Override
    public Collection<DiscreteCompositionState<S>> getSuccStates(DiscreteCompositionState<S> state,
                                                                 XtaAction action,
                                                                 P prec) {
        Collection<? extends XtaState<ExplState>> nextStates
                = innerTransFunc.getSuccStates(state.getXtaState(), action, prec);

        String symbol = extractSymbolForAction(action);

        S nextDfaState = state.getDfaState();
        if (symbol != null && alphabet.containsSymbol(symbol)) {
            nextDfaState = productDfa.getTransition(state.getDfaState(), symbol);
        }

        Collection<DiscreteCompositionState<S>> result = new ArrayList<>();
        for (XtaState<ExplState> xtaState : nextStates) {
            result.add(DiscreteCompositionState.create(xtaState, nextDfaState));
        }

        return result;
    }

    private String extractSymbolForAction(XtaAction xtaAction) {
        List<XtaProcess.Edge> edges = new ArrayList<>();

        if (xtaAction.isBasic()) {
            edges.add(xtaAction.asBasic().getEdge());
        } else if (xtaAction.isBinary()) {
            edges.add(xtaAction.asBinary().getEmitEdge());
            edges.add(xtaAction.asBinary().getRecvEdge());
        } else if (xtaAction.isBroadcast()) {
            edges.add(xtaAction.asBroadcast().getEmitEdge());
            edges.addAll(xtaAction.asBroadcast().getRecvEdges());
        }

        for (XtaProcess.Edge edge : edges) {
            String symbol = edge.getSource().getName() + "->" + edge.getTarget().getName();
            if (alphabet.containsSymbol(symbol)) {
                return symbol;
            }
        }
        return null;
    }
}
