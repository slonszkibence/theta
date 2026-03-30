package hu.bme.mit.theta.xta.learning.base;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.analysis.XtaLts;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract class for a system under learning (SUL) that can make single steps.
 */
public abstract class XtaSUL<S extends State, P extends Prec> {
    protected final XtaAnalysis<S, P> xtaAnalysis;
    protected final LTS<XtaState<?>, XtaAction> lts;
    protected Collection<XtaState<S>> states;
    protected final P prec;

    protected XtaSUL(XtaSystem xtaSystem, XtaAnalysis<S, P> xtaAnalysis, P prec) {
        this.xtaAnalysis = xtaAnalysis;
        this.prec = prec;
        states = new ArrayList<>();
        this.lts = XtaLts.create(xtaSystem);
    }

    /**
     * setup SUL.
     */
    protected void reset() {
        states.clear();
        states.addAll(xtaAnalysis.getInitFunc().getInitStates(prec));
    }

    /**
     * shut down SUL.
     */
    protected void terminate() {
        states.clear();
    }

    /**
     * make one step on the SUL.
     *
     * @param edge
     *         input to the SUL
     */
    protected void processEdge(XtaProcess.Edge edge) {
        Collection<XtaState<S>> nextStates = new ArrayList<>();

        for (var state : states) {
            for (var action : lts.getEnabledActionsFor(state)) {
                if (action.getSourceLocs().contains(edge.getSource()) &&
                    action.getTargetLocs().contains(edge.getTarget())) {
                    nextStates.addAll(xtaAnalysis.getTransFunc().getSuccStates(state, action, prec));
                }
            }
        }

        states =  nextStates;
    }

    protected boolean canStillProcess() {
        return !states.isEmpty();
    }
}
