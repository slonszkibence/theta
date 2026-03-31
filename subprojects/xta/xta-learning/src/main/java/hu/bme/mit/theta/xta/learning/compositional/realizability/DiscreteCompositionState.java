package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.Objects;


public class DiscreteCompositionState<S> implements State {
    private final XtaState<ExplState> xtaState;
    private final S dfaState;

    private DiscreteCompositionState(XtaState<ExplState> xtaState, S dfaState) {
        this.xtaState = xtaState;
        this.dfaState = dfaState;
    }

    public static<S> DiscreteCompositionState<S> create(XtaState<ExplState> xtaState, S dfaState) {
        return new DiscreteCompositionState<>(xtaState, dfaState);
    }

    public S getDfaState() {
        return dfaState;
    }
    public XtaState<ExplState> getXtaState() {
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
        DiscreteCompositionState<?> that = (DiscreteCompositionState<?>) o;
        return Objects.equals(xtaState, that.xtaState) && Objects.equals(dfaState, that.dfaState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xtaState, dfaState);
    }
}
