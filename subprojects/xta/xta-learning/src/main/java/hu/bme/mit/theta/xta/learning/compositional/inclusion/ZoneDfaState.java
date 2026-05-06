package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.zone.ZoneState;

/**
 * Composite state representing a configuration during the zone-based DFA exploration
 * used by the inclusion oracle ({@link XtaInclusionOracle}).
 * <p>
 * In the learning-based compositional model checking framework, this class represents
 * a state in the parallel composition {@code T' || H^c} used for the emptiness check.
 * <p>
 * It strictly combines two components:
 * <ul>
 *   <li>The continuous time state (a {@link ZoneState}), tracking the LU-extrapolated
 *       clock valuations of the timed automaton {@code T'}.</li>
 *   <li>The discrete state of the DFA (of type {@code S}), representing the current
 *       node in the complemented hypothesis {@code H^c}.</li>
 * </ul>
 * A composite state is considered "bottom" (invalid/empty) if its underlying
 * continuous zone is empty ({@code isBottom() == true}), meaning the timing
 * constraints cannot be satisfied.
 *
 * @param <S> The state type of the DFA (typically representing the hypothesis or its complement).
 */
public class ZoneDfaState<S> implements State {
    private final ZoneState zoneState;
    private final S dfaState;

    private ZoneDfaState(final ZoneState zoneState, final S dfaState) {
        this.zoneState = zoneState;
        this.dfaState = dfaState;
    }

    /**
     * Creates a new composite {@link ZoneDfaState}.
     *
     * @param zoneState The continuous time state (Zone).
     * @param dfaState  The discrete DFA state.
     * @param <S>       The state type of the DFA.
     * @return A new {@link ZoneDfaState} instance.
     */
    public static<S> ZoneDfaState<S> create(ZoneState zoneState, S dfaState) {
        return new ZoneDfaState<>(zoneState, dfaState);
    }

    /**
     * @return The continuous time state (Zone).
     */
    public ZoneState getZoneState() {
        return zoneState;
    }

    /**
     * @return The discrete DFA state.
     */
    public S getDfaState() {
        return dfaState;
    }

    /**
     * Checks if the composite state is invalid.
     * <p>
     * Since the DFA state is always assumed to be a valid discrete node, the
     * composite state is considered bottom (empty) solely if the underlying
     * continuous timing constraints (Zone) are unsatisfiable.
     *
     * @return {@code true} if the underlying {@link ZoneState} is empty.
     */
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
