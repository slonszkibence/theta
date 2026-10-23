package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;

import static com.google.common.base.Preconditions.checkNotNull;

public class ZoneDfaAnalysis<S, P extends Prec> implements Analysis<ZoneDfaState<S>, LearnLibAction<String>, P> {
    private final InitFunc<ZoneDfaState<S>, P> initFunc;
    private final TransFunc<ZoneDfaState<S>, LearnLibAction<String>, P> transFunc;
    private final PartialOrd<ZoneDfaState<S>> partialOrd;

    private ZoneDfaAnalysis(final InitFunc<ZoneDfaState<S>, P> initFunc, final TransFunc<ZoneDfaState<S>,
            LearnLibAction<String>, P> transFunc,  final PartialOrd<ZoneDfaState<S>> partialOrd) {
        this.initFunc = checkNotNull(initFunc);
        this.transFunc = checkNotNull(transFunc);
        this.partialOrd = checkNotNull(partialOrd);
    }

    public static<S, P extends Prec> ZoneDfaAnalysis<S, P> create(final InitFunc<ZoneDfaState<S>, P> initFunc, final TransFunc<ZoneDfaState<S>,
            LearnLibAction<String>, P> transFunc,  final PartialOrd<ZoneDfaState<S>> partialOrd) {
        return new ZoneDfaAnalysis<>(initFunc, transFunc, partialOrd);
    }

    @Override
    public InitFunc<ZoneDfaState<S>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<ZoneDfaState<S>, LearnLibAction<String>, P> getTransFunc() {
        return transFunc;
    }
    @Override
    public PartialOrd<ZoneDfaState<S>> getPartialOrd() {
        return partialOrd;
    }
}
