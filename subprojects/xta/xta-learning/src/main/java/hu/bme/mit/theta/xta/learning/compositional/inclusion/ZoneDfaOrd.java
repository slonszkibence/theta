package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.PartialOrd;

public final class ZoneDfaOrd<S> implements PartialOrd<ZoneDfaState<S>> {
    private ZoneDfaOrd() {}

    public static <S> ZoneDfaOrd<S> create() {
        return new ZoneDfaOrd<>();
    }

    @Override
    public boolean isLeq(final ZoneDfaState<S> state1, final ZoneDfaState<S> state2) {
        if (state1.isBottom()) {
            return true;
        }
        return state1.equals(state2);
    }
}
