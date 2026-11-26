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
package hu.bme.mit.theta.xta.analysis.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.algorithm.compositional.CexStatus;
import hu.bme.mit.theta.analysis.algorithm.compositional.LearnedComponent;
import hu.bme.mit.theta.analysis.algorithm.compositional.inclusion.InclusionOracle;
import hu.bme.mit.theta.analysis.algorithm.compositional.membership.MembershipOracle;
import hu.bme.mit.theta.analysis.algorithm.compositional.realizability.CexToPrec;
import hu.bme.mit.theta.analysis.algorithm.compositional.realizability.RealizabilityChecker;
import hu.bme.mit.theta.analysis.dfa.ActionToSymbol;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public final class XtaTimedComponent implements LearnedComponent<XtaAction, XtaClockProjection> {

    private final ActionToSymbol<XtaAction, XtaClockProjection> actionToSymbol;
    private final MembershipOracle<XtaTimedStepper.Config, XtaClockProjection, XtaTimingPrec> membershipOracle;
    private final InclusionOracle<XtaTimedStepper.Config, XtaClockProjection, XtaTimingPrec> equivalenceOracle;
    private final RealizabilityChecker<XtaTimingPrec, XtaAction, XtaClockProjection> realizabilityChecker;

    private XtaTimedComponent(
            final XtaSystem system,
            final XtaTimingPrec prec,
            final CexToPrec<XtaTimingPrec, XtaAction> cexToPrec,
            final SearchStrategy searchStrategy,
            final boolean subsumption,
            final Logger logger) {
        checkNotNull(system);
        checkNotNull(cexToPrec);
        final XtaTimedStepper stepper = XtaTimedStepper.create(system, prec);
        final XtaTimingAnalysis analysis = XtaTimingAnalysis.create(stepper, subsumption);
        final Function<XtaTimedStepper.Config, ?> projection =
                subsumption ? XtaTimedStepper.Config::getLocs : config -> config;

        this.actionToSymbol = action -> Optional.of(stepper.project(action));
        this.membershipOracle = MembershipOracle.create(analysis, prec);
        this.equivalenceOracle = InclusionOracle.builder(analysis, prec, membershipOracle)
                .searchStrategy(searchStrategy)
                .projection(projection)
                .logger(logger)
                .build();

        final XtaTimingPrec fullPrec = XtaTimingPrec.all(system);
        if (prec.equals(fullPrec)) {
            this.realizabilityChecker = RealizabilityChecker.create(actionToSymbol, membershipOracle);
        } else {
            final XtaTimedStepper fullStepper = XtaTimedStepper.create(system, fullPrec);
            this.realizabilityChecker = RealizabilityChecker.create(
                    action -> Optional.of(fullStepper.project(action)),
                    MembershipOracle.create(XtaTimingAnalysis.create(fullStepper, false), fullPrec),
                    actionToSymbol,
                    membershipOracle,
                    prec,
                    cexToPrec,
                    refinedPrec -> new XtaTimedComponent(
                            system, refinedPrec, cexToPrec, searchStrategy, subsumption, logger));
        }
    }

    public static XtaTimedComponent create(
            final XtaSystem system,
            final XtaTimingPrec prec,
            final CexToPrec<XtaTimingPrec, XtaAction> cexToPrec,
            final SearchStrategy searchStrategy,
            final boolean subsumption,
            final Logger logger) {
        return new XtaTimedComponent(system, prec, cexToPrec, searchStrategy, subsumption, logger);
    }

    @Override
    public ActionToSymbol<XtaAction, XtaClockProjection> getActionToSymbol() {
        return actionToSymbol;
    }

    @Override
    public MembershipOracle<XtaTimedStepper.Config, XtaClockProjection, XtaTimingPrec> getMembershipOracle() {
        return membershipOracle;
    }

    @Override
    public InclusionOracle<XtaTimedStepper.Config, XtaClockProjection, XtaTimingPrec> getEquivalenceOracle() {
        return equivalenceOracle;
    }

    @Override
    public CexStatus<XtaAction, XtaClockProjection> checkCex(final List<XtaAction> cexActions) {
        return realizabilityChecker.check(cexActions);
    }
}
