package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionState<S extends State, prodS> implements State {
    private final XtaState<S> xtaState;
    private final prodS dfaState;

    private DiscreteCompositionState(XtaState<S> xtaState, prodS dfaState) {
        this.xtaState = checkNotNull(xtaState);
        this.dfaState = dfaState;
    }

    public static <S extends State, prodS> DiscreteCompositionState<S, prodS> create(
            XtaState<S> xtaState, prodS dfaState) {
        return new DiscreteCompositionState<>(xtaState, dfaState);
    }

    public prodS getDfaState() {
        return dfaState;
    }

    public XtaState<S> getXtaState() {
        return xtaState;
    }

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
