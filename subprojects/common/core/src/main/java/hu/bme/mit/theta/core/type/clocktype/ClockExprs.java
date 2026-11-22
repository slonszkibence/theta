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

public final class ClockExprs {

    private ClockExprs() {
    }

    public static ClockType Clock() {
        return ClockType.getInstance();
    }

    public static ClockLtExpr Lt(final Expr<ClockType> clock, final Expr<IntType> value) {
        return ClockLtExpr.of(clock, value);
    }

    public static ClockLeqExpr Leq(final Expr<ClockType> clock, final Expr<IntType> value) {
        return ClockLeqExpr.of(clock, value);
    }

    public static ClockGtExpr Gt(final Expr<ClockType> clock, final Expr<IntType> value) {
        return ClockGtExpr.of(clock, value);
    }

    public static ClockGeqExpr Geq(final Expr<ClockType> clock, final Expr<IntType> value) {
        return ClockGeqExpr.of(clock, value);
    }

    public static ClockEqExpr Eq(final Expr<ClockType> clock, final Expr<IntType> value) {
        return ClockEqExpr.of(clock, value);
    }

    public static ClockDiffExpr Diff(final Expr<ClockType> leftClock, final Expr<ClockType> rightClock) {
        return ClockDiffExpr.of(leftClock, rightClock);
    }
}
