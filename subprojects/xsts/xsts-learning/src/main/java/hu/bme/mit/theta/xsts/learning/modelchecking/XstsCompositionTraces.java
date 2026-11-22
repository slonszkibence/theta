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

package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class XstsCompositionTraces {

    private XstsCompositionTraces() {
    }

    public static Optional<Trace<XstsState<?>, XstsAction>> projectToData(final Trace<?, ?> trace) {
        if (trace.getStates().isEmpty()
                || !(trace.getState(0) instanceof XstsCompositionState<?, ?>)) {
            return Optional.empty();
        }

        final TimedXstsActionProjections projections = TimedXstsActionProjections.create();

        final List<XstsState<?>> states = trace.getStates().stream()
                .map(state -> (XstsState<?>) ((XstsCompositionState<?, ?>) state).getXstsState())
                .collect(Collectors.toList());
        final List<XstsAction> actions = trace.getActions().stream()
                .map(XstsAction.class::cast)
                .map(projections::dataProjection)
                .toList();

        return Optional.of(Trace.of(states, actions));
    }
}
