package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.xta.Sync;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.expl.XtaExplAnalysis;
import hu.bme.mit.theta.xta.analysis.zone.XtaZoneAnalysis;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;
import hu.bme.mit.theta.xta.learning.base.XtaSUL;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class XtaSULTest {
    private static class TestSUL extends XtaSUL<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>> {
        public TestSUL(
                XtaSystem system,
                XtaAnalysis<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>> analysis,
                Prod2Prec<UnitPrec, ZonePrec> prec, Set<XtaProcess.Edge> observableEdges) {
            super(system, analysis, prec, observableEdges);
        }
        @Override
        public void reset() {
            super.reset();
        }

        @Override
        public void processEdge(XtaProcess.Edge edge) {
            super.processEdge(edge);
        }

        @Override
        public boolean isInternalAction(XtaAction xtaAction) {
            return super.isInternalAction(xtaAction);
        }

        public int getActiveStateCount() {
            return this.states.size();
        }
    }

    @Test
    public void testTauClosureAndDelay() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/model/csma-2.xta");
        Assert.assertNotNull("A teszt modell nem található!", stream);
        XtaSystem xtaSystem = XtaDslManager.createSystem(stream);

        XtaExplAnalysis explAnalysis = XtaExplAnalysis.create(xtaSystem);
        XtaZoneAnalysis zoneAnalysis = XtaZoneAnalysis.create(xtaSystem.getInitLocs());
        Prod2Analysis<ExplState, ZoneState, XtaAction, UnitPrec, ZonePrec> prod2Analysis =
                Prod2Analysis.create(explAnalysis, zoneAnalysis);
        XtaAnalysis<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>> xtaAnalysis =
                XtaAnalysis.create(xtaSystem, prod2Analysis);

        ZonePrec zonePrec = ZonePrec.of(xtaSystem.getClockVars());
        Prod2Prec<UnitPrec, ZonePrec> prec = Prod2Prec.of(UnitPrec.getInstance(), zonePrec);

        Set<XtaProcess.Edge> observableEdges = new HashSet<>();
        XtaProcess.Edge testEdgeToFire = null;

        for (XtaProcess process : xtaSystem.getProcesses()) {
            for (XtaProcess.Edge edge : process.getEdges()) {
                if (edge.getSync().isPresent()) {
                    observableEdges.add(edge);
                    if (edge.getSync().get().getKind() == Sync.Kind.EMIT) {
                        testEdgeToFire = edge;
                    }
                }
            }
        }
        Assert.assertNotNull("Nem találtunk szinkronizációs élet a modellben!", testEdgeToFire);

        TestSUL sul = new TestSUL(xtaSystem, xtaAnalysis, prec, observableEdges);
        sul.reset();

        int statesAfterReset = sul.getActiveStateCount();
        System.out.println("Állapotok száma reset (tau-closure) után: " + statesAfterReset);
        Assert.assertTrue("A SUL nem találta meg a kezdőállapotokat!", statesAfterReset > 0);

        XtaProcess.Edge validFirstEdge = null;

        for (var state : sul.getStates()) {
            for (var action : sul.getLts().getEnabledActionsFor(state)) {
                if (!sul.isInternalAction(action)) {
                    if (action.isBasic()) validFirstEdge = action.asBasic().getEdge();
                    else if (action.isBinary()) validFirstEdge = action.asBinary().getEmitEdge();
                    else if (action.isBroadcast()) validFirstEdge = action.asBroadcast().getEmitEdge();

                    if (validFirstEdge != null && observableEdges.contains(validFirstEdge)) {
                        break;
                    }
                }
            }
            if (validFirstEdge != null) break;
        }

        Assert.assertNotNull("A kezdőállapotból nincs engedélyezett, megfigyelhető lépés!", validFirstEdge);

        System.out.println("Teszt él elsütése (Érvényes lépés): " + validFirstEdge.getSource().getName() + " -> " + validFirstEdge.getTarget().getName());
        sul.processEdge(validFirstEdge);

        int statesAfterStep = sul.getActiveStateCount();
        System.out.println("Állapotok száma a lépés után: " + statesAfterStep);
        Assert.assertTrue("A SUL elakadt egy érvényes lépésnél!", statesAfterStep > 0);
    }
}
