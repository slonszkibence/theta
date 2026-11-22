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

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.zone.ZoneState;

public class ZoneDfaState<S> implements State {
    private final ZoneState zoneState;
    private final S dfaState;

    private ZoneDfaState(final ZoneState zoneState, final S dfaState) {
        this.zoneState = zoneState;
        this.dfaState = dfaState;
    }


    public static<S> ZoneDfaState<S> create(ZoneState zoneState, S dfaState) {
        return new ZoneDfaState<>(zoneState, dfaState);
    }

    public ZoneState getZoneState() {
        return zoneState;
    }

    public S getDfaState() {
        return dfaState;
    }

    @Override
    public boolean isBottom() {
        return zoneState.isBottom();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZoneDfaState<?> that = (ZoneDfaState<?>) obj;
        return zoneState.equals(that.zoneState) && dfaState.equals(that.dfaState);
    }

    @Override
    public int hashCode() {
        int result = zoneState.hashCode();
        result = 31 * result + dfaState.hashCode();
        return result;
    }
}