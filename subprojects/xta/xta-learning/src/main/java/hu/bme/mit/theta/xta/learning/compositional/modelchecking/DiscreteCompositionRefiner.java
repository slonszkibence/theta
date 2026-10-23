package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.RefinerResult;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.expr.refinement.*;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class DiscreteCompositionRefiner<S extends ExprState, prodS, P extends Prec, R extends Refutation>
        implements Refiner<DiscreteCompositionState<S, prodS>, XtaAction, P> {
    private final ExprTraceChecker<R> traceChecker;
    private final RefutationToPrec<P, R> refToPrec;
    private final PruneStrategy pruneStrategy;

    private DiscreteCompositionRefiner(
            final ExprTraceChecker<R> traceChecker,
            final RefutationToPrec<P, R> refToPrec,
            final PruneStrategy pruneStrategy
    ) {
        this.traceChecker = checkNotNull(traceChecker);
        this.refToPrec    = checkNotNull(refToPrec);
        this.pruneStrategy = checkNotNull(pruneStrategy);
    }

    public static <S extends ExprState, prodS, P extends Prec, R extends Refutation>
    DiscreteCompositionRefiner<S, prodS, P, R> create(
            ExprTraceChecker<R> traceChecker,
            RefutationToPrec<P, R> refToPrec,
            PruneStrategy pruneStrategy) {
        return new DiscreteCompositionRefiner<>(traceChecker, refToPrec, pruneStrategy);
    }

    public static <S extends ExprState, prodS, P extends Prec, R extends Refutation>
    DiscreteCompositionRefiner<S, prodS, ?, R> createFromStrategy(
            TraceCheckerStrategy<P, R> strategy,
            PruneStrategy pruneStrategy) {
        return new DiscreteCompositionRefiner<>(
                strategy.checker(), strategy.refToPrec(), pruneStrategy);
    }

    @Override
    public RefinerResult<DiscreteCompositionState<S, prodS>, XtaAction, P> refine(
            final ARG<DiscreteCompositionState<S, prodS>, XtaAction> arg, final P prec) {
        checkNotNull(arg);
        checkNotNull(prec);
        assert !arg.isSafe() : "ARG must be unsafe when the refiner is called";

        final ArgTrace<DiscreteCompositionState<S, prodS>, XtaAction> cex =
                arg.getCexs().findFirst()
                        .orElseThrow(() -> new AssertionError("No counterexample in unsafe ARG"));

        final Trace<DiscreteCompositionState<S, prodS>, XtaAction> fullTrace = cex.toTrace();

        final Trace<XtaState<S>, StmtAction> dataTrace = projectToDataTrace(fullTrace);

        final ExprTraceStatus<R> status = traceChecker.check(dataTrace);

        if (status.isFeasible()) {
            return RefinerResult.unsafe(fullTrace);
        }

        final R refutation = status.asInfeasible().getRefutation();

        P refinedPrec = prec;
        for (int i = 0; i < fullTrace.getStates().size(); i++) {
            refinedPrec = refToPrec.join(refinedPrec, refToPrec.toPrec(refutation, i));
        }

        checkState(!refinedPrec.equals(prec),
                "Refinement stalled: the data projection of the trace is infeasible, but the "
                        + "refutation did not strengthen the precision. Since the trace handed to "
                        + "the trace checker contains no clock constraints, this indicates a "
                        + "defect rather than a timing-related refutation. Precision: %s", prec);

        final ArgNode<DiscreteCompositionState<S, prodS>, XtaAction> nodeToPrune =
                cex.node(refutation.getPruneIndex());
        switch (pruneStrategy) {
            case LAZY -> arg.prune(nodeToPrune);
            case FULL -> arg.pruneAll();
        }

        return RefinerResult.spurious(refinedPrec);
    }

    private Trace<XtaState<S>, StmtAction> projectToDataTrace(
            final Trace<DiscreteCompositionState<S, prodS>, XtaAction> trace) {
        final List<XtaState<S>> xtaStates = trace.getStates().stream()
                .map(DiscreteCompositionState::getXtaState)
                .collect(Collectors.toList());
        final List<StmtAction> dataActions = trace.getActions().stream()
                .map(DiscreteCompositionTransFunc::dataActionOf)
                .collect(Collectors.toList());
        return Trace.of(xtaStates, dataActions);
    }
}
