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
package hu.bme.mit.theta.xsts.analysis.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;
import static hu.bme.mit.theta.core.type.clocktype.ClockExprs.Clock;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

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
import hu.bme.mit.theta.analysis.algorithm.compositional.CompositionalChecker;
import hu.bme.mit.theta.analysis.algorithm.compositional.learning.AssumptionLearner;
import hu.bme.mit.theta.analysis.dfa.DfaAnalysis;
import hu.bme.mit.theta.analysis.dfa.DfaState;
import hu.bme.mit.theta.analysis.expl.ExplAnalysis;
import hu.bme.mit.theta.analysis.expl.ExplStateValuationExtractor;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.expl.ItpRefToExplPrec;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.ExprStatePredicate;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceBwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceFwBinItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceSeqItpChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.JoiningPrecRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.RefutationToPrec;
import hu.bme.mit.theta.analysis.expr.refinement.SingleExprTraceRefiner;
import hu.bme.mit.theta.analysis.impl.ActionMappingAnalysis;
import hu.bme.mit.theta.analysis.impl.PrecMappingAnalysis;
import hu.bme.mit.theta.analysis.pred.ItpRefToPredPrec;
import hu.bme.mit.theta.analysis.pred.PredAbstractors;
import hu.bme.mit.theta.analysis.pred.PredAnalysis;
import hu.bme.mit.theta.analysis.pred.PredStateValuationExtractor;
import hu.bme.mit.theta.analysis.prod2.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.AutomaticItpRefToProd2ExplPredPrec;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAbstractors;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredAnalysis;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredPreStrengtheningOperator;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredStateValuationExtractor;
import hu.bme.mit.theta.analysis.prod2.prod2explpred.Prod2ExplPredStrengtheningOperator;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.utils.ValuationExtractor;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsAnalysis;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.compositional.ClockPrecRefiner;
import hu.bme.mit.theta.xsts.analysis.config.TimedXstsConfigBuilder.ControlFlowSplitting;
import hu.bme.mit.theta.xsts.analysis.compositional.XstsDataExprTraceChecker;
import hu.bme.mit.theta.xsts.analysis.compositional.XstsTimedComponent;
import hu.bme.mit.theta.xsts.analysis.initprec.XstsInitPrec;
import hu.bme.mit.theta.xsts.analysis.timed.ControlFlowSplitXstsLts;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import net.automatalib.automaton.fsa.impl.FastDFAState;

public final class XstsCompositionalConfigBuilder {

    public enum TrackedClocks {
        ALL,
        NECESSARY_SUBSET
    }

    private final XstsConfigBuilder xstsConfig;
    private final SearchStrategy searchStrategy;
    private AssumptionLearner.LearningAlgorithmType learningAlgorithmType = AssumptionLearner.LearningAlgorithmType.TTT;
    private TrackedClocks trackedClocks = TrackedClocks.ALL;
    private ControlFlowSplitting controlFlowSplitting = ControlFlowSplitting.FEASIBLE_ACTIONS_ONLY;

    public XstsCompositionalConfigBuilder(final XstsConfigBuilder xstsConfig) {
        this.xstsConfig = checkNotNull(xstsConfig);
        this.searchStrategy = switch (xstsConfig.search) {
            case BFS -> SearchStrategy.BFS;
            case DFS -> SearchStrategy.DFS;
        };
    }

    public XstsCompositionalConfigBuilder learningAlgorithmType(
            final AssumptionLearner.LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        return this;
    }

    public XstsCompositionalConfigBuilder trackedClocks(final TrackedClocks trackedClocks) {
        this.trackedClocks = checkNotNull(trackedClocks);
        return this;
    }

    public XstsCompositionalConfigBuilder controlFlowSplitting(final ControlFlowSplitting controlFlowSplitting) {
        checkArgument(checkNotNull(controlFlowSplitting).controlFlowSplitting,
                "Compositional verification needs control flow splitting");
        this.controlFlowSplitting = controlFlowSplitting;
        return this;
    }

