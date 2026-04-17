package hu.bme.mit.theta.xta.learning.base;

import de.learnlib.exception.SULException;
import de.learnlib.sul.SUL;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;

import java.util.Set;
import java.util.function.Function;

public class XtaMealySUL<S extends State, P extends Prec, O> extends XtaSUL<S, P> implements SUL<XtaProcess.Edge, O> {
    private final Function<Boolean, O> outputFunction;

    private XtaMealySUL(XtaSystem xtaSystem, XtaAnalysis<S, P> xtaAnalysis, P prec, Function<Boolean, O> outputFunction, Set<XtaProcess.Edge> observableEdges) {
        super(xtaSystem, xtaAnalysis, prec, observableEdges);
        this.outputFunction = outputFunction;
    }

    public static <S extends State, P extends Prec, O> XtaMealySUL<S, P, O>
    create(XtaSystem xtaSystem, XtaAnalysis<S, P> xtaAnalysis, P prec, Function<Boolean, O> outputFunction, Set<XtaProcess.Edge> observableEdges) {
        return new XtaMealySUL<>(xtaSystem, xtaAnalysis, prec, outputFunction, observableEdges);
    }

    @Override
    public void pre() {
        reset();
    }

    @Override
    public void post() {
        terminate();
    }

    @Override
    public O step(XtaProcess.Edge edge) throws SULException {
        if (!canStillProcess()) {
            return outputFunction.apply(false);
        }

        processEdge(edge);

        return outputFunction.apply(canStillProcess());
    }
}
