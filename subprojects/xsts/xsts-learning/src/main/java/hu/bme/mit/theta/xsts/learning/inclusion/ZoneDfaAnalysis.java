package hu.bme.mit.theta.xsts.learning.inclusion;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.xsts.learning.common.LearnLibAction;

import static com.google.common.base.Preconditions.checkNotNull;

public class ZoneDfaAnalysis<S, I> implements Analysis<ZoneDfaState<S>, LearnLibAction<I>, ZonePrec> {
    private final InitFunc<ZoneDfaState<S>, ZonePrec> initFunc;
    private final TransFunc<ZoneDfaState<S>, LearnLibAction<I>, ZonePrec> transFunc;
    private final PartialOrd<ZoneDfaState<S>> partialOrd;

    private ZoneDfaAnalysis(final InitFunc<ZoneDfaState<S>, ZonePrec> initFunc, final TransFunc<ZoneDfaState<S>,
            LearnLibAction<I>, ZonePrec> transFunc,  final PartialOrd<ZoneDfaState<S>> partialOrd) {
        this.initFunc = checkNotNull(initFunc);
        this.transFunc = checkNotNull(transFunc);
        this.partialOrd = checkNotNull(partialOrd);
    }

    public static<S, I> ZoneDfaAnalysis<S, I> create(final InitFunc<ZoneDfaState<S>, ZonePrec> initFunc, final TransFunc<ZoneDfaState<S>,
            LearnLibAction<I>, ZonePrec> transFunc,  final PartialOrd<ZoneDfaState<S>> partialOrd) {
        return new ZoneDfaAnalysis<>(initFunc, transFunc, partialOrd);
    }

    @Override
    public InitFunc<ZoneDfaState<S>, ZonePrec> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<ZoneDfaState<S>, LearnLibAction<I>, ZonePrec> getTransFunc() {
        return transFunc;
    }
    @Override
    public PartialOrd<ZoneDfaState<S>> getPartialOrd() {
        return partialOrd;
    }
}