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

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.zone.ZoneInitFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;


import net.automatalib.automaton.fsa.DFA;


import java.util.Collections;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class ZoneDfaInitFunc<S, I> implements InitFunc<ZoneDfaState<S>, ZonePrec> {
    private final DFA<S, I> hypothesis;

    private ZoneDfaInitFunc(DFA<S, I> hypothesis) {
        this.hypothesis = checkNotNull(hypothesis);
    }

    public static<S, I> ZoneDfaInitFunc<S, I> create(DFA<S, I> hypothesis) {
        return new ZoneDfaInitFunc<>(hypothesis);
    }

    @Override
    public Collection<ZoneDfaState<S>> getInitStates(ZonePrec prec) {
        S initDfaState = hypothesis.getInitialState();
        ZoneState initZone = ZoneInitFunc.getInstance().getInitStates(prec).iterator().next();
        ZoneDfaState<S> initState = ZoneDfaState.create(initZone, initDfaState);

        return Collections.singleton(initState);
    }
}