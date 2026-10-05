package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.RefinerResult;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.refinement.*;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class XstsCompositionRefiner <S extends ExprState, prodS, P extends Prec, R extends Refutation>
        implements Refiner<XstsCompositionState<S, prodS>, XstsAction, P> {

    private final ExprTraceChecker<R> traceChecker;
    private final RefutationToPrec<P, R> refToPrec;
    private final PruneStrategy pruneStrategy;
    private final TimedXstsActionProjections projections;

    private XstsCompositionRefiner(
            final ExprTraceChecker<R> traceChecker,
            final RefutationToPrec<P, R> refToPrec,
            final PruneStrategy pruneStrategy,
            final TimedXstsActionProjections projections
    ) {
        this.traceChecker = checkNotNull(traceChecker);
        this.refToPrec    = checkNotNull(refToPrec);
        this.pruneStrategy = checkNotNull(pruneStrategy);
        this.projections = checkNotNull(projections);
    }

    public static <S extends ExprState, prodS, P extends Prec, R extends Refutation>
    XstsCompositionRefiner<S, prodS, P, R> create(
            final ExprTraceChecker<R> traceChecker,
            final RefutationToPrec<P, R> refToPrec,
            final PruneStrategy pruneStrategy,
            final TimedXstsActionProjections projections
    ) {
        return new XstsCompositionRefiner<>(traceChecker, refToPrec, pruneStrategy, projections);
    }

    public static <S extends ExprState, prodS, P extends Prec, R extends Refutation>
    XstsCompositionRefiner<S, prodS, P, R> createFromStrategy(
            TraceCheckerStrategy<P, R> strategy,
            PruneStrategy pruneStrategy,
            TimedXstsActionProjections projections
    ) {
        return new XstsCompositionRefiner<>(strategy.checker(), strategy.refToPrec(), pruneStrategy, projections);
    }


    @Override
    public RefinerResult<XstsCompositionState<S, prodS>, XstsAction, P> refine(
            ARG<XstsCompositionState<S, prodS>, XstsAction> arg,
            P prec
    ) {
        checkNotNull(arg);
        checkNotNull(prec);

        assert !arg.isSafe() : "ARG must be unsafe when the refiner is called";

        final ArgTrace<XstsCompositionState<S, prodS>, XstsAction> cex =
                        arg.getCexs().
                        findFirst().
                        orElseThrow(() ->
                                new AssertionError("No counterexample in unsafe ARG"));

        final Trace<XstsCompositionState<S, prodS>, XstsAction> fullTrace = cex.toTrace();

        final Trace<XstsState<S>, XstsAction> xstsTrace = projectToXstsTrace(fullTrace);

        final ExprTraceStatus<R> status = traceChecker.check(xstsTrace);

        if (status.isFeasible()) {
            return RefinerResult.unsafe(fullTrace);
        }

        final R refutation = status.asInfeasible().getRefutation();

        P refinedPrec = prec;

        for (int i = 0; i < fullTrace.getStates().size(); i++) {
            refinedPrec = refToPrec.join(refinedPrec, refToPrec.toPrec(refutation, i));
        }

        checkState(!refinedPrec.equals(prec),
                "Refinement stalled: the counterexample is infeasible, but the refutation does not"
                        + " strengthen the precision %s", prec);

        final ArgNode<XstsCompositionState<S, prodS>, XstsAction> nodeToPrune =
                cex.node(refutation.getPruneIndex());

        switch (pruneStrategy) {
            case LAZY -> arg.prune(nodeToPrune);
            case FULL -> arg.pruneAll();
        }

        return RefinerResult.spurious(refinedPrec);
    }

    private Trace<XstsState<S>, XstsAction> projectToXstsTrace(
            final Trace<XstsCompositionState<S, prodS>, XstsAction> trace
    ) {
        final List<XstsState<S>> xstsStates =
                trace.getStates()
                        .stream()
                        .map(XstsCompositionState::getXstsState)
                        .collect(Collectors.toList());
        final List<XstsAction> dataActions =
                trace.getActions()
                        .stream()
                        .map(projections::dataProjection)
                        .collect(Collectors.toList());
        return Trace.of(xstsStates, dataActions);
    }
}
