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
package hu.bme.mit.theta.analysis.impl;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ActionMappingAnalysis<S extends State, A extends Action, AI extends Action, P extends Prec>
    implements Analysis<S, A, P> {

    private final Analysis<S, ? super AI, P> analysis;
    private final TransFunc<S, A, P> transFunc;

    private ActionMappingAnalysis(final Analysis<S, ? super AI, P> analysis,
                                  final Function<? super A, ? extends AI> mapping) {
        this.analysis = checkNotNull(analysis);
        checkNotNull(mapping);
        this.transFunc = (state, action, prec) -> analysis.getTransFunc().getSuccStates(state, mapping.apply(action), prec);
    }

    public static <S extends State, A extends Action, AI extends Action, P extends Prec> ActionMappingAnalysis<S, A, AI, P> create(
            final Analysis<S, ? super AI, P> analysis,
            final Function<? super A, ? extends AI> mapping) {
        return new ActionMappingAnalysis<>(analysis, mapping);
    }

    @Override
    public PartialOrd<S> getPartialOrd() {
        return analysis.getPartialOrd();
    }

    @Override
    public InitFunc<S, P> getInitFunc() {
        return analysis.getInitFunc();
    }

    @Override
    public TransFunc<S, A, P> getTransFunc() {
        return transFunc;
    }
}
