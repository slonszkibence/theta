package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.xta.analysis.XtaAction;

public class RealBugFoundException extends RuntimeException {
    private final Trace<?, XtaAction> trace;

    public RealBugFoundException(Trace<?, XtaAction> trace) {
        super("Valós hiba található a modellben!");
        this.trace = trace;
    }

    public Trace<?, XtaAction> getTrace() {
        return trace;
    }
}