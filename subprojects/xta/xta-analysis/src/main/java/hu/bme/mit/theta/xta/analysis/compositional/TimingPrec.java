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

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.clock.constr.AndConstr;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstrs;
import hu.bme.mit.theta.core.clock.op.ClockOp;
import hu.bme.mit.theta.core.clock.op.ClockOps;
import hu.bme.mit.theta.core.clock.op.CopyOp;
import hu.bme.mit.theta.core.clock.op.GuardOp;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class TimingPrec implements Prec {

    private final Set<VarDecl<ClockType>> clocks;
    private final Set<XtaProcess> processes;
    private final Optional<Set<AtomicConstr>> clockConstraints;

    private TimingPrec(
            final Set<VarDecl<ClockType>> clocks,
            final Set<XtaProcess> processes,
            final Optional<Set<AtomicConstr>> clockConstraints) {
        this.clocks = Set.copyOf(checkNotNull(clocks));
        this.processes = Set.copyOf(checkNotNull(processes));
        this.clockConstraints = checkNotNull(clockConstraints).map(Set::copyOf);
    }

    public static TimingPrec of(final Set<VarDecl<ClockType>> clocks, final Set<XtaProcess> processes) {
        return new TimingPrec(clocks, processes, Optional.empty());
    }

    public static TimingPrec ofClockConstraints(
            final Set<AtomicConstr> clockConstraints, final Set<XtaProcess> processes) {
        final Set<VarDecl<ClockType>> clocks = new LinkedHashSet<>();
        for (final AtomicConstr constraint : clockConstraints) {
            clocks.addAll(constraint.getVars());
        }
        return new TimingPrec(clocks, processes, Optional.of(clockConstraints));
    }

    public static TimingPrec all(final XtaSystem system) {
        return of(new LinkedHashSet<>(system.getClockVars()), new LinkedHashSet<>(system.getProcesses()));
    }

    public static TimingPrec empty() {
        return of(Set.of(), Set.of());
    }

    public static TimingPrec emptyClockConstraints() {
        return ofClockConstraints(Set.of(), Set.of());
    }

    public Set<VarDecl<ClockType>> getClocks() {
        return clocks;
    }

    public Set<XtaProcess> getProcesses() {
        return processes;
    }

    public boolean tracksClockConstraints() {
        return clockConstraints.isPresent();
    }

    public Set<AtomicConstr> getClockConstraints() {
        return clockConstraints.orElse(Set.of());
    }

    public boolean tracks(final XtaProcess process) {
        return processes.contains(process);
    }

    public boolean tracks(final AtomicConstr constraint) {
        return clockConstraints.map(set -> set.contains(constraint))
                .orElseGet(() -> clocks.containsAll(constraint.getVars()));
    }

    /** The clock constraints (conjuncts) of a guard or an invariant. */
    public static List<AtomicConstr> clockConstraintsOf(final ClockConstr constr) {
        if (constr instanceof AtomicConstr atomic) {
            return List.of(atomic);
        }
        if (constr instanceof AndConstr and) {
            return List.copyOf(and.getConstrs());
        }
        return List.of();
    }

    /** The part of the constraint that is tracked, if any. */
    public Optional<ClockConstr> restrict(final ClockConstr constr) {
        if (clockConstraints.isEmpty() && clocks.containsAll(constr.getVars())) {
            return Optional.of(constr);
        }
        final List<AtomicConstr> parts = clockConstraintsOf(constr);
        if (parts.isEmpty()) {
            return clocks.containsAll(constr.getVars()) ? Optional.of(constr) : Optional.empty();
        }
        final List<AtomicConstr> kept = new ArrayList<>();
        for (final AtomicConstr part : parts) {
            if (tracks(part)) {
                kept.add(part);
            }
        }
        if (kept.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(kept.size() == 1 ? kept.get(0) : ClockConstrs.And(kept));
    }

    /** The part of the operation that is tracked, if any; a copy from an untracked clock frees the target. */
    public Optional<ClockOp> restrict(final ClockOp op) {
        if (clockConstraints.isPresent() && op instanceof GuardOp guard) {
            return restrict(guard.getConstr()).map(ClockOps::Guard);
        }
        if (clocks.containsAll(op.getVars())) {
            return Optional.of(op);
        }
        if (op instanceof CopyOp copy && clocks.contains(copy.getVar())) {
            return Optional.of(ClockOps.Free(copy.getVar()));
        }
        if (op instanceof GuardOp guard) {
            return restrict(guard.getConstr()).map(ClockOps::Guard);
        }
        return Optional.empty();
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
        return obj instanceof TimingPrec that
                && clocks.equals(that.clocks)
                && processes.equals(that.processes)
                && clockConstraints.equals(that.clockConstraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clocks, processes, clockConstraints);
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder(getClass().getSimpleName())
                .add(clocks.stream().map(VarDecl::getName).sorted().toList())
                .add(processes.stream().map(XtaProcess::getName).sorted().toList())
                .add(clockConstraints.map(set -> set.stream().map(Object::toString).sorted().toList()).orElse(List.of()))
                .toString();
    }
}
