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

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.BinaryExpr;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.anytype.RefExpr;
import hu.bme.mit.theta.core.utils.ExprUtils;

import static com.google.common.base.Preconditions.checkArgument;

public final class ClockDiffExpr extends BinaryExpr<ClockType, ClockType> {

    private static final int HASH_SEED = 471867;
    private static final String OPERATOR_LABEL = "-";

    protected ClockDiffExpr(Expr<ClockType> leftOp, Expr<ClockType> rightOp) {
        super(leftOp, rightOp);
    }

    public static ClockDiffExpr of(Expr<ClockType> leftOp, Expr<ClockType> rightOp) {
        checkArgument(leftOp instanceof RefExpr);
        checkArgument(rightOp instanceof RefExpr);
        return new ClockDiffExpr(leftOp, rightOp);
    }

    @Override
    public BinaryExpr<ClockType, ClockType> with(Expr<ClockType> leftOp, Expr<ClockType> rightOp) {
        if (leftOp == getLeftOp() && rightOp == getRightOp()) {
            return this;
        } else {
            return ClockDiffExpr.of(leftOp, rightOp);
        }
    }

    @Override
    public BinaryExpr<ClockType, ClockType> withLeftOp(Expr<ClockType> leftOp) {
        return with(leftOp, getRightOp());
    }

    @Override
    public BinaryExpr<ClockType, ClockType> withRightOp(Expr<ClockType> rightOp) {
        return with(getLeftOp(), rightOp);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ClockDiffExpr) {
            final ClockDiffExpr that = (ClockDiffExpr) obj;
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

    @Override
    public ClockType getType() {
        return ClockType.getInstance();
    }

    @Override
    public LitExpr<ClockType> eval(Valuation val) {
        throw new UnsupportedOperationException();
    }
}
