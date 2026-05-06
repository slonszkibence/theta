package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Composite state representing a configuration during the discrete finite-state
 * model checking phase.
 * <p>
 * In the learning-based compositional model checking framework, this class represents
 * a state in the parallel composition {@code A || H} used for safety verification.
 * <p>
 * It strictly combines two components:
 * <ul>
 *   <li>The discrete state of the timed system (an {@link XtaState} wrapping an inner
 *       data state like {@code ExplState}), representing the current variable assignments
 *       and location.</li>
 *   <li>The discrete state of the product DFA (of type {@code prodS}), representing
 *       the current node in the learned hypothesis {@code H}.</li>
 * </ul>
 * A composite state is considered "bottom" (invalid/empty) if its underlying
 * XTA state is bottom (e.g., due to an invalid data variable assignment).
 *
 * @param <S>     The inner data state type of the XTA (e.g., {@code ExplState}).
 * @param <prodS> The state type of the product DFA (representing the learned hypothesis).
 */
public class DiscreteCompositionState<S extends State, prodS> implements State {
    private final XtaState<S> xtaState;
    private final prodS dfaState;

    private DiscreteCompositionState(XtaState<S> xtaState, prodS dfaState) {
        this.xtaState = checkNotNull(xtaState);
        this.dfaState = dfaState;
    }

    /**
     * Creates a new composite {@link DiscreteCompositionState}.
     *
     * @param xtaState The discrete state of the XTA.
     * @param dfaState The discrete state of the product DFA.
     * @param <S>      The inner data state type of the XTA.
     * @param <prodS>  The state type of the product DFA.
     * @return A new {@link DiscreteCompositionState} instance.
     */
    public static <S extends State, prodS> DiscreteCompositionState<S, prodS> create(
            XtaState<S> xtaState, prodS dfaState) {
        return new DiscreteCompositionState<>(xtaState, dfaState);
    }

    /**
     * @return The discrete state of the product DFA.
     */
    public prodS getDfaState() {
        return dfaState;
    }

    /**
     * @return The discrete state of the XTA.
     */
    public XtaState<S> getXtaState() {
        return xtaState;
    }

    /**
     * Checks if the composite state is invalid.
     * <p>
     * The composite state is considered bottom solely if the underlying XTA state
     * is bottom (e.g., if a variable assignment violates a type constraint or guard).
     *
     * @return {@code true} if the underlying {@link XtaState} is bottom.
     */
    @Override
    public boolean isBottom() {
        return xtaState.isBottom();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscreteCompositionState<?, ?> that = (DiscreteCompositionState<?, ?>) o;
        return Objects.equals(xtaState, that.xtaState) && Objects.equals(dfaState, that.dfaState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xtaState, dfaState);
    }

    @Override
    public String toString() {
        return "(" + xtaState + ", " + dfaState + ")";
    }
}
