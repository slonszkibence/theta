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
package hu.bme.mit.theta.analysis.dfa;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.automatalib.automaton.fsa.DFA;

final class DfaTransFunc<S, A extends Action, I> implements TransFunc<DfaState<S>, A, UnitPrec> {

    private final DFA<S, I> dfa;
    private final Set<I> alphabet;
    private final ActionToSymbol<? super A, I> actionToSymbol;
    private final Collection<DfaState<S>> top;
    private final Collection<DfaState<S>> bottom;

    private DfaTransFunc(
            final DFA<S, I> dfa,
            final Collection<? extends I> alphabet,
            final ActionToSymbol<? super A, I> actionToSymbol) {
        this.dfa = checkNotNull(dfa);
        this.alphabet = Set.copyOf(alphabet);
        this.actionToSymbol = checkNotNull(actionToSymbol);
        this.top = List.of(DfaState.top());
        this.bottom = List.of(DfaState.bottom());
    }

    public static <S, A extends Action, I> DfaTransFunc<S, A, I> create(
            final DFA<S, I> dfa,
            final Collection<? extends I> alphabet,
            final ActionToSymbol<? super A, I> actionToSymbol) {
        return new DfaTransFunc<>(dfa, alphabet, actionToSymbol);
    }

    @Override
    public Collection<DfaState<S>> getSuccStates(
            final DfaState<S> state, final A action, final UnitPrec prec) {
        checkNotNull(state);
        checkNotNull(action);
        checkNotNull(prec);
        if (state.isBottom()) {
            return bottom;
        }

        final Optional<I> symbol = actionToSymbol.toSymbol(action);
        if (symbol.isEmpty()) {
            return isAccepting(state) ? List.of(state) : bottom;
        }
        if (state.isTop() || !alphabet.contains(symbol.get())) {
            return top;
        }

        final S succ = dfa.getTransition(state.getState(), symbol.get());
        if (succ == null || !dfa.isAccepting(succ)) {
            return bottom;
        }
        return List.of(DfaState.of(succ));
    }

    private boolean isAccepting(final DfaState<S> state) {
        return state.isTop() || dfa.isAccepting(state.getState());
    }
}
