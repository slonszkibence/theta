package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.xta.analysis.XtaOrd;
import hu.bme.mit.theta.xta.analysis.XtaState;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionOrd<S extends State, prodS>
        implements PartialOrd<DiscreteCompositionState<S, prodS>> {
    private final PartialOrd<XtaState<S>> xtaOrd;

    private DiscreteCompositionOrd(final PartialOrd<XtaState<S>> xtaOrd) {
        this.xtaOrd = checkNotNull(xtaOrd);
    }

    public static <prodS> DiscreteCompositionOrd<?, prodS> createExpl() {
        return new DiscreteCompositionOrd<>(XtaOrd.create(ExplOrd.getInstance()));
    }

    public static <S extends State, prodS> DiscreteCompositionOrd<S, prodS> create(
            PartialOrd<S> innerOrd) {
        return new DiscreteCompositionOrd<>(XtaOrd.create(innerOrd));
    }

    public static <S extends State, prodS> DiscreteCompositionOrd<S, prodS> createWithXtaOrd(
            PartialOrd<XtaState<S>> xtaOrd) {
        return new DiscreteCompositionOrd<>(xtaOrd);
    }

    @Override
    public boolean isLeq(final DiscreteCompositionState<S, prodS> s1,
                         final DiscreteCompositionState<S, prodS> s2) {
        checkNotNull(s1);
        checkNotNull(s2);
        if (!s1.getDfaState().equals(s2.getDfaState())) {
            return false;
        }
        return xtaOrd.isLeq(s1.getXtaState(), s2.getXtaState());
    }
}
