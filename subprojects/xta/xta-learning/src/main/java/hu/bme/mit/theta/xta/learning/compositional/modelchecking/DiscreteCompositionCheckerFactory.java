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

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.LifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.RandomWaitlist;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.List;
import java.util.function.Predicate;

public class DiscreteCompositionCheckerFactory {
    public enum SearchStrategy {
        BFS,
        DFS,
        RANDOM
    }

    private DiscreteCompositionCheckerFactory() {}

    public static <S extends State, prodS, P extends Prec> SafetyChecker<DiscreteCompositionState<S, prodS>, XtaAction, P> create(
            ModelCheckingStrategy<S, prodS, P> modelCheckingStrategy,
            SearchStrategy searchStrategy,
            DiscreteCompositionAnalysis<S, prodS, P> analysis,
            LTS<DiscreteCompositionState<S, prodS>, XtaAction> lts,
            Predicate<DiscreteCompositionState<S, prodS>> targetPred
    ) {
        Abstractor<DiscreteCompositionState<S, prodS>, XtaAction, P> abstractor =
                createAbstractor(searchStrategy, analysis, lts, targetPred);

        return modelCheckingStrategy.build(abstractor);
    }

    private static <S extends State, prodS, P extends Prec>
    Abstractor<DiscreteCompositionState<S, prodS>, XtaAction, P> createAbstractor(
            SearchStrategy searchStrategy,
            DiscreteCompositionAnalysis<S, prodS, P> analysis,
            LTS<DiscreteCompositionState<S, prodS>, XtaAction> lts,
            Predicate<DiscreteCompositionState<S, prodS>> targetPred
    ) {
        ArgBuilder<DiscreteCompositionState<S, prodS>, XtaAction, P> argBuilder =
                ArgBuilder.create(lts, analysis, targetPred, true);

        return BasicAbstractor.builder(argBuilder)
                .waitlist(createWaitlist(searchStrategy))

                .projection(state -> List.of(
                        state.getDfaState(),
                        state.getXtaState().getLocs(),
                        state.getXtaState().getState()))
                .build();
    }

    private static <D extends State, prodS>
    Waitlist<ArgNode<DiscreteCompositionState<D, prodS>, XtaAction>> createWaitlist(
            SearchStrategy searchStrategy
    ) {
        return switch (searchStrategy) {
            case BFS    -> FifoWaitlist.create();
            case DFS    -> LifoWaitlist.create();
            case RANDOM -> RandomWaitlist.create();
        };
    }
}
