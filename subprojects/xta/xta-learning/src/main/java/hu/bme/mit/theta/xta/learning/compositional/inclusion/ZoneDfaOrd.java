package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.zone.BoundFunc;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Partial order implementation for the zone-based DFA exploration used by the
 * inclusion oracle ({@link XtaInclusionOracle}).
 * <p>
 * In the learning-based compositional model checking framework, this class provides
 * the subsumption (covering) check during the emptiness check of the parallel
 * composition {@code T' || H^c}.
 * <p>
 * The partial order determines if one {@link ZoneDfaState} is "less than or equal to"
 * another, meaning the behavior from the first state is completely covered by the
 * behavior from the second state. This is true if and only if:
 * <ol>
 *   <li>The discrete DFA states (representing {@code H^c}) are exactly equal.</li>
 *   <li>The continuous Zone of the first state is a subset of the Zone of the second
 *       state (i.e., {@code Z1 ⊆ Z2}), evaluated under the system's maximum
 *       constants (LU bounds) to ensure finiteness.</li>
 * </ol>
 * If {@code state1 <= state2}, the exploration algorithm can safely discard {@code state1}.
 *
 * @param <S> The state type of the DFA (typically representing the hypothesis or its complement).
 */
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