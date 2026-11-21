package hu.bme.mit.theta.core.type.clocktype;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.inttype.IntType;

public final class ClockLeqExpr extends ClockConstraintExpr {

    private static final int HASH_SEED = 7213;
    private static final ClockConstraintRel REL = ClockConstraintRel.LEQ;
    private static final String OPERATOR_LABEL = "<=";

    protected ClockLeqExpr(Expr<ClockType> clock, Expr<IntType> value) {
        super(clock, value);
    }

    public static ClockLeqExpr of(final Expr<ClockType> clock, final Expr<IntType> value) {
        return new ClockLeqExpr(clock, value);
    }

    @Override
    public ClockLeqExpr with(Expr<ClockType> clock, Expr<IntType> value) {
        if (clock == getLeftOp() && value == getRightOp()) {
            return this;
        } else {
            return ClockLeqExpr.of(clock, value);
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
        } else if (obj instanceof ClockLeqExpr) {
            final ClockLeqExpr that = (ClockLeqExpr) obj;
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
