package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsCompositionInitFunc<S extends ExprState, prodS, P extends Prec>
        implements InitFunc<XstsCompositionState<S, prodS>, P> {

    private final InitFunc<XstsState<S>, P> innerInitFunc;
    private final DFA<prodS, XstsAction> productDfa;

    private XstsCompositionInitFunc(InitFunc<XstsState<S>, P> innerInitFunc, DFA<prodS, XstsAction> productDfa) {
        this.innerInitFunc = checkNotNull(innerInitFunc);
        this.productDfa = checkNotNull(productDfa);
    }

    public static <S extends ExprState, prodS, P extends Prec> XstsCompositionInitFunc<S, prodS, P> create(
            InitFunc<XstsState<S>, P> innerInitFunc, DFA<prodS, XstsAction> productDfa) {
        return new XstsCompositionInitFunc<>(innerInitFunc, productDfa);
    }

    @Override
    public Collection<XstsCompositionState<S, prodS>> getInitStates(P prec) {
        prodS dfaInitState = productDfa.getInitialState();
        Collection<? extends XstsState<S>> xstsInitStates = innerInitFunc.getInitStates(prec);

        Collection<XstsCompositionState<S, prodS>> compositionStates = new ArrayList<>();

        for (XstsState<S> xstsState : xstsInitStates) {
            compositionStates.add(XstsCompositionState.create(xstsState, dfaInitState));
        }
        return compositionStates;
    }
}
