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

package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

public interface ModelCheckingStrategy <S extends ExprState, prodS, P extends Prec> {
        SafetyChecker<XstsCompositionState<S, prodS>, XstsAction, P> build(
                Abstractor<XstsCompositionState<S, prodS>, XstsAction, P> abstractor
        );

    static <S extends ExprState, prodS, P extends Prec> ModelCheckingStrategy<S, prodS, P> cegar(
            Refiner<XstsCompositionState<S, prodS>, XstsAction, P> refiner,
            Logger logger
    ) {
        return abstractor -> CegarChecker.create(abstractor, refiner, logger);
    }
}
