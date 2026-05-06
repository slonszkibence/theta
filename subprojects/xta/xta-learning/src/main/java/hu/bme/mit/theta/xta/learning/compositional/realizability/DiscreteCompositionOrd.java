package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.xta.analysis.XtaOrd;
import hu.bme.mit.theta.xta.analysis.XtaState;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Partial order implementation for the discrete finite-state model checking phase.
 * <p>
 * In the learning-based compositional model checking framework, this class provides
 * the subsumption (covering) check during the safety verification of the parallel
 * composition {@code A || H}.
 * <p>
 * A composite state {@code s1} is considered "less than or equal to" {@code s2}
 * (meaning the behavior from {@code s1} is completely covered by {@code s2}) if
 * and only if:
 * <ol>
 *   <li>The discrete DFA states (representing the learned hypothesis {@code H})
 *       are exactly equal.</li>
 *   <li>The inner XTA states are covered according to the underlying partial order
 *       (e.g., subset inclusion or exact match via {@link ExplOrd}).</li>
 * </ol>
 *
 * @param <S>     The inner data state type of the XTA (e.g., {@code ExplState}).
 * @param <prodS> The state type of the product DFA (representing the learned hypothesis).
 */
public class DiscreteCompositionOrd<S extends State, prodS>
        implements PartialOrd<DiscreteCompositionState<S, prodS>> {

    private final PartialOrd<XtaState<S>> xtaOrd;

    private DiscreteCompositionOrd(final PartialOrd<XtaState<S>> xtaOrd) {
        this.xtaOrd = checkNotNull(xtaOrd);
    }

    /**
     * Convenience factory creating a partial order based purely on explicit states
     * ({@link ExplOrd}). This represents the original, purely discrete behavior.
     *
     * @param <prodS> The state type of the product DFA.
     * @return A new {@link DiscreteCompositionOrd} instance using explicit state ordering.
     */
    public static <prodS> DiscreteCompositionOrd<?, prodS> createExpl() {
        return new DiscreteCompositionOrd<>(XtaOrd.create(ExplOrd.getInstance()));
    }

    /**
     * General factory for creating a partial order with a custom inner state ordering.
     *
     * @param innerOrd The partial order for the inner data state type {@code S}.
     * @param <S>      The inner data state type.
     * @param <prodS>  The state type of the product DFA.
     * @return A new {@link DiscreteCompositionOrd} instance.
     */
    public static <S extends State, prodS> DiscreteCompositionOrd<S, prodS> create(
            PartialOrd<S> innerOrd) {
        return new DiscreteCompositionOrd<>(XtaOrd.create(innerOrd));
    }

    /**
     * General factory for creating a partial order using an already instantiated
     * {@link XtaOrd}.
     *
     * @param xtaOrd  The partial order for the {@link XtaState}.
     * @param <S>     The inner data state type.
     * @param <prodS> The state type of the product DFA.
     * @return A new {@link DiscreteCompositionOrd} instance.
     */
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
