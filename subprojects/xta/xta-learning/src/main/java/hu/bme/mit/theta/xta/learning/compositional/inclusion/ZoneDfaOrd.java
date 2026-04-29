package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.zone.BoundFunc;

import static com.google.common.base.Preconditions.checkNotNull;

public class ZoneDfaOrd<S> implements PartialOrd<ZoneDfaState<S>> {
    private final BoundFunc luBounds;

    private ZoneDfaOrd(BoundFunc luBounds) {
        this.luBounds = checkNotNull(luBounds);
    }

    public static <S> ZoneDfaOrd<S> create(BoundFunc luBounds) {
        return new ZoneDfaOrd<>(luBounds);
    }

    @Override
    public boolean isLeq(ZoneDfaState<S> state1, ZoneDfaState<S> state2) {
        checkNotNull(state1);
        checkNotNull(state2);

        if (!state1.getDfaState().equals(state2.getDfaState())) {
            return false;
        }
        return state1.getZoneState().isLeq(state2.getZoneState(), luBounds);
    }
}
