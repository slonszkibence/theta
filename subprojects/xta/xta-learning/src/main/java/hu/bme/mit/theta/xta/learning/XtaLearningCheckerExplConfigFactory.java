package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.Refutation;
import hu.bme.mit.theta.analysis.expr.refinement.RefutationToPrec;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.realizability.*;

import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.stream.Collectors;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

/**
 * A builder/factory class for configuring and creating a learning-based
 * compositional checker specifically for the explicit state ({@link ExplState}) domain.
 * <p>
 * This factory simplifies the initialization of the complex learning architecture
 * defined in Sankur's paper. It provides fluent setter methods to configure:
 * <ul>
 *   <li>The active learning algorithm (e.g., TTT, L*).</li>
 *   <li>The Equivalence Oracle type (e.g., Inclusion, Random Words).</li>
 *   <li>The internal discrete model checking search strategy and refinement rules.</li>
 * </ul>
 * When {@link #build()} is called, it constructs the necessary explicit analyses
 * and wires them into a CEGAR-based {@link ModelCheckingStrategy} for the
 * purely discrete finite-state phase ({@code A || H}).
 */
public class XtaLearningCheckerExplConfigFactory {
    private final XtaSystem xtaSystem;
    private LearningAlgorithmType learningAlgorithmType = LearningAlgorithmType.TTT;
    private EQOracleType eqOracleType = EQOracleType.XTA_INCLUSION;
    private DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy =
            DiscreteCompositionCheckerFactory.SearchStrategy.BFS;
    private TraceCheckerStrategy<ExplPrec, ?> traceCheckerStrategy = defaultTraceCheckerStrategy();
    private PruneStrategy pruneStrategy = PruneStrategy.FULL;
    private int eqMaxDepth = 2;
    private int eqRandomMinLength = 10;
    private int eqRandomMaxLength = 1000;
    private int eqRandomMaxTests = 10000;
    private Logger logger = NullLogger.getInstance();

    private XtaLearningCheckerExplConfigFactory(XtaSystem xtaSystem) {
        this.xtaSystem = xtaSystem;
    }

    /**
     * Initializes a new configuration factory for the given timed automaton system.
     *
     * @param xtaSystem The system to be verified.
     * @return A new {@link XtaLearningCheckerExplConfigFactory} instance.
     */
    public static XtaLearningCheckerExplConfigFactory create(XtaSystem xtaSystem) {
        return new XtaLearningCheckerExplConfigFactory(xtaSystem);
    }

    public XtaLearningCheckerExplConfigFactory learningAlgorithmType(LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = learningAlgorithmType;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory eqOracleType(EQOracleType eqOracleType) {
        this.eqOracleType = eqOracleType;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory searchStrategy(
            DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy) {
        this.searchStrategy = searchStrategy;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory pruneStrategy(PruneStrategy pruneStrategy) {
        this.pruneStrategy = pruneStrategy;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory traceCheckerStrategy(
            TraceCheckerStrategy<ExplPrec, ?> traceCheckerStrategy) {
        this.traceCheckerStrategy = traceCheckerStrategy;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory eqMaxDepth(int eqMaxDepth) {
        this.eqMaxDepth = eqMaxDepth;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory eqRandomParameters(
            int eqMinLength, int eqMaxLength, int eqMaxTests) {
        this.eqRandomMinLength = eqMinLength;
        this.eqRandomMaxLength = eqMaxLength;
        this.eqRandomMaxTests = eqMaxTests;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public XtaLearningCheckerExplConfigFactory consoleLogger(Logger.Level level) {
        this.logger = new ConsoleLogger(level);
        return this;
    }

    /**
     * Builds and wires together the complete learning checker configuration.
     * <p>
     * This method initializes the explicit statement analysis starting with an
     * empty precision ({@link ExplPrec#empty()}). It constructs a CEGAR-based
     * model checking strategy using the configured trace checker and pruning rules.
     *
     * @return A fully initialized {@link XtaLearningCheckerConfig} ready for execution.
     */
    public XtaLearningCheckerConfig<ExplState, ExplPrec> build() {
        ExplStmtAnalysis explStmtAnalysis =
                ExplStmtAnalysis.create(
                        Z3SolverFactory.getInstance().createSolver(),
                        xtaSystem.getInitVal().toExpr());

        XtaAnalysis<ExplState, ExplPrec> xtaAnalysis =
                XtaAnalysis.create(xtaSystem, explStmtAnalysis);

        ModelCheckingStrategy<ExplState, FastDFAState, ExplPrec> modelCheckingStrategy =
                ModelCheckingStrategy.cegar(buildRefiner(traceCheckerStrategy));

        //ModelCheckingStrategy<ExplState, FastDFAState, ExplPrec> modelCheckingStrategy =
        //        ModelCheckingStrategy.plain();

        return XtaLearningCheckerConfig.create(
                xtaSystem,
                learningAlgorithmType,
                eqOracleType,
                modelCheckingStrategy,
                searchStrategy,
                xtaAnalysis,
                ExplPrec.empty(),
                ExplOrd.getInstance(),
                eqMaxDepth,
                eqRandomMinLength,
                eqRandomMaxLength,
                eqRandomMaxTests,
                logger
        );
    }

    private <R extends Refutation>
    Refiner<DiscreteCompositionState<ExplState, FastDFAState>, XtaAction, ExplPrec>
    buildRefiner(TraceCheckerStrategy<ExplPrec, R> strategy) {
        RefutationToPrec<ExplPrec, R> clockFilteredRefToPrec = new RefutationToPrec<>() {
            @Override
            public ExplPrec toPrec(R refutation, int index) {
                ExplPrec raw = strategy.refToPrec().toPrec(refutation, index);
                return ExplPrec.of(
                        raw.getVars().stream()
                                .filter(v -> !(v.getType() instanceof RatType))
                                .collect(Collectors.toSet())
                );
            }
            @Override
            public ExplPrec join(ExplPrec prec1, ExplPrec prec2) {
                return strategy.refToPrec().join(prec1, prec2);
            }
        };
        return DiscreteCompositionRefiner.create(
                strategy.checker(), clockFilteredRefToPrec, pruneStrategy);
    }

    private static TraceCheckerStrategy<ExplPrec, ItpRefutation> defaultTraceCheckerStrategy() {
        return TraceCheckerStrategy.seqItp(
                True(),
                True(),
                Z3SolverFactory.getInstance().createItpSolver()
        );
    }
}