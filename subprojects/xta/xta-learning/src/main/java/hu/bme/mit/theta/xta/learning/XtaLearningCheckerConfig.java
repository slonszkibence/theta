package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.zone.BoundFunc;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
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
import hu.bme.mit.theta.xta.learning.compositional.realizability.*;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main orchestrator for the learning-based compositional model checking framework.
 * <p>
 * This class ties together all the components described in Ocan Sankur's architecture:
 * <ol>
 *   <li><b>The Learner:</b> Infers the timing behavior (hypothesis {@code H}) of the system.</li>
 *   <li><b>The Model Checker:</b> Evaluates the safety of the discrete abstraction
 *       composed with the learned hypothesis ({@code A || H}).</li>
 *   <li><b>The Oracles:</b> Answers Membership and Equivalence queries to guide the
 *       learning process, validating counterexamples along the way.</li>
 * </ol>
 *
 * @param <D> The internal data state type (e.g., ExplState or Prod2State).
 * @param <P> The precision type.
 */
public class XtaLearningCheckerConfig<D extends State, P extends Prec> {
    private final XtaSystem xtaSystem;
    private final LearningAlgorithmType learningAlgorithmType;
    private final EQOracleType eqOracleType;
    private final ModelCheckingStrategy<D, FastDFAState, P> modelCheckingStrategy;
    private final DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy;
    private final XtaAnalysis<D, P> xtaAnalysis;
    private final P initialPrec;
    private final PartialOrd<D> dataOrd;
    private final int eqMaxDepth;
    private final int eqRandomMinLength;
    private final int eqRandomMaxLength;
    private final int eqRandomMaxTests;
    private final Logger logger;

    // ── Inner record: CheckContext ─────────────────────────────────────────────

    /**
     * Immutable context holding all instantiated components for the current verification run.
     */
    private record CheckContext<D extends State, P extends Prec>(
            Alphabet<String> alphabet,
            SUL<String, Boolean> mappedSul,
            FastDFA<String> untimedAutomaton,
            MembershipOracle.DFAMembershipOracle<String> mqOracle,
            LearningAlgorithm.DFALearner<String> learner,
            EquivalenceOracle<DFA<?, String>, String, Boolean> eqOracle,
            XtaAnalysis<D, P> xtaAnalysis,
            long[] mqCounter
    ) {}

    // ── Internal utility records ─────────────────────────────────────────────────

    private record LearningPhaseResult(DFA<?, String> hypothesis, int refinements) {}

    private record ModelCheckPhaseResult<D extends State>(
            SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> safetyResult,
            int productDfaStates) {}

    /**
     * Extended result object containing both the formal safety verdict and
     * execution statistics.
     */
    public record CheckResult<D extends State>(
            SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> safetyResult,
            LearningStatistics statistics) {}

    private XtaLearningCheckerConfig(
            XtaSystem xtaSystem,
            LearningAlgorithmType learningAlgorithmType,
            EQOracleType eqOracleType,
            ModelCheckingStrategy<D, FastDFAState, P> modelCheckingStrategy,
            DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy,
            XtaAnalysis<D, P> xtaAnalysis,
            P initialPrec,
            PartialOrd<D> dataOrd,
            int eqMaxDepth,
            int eqRandomMinLength,
            int eqRandomMaxLength,
            int eqRandomMaxTests,
            Logger logger
    ) {
        this.xtaSystem = checkNotNull(xtaSystem);
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        this.eqOracleType = checkNotNull(eqOracleType);
        this.modelCheckingStrategy = checkNotNull(modelCheckingStrategy);
        this.searchStrategy = checkNotNull(searchStrategy);
        this.xtaAnalysis = checkNotNull(xtaAnalysis);
        this.initialPrec = checkNotNull(initialPrec);
        this.dataOrd = checkNotNull(dataOrd);
        this.eqMaxDepth = eqMaxDepth;
        this.eqRandomMinLength = eqRandomMinLength;
        this.eqRandomMaxLength = eqRandomMaxLength;
        this.eqRandomMaxTests = eqRandomMaxTests;
        this.logger = logger;
    }

