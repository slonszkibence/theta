package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.analysis.zone.BoundFunc;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstrs;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.clocktype.ClockConstraintExpr;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ClockBoundsCollector {

    private ClockBoundsCollector() {
    }

    public record Result(Map<VarDecl<ClockType>, Integer> ceilings, BoundFunc luBounds) {
    }

    public static Result collect(
            final Collection<VarDecl<ClockType>> clocks, final Collection<XstsAction> clockActions) {
        final Map<VarDecl<ClockType>, Integer> ceilings = new HashMap<>();
        for (final VarDecl<ClockType> clock : clocks) {
            ceilings.put(clock, 0);
        }

        final BoundFunc.Builder luBoundsBuilder = BoundFunc.builder();

        for (final XstsAction clockAction : clockActions) {
            for (final Stmt stmt : clockAction.getStmts()) {
                if (stmt instanceof AssumeStmt assume) {
                    for (final Expr<BoolType> conjunct : ExprUtils.getConjuncts(assume.getCond())) {
                        if (conjunct instanceof ClockConstraintExpr clockExpr) {
                            final ClockConstr constr = ClockConstrs.fromClockExpr(clockExpr);
                            updateCeilings(ceilings, constr);
                            addToBounds(luBoundsBuilder, constr);
                        }
                    }
                }
            }
        }

        return new Result(ceilings, luBoundsBuilder.build());
    }

    private static void updateCeilings(
            final Map<VarDecl<ClockType>, Integer> ceilings, final ClockConstr constr) {
        if (constr instanceof AtomicConstr atomicConstr) {
            for (final VarDecl<ClockType> clock : constr.getVars()) {
                ceilings.merge(clock, atomicConstr.getBound(), Math::max);
            }
        }
    }

    private static void addToBounds(final BoundFunc.Builder builder, final ClockConstr constr) {
        try {
            builder.add(constr);
        } catch (final UnsupportedOperationException e) {
            // diagonal constraints are not supported by BoundFunc either - skip
        }
    }
}
