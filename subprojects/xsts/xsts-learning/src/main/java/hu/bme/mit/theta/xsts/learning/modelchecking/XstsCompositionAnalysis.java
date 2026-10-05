package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.*;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsCompositionAnalysis<S extends ExprState, prodS, P extends Prec>
        implements Analysis<XstsCompositionState<S, prodS>, XstsAction, P> {

    private final InitFunc<XstsCompositionState<S, prodS>, P> initFunc;
    private final TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> transFunc;
    private final PartialOrd<XstsCompositionState<S, prodS>> partialOrd;

    private XstsCompositionAnalysis(
            InitFunc<XstsCompositionState<S, prodS>, P> initFunc,
            TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> transFunc,
            PartialOrd<XstsCompositionState<S, prodS>> partialOrd) {
        this.initFunc = checkNotNull(initFunc);
        this.transFunc = checkNotNull(transFunc);
        this.partialOrd = checkNotNull(partialOrd);
    }

    public static <S extends ExprState, prodS, P extends Prec> XstsCompositionAnalysis<S, prodS, P> create(
            InitFunc<XstsCompositionState<S, prodS>, P> initFunc,
            TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> transFunc,
            PartialOrd<XstsCompositionState<S, prodS>> partialOrd) {
        return new XstsCompositionAnalysis<>(initFunc, transFunc, partialOrd);
    }

    @Override
    public PartialOrd<XstsCompositionState<S, prodS>> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<XstsCompositionState<S, prodS>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> getTransFunc() {
        return transFunc;
    }
}
