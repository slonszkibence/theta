package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.Refutation;
import hu.bme.mit.theta.analysis.expr.refinement.autoexpl.AutoExpl;
import hu.bme.mit.theta.analysis.expr.refinement.autoexpl.NewAtomsAutoExpl;
import hu.bme.mit.theta.analysis.pred.ExprSplitters;
import hu.bme.mit.theta.analysis.pred.PredAbstractors;
import hu.bme.mit.theta.analysis.pred.PredAnalysis;
import hu.bme.mit.theta.analysis.pred.PredOrd;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2Ord;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAbstractors;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAnalysis;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredStrengtheningOperator;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.DiscreteCompositionCheckerFactory;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.DiscreteCompositionRefiner;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.DiscreteCompositionState;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.ModelCheckingStrategy;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.TraceCheckerStrategy;

import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

public final class XtaLearningCheckerConfigFactory {
    public enum DataDomain {
        EXPL, PRED_BOOL, PRED_CART, PRED_SPLIT,
        EXPL_PRED_BOOL, EXPL_PRED_CART, EXPL_PRED_SPLIT
    }

    public enum PredSplit {
        WHOLE(ExprSplitters.whole()),
        CONJUNCTS(ExprSplitters.conjuncts()),
        ATOMS(ExprSplitters.atoms());

        public final ExprSplitters.ExprSplitter splitter;

        PredSplit(final ExprSplitters.ExprSplitter splitter) {
            this.splitter = checkNotNull(splitter);
        }
    }

    public enum Refinement { SEQ_ITP, FW_BIN_ITP, BW_BIN_ITP }

    public enum InitPrec { ALL_DATA, EMPTY }

    private static final int DEFAULT_MAX_ENUM = 250;
    private static final int DEFAULT_MAX_LEARNING_ITERATIONS = 10_000;
    private static final int DEFAULT_MAX_OUTER_ITERATIONS = 1_000;

    private final XtaSystem xtaSystem;
    private DataDomain dataDomain = DataDomain.EXPL;
    private PredSplit predSplit = PredSplit.ATOMS;
    private Refinement refinement = Refinement.SEQ_ITP;
    private InitPrec initPrec = InitPrec.ALL_DATA;
    private int maxEnum = DEFAULT_MAX_ENUM;
    private int maxLearningIterations = DEFAULT_MAX_LEARNING_ITERATIONS;
    private int maxOuterIterations = DEFAULT_MAX_OUTER_ITERATIONS;
    private XtaLearningCheckerConfig.InclusionSearchStrategy inclusionSearchStrategy =
            XtaLearningCheckerConfig.InclusionSearchStrategy.BFS;
    private LearningAlgorithmType learningAlgorithmType = LearningAlgorithmType.TTT;
    private DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy =
            DiscreteCompositionCheckerFactory.SearchStrategy.BFS;
    private PruneStrategy pruneStrategy = PruneStrategy.FULL;
    private Logger logger = NullLogger.getInstance();

    private XtaLearningCheckerConfigFactory(final XtaSystem xtaSystem) {
        this.xtaSystem = checkNotNull(xtaSystem);
    }

    public static XtaLearningCheckerConfigFactory create(final XtaSystem xtaSystem) {
        return new XtaLearningCheckerConfigFactory(xtaSystem);
    }

    public XtaLearningCheckerConfigFactory dataDomain(final DataDomain dataDomain) {
        this.dataDomain = checkNotNull(dataDomain);
        return this;
    }

    public XtaLearningCheckerConfigFactory predSplit(final PredSplit predSplit) {
        this.predSplit = checkNotNull(predSplit);
        return this;
    }

    public XtaLearningCheckerConfigFactory refinement(final Refinement refinement) {
        this.refinement = checkNotNull(refinement);
        return this;
    }

    public XtaLearningCheckerConfigFactory initPrec(final InitPrec initPrec) {
        this.initPrec = checkNotNull(initPrec);
        return this;
    }

    public XtaLearningCheckerConfigFactory learningAlgorithmType(
            final LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        return this;
    }

