package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.xta.XtaProcess;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.HashMap;
import java.util.Map;

public class UntimedAutomatonBuilder {
    private UntimedAutomatonBuilder() {}

    public static FastDFA<String> build(XtaProcess process, Alphabet<String> alphabet) {
        FastDFA<String> dfa = new FastDFA<>(alphabet);

        Map<XtaProcess.Loc, FastDFAState> stateMap = new HashMap<>();

        for (XtaProcess.Loc loc : process.getLocs()) {
            FastDFAState dfaState = dfa.addState(true);
            stateMap.put(loc, dfaState);

            if (loc.equals(process.getInitLoc())) {
                dfa.setInitialState(dfaState);
            }
        }

        for (XtaProcess.Loc loc : process.getLocs()) {
            for (XtaProcess.Edge edge : loc.getOutEdges()) {

                String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
                dfa.addTransition(stateMap.get(loc), symbol, stateMap.get(edge.getTarget()));
            }
        }


        return dfa;
    }
}
