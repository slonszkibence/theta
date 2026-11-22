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

package hu.bme.mit.theta.xta.analysis.proba;

import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xta.analysis.XtaState;

import java.util.function.Predicate;

public class XtaStatePredicate <P extends Predicate, S extends ExprState> implements Predicate<XtaState<S>>{
    private final P pred;

    public XtaStatePredicate(final P pred) {
        this.pred = pred;
    }

    @Override
    public boolean test(XtaState<S> state) {
        return pred.test(state.getState());
    }
}
