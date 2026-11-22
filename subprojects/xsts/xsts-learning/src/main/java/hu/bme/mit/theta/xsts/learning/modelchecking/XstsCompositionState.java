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

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xsts.analysis.XstsState;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsCompositionState<S extends ExprState, prodS> implements State {
    private final XstsState<S> xstsState;
    private final prodS dfaState;

    private XstsCompositionState(XstsState<S> xstsState, prodS dfaState) {
        this.xstsState = checkNotNull(xstsState);
        this.dfaState = dfaState;
    }

    public static <S extends ExprState, prodS> XstsCompositionState<S, prodS> create(
            XstsState<S> xstsState, prodS dfaState) {
        return new XstsCompositionState<>(xstsState, dfaState);
    }

    public prodS getDfaState() {
        return dfaState;
    }

    public XstsState<S> getXstsState() {
        return xstsState;
    }

    @Override
    public boolean isBottom() {
        return xstsState.isBottom();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XstsCompositionState<?, ?> that = (XstsCompositionState<?, ?>) o;
        return Objects.equals(xstsState, that.xstsState) && Objects.equals(dfaState, that.dfaState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xstsState, dfaState);
    }

    @Override
    public String toString() {
        return "(" + xstsState + ", " + dfaState + ")";
    }
}