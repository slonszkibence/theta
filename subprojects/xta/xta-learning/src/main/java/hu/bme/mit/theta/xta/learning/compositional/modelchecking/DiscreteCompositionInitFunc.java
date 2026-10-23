package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.analysis.XtaState;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionInitFunc<S extends State, prodS, P extends Prec>
        implements InitFunc<DiscreteCompositionState<S, prodS>, P> {
    private final InitFunc<XtaState<S>, P> innerInitFunc;
    private final DFA<prodS, String> productDfa;

    private DiscreteCompositionInitFunc(InitFunc<XtaState<S>, P> innerInitFunc,
                                        DFA<prodS, String> productDfa) {
        this.innerInitFunc = checkNotNull(innerInitFunc);
        this.productDfa = checkNotNull(productDfa);
    }

    public static <S extends State, prodS, P extends Prec> DiscreteCompositionInitFunc<S, prodS, P> create(
            InitFunc<XtaState<S>, P> innerInitFunc, DFA<prodS, String> hypothesis) {
        return new DiscreteCompositionInitFunc<>(innerInitFunc, hypothesis);
    }

    @Override
    public Collection<DiscreteCompositionState<S, prodS>> getInitStates(P prec) {
        prodS dfaInitState = productDfa.getInitialState();
        Collection<? extends XtaState<S>> xtaInitStates = innerInitFunc.getInitStates(prec);

        Collection<DiscreteCompositionState<S, prodS>> result = new ArrayList<>();
        for (XtaState<S> xtaInitState : xtaInitStates) {
            result.add(DiscreteCompositionState.create(xtaInitState, dfaInitState));
        }
        return result;
    }
}
