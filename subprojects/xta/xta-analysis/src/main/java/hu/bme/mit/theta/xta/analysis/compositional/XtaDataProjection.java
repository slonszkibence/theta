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
package hu.bme.mit.theta.xta.analysis.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.DelayStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.ResetStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.List;

public final class XtaDataProjection extends StmtAction {

    private final List<Stmt> stmts;

    private XtaDataProjection(final XtaAction action) {
        this.stmts = checkNotNull(action).getStmts().stream().filter(XtaDataProjection::isData).toList();
    }

    public static XtaDataProjection of(final XtaAction action) {
        return new XtaDataProjection(action);
    }

    @Override
    public List<Stmt> getStmts() {
        return stmts;
    }

    private static boolean isData(final Stmt stmt) {
        if (stmt instanceof ResetStmt || stmt instanceof DelayStmt) {
            return false;
        }
        if (stmt instanceof AssumeStmt assume) {
            return ExprUtils.getVars(assume.getCond()).stream().noneMatch(XtaDataProjection::isClock);
        }
        if (stmt instanceof AssignStmt<?> assign) {
            return !isClock(assign.getVarDecl());
        }
        if (stmt instanceof HavocStmt<?> havoc) {
            return !isClock(havoc.getVarDecl());
        }
        return true;
    }

    private static boolean isClock(final VarDecl<?> var) {
        return var.getType() instanceof ClockType;
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder(getClass().getSimpleName()).addAll(stmts).toString();
    }
}