    public XstsConfig<?, XstsAction, UnitPrec> build(final XSTS xsts) {
        checkArgument(xstsConfig.clockReplacer == XstsConfigBuilder.ClockReplacer.NONE,
                "Compositional verification learns the timing behavior, they must not be replaced");
        final Solver abstractionSolver = xstsConfig.solverFactory.createSolver();
        final Expr<BoolType> initFormula = xsts.getInitFormula();
        final XstsInitPrec initPrec = xstsConfig.initPrec.builder;

        return switch (xstsConfig.domain) {
            case EXPL -> build(xsts,
                    ExplStmtAnalysis.create(abstractionSolver, initFormula, xstsConfig.maxEnum),
                    initPrec.createExpl(xsts),
                    new ItpRefToExplPrec(),
                    ExplStateValuationExtractor.getInstance());
            case PRED_BOOL, PRED_CART, PRED_SPLIT -> build(xsts,
                    PredAnalysis.create(abstractionSolver, predAbstractor(abstractionSolver), initFormula),
                    initPrec.createPred(xsts),
                    new ItpRefToPredPrec(xstsConfig.predSplit.splitter),
                    PredStateValuationExtractor.getInstance());
            case EXPL_PRED_BOOL, EXPL_PRED_CART, EXPL_PRED_SPLIT -> build(xsts,
                    Prod2Analysis.create(
                            ExplStmtAnalysis.create(abstractionSolver, initFormula, xstsConfig.maxEnum),
                            PredAnalysis.create(abstractionSolver, predAbstractor(abstractionSolver), initFormula),
                            Prod2ExplPredPreStrengtheningOperator.create(),
                            Prod2ExplPredStrengtheningOperator.create(abstractionSolver)),
                    initPrec.createProd2ExplPred(xsts),
                    AutomaticItpRefToProd2ExplPredPrec.create(
                            xstsConfig.autoExpl.builder.create(xsts), xstsConfig.predSplit.splitter),
                    Prod2ExplPredStateValuationExtractor.create(ExplStateValuationExtractor.getInstance()));
            case EXPL_PRED_COMBINED -> build(xsts,
                    Prod2ExplPredAnalysis.create(
                            ExplAnalysis.create(abstractionSolver, initFormula),
                            PredAnalysis.create(abstractionSolver,
                                    PredAbstractors.booleanAbstractor(abstractionSolver), initFormula),
                            Prod2ExplPredStrengtheningOperator.create(abstractionSolver),
                            Prod2ExplPredAbstractors.booleanAbstractor(abstractionSolver)),
                    initPrec.createProd2ExplPred(xsts),
                    AutomaticItpRefToProd2ExplPredPrec.create(
                            xstsConfig.autoExpl.builder.create(xsts), xstsConfig.predSplit.splitter),
                    Prod2ExplPredStateValuationExtractor.create(ExplStateValuationExtractor.getInstance()));
        };
    }

