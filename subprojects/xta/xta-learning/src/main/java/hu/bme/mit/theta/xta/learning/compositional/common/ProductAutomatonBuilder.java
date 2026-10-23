package hu.bme.mit.theta.xta.learning.compositional.common;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public final class ProductAutomatonBuilder {
    private ProductAutomatonBuilder() {}

    public static <S, I> FastDFA<I> copy(final DFA<S, I> hypothesis, final Alphabet<I> alphabet) {
        final FastDFA<I> copy = new FastDFA<>(alphabet);
        final S init = hypothesis.getInitialState();
        if (init == null) {
            return copy;
        }
        final Map<S, FastDFAState> visited = new HashMap<>();
        final Queue<S> queue = new ArrayDeque<>();
        final FastDFAState copiedInit = copy.addState(hypothesis.isAccepting(init));
        copy.setInitialState(copiedInit);
        visited.put(init, copiedInit);
        queue.add(init);
        while (!queue.isEmpty()) {
            final S current = queue.poll();
            if (!hypothesis.isAccepting(current)) {
                continue;
            }
            for (final I symbol : alphabet) {
                final S succ = hypothesis.getTransition(current, symbol);
                if (succ == null) {
                    continue;
                }
                FastDFAState copiedSucc = visited.get(succ);
                if (copiedSucc == null) {
                    copiedSucc = copy.addState(hypothesis.isAccepting(succ));
                    visited.put(succ, copiedSucc);
                    queue.add(succ);
                }
                copy.addTransition(visited.get(current), symbol, copiedSucc);
            }
        }
        return copy;
    }

    public static <I> FastDFAState addUniversalState(final FastDFA<I> dfa,
                                                     final Alphabet<I> alphabet) {
        final FastDFAState universalState = dfa.addState(true);
        for (final I symbol : alphabet) {
            dfa.addTransition(universalState, symbol, universalState);
        }
        return universalState;
    }
}
