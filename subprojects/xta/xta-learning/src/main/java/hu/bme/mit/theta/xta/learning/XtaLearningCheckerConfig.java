package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.zone.BoundFunc;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleFactory;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmFactory;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.common.ProductAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.UntimedAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.compositional.inclusion.XtaInclusionOracle;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionAnalysis;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionCheckerFactory;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionLts;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionOrd;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionState;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionStrategy;
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

import java.util.*;
import java.util.function.Predicate;

public class XtaLearningCheckerConfig {
    private final XtaSystem xtaSystem;
    private final LearningAlgorithmType learningAlgorithmType;
    private final EQOracleType eqOracleType;
    private final DiscreteCompositionStrategy checkerStrategy;
    private final Refiner<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> refiner;
    private final int eqMaxDepth;
    private final int eqRandomMinLength;
    private final int eqRandomMaxLength;
    private final int eqRandomMaxTests;
    private final Logger logger;

    private XtaLearningCheckerConfig(XtaSystem xtaSystem,
                                     LearningAlgorithmType learningAlgorithmType,
                                     EQOracleType eqOracleType,
                                     DiscreteCompositionStrategy checkerStrategy,
                                     Refiner<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> refiner,
                                     int eqMaxDepth,
                                     int eqRandomMinLength,
                                     int eqRandomMaxLength,
                                     int eqRandomMaxTests,
                                     Logger logger) {
        this.xtaSystem = xtaSystem;
        this.learningAlgorithmType = learningAlgorithmType;
        this.eqOracleType = eqOracleType;
        this.checkerStrategy = checkerStrategy;
        this.refiner = refiner;
        this.eqMaxDepth = eqMaxDepth;
        this.eqRandomMinLength = eqRandomMinLength;
        this.eqRandomMaxLength = eqRandomMaxLength;
        this.eqRandomMaxTests = eqRandomMaxTests;
        this.logger = logger;
    }

    public static XtaLearningCheckerConfig create(XtaSystem xtaSystem,
                                                  LearningAlgorithmType learningAlgorithmType,
                                                  EQOracleType eqOracleType,
                                                  DiscreteCompositionStrategy checkerStrategy,
                                                  Refiner<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> refiner,
                                                  int eqMaxDepth,
                                                  int eqRandomMinLength,
                                                  int eqRandomMaxLength,
                                                  int eqRandomMaxTests,
                                                  Logger logger) {
        return new XtaLearningCheckerConfig(
                xtaSystem,
                learningAlgorithmType,
                eqOracleType,
                checkerStrategy,
                refiner,
                eqMaxDepth,
                eqRandomMinLength,
                eqRandomMaxLength,
                eqRandomMaxTests,
                logger
        );
    }

