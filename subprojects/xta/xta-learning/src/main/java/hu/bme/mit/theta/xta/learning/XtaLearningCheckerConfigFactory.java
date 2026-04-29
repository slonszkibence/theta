package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceSeqItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionRefiner;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionState;
import hu.bme.mit.theta.xta.learning.compositional.realizability.DiscreteCompositionStrategy;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

public class XtaLearningCheckerConfigFactory {
    private final XtaSystem xtaSystem;
    private LearningAlgorithmType learningAlgorithmType = LearningAlgorithmType.TTT;
    private EQOracleType eqOracleType = EQOracleType.XTA_INCLUSION;
    private DiscreteCompositionStrategy checkerStrategy = DiscreteCompositionStrategy.CEGAR_CHECKER;
    private Refiner<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> refiner = null;
    private int eqMaxDepth = 2;
    private int eqRandomMinLength = 10;
    private int eqRandomMaxLength = 1000;
    private int eqRandomMaxTests = 10000;
    private Logger logger = NullLogger.getInstance();

    private XtaLearningCheckerConfigFactory(XtaSystem xtaSystem) {
        this.xtaSystem = xtaSystem;
    }

    public static XtaLearningCheckerConfigFactory create(XtaSystem xtaSystem) {
        return new XtaLearningCheckerConfigFactory(xtaSystem);
    }

    public XtaLearningCheckerConfigFactory learningAlgorithmType(LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = learningAlgorithmType;
        return this;
    }

    public XtaLearningCheckerConfigFactory eqOracleType(EQOracleType eqOracleType) {
        this.eqOracleType = eqOracleType;
        return this;
    }

    public XtaLearningCheckerConfigFactory checkerStrategy(DiscreteCompositionStrategy checkerStrategy) {
        this.checkerStrategy = checkerStrategy;
        return this;
    }

    public XtaLearningCheckerConfigFactory refiner(
            Refiner<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> refiner) {
        this.refiner = refiner;
        return this;
    }

    public XtaLearningCheckerConfigFactory eqMaxDepth(int eqMaxDepth) {
        this.eqMaxDepth = eqMaxDepth;
        return this;
    }

    public XtaLearningCheckerConfigFactory eqRandomParameters(int eqMinLength, int eqMaxLength, int eqMaxTests) {
        this.eqRandomMinLength = eqMinLength;
        this.eqRandomMaxLength = eqMaxLength;
        this.eqRandomMaxTests = eqMaxTests;
        return this;
    }

    public XtaLearningCheckerConfigFactory logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public XtaLearningCheckerConfigFactory consoleLogger(Logger.Level level) {
        this.logger = new ConsoleLogger(level);
        return this;
    }

    public XtaLearningCheckerConfig build() {
        Refiner<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> effectiveRefiner =
                (this.refiner != null) ? this.refiner : createDefaultRefiner();

        return XtaLearningCheckerConfig.create(
                xtaSystem,
                learningAlgorithmType,
                eqOracleType,
                checkerStrategy,
                effectiveRefiner,
                eqMaxDepth,
                eqRandomMinLength,
                eqRandomMaxLength,
                eqRandomMaxTests,
                logger
        );
    }

    private static Refiner<DiscreteCompositionState<FastDFAState>, XtaAction, ExplPrec> createDefaultRefiner() {
        return DiscreteCompositionRefiner.create(
                ExprTraceSeqItpChecker.create(
                        True(), True(),
                        Z3SolverFactory.getInstance().createItpSolver()
                ),
                PruneStrategy.LAZY
        );
    }
}
