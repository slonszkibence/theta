/*
 *  Copyright 2026 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
