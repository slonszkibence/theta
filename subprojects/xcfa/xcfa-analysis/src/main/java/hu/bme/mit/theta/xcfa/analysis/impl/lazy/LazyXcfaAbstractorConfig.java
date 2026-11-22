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

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.AbstractorResult;
import hu.bme.mit.theta.analysis.algorithm.lazy.LazyState;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaAction;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaPrec;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaState;

public class LazyXcfaAbstractorConfig<SConcr extends ExprState, SAbstr extends ExprState, A extends XcfaAction, P extends Prec> {
    private final Abstractor<LazyState<XcfaState<SConcr>, XcfaState<SAbstr>>, A, XcfaPrec<P>> lazyXcfaAbstractor;
    private final XcfaPrec<P> prec;
    private ARG<LazyState<XcfaState<SConcr>, XcfaState<SAbstr>>, A> arg;

    public LazyXcfaAbstractorConfig(final Abstractor<LazyState<XcfaState<SConcr>, XcfaState<SAbstr>>, A, XcfaPrec<P>> lazyXcfaAbstractor, final XcfaPrec<P> prec) {
        this.lazyXcfaAbstractor = lazyXcfaAbstractor;
        this.prec = prec;
    }

    public AbstractorResult check() {
        arg = lazyXcfaAbstractor.createArg();
        return lazyXcfaAbstractor.check(arg, prec);
    }

    public ARG<LazyState<XcfaState<SConcr>, XcfaState<SAbstr>>, A> getArg() {
        return arg;
    }
}
