package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.analysis.*;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import static com.google.common.base.Preconditions.checkNotNull;

public class XtaDfaAnalysis<S extends State, P extends Prec, DfaState>
        implements Analysis<XtaDfaState<S, DfaState>, XtaAction, P> {

    private final InitFunc<XtaDfaState<S, DfaState>, P> initFunc;
    private final TransFunc<XtaDfaState<S, DfaState>, XtaAction, P> transFunc;
    private final PartialOrd<XtaDfaState<S, DfaState>> partialOrd;

    private XtaDfaAnalysis(
            InitFunc<XtaDfaState<S, DfaState>, P> initFunc,
            TransFunc<XtaDfaState<S, DfaState>, XtaAction, P> transFunc,
            PartialOrd<XtaDfaState<S, DfaState>> partialOrd
    ) {
        this.initFunc = checkNotNull(initFunc);
        this.transFunc = checkNotNull(transFunc);
        this.partialOrd = checkNotNull(partialOrd);
    }

    public static<S extends State, P extends Prec, DfaState> XtaDfaAnalysis<S,P,DfaState> create(
            InitFunc<XtaDfaState<S, DfaState>, P> initFunc,
            TransFunc<XtaDfaState<S, DfaState>, XtaAction, P> transFunc,
            PartialOrd<XtaDfaState<S, DfaState>> partialOrd
    ) {
        return new XtaDfaAnalysis<>(initFunc, transFunc, partialOrd);
    }

    @Override
    public PartialOrd<XtaDfaState<S, DfaState>> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<XtaDfaState<S, DfaState>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<XtaDfaState<S, DfaState>, XtaAction, P> getTransFunc() {
        return transFunc;
    }
}
