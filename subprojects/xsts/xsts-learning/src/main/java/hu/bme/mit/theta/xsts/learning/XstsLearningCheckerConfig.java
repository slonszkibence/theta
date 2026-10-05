package hu.bme.mit.theta.xsts.learning;

import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.analysis.zone.ZoneStmtTransFunc;
import hu.bme.mit.theta.core.clock.constr.ClockConstrs;
import hu.bme.mit.theta.core.clock.constr.DiffConstr;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.clocktype.ClockConstraintExpr;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xsts.learning.common.HypothesisGraph;
import hu.bme.mit.theta.xsts.learning.common.ClockBoundsCollector;
import hu.bme.mit.theta.xsts.learning.common.DataProjectedAnalysis;
import hu.bme.mit.theta.xsts.learning.common.LearningCancellation;
import hu.bme.mit.theta.xsts.learning.common.XstsTimingMapper;
import hu.bme.mit.theta.xsts.learning.inclusion.TimedXstsInclusionOracle;
import hu.bme.mit.theta.xsts.learning.learnalgo.LearningAlgorithmFactory;
import hu.bme.mit.theta.xsts.learning.membership.TimedXstsMembershipOracle;
import hu.bme.mit.theta.xsts.learning.membership.TimedXstsSul;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarStatistics;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.ExprStatePredicate;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.utils.ValuationExtractor;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfig;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsAnalysis;
import hu.bme.mit.theta.xsts.analysis.XstsOrd;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.timed.ControlFlowSplitXstsLts;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;
import hu.bme.mit.theta.xsts.learning.common.*;
import hu.bme.mit.theta.xsts.learning.learnalgo.LearningAlgorithmType;
import hu.bme.mit.theta.xsts.learning.modelchecking.ModelCheckingStrategy;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionAnalysis;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionCheckerFactory;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionInitFunc;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionLts;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionOrd;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionState;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionTransFunc;
import hu.bme.mit.theta.xsts.learning.realizability.XstsRealizabilityOracle;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;
import net.automatalib.alphabet.SupportsGrowingAlphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.mapper.MappedSUL;
import de.learnlib.sul.SUL;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Optional;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;

