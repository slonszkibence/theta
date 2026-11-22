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

import hu.bme.mit.theta.analysis.PartialOrd;

public final class ZoneDfaOrd<S> implements PartialOrd<ZoneDfaState<S>> {
    private ZoneDfaOrd() {}

    public static <S> ZoneDfaOrd<S> create() {
        return new ZoneDfaOrd<>();
    }

    @Override
    public boolean isLeq(final ZoneDfaState<S> state1, final ZoneDfaState<S> state2) {
        if (state1.isBottom()) {
            return true;
        }
        return state1.equals(state2);
    }
}
