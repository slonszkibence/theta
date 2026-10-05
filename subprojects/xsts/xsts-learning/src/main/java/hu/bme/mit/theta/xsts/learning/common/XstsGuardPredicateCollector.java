package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xsts.XSTS;

import hu.bme.mit.theta.core.decl.VarDecl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsGuardPredicateCollector {
    private XstsGuardPredicateCollector() {}

    public static PredPrec collect(final XSTS xsts) {
        return collect(xsts, Set.of());
    }

    public static PredPrec collect(final XSTS xsts, final Collection<VarDecl<?>> exactlyTrackedVars) {
        checkNotNull(exactlyTrackedVars);
        final Set<Expr<BoolType>> preds = new HashSet<>();
        final PredicateCollectingVisitor visitor = PredicateCollectingVisitor.create(preds);
        xsts.getInit().accept(visitor, null);
        xsts.getTran().accept(visitor, null);
        xsts.getEnv().accept(visitor, null);

        if (!exactlyTrackedVars.isEmpty()) {
            final Set<VarDecl<?>> tracked = new HashSet<>(exactlyTrackedVars);
            preds.removeIf(pred -> tracked.containsAll(ExprUtils.getVars(pred)));
        }
        return PredPrec.of(preds);
    }

     static final class PredicateCollectingVisitor implements StmtVisitor<Void, Void> {
        private final Set<Expr<BoolType>> preds;

        private PredicateCollectingVisitor(Set<Expr<BoolType>> preds) {
            this.preds = checkNotNull(preds);
        }

        public static PredicateCollectingVisitor create(Set<Expr<BoolType>> preds) {
            return new PredicateCollectingVisitor(preds);
        }

        @Override
        public Void visit(SkipStmt stmt, Void param) {
            return null;
        }

        @Override
        public Void visit(AssumeStmt stmt, Void param) {
            addNonClockAtoms(stmt.getCond());
            return null;
        }

        @Override
        public <DeclType extends Type> Void visit(AssignStmt<DeclType> stmt, Void param) {
            return null;
        }

        @Override
        public <DeclType extends Type> Void visit(HavocStmt<DeclType> stmt, Void param) {
            return null;
        }

        @Override
        public Void visit(SequenceStmt stmt, Void param) {
            stmt.getStmts().forEach(s -> s.accept(this, param));
            return null;
        }

        @Override
        public Void visit(NonDetStmt stmt, Void param) {
            stmt.getStmts().forEach(s -> s.accept(this, param));
            return null;
        }

        @Override
        public Void visit(OrtStmt stmt, Void param) {
            return null;
        }

        @Override
        public Void visit(LoopStmt stmt, Void param) {
            return null;
        }

        @Override
        public Void visit(IfStmt stmt, Void param) {
            addNonClockAtoms(stmt.getCond());
            stmt.getThen().accept(this, param);
            stmt.getElze().accept(this, param);
            return null;
        }

        @Override
        public Void visit(DelayStmt stmt, Void param) {
            return null;
        }

        @Override
        public Void visit(ResetStmt stmt, Void param) {
            return null;
        }

        private void addNonClockAtoms(final Expr<BoolType> cond) {
            for (final Expr<BoolType> atom : ExprUtils.getAtoms(cond)) {
                final boolean mentionsClock = ExprUtils.getVars(atom).stream()
                        .anyMatch(v -> v.getType() instanceof ClockType);
                if (!mentionsClock) {
                    preds.add(atom);
                }
            }
        }
    }
}
