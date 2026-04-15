package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public final class XtaDfaState<S extends State, DfaState> implements State {

    private final XtaState<S> xtaState;
    private final DfaState dfaState;

    private XtaDfaState(XtaState<S> xtaState, DfaState dfaState) {
        this.xtaState = checkNotNull(xtaState);
        this.dfaState = checkNotNull(dfaState);
    }

    public static <S extends State, DfaState> XtaDfaState<S, DfaState> create(XtaState<S> xtaState, DfaState dfaState) {
        return new XtaDfaState<>(xtaState, dfaState);
    }

    public DfaState getDfaState() {
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
        XtaDfaState<?, ?> that = (XtaDfaState<?, ?>) o;

        return xtaState.equals(that.xtaState) && dfaState.equals(that.dfaState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xtaState, dfaState);
    }

    @Override
    public String toString() {
        return xtaState.toString() + " | DFA State: " + dfaState.toString();
    }
}