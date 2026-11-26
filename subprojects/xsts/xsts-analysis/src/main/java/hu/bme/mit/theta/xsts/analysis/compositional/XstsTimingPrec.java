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

package hu.bme.mit.theta.xsts.analysis.compositional;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public final class XstsTimingPrec implements Prec {
    private final Set<VarDecl<ClockType>> clocks;
    private final Optional<Set<AtomicConstr>> clockConstraints;

    private XstsTimingPrec(Set<VarDecl<ClockType>> clocks, Optional<Set<AtomicConstr>> clockConstraints) {
        this.clocks = Collections.unmodifiableSet(new LinkedHashSet<>(checkNotNull(clocks)));
        this.clockConstraints = checkNotNull(clockConstraints).map(set -> Collections.unmodifiableSet(new LinkedHashSet<>(set)));
    }

    public static XstsTimingPrec of(Set<VarDecl<ClockType>> clocks) {
        return new XstsTimingPrec(clocks, Optional.empty());
    }
    public static XstsTimingPrec ofClockConstraints(Set<AtomicConstr> clockConstraints) {
        final Set<VarDecl<ClockType>> clocks = new LinkedHashSet<>();
        for (final AtomicConstr constraint : clockConstraints) {
            clocks.addAll(constraint.getVars());
        }
        return new XstsTimingPrec(clocks, Optional.of(clockConstraints));
    }

    public static XstsTimingPrec emptyClockConstraints() {
        return ofClockConstraints(Set.of());
    }
    public Set<VarDecl<ClockType>> getClocks() {
        return clocks;
    }

    public boolean tracksClockConstraints() {
        return clockConstraints.isPresent();
    }

    public Set<AtomicConstr> getClockConstraints() {
        return clockConstraints.orElse(Set.of());
    }

    public boolean tracks(AtomicConstr constraint) {
        return clockConstraints.map(set -> set.contains(constraint))
                .orElseGet(() -> clocks.containsAll(constraint.getVars()));
    }

    public ZonePrec toZonePrec() {
        return ZonePrec.of(clocks);
    }

    public TimedXstsActionProjections toProjections() {
        return clockConstraints
                .map(TimedXstsActionProjections::createForClockConstraintsTracking)
                .orElseGet(() -> TimedXstsActionProjections.createForClockTracking(clocks));
    }

    @Override
    public Collection<VarDecl<?>> getUsedVars() {
        return List.copyOf(clocks);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof XstsTimingPrec that
                && clocks.equals(that.clocks)
                && clockConstraints.equals(that.clockConstraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clocks, clockConstraints);
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder(getClass().getSimpleName())
                .add(clocks.stream().map(VarDecl::getName).sorted().toList())
                .add(clockConstraints.map(set -> set.stream().map(Object::toString).sorted().toList()).orElse(List.of()))
                .toString();
    }
}
