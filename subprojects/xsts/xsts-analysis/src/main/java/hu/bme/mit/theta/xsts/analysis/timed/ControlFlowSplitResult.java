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

package hu.bme.mit.theta.xsts.analysis.timed;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.PathUtils;

import java.util.Collection;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.And;

public class ControlFlowSplitResult {

    private final Stmt flaggedStmt;
    private final Collection<VarDecl<BoolType>> flags;
    private final Expr<BoolType> indexedFlagConstraintExpr;


    private ControlFlowSplitResult(final Stmt flaggedStmt, final Collection<VarDecl<BoolType>> flags,
                                   final Collection<Expr<BoolType>> flagConstraints) {

        this.flaggedStmt = flaggedStmt;
        this.flags = flags;
        this.indexedFlagConstraintExpr = PathUtils.unfold(And(flagConstraints), 0);
    }

    public static ControlFlowSplitResult of(final Stmt flaggedStmt, final Collection<VarDecl<BoolType>> flags,
                                            final Collection<Expr<BoolType>> flagConstraints) {
        return new ControlFlowSplitResult(flaggedStmt, flags, flagConstraints);
    }

    public Stmt getFlaggedStmt() {
        return flaggedStmt;
    }

    public Collection<VarDecl<BoolType>> getFlags() {
        return flags;
    }

    public Expr<BoolType> getIndexedFlagConstraintExpr() {
        return indexedFlagConstraintExpr;
    }
}
