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

package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaLts;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.Collection;

public class DiscreteCompositionLts<S extends State, prodS>
        implements LTS<DiscreteCompositionState<S, prodS>, XtaAction> {
    private final LTS<XtaState<?>, XtaAction> innerLts;

    private DiscreteCompositionLts(final XtaSystem xtaSystem) {
        this.innerLts = XtaLts.create(checkNotNull(xtaSystem));
    }

    public static <D extends State, prodS> DiscreteCompositionLts<D, prodS> create(
            final XtaSystem xtaSystem) {
        return new DiscreteCompositionLts<>(xtaSystem);
    }

    @Override
    public Collection<XtaAction> getEnabledActionsFor(
            final DiscreteCompositionState<S, prodS> compositionState) {
        return innerLts.getEnabledActionsFor(compositionState.getXtaState());
    }
}