    public SafetyResult<DiscreteCompositionState<FastDFAState>, XtaAction> check() {
        Map<VarDecl<RatType>, Integer> ceilings = computeCeilings(xtaSystem);

        XtaTPrimeSul sul = XtaTPrimeSul.create(ceilings);
        XtaTimingMapper<Boolean, Boolean> mapper = XtaTimingMapper.create(xtaSystem, output -> output);
        Alphabet<String> alphabet = Alphabets.fromCollection(mapper.getAlphabet());

        FastDFA<String> untimedAutomaton = null;
        for (XtaProcess process : xtaSystem.getProcesses()) {
            FastDFA<String> procDFA = UntimedAutomatonBuilder.build(process, alphabet);

            Set<String> procSymbols = new HashSet<>();
            for (XtaProcess.Edge edge : process.getEdges()) {
                procSymbols.add(XtaTimingMapper.generateSymbolForEdge(edge));
            }

            for (FastDFAState state : procDFA.getStates()) {
                for (String sym : alphabet) {
                    if (!procSymbols.contains(sym) && procDFA.getTransition(state, sym) == null) {
                        procDFA.setTransition(state, sym, state);
                    }
                }
            }

            untimedAutomaton = (untimedAutomaton == null)
                    ? procDFA
                    : ProductAutomatonBuilder.buildProduct(untimedAutomaton, procDFA, alphabet);
        }

        SUL<String, Boolean> mappedSul = new MappedSUL<>(mapper, sul);
        FastDFA<String> finalUntimedAutomaton = untimedAutomaton;
        Map<Word<String>, Boolean> mqCache = new HashMap<>();
        MembershipOracle.DFAMembershipOracle<String> mqOracle = queries -> {
            for (Query<String, Boolean> query : queries) {
                Boolean cached = mqCache.get(query.getInput());
                if (cached != null) {
                    query.answer(cached);
                    continue;
                }
                boolean answer;
                if (!finalUntimedAutomaton.accepts(query.getInput())) {
                    answer = true;
                } else {
                    mappedSul.pre();
                    boolean accepted = true;
                    for (String symbol : query.getInput()) {
                        Boolean stepResult = mappedSul.step(symbol);
                        if (stepResult == null || !stepResult) { accepted = false; break; }
                    }
                    mappedSul.post();
                    answer = accepted;
                }
                mqCache.put(query.getInput(), answer);
                query.answer(answer);
            }
        };

        LearningAlgorithm.DFALearner<String> learner =
                LearningAlgorithmFactory.create(learningAlgorithmType, alphabet, mqOracle);
        BoundFunc luBounds = computeLuBounds(xtaSystem);
        XtaInclusionOracle inclusionOracle = XtaInclusionOracle.create(ceilings, mapper, luBounds, mqOracle);
        EquivalenceOracle<DFA<?, String>, String, Boolean> eqOracle =
                EQOracleFactory.create(
                        eqOracleType, mqOracle, inclusionOracle,
                        eqMaxDepth, eqRandomMinLength, eqRandomMaxLength, eqRandomMaxTests);

        ExplStmtAnalysis explStmtAnalysis = ExplStmtAnalysis.create(
                Z3SolverFactory.getInstance().createSolver(),
                xtaSystem.getInitVal().toExpr()
        );
        XtaAnalysis<ExplState, ExplPrec> xtaAnalysis = XtaAnalysis.create(xtaSystem, explStmtAnalysis);

        Predicate<DiscreteCompositionState<FastDFAState>> targetPred = state ->
                state.getXtaState().isError();

        logger.write(Logger.Level.MAINSTEP, "Starting learning-based model checking%n");
        logger.write(Logger.Level.INFO, "Alphabet size: %d%n", alphabet.size());
        learner.startLearning();

        int outerIteration = 0;
        while (true) {
            outerIteration++;
            logger.write(Logger.Level.MAINSTEP, "=== Outer iteration %d ===%n", outerIteration);

            long learningStart = System.currentTimeMillis();
            int innerRefinements = 0;
            while (true) {
                DFA<?, String> hypothesis = learner.getHypothesisModel();
                logger.write(Logger.Level.SUBSTEP,
                        "  [Learning] Hypothesis states: %d, running EQ oracle...%n",
                        hypothesis.size());
                DefaultQuery<String, Boolean> ceq = eqOracle.findCounterExample(hypothesis, alphabet);
                if (ceq == null) break;
                innerRefinements++;
                logger.write(Logger.Level.SUBSTEP,
                        "  [Learning] Counterexample found (length %d), refining...%n",
                        ceq.getInput().size());
                learner.refineHypothesis(ceq);
            }
            long learningMs = System.currentTimeMillis() - learningStart;
            logger.write(Logger.Level.MAINSTEP,
                    "  [Learning] Converged after %d refinements in %d ms. Hypothesis states: %d%n",
                    innerRefinements, learningMs, learner.getHypothesisModel().size());

            DFA<?, String> hypothesis = learner.getHypothesisModel();
            FastDFA<String> productDFA = ProductAutomatonBuilder.buildProduct(untimedAutomaton, hypothesis, alphabet);
            logger.write(Logger.Level.SUBSTEP,
                    "  [ModelCheck] Product DFA states: %d, starting CEGAR checker...%n",
                    productDFA.size());

            DiscreteCompositionAnalysis<FastDFAState, ExplPrec> analysis = DiscreteCompositionAnalysis.create(
                    DiscreteCompositionOrd.create(),
                    alphabet,
                    productDFA,
                    xtaAnalysis.getInitFunc(),
                    xtaAnalysis.getTransFunc()
            );

            DiscreteCompositionLts<FastDFAState> lts =
                    DiscreteCompositionLts.create(xtaSystem, alphabet, productDFA);

            SafetyChecker<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> checker =
                    DiscreteCompositionCheckerFactory.createCegar(
                            checkerStrategy, analysis, lts, targetPred, refiner);

            long modelCheckStart = System.currentTimeMillis();
            SafetyResult<DiscreteCompositionState<FastDFAState>, XtaAction> result =
                    checker.check(ExplPrec.empty());
            long modelCheckMs = System.currentTimeMillis() - modelCheckStart;
            logger.write(Logger.Level.MAINSTEP,
                    "  [ModelCheck] Result: %s in %d ms%n",
                    result.isSafe() ? "SAFE" : "UNSAFE (data-feasible cex)", modelCheckMs);

            if (result.isSafe()) {
                logger.write(Logger.Level.RESULT, "Result: SAFE (after %d outer iterations)%n", outerIteration);
                return result;
            }

            Trace<DiscreteCompositionState<FastDFAState>, XtaAction> cexTrace =
                    result.asUnsafe().getTrace();
            Word<String> cexWord = extractWord(cexTrace, alphabet);
            logger.write(Logger.Level.SUBSTEP,
                    "  [Realizability] Counterexample trace length: %d, word length: %d%n",
                    cexTrace.length(), cexWord.size());

            long realizabilityStart = System.currentTimeMillis();
            boolean realizable = checkMembership(mappedSul, cexWord);
            long realizabilityMs = System.currentTimeMillis() - realizabilityStart;
            logger.write(Logger.Level.MAINSTEP,
                    "  [Realizability] %s in %d ms%n",
                    realizable ? "REALIZABLE (true bug)" : "SPURIOUS (refining hypothesis)", realizabilityMs);

            if (realizable) {
                logger.write(Logger.Level.RESULT, "Result: UNSAFE (after %d outer iterations)%n", outerIteration);
                return result;
            }

            learner.refineHypothesis(new DefaultQuery<>(cexWord, false));
        }
    }

