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

package hu.bme.mit.theta.xcfa.analysis.impl.lazy;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.lazy.InitAbstractor;
import hu.bme.mit.theta.analysis.algorithm.lazy.LazyState;
import hu.bme.mit.theta.analysis.algorithm.lazy.LazyStrategy;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaAction;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaState;

import java.util.Collection;
import java.util.function.Function;
public  class XcfaLazyStrategy<DConcr extends ExprState, DAbstr extends ExprState, A extends XcfaAction, DPrec extends Prec> implements LazyStrategy<DConcr, DAbstr, LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> {
    private final LazyStrategy<DConcr, DAbstr, LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> lazyStrategy;

    public XcfaLazyStrategy(LazyStrategy<DConcr, DAbstr, LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> lazyStrategy) {
        this.lazyStrategy = lazyStrategy;
    }

    @Override
    public Function<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, ?> getProjection() {
        return s -> Tuple2.of(s.getConcrState().getCurrentLoc(), lazyStrategy.getProjection().apply(s));
    }

    @Override
    public InitAbstractor<DConcr, DAbstr> getInitAbstractor() {
        return lazyStrategy.getInitAbstractor();
    }

    @Override
    public PartialOrd<DAbstr> getPartialOrd() {
        return lazyStrategy.getPartialOrd();
    }

    @Override
    public boolean inconsistentState(LazyState<XcfaState<DConcr>, XcfaState<DAbstr>> state) {
        return lazyStrategy.inconsistentState(state);
    }

    @Override
    public boolean mightCover(ArgNode<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> coveree, ArgNode<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> coverer) {
        return lazyStrategy.mightCover(coveree, coverer);
    }

    @Override
    public void cover(ArgNode<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> coveree, ArgNode<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> coverer, Collection<ArgNode<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A>> uncoveredNodes) {
        lazyStrategy.cover(coveree, coverer, uncoveredNodes);
    }

    @Override
    public void disable(ArgNode<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A> node, A action, LazyState<XcfaState<DConcr>, XcfaState<DAbstr>> succState, Collection<ArgNode<LazyState<XcfaState<DConcr>, XcfaState<DAbstr>>, A>> uncoveredNodes) {
        lazyStrategy.disable(node, action, succState, uncoveredNodes);
    }
}
