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

import hu.bme.mit.theta.analysis.PartialOrd;

final class DfaOrd<S> implements PartialOrd<DfaState<S>> {

    private DfaOrd() {}

    public static <S> DfaOrd<S> create() {
        return new DfaOrd<>();
    }

    @Override
    public boolean isLeq(final DfaState<S> state1, final DfaState<S> state2) {
        checkNotNull(state1);
        checkNotNull(state2);
        return state1.isLeq(state2);
    }
}
