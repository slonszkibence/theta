package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.xta.analysis.XtaOrd;
import hu.bme.mit.theta.xta.analysis.XtaState;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionOrd<S> implements PartialOrd<DiscreteCompositionState<S>> {

    private final PartialOrd<XtaState<ExplState>> xtaOrd;

    private DiscreteCompositionOrd(final PartialOrd<XtaState<ExplState>> xtaOrd) {
        this.xtaOrd = checkNotNull(xtaOrd);
    }

    public static <S> DiscreteCompositionOrd<S> create() {
        return new DiscreteCompositionOrd<>(XtaOrd.create(ExplOrd.getInstance()));
    }


    public static <S> DiscreteCompositionOrd<S> create(final PartialOrd<XtaState<ExplState>> xtaOrd) {
        return new DiscreteCompositionOrd<>(xtaOrd);
    }

    @Override
    public boolean isLeq(final DiscreteCompositionState<S> s1, final DiscreteCompositionState<S> s2) {
        checkNotNull(s1);
        checkNotNull(s2);
        if (!s1.getDfaState().equals(s2.getDfaState())) {
            return false;
        }
        return xtaOrd.isLeq(s1.getXtaState(), s2.getXtaState());
    }
}
