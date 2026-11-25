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
import hu.bme.mit.theta.common.Utils;

import java.util.Optional;

public final class SymbolAction<I> implements Action {

    private final I symbol;

    private SymbolAction(final I symbol) {
        this.symbol = checkNotNull(symbol, "Symbol must not be null");
    }

    public static <I> SymbolAction<I> of(final I symbol) {
        return new SymbolAction<>(symbol);
    }

    public static <I> ActionToSymbol<SymbolAction<I>, I> toSymbol() {
        return action -> Optional.of(action.getSymbol());
    }

    public I getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof SymbolAction<?> that && this.symbol.equals(that.symbol);
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder(getClass().getSimpleName()).add(symbol).toString();
    }
}
