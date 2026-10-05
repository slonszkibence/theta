package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import static com.google.common.base.Preconditions.checkNotNull;


public final class DataProjectedAnalysis {

    private DataProjectedAnalysis() {}

    public static <S extends ExprState, P extends Prec> Analysis<S, XstsAction, P> of(
            final Analysis<S, StmtAction, P> baseAnalysis,
            final TimedXstsActionProjections projections) {
        checkNotNull(baseAnalysis);
        checkNotNull(projections);

        final TransFunc<S, XstsAction, P> transFunc = (state, action, prec) ->
                baseAnalysis.getTransFunc().getSuccStates(state, projections.dataProjection(action), prec);

        return new Analysis<>() {
            @Override
            public PartialOrd<S> getPartialOrd() {
                return baseAnalysis.getPartialOrd();
            }

            @Override
            public InitFunc<S, P> getInitFunc() {
                return baseAnalysis.getInitFunc();
            }

            @Override
            public TransFunc<S, XstsAction, P> getTransFunc() {
                return transFunc;
            }
        };
    }
}
