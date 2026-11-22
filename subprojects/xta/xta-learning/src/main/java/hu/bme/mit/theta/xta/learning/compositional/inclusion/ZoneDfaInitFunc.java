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

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ZoneDfaInitFunc<S, P extends Prec> implements InitFunc<ZoneDfaState<S>, P> {
    private final DFA<S, String> hypothesis;
    private final TimedStepper stepper;

    private ZoneDfaInitFunc(final DFA<S, String> hypothesis, final TimedStepper stepper) {
        this.hypothesis = checkNotNull(hypothesis);
        this.stepper = checkNotNull(stepper);
    }

    public static <S, P extends Prec> ZoneDfaInitFunc<S, P> create(
            final DFA<S, String> hypothesis, final TimedStepper stepper) {
        return new ZoneDfaInitFunc<>(hypothesis, stepper);
    }

    @Override
    public Collection<ZoneDfaState<S>> getInitStates(final P prec) {
        return Collections.singleton(
                ZoneDfaState.create(stepper.initial(), hypothesis.getInitialState()));
    }
}
