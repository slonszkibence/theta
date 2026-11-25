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
package hu.bme.mit.theta.analysis.zone;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.core.clock.constr.AndConstr;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstrs;
import hu.bme.mit.theta.core.clock.constr.UnitConstr;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.clocktype.ClockConstraintExpr;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.utils.ExprUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClockBounds {

    private final Map<VarDecl<ClockType>, Integer> ceilings;
    private final BoundFunc luBounds;
    private final boolean diagonalFree;

    private ClockBounds(
            final Map<VarDecl<ClockType>, Integer> ceilings, final BoundFunc luBounds, final boolean diagonalFree) {
        this.ceilings = ceilings;
        this.luBounds = luBounds;
        this.diagonalFree = diagonalFree;
    }

    public static ClockBounds of(
            final Collection<? extends VarDecl<ClockType>> clocks, final Collection<? extends ClockConstr> constrs) {
        checkNotNull(clocks);
        checkNotNull(constrs);
        final Map<VarDecl<ClockType>, Integer> ceilings = new LinkedHashMap<>();
        for (final VarDecl<ClockType> clock : clocks) {
            ceilings.put(clock, 0);
        }
        boolean diagonalFree = true;
        for (final ClockConstr constr : constrs) {
            updateCeilings(ceilings, constr);
            diagonalFree &= isDiagonalFree(constr);
        }

        final BoundFunc.Builder luBounds = BoundFunc.builder();
        if (diagonalFree) {
            for (final ClockConstr constr : constrs) {
                luBounds.add(constr);
            }
        }
        return new ClockBounds(ceilings, luBounds.build(), diagonalFree);
    }

    public static ClockBounds fromStmts(
            final Collection<? extends VarDecl<ClockType>> clocks, final Collection<? extends Stmt> stmts) {
        final List<ClockConstr> constrs = new ArrayList<>();
        collectConstrs(stmts, constrs);
        return of(clocks, constrs);
    }

    public boolean isDiagonalFree() {
        return diagonalFree;
    }

    public ZoneState normalize(final ZoneState zone) {
        checkNotNull(zone);
        if (!diagonalFree || zone.isBottom()) {
            return zone;
        }
        return zone.transform().norm(ceilings).build();
    }

    public boolean isLeq(final ZoneState zone, final ZoneState that) {
        checkNotNull(zone);
        checkNotNull(that);
        return diagonalFree ? zone.isLeq(that, luBounds) : zone.isLeq(that);
    }

    private static void collectConstrs(final Collection<? extends Stmt> stmts, final List<ClockConstr> constrs) {
        for (final Stmt stmt : stmts) {
            if (stmt instanceof AssumeStmt assume) {
                for (final Expr<BoolType> conjunct : ExprUtils.getConjuncts(assume.getCond())) {
                    if (conjunct instanceof ClockConstraintExpr clockExpr) {
                        constrs.add(ClockConstrs.fromClockExpr(clockExpr));
                    }
                }
            } else if (stmt instanceof SequenceStmt sequence) {
                collectConstrs(sequence.getStmts(), constrs);
            }
        }
    }

    private static void updateCeilings(final Map<VarDecl<ClockType>, Integer> ceilings, final ClockConstr constr) {
        if (constr instanceof AtomicConstr atomic) {
            for (final VarDecl<ClockType> clock : atomic.getVars()) {
                ceilings.merge(clock, Math.abs(atomic.getBound()), Math::max);
            }
        } else if (constr instanceof AndConstr and) {
            for (final ClockConstr conjunct : and.getConstrs()) {
                updateCeilings(ceilings, conjunct);
            }
        }
    }

    private static boolean isDiagonalFree(final ClockConstr constr) {
        if (constr instanceof AndConstr and) {
            return and.getConstrs().stream().allMatch(ClockBounds::isDiagonalFree);
        }
        return !(constr instanceof AtomicConstr) || constr instanceof UnitConstr;
    }
}
