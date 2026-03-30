package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

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
