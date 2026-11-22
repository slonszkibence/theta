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

package hu.bme.mit.theta.xsts.learning.inclusion;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.xsts.learning.common.LearnLibAction;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

public class ZoneDfaLts<S, I> implements LTS<ZoneDfaState<S>, LearnLibAction<I>> {
    private final DFA<S, I> hypothesis;
    private final Collection<? extends I> alphabet;

    private ZoneDfaLts(DFA<S, I> hypothesis, Collection<? extends I> alphabet) {
        this.hypothesis = hypothesis;
        this.alphabet = alphabet;
    }

    public static<S, I> ZoneDfaLts<S, I> create(DFA<S, I> hypothesis, Collection<? extends I> alphabet) {
        return new ZoneDfaLts<>(hypothesis, alphabet);
    }

    @Override
    public Collection<LearnLibAction<I>> getEnabledActionsFor(ZoneDfaState<S> state) {
        Collection<LearnLibAction<I>> enabledActions = new ArrayList<>();
        S currentDfaState = state.getDfaState();

        for (I symbol : alphabet) {
            S nextDfaState = hypothesis.getTransition(currentDfaState, symbol);

            if (nextDfaState != null) {
                enabledActions.add(LearnLibAction.create(symbol));
            }
        }
        return  enabledActions;
    }
}