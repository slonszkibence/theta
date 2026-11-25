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
package hu.bme.mit.theta.xsts.analysis.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.algorithm.compositional.CexStatus;
import hu.bme.mit.theta.analysis.algorithm.compositional.LearnedComponent;
import hu.bme.mit.theta.analysis.algorithm.compositional.inclusion.InclusionOracle;
import hu.bme.mit.theta.analysis.algorithm.compositional.membership.MembershipOracle;
import hu.bme.mit.theta.analysis.algorithm.compositional.realizability.CexToPrec;
import hu.bme.mit.theta.analysis.algorithm.compositional.realizability.RealizabilityChecker;
import hu.bme.mit.theta.analysis.dfa.ActionToSymbol;
import hu.bme.mit.theta.analysis.dfa.SymbolAction;
import hu.bme.mit.theta.analysis.impl.ActionMappingAnalysis;
import hu.bme.mit.theta.analysis.zone.ClockBounds;
import hu.bme.mit.theta.analysis.zone.ZoneInitFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.analysis.zone.ZoneStmtAnalysis;
import hu.bme.mit.theta.analysis.zone.ZoneStmtTransFunc;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.learnlib.oracle.EquivalenceOracle;

public final class XstsTimedComponent implements LearnedComponent<XstsAction, XstsAction> {

    private final ZonePrec prec;
    private final SearchStrategy searchStrategy;
    private final Logger logger;
    private final ActionToSymbol<XstsAction, XstsAction> actionToSymbol;
    private final MembershipOracle<ZoneState, XstsAction, ZonePrec> membershipOracle;
    private final RealizabilityChecker<ZonePrec, XstsAction, XstsAction> realizabilityChecker;

    private XstsTimedComponent(
            final Collection<? extends VarDecl<ClockType>> clocks,
            final ZonePrec prec,
            final CexToPrec<ZonePrec, XstsAction> cexToPrec,
            final SearchStrategy searchStrategy,
            final Logger logger) {
        final List<VarDecl<ClockType>> allClocks = List.copyOf(checkNotNull(clocks));
        this.prec = checkNotNull(prec);
        this.searchStrategy = checkNotNull(searchStrategy);
        this.logger = checkNotNull(logger);
        checkNotNull(cexToPrec);

        this.actionToSymbol = clockProjection(TimedXstsActionProjections.create(prec.getVars()));
        this.membershipOracle = MembershipOracle.create(zoneAnalysis(), prec);

        if (prec.getVars().containsAll(allClocks)) {
            this.realizabilityChecker = RealizabilityChecker.create(actionToSymbol, membershipOracle);
        } else {
            this.realizabilityChecker = RealizabilityChecker.create(
                    clockProjection(TimedXstsActionProjections.create()),
                    MembershipOracle.create(zoneAnalysis(), ZonePrec.of(allClocks)),
                    actionToSymbol,
                    membershipOracle,
                    prec,
                    cexToPrec,
                    refinedPrec -> new XstsTimedComponent(allClocks, refinedPrec, cexToPrec, searchStrategy, logger));
        }
    }

    public static XstsTimedComponent create(
            final Collection<? extends VarDecl<ClockType>> clocks,
            final ZonePrec prec,
            final CexToPrec<ZonePrec, XstsAction> cexToPrec,
            final SearchStrategy searchStrategy,
            final Logger logger) {
        return new XstsTimedComponent(clocks, prec, cexToPrec, searchStrategy, logger);
    }

    @Override
    public ActionToSymbol<XstsAction, XstsAction> getActionToSymbol() {
        return actionToSymbol;
    }

    @Override
    public MembershipOracle<ZoneState, XstsAction, ZonePrec> getMembershipOracle() {
        return membershipOracle;
    }

    @Override
    public EquivalenceOracle.DFAEquivalenceOracle<XstsAction> getEquivalenceOracle() {
        return (hypothesis, inputs) -> {
            final ClockBounds bounds = ClockBounds.fromStmts(prec.getVars(), stmtsOf(inputs));
            return InclusionOracle.builder(NormalizedZoneAnalysis.create(bounds), prec, membershipOracle)
                    .searchStrategy(searchStrategy)
                    .logger(logger)
                    .build()
                    .findCounterExample(hypothesis, inputs);
        };
    }

    @Override
    public CexStatus<XstsAction, XstsAction> checkCex(final List<XstsAction> cexActions) {
        return realizabilityChecker.check(cexActions);
    }

    private static ActionToSymbol<XstsAction, XstsAction> clockProjection(
            final TimedXstsActionProjections projections) {
        return action -> {
            final XstsAction clockAction = projections.clockProjection(action);
            return clockAction.getStmts().isEmpty() ? Optional.empty() : Optional.of(clockAction);
        };
    }

    private static Analysis<ZoneState, SymbolAction<XstsAction>, ZonePrec> zoneAnalysis() {
        return ActionMappingAnalysis.create(ZoneStmtAnalysis.getInstance(), SymbolAction::getSymbol);
    }

    private static List<Stmt> stmtsOf(final Collection<? extends XstsAction> actions) {
        final List<Stmt> stmts = new ArrayList<>();
        for (final XstsAction action : actions) {
            stmts.addAll(action.getStmts());
        }
        return stmts;
    }

    private static final class NormalizedZoneAnalysis
            implements Analysis<ZoneState, SymbolAction<XstsAction>, ZonePrec> {

        private final PartialOrd<ZoneState> partialOrd;
        private final TransFunc<ZoneState, SymbolAction<XstsAction>, ZonePrec> transFunc;

        private NormalizedZoneAnalysis(final ClockBounds bounds) {
            this.partialOrd = bounds::isLeq;
            this.transFunc = (state, action, prec) -> {
                final List<ZoneState> succStates = new ArrayList<>();
                for (final ZoneState succState :
                        ZoneStmtTransFunc.getInstance().getSuccStates(state, action.getSymbol(), prec)) {
                    succStates.add(bounds.normalize(succState));
                }
                return succStates;
            };
        }

        static NormalizedZoneAnalysis create(final ClockBounds bounds) {
            return new NormalizedZoneAnalysis(checkNotNull(bounds));
        }

        @Override
        public PartialOrd<ZoneState> getPartialOrd() {
            return partialOrd;
        }

        @Override
        public InitFunc<ZoneState, ZonePrec> getInitFunc() {
            return ZoneInitFunc.getInstance();
        }

        @Override
        public TransFunc<ZoneState, SymbolAction<XstsAction>, ZonePrec> getTransFunc() {
            return transFunc;
        }
    }
}