    private Word<String> extractWord(
            Trace<DiscreteCompositionState<FastDFAState>, XtaAction> trace,
            Alphabet<String> alphabet) {
        List<String> symbols = new ArrayList<>();
        for (XtaAction action : trace.getActions()) {
            for (XtaProcess.Edge edge : getEdgesForAction(action)) {
                String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
                if (alphabet.containsSymbol(symbol)) {
                    symbols.add(symbol);
                    break;
                }
            }
        }
        return Word.fromList(symbols);
    }

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

    private boolean checkMembership(SUL<String, Boolean> sul, Word<String> word) {
        sul.pre();
        try {
            for (String symbol : word) {
                Boolean result = sul.step(symbol);
                if (result == null || !result) return false;
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
                    if (guard.isClockGuard()) updateCeiling(ceilings, guard.asClockGuard());
                }
            }
            for (XtaProcess.Loc loc : process.getLocs()) {
                for (Guard inv : loc.getInvars()) {
                    if (inv.isClockGuard()) updateCeiling(ceilings, inv.asClockGuard());
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

    private static BoundFunc computeLuBounds(XtaSystem system) {
        BoundFunc.Builder builder = BoundFunc.builder();
        for (XtaProcess process : system.getProcesses()) {
            for (XtaProcess.Loc loc : process.getLocs()) {
                for (Guard guard : loc.getInvars()) {
                    if (guard.isClockGuard()) {
                        addClockConstrToBounds(builder, guard.asClockGuard().getClockConstr());
                    }
                }
            }
            for (XtaProcess.Edge edge : process.getEdges()) {
                for (Guard guard : edge.getGuards()) {
                    if (guard.isClockGuard()) {
                        addClockConstrToBounds(builder, guard.asClockGuard().getClockConstr());
                    }
                }
            }
        }
        return builder.build();
    }

    private static void addClockConstrToBounds(BoundFunc.Builder builder, ClockConstr constr) {
        try {
            builder.add(constr);
        } catch (UnsupportedOperationException e) {
            // Diagonális feltételek (pl. DiffGeqConstr: x - y >= c) nem támogatottak — kihagyás
        }
    }
}
