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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ActionToSymbolCollector<A, I> implements ActionToSymbol<A, I> {

    private final ActionToSymbol<? super A, I> actionToSymbol;
    private final Set<I> knownSymbols;
    private final Map<I, A> discoveredSymbols;

    private ActionToSymbolCollector(
            final ActionToSymbol<? super A, I> actionToSymbol, final Collection<? extends I> knownSymbols) {
        this.actionToSymbol = checkNotNull(actionToSymbol);
        this.knownSymbols = Set.copyOf(knownSymbols);
        this.discoveredSymbols = new LinkedHashMap<>();
    }

    public static <A, I> ActionToSymbolCollector<A, I> create(
            final ActionToSymbol<? super A, I> actionToSymbol, final Collection<? extends I> knownSymbols) {
        return new ActionToSymbolCollector<>(actionToSymbol, knownSymbols);
    }

    @Override
    public Optional<I> toSymbol(final A action) {
        final Optional<I> symbol = actionToSymbol.toSymbol(action);
        symbol.filter(s -> !knownSymbols.contains(s)).ifPresent(s -> discoveredSymbols.putIfAbsent(s, action));
        return symbol;
    }

    public Map<I, A> getDiscoveredSymbols() {
        return discoveredSymbols;
    }
}
