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

package hu.bme.mit.theta.xcfa.analysis.impl.lazy;

import com.google.common.collect.Lists;
import hu.bme.mit.theta.analysis.algorithm.lazy.expr.ExprActionPre;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.WpState;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaAction;

public class XcfaExprActionPre implements ExprActionPre<XcfaAction> {
    private XcfaExprActionPre() {}

    public static XcfaExprActionPre create() {
        return new XcfaExprActionPre();
    }

    @Override
    public Expr<BoolType> pre(Expr<BoolType> state, XcfaAction action) {
        WpState wp = WpState.of(state);
        for(final Stmt stmt : Lists.reverse(action.getStmts())) {
            wp = wp.wep(stmt);
        }
        return wp.getExpr();
    }
}
