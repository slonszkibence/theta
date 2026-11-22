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

package hu.bme.mit.theta.xcfa.analysis.impl.lazy;

import hu.bme.mit.theta.analysis.algorithm.lazy.InitAbstractor;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xcfa.analysis.common.XcfaState;
import hu.bme.mit.theta.xcfa.analysis.impl.singlethread.XcfaSTState;
import hu.bme.mit.theta.xcfa.model.XcfaLocation;

import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;

public final class XcfaInitAbstractor<SConcr extends ExprState, SAbstr extends ExprState> implements InitAbstractor<XcfaState<SConcr>, XcfaState<SAbstr>> {

    private final InitAbstractor<SConcr, SAbstr> initAbstractor;
    private final BiFunction<XcfaLocation, SAbstr, XcfaState<SAbstr>> abstractStateCreator;

    private XcfaInitAbstractor(final InitAbstractor<SConcr, SAbstr> initAbstractor,
                               final BiFunction<XcfaLocation, SAbstr, XcfaState<SAbstr>> abstractStateCreator) {
        this.initAbstractor = checkNotNull(initAbstractor);
        this.abstractStateCreator = abstractStateCreator;
    }

    public static <SConcr extends ExprState, SAbstr extends ExprState> XcfaInitAbstractor<SConcr, SAbstr>
    create(final InitAbstractor<SConcr, SAbstr> initAbstractor,
           final BiFunction<XcfaLocation, SAbstr, XcfaState<SAbstr>> abstractStateCreator) {
        return new XcfaInitAbstractor<>(initAbstractor, abstractStateCreator);
    }

    @Override
    public XcfaState<SAbstr> getInitAbstrState(final XcfaState<SConcr> state) {
        final XcfaLocation locs = state.getCurrentLoc();
        final SConcr concrState = state.getGlobalState();
        final SAbstr abstrState = initAbstractor.getInitAbstrState(concrState);
        return abstractStateCreator.apply(locs, abstrState);
        // return XcfaSTState.create(locs, abstrState);
    }
}
