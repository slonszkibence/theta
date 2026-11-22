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

import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.UnitLeqConstr;
import hu.bme.mit.theta.core.clock.constr.UnitLtConstr;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaProcess.Edge;
import hu.bme.mit.theta.xta.XtaProcess.Loc;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

public final class TimingPrecRefiner {

    private TimingPrecRefiner() {
    }

    public static TimingPrec refine(final XtaSystem system, final List<XtaAction> actions,
                                    final TimingPrec tracked) {
        final Set<XtaProcess> processes = new LinkedHashSet<>(system.getProcesses());
        final Set<VarDecl<ClockType>> clocks = new LinkedHashSet<>(system.getClockVars());
        checkState(!feasible(system, actions, TimingPrec.of(clocks, processes)),
                "the counterexample is realizable, so it needs no timing refinement");

        for (final XtaProcess process : List.copyOf(processes)) {
            if (tracked.processes().contains(process)) {
                continue;
            }
            processes.remove(process);
            if (feasible(system, actions, TimingPrec.of(clocks, processes))) {
                processes.add(process);
            }
        }
        for (final VarDecl<ClockType> clock : List.copyOf(clocks)) {
            if (tracked.clocks().contains(clock)) {
                continue;
            }
            clocks.remove(clock);
            if (feasible(system, actions, TimingPrec.of(clocks, processes))) {
                clocks.add(clock);
            }
        }

        final TimingPrec refined = TimingPrec.of(clocks, processes);
        checkState(!refined.equals(tracked),
                "the counterexample is already infeasible under the tracked timing");
        return refined;
    }

    /**
     * Like {@link #refine}, but the new precision consists of atomic clock constraints: among the
     * atoms occurring along the counterexample (clock guards of its edges, invariants of the visited
     * locations), keeps a deletion-minimal set that still makes it infeasible. With
     * {@code batchInvariants}, a newly added upper bound on a clock brings every location invariant
     * over that clock along, which saves the refinement rounds that would add them one by one.
     */
    public static TimingPrec refineAtoms(final XtaSystem system, final List<XtaAction> actions,
                                         final TimingPrec tracked, final boolean batchInvariants) {
        final Set<XtaProcess> processes = new LinkedHashSet<>(system.getProcesses());
        final Set<AtomicConstr> atoms = new LinkedHashSet<>(tracked.atoms());
        atoms.addAll(atomsAlong(system, actions));
        checkState(!feasible(system, actions, TimingPrec.ofAtoms(atoms, processes)),
                "the counterexample is realizable, so it needs no timing refinement");

        for (final XtaProcess process : List.copyOf(processes)) {
            if (tracked.processes().contains(process)) {
                continue;
            }
            processes.remove(process);
            if (feasible(system, actions, TimingPrec.ofAtoms(atoms, processes))) {
                processes.add(process);
            }
        }
        for (final AtomicConstr atom : List.copyOf(atoms)) {
            if (tracked.atoms().contains(atom)) {
                continue;
            }
            atoms.remove(atom);
            if (feasible(system, actions, TimingPrec.ofAtoms(atoms, processes))) {
                atoms.add(atom);
            }
        }

        if (batchInvariants) {
            addInvariantsOfBoundedClocks(system, atoms, tracked);
        }

        final TimingPrec refined = TimingPrec.ofAtoms(atoms, processes);
        checkState(!refined.equals(tracked),
                "the counterexample is already infeasible under the tracked timing");
        return refined;
    }

    private static void addInvariantsOfBoundedClocks(final XtaSystem system,
                                                     final Set<AtomicConstr> atoms,
                                                     final TimingPrec tracked) {
        final Set<AtomicConstr> upperBounds = new LinkedHashSet<>();
        for (final AtomicConstr atom : atoms) {
            if (!tracked.atoms().contains(atom)
                    && (atom instanceof UnitLeqConstr || atom instanceof UnitLtConstr)) {
                upperBounds.add(atom);
            }
        }
        if (upperBounds.isEmpty()) {
            return;
        }
        final Set<AtomicConstr> invariantAtoms = new LinkedHashSet<>();
        for (final XtaProcess process : system.getProcesses()) {
            for (final Loc loc : process.getLocs()) {
                addInvariantAtoms(invariantAtoms, loc);
            }
        }
        for (final AtomicConstr bound : upperBounds) {
            for (final AtomicConstr invariant : invariantAtoms) {
                if (invariant.getVars().equals(bound.getVars())) {
                    atoms.add(invariant);
                }
            }
        }
    }

    private static Set<AtomicConstr> atomsAlong(final XtaSystem system,
                                                final List<XtaAction> actions) {
        final Set<AtomicConstr> result = new LinkedHashSet<>();
        for (final XtaProcess process : system.getProcesses()) {
            addInvariantAtoms(result, process.getInitLoc());
        }
        for (final XtaAction action : actions) {
            for (final Loc loc : action.getSourceLocs()) {
                addInvariantAtoms(result, loc);
            }
            for (final Edge edge : TimedStepper.edgesOf(action)) {
                for (final Guard guard : edge.getGuards()) {
                    if (guard.isClockGuard()) {
                        result.addAll(TimingPrec.atomsOf(guard.asClockGuard().getClockConstr()));
                    }
                }
            }
            for (final Loc loc : action.getTargetLocs()) {
                addInvariantAtoms(result, loc);
            }
        }
        return result;
    }

    private static void addInvariantAtoms(final Set<AtomicConstr> result, final Loc loc) {
        for (final Guard invariant : loc.getInvars()) {
            if (invariant.isClockGuard()) {
                result.addAll(TimingPrec.atomsOf(invariant.asClockGuard().getClockConstr()));
            }
        }
    }

    public static boolean feasible(final XtaSystem system, final List<XtaAction> actions,
                                   final TimingPrec prec) {
        final TimedStepper stepper = TimedStepper.of(system, prec);
        TimedStepper.Config config = stepper.initial();
        if (config.zone().isBottom()) {
            return false;
        }
        for (final XtaAction action : actions) {
            config = stepper.step(config, stepper.register(action));
            if (config == null) {
                return false;
            }
        }
        return true;
    }
}