    public XtaLearningCheckerConfigFactory inclusionSearchStrategy(
            final XtaLearningCheckerConfig.InclusionSearchStrategy inclusionSearchStrategy) {
        this.inclusionSearchStrategy = checkNotNull(inclusionSearchStrategy);
        return this;
    }

    public XtaLearningCheckerConfigFactory searchStrategy(
            final DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy) {
        this.searchStrategy = checkNotNull(searchStrategy);
        return this;
    }

    public XtaLearningCheckerConfigFactory maxEnum(final int maxEnum) {
        this.maxEnum = maxEnum;
        return this;
    }

    public XtaLearningCheckerConfigFactory maxLearningIterations(final int maxLearningIterations) {
        this.maxLearningIterations = maxLearningIterations;
        return this;
    }

    public XtaLearningCheckerConfigFactory maxOuterIterations(final int maxOuterIterations) {
        this.maxOuterIterations = maxOuterIterations;
        return this;
    }

    public XtaLearningCheckerConfigFactory pruneStrategy(final PruneStrategy pruneStrategy) {
        this.pruneStrategy = checkNotNull(pruneStrategy);
        return this;
    }

    public XtaLearningCheckerConfigFactory logger(final Logger logger) {
        this.logger = checkNotNull(logger);
        return this;
    }

    public XtaLearningCheckerConfigFactory consoleLogger(final Logger.Level level) {
        this.logger = new ConsoleLogger(level);
        return this;
    }

    public XtaLearningCheckerConfig<?, ?> build() {
        return switch (dataDomain) {
            case EXPL -> buildExpl();
            case PRED_BOOL, PRED_CART, PRED_SPLIT -> buildPred();
            case EXPL_PRED_BOOL, EXPL_PRED_CART, EXPL_PRED_SPLIT -> buildExplPred();
        };
    }

    private XtaLearningCheckerConfig<ExplState, ExplPrec> buildExpl() {
        final ExplStmtAnalysis dataAnalysis = createExplAnalysis();
        final ExplPrec prec = initPrec == InitPrec.ALL_DATA
                ? ExplPrec.of(xtaSystem.getDataVars())
                : ExplPrec.empty();
        return assemble(dataAnalysis, ExplOrd.getInstance(), prec,
                switch (refinement) {
                    case SEQ_ITP -> TraceCheckerStrategy.seqItp(True(), True(), itpSolver());
                    case FW_BIN_ITP -> TraceCheckerStrategy.fwBinItp(True(), True(), itpSolver());
                    case BW_BIN_ITP -> TraceCheckerStrategy.bwBinItp(True(), True(), itpSolver());
                });
    }

    private XtaLearningCheckerConfig<PredState, PredPrec> buildPred() {
        final Solver solver = Z3SolverFactory.getInstance().createSolver();
        final PredAnalysis<StmtAction> dataAnalysis = createPredAnalysis(solver);
        final PredPrec prec = initPrec == InitPrec.ALL_DATA
                ? PredPrec.of(guardAtoms())
                : PredPrec.of();
        return assemble(dataAnalysis, PredOrd.create(solver), prec,
                switch (refinement) {
                    case SEQ_ITP ->
                            TraceCheckerStrategy.seqItpPred(True(), True(), itpSolver(), predSplit.splitter);
                    case FW_BIN_ITP ->
                            TraceCheckerStrategy.fwBinItpPred(True(), True(), itpSolver(), predSplit.splitter);
                    case BW_BIN_ITP ->
                            TraceCheckerStrategy.bwBinItpPred(True(), True(), itpSolver(), predSplit.splitter);
                });
    }

