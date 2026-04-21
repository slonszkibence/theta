package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.Guard;
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
import net.automatalib.word.Word;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.mapper.MappedSUL;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import de.learnlib.query.Query;
import de.learnlib.sul.SUL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        Predicate<DiscreteCompositionState<FastDFAState>> targetPred = state ->
                state.getXtaState().isError();

        XtaExplAnalysis explAnalysis = XtaExplAnalysis.create(xtaSystem);
        XtaAnalysis<ExplState, UnitPrec> xtaAnalysis = XtaAnalysis.create(xtaSystem, explAnalysis);

        // Start the learning process
        learner.startLearning();

        while (true) {
            // Inner learning loop: refine hypothesis until EQ oracle finds no more counterexamples
            while (true) {
                DFA<?, String> hypothesis = learner.getHypothesisModel();
                DefaultQuery<String, Boolean> ceq = eqOracle.findCounterExample(hypothesis, alphabet);
                if (ceq == null) break;
                learner.refineHypothesis(ceq);
            }

            // Build the product automaton A||H from the current hypothesis
            DFA<?, String> hypothesis = learner.getHypothesisModel();
            FastDFA<String> productDFA = ProductAutomatonBuilder.buildProduct(untimedAutomaton, hypothesis, alphabet);

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

            SafetyResult<DiscreteCompositionState<FastDFAState>, XtaAction> result =
                    checker.check(UnitPrec.getInstance());

            if (result.isSafe()) {
                // L(A||H) satisfies the spec → SAFE (since L(T) ⊆ L(H) after EQ oracle converged)
                return result;
            }

            // The model checker found a candidate counterexample. Run realizability check:
            // is this counterexample timing-feasible in T?
            Trace<DiscreteCompositionState<FastDFAState>, XtaAction> cexTrace =
                    result.asUnsafe().getTrace();
            Word<String> cexWord = extractWord(cexTrace, alphabet);

            boolean realizable = checkMembership(mappedSul, cexWord);

            if (realizable) {
                // cexWord ∈ L(T): real counterexample, the system is truly UNSAFE
                return result;
            }

            // cexWord ∉ L(T): spurious counterexample caused by H over-approximating L(T).
            // Refine the learner with (cexWord, false) so H excludes this spurious word.
            learner.refineHypothesis(new DefaultQuery<>(cexWord, false));
        }
    }

    /**
     * Extracts the abstract symbol word from a counterexample trace by mapping
     * each XtaAction to its edge symbol (if the symbol is in the learning alphabet).
     */
    private Word<String> extractWord(
            Trace<DiscreteCompositionState<FastDFAState>, XtaAction> trace,
            Alphabet<String> alphabet) {
        List<String> symbols = new ArrayList<>();
        for (XtaAction action : trace.getActions()) {
            List<XtaProcess.Edge> edges = getEdgesForAction(action);
            for (XtaProcess.Edge edge : edges) {
                String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
                if (alphabet.containsSymbol(symbol)) {
                    symbols.add(symbol);
                    break;
                }
            }
        }
        return Word.fromList(symbols);
    }

    /**
     * Returns the list of XTA edges involved in a single XtaAction
     * (basic, binary sync, or broadcast).
     */
    private List<XtaProcess.Edge> getEdgesForAction(XtaAction action) {
        List<XtaProcess.Edge> edges = new ArrayList<>();
        if (action.isBasic()) {
            edges.add(action.asBasic().getEdge());
        } else if (action.isBinary()) {
            edges.add(action.asBinary().getEmitEdge());
            edges.add(action.asBinary().getRecvEdge());
        } else if (action.isBroadcast()) {
            edges.add(action.asBroadcast().getEmitEdge());
            edges.addAll(action.asBroadcast().getRecvEdges());
        }
        return edges;
    }

    /**
     * Simulates the given word on the T' SUL (via the mappedSul) and returns
     * {@code true} if all steps are timing-feasible (i.e., the word is in L(T)).
     */
    private boolean checkMembership(SUL<String, Boolean> sul, Word<String> word) {
        sul.pre();
        try {
            for (String symbol : word) {
                Boolean result = sul.step(symbol);
                if (result == null || !result) {
                    return false;
                }
            }
            return true;
        } finally {
            sul.post();
        }
    }

    private static Map<VarDecl<RatType>, Integer> computeCeilings(XtaSystem system) {
        Map<VarDecl<RatType>, Integer> ceilings = new HashMap<>();
        for (VarDecl<RatType> clock : system.getClockVars()) {
            ceilings.put(clock, 0);
        }
        for (XtaProcess process : system.getProcesses()) {
            for (XtaProcess.Edge edge : process.getEdges()) {
                for (Guard guard : edge.getGuards()) {
                    if (guard.isClockGuard()) {
                        updateCeiling(ceilings, guard.asClockGuard());
                    }
                }
            }
            for (XtaProcess.Loc loc : process.getLocs()) {
                for (Guard inv : loc.getInvars()) {
                    if (inv.isClockGuard()) {
                        updateCeiling(ceilings, inv.asClockGuard());
                    }
                }
            }
        }
        return ceilings;
    }

    private static void updateCeiling(Map<VarDecl<RatType>, Integer> ceilings, Guard.ClockGuard guard) {
        ClockConstr constr = guard.getClockConstr();
        if (constr instanceof AtomicConstr ac) {
            int bound = ac.getBound();
            for (VarDecl<RatType> var : constr.getVars()) {
                ceilings.merge(var, bound, Math::max);
            }
        }
    }
}
