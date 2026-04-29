package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.RefinerResult;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ItpRefToExplPrec;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceStatus;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionRefiner<S> implements Refiner<DiscreteCompositionState<S>, XtaAction, ExplPrec> {

    private final ExprTraceChecker<ItpRefutation> traceChecker;
    private final ItpRefToExplPrec refToPrec;
    private final PruneStrategy pruneStrategy;

    private DiscreteCompositionRefiner(final ExprTraceChecker<ItpRefutation> traceChecker,
                                       final PruneStrategy pruneStrategy) {
        this.traceChecker = checkNotNull(traceChecker);
        this.refToPrec = new ItpRefToExplPrec();
        this.pruneStrategy = checkNotNull(pruneStrategy);
    }

    public static <S> DiscreteCompositionRefiner<S> create(
            final ExprTraceChecker<ItpRefutation> traceChecker,
            final PruneStrategy pruneStrategy) {
        return new DiscreteCompositionRefiner<>(traceChecker, pruneStrategy);
    }

    @Override
    public RefinerResult<DiscreteCompositionState<S>, XtaAction, ExplPrec> refine(
            final ARG<DiscreteCompositionState<S>, XtaAction> arg, final ExplPrec prec) {
        checkNotNull(arg);
        checkNotNull(prec);
        assert !arg.isSafe() : "ARG must be unsafe when the refiner is called";

        final ArgTrace<DiscreteCompositionState<S>, XtaAction> cex =
                arg.getCexs().findFirst()
                   .orElseThrow(() -> new AssertionError("No counterexample in unsafe ARG"));

        final Trace<DiscreteCompositionState<S>, XtaAction> fullTrace = cex.toTrace();

        final Trace<XtaState<ExplState>, XtaAction> xtaTrace = projectToXtaTrace(fullTrace);

        final ExprTraceStatus<ItpRefutation> status = traceChecker.check(xtaTrace);

        if (status.isFeasible()) {
            return RefinerResult.unsafe(fullTrace);
        }

        final ItpRefutation refutation = status.asInfeasible().getRefutation();

        ExplPrec refinedPrec = prec;
        for (int i = 0; i < fullTrace.getStates().size(); i++) {
            refinedPrec = refinedPrec.join(refToPrec.toPrec(refutation, i));
        }

        final ArgNode<DiscreteCompositionState<S>, XtaAction> nodeToPrune =
                cex.node(refutation.getPruneIndex());
        switch (pruneStrategy) {
            case LAZY -> arg.prune(nodeToPrune);
            case FULL -> arg.pruneAll();
        }

        return RefinerResult.spurious(refinedPrec);
    }


    private Trace<XtaState<ExplState>, XtaAction> projectToXtaTrace(
            final Trace<DiscreteCompositionState<S>, XtaAction> trace) {
        final List<XtaState<ExplState>> xtaStates = trace.getStates().stream()
                .map(DiscreteCompositionState::getXtaState)
                .collect(Collectors.toList());
        return Trace.of(xtaStates, trace.getActions());
    }
}
