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
import static com.google.common.base.Preconditions.checkState;

import hu.bme.mit.theta.analysis.algorithm.compositional.realizability.CexToPrec;
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
import java.util.Optional;
import java.util.Set;

public final class XtaTimingPrecRefiner implements CexToPrec<XtaTimingPrec, XtaAction> {

    private final XtaSystem system;
    private final boolean clockConstraintLevel;
    private final boolean batchInvariants;

    private XtaTimingPrecRefiner(final XtaSystem system, final boolean clockConstraintLevel, final boolean batchInvariants) {
        this.system = checkNotNull(system);
        this.clockConstraintLevel = clockConstraintLevel;
        this.batchInvariants = batchInvariants;
    }

    public static XtaTimingPrecRefiner create(final XtaSystem system) {
        return new XtaTimingPrecRefiner(system, false, false);
    }

    public static XtaTimingPrecRefiner createForClockConstraints(final XtaSystem system, final boolean batchInvariants) {
        return new XtaTimingPrecRefiner(system, true, batchInvariants);
    }

    @Override
    public XtaTimingPrec toPrec(final XtaTimingPrec tracked, final List<XtaAction> cexActions) {
        checkNotNull(tracked);
        checkNotNull(cexActions);
        final XtaTimingPrec refined = clockConstraintLevel ? refineClockConstraints(tracked, cexActions) : refineClocks(tracked, cexActions);
        checkState(!refined.equals(tracked), "The counterexample is already infeasible under the tracked timing");
        return refined;
    }

    private XtaTimingPrec refineClocks(final XtaTimingPrec tracked, final List<XtaAction> actions) {
        final Set<XtaProcess> processes = new LinkedHashSet<>(system.getProcesses());
        final Set<VarDecl<ClockType>> clocks = new LinkedHashSet<>(system.getClockVars());
        checkState(!feasible(actions, XtaTimingPrec.of(clocks, processes)),
                "The counterexample is realizable, so it needs no timing refinement");

        for (final XtaProcess process : List.copyOf(processes)) {
            if (!tracked.getProcesses().contains(process)) {
                processes.remove(process);
                if (feasible(actions, XtaTimingPrec.of(clocks, processes))) {
                    processes.add(process);
                }
            }
        }
        for (final VarDecl<ClockType> clock : List.copyOf(clocks)) {
            if (!tracked.getClocks().contains(clock)) {
                clocks.remove(clock);
                if (feasible(actions, XtaTimingPrec.of(clocks, processes))) {
                    clocks.add(clock);
                }
            }
        }
        return XtaTimingPrec.of(clocks, processes);
    }

    private XtaTimingPrec refineClockConstraints(final XtaTimingPrec tracked, final List<XtaAction> actions) {
        final Set<XtaProcess> processes = new LinkedHashSet<>(system.getProcesses());
        final Set<AtomicConstr> constraints = new LinkedHashSet<>(tracked.getClockConstraints());
        constraints.addAll(clockConstraintsAlong(actions));
        checkState(!feasible(actions, XtaTimingPrec.ofClockConstraints(constraints, processes)),
                "The counterexample is realizable, so it needs no timing refinement");

        for (final XtaProcess process : List.copyOf(processes)) {
            if (!tracked.getProcesses().contains(process)) {
                processes.remove(process);
                if (feasible(actions, XtaTimingPrec.ofClockConstraints(constraints, processes))) {
                    processes.add(process);
                }
            }
        }
        for (final AtomicConstr constraint : List.copyOf(constraints)) {
            if (!tracked.getClockConstraints().contains(constraint)) {
                constraints.remove(constraint);
                if (feasible(actions, XtaTimingPrec.ofClockConstraints(constraints, processes))) {
                    constraints.add(constraint);
                }
            }
        }
        if (batchInvariants) {
            addInvariantsOfBoundedClocks(constraints, tracked);
        }
        return XtaTimingPrec.ofClockConstraints(constraints, processes);
    }

    private void addInvariantsOfBoundedClocks(final Set<AtomicConstr> constraints, final XtaTimingPrec tracked) {
        final Set<AtomicConstr> upperBounds = new LinkedHashSet<>();
        for (final AtomicConstr constraint : constraints) {
            if (!tracked.getClockConstraints().contains(constraint) && (constraint instanceof UnitLeqConstr || constraint instanceof UnitLtConstr)) {
                upperBounds.add(constraint);
            }
        }
        if (upperBounds.isEmpty()) {
            return;
        }
        final Set<AtomicConstr> invariants = new LinkedHashSet<>();
        for (final XtaProcess process : system.getProcesses()) {
            for (final Loc loc : process.getLocs()) {
                addInvariantConstraints(invariants, loc);
            }
        }
        for (final AtomicConstr bound : upperBounds) {
            for (final AtomicConstr invariant : invariants) {
                if (invariant.getVars().equals(bound.getVars())) {
                    constraints.add(invariant);
                }
            }
        }
    }

    private Set<AtomicConstr> clockConstraintsAlong(final List<XtaAction> actions) {
        final Set<AtomicConstr> result = new LinkedHashSet<>();
        for (final XtaProcess process : system.getProcesses()) {
            addInvariantConstraints(result, process.getInitLoc());
        }
        for (final XtaAction action : actions) {
            for (final Loc loc : action.getSourceLocs()) {
                addInvariantConstraints(result, loc);
            }
            for (final Edge edge : XtaTimedStepper.edgesOf(action)) {
                for (final Guard guard : edge.getGuards()) {
                    if (guard.isClockGuard()) {
                        result.addAll(XtaTimingPrec.clockConstraintsOf(guard.asClockGuard().getClockConstr()));
                    }
                }
            }
            for (final Loc loc : action.getTargetLocs()) {
                addInvariantConstraints(result, loc);
            }
        }
        return result;
    }

    private static void addInvariantConstraints(final Set<AtomicConstr> result, final Loc loc) {
        for (final Guard invariant : loc.getInvars()) {
            if (invariant.isClockGuard()) {
                result.addAll(XtaTimingPrec.clockConstraintsOf(invariant.asClockGuard().getClockConstr()));
            }
        }
    }

    private boolean feasible(final List<XtaAction> actions, final XtaTimingPrec prec) {
        final XtaTimedStepper stepper = XtaTimedStepper.create(system, prec);
        Optional<XtaTimedStepper.Config> config = Optional.of(stepper.initial()).filter(c -> !c.isBottom());
        for (final XtaAction action : actions) {
            if (config.isEmpty()) {
                return false;
            }
            config = stepper.step(config.get(), stepper.project(action));
        }
        return config.isPresent();
    }
}