public class XstsLearningCheckerConfig<S extends ExprState, P extends Prec>
        implements SafetyChecker<XstsCompositionState<S, FastDFAState>, XstsAction, P> {

    private final XSTS xsts;
    private final SolverFactory solverFactory;
    private final LearningAlgorithmType learningAlgorithmType;
    private final ModelCheckingStrategy<S, FastDFAState, P> modelCheckingStrategy;
    private final SearchStrategy searchStrategy;
    private final Analysis<S, StmtAction, P> baseDataAnalysis;
    private final ValuationExtractor<S> valExtractor;
    private final P prec;
    private final PartialOrd<S> dataOrd;
    private final int maxLearningIterations;
    private final int maxOuterIterations;
    private final Logger logger;

    /** The last finished run, kept so the artifacts can be written after the fact. */
    private CheckResult<S> lastResult;

    private XstsLearningCheckerConfig(
            final XSTS xsts,
            final SolverFactory solverFactory,
            final LearningAlgorithmType learningAlgorithmType,
            final ModelCheckingStrategy<S, FastDFAState, P> modelCheckingStrategy,
            final SearchStrategy searchStrategy,
            final Analysis<S, StmtAction, P> baseDataAnalysis,
            final ValuationExtractor<S> valExtractor,
            final P prec,
            final PartialOrd<S> dataOrd,
            final int maxLearningIterations,
            final int maxOuterIterations,
            final Logger logger
    ) {
        this.xsts = checkNotNull(xsts);
        this.solverFactory = checkNotNull(solverFactory);
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        this.modelCheckingStrategy = checkNotNull(modelCheckingStrategy);
        this.searchStrategy = checkNotNull(searchStrategy);
        this.baseDataAnalysis = checkNotNull(baseDataAnalysis);
        this.valExtractor = checkNotNull(valExtractor);
        this.prec = checkNotNull(prec);
        this.dataOrd = checkNotNull(dataOrd);
        this.maxLearningIterations = maxLearningIterations;
        this.maxOuterIterations = maxOuterIterations;
        this.logger = checkNotNull(logger);
    }

    public static <S extends ExprState, P extends Prec> XstsLearningCheckerConfig<S, P> create(
            final XSTS xsts,
            final SolverFactory solverFactory,
            final LearningAlgorithmType learningAlgorithmType,
            final ModelCheckingStrategy<S, FastDFAState, P> modelCheckingStrategy,
            final SearchStrategy searchStrategy,
            final Analysis<S, StmtAction, P> baseDataAnalysis,
            final ValuationExtractor<S> valExtractor,
            final P prec,
            final PartialOrd<S> dataOrd,
            final int maxLearningIterations,
            final int maxOuterIterations,
            final Logger logger
    ) {
        return new XstsLearningCheckerConfig<>(
                xsts, solverFactory, learningAlgorithmType, modelCheckingStrategy, searchStrategy,
                baseDataAnalysis, valExtractor, prec, dataOrd,
                maxLearningIterations, maxOuterIterations, logger);
    }

    // ── Public API ──────────────────────────────────────────────────────────
    @Override
    public SafetyResult<XstsCompositionState<S, FastDFAState>, XstsAction> check(final P prec) {
        return checkWithStats(prec).safetyResult();
    }
    public SafetyResult<XstsCompositionState<S, FastDFAState>, XstsAction> check() {
        return check(prec);
    }

    public CheckResult<S> checkWithStats() {
        return checkWithStats(prec);
    }

    public P getInitPrec() {
        return prec;
    }

    public XstsConfig<?, ?, ?> asXstsConfig() {
        return XstsConfig.create(this, prec);
    }

    public CheckResult<S> checkWithStats(final P prec) {
        final CheckResult<S> result = runCheck(prec);
        lastResult = result;
        return result;
    }


    public void writeArtifacts(final Path outDir, final String baseName) throws IOException {
        checkState(lastResult != null, "writeArtifacts needs a finished run - call check first");
        Files.createDirectories(outDir);

        final UntimedXsts.Result timeless =
                UntimedXsts.extract(xsts, solverFactory.createSolver());

        write(outDir, baseName + "-txsts.dot", TxstsGraph.create(timeless));
        write(outDir, baseName + "-timeless.dot", UntimedXstsGraph.create(timeless, true));
        write(outDir, baseName + "-hypothesis.dot",
                HypothesisGraph.create(lastResult.hypothesis(), lastResult.alphabet()));
        write(outDir, baseName + "-arg.dot",
                ArgVisualizer.getDefault().visualize(lastResult.safetyResult().getArg()));
    }

    private void write(final Path outDir, final String name, final Graph graph) throws IOException {
        final Path path = outDir.resolve(name);
        Files.writeString(path, GraphvizWriter.getInstance().writeString(graph));
        logger.write(Logger.Level.RESULT, "Written: %s%n", path.toAbsolutePath());
    }

    private CheckResult<S> runCheck(final P prec) {
        final long algorithmStart = System.currentTimeMillis();
        final long setupStart = algorithmStart;
        final CheckContext<S, P> ctx = buildContext();
        final long setupMs = System.currentTimeMillis() - setupStart;
        final long[] learningMs = {0};
        final long[] modelCheckingMs = {0};
        final long[] realizabilityMs = {0};

        final GrowingMapAlphabet<XstsAction> alphabet = new GrowingMapAlphabet<>();
        final Set<XstsAction> discovered = new LinkedHashSet<>();
        final XstsCompositionLts.UnknownSymbolHandler collector = discovered::add;

        LearningAlgorithm.DFALearner<XstsAction> learner = null;
        EquivalenceOracle.DFAEquivalenceOracle<XstsAction> eqOracle = null;
        DFA<?, XstsAction> hypothesis = null;

        int totalRefinements = 0;
        int totalCegarIterations = 0;
        long totalRefinerMs = 0;
        int lastHypothesisStates = 0;
        int lastCompositionDfaStates = 0;

        for (int outer = 1; outer <= maxOuterIterations; outer++) {
            LearningCancellation.check("outer iteration");
            logger.write(Logger.Level.MAINSTEP, "%n=== Outer iteration #%d ===%n", outer);

            final boolean unconstrainedRound = learner == null;

            final FastDFA<XstsAction> compositionDfa;
            if (learner == null) {
                compositionDfa = unconstrained(alphabet);
                hypothesis = compositionDfa;
            } else {
                final long learnStart = System.currentTimeMillis();
                final LearningPhaseResult lpr = runLearningPhase(ctx, learner, eqOracle, alphabet);
                learningMs[0] += System.currentTimeMillis() - learnStart;
                totalRefinements += lpr.refinements();
                hypothesis = lpr.hypothesis();
                compositionDfa = ProductAutomatonBuilder.copy(hypothesis, alphabet);
            }
            lastHypothesisStates = hypothesis.size();
            lastCompositionDfaStates = compositionDfa.size();

            final FastDFAState top = unconstrainedRound
                    ? compositionDfa.getInitialState()
                    : ProductAutomatonBuilder.addUniversalState(compositionDfa, alphabet);

            logger.write(Logger.Level.SUBSTEP,
                    "  [ModelCheck] H states in composition: %d, |alphabet|=%d, starting checker...%n",
                    compositionDfa.size(), alphabet.size());

            discovered.clear();
            final long mcStart = System.currentTimeMillis();
            final SafetyResult<XstsCompositionState<S, FastDFAState>, XstsAction> result =
                    buildChecker(ctx, compositionDfa, alphabet, collector, top).check(prec);
            modelCheckingMs[0] += System.currentTimeMillis() - mcStart;


            final Optional<CegarStatistics> innerStats = result.getStats()
                    .filter(CegarStatistics.class::isInstance).map(CegarStatistics.class::cast);
            totalCegarIterations += innerStats.map(CegarStatistics::getIterations).orElse(0);
            totalRefinerMs += innerStats.map(CegarStatistics::getRefinerTimeMs).orElse(0L);

            logger.write(Logger.Level.SUBSTEP,
                    "  [Composition ARG] %d nodes, %d incomplete, %d unsafe, %d ms%n",
                    result.getArg().size(), result.getArg().getIncompleteNodes().count(),
                    result.getArg().getUnsafeNodes().count(),
                    System.currentTimeMillis() - mcStart);

            if (!discovered.isEmpty()) {
                logger.write(Logger.Level.MAINSTEP,
                        "  [Alphabet] %d new symbol(s), alphabet %d -> %d%n",
                        discovered.size(), alphabet.size(), alphabet.size() + discovered.size());
                checkNoDiagonalConstraints(discovered);
                if (learner == null) {
                    for (final XstsAction symbol : discovered) {
                        alphabet.addSymbol(symbol);
                    }
                    learner = LearningAlgorithmFactory.create(
                            learningAlgorithmType, alphabet, ctx.mqOracle());
                    learner.startLearning();
                } else {
                    growLearnerAlphabet(learner, discovered);
                    for (final XstsAction symbol : discovered) {
                        alphabet.addSymbol(symbol);
                    }
                }
                eqOracle = buildEqOracle(ctx, alphabet);

                hypothesis = unconstrainedRound
                        ? unconstrained(alphabet)
                        : learner.getHypothesisModel();
                lastHypothesisStates = hypothesis.size();
            }

            if (result.isSafe()) {
                logger.write(Logger.Level.RESULT, "Result: SAFE (%d outer iterations)%n", outer);
                final LearningStatistics stats = statistics(lastHypothesisStates,
                        lastCompositionDfaStates, totalRefinements, outer, ctx, alphabet,
                        algorithmStart, setupMs, learningMs[0], modelCheckingMs[0],
                        realizabilityMs[0], ctx.eqCount(), totalCegarIterations, totalRefinerMs);
                return new CheckResult<>(withStats(result, stats), stats, hypothesis, alphabet);
            }

            final Word<XstsAction> cexWord = extractWord(result.asUnsafe().getTrace(), ctx.projections());
            logger.write(Logger.Level.MAINSTEP,
                    "  [Counterexample] length %d, checking realizability...%n", cexWord.size());
            logWord("  [Counterexample]", cexWord);
            logTrace(result.asUnsafe().getTrace(), ctx.projections());

            final long realStart = System.currentTimeMillis();
            final boolean realizable = ctx.realizabilityOracle().isRealizable(cexWord);
            realizabilityMs[0] += System.currentTimeMillis() - realStart;
            if (realizable) {
                logger.write(Logger.Level.RESULT, "Result: UNSAFE (%d outer iterations)%n", outer);
                final LearningStatistics stats = statistics(lastHypothesisStates,
                        lastCompositionDfaStates, totalRefinements, outer, ctx, alphabet,
                        algorithmStart, setupMs, learningMs[0], modelCheckingMs[0],
                        realizabilityMs[0], ctx.eqCount(), totalCegarIterations, totalRefinerMs);
                return new CheckResult<>(withStats(result, stats), stats, hypothesis, alphabet);
            }

            logger.write(Logger.Level.MAINSTEP,
                    "  [Spurious] refining hypothesis with the counterexample (length %d)%n",
                    cexWord.size());
            checkState(learner != null,
                    "a spurious counterexample needs a learner, but the alphabet stayed empty");
            learner.refineHypothesis(new DefaultQuery<>(cexWord, false));
        }

        throw new IllegalStateException(
                "Did not converge within " + maxOuterIterations + " outer iterations");
    }

    private LearningStatistics statistics(final int hypothesisStates, final int compositionDfaStates,
                                          final int refinements, final int outer,
                                          final CheckContext<S, P> ctx,
                                          final Alphabet<XstsAction> alphabet,
                                          final long algorithmStartMs, final long setupMs,
                                          final long learningMs, final long modelCheckingMs,
                                          final long realizabilityMs, final long eqQueries,
                                          final int cegarIterations, final long refinerMs) {
        return new LearningStatistics(
                System.currentTimeMillis() - algorithmStartMs,
                learningMs,
                modelCheckingMs,
                realizabilityMs,
                ctx.mqCount(),
                eqQueries,
                outer,
                alphabet.size(),
                hypothesisStates,
                compositionDfaStates,
                cegarIterations,
                refinerMs,
                setupMs,
                refinements);
    }

    private SafetyResult<XstsCompositionState<S, FastDFAState>, XstsAction> withStats(
            final SafetyResult<XstsCompositionState<S, FastDFAState>, XstsAction> result,
            final LearningStatistics stats) {
        return result.isSafe()
                ? SafetyResult.safe(result.getArg(), stats)
                : SafetyResult.unsafe(result.asUnsafe().getTrace(), result.getArg(), stats);
    }

    private static FastDFA<XstsAction> unconstrained(final Alphabet<XstsAction> alphabet) {
        final FastDFA<XstsAction> dfa = new FastDFA<>(alphabet);
        final FastDFAState state = dfa.addState(true);
        dfa.setInitialState(state);
        for (final XstsAction symbol : alphabet) {
            dfa.addTransition(state, symbol, state);
        }
        return dfa;
    }

    @SuppressWarnings("unchecked")
    private static void growLearnerAlphabet(final LearningAlgorithm.DFALearner<XstsAction> learner,
                                            final Collection<XstsAction> symbols) {
        if (!(learner instanceof SupportsGrowingAlphabet)) {
            throw new UnsupportedOperationException(
                    "A lazily grown alphabet needs a learner that implements SupportsGrowingAlphabet"
                            + " (TTT and KV do); got " + learner.getClass().getSimpleName());
        }
        final SupportsGrowingAlphabet<XstsAction> growing = (SupportsGrowingAlphabet<XstsAction>) learner;
        for (final XstsAction symbol : symbols) {
            growing.addAlphabetSymbol(symbol);
        }
    }

    private EquivalenceOracle.DFAEquivalenceOracle<XstsAction> buildEqOracle(
            final CheckContext<S, P> ctx, final Alphabet<XstsAction> alphabet) {
        final ClockBoundsCollector.Result bounds =
                ClockBoundsCollector.collect(ctx.clocks(), alphabet);
        return TimedXstsInclusionOracle.create(bounds.ceilings(), ctx.mapper(), bounds.luBounds(),
                ctx.mqOracle(), searchStrategy, logger);
    }

    public record CheckResult<S extends ExprState>(
            SafetyResult<XstsCompositionState<S, FastDFAState>, XstsAction> safetyResult,
            LearningStatistics statistics,
            DFA<?, XstsAction> hypothesis,
            Alphabet<XstsAction> alphabet
    ) {}

    private record LearningPhaseResult(DFA<?, XstsAction> hypothesis, int refinements) {}

    // ── Learning phase (inner loop: learn H until it agrees with the realizability-checked SUL) ──

    private LearningPhaseResult runLearningPhase(
            final CheckContext<S, P> ctx,
            final LearningAlgorithm.DFALearner<XstsAction> learner,
            final EquivalenceOracle.DFAEquivalenceOracle<XstsAction> eqOracle,
            final Alphabet<XstsAction> alphabet) {
        final long phaseStart = System.currentTimeMillis();
        for (int i = 1; i <= maxLearningIterations; i++) {
            LearningCancellation.check("learning iteration");
            final DFA<?, XstsAction> hypothesis = learner.getHypothesisModel();
            final int statesBefore = hypothesis.size();
            final long mqBefore = ctx.mqCount();

            logger.write(Logger.Level.DETAIL,
                    "    [Learning] round #%d: asking the eq oracle about H (%d states, %d symbols)%n",
                    i, statesBefore, alphabet.size());

            final long eqStart = System.currentTimeMillis();
            ctx.eqCounter()[0]++;
            final DefaultQuery<XstsAction, Boolean> cex =
                    eqOracle.findCounterExample(hypothesis, alphabet);
            final long eqMs = System.currentTimeMillis() - eqStart;

            if (cex == null) {
                logger.write(Logger.Level.MAINSTEP,
                        "  [Learning] converged after %d refinements, %d states, %d MQ, %d ms%n",
                        i - 1, statesBefore, ctx.mqCount(), System.currentTimeMillis() - phaseStart);
                return new LearningPhaseResult(hypothesis, i - 1);
            }

            logWord("    [Learning] counterexample", cex.getInput());
            learner.refineHypothesis(cex);

            logger.write(Logger.Level.SUBSTEP,
                    "  [Learning] refinement #%d: %s word of length %d | H %d -> %d states | "
                            + "%d MQ | eq oracle %d ms%n",
                    i, Boolean.TRUE.equals(cex.getOutput()) ? "MISSING" : "SPURIOUS",
                    cex.getInput().size(), statesBefore,
                    learner.getHypothesisModel().size(),
                    ctx.mqCount() - mqBefore, eqMs);
        }
        throw new IllegalStateException(
                "Learning did not converge within " + maxLearningIterations + " iterations");
    }

    // ── Context: everything built once per check() run ─────────────────────

    private record CheckContext<S extends ExprState, P extends Prec>(
            XstsRealizabilityOracle realizabilityOracle,
            InitFunc<XstsState<S>, P> innerInitFunc,
            TransFunc<XstsState<S>, XstsAction, P> innerTransFunc,
            ControlFlowSplitXstsLts<S> compositionInnerLts,
            TimedXstsActionProjections projections,
            XstsTimingMapper<XstsAction, Boolean, Boolean> mapper,
            TimedXstsMembershipOracle<XstsAction> mqOracle,
            List<VarDecl<ClockType>> clocks,
            long[] mqCounter,
            long[] eqCounter
    ) {
        long mqCount() {
            return mqCounter[0];
        }

        long eqCount() {
            return eqCounter[0];
        }
    }

    private CheckContext<S, P> buildContext() {
        final List<VarDecl<ClockType>> clocks = xsts.getVars().stream()
                .filter(v -> v.getType() instanceof ClockType)
                .map(XstsLearningCheckerConfig::asClock)
                .toList();
        if (clocks.isEmpty()) {
            throw new IllegalArgumentException("The model has no clocks - nothing to learn");
        }
        final ZonePrec zonePrec = ZonePrec.of(clocks);
        final TimedXstsActionProjections projections = TimedXstsActionProjections.create();

        final XstsTimingMapper<XstsAction, Boolean, Boolean> mapper =
                XstsTimingMapper.identity(accepted -> accepted);
        final SUL<XstsAction, Boolean> sul =
                new MappedSUL<>(mapper, TimedXstsSul.create(projections, zonePrec));

        final long[] mqCounter = {0};
        final long[] eqCounter = {0};
        final SUL<XstsAction, Boolean> countingSul = new SUL<>() {
            @Override
            public void pre() {
                mqCounter[0]++;
                sul.pre();
            }

            @Override
            public void post() {
                sul.post();
            }

            @Override
            public Boolean step(final XstsAction symbol) {
                return sul.step(symbol);
            }
        };

        final TimedXstsMembershipOracle<XstsAction> mqOracle =
                TimedXstsMembershipOracle.create(countingSul);
        final XstsRealizabilityOracle realizabilityOracle =
                XstsRealizabilityOracle.create(sul, logger);

        final Analysis<XstsState<S>, XstsAction, P> xstsAnalysis =
                XstsAnalysis.create(DataProjectedAnalysis.of(baseDataAnalysis, projections));
        final ControlFlowSplitXstsLts<S> compositionInnerLts = ControlFlowSplitXstsLts.create(
                xsts, solverFactory.createSolver(), valExtractor);

        return new CheckContext<>(realizabilityOracle, xstsAnalysis.getInitFunc(),
                xstsAnalysis.getTransFunc(), compositionInnerLts, projections, mapper, mqOracle,
                clocks, mqCounter, eqCounter);
    }

    private static ZoneState anyValuation(final List<VarDecl<ClockType>> clocks) {
        final ZoneState.Builder builder = ZoneState.zero(clocks).transform();
        for (final VarDecl<ClockType> clock : clocks) {
            builder.free(clock);
        }
        return builder.nonnegative().build();
    }

    private static boolean isLive(final XstsAction symbol, final ZoneState anyValuation,
                                  final ZonePrec zonePrec,
                                  final TimedXstsActionProjections projections) {
        return !ZoneStmtTransFunc.getInstance()
                .getSuccStates(anyValuation, projections.clockProjection(symbol), zonePrec)
                .iterator().next().isBottom();
    }

    private static void checkNoDiagonalConstraints(final Collection<XstsAction> alphabet) {
        for (final XstsAction symbol : alphabet) {
            for (final Stmt stmt : symbol.getStmts()) {
                if (!(stmt instanceof AssumeStmt assume)) {
                    continue;
                }
                for (final Expr<BoolType> conjunct : ExprUtils.getConjuncts(assume.getCond())) {
                    if (conjunct instanceof ClockConstraintExpr clockExpr
                            && ClockConstrs.fromClockExpr(clockExpr) instanceof DiffConstr) {
                        throw new UnsupportedOperationException(
                                "Diagonal clock constraint in alphabet symbol " + symbol
                                        + " - k-normalisation and LU-bound subsumption are not"
                                        + " sound for these. Learning would be unsound.");
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static VarDecl<ClockType> asClock(final VarDecl<?> var) {
        return (VarDecl<ClockType>) var;
    }

    // ── Checker builder (A||H CEGAR phase) ──────────────────────────────────

    private SafetyChecker<XstsCompositionState<S, FastDFAState>, XstsAction, P> buildChecker(
            final CheckContext<S, P> ctx, final FastDFA<XstsAction> compositionDfa,
            final Alphabet<XstsAction> alphabet,
            final XstsCompositionLts.UnknownSymbolHandler unknownSymbolHandler,
            final FastDFAState topState) {
        final Solver targetPredSolver = solverFactory.createSolver();
        final Predicate<ExprState> dataTarget = new ExprStatePredicate(Not(xsts.getProp()), targetPredSolver);
        final Predicate<XstsCompositionState<S, FastDFAState>> targetPred =
                state -> dataTarget.test(state.getXstsState().getState());

        final XstsCompositionAnalysis<S, FastDFAState, P> analysis = XstsCompositionAnalysis.create(
                XstsCompositionInitFunc.create(ctx.innerInitFunc(), compositionDfa),
                XstsCompositionTransFunc.create(ctx.innerTransFunc(), compositionDfa, alphabet,
                        ctx.projections(), unknownSymbolHandler, topState),
                XstsCompositionOrd.create(XstsOrd.create(dataOrd)));

        final XstsCompositionLts<S, FastDFAState> lts =
                XstsCompositionLts.create(compositionDfa, alphabet, ctx.compositionInnerLts(),
                        ctx.projections(), unknownSymbolHandler, topState, logger);

        return XstsCompositionCheckerFactory.create(
                modelCheckingStrategy, searchStrategy, analysis, lts, targetPred, logger);
    }

    // ── Counterexample extraction ───────────────────────────────────────────

    private void logWord(final String prefix, final Word<XstsAction> word) {
        if (word.isEmpty()) {
            logger.write(Logger.Level.DETAIL, "%s: <empty word>%n", prefix);
            return;
        }
        int i = 0;
        for (final XstsAction symbol : word) {
            logger.write(Logger.Level.DETAIL, "%s [%d] %s%n", prefix, i++,
                    HypothesisGraph.symbolLabel(symbol));
        }
    }

    private void logTrace(
            final Trace<XstsCompositionState<S, FastDFAState>, XstsAction> trace,
            final TimedXstsActionProjections projections
    ) {
        logger.write(Logger.Level.DETAIL, "  [Trace] %d states, %d actions%n",
                trace.length() + 1, trace.length());
        for (int i = 0; i < trace.length(); i++) {
            logger.write(Logger.Level.DETAIL, "    s%-3d %s%n", i, trace.getState(i));
            logger.write(Logger.Level.DETAIL, "      -> %s%n", trace.getAction(i).getStmts());
            logger.write(Logger.Level.DETAIL, "         clock: %s%n",
                    HypothesisGraph.symbolLabel(projections.clockProjection(trace.getAction(i))));
        }
        logger.write(Logger.Level.DETAIL, "    s%-3d %s%n", trace.length(), trace.getState(trace.length()));
    }

    private Word<XstsAction> extractWord(
            final Trace<XstsCompositionState<S, FastDFAState>, XstsAction> trace,
            final TimedXstsActionProjections projections) {
        final List<XstsAction> symbols = new ArrayList<>();
        for (final XstsAction action : trace.getActions()) {
            final XstsAction clockAction = projections.clockProjection(action);
            if (!clockAction.getStmts().isEmpty()) {
                symbols.add(clockAction);
            }
        }
        return Word.fromList(symbols);
    }
}
