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

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.unit.UnitPrec;

import java.util.Collection;
import java.util.List;

final class DfaInitFunc<S> implements InitFunc<DfaState<S>, UnitPrec> {

    private final Collection<DfaState<S>> initStates;

    private DfaInitFunc(final DfaState<S> initState) {
        this.initStates = List.of(checkNotNull(initState));
    }

    public static <S> DfaInitFunc<S> create(final DfaState<S> initState) {
        return new DfaInitFunc<>(initState);
    }

    @Override
    public Collection<DfaState<S>> getInitStates(final UnitPrec prec) {
        checkNotNull(prec);
        return initStates;
    }
}
