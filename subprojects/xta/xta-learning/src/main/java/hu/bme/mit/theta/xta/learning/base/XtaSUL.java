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

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract class for a system under learning (SUL) that can make single steps.
 */
public abstract class XtaSUL<S extends State, P extends Prec> {
    protected final XtaAnalysis<S, P> xtaAnalysis;
    protected final LTS<XtaState<?>, XtaAction> lts;
    protected Set<XtaState<S>> states;
    protected final P prec;
    protected final Set<XtaProcess.Edge> observableEdges;

    protected XtaSUL(XtaSystem xtaSystem, XtaAnalysis<S, P> xtaAnalysis, P prec, Set<XtaProcess.Edge> observableEdges) {
        this.xtaAnalysis = checkNotNull(xtaAnalysis);
        this.prec = checkNotNull(prec);
        this.observableEdges = observableEdges;
        this.states = new HashSet<>();
        this.lts = XtaLts.create(xtaSystem);
    }

    /**
     * setup SUL.
     */
    protected void reset() {
        states.clear();
        Collection<? extends XtaState<S>> initStates = xtaAnalysis.getInitFunc().getInitStates(prec);
        states.addAll(computeTauClosure(initStates));
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

        states = computeTauClosure(nextStates);
    }

    protected boolean canStillProcess() {
        return !states.isEmpty();
    }

    protected Set<XtaState<S>> computeTauClosure(Collection<? extends XtaState<S>> initialStates) {
        Set<XtaState<S>> closure = new HashSet<>(initialStates);
        Queue<XtaState<S>> queue = new LinkedList<>(initialStates);

        while (!queue.isEmpty()) {
            XtaState<S> state = queue.poll();
            for (var action : lts.getEnabledActionsFor(state)) {
                if (isInternalAction(action)) {
                    Collection<? extends XtaState<S>> succStates = xtaAnalysis.getTransFunc().getSuccStates(state, action, prec);
                    for (XtaState<S> succState : succStates) {
                        if (closure.add(succState)) {
                            queue.add(succState);
                        }
                    }
                }
            }
        }
        return closure;
    }

    protected boolean isInternalAction(XtaAction action) {
        List<XtaProcess.Edge> edges = new ArrayList<>();
        if (action.isBasic()) edges.add(action.asBasic().getEdge());
        else if (action.isBinary()) {
            edges.add(action.asBinary().getEmitEdge());
            edges.add(action.asBinary().getRecvEdge());
        } else if (action.isBroadcast()) {
            edges.add(action.asBroadcast().getEmitEdge());
            edges.addAll(action.asBroadcast().getRecvEdges());
        }


        for (XtaProcess.Edge edge : edges) {
            if (observableEdges.contains(edge)) {
                return false;
            }
        }

        return true;
    }

    public XtaAnalysis<S, P> getXtaAnalysis() {
        return xtaAnalysis;
    }
    public LTS<XtaState<?>, XtaAction> getLts() {
        return lts;
    }
    public Set<XtaState<S>> getStates() {
        return states;
    }
    public P getPrec() {
        return prec;
    }
    public Set<XtaProcess.Edge> getObservableEdges() {
        return observableEdges;
    }
}
