package hu.bme.mit.theta.xsts.learning;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.utils.ValuationExtractor;
import hu.bme.mit.theta.analysis.pred.PredStateValuationExtractor;
import hu.bme.mit.theta.analysis.expl.ExplOrd;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStateValuationExtractor;
import hu.bme.mit.theta.analysis.expl.ExplAnalysis;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.autoexpl.AutoExpl;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.Refutation;
import hu.bme.mit.theta.analysis.pred.ExprSplitters;
import hu.bme.mit.theta.analysis.pred.PredAbstractors;
import hu.bme.mit.theta.analysis.pred.PredAnalysis;
import hu.bme.mit.theta.analysis.pred.PredOrd;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Ord;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAbstractors;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAnalysis;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredPreStrengtheningOperator;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredStateValuationExtractor;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredStrengtheningOperator;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfigBuilder;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;
import hu.bme.mit.theta.xsts.learning.common.XstsGuardPredicateCollector;
import hu.bme.mit.theta.xsts.learning.learnalgo.LearningAlgorithmType;
import hu.bme.mit.theta.xsts.learning.modelchecking.ModelCheckingStrategy;
import hu.bme.mit.theta.xsts.learning.modelchecking.TraceCheckerStrategy;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionRefiner;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionState;

import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;
import static hu.bme.mit.theta.core.type.clocktype.ClockExprs.Clock;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

public class XstsLearningCheckerConfigFactory {

    public enum PredAbstractor { BOOL, CART, SPLIT }

    public enum PredSplit {
        WHOLE(ExprSplitters.whole()),
        CONJUNCTS(ExprSplitters.conjuncts()),
        ATOMS(ExprSplitters.atoms());

        public final ExprSplitters.ExprSplitter splitter;
        PredSplit(ExprSplitters.ExprSplitter splitter) { this.splitter = checkNotNull(splitter);}
    }

    public enum Refinement { FW_BIN_ITP, BW_BIN_ITP, SEQ_ITP }

    public enum CompositionInitPrec {
        SAME,
        CTRL
    }

    private static final int DEFAULT_MAX_LEARNING_ITERATIONS = 50;
    private static final int DEFAULT_MAX_OUTER_ITERATIONS = 50;
    private static final int DEFAULT_MAX_ENUM = 250;

    private final XSTS xsts;
    private final XstsConfigBuilder.Domain domain;
    private final SolverFactory solverFactory;
    private LearningAlgorithmType learningAlgorithmType = LearningAlgorithmType.TTT;
    private SearchStrategy searchStrategy = SearchStrategy.BFS;
    private TraceCheckerStrategy<Prod2Prec<ExplPrec, PredPrec>, ?> traceCheckerStrategy;
    private PruneStrategy pruneStrategy = PruneStrategy.FULL;
    private PredAbstractor predAbstractor = PredAbstractor.CART;
    private PredSplit predSplit = PredSplit.ATOMS;
    private Refinement refinement = Refinement.SEQ_ITP;
    private XstsConfigBuilder.AutoExpl autoExpl = XstsConfigBuilder.AutoExpl.NEWOPERANDS;
    private int maxLearningIterations = DEFAULT_MAX_LEARNING_ITERATIONS;
    private int maxOuterIterations = DEFAULT_MAX_OUTER_ITERATIONS;
    private int maxEnum = DEFAULT_MAX_ENUM;
    private Logger logger = NullLogger.getInstance();

    private XstsLearningCheckerConfigFactory(final XSTS xsts, final SolverFactory solverFactory, final XstsConfigBuilder.Domain domain) {
        this.xsts = checkNotNull(xsts);
        this.domain = checkNotNull(domain);
        this.solverFactory = checkNotNull(solverFactory);
    }

    public static XstsLearningCheckerConfigFactory create(final XSTS xsts, final SolverFactory solverFactory, final XstsConfigBuilder.Domain domain) {
        return new XstsLearningCheckerConfigFactory(xsts, solverFactory, domain);
    }

    public static XstsLearningCheckerConfigFactory create(final XSTS xsts, final SolverFactory solverFactory) {
        return create(xsts, solverFactory, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED);
    }


    public XstsLearningCheckerConfigFactory learningAlgorithmType(final LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        return this;
    }

    public XstsLearningCheckerConfigFactory searchStrategy(final SearchStrategy searchStrategy) {
        this.searchStrategy = checkNotNull(searchStrategy);
        return this;
    }

    public XstsLearningCheckerConfigFactory traceCheckerStrategy(
            final TraceCheckerStrategy<Prod2Prec<ExplPrec, PredPrec>, ?> traceCheckerStrategy) {
        this.traceCheckerStrategy = checkNotNull(traceCheckerStrategy);
        return this;
    }

    public XstsLearningCheckerConfigFactory maxEnum(final int maxEnum) {
        this.maxEnum = maxEnum;
        return this;
    }

