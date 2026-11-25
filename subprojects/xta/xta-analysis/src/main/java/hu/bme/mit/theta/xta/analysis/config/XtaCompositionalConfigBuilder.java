/*
 *  Copyright 2026 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.xta.analysis.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.RefinerResult;
import hu.bme.mit.theta.analysis.algorithm.compositional.CompositionalChecker;
import hu.bme.mit.theta.analysis.algorithm.compositional.learning.AssumptionLearner;
import hu.bme.mit.theta.analysis.dfa.DfaAnalysis;
import hu.bme.mit.theta.analysis.dfa.DfaState;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.expl.ItpRefToExplPrec;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceBwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceFwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceSeqItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.JoiningPrecRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.RefutationToPrec;
import hu.bme.mit.theta.analysis.expr.refinement.SingleExprTraceRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.autoexpl.NewAtomsAutoExpl;
import hu.bme.mit.theta.analysis.impl.ActionMappingAnalysis;
import hu.bme.mit.theta.analysis.impl.PrecMappingAnalysis;
import hu.bme.mit.theta.analysis.pred.ItpRefToPredPrec;
import hu.bme.mit.theta.analysis.pred.PredAbstractors;
import hu.bme.mit.theta.analysis.pred.PredAnalysis;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.AutomaticItpRefToProd2ExplPredPrec;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAbstractors;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAnalysis;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredStrengtheningOperator;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.XtaLts;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.analysis.compositional.TimingPrec;
import hu.bme.mit.theta.xta.analysis.compositional.TimingPrecRefiner;
import hu.bme.mit.theta.xta.analysis.compositional.XtaClockProjection;
import hu.bme.mit.theta.xta.analysis.compositional.XtaDataExprTraceChecker;
import hu.bme.mit.theta.xta.analysis.compositional.XtaDataProjection;
import hu.bme.mit.theta.xta.analysis.compositional.XtaTimedComponent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import net.automatalib.automaton.fsa.impl.FastDFAState;

public final class XtaCompositionalConfigBuilder {

    public enum DataDomain {
        EXPL, PRED_BOOL, PRED_CART, PRED_SPLIT, EXPL_PRED_BOOL, EXPL_PRED_CART, EXPL_PRED_SPLIT
    }

    public enum Refinement {
        SEQ_ITP, FW_BIN_ITP, BW_BIN_ITP
    }

    public enum InitPrec {
        ALL_DATA, EMPTY
    }

    public enum TrackedClocks {
        ALL,
        NECESSARY_SUBSET,
        NECESSARY_CONSTRAINTS
    }

    private final DataDomain domain;
    private final Refinement refinement;
    private final SolverFactory solverFactory;
    private XtaConfigBuilder.PredSplit predSplit = XtaConfigBuilder.PredSplit.ATOMS;
    private InitPrec initPrec = InitPrec.ALL_DATA;
    private int maxEnum = 250;
    private SearchStrategy search = SearchStrategy.BFS;
    private PruneStrategy pruneStrategy = PruneStrategy.FULL;
    private AssumptionLearner.LearningAlgorithmType learningAlgorithmType = AssumptionLearner.LearningAlgorithmType.TTT;
    private TrackedClocks trackedClocks = TrackedClocks.ALL;
    private SearchStrategy inclusionSearch = SearchStrategy.BFS;
    private boolean inclusionSubsumption = false;
    private boolean invariantBatching = false;
    private Logger logger = NullLogger.getInstance();

    public XtaCompositionalConfigBuilder(
            final DataDomain domain, final Refinement refinement, final SolverFactory solverFactory) {
        this.domain = checkNotNull(domain);
        this.refinement = checkNotNull(refinement);
        this.solverFactory = checkNotNull(solverFactory);
    }

    public XtaCompositionalConfigBuilder predSplit(final XtaConfigBuilder.PredSplit predSplit) {
        this.predSplit = checkNotNull(predSplit);
        return this;
    }

    public XtaCompositionalConfigBuilder initPrec(final InitPrec initPrec) {
        this.initPrec = checkNotNull(initPrec);
        return this;
    }

    public XtaCompositionalConfigBuilder maxEnum(final int maxEnum) {
        this.maxEnum = maxEnum;
        return this;
    }

    /** The search strategy of the check of the composition. */
    public XtaCompositionalConfigBuilder search(final SearchStrategy search) {
        this.search = checkNotNull(search);
        return this;
    }

    public XtaCompositionalConfigBuilder pruneStrategy(final PruneStrategy pruneStrategy) {
        this.pruneStrategy = checkNotNull(pruneStrategy);
        return this;
    }

    public XtaCompositionalConfigBuilder learningAlgorithmType(
            final AssumptionLearner.LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        return this;
    }

    public XtaCompositionalConfigBuilder trackedClocks(final TrackedClocks trackedClocks) {
        this.trackedClocks = checkNotNull(trackedClocks);
        return this;
    }

    public XtaCompositionalConfigBuilder inclusionSearch(final SearchStrategy inclusionSearch) {
        this.inclusionSearch = checkNotNull(inclusionSearch);
        return this;
    }

    public XtaCompositionalConfigBuilder inclusionSubsumption(final boolean inclusionSubsumption) {
        this.inclusionSubsumption = inclusionSubsumption;
        return this;
    }

    public XtaCompositionalConfigBuilder invariantBatching(final boolean invariantBatching) {
        this.invariantBatching = invariantBatching;
        return this;
    }

    public XtaCompositionalConfigBuilder logger(final Logger logger) {
        this.logger = checkNotNull(logger);
        return this;
    }

    public XtaConfig<?, XtaAction, UnitPrec> build(final XtaSystem system) {
        final Solver solver = solverFactory.createSolver();
        final Expr<BoolType> initExpr = system.getInitVal().toExpr();

        return switch (domain) {
            case EXPL -> build(system,
                    ExplStmtAnalysis.create(solverFactory.createSolver(), initExpr, maxEnum),
                    initPrec == InitPrec.ALL_DATA ? ExplPrec.of(system.getDataVars()) : ExplPrec.empty(),
                    new ItpRefToExplPrec());
            case PRED_BOOL, PRED_CART, PRED_SPLIT -> build(system,
                    PredAnalysis.create(solver, predAbstractor(solver), initExpr),
                    initPrec == InitPrec.ALL_DATA ? PredPrec.of(guardAtoms(system)) : PredPrec.of(),
                    new ItpRefToPredPrec(predSplit.splitter));
            case EXPL_PRED_BOOL, EXPL_PRED_CART, EXPL_PRED_SPLIT -> build(system,
                    Prod2ExplPredAnalysis.create(
                            ExplStmtAnalysis.create(solverFactory.createSolver(), initExpr, maxEnum),
                            PredAnalysis.create(solver, predAbstractor(solver), initExpr),
                            Prod2ExplPredStrengtheningOperator.create(solver),
                            Prod2ExplPredAbstractors.booleanAbstractor(solver)),
                    Prod2Prec.of(
                            initPrec == InitPrec.ALL_DATA ? ExplPrec.of(system.getDataVars()) : ExplPrec.empty(),
                            PredPrec.of()),
                    AutomaticItpRefToProd2ExplPredPrec.create(
                            new NewAtomsAutoExpl(Set.of(), guardAtoms(system), 0), predSplit.splitter));
        };
    }

    private <S extends ExprState, P extends Prec> XtaConfig<?, XtaAction, UnitPrec> build(
            final XtaSystem system,
            final Analysis<S, ? super StmtAction, P> dataAnalysis,
            final P initPrec,
            final RefutationToPrec<P, ItpRefutation> refToPrec) {
        final XtaAnalysis<S, P> easyAnalysis = XtaAnalysis.create(system,
                ActionMappingAnalysis.<S, XtaAction, StmtAction, P>create(dataAnalysis, XtaDataProjection::of));
        final XtaLts easyLts = XtaLts.create(system);
        final ExprTraceChecker<ItpRefutation> traceChecker = XtaDataExprTraceChecker.create(createTraceChecker());
        final AtomicReference<P> lastPrec = new AtomicReference<>(initPrec);

        final Function<DfaAnalysis<FastDFAState, XtaAction, XtaClockProjection>,
                SafetyChecker<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, UnitPrec>>
                checkerFactory = assumption -> {
                    final Analysis<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, P> analysis =
                            PrecMappingAnalysis.create(Prod2Analysis.create(assumption, easyAnalysis),
                                    (final P prec) -> Prod2Prec.of(UnitPrec.getInstance(), prec));
                    final LTS<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction> lts =
                            state -> easyLts.getEnabledActionsFor(state.getState2());
                    final ArgBuilder<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, P> argBuilder =
                            ArgBuilder.create(lts, analysis, state -> state.getState2().isError(), true);
                    final Abstractor<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, P> abstractor =
                            BasicAbstractor.builder(argBuilder)
                                    .waitlist(search.createWaitlist())
                                    .projection(state -> List.of(
                                            state.getState1(), state.getState2().getLocs(), state.getState2().getState()))
                                    .logger(logger)
                                    .build();
                    final Refiner<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, P> refiner =
                            SingleExprTraceRefiner.create(traceChecker, JoiningPrecRefiner.create(refToPrec),
                                    pruneStrategy, logger);
                    final Refiner<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, P> precKeepingRefiner =
                            (arg, prec) -> {
                                final RefinerResult<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, P>
                                        result = refiner.refine(arg, prec);
                                if (result.isSpurious()) {
                                    lastPrec.set(result.asSpurious().getRefinedPrec());
                                }
                                return result;
                            };
                    final CegarChecker<Prod2State<DfaState<FastDFAState>, XtaState<S>>, XtaAction, P> cegarChecker =
                            CegarChecker.create(abstractor, precKeepingRefiner, logger);
                    return unitPrec -> cegarChecker.check(lastPrec.get());
                };

        final XtaTimedComponent component = XtaTimedComponent.create(
                system, initialTimingPrec(system), timingPrecRefiner(system), inclusionSearch, inclusionSubsumption,
                logger);
        return XtaConfig.create(
                CompositionalChecker.create(component, checkerFactory, learningAlgorithmType, logger),
                UnitPrec.getInstance());
    }

    private TimingPrec initialTimingPrec(final XtaSystem system) {
        return switch (trackedClocks) {
            case ALL -> TimingPrec.all(system);
            case NECESSARY_SUBSET -> TimingPrec.empty();
            case NECESSARY_CONSTRAINTS -> TimingPrec.emptyClockConstraints();
        };
    }

    private TimingPrecRefiner timingPrecRefiner(final XtaSystem system) {
        return trackedClocks == TrackedClocks.NECESSARY_CONSTRAINTS
                ? TimingPrecRefiner.createForClockConstraints(system, invariantBatching)
                : TimingPrecRefiner.create(system);
    }

    private ExprTraceChecker<ItpRefutation> createTraceChecker() {
        final ItpSolver itpSolver = solverFactory.createItpSolver();
        return switch (refinement) {
            case SEQ_ITP -> ExprTraceSeqItpChecker.create(True(), True(), itpSolver);
            case FW_BIN_ITP -> ExprTraceFwBinItpChecker.create(True(), True(), itpSolver);
            case BW_BIN_ITP -> ExprTraceBwBinItpChecker.create(True(), True(), itpSolver);
        };
    }

    private PredAbstractors.PredAbstractor predAbstractor(final Solver solver) {
        return switch (domain) {
            case PRED_BOOL, EXPL_PRED_BOOL -> PredAbstractors.booleanAbstractor(solver);
            case PRED_CART, EXPL_PRED_CART -> PredAbstractors.cartesianAbstractor(solver);
            case PRED_SPLIT, EXPL_PRED_SPLIT -> PredAbstractors.booleanSplitAbstractor(solver);
            case EXPL -> throw new UnsupportedOperationException("The explicit domain has no predicate abstractor");
        };
    }

    private static Set<Expr<BoolType>> guardAtoms(final XtaSystem system) {
        final Set<Expr<BoolType>> atoms = new LinkedHashSet<>();
        for (final XtaProcess process : system.getProcesses()) {
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
}
