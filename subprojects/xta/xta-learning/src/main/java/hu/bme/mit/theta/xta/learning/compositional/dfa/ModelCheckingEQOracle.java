package hu.bme.mit.theta.xta.learning.compositional.dfa;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import de.learnlib.sul.SUL;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.XtaLts;
import hu.bme.mit.theta.xta.analysis.expl.XtaExplAnalysis;
import hu.bme.mit.theta.xta.analysis.zone.XtaZoneAnalysis;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ModelCheckingEQOracle implements EquivalenceOracle<DFA<?, String>, String, Boolean> {

    private final XtaSystem xtaSystem;
    private final Alphabet<String> alphabet;
    private final EquivalenceOracle<DFA<?, String>, String, Boolean> inclusionOracle;
    private final SUL<String, Boolean> mappedSul;
    private final XtaDfaCheckerStrategy strategy;
    private final Set<String> shieldSymbols;

    public ModelCheckingEQOracle(XtaSystem xtaSystem, Alphabet<String> alphabet,
                                 EquivalenceOracle<DFA<?, String>, String, Boolean> inclusionOracle,
                                 SUL<String, Boolean> mappedSul, XtaDfaCheckerStrategy strategy,
                                 Set<String> shieldSymbols) {
        this.xtaSystem = xtaSystem;
        this.alphabet = alphabet;
        this.inclusionOracle = inclusionOracle;
        this.mappedSul = mappedSul;
        this.strategy = strategy;
        this.shieldSymbols = shieldSymbols;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultQuery<String, Boolean> findCounterExample(DFA<?, String> hypothesis, Collection<? extends String> inputs) {
        Logger logger = new ConsoleLogger(Logger.Level.SUBSTEP);

        XtaExplAnalysis explAnalysis = XtaExplAnalysis.create(xtaSystem);
        XtaZoneAnalysis zoneAnalysis = XtaZoneAnalysis.create(xtaSystem.getInitLocs());
        Prod2Analysis<ExplState, ZoneState, XtaAction, UnitPrec, ZonePrec> prod2Analysis = Prod2Analysis.create(explAnalysis, zoneAnalysis);
        XtaAnalysis<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>> xtaAnalysis = XtaAnalysis.create(xtaSystem, prod2Analysis);

        XtaDfaInitFunc<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>, Object> lustaInit =
                XtaDfaInitFunc.create(xtaAnalysis.getInitFunc(), (DFA<Object, String>) hypothesis);

        // JAVÍTÁS: Átadjuk a shieldSymbols halmazt!
        XtaDfaTransFunc<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>, Object> lustaTrans =
                XtaDfaTransFunc.create(xtaAnalysis.getTransFunc(), (DFA<Object, String>) hypothesis, alphabet, shieldSymbols);

        PartialOrd<XtaDfaState<Prod2State<ExplState, ZoneState>, Object>> partialOrd = (s1, s2) -> s1.equals(s2);
        XtaDfaAnalysis<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>, Object> lustaAnalysis =
                XtaDfaAnalysis.create(lustaInit, lustaTrans, partialOrd);

        LTS<XtaDfaState<Prod2State<ExplState, ZoneState>, Object>, XtaAction> lts =
                state -> XtaLts.create(xtaSystem).getEnabledActionsFor(state.getXtaState());

        // A Theta natív hiba-felismerése (A SequenceInputStream miatt a .prop isError() lesz!)
        Predicate<XtaDfaState<Prod2State<ExplState, ZoneState>, Object>> targetPred =
                state -> state.getXtaState().isError();

        SafetyChecker<XtaDfaState<Prod2State<ExplState, ZoneState>, Object>, XtaAction, Prod2Prec<UnitPrec, ZonePrec>> checker =
                XtaDfaCheckerFactory.create(strategy, lustaAnalysis, lts, targetPred, logger);

        ZonePrec zonePrec = ZonePrec.of(xtaSystem.getClockVars());
        Prod2Prec<UnitPrec, ZonePrec> prod2Prec = Prod2Prec.of(UnitPrec.getInstance(), zonePrec);

        SafetyResult<?, XtaAction> result = checker.check(prod2Prec);

        if (!result.isSafe()) {
            Trace<?, XtaAction> trace = result.asUnsafe().getTrace();
            List<String> word = extractWordFromTrace(trace);

            mappedSul.pre();
            boolean acceptedBySul = true;
            for (String symbol : word) {
                Boolean stepResult = mappedSul.step(symbol);
                if (stepResult == null || !stepResult) {
                    acceptedBySul = false;
                    break;
                }
            }
            mappedSul.post();

            if (acceptedBySul) {
                // Valódi hiba -> Kivétel dobása (Megállítja a LearnLib-et)
                throw new RealBugFoundException(trace);
            } else {
                // Hamis hiba (CEGAR loop) -> Visszaküldjük finomításra!
                return new DefaultQuery<>(Word.fromList(word), false);
            }
        }

        return inclusionOracle.findCounterExample(hypothesis, inputs);
    }

    private List<String> extractWordFromTrace(Trace<?, XtaAction> trace) {
        List<String> word = new ArrayList<>();
        for (XtaAction action : trace.getActions()) {
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
                String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
                if (alphabet.containsSymbol(symbol)) {
                    word.add(symbol);
                }
            }
        }
        return word;
    }
}