    public static <D extends State, P extends Prec> XtaLearningCheckerConfig<D, P> create(
            XtaSystem xtaSystem,
            LearningAlgorithmType learningAlgorithmType,
            EQOracleType eqOracleType,
            ModelCheckingStrategy<D, FastDFAState, P> modelCheckingStrategy,
            DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy,
            XtaAnalysis<D, P> xtaAnalysis,
            P initialPrec,
            PartialOrd<D> dataOrd,
            int eqMaxDepth,
            int eqRandomMinLength,
            int eqRandomMaxLength,
            int eqRandomMaxTests,
            Logger logger
    ) {
        return new XtaLearningCheckerConfig<>(
                xtaSystem, learningAlgorithmType, eqOracleType,
                modelCheckingStrategy, searchStrategy,
                xtaAnalysis, initialPrec, dataOrd,
                eqMaxDepth, eqRandomMinLength, eqRandomMaxLength, eqRandomMaxTests,
                logger);
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Executes the main learning-based verification loop.
     *
     * @return The standard safety result.
     */
    public SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> check() {
        return checkWithStats().safetyResult();
    }

    /**
     * Executes the main learning-based verification loop and returns statistics.
     *
     * @return The check result including execution statistics.
     */
    public CheckResult<D> checkWithStats() {
        CheckContext<D, P> ctx = buildContext();

        logger.write(Logger.Level.INFO,
                "[Learning] Starting — alphabet size: %d, algorithm: %s, EQ oracle: %s%n",
                ctx.alphabet().size(), learningAlgorithmType, eqOracleType);

        ctx.learner().startLearning();

        int outerIteration = 0;
        int totalRefinements = 0;
        int lastHypothesisStates = 0;
        int lastProductDfaStates = 0;
        long startTime = System.currentTimeMillis();

        while (true) {
            outerIteration++;
            logger.write(Logger.Level.MAINSTEP,
                    "%n=== Outer iteration #%d (elapsed: %d ms, MQ: %d) ===%n",
                    outerIteration, System.currentTimeMillis() - startTime, ctx.mqCounter()[0]);

            // Phase 1: Learn Hypothesis
            LearningPhaseResult lpr = runLearningPhase(ctx);
            lastHypothesisStates = lpr.hypothesis().size();
            totalRefinements += lpr.refinements();

            // Phase 2: Model Check A || H
            ModelCheckPhaseResult<D> mcr = runModelCheckingPhase(ctx, lpr.hypothesis());
            lastProductDfaStates = mcr.productDfaStates();

            // If the discrete model is safe, the real timed system is guaranteed to be safe
            if (mcr.safetyResult().isSafe()) {
                logger.write(Logger.Level.RESULT,
                        "Result: SAFE (%d outer iterations, %d MQ, %d ms)%n",
                        outerIteration, ctx.mqCounter()[0], System.currentTimeMillis() - startTime);
                return new CheckResult<>(mcr.safetyResult(),
                        new LearningStatistics(lastHypothesisStates, lastProductDfaStates,
                                totalRefinements, outerIteration, ctx.mqCounter()[0]));
            }

            // Phase 3: Validate Counterexample
            Word<String> cexWord = extractWord(mcr.safetyResult().asUnsafe().getTrace(), ctx.alphabet());
            logger.write(Logger.Level.MAINSTEP,
                    "  [Counterexample] Length: %d, checking realizability...%n", cexWord.size());

            if (checkRealizability(ctx.mappedSul(), ctx.untimedAutomaton(), cexWord)) {
                logger.write(Logger.Level.RESULT,
                        "Result: UNSAFE (%d outer iterations, %d MQ, %d ms)%n",
                        outerIteration, ctx.mqCounter()[0], System.currentTimeMillis() - startTime);
                return new CheckResult<>(mcr.safetyResult(),
                        new LearningStatistics(lastHypothesisStates, lastProductDfaStates,
                                totalRefinements, outerIteration, ctx.mqCounter()[0]));
            }

            // The counterexample is spurious (timing infeasible). Refine the hypothesis.
            logger.write(Logger.Level.MAINSTEP,
                    "  [Spurious] Refining hypothesis with word (length %d)%n", cexWord.size());
            ctx.learner().refineHypothesis(new DefaultQuery<>(cexWord, false));
        }
    }

    // ── Learning Phase ─────────────────────────────────────────────────────────

    private LearningPhaseResult runLearningPhase(CheckContext<D, P> ctx) {
        long start = System.currentTimeMillis();
        int refinements = 0;

        while (true) {
            DFA<?, String> hypothesis = ctx.learner().getHypothesisModel();
            logger.write(Logger.Level.SUBSTEP,
                    "  [Learning] Hypothesis states: %d, running EQ oracle...%n", hypothesis.size());

            DefaultQuery<String, Boolean> ceq =
                    ctx.eqOracle().findCounterExample(hypothesis, ctx.alphabet());
            if (ceq == null) {
                logger.write(Logger.Level.MAINSTEP,
                        "  [Learning] Converged after %d refinements in %d ms. States: %d%n",
                        refinements, System.currentTimeMillis() - start, hypothesis.size());
                return new LearningPhaseResult(hypothesis, refinements);
            }

            refinements++;
            logger.write(Logger.Level.SUBSTEP,
                    "  [Learning] Counterexample (length %d), refining...%n", ceq.getInput().size());
            ctx.learner().refineHypothesis(ceq);
        }
    }

    // ── Model Checking Phase ────────────────────────────────────────────────

    private static final int MODEL_CHECK_LOG_INTERVAL_SEC = 5;

    private ModelCheckPhaseResult<D> runModelCheckingPhase(
            CheckContext<D, P> ctx, DFA<?, String> hypothesis) {
        FastDFA<String> productDFA = ProductAutomatonBuilder.buildProduct(
                ctx.untimedAutomaton(), hypothesis, ctx.alphabet());
        logger.write(Logger.Level.SUBSTEP,
                "  [ModelCheck] Product DFA states: %d, starting checker...%n", productDFA.size());

        SafetyChecker<DiscreteCompositionState<D, FastDFAState>, XtaAction, P> checker =
                buildChecker(ctx, productDFA);

        long start = System.currentTimeMillis();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "modelcheck-progress");
            t.setDaemon(true);
            return t;
        });
        ScheduledFuture<?> progressTask = scheduler.scheduleAtFixedRate(() ->
                        logger.write(Logger.Level.SUBSTEP,
                                "  [ModelCheck] Still running... (%d s elapsed)%n",
                                (System.currentTimeMillis() - start) / 1000),
                MODEL_CHECK_LOG_INTERVAL_SEC, MODEL_CHECK_LOG_INTERVAL_SEC, TimeUnit.SECONDS);

        SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> result;
        try {
            result = checker.check(initialPrec);
        } finally {
            progressTask.cancel(false);
            scheduler.shutdown();
        }

        logger.write(Logger.Level.MAINSTEP,
                "  [ModelCheck] Result: %s in %d ms%n",
                result.isSafe() ? "SAFE" : "UNSAFE", System.currentTimeMillis() - start);
        return new ModelCheckPhaseResult<>(result, productDFA.size());
    }

    // ── Realizability Checking ────────────────────────────────────────────

    private boolean checkRealizability(
            SUL<String, Boolean> sul, FastDFA<String> untimedAutomaton, Word<String> cexWord) {
        long start = System.currentTimeMillis();
        if (!untimedAutomaton.accepts(cexWord)) {
            logger.write(Logger.Level.MAINSTEP,
                    "  [Realizability] SPURIOUS (untimed automaton rejects cex) in %d ms%n",
                    System.currentTimeMillis() - start);
            return false;
        }
        boolean realizable = runMembershipQuery(sul, cexWord);
        logger.write(Logger.Level.MAINSTEP,
                "  [Realizability] %s in %d ms%n",
                realizable ? "REALIZABLE (true bug)" : "SPURIOUS (refining hypothesis)",
                System.currentTimeMillis() - start);
        return realizable;
    }

    // ── Context Initialization ───────────────────────────────────────────────────

    private CheckContext<D, P> buildContext() {
        Map<VarDecl<RatType>, Integer> ceilings = computeCeilings(xtaSystem);
        BoundFunc luBounds = computeLuBounds(xtaSystem);

        XtaTPrimeSul sul = XtaTPrimeSul.create(ceilings);
        XtaTimingMapper<Boolean, Boolean> mapper = XtaTimingMapper.create(xtaSystem, output -> output);
        Alphabet<String> alphabet = Alphabets.fromCollection(mapper.getAlphabet());
        SUL<String, Boolean> mappedSul = new MappedSUL<>(mapper, sul);

        FastDFA<String> untimedAutomaton = buildUntimedAutomaton(alphabet);
        long[] mqCounter = {0};
        MembershipOracle.DFAMembershipOracle<String> mqOracle =
                buildMqOracle(mappedSul, untimedAutomaton, mqCounter);

        LearningAlgorithm.DFALearner<String> learner =
                LearningAlgorithmFactory.create(learningAlgorithmType, alphabet, mqOracle);
        EquivalenceOracle<DFA<?, String>, String, Boolean> eqOracle =
                EQOracleFactory.create(eqOracleType, mqOracle,
                        XtaInclusionOracle.create(ceilings, mapper, luBounds, mqOracle, logger),
                        eqMaxDepth, eqRandomMinLength, eqRandomMaxLength, eqRandomMaxTests);

        logger.write(Logger.Level.INFO,
                "[Context] Untimed automaton states: %d, alphabet size: %d%n",
                untimedAutomaton != null ? untimedAutomaton.size() : 0, alphabet.size());

        return new CheckContext<>(
                alphabet, mappedSul, untimedAutomaton,
                mqOracle, learner, eqOracle, xtaAnalysis, mqCounter);
    }

    // ── Checker Builder ─────────────────────────────────────────────────────

    private SafetyChecker<DiscreteCompositionState<D, FastDFAState>, XtaAction, P> buildChecker(
            CheckContext<D, P> ctx, FastDFA<String> productDFA) {
        Predicate<DiscreteCompositionState<D, FastDFAState>> targetPred =
                state -> state.getXtaState().isError();

        DiscreteCompositionAnalysis<D, FastDFAState, P> analysis =
                DiscreteCompositionAnalysis.create(
                        DiscreteCompositionOrd.create(dataOrd),
                        ctx.alphabet(),
                        productDFA,
                        ctx.xtaAnalysis().getInitFunc(),
                        ctx.xtaAnalysis().getTransFunc());

        DiscreteCompositionLts<D, FastDFAState> lts =
                DiscreteCompositionLts.create(xtaSystem, ctx.alphabet(), productDFA);

        return DiscreteCompositionCheckerFactory.create(
                modelCheckingStrategy, searchStrategy, analysis, lts, targetPred);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private FastDFA<String> buildUntimedAutomaton(Alphabet<String> alphabet) {
        FastDFA<String> result = null;
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
            result = (result == null)
                    ? procDFA
                    : ProductAutomatonBuilder.buildProduct(result, procDFA, alphabet);
        }
        return result;
    }

    private static final int MQ_LOG_INTERVAL = 200;

    private MembershipOracle.DFAMembershipOracle<String> buildMqOracle(
            SUL<String, Boolean> mappedSul, FastDFA<String> untimedAutomaton, long[] mqCounter) {
        Map<Word<String>, Boolean> cache = new HashMap<>();
        return queries -> {
            for (Query<String, Boolean> query : queries) {
                query.answer(cache.computeIfAbsent(query.getInput(), word -> {
                    mqCounter[0]++;
                    if (mqCounter[0] % MQ_LOG_INTERVAL == 0) {
                        logger.write(Logger.Level.SUBSTEP,
                                "  [MQ] #%d query (cache: %d entries, word length: %d)%n",
                                mqCounter[0], cache.size(), word.size());
                    }
                    return queryMembership(mappedSul, untimedAutomaton, word);
                }));
            }
        };
    }

    private boolean queryMembership(
            SUL<String, Boolean> sul, FastDFA<String> untimedAutomaton, Word<String> word) {
        if (!untimedAutomaton.accepts(word)) return true;
        return runMembershipQuery(sul, word);
    }

    private boolean runMembershipQuery(SUL<String, Boolean> sul, Word<String> word) {
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

    private Word<String> extractWord(
            Trace<DiscreteCompositionState<D, FastDFAState>, XtaAction> trace,
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

    // ── Clock Bound Calculations ──────────────────────────────────────────────────

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

    private static void updateCeiling(
            Map<VarDecl<RatType>, Integer> ceilings, Guard.ClockGuard guard) {
        ClockConstr constr = guard.getClockConstr();
        if (constr instanceof AtomicConstr ac) {
            int bound = ac.getBound();
            for (VarDecl<RatType> variable : constr.getVars()) {
                ceilings.merge(variable, bound, Math::max);
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
