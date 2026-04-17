package hu.bme.mit.theta.xta.learning.base;

import de.learnlib.exception.SULException;
import de.learnlib.sul.SUL;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;

import java.util.Set;

public class XtaDFASUL<S extends State, P extends Prec> extends XtaSUL<S, P> implements SUL<XtaProcess.Edge, Boolean> {
    private XtaDFASUL(XtaSystem xtaSystem, XtaAnalysis<S, P> xtaAnalysis, P prec, Set<XtaProcess.Edge> observableEdges) {
        super(xtaSystem, xtaAnalysis, prec, observableEdges);
    }

    public static <S extends State, P extends Prec> XtaDFASUL<S, P>
    create(XtaSystem xtaSystem, XtaAnalysis<S, P> xtaAnalysis, P prec, Set<XtaProcess.Edge> observableEdges) {
        return new XtaDFASUL<>(xtaSystem, xtaAnalysis, prec, observableEdges);
    }

    /**
     * setup SUL.
     */
    @Override
    public void pre() {
        reset();
    }

    /**
     * shut down SUL.
     */
    @Override
    public void post() {
        terminate();
    }

    /**
     * make one step on the SUL.
     *
     * @param edge
     *         input to the SUL
     *
     * @return output of SUL
     */
    @Override
    public Boolean step(XtaProcess.Edge edge) throws SULException {
        if (!canStillProcess()) {
            return false;
        }

        processEdge(edge);

        return canStillProcess();
    }
}
