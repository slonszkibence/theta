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

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.xta.XtaProcess.Loc;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ZoneDfaState<S> implements State {
    private final TimedStepper.Config config;
    private final S dfaState;

    private ZoneDfaState(final TimedStepper.Config config, final S dfaState) {
        this.config = checkNotNull(config);
        this.dfaState = checkNotNull(dfaState);
    }

    public static <S> ZoneDfaState<S> create(final TimedStepper.Config config, final S dfaState) {
        return new ZoneDfaState<>(config, dfaState);
    }

    public TimedStepper.Config getConfig() {
        return config;
    }

    public ZoneState getZoneState() {
        return config.zone();
    }

    public List<Loc> getLocs() {
        return config.locs();
    }

    public S getDfaState() {
        return dfaState;
    }

    @Override
    public boolean isBottom() {
        return config.zone().isBottom();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ZoneDfaState<?>)) {
            return false;
        }
        final ZoneDfaState<?> that = (ZoneDfaState<?>) obj;
        return config.equals(that.config) && dfaState.equals(that.dfaState);
    }

    @Override
    public int hashCode() {
        return 31 * config.hashCode() + dfaState.hashCode();
    }

    @Override
    public String toString() {
        return "ZoneDfaState(" + config + ", " + dfaState + ")";
    }
}
