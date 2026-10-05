package hu.bme.mit.theta.core.type.clocktype;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.inttype.IntType;

public class ClockEqExpr extends ClockConstraintExpr {

    private static final int HASH_SEED = 7237;
    private static final ClockConstraintRel REL = ClockConstraintRel.EQ;
    private static final String OPERATOR_LABEL = "=";

    protected ClockEqExpr(Expr<ClockType> clock, Expr<IntType> value) {
        super(clock, value);
    }

    public static ClockEqExpr of(final Expr<ClockType> clock, final Expr<IntType> value) {
        return new ClockEqExpr(clock, value);
    }

    @Override
    public ClockEqExpr with(Expr<ClockType> clock, Expr<IntType> value) {
        if (clock == getLeftOp() && value == getRightOp()) {
            return this;
        } else {
            return ClockEqExpr.of(clock, value);
        }
    }

    @Override
    public ClockConstraintRel getRel() {
        return REL;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ClockEqExpr) {
            final ClockEqExpr that = (ClockEqExpr) obj;
            return this.getLeftOp().equals(that.getLeftOp()) && this.getRightOp().equals(that.getRightOp());
        } else {
            return false;
        }
    }

    @Override
    protected int getHashSeed() {
        return HASH_SEED;
    }

    @Override
    public String getOperatorLabel() {
        return OPERATOR_LABEL;
    }
}
