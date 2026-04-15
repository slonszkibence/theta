package hu.bme.mit.theta.xta.learning;

import de.learnlib.util.Experiment;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.XtaLts;
import hu.bme.mit.theta.xta.analysis.expl.XtaExplAnalysis;
import hu.bme.mit.theta.xta.analysis.zone.XtaZoneAnalysis;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleFactory;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmFactory;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.common.UntimedAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.compositional.dfa.*;
import hu.bme.mit.theta.xta.learning.compositional.inclusion.XtaInclusionOracle;
import hu.bme.mit.theta.xta.learning.compositional.sul.XtaTPrimeSul;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.mapper.MappedSUL;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import de.learnlib.sul.SUL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public class XtaLearningCheckerConfig {
    private final XtaSystem xtaSystem;
    private final LearningAlgorithmType learningAlgorithmType;
    private final EQOracleType eqOracleType;
    private final XtaDfaCheckerStrategy checkerStrategy;
    private final int eqMaxDepth;
    private final int eqRandomMinLength;
    private final int eqRandomMaxLength;
    private final int eqRandomMaxTests;

    private XtaLearningCheckerConfig(XtaSystem xtaSystem,
                                     LearningAlgorithmType learningAlgorithmType,
                                     EQOracleType eqOracleType,
                                     XtaDfaCheckerStrategy checkerStrategy,
                                     int eqMaxDepth,
                                     int eqRandomMinLength,
                                     int eqRandomMaxLength,
                                     int eqRandomMaxTests) {
        this.xtaSystem = checkNotNull(xtaSystem);
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        this.eqOracleType = checkNotNull(eqOracleType);
        this.checkerStrategy = checkNotNull(checkerStrategy);
        this.eqMaxDepth = eqMaxDepth;
        this.eqRandomMinLength = eqRandomMinLength;
        this.eqRandomMaxLength = eqRandomMaxLength;
        this.eqRandomMaxTests = eqRandomMaxTests;
    }

    public static XtaLearningCheckerConfig create(
            XtaSystem xtaSystem,
            LearningAlgorithmType learningAlgorithmType,
            EQOracleType eqOracleType,
            XtaDfaCheckerStrategy checkerStrategy,
            int eqMaxDepth,
            int eqRandomMinLength,
            int eqRandomMaxLength,
            int eqRandomMaxTests) {
        return new XtaLearningCheckerConfig(
                xtaSystem,
                learningAlgorithmType,
                eqOracleType,
                checkerStrategy,
                eqMaxDepth,
                eqRandomMinLength,
                eqRandomMaxLength,
                eqRandomMaxTests
        );
    }

    public SafetyResult<?, XtaAction> check() {
        XtaProcess firstProcess = xtaSystem.getProcesses().get(0);

        Set<String> shieldSymbols = new HashSet<>();
        for (XtaProcess.Loc loc : firstProcess.getLocs()) {
            for (XtaProcess.Edge edge : loc.getOutEdges()) {
                shieldSymbols.add(XtaTimingMapper.generateSymbolForEdge(edge));
            }
        }

        Map<VarDecl<RatType>, Integer> ceilings = computeCeilings(xtaSystem);
        XtaTPrimeSul sul = XtaTPrimeSul.create(ceilings);
        XtaTimingMapper<Boolean, Boolean> mapper = XtaTimingMapper.create(xtaSystem, output -> output);
        Alphabet<String> alphabet = Alphabets.fromCollection(mapper.getAlphabet());

        FastDFA<String> untimedAutomaton = UntimedAutomatonBuilder.build(firstProcess, alphabet);
        SUL<String, Boolean> mappedSul = new MappedSUL<>(mapper, sul);

        MembershipOracle.DFAMembershipOracle<String> mqOracle = queries -> {
            for (Query<String, Boolean> query : queries) {
                if (!untimedAutomaton.accepts(query.getInput())) {
                    query.answer(true);
                    continue;
                }
                mappedSul.pre();
                boolean accepted = true;
                for (String symbol : query.getInput()) {
                    Boolean stepResult = mappedSul.step(symbol);
                    if (stepResult == null || !stepResult) {
                        accepted = false;
                        break;
                    }
                }
                query.answer(accepted);
                mappedSul.post();
            }
        };

        LearningAlgorithm.DFALearner<String> learner = LearningAlgorithmFactory.create(learningAlgorithmType, alphabet, mqOracle);
        XtaInclusionOracle inclusionOracle = XtaInclusionOracle.create(ceilings, mapper);

        ModelCheckingEQOracle modelCheckingEqOracle = new ModelCheckingEQOracle(
                xtaSystem, alphabet, inclusionOracle, mappedSul, checkerStrategy, shieldSymbols
        );

        Experiment.DFAExperiment<String> experiment = new Experiment.DFAExperiment<>(learner, modelCheckingEqOracle, alphabet);

        PartialOrd<hu.bme.mit.theta.analysis.State> dummyOrd = (s1, s2) -> false;
        ARG<hu.bme.mit.theta.analysis.State, XtaAction> dummyArg =
                ARG.create(dummyOrd);

        try {
            experiment.run();
            return SafetyResult.safe(dummyArg);

        } catch (RealBugFoundException e) {
            @SuppressWarnings("unchecked")
            Trace<State, XtaAction> trace =
                    (Trace<State, XtaAction>) e.getTrace();

            return SafetyResult.unsafe(trace, dummyArg);
        }
    }

    private static Map<VarDecl<RatType>, Integer> computeCeilings(XtaSystem system) {
        Map<VarDecl<RatType>, Integer> ceilings = new HashMap<>();
        for (VarDecl<RatType> clock : system.getClockVars()) {
            ceilings.put(clock, 5);
        }
        return ceilings;
    }
}
