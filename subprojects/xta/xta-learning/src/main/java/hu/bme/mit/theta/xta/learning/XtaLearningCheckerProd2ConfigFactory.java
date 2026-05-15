package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceSeqItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.Refutation;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Ord;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.zone.ZoneOrd;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.zone.XtaZoneAnalysis;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.*;

import net.automatalib.automaton.fsa.impl.FastDFAState;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

/**
 * A builder/factory class for configuring and creating a learning-based
 * compositional checker using a combined Product state space
 * ({@link Prod2State} of {@link ExplState} and {@link ZoneState}).
 * <p>
 * Unlike the purely discrete {@code ExplConfigFactory}, this configuration
 * pushes both the discrete data variables and the continuous clock constraints
 * into the inner model checking phase. The inner XTA abstraction directly
 * computes zone operations alongside explicit state updates.
 * <p>
 * This factory provides fluent setter methods to configure:
 * <ul>
 *   <li>The active learning algorithm (e.g., TTT, L*).</li>
 *   <li>The Equivalence Oracle type.</li>
 *   <li>The internal search strategy, pruning rules, and SMT trace checking logic.</li>
 * </ul>
 */
public class XtaLearningCheckerProd2ConfigFactory {
    private final XtaSystem xtaSystem;
    private LearningAlgorithmType learningAlgorithmType = LearningAlgorithmType.TTT;
    private EQOracleType eqOracleType = EQOracleType.XTA_INCLUSION;
    private DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy =
            DiscreteCompositionCheckerFactory.SearchStrategy.BFS;
    private TraceCheckerStrategy<Prod2Prec<ExplPrec, ZonePrec>, ?> traceCheckerStrategy = defaultTraceCheckerStrategy();
    private PruneStrategy pruneStrategy = PruneStrategy.LAZY;
    private int eqMaxDepth = 2;
    private int eqRandomMinLength = 10;
    private int eqRandomMaxLength = 1000;
    private int eqRandomMaxTests = 10000;
    private Logger logger = NullLogger.getInstance();

    private XtaLearningCheckerProd2ConfigFactory(XtaSystem xtaSystem) {
        this.xtaSystem = xtaSystem;
    }

    /**
     * Initializes a new product-based configuration factory for the given timed automaton system.
     *
     * @param xtaSystem The system to be verified.
     * @return A new {@link XtaLearningCheckerProd2ConfigFactory} instance.
     */
    public static XtaLearningCheckerProd2ConfigFactory create(XtaSystem xtaSystem) {
        return new XtaLearningCheckerProd2ConfigFactory(xtaSystem);
    }

