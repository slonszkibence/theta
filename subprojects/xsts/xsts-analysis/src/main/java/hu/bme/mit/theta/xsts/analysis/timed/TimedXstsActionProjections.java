package hu.bme.mit.theta.xsts.analysis.timed;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.StmtContainsTimingVisitor;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import java.util.*;

import static hu.bme.mit.theta.core.stmt.Stmts.Assume;

public class TimedXstsActionProjections {

    private final Map<XstsAction, XstsAction> dataProjections;
    private final Map<XstsAction, XstsAction> clockProjections;
    private final List<VarDecl<ClockType>> trackedClocks;

    private TimedXstsActionProjections(final List<VarDecl<ClockType>> trackedClocks) {
        dataProjections = new HashMap<>();
        clockProjections = new HashMap<>();
        this.trackedClocks = trackedClocks;
    }

    public static TimedXstsActionProjections create() {
        return new TimedXstsActionProjections(null);
    }

    public static TimedXstsActionProjections create(Collection<VarDecl<ClockType>> trackedClocks) {
        return new TimedXstsActionProjections(List.copyOf(trackedClocks));
    }

    public XstsAction dataProjection(final XstsAction action) {
        XstsAction dataAction = dataProjections.get(action);
        if (dataAction == null) {
            createProjections(action);
            dataAction = dataProjections.get(action);
        }
        return dataAction;
    }

    public XstsAction clockProjection(final XstsAction action) {
        XstsAction clockAction = clockProjections.get(action);
        if (clockAction == null) {
            createProjections(action);
            clockAction = clockProjections.get(action);
        }
        return clockAction;
    }

    private void createProjections(final XstsAction action) {
        final List<Stmt> dataStmts = new ArrayList<>();
        final List<Stmt> clockStmts = new ArrayList<>();
        final Tuple2<List<Stmt>, List<Stmt>> projections = Tuple2.of(dataStmts, clockStmts);

        for (final Stmt stmt : action.getStmts()) {
            stmt.accept(new ProjectionVisitor(trackedClocks), projections);
        }

        dataProjections.put(action, XstsAction.create(dataStmts));
        clockProjections.put(action, XstsAction.create(normalizeClockStmts(clockStmts)));
    }

    private static List<Stmt> normalizeClockStmts(final List<Stmt> stmts) {
        final List<Stmt> result = new ArrayList<>();
        final Set<Expr<BoolType>> holding = new HashSet<>();
        final Map<VarDecl<ClockType>, Integer> resetValue = new HashMap<>();
        for (final Stmt stmt : stmts) {
            if (stmt instanceof DelayStmt) {
                if (!result.isEmpty() && result.get(result.size() - 1) instanceof DelayStmt) {
                    continue;
                }
                holding.clear();
                resetValue.clear();
            } else if (stmt instanceof ResetStmt reset) {
                final VarDecl<ClockType> clock = reset.getClockDecl();
                if (Objects.equals(resetValue.get(clock), reset.getValue())) {
                    continue;
                }
                resetValue.put(clock, reset.getValue());
                holding.removeIf(g -> ExprUtils.getVars(g).contains(clock));
            } else if (stmt instanceof AssumeStmt assume) {
                if (!holding.add(assume.getCond())) {
                    continue;
                }
            }
            result.add(stmt);
        }
        return result;
    }

    private static class ProjectionVisitor implements StmtVisitor<Tuple2<List<Stmt>, List<Stmt>>, Void> {
        private final List<VarDecl<ClockType>> trackedClocks;

        public ProjectionVisitor(final List<VarDecl<ClockType>> trackedClocks) {
            this.trackedClocks = trackedClocks;
        }

        private boolean tracks(final Collection<VarDecl<?>> clocks) {
            if (trackedClocks == null) {
                return true;
            }
            return trackedClocks.containsAll(clocks);
        }

        @Override
        public Void visit(SkipStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            return null;
        }

        @Override
        public Void visit(AssumeStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            for (final Expr<BoolType> cond : ExprUtils.getConjuncts(stmt.getCond())) {
                final Collection<VarDecl<?>> vars = ExprUtils.getVars(cond);
                final long varsCnt = vars.size();
                final long clocksCnt = vars.stream().filter(v -> v.getType() instanceof ClockType).count();
                if (clocksCnt == 0) {
                    addDataStmt(Assume(cond), projections);
                } else if (clocksCnt == varsCnt) {
                    if (tracks(vars)) {
                        addClockStmt(Assume(cond), projections);
                    }
                } else {
                    throw new AssertionError();
                }
            }
            return null;
        }

        @Override
        public <DeclType extends Type> Void visit(AssignStmt<DeclType> stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            shouldNotContainClock(stmt);
            addDataStmt(stmt, projections);
            return null;
        }

        @Override
        public <DeclType extends Type> Void visit(HavocStmt<DeclType> stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            shouldNotContainClock(stmt);
            addDataStmt(stmt, projections);
            return null;
        }

        @Override
        public Void visit(SequenceStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            stmt.getStmts().forEach(subStmt -> subStmt.accept(this, projections));
            return null;
        }

        @Override
        public Void visit(NonDetStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            if (stmt.getStmts().size() > 1) {
                shouldNotContainClock(stmt);
            }
            addDataStmt(stmt, projections);
            return null;
        }

        @Override
        public Void visit(OrtStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            shouldNotContainClock(stmt);
            addDataStmt(stmt, projections);
            return null;
        }

        @Override
        public Void visit(LoopStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            shouldNotContainClock(stmt);
            addDataStmt(stmt, projections);
            return null;
        }

        @Override
        public Void visit(IfStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            shouldNotContainClock(stmt);
            addDataStmt(stmt, projections);
            return null;
        }

        @Override
        public Void visit(DelayStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            if (trackedClocks == null || !trackedClocks.isEmpty()) {
                addClockStmt(stmt, projections);
            }
            return null;
        }

        @Override
        public Void visit(ResetStmt stmt, Tuple2<List<Stmt>, List<Stmt>> projections) {
            if (tracks(List.of(stmt.getClockDecl()))) {
                addClockStmt(stmt, projections);
            }
            return null;
        }
        
        private void addDataStmt(final Stmt dataStmt, final Tuple2<List<Stmt>, List<Stmt>> projections) {
            projections.get1().add(dataStmt);
        }
        
        private void addClockStmt(final Stmt clockStmt, final Tuple2<List<Stmt>, List<Stmt>> projections) {
            projections.get2().add(clockStmt);
        }

        private static void shouldNotContainClock(final Stmt stmt) {
            if (stmt.accept(StmtContainsTimingVisitor.getInstance(), null)) {
                throw new AssertionError();
            }
        }
    }
}
