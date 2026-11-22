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

import hu.bme.mit.theta.analysis.*;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsCompositionAnalysis<S extends ExprState, prodS, P extends Prec>
        implements Analysis<XstsCompositionState<S, prodS>, XstsAction, P> {

    private final InitFunc<XstsCompositionState<S, prodS>, P> initFunc;
    private final TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> transFunc;
    private final PartialOrd<XstsCompositionState<S, prodS>> partialOrd;

    private XstsCompositionAnalysis(
            InitFunc<XstsCompositionState<S, prodS>, P> initFunc,
            TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> transFunc,
            PartialOrd<XstsCompositionState<S, prodS>> partialOrd) {
        this.initFunc = checkNotNull(initFunc);
        this.transFunc = checkNotNull(transFunc);
        this.partialOrd = checkNotNull(partialOrd);
    }

    public static <S extends ExprState, prodS, P extends Prec> XstsCompositionAnalysis<S, prodS, P> create(
            InitFunc<XstsCompositionState<S, prodS>, P> initFunc,
            TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> transFunc,
            PartialOrd<XstsCompositionState<S, prodS>> partialOrd) {
        return new XstsCompositionAnalysis<>(initFunc, transFunc, partialOrd);
    }

    @Override
    public PartialOrd<XstsCompositionState<S, prodS>> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<XstsCompositionState<S, prodS>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> getTransFunc() {
        return transFunc;
    }
}
