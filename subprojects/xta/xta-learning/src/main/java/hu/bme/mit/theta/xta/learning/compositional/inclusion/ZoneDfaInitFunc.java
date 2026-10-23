package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ZoneDfaInitFunc<S, P extends Prec> implements InitFunc<ZoneDfaState<S>, P> {
    private final DFA<S, String> hypothesis;
    private final TimedStepper stepper;

    private ZoneDfaInitFunc(final DFA<S, String> hypothesis, final TimedStepper stepper) {
        this.hypothesis = checkNotNull(hypothesis);
        this.stepper = checkNotNull(stepper);
    }

    public static <S, P extends Prec> ZoneDfaInitFunc<S, P> create(
            final DFA<S, String> hypothesis, final TimedStepper stepper) {
        return new ZoneDfaInitFunc<>(hypothesis, stepper);
    }

    @Override
    public Collection<ZoneDfaState<S>> getInitStates(final P prec) {
        return Collections.singleton(
                ZoneDfaState.create(stepper.initial(), hypothesis.getInitialState()));
    }
}
