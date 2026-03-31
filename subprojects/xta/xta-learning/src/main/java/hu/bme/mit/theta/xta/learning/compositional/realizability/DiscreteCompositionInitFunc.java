package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.xta.analysis.XtaState;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionInitFunc<S, P extends Prec> implements InitFunc<DiscreteCompositionState<S>, P> {
    private final InitFunc<XtaState<ExplState>, P> innerInitFunc;
    private final DFA<S, String> hypothesis;

    private DiscreteCompositionInitFunc(InitFunc<XtaState<ExplState>, P> innerInitFunc, DFA<S, String> hypothesis) {
        this.innerInitFunc = checkNotNull(innerInitFunc);
        this.hypothesis = checkNotNull(hypothesis);
    }

    public static<S, P extends Prec> DiscreteCompositionInitFunc<S, P>
    create(InitFunc<XtaState<ExplState>, P> innerInitFunc,  DFA<S, String> hypothesis) {
        return new DiscreteCompositionInitFunc<>(innerInitFunc, hypothesis);
    }

    @Override
    public Collection<DiscreteCompositionState<S>> getInitStates(P prec) {
        S dfaInitState = hypothesis.getInitialState();
        Collection<? extends XtaState<ExplState>> xtaInitStates = innerInitFunc.getInitStates(prec);

        Collection<DiscreteCompositionState<S>> result = new ArrayList<>();

        for (XtaState<ExplState> xtaInitState : xtaInitStates) {
            result.add(DiscreteCompositionState.create(xtaInitState, dfaInitState));
        }

        return result;
    }
}
