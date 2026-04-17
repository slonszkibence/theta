package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class XtaDfaTransFunc<S extends State, P extends Prec, DfaState>
        implements TransFunc<XtaDfaState<S, DfaState>, XtaAction, P> {
    private final TransFunc<XtaState<S>, XtaAction, P> xtaTransFunc;
    private final DFA<DfaState, String> dfa;
    private final Alphabet<String> alphabet;
    private final Set<String> shieldSymbols;

    private XtaDfaTransFunc(TransFunc<XtaState<S>, XtaAction, P> xtaTransFunc, DFA<DfaState, String> dfa,
                            Alphabet<String> alphabet, Set<String> shieldSymbols) {
        this.xtaTransFunc = checkNotNull(xtaTransFunc);
        this.dfa = checkNotNull(dfa);
        this.alphabet = checkNotNull(alphabet);
        this.shieldSymbols = checkNotNull(shieldSymbols);
    }

    public static<S extends State, P extends Prec, DfaState> XtaDfaTransFunc<S, P, DfaState> create(
            TransFunc<XtaState<S>, XtaAction, P> xtaTransFunc, DFA<DfaState, String> dfa,
            Alphabet<String> alphabet, Set<String> shieldSymbols) {
        return new XtaDfaTransFunc<>(xtaTransFunc, dfa, alphabet, shieldSymbols);
    }

    @Override
    public Collection<XtaDfaState<S, DfaState>> getSuccStates(XtaDfaState<S, DfaState> xtaDfaState, XtaAction xtaAction, P prec) {
        DfaState dfaState = xtaDfaState.getDfaState();

        XtaProcess.Edge representativeEdge = null;
        if (xtaAction.isBasic()) representativeEdge = xtaAction.asBasic().getEdge();
        else if (xtaAction.isBinary()) representativeEdge = xtaAction.asBinary().getEmitEdge();
        else if (xtaAction.isBroadcast()) representativeEdge = xtaAction.asBroadcast().getEmitEdge();

        if (representativeEdge != null) {
            String symbol = XtaTimingMapper.generateSymbolForEdge(representativeEdge);

            if (!shieldSymbols.contains(symbol) && !symbol.contains("ErrorLoc")) {
                if (alphabet.containsSymbol(symbol)) {
                    dfaState = dfa.getSuccessor(dfaState, symbol);
                    if (dfaState == null) {
                        return Collections.emptyList();
                    }
                }
            }
        }

        Collection<? extends XtaState<S>> xtaSuccStates = xtaTransFunc.getSuccStates(xtaDfaState.getXtaState(), xtaAction, prec);
        final DfaState finalDfaState = dfaState;

        return xtaSuccStates.stream()
                .map(xtaSuccState -> XtaDfaState.create(xtaSuccState, finalDfaState))
                .collect(Collectors.toList());
    }

    private List<String> extractWordFromTrace(Trace<?, XtaAction> trace) {
        List<String> word = new ArrayList<>();
        for (XtaAction action : trace.getActions()) {
            XtaProcess.Edge representativeEdge = null;

            if (action.isBasic()) {
                representativeEdge = action.asBasic().getEdge();
            } else if (action.isBinary()) {
                representativeEdge = action.asBinary().getEmitEdge(); // Csak az emit!
            } else if (action.isBroadcast()) {
                representativeEdge = action.asBroadcast().getEmitEdge(); // Csak az emit!
            }

            if (representativeEdge != null) {
                String symbol = XtaTimingMapper.generateSymbolForEdge(representativeEdge);
                if (alphabet.containsSymbol(symbol)) {
                    word.add(symbol);
                }
            }
        }
        return word;
    }
}