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

package hu.bme.mit.theta.core.clock.impl;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.type.rattype.RatType;

import java.util.Collection;

import static hu.bme.mit.theta.core.type.rattype.RatExprs.Add;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Rat;
import static hu.bme.mit.theta.core.type.rattype.RatExprs.Sub;

public final class RatClockImpl extends ClockImpl<RatType> {

    protected RatClockImpl(final Collection<VarDecl<ClockType>> clocks) {
        super(clocks);
    }

    public static RatClockImpl fromClocks(final Collection<VarDecl<ClockType>> clocks) {
        return new RatClockImpl(clocks);
    }

    @Override
    protected RatType type() {
        return Rat();
    }

    @Override
    protected Expr<RatType> addExpr(Expr<RatType> leftOp, Expr<RatType> rightOp) {
        return Add(leftOp, rightOp);
    }

    @Override
    protected Expr<RatType> subExpr(Expr<RatType> leftOp, Expr<RatType> rightOp) {
        return Sub(leftOp, rightOp);
    }
}
