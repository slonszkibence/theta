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

package hu.bme.mit.theta.xta.analysis.lazy;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.AbstractorResult;
import hu.bme.mit.theta.analysis.algorithm.lazy.LazyAbstractor;
import hu.bme.mit.theta.analysis.algorithm.lazy.LazyState;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;

public class LazyXtaAbstractorConfig<SConcr extends State, SAbstr extends State, P extends Prec> {
    private final Abstractor<LazyState<XtaState<SConcr>, XtaState<SAbstr>>, XtaAction, P> lazyXtaAbstractor;
    private final P prec;
    private ARG<LazyState<XtaState<SConcr>, XtaState<SAbstr>>, XtaAction> arg;

    public LazyXtaAbstractorConfig(final Abstractor<LazyState<XtaState<SConcr>, XtaState<SAbstr>>, XtaAction, P> lazyXtaAbstractor, final P prec) {
        this.lazyXtaAbstractor = lazyXtaAbstractor;
        this.prec = prec;
    }

    public AbstractorResult check() {
        arg = lazyXtaAbstractor.createArg();
        return lazyXtaAbstractor.check(arg, prec);
    }

    public ARG<LazyState<XtaState<SConcr>, XtaState<SAbstr>>, XtaAction> getArg() {
        return arg;
    }
}