    public XtaLearningCheckerProd2ConfigFactory learningAlgorithmType(LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = learningAlgorithmType;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory eqOracleType(EQOracleType eqOracleType) {
        this.eqOracleType = eqOracleType;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory searchStrategy(
            DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy) {
        this.searchStrategy = searchStrategy;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory pruneStrategy(PruneStrategy pruneStrategy) {
        this.pruneStrategy = pruneStrategy;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory traceCheckerStrategy(
            TraceCheckerStrategy<Prod2Prec<ExplPrec, ZonePrec>, ?> traceCheckerStrategy) {
        this.traceCheckerStrategy = traceCheckerStrategy;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory eqMaxDepth(int eqMaxDepth) {
        this.eqMaxDepth = eqMaxDepth;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory eqRandomParameters(
            int eqMinLength, int eqMaxLength, int eqMaxTests) {
        this.eqRandomMinLength = eqMinLength;
        this.eqRandomMaxLength = eqMaxLength;
        this.eqRandomMaxTests = eqMaxTests;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public XtaLearningCheckerProd2ConfigFactory consoleLogger(Logger.Level level) {
        this.logger = new ConsoleLogger(level);
        return this;
    }

    /**
     * Builds and wires together the complete product-state learning checker configuration.
     * <p>
     * This method combines {@link ExplStmtAnalysis} and {@link XtaZoneAnalysis} into
     * a single {@link Prod2Analysis}. It initializes the exploration with an empty
     * explicit precision (no discrete variables tracked initially) but a <b>full</b>
     * zone precision (all clock variables are tracked from the beginning).
     *
     * @return A fully initialized {@link XtaLearningCheckerConfig} ready for execution.
     */
    public XtaLearningCheckerConfig<Prod2State<ExplState, ZoneState>, Prod2Prec<ExplPrec, ZonePrec>> build() {
        XtaZoneAnalysis zoneAnalysis = XtaZoneAnalysis.create(xtaSystem.getInitLocs());

        ExplStmtAnalysis explStmtAnalysis =
                ExplStmtAnalysis.create(
                        Z3SolverFactory.getInstance().createSolver(),
                        xtaSystem.getInitVal().toExpr());

        Analysis<Prod2State<ExplState, ZoneState>, XtaAction, Prod2Prec<ExplPrec, ZonePrec>> prod2Analysis =
                Prod2Analysis.create(explStmtAnalysis, zoneAnalysis);

        XtaAnalysis<Prod2State<ExplState, ZoneState>, Prod2Prec<ExplPrec, ZonePrec>> xtaAnalysis =
                XtaAnalysis.create(xtaSystem, prod2Analysis);


        ExplPrec explPrec = ExplPrec.empty();
        Prod2Prec<ExplPrec, ZonePrec> initialPrec =
                Prod2Prec.of(explPrec, ZonePrec.of(xtaSystem.getClockVars()));

        //ModelCheckingStrategy<Prod2State<ExplState, ZoneState>, FastDFAState, Prod2Prec<ExplPrec, ZonePrec>> modelCheckingStrategy =
        //        ModelCheckingStrategy.plain();

        ModelCheckingStrategy<Prod2State<ExplState, ZoneState>, FastDFAState, Prod2Prec<ExplPrec, ZonePrec>> modelCheckingStrategy =
                ModelCheckingStrategy.cegar(buildRefiner(traceCheckerStrategy));

        return XtaLearningCheckerConfig.create(
                xtaSystem,
                learningAlgorithmType,
                eqOracleType,
                modelCheckingStrategy,
                searchStrategy,
                xtaAnalysis,
                initialPrec,
                Prod2Ord.create(ExplOrd.getInstance(), ZoneOrd.getInstance()),
                eqMaxDepth,
                eqRandomMinLength,
                eqRandomMaxLength,
                eqRandomMaxTests,
                logger
        );
    }

    /**
     * Constructs the refiner for the CEGAR loop using the configured trace checking strategy.
     *
     * @param strategy The trace checker strategy to evaluate feasibility and extract precision.
     * @param <R>      The refutation type.
     * @return A {@link Refiner} tailored for the composite product state.
     */
    private <R extends Refutation>
    Refiner<DiscreteCompositionState<Prod2State<ExplState, ZoneState>, FastDFAState>, XtaAction, Prod2Prec<ExplPrec, ZonePrec>>
    buildRefiner(TraceCheckerStrategy<Prod2Prec<ExplPrec, ZonePrec>, R> strategy) {
        return DiscreteCompositionRefiner.create(
                strategy.checker(),
                strategy.refToPrec(),
                pruneStrategy);
    }

    /**
     * @return The default trace checking strategy: Sequence Interpolation using Z3,
     *         paired with {@link RefutationToProd2ExplPrec} to ensure only the explicit
     *         data precision is refined, leaving the zone precision untouched.
     */
    private static TraceCheckerStrategy<Prod2Prec<ExplPrec, ZonePrec>, ItpRefutation> defaultTraceCheckerStrategy() {
        return TraceCheckerStrategy.of(
                ExprTraceSeqItpChecker.create(True(), True(), Z3SolverFactory.getInstance().createItpSolver()),
                new RefutationToProd2ExplPrec()
        );
    }
}