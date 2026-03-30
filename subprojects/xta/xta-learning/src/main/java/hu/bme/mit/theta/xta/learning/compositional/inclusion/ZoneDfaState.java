package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.zone.ZoneState;

public class ZoneDfaState<S> implements State {
    private final ZoneState zoneState;
    private final S dfaState;

    private ZoneDfaState(final ZoneState zoneState, final S dfaState) {
        this.zoneState = zoneState;
        this.dfaState = dfaState;
    }

    public static<S> ZoneDfaState<S> create(ZoneState zoneState, S dfaState) {
        return new ZoneDfaState<>(zoneState, dfaState);
    }

    public ZoneState getZoneState() {
        return zoneState;
    }

    public S getDfaState() {
        return dfaState;
    }

    @Override
    public boolean isBottom() {
        return zoneState.isBottom();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZoneDfaState<?> that = (ZoneDfaState<?>) obj;
        return zoneState.equals(that.zoneState) && dfaState.equals(that.dfaState);
    }

    @Override
    public int hashCode() {
        int result = zoneState.hashCode();
        result = 31 * result + dfaState.hashCode();
        return result;
    }
}
