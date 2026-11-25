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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import hu.bme.mit.theta.analysis.algorithm.compositional.realizability.CexToPrec;
import hu.bme.mit.theta.analysis.zone.ZoneInitFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.analysis.zone.ZoneStmtTransFunc;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.ResetStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Refines the tracked clocks with a counterexample that is realizable over the tracked clocks but
 * not over all clocks: adds the clocks of the counterexample, then greedily drops each new clock
 * whose removal makes the counterexample realizable again.
 */
public final class ClockPrecRefiner implements CexToPrec<ZonePrec, XstsAction> {

    private final Map<VarDecl<?>, VarDecl<ClockType>> clocks;
    private final TimedXstsActionProjections projections;

    private ClockPrecRefiner(final Collection<? extends VarDecl<ClockType>> clocks) {
        this.clocks = new LinkedHashMap<>();
        for (final VarDecl<ClockType> clock : checkNotNull(clocks)) {
            this.clocks.put(clock, clock);
        }
        this.projections = TimedXstsActionProjections.create();
    }

    public static ClockPrecRefiner create(final Collection<? extends VarDecl<ClockType>> clocks) {
        return new ClockPrecRefiner(clocks);
    }

    @Override
    public ZonePrec toPrec(final ZonePrec prec, final List<XstsAction> cexActions) {
        checkNotNull(prec);
        final List<XstsAction> word = clockWord(cexActions);
        final Set<VarDecl<ClockType>> tracked = prec.getVars();
        final Set<VarDecl<ClockType>> refined = new LinkedHashSet<>(tracked);
        refined.addAll(clocksOf(word));
        checkState(!feasible(word, refined), "The counterexample is realizable, so it needs no clock refinement");

        for (final VarDecl<ClockType> clock : List.copyOf(refined)) {
            if (tracked.contains(clock)) {
                continue;
            }
            refined.remove(clock);
            if (feasible(word, refined)) {
                refined.add(clock);
            }
        }

        checkState(!tracked.containsAll(refined),
                "The counterexample is infeasible over the tracked clocks %s, so it needs no clock refinement", tracked);
        return ZonePrec.of(refined);
    }

    private List<XstsAction> clockWord(final List<XstsAction> actions) {
        final List<XstsAction> word = new ArrayList<>();
        for (final XstsAction action : actions) {
            final XstsAction clockAction = projections.clockProjection(action);
            if (!clockAction.getStmts().isEmpty()) {
                word.add(clockAction);
            }
        }
        return word;
    }

    private Set<VarDecl<ClockType>> clocksOf(final List<XstsAction> word) {
        final Set<VarDecl<ClockType>> result = new LinkedHashSet<>();
        for (final XstsAction action : word) {
            for (final Stmt stmt : action.getStmts()) {
                if (stmt instanceof ResetStmt reset) {
                    result.add(reset.getClockDecl());
                } else if (stmt instanceof AssumeStmt assume) {
                    for (final VarDecl<?> var : ExprUtils.getVars(assume.getCond())) {
                        final VarDecl<ClockType> clock = clocks.get(var);
                        if (clock != null) {
                            result.add(clock);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static boolean feasible(final List<XstsAction> word, final Set<VarDecl<ClockType>> clocks) {
        final TimedXstsActionProjections clockProjections = TimedXstsActionProjections.create(clocks);
        final ZonePrec prec = ZonePrec.of(clocks);
        ZoneState zone = ZoneInitFunc.getInstance().getInitStates(prec).iterator().next();
        for (final XstsAction action : word) {
            zone = ZoneStmtTransFunc.getInstance()
                    .getSuccStates(zone, clockProjections.clockProjection(action), prec)
                    .iterator()
                    .next();
            if (zone.isBottom()) {
                return false;
            }
        }
        return true;
    }
}