    public XstsLearningCheckerConfigFactory pruneStrategy(final PruneStrategy pruneStrategy) {
        this.pruneStrategy = checkNotNull(pruneStrategy);
        return this;
    }

    public XstsLearningCheckerConfigFactory predAbstractor(final PredAbstractor predAbstractor) {
        this.predAbstractor = checkNotNull(predAbstractor);
        return this;
    }

    public XstsLearningCheckerConfigFactory predSplit(final PredSplit predSplit) {
        this.predSplit = checkNotNull(predSplit);
        return this;
    }

    public XstsLearningCheckerConfigFactory refinement(final Refinement refinement) {
        this.refinement = checkNotNull(refinement);
        return this;
    }

    public XstsLearningCheckerConfigFactory autoExpl(final XstsConfigBuilder.AutoExpl autoExpl) {
        this.autoExpl = checkNotNull(autoExpl);
        return this;
    }

    public XstsLearningCheckerConfigFactory maxLearningIterations(final int maxLearningIterations) {
        this.maxLearningIterations = maxLearningIterations;
        return this;
    }

    public XstsLearningCheckerConfigFactory maxOuterIterations(final int maxOuterIterations) {
        this.maxOuterIterations = maxOuterIterations;
        return this;
    }

    public XstsLearningCheckerConfigFactory logger(final Logger logger) {
        this.logger = checkNotNull(logger);
        return this;
    }

    public XstsLearningCheckerConfigFactory consoleLogger(final Logger.Level level) {
        this.logger = new ConsoleLogger(level);
        return this;
    }


    public XstsLearningCheckerConfig<?, ?> build() {
        return switch (domain) {
            case EXPL -> buildExpl();
            case PRED_BOOL, PRED_CART, PRED_SPLIT -> buildPred();
            case EXPL_PRED_BOOL, EXPL_PRED_CART, EXPL_PRED_SPLIT, EXPL_PRED_COMBINED -> buildExplPred();
        };
    }

    private XstsLearningCheckerConfig<ExplState, ExplPrec> buildExpl() {
        final Solver abstractionSolver = solverFactory.createSolver();
        return assemble(
                ExplStmtAnalysis.create(abstractionSolver, xsts.getInitFormula(), maxEnum),
                ExplStateValuationExtractor.getInstance(),
                ExplPrec.of(xsts.getCtrlVars()),
                ExplOrd.getInstance(),
                explTraceCheckerStrategy());
    }

    private XstsLearningCheckerConfig<PredState, PredPrec> buildPred() {
        final Solver abstractionSolver = solverFactory.createSolver();
        return assemble(
                PredAnalysis.create(abstractionSolver, predAbstractorFor(domain, abstractionSolver),
                        xsts.getInitFormula()),
                PredStateValuationExtractor.getInstance(),
                PredPrec.of(),
                PredOrd.create(abstractionSolver),
                predTraceCheckerStrategy());
    }

    private XstsLearningCheckerConfig<Prod2State<ExplState, PredState>, Prod2Prec<ExplPrec, PredPrec>>
    buildExplPred() {
        final Solver abstractionSolver = solverFactory.createSolver();
        final Set<VarDecl<?>> explVars = xsts.getCtrlVars();
        final Analysis<Prod2State<ExplState, PredState>, StmtAction, Prod2Prec<ExplPrec, PredPrec>> dataAnalysis =
                domain == XstsConfigBuilder.Domain.EXPL_PRED_COMBINED
                        ? Prod2ExplPredAnalysis.create(
                                ExplAnalysis.create(abstractionSolver, xsts.getInitFormula()),
                                PredAnalysis.create(abstractionSolver,
                                        PredAbstractors.booleanAbstractor(abstractionSolver),
                                        xsts.getInitFormula()),
                                Prod2ExplPredStrengtheningOperator.create(abstractionSolver),
                                Prod2ExplPredAbstractors.booleanAbstractor(abstractionSolver))
                        : Prod2Analysis.create(
                                ExplStmtAnalysis.create(abstractionSolver, xsts.getInitFormula(), maxEnum),
                                PredAnalysis.create(abstractionSolver,
                                        predAbstractorFor(domain, abstractionSolver), xsts.getInitFormula()),
                                Prod2ExplPredPreStrengtheningOperator.create(),
                                Prod2ExplPredStrengtheningOperator.create(abstractionSolver));

        final TraceCheckerStrategy<Prod2Prec<ExplPrec, PredPrec>, ?> strategy =
                traceCheckerStrategy != null ? traceCheckerStrategy : defaultTraceCheckerStrategy();

        return assemble(
                dataAnalysis,
                Prod2ExplPredStateValuationExtractor.create(ExplStateValuationExtractor.getInstance()),
                Prod2Prec.of(ExplPrec.of(explVars), PredPrec.of()),
                Prod2Ord.create(ExplOrd.getInstance(), PredOrd.create(abstractionSolver)),
                strategy);
    }

