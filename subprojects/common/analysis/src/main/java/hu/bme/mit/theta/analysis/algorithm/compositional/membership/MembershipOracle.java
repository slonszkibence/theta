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
package hu.bme.mit.theta.analysis.algorithm.compositional.membership;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.dfa.SymbolAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.query.Query;
import net.automatalib.word.Word;

public final class MembershipOracle<S extends State, I, P extends Prec>
        implements de.learnlib.oracle.MembershipOracle.DFAMembershipOracle<I> {

    private final Analysis<S, ? super SymbolAction<I>, ? super P> analysis;
    private final P prec;
    private final Map<Word<I>, Boolean> cache;
    private long queries;
    private long runs;

    private MembershipOracle(final Analysis<S, ? super SymbolAction<I>, ? super P> analysis, final P prec) {
        this.analysis = checkNotNull(analysis);
        this.prec = checkNotNull(prec);
        this.cache = new HashMap<>();
        this.queries = 0;
        this.runs = 0;
    }

    public static <S extends State, I, P extends Prec> MembershipOracle<S, I, P> create(
            final Analysis<S, ? super SymbolAction<I>, ? super P> analysis, final P prec) {
        return new MembershipOracle<>(analysis, prec);
    }

    @Override
    public void processQueries(final Collection<? extends Query<I, Boolean>> queries) {
        for (final Query<I, Boolean> query : queries) {
            query.answer(accepts(query.getInput()));
        }
    }

    public boolean accepts(final Word<I> word) {
        queries++;
        return cache.computeIfAbsent(word, this::run);
    }

    public long getQueryCount() {
        return queries;
    }

    public long getRunCount() {
        return runs;
    }

    private boolean run(final Word<I> word) {
        runs++;
        Collection<? extends S> states = live(analysis.getInitFunc().getInitStates(prec));
        for (final I symbol : word) {
            if (states.isEmpty()) {
                return false;
            }
            final SymbolAction<I> action = SymbolAction.of(symbol);
            final List<S> succStates = new ArrayList<>();
            for (final S state : states) {
                succStates.addAll(live(analysis.getTransFunc().getSuccStates(state, action, prec)));
            }
            states = succStates;
        }
        return !states.isEmpty();
    }

    private List<S> live(final Collection<? extends S> states) {
        final List<S> result = new ArrayList<>();
        for (final S state : states) {
            if (!state.isBottom()) {
                result.add(state);
            }
        }
        return result;
    }
}
