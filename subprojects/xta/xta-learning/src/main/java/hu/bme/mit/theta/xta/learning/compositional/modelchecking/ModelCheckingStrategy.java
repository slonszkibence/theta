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

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.RefinerResult;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.concurrent.atomic.AtomicReference;

public interface ModelCheckingStrategy<S extends State, prodS, P extends Prec> {
    SafetyChecker<DiscreteCompositionState<S, prodS>, XtaAction, P> build(
            Abstractor<DiscreteCompositionState<S, prodS>, XtaAction, P> abstractor
    );

    default P reusablePrec(final P initialPrec) {
        return initialPrec;
    }

    static <S extends State, prodS, P extends Prec> ModelCheckingStrategy<S, prodS, P> cegar(
            Refiner<DiscreteCompositionState<S, prodS>, XtaAction, P> refiner
    ) {
        final AtomicReference<P> lastRefinedPrec = new AtomicReference<>();

        final Refiner<DiscreteCompositionState<S, prodS>, XtaAction, P> trackingRefiner =
                (arg, prec) -> {
                    final RefinerResult<DiscreteCompositionState<S, prodS>, XtaAction, P> result =
                            refiner.refine(arg, prec);
                    if (result.isSpurious()) {
                        lastRefinedPrec.set(result.asSpurious().getRefinedPrec());
                    }
                    return result;
                };

        return new ModelCheckingStrategy<>() {
            @Override
            public SafetyChecker<DiscreteCompositionState<S, prodS>, XtaAction, P> build(
                    final Abstractor<DiscreteCompositionState<S, prodS>, XtaAction, P> abstractor
            ) {
                return CegarChecker.create(abstractor, trackingRefiner);
            }

            @Override
            public P reusablePrec(final P initialPrec) {
                final P tracked = lastRefinedPrec.get();
                return tracked == null ? initialPrec : tracked;
            }
        };
    }
}
