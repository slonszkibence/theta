package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ItpRefToExplPrec;
import hu.bme.mit.theta.analysis.expl.VarsRefToExplPrec;
import hu.bme.mit.theta.analysis.expr.refinement.autoexpl.AutoExpl;
import hu.bme.mit.theta.analysis.pred.ExprSplitters;
import hu.bme.mit.theta.analysis.pred.ItpRefToPredPrec;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.AutomaticItpRefToProd2ExplPredPrec;
import hu.bme.mit.theta.analysis.expr.refinement.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.UCSolver;

public interface TraceCheckerStrategy<P extends Prec, R extends Refutation> {
    ExprTraceChecker<R> checker();

    RefutationToPrec<P, R> refToPrec();

    static <P extends Prec, R extends Refutation> TraceCheckerStrategy<P, R> of(
            ExprTraceChecker<R> checker,
            RefutationToPrec<P, R> refToPrec
    ) {
        return new TraceCheckerStrategy<>() {
            @Override public ExprTraceChecker<R> checker() { return checker; }
            @Override public RefutationToPrec<P, R> refToPrec() { return refToPrec; }
        };
    }

    static TraceCheckerStrategy<ExplPrec, ItpRefutation> seqItp(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver
    ) {
        return of(ExprTraceSeqItpChecker.create(init, target, solver), new ItpRefToExplPrec());
    }

    static TraceCheckerStrategy<ExplPrec, ItpRefutation> fwBinItp(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver
    ) {
        return of(ExprTraceFwBinItpChecker.create(init, target, solver), new ItpRefToExplPrec());
    }

    static TraceCheckerStrategy<ExplPrec, ItpRefutation> bwBinItp(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver
    ) {
        return of(ExprTraceBwBinItpChecker.create(init, target, solver), new ItpRefToExplPrec());
    }

    static TraceCheckerStrategy<ExplPrec, VarsRefutation> unsatCore(
            Expr<BoolType> init, Expr<BoolType> target, UCSolver solver
    ) {
        return of(ExprTraceUnsatCoreChecker.create(init, target, solver), new VarsRefToExplPrec());
    }

    static TraceCheckerStrategy<PredPrec, ItpRefutation> seqItpPred(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver,
            ExprSplitters.ExprSplitter splitter
    ) {
        return of(ExprTraceSeqItpChecker.create(init, target, solver),
                new ItpRefToPredPrec(splitter));
    }

    static TraceCheckerStrategy<PredPrec, ItpRefutation> fwBinItpPred(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver,
            ExprSplitters.ExprSplitter splitter
    ) {
        return of(ExprTraceFwBinItpChecker.create(init, target, solver),
                new ItpRefToPredPrec(splitter));
    }

    static TraceCheckerStrategy<PredPrec, ItpRefutation> bwBinItpPred(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver,
            ExprSplitters.ExprSplitter splitter
    ) {
        return of(ExprTraceBwBinItpChecker.create(init, target, solver),
                new ItpRefToPredPrec(splitter));
    }

    static TraceCheckerStrategy<Prod2Prec<ExplPrec, PredPrec>, ItpRefutation>
    seqItpProd2ExplPredAuto(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver,
            AutoExpl autoExpl, ExprSplitters.ExprSplitter splitter
    ) {
        return of(ExprTraceSeqItpChecker.create(init, target, solver),
                AutomaticItpRefToProd2ExplPredPrec.create(autoExpl, splitter));
    }

    static TraceCheckerStrategy<Prod2Prec<ExplPrec, PredPrec>, ItpRefutation>
    fwBinItpProd2ExplPredAuto(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver,
            AutoExpl autoExpl, ExprSplitters.ExprSplitter splitter
    ) {
        return of(ExprTraceFwBinItpChecker.create(init, target, solver),
                AutomaticItpRefToProd2ExplPredPrec.create(autoExpl, splitter));
    }

    static TraceCheckerStrategy<Prod2Prec<ExplPrec, PredPrec>, ItpRefutation>
    bwBinItpProd2ExplPredAuto(
            Expr<BoolType> init, Expr<BoolType> target, ItpSolver solver,
            AutoExpl autoExpl, ExprSplitters.ExprSplitter splitter
    ) {
        return of(ExprTraceBwBinItpChecker.create(init, target, solver),
                AutomaticItpRefToProd2ExplPredPrec.create(autoExpl, splitter));
    }

    static <R extends Refutation> TraceCheckerStrategy<ExplPrec, R> combined(
            ExprTraceChecker<R> checker1,
            ExprTraceChecker<R> checker2,
            ExprTraceStatusMerger<R> merger,
            RefutationToPrec<ExplPrec, R> refToPrec
    ) {
        return of(ExprTraceCombinedChecker.create(checker1, checker2, merger), refToPrec);
    }
}
