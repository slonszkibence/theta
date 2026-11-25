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
package hu.bme.mit.theta.xta.analysis.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.op.ClockOp;
import hu.bme.mit.theta.xta.XtaProcess.Loc;

import java.util.List;
import java.util.Optional;


public final class XtaClockProjection {

    private final List<EdgeClock> edgeClocks;
    private final int hashCode;

    private XtaClockProjection(final List<EdgeClock> edgeClocks) {
        this.edgeClocks = List.copyOf(checkNotNull(edgeClocks));
        this.hashCode = this.edgeClocks.hashCode();
    }

    public static XtaClockProjection of(final List<EdgeClock> edgeClocks) {
        return new XtaClockProjection(edgeClocks);
    }

    public List<EdgeClock> getEdgeClocks() {
        return edgeClocks;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof XtaClockProjection that
                && hashCode == that.hashCode
                && edgeClocks.equals(that.edgeClocks);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder(getClass().getSimpleName()).addAll(edgeClocks).toString();
    }

    public record EdgeClock(Optional<Loc> source, Optional<Loc> target, List<ClockConstr> guards, List<ClockOp> updates) {

        public EdgeClock {
            checkNotNull(source);
            checkNotNull(target);
            guards = List.copyOf(guards);
            updates = List.copyOf(updates);
        }

        public boolean isTracked() {
            return source.isPresent();
        }

        @Override
        public String toString() {
            final String locations = source.isPresent() && target.isPresent()
                    ? String.format("%s->%s", source.get().getName(), target.get().getName())
                    : "";
            return String.format("%sg%su%s", locations, guards, updates);
        }
    }
}
