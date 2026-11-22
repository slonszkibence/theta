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

package hu.bme.mit.theta.analysis.prod2.prod2explpred;

import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStateValuationExtractor;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.utils.ValuationExtractor;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.Valuation;

import java.util.Collection;

public class Prod2ExplPredStateValuationExtractor implements ValuationExtractor<Prod2State<ExplState, PredState>> {

    private final ExplStateValuationExtractor explValExtractor;

    private Prod2ExplPredStateValuationExtractor(final ExplStateValuationExtractor explValExtractor) {
        this.explValExtractor = explValExtractor;
    }

    public static Prod2ExplPredStateValuationExtractor create(final ExplStateValuationExtractor explStateValExtractor) {
        return new Prod2ExplPredStateValuationExtractor(explStateValExtractor);
    }

    @Override
    public Valuation extractValuation(Prod2State<ExplState, PredState> state) {
        final ExplState explState = state.getState1();
        return explValExtractor.extractValuation(explState);
    }

    @Override
    public Valuation extractValuationForVars(Prod2State<ExplState, PredState> state, Collection<VarDecl<?>> vars) {
        final ExplState explState = state.getState1();
        return explValExtractor.extractValuationForVars(explState, vars);
    }
}
