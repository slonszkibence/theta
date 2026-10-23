package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarStatistics;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmFactory;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;
import hu.bme.mit.theta.xta.learning.compositional.common.ProductAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.ZoneSemantics;
import hu.bme.mit.theta.xta.learning.compositional.inclusion.XtaInclusionOracle;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.*;
import hu.bme.mit.theta.xta.learning.compositional.membership.XtaTimedSul;
import hu.bme.mit.theta.xta.learning.compositional.realizability.XtaRealizabilityOracle;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.SupportsGrowingAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
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
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class XtaLearningCheckerConfig<D extends State, P extends Prec> {
    private final XtaSystem xtaSystem;
    private final LearningAlgorithmType learningAlgorithmType;
    private final ModelCheckingStrategy<D, FastDFAState, P> modelCheckingStrategy;
    private final DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy;
    private final XtaAnalysis<D, P> xtaAnalysis;
    private final P initialPrec;
    private final TransFunc<D, ? super StmtAction, ? super P> dataTransFunc;
    private final PartialOrd<D> dataOrd;
    private final InclusionSearchStrategy inclusionSearchStrategy;
    private final int maxLearningIterations;
    private final int maxOuterIterations;
    private final Logger logger;

    private record CheckContext<D extends State, P extends Prec>(
            Alphabet<String> alphabet,
            ZoneSemantics semantics,
            TimedStepper stepper,
            MembershipOracle.DFAMembershipOracle<String> mqOracle,
            EquivalenceOracle<DFA<?, String>, String, Boolean> eqOracle,
            XtaRealizabilityOracle realizabilityOracle,
            XtaAnalysis<D, P> xtaAnalysis,
            long[] mqCounter,
            long[] sulCounter,
            long[] eqCounter
    ) {}

    private record LearningPhaseResult(DFA<?, String> hypothesis, int refinements) {}

    private static final class Timings {
        private long setupMs;
        private long learningMs;
        private long modelCheckingMs;
        private long refinerMs;
        private long realizabilityMs;
        private int cegarIterations;
    }

    private record ModelCheckPhaseResult<D extends State>(
            SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> safetyResult,
            int productDfaStates) {}

    public record CheckResult<D extends State>(
            SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> safetyResult,
            LearningStatistics statistics) {}

    private XtaLearningCheckerConfig(
            XtaSystem xtaSystem,
            LearningAlgorithmType learningAlgorithmType,
            ModelCheckingStrategy<D, FastDFAState, P> modelCheckingStrategy,
            DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy,
            XtaAnalysis<D, P> xtaAnalysis,
            TransFunc<D, ? super StmtAction, ? super P> dataTransFunc,
            P initialPrec,
            PartialOrd<D> dataOrd,
            InclusionSearchStrategy inclusionSearchStrategy,
            int maxLearningIterations,
            int maxOuterIterations,
            Logger logger
    ) {
        checkArgument(maxLearningIterations > 0, "maxLearningIterations must be positive");
        checkArgument(maxOuterIterations > 0, "maxOuterIterations must be positive");
        this.maxLearningIterations = maxLearningIterations;
        this.maxOuterIterations = maxOuterIterations;
        this.inclusionSearchStrategy = checkNotNull(inclusionSearchStrategy);
        this.xtaSystem = checkNotNull(xtaSystem);
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        this.modelCheckingStrategy = checkNotNull(modelCheckingStrategy);
        this.searchStrategy = checkNotNull(searchStrategy);
        this.xtaAnalysis = checkNotNull(xtaAnalysis);
        this.dataTransFunc = checkNotNull(dataTransFunc);
        this.initialPrec = checkNotNull(initialPrec);
        this.dataOrd = checkNotNull(dataOrd);
        this.logger = logger;
    }

    public enum InclusionSearchStrategy {
        BFS(SearchStrategy.BFS),
        DFS(SearchStrategy.DFS),
        RANDOM(SearchStrategy.RANDOM);

        private final SearchStrategy strategy;

        InclusionSearchStrategy(final SearchStrategy strategy) {
            this.strategy = strategy;
        }

        public SearchStrategy strategy() {
            return strategy;
        }
    }

    public static <D extends State, P extends Prec> XtaLearningCheckerConfig<D, P> create(
            XtaSystem xtaSystem,
            LearningAlgorithmType learningAlgorithmType,
            ModelCheckingStrategy<D, FastDFAState, P> modelCheckingStrategy,
            DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy,
            XtaAnalysis<D, P> xtaAnalysis,
            TransFunc<D, ? super StmtAction, ? super P> dataTransFunc,
            P initialPrec,
            PartialOrd<D> dataOrd,
            InclusionSearchStrategy inclusionSearchStrategy,
            int maxLearningIterations,
            int maxOuterIterations,
            Logger logger
    ) {
        return new XtaLearningCheckerConfig<>(
                xtaSystem, learningAlgorithmType,
                modelCheckingStrategy, searchStrategy,
                xtaAnalysis, dataTransFunc, initialPrec, dataOrd,
                inclusionSearchStrategy, maxLearningIterations, maxOuterIterations,
                logger);
    }

    public SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> check() {
        return checkWithStats().safetyResult();
    }

    public CheckResult<D> checkWithStats() {
        final long startTime = System.currentTimeMillis();
        final Timings timings = new Timings();
        CheckContext<D, P> ctx = buildContext();
        timings.setupMs = System.currentTimeMillis() - startTime;

        logger.write(Logger.Level.INFO,
                "[Learning] Starting with a lazily discovered alphabet, algorithm: %s%n",
                learningAlgorithmType);

        LearningAlgorithm.DFALearner<String> learner = null;
        Alphabet<String> sigma = snapshot(ctx.stepper());

        final Map<String, XtaAction> discovered = new LinkedHashMap<>();
        final DiscreteCompositionTransFunc.UnknownSymbolHandler collector = discovered::putIfAbsent;

        int outerIteration = 0;
        int totalRefinements = 0;
        int lastHypothesisStates = 0;
        int lastProductDfaStates = 0;

        while (outerIteration < maxOuterIterations) {
            outerIteration++;
            logger.write(Logger.Level.MAINSTEP,
                    "%n=== Outer iteration #%d (elapsed: %d ms, MQ: %d) ===%n",
                    outerIteration, System.currentTimeMillis() - startTime, ctx.mqCounter()[0]);

            final DFA<?, String> hypothesis;
            final FastDFA<String> compositionDfa;
            final FastDFAState topState;
            if (learner != null) {
                final long learningStart = System.currentTimeMillis();
                final LearningPhaseResult lpr = runLearningPhase(ctx, learner, sigma);
                timings.learningMs += System.currentTimeMillis() - learningStart;
                totalRefinements += lpr.refinements();
                hypothesis = lpr.hypothesis();
                compositionDfa = ProductAutomatonBuilder.copy(hypothesis, sigma);
                topState = ProductAutomatonBuilder.addUniversalState(compositionDfa, sigma);
            } else {
                final FastDFA<String> everything = unconstrained(sigma);
                hypothesis = everything;
                compositionDfa = everything;
                topState = everything.getInitialState();
                logger.write(Logger.Level.MAINSTEP,
                        "  [Precheck] Discrete-only round: the assumption accepts every word,"
                                + " so this checks A alone%n");
            }
            lastHypothesisStates = hypothesis.size();

            discovered.clear();
            final long modelCheckStart = System.currentTimeMillis();
            ModelCheckPhaseResult<D> mcr =
                    runModelCheckingPhase(ctx, compositionDfa, sigma, topState, collector);
            timings.modelCheckingMs += System.currentTimeMillis() - modelCheckStart;
            lastProductDfaStates = mcr.productDfaStates();
            collectCegarStatistics(mcr.safetyResult(), timings);

            final List<String> newSymbols = register(ctx.stepper(), discovered);
            if (!newSymbols.isEmpty()) {
                if (learner != null) {
                    growLearnerAlphabet(learner, newSymbols);
                }
                sigma = snapshot(ctx.stepper());
                logger.write(Logger.Level.MAINSTEP,
                        "  [Alphabet] %d symbol(s) discovered, the alphabet is now %d%n",
                        newSymbols.size(), sigma.size());
            }

            if (mcr.safetyResult().isSafe()) {
                logger.write(Logger.Level.RESULT,
                        "Result: SAFE (%d outer iterations, %d symbols, %d MQ, %d ms)%n",
                        outerIteration, sigma.size(), ctx.mqCounter()[0],
                        System.currentTimeMillis() - startTime);
                return finish(ctx, mcr.safetyResult(), timings, lastHypothesisStates,
                        lastProductDfaStates, sigma.size(), totalRefinements, outerIteration,
                        startTime);
            }

            final Trace<DiscreteCompositionState<D, FastDFAState>, XtaAction> cexTrace =
                    mcr.safetyResult().asUnsafe().getTrace();

            Word<String> cexWord = extractWord(cexTrace, ctx.stepper());
            logger.write(Logger.Level.MAINSTEP,
                    "  [Counterexample] Length: %d, checking realizability...%n", cexWord.size());

            final long realizabilityStart = System.currentTimeMillis();
            final boolean realizable = ctx.realizabilityOracle().isRealizable(cexWord);
            timings.realizabilityMs += System.currentTimeMillis() - realizabilityStart;
            if (realizable) {
                logger.write(Logger.Level.RESULT,
                        "Result: UNSAFE (%d outer iterations, %d symbols, %d MQ, %d ms)%n",
                        outerIteration, sigma.size(), ctx.mqCounter()[0],
                        System.currentTimeMillis() - startTime);
                return finish(ctx, mcr.safetyResult(), timings, lastHypothesisStates,
                        lastProductDfaStates, sigma.size(), totalRefinements, outerIteration,
                        startTime);
            }

            if (learner == null) {
                logger.write(Logger.Level.MAINSTEP,
                        "  [Precheck] A alone is not safe, starting the learner%n");

                learner = LearningAlgorithmFactory.create(
                        learningAlgorithmType, new GrowingMapAlphabet<>(sigma), ctx.mqOracle());
                learner.startLearning();
            }

            if (learner.getHypothesisModel().accepts(cexWord)) {
                logger.write(Logger.Level.MAINSTEP,
                        "  [Spurious] Refining hypothesis with word (length %d)%n", cexWord.size());
                learner.refineHypothesis(new DefaultQuery<>(cexWord, false));
            } else {
                logger.write(Logger.Level.MAINSTEP,
                        "  [Spurious] The hypothesis already rejects the word, no refinement%n");
            }
        }

        throw new IllegalStateException(
                "The outer loop did not converge within " + maxOuterIterations
                        + " iterations - raise maxOuterIterations if this is a large model");
    }

    private CheckResult<D> finish(
            final CheckContext<D, P> ctx,
            final SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> safetyResult,
            final Timings timings, final int hypothesisStates, final int compositionDfaStates,
            final int alphabetSize, final int refinements, final int outerIterations,
            final long startTime) {
        final LearningStatistics statistics = new LearningStatistics(
                hypothesisStates, compositionDfaStates, alphabetSize, refinements, outerIterations,
                timings.cegarIterations, ctx.mqCounter()[0], ctx.sulCounter()[0],
                ctx.eqCounter()[0], System.currentTimeMillis() - startTime, timings.setupMs,
                timings.learningMs, timings.modelCheckingMs, timings.refinerMs,
                timings.realizabilityMs);
        logger.write(Logger.Level.RESULT, "%s", statistics);
        return new CheckResult<>(withStatistics(safetyResult, statistics), statistics);
    }

    private SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> withStatistics(
            final SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> safetyResult,
            final LearningStatistics statistics) {
        return safetyResult.isSafe()
                ? SafetyResult.safe(safetyResult.getArg(), statistics)
                : SafetyResult.unsafe(safetyResult.asUnsafe().getTrace(), safetyResult.getArg(),
                        statistics);
    }

    private static <D extends State> void collectCegarStatistics(
            final SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> safetyResult,
            final Timings timings) {
        safetyResult.getStats()
                .filter(CegarStatistics.class::isInstance)
                .map(CegarStatistics.class::cast)
                .ifPresent(cegarStatistics -> {
                    timings.cegarIterations += cegarStatistics.getIterations();
                    timings.refinerMs += cegarStatistics.getRefinerTimeMs();
                });
    }

    private static FastDFA<String> unconstrained(final Alphabet<String> alphabet) {
        final FastDFA<String> dfa = new FastDFA<>(alphabet);
        final FastDFAState state = dfa.addState(true);
        dfa.setInitialState(state);
        for (final String symbol : alphabet) {
            dfa.addTransition(state, symbol, state);
        }
        return dfa;
    }

    private static Alphabet<String> snapshot(final TimedStepper stepper) {
        return Alphabets.fromCollection(new ArrayList<>(stepper.alphabet()));
    }

    private static List<String> register(final TimedStepper stepper,
                                         final Map<String, XtaAction> discovered) {
        final List<String> newSymbols = new ArrayList<>();
        for (final XtaAction action : discovered.values()) {
            final int before = stepper.alphabet().size();
            final String symbol = stepper.register(action);
            if (stepper.alphabet().size() > before) {
                newSymbols.add(symbol);
            }
        }
        return newSymbols;
    }

    private static void growLearnerAlphabet(final LearningAlgorithm.DFALearner<String> learner,
                                            final List<String> symbols) {
        if (!(learner instanceof SupportsGrowingAlphabet)) {
            throw new UnsupportedOperationException(
                    "The alphabet is discovered during the search, so the learner has to accept"
                            + " new symbols: " + learner.getClass().getSimpleName()
                            + " does not implement SupportsGrowingAlphabet");
        }
        @SuppressWarnings("unchecked")
        final SupportsGrowingAlphabet<String> growing = (SupportsGrowingAlphabet<String>) learner;
        for (final String symbol : symbols) {
            growing.addAlphabetSymbol(symbol);
        }
    }

    private LearningPhaseResult runLearningPhase(CheckContext<D, P> ctx,
                                                LearningAlgorithm.DFALearner<String> learner,
                                                Alphabet<String> sigma) {
        long start = System.currentTimeMillis();
        int refinements = 0;

        while (refinements < maxLearningIterations) {
            DFA<?, String> hypothesis = learner.getHypothesisModel();
            logger.write(Logger.Level.SUBSTEP,
                    "  [Learning] Hypothesis states: %d, running EQ oracle...%n", hypothesis.size());

            ctx.eqCounter()[0]++;
            DefaultQuery<String, Boolean> ceq =
                    ctx.eqOracle().findCounterExample(hypothesis, sigma);
            if (ceq == null) {
                logger.write(Logger.Level.MAINSTEP,
                        "  [Learning] Converged after %d refinements in %d ms. States: %d%n",
                        refinements, System.currentTimeMillis() - start, hypothesis.size());
                return new LearningPhaseResult(hypothesis, refinements);
            }

            refinements++;
            logger.write(Logger.Level.SUBSTEP,
                    "  [Learning] Counterexample (length %d), refining...%n", ceq.getInput().size());
            learner.refineHypothesis(ceq);
        }

        throw new IllegalStateException(
                "The learner did not converge within " + maxLearningIterations
                        + " refinements - raise maxLearningIterations if this is a large model");
    }

    private static final int MODEL_CHECK_LOG_INTERVAL_SEC = 5;

    private ModelCheckPhaseResult<D> runModelCheckingPhase(
            CheckContext<D, P> ctx, FastDFA<String> compositionDfa, Alphabet<String> sigma,
            FastDFAState topState, DiscreteCompositionTransFunc.UnknownSymbolHandler collector) {
        logger.write(Logger.Level.SUBSTEP,
                "  [ModelCheck] Composition DFA states: %d, starting checker...%n",
                compositionDfa.size());

        SafetyChecker<DiscreteCompositionState<D, FastDFAState>, XtaAction, P> checker =
                buildChecker(ctx, compositionDfa, sigma, collector, topState);

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

        final P prec = modelCheckingStrategy.reusablePrec(initialPrec);
        if (!prec.equals(initialPrec)) {
            logger.write(Logger.Level.SUBSTEP,
                    "  [ModelCheck] Reusing the precision refined in an earlier iteration%n");
        }

        SafetyResult<DiscreteCompositionState<D, FastDFAState>, XtaAction> result;
        try {
            result = checker.check(prec);
        } finally {
            progressTask.cancel(false);
            scheduler.shutdown();
        }

        logger.write(Logger.Level.MAINSTEP,
                "  [ModelCheck] Result: %s in %d ms%n",
                result.isSafe() ? "SAFE" : "UNSAFE", System.currentTimeMillis() - start);
        return new ModelCheckPhaseResult<>(result, compositionDfa.size());
    }

    private CheckContext<D, P> buildContext() {
        final ZoneSemantics semantics = ZoneSemantics.of(xtaSystem);
        final TimedStepper stepper = TimedStepper.of(xtaSystem, semantics);
        final Alphabet<String> alphabet = stepper.alphabet();
        final SUL<String, Boolean> mappedSul = XtaTimedSul.create(stepper);

        long[] mqCounter = {0};
        long[] sulCounter = {0};
        long[] eqCounter = {0};
        MembershipOracle.DFAMembershipOracle<String> mqOracle =
                buildMqOracle(mappedSul, mqCounter, sulCounter);

        EquivalenceOracle<DFA<?, String>, String, Boolean> eqOracle =
                XtaInclusionOracle.create(semantics, stepper, mqOracle,
                        inclusionSearchStrategy.strategy(), logger);
        XtaRealizabilityOracle realizabilityOracle =
                XtaRealizabilityOracle.create(mqOracle, logger);

        logger.write(Logger.Level.INFO,
                "[Context] Inclusion search: %s, initial delay: %s, zones: %s%n",
                inclusionSearchStrategy,
                semantics.initialDelayAllowed() ? "allowed" : "forbidden (urgent/committed init)",
                semantics.normalize() ? "k-normalized, LU subsumption"
                        : "exact (diagonal constraints present)");

        return new CheckContext<>(
                alphabet, semantics, stepper,
                mqOracle, eqOracle, realizabilityOracle, xtaAnalysis,
                mqCounter, sulCounter, eqCounter);
    }

    private SafetyChecker<DiscreteCompositionState<D, FastDFAState>, XtaAction, P> buildChecker(
            CheckContext<D, P> ctx, FastDFA<String> compositionDfa, Alphabet<String> sigma,
            DiscreteCompositionTransFunc.UnknownSymbolHandler collector, FastDFAState topState) {
        Predicate<DiscreteCompositionState<D, FastDFAState>> targetPred =
                state -> state.getXtaState().isError();

        DiscreteCompositionAnalysis<D, FastDFAState, P> analysis =
                DiscreteCompositionAnalysis.create(
                        DiscreteCompositionOrd.create(dataOrd),
                        sigma,
                        compositionDfa,
                        ctx.xtaAnalysis().getInitFunc(),
                        dataTransFunc,
                        ctx.stepper(),
                        collector,
                        topState);

        DiscreteCompositionLts<D, FastDFAState> lts =
                DiscreteCompositionLts.create(xtaSystem);

        return DiscreteCompositionCheckerFactory.create(
                modelCheckingStrategy, searchStrategy, analysis, lts, targetPred);
    }

    private static final int MQ_LOG_INTERVAL = 200;

    private MembershipOracle.DFAMembershipOracle<String> buildMqOracle(
            SUL<String, Boolean> mappedSul, long[] mqCounter, long[] sulCounter) {
        Map<Word<String>, Boolean> cache = new HashMap<>();
        return queries -> {
            for (Query<String, Boolean> query : queries) {
                mqCounter[0]++;
                query.answer(cache.computeIfAbsent(query.getInput(), word -> {
                    sulCounter[0]++;
                    if (sulCounter[0] % MQ_LOG_INTERVAL == 0) {
                        logger.write(Logger.Level.SUBSTEP,
                                "  [MQ] #%d SUL run (cache: %d entries, word length: %d)%n",
                                sulCounter[0], cache.size(), word.size());
                    }
                    return runMembershipQuery(mappedSul, word);
                }));
            }
        };
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
            TimedStepper stepper) {
        List<String> symbols = new ArrayList<>();
        for (XtaAction action : trace.getActions()) {
            symbols.add(stepper.symbolOf(action));
        }
        return Word.fromList(symbols);
    }
}
