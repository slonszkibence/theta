package hu.bme.mit.theta.xta.learning.inclusion2_0;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.compositional.dfa.XtaDfaState;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class XtaInclusionDfaTransFunc<S extends State, P extends Prec, DfaState>
        implements TransFunc<XtaDfaState<S, DfaState>, XtaAction, P> {
    private final TransFunc<XtaState<S>, XtaAction, P> xtaTransFunc;
    private final DFA<DfaState, String> dfa;
    private final Alphabet<String> alphabet;
    private final Set<String> shieldSymbols;

    private XtaInclusionDfaTransFunc(
            TransFunc<XtaState<S>, XtaAction, P> xtaTransFunc,
            DFA<DfaState, String> dfa,
            Alphabet<String> alphabet,
            Set<String> shieldSymbols
    ) {
        this.xtaTransFunc = checkNotNull(xtaTransFunc);
        this.dfa = checkNotNull(dfa);
        this.alphabet = checkNotNull(alphabet);
        this.shieldSymbols = checkNotNull(shieldSymbols);
    }

    public static<S extends State, P extends Prec, DfaState> XtaInclusionDfaTransFunc<S, P, DfaState> create(
            TransFunc<XtaState<S>, XtaAction, P> xtaTransFunc,
            DFA<DfaState, String> dfa,
            Alphabet<String> alphabet,
            Set<String> shieldSymbols
    ) {
        return new XtaInclusionDfaTransFunc<>(xtaTransFunc, dfa, alphabet, shieldSymbols);
    }

    @Override
    public Collection<XtaDfaState<S, DfaState>> getSuccStates(XtaDfaState<S, DfaState> state, XtaAction action, P prec) {
        DfaState dfaState = state.getDfaState();

        if (dfaState == null) {
            return List.of();
        }

        XtaProcess.Edge representativeEdge = null;
        if (action.isBasic())
            representativeEdge = action.asBasic().getEdge();
        else if (action.isBinary())
            representativeEdge = action.asBinary().getEmitEdge();
        else if (action.isBroadcast())
            representativeEdge = action.asBroadcast().getEmitEdge();

        if (representativeEdge != null) {
            String symbol = XtaTimingMapper.generateSymbolForEdge(representativeEdge);

            if (!symbol.contains("ErrorLoc")) {
                if (alphabet.containsSymbol(symbol)) {
                    dfaState = dfa.getSuccessor(dfaState, symbol);
                }
            }
        }

        Collection<? extends XtaState<S>> xtaSuccStates = xtaTransFunc.getSuccStates(state.getXtaState(), action, prec);
        final DfaState finalDfaState = dfaState;

        return xtaSuccStates.stream()
                .map(xtaSuccState -> XtaDfaState.create(xtaSuccState, finalDfaState))
                .collect(Collectors.toList());
    }
}