    private XtaLearningCheckerConfig<Prod2State<ExplState, PredState>,
            Prod2Prec<ExplPrec, PredPrec>> buildExplPred() {
        final Solver solver = Z3SolverFactory.getInstance().createSolver();
        final Prod2ExplPredAnalysis<StmtAction> dataAnalysis = Prod2ExplPredAnalysis.create(
                createExplAnalysis(),
                createPredAnalysis(solver),
                Prod2ExplPredStrengtheningOperator.create(solver),
                Prod2ExplPredAbstractors.booleanAbstractor(solver));
        final Prod2Prec<ExplPrec, PredPrec> prec = Prod2Prec.of(
                initPrec == InitPrec.ALL_DATA
                        ? ExplPrec.of(xtaSystem.getDataVars())
                        : ExplPrec.empty(),
                PredPrec.of());

        final AutoExpl autoExpl = new NewAtomsAutoExpl(Set.of(), guardAtoms(), 0);
        return assemble(dataAnalysis, Prod2Ord.create(ExplOrd.getInstance(), PredOrd.create(solver)),
                prec,
                switch (refinement) {
                    case SEQ_ITP -> TraceCheckerStrategy.seqItpProd2ExplPredAuto(
                            True(), True(), itpSolver(), autoExpl, predSplit.splitter);
                    case FW_BIN_ITP -> TraceCheckerStrategy.fwBinItpProd2ExplPredAuto(
                            True(), True(), itpSolver(), autoExpl, predSplit.splitter);
                    case BW_BIN_ITP -> TraceCheckerStrategy.bwBinItpProd2ExplPredAuto(
                            True(), True(), itpSolver(), autoExpl, predSplit.splitter);
                });
    }

    private <D extends ExprState, P extends Prec, R extends Refutation>
    XtaLearningCheckerConfig<D, P> assemble(
            final Analysis<D, StmtAction, P> dataAnalysis,
            final PartialOrd<D> dataOrd,
            final P initialPrec,
            final TraceCheckerStrategy<P, R> traceCheckerStrategy) {
        final XtaAnalysis<D, P> xtaAnalysis = XtaAnalysis.create(xtaSystem, dataAnalysis);
        final Refiner<DiscreteCompositionState<D, FastDFAState>, XtaAction, P> refiner =
                DiscreteCompositionRefiner.create(traceCheckerStrategy.checker(),
                        traceCheckerStrategy.refToPrec(), pruneStrategy);
        final ModelCheckingStrategy<D, FastDFAState, P> modelCheckingStrategy =
                ModelCheckingStrategy.cegar(refiner);

        return XtaLearningCheckerConfig.create(
                xtaSystem,
                learningAlgorithmType,
                modelCheckingStrategy,
                searchStrategy,
                xtaAnalysis,
                dataAnalysis.getTransFunc(),
                initialPrec,
                dataOrd,
                inclusionSearchStrategy,
                maxLearningIterations,
                maxOuterIterations,
                logger);
    }

    private ExplStmtAnalysis createExplAnalysis() {
        return ExplStmtAnalysis.create(
                Z3SolverFactory.getInstance().createSolver(),
                xtaSystem.getInitVal().toExpr(),
                maxEnum);
    }

    private PredAnalysis<StmtAction> createPredAnalysis(final Solver solver) {
        return PredAnalysis.create(solver, switch (dataDomain) {
            case PRED_BOOL, EXPL_PRED_BOOL -> PredAbstractors.booleanAbstractor(solver);
            case PRED_CART, EXPL_PRED_CART -> PredAbstractors.cartesianAbstractor(solver);
            case PRED_SPLIT, EXPL_PRED_SPLIT -> PredAbstractors.booleanSplitAbstractor(solver);
            case EXPL -> throw new AssertionError("The explicit domain needs no abstractor");
        }, xtaSystem.getInitVal().toExpr());
    }

    private Set<Expr<BoolType>> guardAtoms() {
        final Set<Expr<BoolType>> atoms = new LinkedHashSet<>();
        for (final XtaProcess process : xtaSystem.getProcesses()) {
            for (final XtaProcess.Edge edge : process.getEdges()) {
                for (final Guard guard : edge.getGuards()) {
                    if (guard.isDataGuard()) {
                        atoms.addAll(ExprUtils.getAtoms(guard.toExpr()));
                    }
                }
            }
        }
        return atoms;
    }

    private hu.bme.mit.theta.solver.ItpSolver itpSolver() {
        return Z3SolverFactory.getInstance().createItpSolver();
    }
}
