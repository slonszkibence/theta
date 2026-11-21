package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xsts.analysis.XstsOrd;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsCompositionOrd <S extends ExprState, prodS>
        implements PartialOrd<XstsCompositionState<S, prodS>> {

    private final XstsOrd<S> xstsOrd;

    private XstsCompositionOrd(XstsOrd<S> xstsOrd) {
        this.xstsOrd = checkNotNull(xstsOrd);
    }

    public static <S extends ExprState, prodS> XstsCompositionOrd<S, prodS> create(XstsOrd<S> xstsOrd) {
        return new XstsCompositionOrd<>(xstsOrd);
    }

    @Override
    public boolean isLeq(XstsCompositionState<S, prodS> s1, XstsCompositionState<S, prodS> s2) {
        checkNotNull(s1);
        checkNotNull(s2);

        if (!s1.getDfaState().equals(s2.getDfaState())) {
            return false;
        }

        return xstsOrd.isLeq(s1.getXstsState(), s2.getXstsState());
    }
}
