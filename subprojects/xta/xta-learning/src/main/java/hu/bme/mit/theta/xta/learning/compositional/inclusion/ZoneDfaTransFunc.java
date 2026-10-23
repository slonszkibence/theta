package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ZoneDfaTransFunc<S, P extends Prec>
        implements TransFunc<ZoneDfaState<S>, LearnLibAction<String>, P> {
    private final DFA<S, String> hypothesis;
    private final TimedStepper stepper;

    private ZoneDfaTransFunc(final DFA<S, String> hypothesis, final TimedStepper stepper) {
        this.hypothesis = checkNotNull(hypothesis);
        this.stepper = checkNotNull(stepper);
    }

    public static <S, P extends Prec> ZoneDfaTransFunc<S, P> create(
            final DFA<S, String> hypothesis, final TimedStepper stepper) {
        return new ZoneDfaTransFunc<>(hypothesis, stepper);
    }

    @Override
    public Collection<ZoneDfaState<S>> getSuccStates(final ZoneDfaState<S> state,
                                                     final LearnLibAction<String> action,
                                                     final P prec) {
        final String symbol = action.getSymbol();
        final S nextDfaState = hypothesis.getTransition(state.getDfaState(), symbol);
        if (nextDfaState == null) {
            return Collections.emptyList();
        }
        final TimedStepper.Config nextConfig = stepper.step(state.getConfig(), symbol);
        if (nextConfig == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(ZoneDfaState.create(nextConfig, nextDfaState));
    }
}