    private <S extends ExprState, P extends Prec> XstsConfig<?, XstsAction, UnitPrec> build(
            final XSTS xsts,
            final Analysis<S, ? super XstsAction, P> dataAnalysis,
            final P initPrec,
            final RefutationToPrec<P, ItpRefutation> refToPrec,
            final ValuationExtractor<S> valExtractor) {
        final List<VarDecl<ClockType>> clocks = xsts.getVars().stream()
                .filter(var -> var.getType() instanceof ClockType)
                .map(var -> cast(var, Clock()))
                .toList();
        checkArgument(!clocks.isEmpty(), "The model has no clocks, there is nothing to learn");
        final Logger logger = xstsConfig.logger;

        final TimedXstsActionProjections projections = TimedXstsActionProjections.create();
        final XstsAnalysis<S, P> easyAnalysis = XstsAnalysis.create(
                ActionMappingAnalysis.<S, XstsAction, XstsAction, P>create(dataAnalysis, projections::dataProjection));
        final ControlFlowSplitXstsLts<S> cfSplitLts =
                ControlFlowSplitXstsLts.create(xsts, xstsConfig.solverFactory.createSolver(), valExtractor);
        final LTS<XstsState<S>, XstsAction> easyLts = controlFlowSplitting.feasibleActionsOnly
                ? state -> feasibleActions(cfSplitLts, state)
                : cfSplitLts;
        final Predicate<ExprState> dataTarget =
                new ExprStatePredicate(Not(xsts.getProp()), xstsConfig.solverFactory.createSolver());
        final ExprTraceChecker<ItpRefutation> traceChecker =
                XstsDataExprTraceChecker.create(createTraceChecker(xsts), projections);

        final Function<DfaAnalysis<FastDFAState, XstsAction, XstsAction>,
                SafetyChecker<Prod2State<DfaState<FastDFAState>, XstsState<S>>, XstsAction, UnitPrec>>
                checkerFactory = assumption -> {
                    final Analysis<Prod2State<DfaState<FastDFAState>, XstsState<S>>, XstsAction, P> analysis =
                            PrecMappingAnalysis.create(Prod2Analysis.create(assumption, easyAnalysis),
                                    (final P prec) -> Prod2Prec.of(UnitPrec.getInstance(), prec));
                    final LTS<Prod2State<DfaState<FastDFAState>, XstsState<S>>, XstsAction> lts =
                            state -> easyLts.getEnabledActionsFor(state.getState2());
                    final ArgBuilder<Prod2State<DfaState<FastDFAState>, XstsState<S>>, XstsAction, P> argBuilder =
                            ArgBuilder.create(lts, analysis, state -> dataTarget.test(state.getState2().getState()), true);
                    final Abstractor<Prod2State<DfaState<FastDFAState>, XstsState<S>>, XstsAction, P> abstractor =
                            BasicAbstractor.builder(argBuilder)
                                    .waitlist(searchStrategy.createWaitlist())
                                    .projection(Prod2State::getState1)
                                    .logger(logger)
                                    .build();
                    final Refiner<Prod2State<DfaState<FastDFAState>, XstsState<S>>, XstsAction, P> refiner =
                            SingleExprTraceRefiner.create(traceChecker, JoiningPrecRefiner.create(refToPrec),
                                    xstsConfig.pruneStrategy, logger);
                    final CegarChecker<Prod2State<DfaState<FastDFAState>, XstsState<S>>, XstsAction, P> cegarChecker =
                            CegarChecker.create(abstractor, refiner, logger);
                    return unitPrec -> cegarChecker.check(initPrec);
                };

        final ZonePrec zonePrec = switch (trackedClocks) {
            case ALL -> ZonePrec.of(clocks);
            case NECESSARY_SUBSET -> ZonePrec.of(List.of());
        };
        final XstsTimedComponent component = XstsTimedComponent.create(
                clocks, zonePrec, ClockPrecRefiner.create(clocks), searchStrategy, logger);
        return XstsConfig.create(
                CompositionalChecker.create(component, checkerFactory, learningAlgorithmType, logger),
                UnitPrec.getInstance());
    }

    private static <S extends ExprState> List<XstsAction> feasibleActions(
            final ControlFlowSplitXstsLts<S> lts, final XstsState<S> state) {
        lts.reset();
        lts.splitControlFlows(state, true);
        lts.updateSourceAbstractState(state);
        final List<XstsAction> actions = new ArrayList<>();
        for (Optional<XstsAction> action = lts.getNextEnabledAction(); action.isPresent();
                action = lts.getNextEnabledAction()) {
            actions.add(action.get());
        }
        return actions;
    }

    private ExprTraceChecker<ItpRefutation> createTraceChecker(final XSTS xsts) {
        final Expr<BoolType> initFormula = xsts.getInitFormula();
        final Expr<BoolType> negProp = Not(xsts.getProp());
        final ItpSolver itpSolver = xstsConfig.solverFactory.createItpSolver();
        return switch (xstsConfig.refinement) {
            case FW_BIN_ITP -> ExprTraceFwBinItpChecker.create(initFormula, negProp, itpSolver);
            case BW_BIN_ITP -> ExprTraceBwBinItpChecker.create(initFormula, negProp, itpSolver);
            case SEQ_ITP -> ExprTraceSeqItpChecker.create(initFormula, negProp, itpSolver);
            case MULTI_SEQ, UNSAT_CORE -> throw new UnsupportedOperationException(String.format(
                    "%s refinement is not supported by compositional verification", xstsConfig.refinement));
        };
    }

    private PredAbstractors.PredAbstractor predAbstractor(final Solver solver) {
        return switch (xstsConfig.domain) {
            case PRED_BOOL, EXPL_PRED_BOOL -> PredAbstractors.booleanAbstractor(solver);
            case PRED_CART, EXPL_PRED_CART -> PredAbstractors.cartesianAbstractor(solver);
            case PRED_SPLIT, EXPL_PRED_SPLIT -> PredAbstractors.booleanSplitAbstractor(solver);
            default -> throw new UnsupportedOperationException(
                    String.format("%s domain has no predicate abstractor", xstsConfig.domain));
        };
    }
}
