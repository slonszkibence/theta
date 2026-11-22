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

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.analysis.XtaState;

import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionInitFunc<S extends State, prodS, P extends Prec>
        implements InitFunc<DiscreteCompositionState<S, prodS>, P> {
    private final InitFunc<XtaState<S>, P> innerInitFunc;
    private final DFA<prodS, String> productDfa;

    private DiscreteCompositionInitFunc(InitFunc<XtaState<S>, P> innerInitFunc,
                                        DFA<prodS, String> productDfa) {
        this.innerInitFunc = checkNotNull(innerInitFunc);
        this.productDfa = checkNotNull(productDfa);
    }

    public static <S extends State, prodS, P extends Prec> DiscreteCompositionInitFunc<S, prodS, P> create(
            InitFunc<XtaState<S>, P> innerInitFunc, DFA<prodS, String> hypothesis) {
        return new DiscreteCompositionInitFunc<>(innerInitFunc, hypothesis);
    }

    @Override
    public Collection<DiscreteCompositionState<S, prodS>> getInitStates(P prec) {
        prodS dfaInitState = productDfa.getInitialState();
        Collection<? extends XtaState<S>> xtaInitStates = innerInitFunc.getInitStates(prec);

        Collection<DiscreteCompositionState<S, prodS>> result = new ArrayList<>();
        for (XtaState<S> xtaInitState : xtaInitStates) {
            result.add(DiscreteCompositionState.create(xtaInitState, dfaInitState));
        }
        return result;
    }
}