    private <S extends ExprState, P extends Prec, R extends Refutation> XstsLearningCheckerConfig<S, P> assemble(
            final Analysis<S, StmtAction, P> dataAnalysis,
            final ValuationExtractor<S> valExtractor,
            final P prec,
            final PartialOrd<S> dataOrd,
            final TraceCheckerStrategy<P, R> strategy) {

        final ModelCheckingStrategy<S, FastDFAState, P> modelCheckingStrategy =
                ModelCheckingStrategy.cegar(XstsCompositionRefiner.createFromStrategy(
                        strategy, pruneStrategy, TimedXstsActionProjections.create()), logger);

        return XstsLearningCheckerConfig.create(
                xsts, solverFactory, learningAlgorithmType, modelCheckingStrategy, searchStrategy,
                dataAnalysis, valExtractor, prec, dataOrd,
                maxLearningIterations, maxOuterIterations, logger);
    }

    private TraceCheckerStrategy<ExplPrec, ItpRefutation> explTraceCheckerStrategy() {
        final ItpSolver itpSolver = solverFactory.createItpSolver();
        final Expr<BoolType> initFormula = xsts.getInitFormula();
        final Expr<BoolType> negProp = Not(xsts.getProp());
        return switch (refinement) {
            case FW_BIN_ITP -> TraceCheckerStrategy.fwBinItp(initFormula, negProp, itpSolver);
            case BW_BIN_ITP -> TraceCheckerStrategy.bwBinItp(initFormula, negProp, itpSolver);
            case SEQ_ITP -> TraceCheckerStrategy.seqItp(initFormula, negProp, itpSolver);
            default -> throw new UnsupportedOperationException(
                    refinement + " refinement is not supported with the " + domain + " domain.");
        };
    }

    private TraceCheckerStrategy<PredPrec, ItpRefutation> predTraceCheckerStrategy() {
        final ItpSolver itpSolver = solverFactory.createItpSolver();
        final Expr<BoolType> initFormula = xsts.getInitFormula();
        final Expr<BoolType> negProp = Not(xsts.getProp());
        return switch (refinement) {
            case FW_BIN_ITP -> TraceCheckerStrategy.fwBinItpPred(
                    initFormula, negProp, itpSolver, predSplit.splitter);
            case BW_BIN_ITP -> TraceCheckerStrategy.bwBinItpPred(
                    initFormula, negProp, itpSolver, predSplit.splitter);
            case SEQ_ITP -> TraceCheckerStrategy.seqItpPred(
                    initFormula, negProp, itpSolver, predSplit.splitter);
            default -> throw new UnsupportedOperationException(
                    refinement + " refinement is not supported with the " + domain + " domain.");
        };
    }

    private PredAbstractors.PredAbstractor predAbstractorFor(
            final XstsConfigBuilder.Domain domain, final Solver abstractionSolver) {
        return switch (domain) {
            case PRED_BOOL, EXPL_PRED_BOOL -> PredAbstractors.booleanAbstractor(abstractionSolver);
            case PRED_CART, EXPL_PRED_CART -> PredAbstractors.cartesianAbstractor(abstractionSolver);
            case PRED_SPLIT, EXPL_PRED_SPLIT -> PredAbstractors.booleanSplitAbstractor(abstractionSolver);
            default -> buildPredAbstractor(abstractionSolver);
        };
    }

    private TraceCheckerStrategy<Prod2Prec<ExplPrec, PredPrec>, ItpRefutation> defaultTraceCheckerStrategy() {
        final ItpSolver itpSolver = solverFactory.createItpSolver();
        final Expr<BoolType> initFormula = xsts.getInitFormula();
        final Expr<BoolType> negProp = Not(xsts.getProp());
        // Stateful across refinements, so it must be created once and shared by every call.
        final AutoExpl autoExplInstance = autoExpl.builder.create(xsts);

        return switch (refinement) {
            case FW_BIN_ITP -> TraceCheckerStrategy.fwBinItpProd2ExplPredAuto(
                    initFormula, negProp, itpSolver, autoExplInstance, predSplit.splitter);
            case BW_BIN_ITP -> TraceCheckerStrategy.bwBinItpProd2ExplPredAuto(
                    initFormula, negProp, itpSolver, autoExplInstance, predSplit.splitter);
            case SEQ_ITP -> TraceCheckerStrategy.seqItpProd2ExplPredAuto(
                    initFormula, negProp, itpSolver, autoExplInstance, predSplit.splitter);
            default -> throw new UnsupportedOperationException("Unsupported refinement: " + refinement);
        };
    }

    private PredAbstractors.PredAbstractor buildPredAbstractor(final Solver abstractionSolver) {
        return switch (predAbstractor) {
            case BOOL -> PredAbstractors.booleanAbstractor(abstractionSolver);
            case CART -> PredAbstractors.cartesianAbstractor(abstractionSolver);
            case SPLIT -> PredAbstractors.booleanSplitAbstractor(abstractionSolver);
            default -> throw new UnsupportedOperationException("Unsupported predicate abstractor: " + predAbstractor);
        };
    }
}
