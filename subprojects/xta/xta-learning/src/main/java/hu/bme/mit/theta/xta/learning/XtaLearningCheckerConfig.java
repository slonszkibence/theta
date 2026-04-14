package hu.bme.mit.theta.xta.learning;

import de.learnlib.util.Experiment;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.expl.XtaExplAnalysis;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleFactory;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmFactory;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.common.ProductAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.UntimedAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.compositional.inclusion.XtaInclusionOracle;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionState;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionStrategy;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionLts;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionAnalysis;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionCheckerFactory;
import hu.bme.mit.theta.xta.learning.compositional.sul.XtaTPrimeSul;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.mapper.MappedSUL;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import de.learnlib.sul.SUL;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class XtaLearningCheckerConfig {
    private final XtaSystem xtaSystem;
    private final LearningAlgorithmType learningAlgorithmType;
    private final EQOracleType eqOracleType;
    private final DiscreteCompositionStrategy checkerStrategy;
    private final int eqMaxDepth;
    private final int eqRandomMinLength;
    private final int eqRandomMaxLength;
    private final int eqRandomMaxTests;

    private XtaLearningCheckerConfig(XtaSystem xtaSystem,
                                     LearningAlgorithmType learningAlgorithmType,
                                     EQOracleType eqOracleType,
                                     DiscreteCompositionStrategy checkerStrategy,
                                     int eqMaxDepth,
                                     int eqRandomMinLength,
                                     int eqRandomMaxLength,
                                     int eqRandomMaxTests) {
        this.xtaSystem = xtaSystem;
        this.learningAlgorithmType = learningAlgorithmType;
        this.eqOracleType = eqOracleType;
        this.checkerStrategy = checkerStrategy;
        this.eqMaxDepth = eqMaxDepth;
        this.eqRandomMinLength = eqRandomMinLength;
        this.eqRandomMaxLength = eqRandomMaxLength;
        this.eqRandomMaxTests = eqRandomMaxTests;
    }

    public static XtaLearningCheckerConfig create(XtaSystem xtaSystem,
                                                  LearningAlgorithmType learningAlgorithmType,
                                                  EQOracleType eqOracleType,
                                                  DiscreteCompositionStrategy checkerStrategy,
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

    public SafetyResult<DiscreteCompositionState<FastDFAState>, XtaAction> check() {
        XtaProcess firstProcess = xtaSystem.getProcesses().get(0);

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
        EquivalenceOracle<DFA<?, String>, String, Boolean> eqOracle =
                EQOracleFactory.create(
                        eqOracleType,
                        mqOracle,
                        inclusionOracle,
                        eqMaxDepth,
                        eqRandomMinLength,
                        eqRandomMaxLength,
                        eqRandomMaxTests);

        Experiment.DFAExperiment<String> experiment = new Experiment.DFAExperiment<>(learner, eqOracle, alphabet);
        experiment.run();
        DFA<?, String> resultDFA = experiment.getFinalHypothesis();

        FastDFA<String> productDFA = ProductAutomatonBuilder.buildProduct(untimedAutomaton, resultDFA, alphabet);


        Predicate<DiscreteCompositionState<FastDFAState>> targetPred = state ->
            state.getXtaState().isError();

        XtaExplAnalysis explAnalysis = XtaExplAnalysis.create(xtaSystem);
        XtaAnalysis<ExplState, UnitPrec> xtaAnalysis = XtaAnalysis.create(xtaSystem, explAnalysis);

        DiscreteCompositionAnalysis<FastDFAState, UnitPrec> analysis = DiscreteCompositionAnalysis.create(
                DiscreteCompositionState::equals,
                alphabet,
                productDFA,
                xtaAnalysis.getInitFunc(),
                xtaAnalysis.getTransFunc()
        );

        DiscreteCompositionLts<FastDFAState> lts = DiscreteCompositionLts.create(xtaSystem, alphabet, productDFA);

        SafetyChecker<DiscreteCompositionState<FastDFAState>, XtaAction, UnitPrec> checker =
                DiscreteCompositionCheckerFactory.create(
                        checkerStrategy,
                        analysis,
                        lts,
                        targetPred
                );

        return checker.check(UnitPrec.getInstance());
    }

    private static Map<VarDecl<RatType>, Integer> computeCeilings(XtaSystem system) {
        Map<VarDecl<RatType>, Integer> ceilings = new HashMap<>();
        for (VarDecl<RatType> clock : system.getClockVars()) {
            ceilings.put(clock, 5);
        }
        return ceilings;
    }
}
