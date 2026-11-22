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
package hu.bme.mit.theta.analysis.dfa;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.unit.UnitPrec;

import java.util.Collection;
import java.util.List;

import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

public final class DfaAnalysis<S, A extends Action, I> implements Analysis<DfaState<S>, A, UnitPrec> {

    private final PartialOrd<DfaState<S>> partialOrd;
    private final InitFunc<DfaState<S>, UnitPrec> initFunc;
    private final TransFunc<DfaState<S>, A, UnitPrec> transFunc;

    private DfaAnalysis(
            final InitFunc<DfaState<S>, UnitPrec> initFunc,
            final TransFunc<DfaState<S>, A, UnitPrec> transFunc) {
        this.partialOrd = DfaOrd.create();
        this.initFunc = checkNotNull(initFunc);
        this.transFunc = checkNotNull(transFunc);
    }

    public static <S, A extends Action, I> DfaAnalysis<S, A, I> create(
            final DFA<S, I> dfa,
            final Collection<? extends I> alphabet,
            final ActionToSymbol<? super A, I> actionToSymbol) {
        checkNotNull(dfa);
        final S init = dfa.getInitialState();
        final DfaState<S> initState = init == null ? DfaState.bottom() : DfaState.of(init);
        return new DfaAnalysis<>(
                DfaInitFunc.create(initState), DfaTransFunc.create(dfa, alphabet, actionToSymbol));
    }

    public static <A extends Action, I> DfaAnalysis<FastDFAState, A, I> universal(
            final ActionToSymbol<? super A, I> actionToSymbol) {
        final DFA<FastDFAState, I> empty = new FastDFA<>(Alphabets.fromList(List.of()));
        return new DfaAnalysis<>(
                DfaInitFunc.create(DfaState.top()), DfaTransFunc.create(empty, List.of(), actionToSymbol));
    }

    @Override
    public PartialOrd<DfaState<S>> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<DfaState<S>, UnitPrec> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<DfaState<S>, A, UnitPrec> getTransFunc() {
        return transFunc;
    }
}
