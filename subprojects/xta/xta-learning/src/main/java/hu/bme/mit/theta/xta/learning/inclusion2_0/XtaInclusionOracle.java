package hu.bme.mit.theta.xta.learning.inclusion2_0;



import com.google.common.base.Predicate;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Ord;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.zone.ZoneOrd;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.analysis.expl.XtaExplAnalysis;
import hu.bme.mit.theta.xta.analysis.zone.XtaZoneAnalysis;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.compositional.dfa.*;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;

import net.automatalib.automaton.fsa.DFA;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.word.Word;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class XtaInclusionOracle implements EquivalenceOracle.DFAEquivalenceOracle<String> {
    private final XtaSystem xtaSystem;
    private final Alphabet<String> alphabet;
    private final Set<String> shieldSymbols;
    private final XtaDfaCheckerStrategy searchStrategy;
    private final XtaProcess targetProcess;

    private XtaInclusionOracle(
            XtaSystem xtaSystem,
            XtaProcess targetProcess,
            Alphabet<String> alphabet,
            Set<String> shieldSymbols,
            XtaDfaCheckerStrategy searchStrategy
    ) {
        this.xtaSystem = checkNotNull(xtaSystem);
        this.targetProcess = checkNotNull(targetProcess);
        this.alphabet = checkNotNull(alphabet);
        this.shieldSymbols = checkNotNull(shieldSymbols);
        this.searchStrategy = searchStrategy;
    }

    public static XtaInclusionOracle create(
            XtaSystem xtaSystem,
            XtaProcess targetProcess,
            Alphabet<String> alphabet,
            Set<String> shieldSymbols,
            XtaDfaCheckerStrategy searchStrategy
    ) {
        return new XtaInclusionOracle(xtaSystem, targetProcess, alphabet, shieldSymbols, searchStrategy);
    }

    @Override
    public DefaultQuery<String, Boolean> findCounterExample(DFA<?, String> hypothesis, Collection<? extends String> inputs) {
        return doFindCounterExample(hypothesis, inputs);
    }

    private <S> DefaultQuery<String, Boolean> doFindCounterExample(DFA<S, String> hypothesis, Collection<? extends String> inputs) {
        ConsoleLogger logger = new ConsoleLogger(Logger.Level.DETAIL);

        XtaExplAnalysis xtaExplAnalysis = XtaExplAnalysis.create(xtaSystem);
        XtaZoneAnalysis xtaZoneAnalysis = XtaZoneAnalysis.create(xtaSystem.getInitLocs());
        Prod2Analysis<ExplState, ZoneState, XtaAction, UnitPrec, ZonePrec> prod2Analysis =
                Prod2Analysis.create(xtaExplAnalysis, xtaZoneAnalysis);
        XtaAnalysis<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>> xtaAnalysis =
                XtaAnalysis.create(xtaSystem, prod2Analysis);
        XtaDfaInitFunc<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>, S> xtaDfaInitFunc =
                XtaDfaInitFunc.create(xtaAnalysis.getInitFunc(), hypothesis);
        XtaInclusionDfaTransFunc<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>, S> xtaInclusionDfaTransFunc =
                XtaInclusionDfaTransFunc.create(xtaAnalysis.getTransFunc(), hypothesis, alphabet, shieldSymbols);

        PartialOrd<Prod2State<ExplState, ZoneState>> prod2Ord =
                Prod2Ord.create(ExplOrd.getInstance(), ZoneOrd.getInstance());

        PartialOrd<XtaDfaState<Prod2State<ExplState, ZoneState>, S>> partialOrd = (s1, s2) -> {
            if (s1.getXtaState().getState().isBottom()) return true;
            if (s2.getXtaState().getState().isBottom()) return false;

            if (!java.util.Objects.equals(s1.getDfaState(), s2.getDfaState())) return false;
            if (!s1.getXtaState().getLocs().equals(s2.getXtaState().getLocs())) return false;

            if (!s1.getXtaState().getState().getState1().equals(s2.getXtaState().getState().getState1())) {
                return false;
            }
            return s1.getXtaState().getState().getState2().isLeq(s2.getXtaState().getState().getState2());
        };

        XtaDfaAnalysis<Prod2State<ExplState, ZoneState>, Prod2Prec<UnitPrec, ZonePrec>, S> xtaDfaAnalysis =
                XtaDfaAnalysis.create(xtaDfaInitFunc, xtaInclusionDfaTransFunc, partialOrd);

        LTS<XtaDfaState<Prod2State<ExplState, ZoneState>, S>, XtaAction> lts = state ->  {
            Collection<XtaAction> actions = new ArrayList<>();
            XtaState<Prod2State<ExplState, ZoneState>> xtaState = state.getXtaState();

            for (int i = 0; i < xtaSystem.getProcesses().size(); i++) {
                XtaProcess process = xtaSystem.getProcesses().get(i);

                if (!process.getName().equals(targetProcess.getName())) {
                    XtaProcess.Loc currentLoc = xtaState.getLocs().get(i);

                    for (XtaProcess.Edge edge : currentLoc.getOutEdges()) {
                        actions.add(XtaAction.basic(xtaSystem, xtaState.getLocs(), edge));
                    }
                }
            }
            return actions;
        };

        Predicate<XtaDfaState<Prod2State<ExplState, ZoneState>, S>> targetPred =
                s -> s.getDfaState() == null;

        UnitPrec unitPrec = UnitPrec.getInstance();
        Set<VarDecl<RatType>> activeClocks =
                xtaSystem.getClockVars().stream()
                        .filter(clock -> !clock.getName().contains(targetProcess.getName()))
                        .collect(Collectors.toSet());

        ZonePrec zonePrec = ZonePrec.of(activeClocks);
        Prod2Prec<UnitPrec, ZonePrec> prec = Prod2Prec.of(unitPrec, zonePrec);

        SafetyChecker<XtaDfaState<Prod2State<ExplState, ZoneState>, S>, XtaAction, Prod2Prec<UnitPrec, ZonePrec>> checker =
                XtaDfaCheckerFactory.create(searchStrategy, xtaDfaAnalysis, lts, targetPred, logger);

        SafetyResult<?, XtaAction> result = checker.check(prec);

        if (!result.isSafe()) {
            Trace<?, XtaAction> trace = result.asUnsafe().getTrace();
            List<String> counterExampleWord = new ArrayList<>();

            for (XtaAction action : trace.getActions()) {
                XtaProcess.Edge representativeEdge = null;

                if (action.isBasic()) representativeEdge = action.asBasic().getEdge();
                else if (action.isBinary()) representativeEdge = action.asBinary().getEmitEdge();
                else if (action.isBroadcast()) representativeEdge = action.asBroadcast().getEmitEdge();

                if (representativeEdge != null) {
                    String symbol = XtaTimingMapper.generateSymbolForEdge(representativeEdge);

                    if (alphabet.containsSymbol(symbol)) {
                        counterExampleWord.add(symbol);
                    }
                }
            }

            return new DefaultQuery<>(Word.fromList(counterExampleWord), false);
        }

        return null;
    }
}
