package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.xta.XtaProcess;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UntimedAutomatonBuilder {
    private UntimedAutomatonBuilder() {}

    public static FastDFA<String> build(XtaProcess process, Alphabet<String> alphabet) {
        FastDFA<String> dfa = new FastDFA<>(alphabet);
        Map<XtaProcess.Loc, FastDFAState> stateMap = new HashMap<>();

        // 1. Állapotok létrehozása (minden állapot elfogadó)
        for (XtaProcess.Loc loc : process.getLocs()) {
            FastDFAState dfaState = dfa.addState(true);
            stateMap.put(loc, dfaState);

            if (loc.equals(process.getInitLoc())) {
                dfa.setInitialState(dfaState);
            }
        }

        // 2. A folyamat SAJÁT éleinek kigyűjtése és hozzáadása a DFA-hoz
        Set<String> processSymbols = new HashSet<>();
        for (XtaProcess.Loc loc : process.getLocs()) {
            for (XtaProcess.Edge edge : loc.getOutEdges()) {
                String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
                processSymbols.add(symbol);
                dfa.addTransition(stateMap.get(loc), symbol, stateMap.get(edge.getTarget()));
            }
        }

        // 3. A KÖRNYEZET lépéseinek engedélyezése (A Kulcslépés!)
        // Azokra a szimbólumokra, amik nem a pajzs folyamathoz tartoznak (pl. ErrorProc, p1, p2),
        // húzunk egy hurokélt, így a pajzs "egy helyben toporog", miközben a többiek lépnek.
        for (XtaProcess.Loc loc : process.getLocs()) {
            FastDFAState dfaState = stateMap.get(loc);
            for (String symbol : alphabet) {
                if (!processSymbols.contains(symbol)) {
                    dfa.addTransition(dfaState, symbol, dfaState);
                }
            }
        }

        return dfa;
    }
}
