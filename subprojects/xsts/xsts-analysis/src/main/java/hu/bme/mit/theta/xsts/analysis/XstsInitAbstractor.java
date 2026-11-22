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

package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.algorithm.lazy.InitAbstractor;
import hu.bme.mit.theta.analysis.expr.ExprState;

import static com.google.common.base.Preconditions.checkNotNull;

public final class XstsInitAbstractor<SConcr extends ExprState, SAbstr extends ExprState>
        implements InitAbstractor<XstsState<SConcr>, XstsState<SAbstr>> {

    private final InitAbstractor<SConcr, SAbstr> initAbstractor;

    private XstsInitAbstractor(final InitAbstractor<SConcr, SAbstr> initAbstractor) {
        this.initAbstractor = checkNotNull(initAbstractor);
    }

    public static <SConcr extends ExprState, SAbstr extends ExprState> XstsInitAbstractor<SConcr, SAbstr>
    create(final InitAbstractor<SConcr, SAbstr> initAbstractor) {
        return new XstsInitAbstractor<>(initAbstractor);
    }

    @Override
    public XstsState<SAbstr> getInitAbstrState(XstsState<SConcr> concrXstsState) {
        final SConcr concrState = concrXstsState.getState();
        final SAbstr abstrState = initAbstractor.getInitAbstrState(concrState);
        final boolean lastActionWasEnv = concrXstsState.lastActionWasEnv();
        final boolean isInitialized = concrXstsState.isInitialized();
        final XstsState<SAbstr> abstrXstsState = XstsState.of(abstrState, lastActionWasEnv, isInitialized);
        return abstrXstsState;
    }
}
