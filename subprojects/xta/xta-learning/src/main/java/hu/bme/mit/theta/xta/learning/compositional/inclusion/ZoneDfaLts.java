package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Labeled Transition System (LTS) implementation for the zone-based DFA exploration
 * used by the inclusion oracle ({@link XtaInclusionOracle}).
 * <p>
 * In the learning-based compositional model checking framework, this LTS defines
 * the enabled actions (transitions) from a given {@link ZoneDfaState} during the
 * emptiness check of the parallel composition {@code T' || H^c}.
 * <p>
 * Instead of querying the continuous timed automaton ({@code T'}) for enabled actions,
 * this LTS is driven entirely by the discrete hypothesis DFA (representing {@code H^c}).
 * For a given state, it iterates over the entire input alphabet and considers an action
 * enabled if and only if the DFA has a defined transition for that symbol from its
 * current state. The validity of these actions in the continuous time domain (i.e.,
 * whether the corresponding clock guards and invariants can be satisfied) is evaluated
 * later by the transition function ({@link ZoneDfaTransFunc}).
 *
 * @param <S> The state type of the DFA (typically representing the hypothesis or its complement).
 */
public class ZoneDfaLts<S> implements LTS<ZoneDfaState<S>, LearnLibAction<String>> {
    private final DFA<S, String> hypothesis;
    private final Collection<? extends String> alphabet;

    private ZoneDfaLts(DFA<S, String> hypothesis, Collection<? extends String> alphabet) {
        this.hypothesis = hypothesis;
        this.alphabet = alphabet;
    }

    public static<S> ZoneDfaLts<S> create(DFA<S, String> hypothesis, Collection<? extends String> alphabet) {
        return new ZoneDfaLts<>(hypothesis, alphabet);
    }

    @Override
    public Collection<LearnLibAction<String>> getEnabledActionsFor(ZoneDfaState<S> state) {
        Collection<LearnLibAction<String>> enabledActions = new ArrayList<>();
        S currentDfaState = state.getDfaState();

        for (String symbol : alphabet) {
            S nextDfaState = hypothesis.getTransition(currentDfaState, symbol);

            if (nextDfaState != null) {
                enabledActions.add(LearnLibAction.create(symbol));
            }
        }
        return  enabledActions;
    }
}
