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

package hu.bme.mit.theta.xta.learning.compositional.common;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The part of the timing the learned language is built over: the tracked process locations, and
 * either every constraint over the tracked clocks, or (atom level) only the tracked atomic
 * constraints, in which case a clock counts as tracked if some tracked atom mentions it.
 */
public final class TimingPrec {
    private final Set<VarDecl<ClockType>> clocks;
    private final Set<XtaProcess> processes;
    private final Set<AtomicConstr> atoms;

    private TimingPrec(final Set<VarDecl<ClockType>> clocks, final Set<XtaProcess> processes,
                       final Set<AtomicConstr> atoms) {
        this.clocks = Set.copyOf(checkNotNull(clocks));
        this.processes = Set.copyOf(checkNotNull(processes));
        this.atoms = atoms == null ? null : Set.copyOf(atoms);
    }

    public static TimingPrec of(final Set<VarDecl<ClockType>> clocks,
                                final Set<XtaProcess> processes) {
        return new TimingPrec(clocks, processes, null);
    }

    public static TimingPrec ofAtoms(final Set<AtomicConstr> atoms,
                                     final Set<XtaProcess> processes) {
        final Set<VarDecl<ClockType>> clocks = new LinkedHashSet<>();
        for (final AtomicConstr atom : atoms) {
            clocks.addAll(atom.getVars());
        }
        return new TimingPrec(clocks, processes, atoms);
    }

    public static TimingPrec all(final XtaSystem system) {
        return new TimingPrec(new LinkedHashSet<>(system.getClockVars()),
                new LinkedHashSet<>(system.getProcesses()), null);
    }

    public static TimingPrec empty() {
        return new TimingPrec(Set.of(), Set.of(), null);
    }

    public static TimingPrec emptyAtoms() {
        return new TimingPrec(Set.of(), Set.of(), Set.of());
    }

    public Set<VarDecl<ClockType>> clocks() {
        return clocks;
    }

    public Set<XtaProcess> processes() {
        return processes;
    }

    public boolean isAtomLevel() {
        return atoms != null;
    }

    public Set<AtomicConstr> atoms() {
        return atoms == null ? Set.of() : atoms;
    }

    public boolean tracks(final XtaProcess process) {
        return processes.contains(process);
    }

    public boolean tracks(final AtomicConstr atom) {
        return atoms == null ? clocks.containsAll(atom.getVars()) : atoms.contains(atom);
    }

    public boolean containsAll(final TimingPrec that) {
        return clocks.containsAll(that.clocks) && processes.containsAll(that.processes)
                && (atoms == null || atoms.containsAll(that.atoms()));
    }

    public static List<AtomicConstr> atomsOf(final ClockConstr constr) {
        if (constr instanceof AtomicConstr atomic) {
            return List.of(atomic);
        }
        if (constr instanceof AndConstr and) {
            return List.copyOf(and.getConstrs());
        }
        return List.of();
    }

    public Optional<ClockConstr> restrict(final ClockConstr constr) {
        if (atoms == null && clocks.containsAll(constr.getVars())) {
            return Optional.of(constr);
        }
        final List<AtomicConstr> parts = atomsOf(constr);
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

    public Optional<ClockOp> restrict(final ClockOp op) {
        if (atoms != null && op instanceof GuardOp guard) {
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
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TimingPrec that)) {
            return false;
        }
        return clocks.equals(that.clocks) && processes.equals(that.processes)
                && Objects.equals(atoms, that.atoms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clocks, processes, atoms);
    }

    @Override
    public String toString() {
        final List<String> clockNames = clocks.stream().map(VarDecl::getName).sorted().toList();
        final List<String> processNames =
                processes.stream().map(XtaProcess::getName).sorted().toList();
        final String atomPart = atoms == null ? ""
                : ", atoms=" + atoms.stream().map(Object::toString).sorted().toList();
        return "TimingPrec(clocks=" + clockNames + atomPart + ", locations of=" + processNames + ")";
    }
}
