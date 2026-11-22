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

package hu.bme.mit.theta.core.type.clocktype;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.inttype.IntType;

public final class ClockGtExpr extends ClockConstraintExpr {

    private static final int HASH_SEED = 7219;
    private static final ClockConstraintRel REL = ClockConstraintRel.GT;
    private static final String OPERATOR_LABEL = ">";

    protected ClockGtExpr(Expr<ClockType> clock, Expr<IntType> value) {
        super(clock, value);
    }

    public static ClockGtExpr of(final Expr<ClockType> clock, final Expr<IntType> value) {
        return new ClockGtExpr(clock, value);
    }

    @Override
    public ClockGtExpr with(Expr<ClockType> clock, Expr<IntType> value) {
        if (clock == getLeftOp() && value == getRightOp()) {
            return this;
        } else {
            return ClockGtExpr.of(clock, value);
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
        } else if (obj instanceof ClockGtExpr) {
            final ClockGtExpr that = (ClockGtExpr) obj;
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
