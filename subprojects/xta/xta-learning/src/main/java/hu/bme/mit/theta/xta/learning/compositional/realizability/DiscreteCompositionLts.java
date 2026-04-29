package hu.bme.mit.theta.xta.learning.compositional.realizability;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaLts;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class DiscreteCompositionLts<S> implements LTS<DiscreteCompositionState<S>, XtaAction> {
    private final Alphabet<String> alphabet;
    private final DFA<S, String> productDfa;
    private final LTS<XtaState<?>, XtaAction> innerLts;

    private DiscreteCompositionLts(XtaSystem xtaSystem, Alphabet<String> alphabet, DFA<S, String> productDfa) {
        this.alphabet = checkNotNull(alphabet);
        this.productDfa = checkNotNull(productDfa);
        this.innerLts = XtaLts.create(xtaSystem);
    }

    public static<S> DiscreteCompositionLts<S> create(XtaSystem xtaSystem, Alphabet<String> alphabet, DFA<S, String> productDfa) {
        return new DiscreteCompositionLts<>(xtaSystem, alphabet, productDfa);
    }

    @Override
    public Collection<XtaAction> getEnabledActionsFor(final DiscreteCompositionState<S> compositionState) {
        final Collection<XtaAction> xtaActions =
                innerLts.getEnabledActionsFor(compositionState.getXtaState());

        Collection<XtaAction> validDiscreteActions = new ArrayList<>();

        for (XtaAction xtaAction : xtaActions) {
            if (isAccepting(xtaAction, compositionState.getDfaState())) {
                validDiscreteActions.add(xtaAction);
            }
        }

        return validDiscreteActions;
    }

    private boolean isAccepting(final XtaAction xtaAction, S dfaState) {
        List<XtaProcess.Edge> edges = new ArrayList<>();

        if (xtaAction.isBasic()) {
            edges.add(xtaAction.asBasic().getEdge());
        }
        else if (xtaAction.isBinary()) {
            edges.add(xtaAction.asBinary().getEmitEdge());
            edges.add(xtaAction.asBinary().getRecvEdge());
        }
        else if (xtaAction.isBroadcast()) {
            edges.add(xtaAction.asBroadcast().getEmitEdge());
            edges.addAll(xtaAction.asBroadcast().getRecvEdges());
        }

        S currDfaState = dfaState;
        for (XtaProcess.Edge edge : edges) {
            String symbol = XtaTimingMapper.generateSymbolForEdge(edge);

            if (alphabet.containsSymbol(symbol)) {
                currDfaState = productDfa.getTransition(currDfaState, symbol);

                if (currDfaState == null) {
                    return false;
                }
            }
        }
        return productDfa.isAccepting(currDfaState);
    }
}
