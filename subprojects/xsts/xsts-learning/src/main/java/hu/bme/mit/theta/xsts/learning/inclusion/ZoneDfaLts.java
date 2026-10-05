package hu.bme.mit.theta.xsts.learning.inclusion;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.xsts.learning.common.LearnLibAction;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

public class ZoneDfaLts<S, I> implements LTS<ZoneDfaState<S>, LearnLibAction<I>> {
    private final DFA<S, I> hypothesis;
    private final Collection<? extends I> alphabet;

    private ZoneDfaLts(DFA<S, I> hypothesis, Collection<? extends I> alphabet) {
        this.hypothesis = hypothesis;
        this.alphabet = alphabet;
    }

    public static<S, I> ZoneDfaLts<S, I> create(DFA<S, I> hypothesis, Collection<? extends I> alphabet) {
        return new ZoneDfaLts<>(hypothesis, alphabet);
    }

    @Override
    public Collection<LearnLibAction<I>> getEnabledActionsFor(ZoneDfaState<S> state) {
        Collection<LearnLibAction<I>> enabledActions = new ArrayList<>();
        S currentDfaState = state.getDfaState();

        for (I symbol : alphabet) {
            S nextDfaState = hypothesis.getTransition(currentDfaState, symbol);

            if (nextDfaState != null) {
                enabledActions.add(LearnLibAction.create(symbol));
            }
        }
        return  enabledActions;
    }
}