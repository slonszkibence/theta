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

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.dfa.SymbolAction;
import hu.bme.mit.theta.analysis.zone.ClockBounds;

import java.util.List;

public final class XtaTimingAnalysis
        implements Analysis<TimedStepper.Config, SymbolAction<XtaClockProjection>, TimingPrec> {

    private final PartialOrd<TimedStepper.Config> partialOrd;
    private final InitFunc<TimedStepper.Config, TimingPrec> initFunc;
    private final TransFunc<TimedStepper.Config, SymbolAction<XtaClockProjection>, TimingPrec> transFunc;

    private XtaTimingAnalysis(final TimedStepper stepper, final boolean subsumption) {
        checkNotNull(stepper);
        final ClockBounds bounds = stepper.getBounds();
        this.partialOrd = subsumption
                ? (config, that) -> config.getLocs().equals(that.getLocs())
                        && bounds.isLeq(config.getZone(), that.getZone())
                : TimedStepper.Config::equals;
        this.initFunc = prec -> List.of(stepper.initial());
        this.transFunc = (config, action, prec) -> stepper.step(config, action.getSymbol()).stream().toList();
    }

    public static XtaTimingAnalysis create(final TimedStepper stepper, final boolean subsumption) {
        return new XtaTimingAnalysis(stepper, subsumption);
    }

    @Override
    public PartialOrd<TimedStepper.Config> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<TimedStepper.Config, TimingPrec> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<TimedStepper.Config, SymbolAction<XtaClockProjection>, TimingPrec> getTransFunc() {
        return transFunc;
    }
}
