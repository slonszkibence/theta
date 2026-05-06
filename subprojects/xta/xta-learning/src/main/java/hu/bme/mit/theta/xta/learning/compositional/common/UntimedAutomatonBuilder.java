package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.xta.XtaProcess;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the untimed DFA representation of a single {@link XtaProcess}.
 * <p>
 * In the learning-based compositional model checking framework, the input
 * model is seen as a parallel composition {@code A ‖ T}, where {@code A} is
 * a large finite-state machine and {@code T} is a relatively small timed
 * automaton. This builder constructs the {@code A} component for a single
 * process by discarding all clock guards, resets, and invariants, and
 * retaining only the discrete location structure and the edge labels.
 * <p>
 * All locations are mapped to accepting DFA states, since the untimed
 * automaton captures reachability of discrete behavior rather than
 * a specific acceptance condition. Edges whose symbols are not present
 * in the provided alphabet are silently skipped.
 */
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
                if (alphabet.containsSymbol(symbol)) {
                    dfa.addTransition(stateMap.get(loc), symbol, stateMap.get(edge.getTarget()));
                }
            }
        }

        return dfa;
    }
}