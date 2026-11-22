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

package hu.bme.mit.theta.analysis.expl;

import hu.bme.mit.theta.analysis.utils.ValuationExtractor;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.LitExpr;

import java.util.Collection;
import java.util.Map;

public final class ExplStateValuationExtractor implements ValuationExtractor<ExplState> {

    private static final class LazyHolder {
        private static final ExplStateValuationExtractor INSTANCE = new ExplStateValuationExtractor();
    }

    private ExplStateValuationExtractor() {
    }

    public static ExplStateValuationExtractor getInstance() {
        return ExplStateValuationExtractor.LazyHolder.INSTANCE;
    }

    @Override
    public Valuation extractValuation(ExplState state) {
        return state.getVal();
    }

    @Override
    public Valuation extractValuationForVars(ExplState state, Collection<VarDecl<?>> vars) {

        final ImmutableValuation.Builder builder = ImmutableValuation.builder();

        final Map<Decl<?>, LitExpr<?>> varToValue = state.getVal().toMap();

        for (final VarDecl<?> varDecl : vars) {
            final LitExpr<?> value = varToValue.get(varDecl);
            if (value != null) {
                builder.put(varDecl, value);
            }
        }

        return builder.build();
    }
}
