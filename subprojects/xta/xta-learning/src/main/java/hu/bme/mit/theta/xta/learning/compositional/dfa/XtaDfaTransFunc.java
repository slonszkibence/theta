package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
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
        List<XtaProcess.Edge> edges = extractEdgesFromAction(xtaAction);

        for (XtaProcess.Edge edge : edges) {
            String symbol = XtaTimingMapper.generateSymbolForEdge(edge);

            if (shieldSymbols.contains(symbol)) {
                continue;
            }
            if (symbol.contains("ErrorLoc")) {
                continue;
            }

            if (alphabet.containsSymbol(symbol)) {
                dfaState = dfa.getSuccessor(dfaState, symbol);
                if (dfaState == null) {
                    return Collections.emptyList();
                }
            }
        }

        Collection<? extends XtaState<S>> xtaSuccStates = xtaTransFunc.getSuccStates(xtaDfaState.getXtaState(), xtaAction, prec);
        final DfaState finalDfaState = dfaState;

        return xtaSuccStates.stream()
                .map(xtaSuccState -> XtaDfaState.create(xtaSuccState, finalDfaState))
                .collect(Collectors.toList());
    }

    private List<XtaProcess.Edge> extractEdgesFromAction(XtaAction xtaAction) {
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
        return edges;
    }
}