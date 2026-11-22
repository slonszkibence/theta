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

package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

public class ZoneDfaLts<S> implements LTS<ZoneDfaState<S>, LearnLibAction<String>> {
    private final DFA<S, String> hypothesis;
    private final Collection<? extends String> alphabet;

    private ZoneDfaLts(DFA<S, String> hypothesis, Collection<? extends String> alphabet) {
        this.hypothesis = hypothesis;
        this.alphabet = alphabet;
    }

    public static<S> ZoneDfaLts<S> create(DFA<S, String> hypothesis, Collection<? extends String> alphabet) {
        return new ZoneDfaLts<>(hypothesis, alphabet);
    }

    @Override
    public Collection<LearnLibAction<String>> getEnabledActionsFor(ZoneDfaState<S> state) {
        Collection<LearnLibAction<String>> enabledActions = new ArrayList<>();
        S currentDfaState = state.getDfaState();

        for (String symbol : alphabet) {
            S nextDfaState = hypothesis.getTransition(currentDfaState, symbol);

            if (nextDfaState != null) {
                enabledActions.add(LearnLibAction.create(symbol));
            }
        }
        return  enabledActions;
    }
}
