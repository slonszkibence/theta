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
package hu.bme.mit.theta.analysis.algorithm.compositional.inclusion;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.dfa.DfaAnalysis;
import hu.bme.mit.theta.analysis.dfa.DfaState;
import hu.bme.mit.theta.analysis.dfa.SymbolAction;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.LifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.RandomWaitlist;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;

public final class InclusionOracle<S extends State, I, P extends Prec>
        implements EquivalenceOracle.DFAEquivalenceOracle<I> {

    private final Analysis<S, ? super SymbolAction<I>, ? super P> analysis;
    private final P prec;
    private final MembershipOracle.DFAMembershipOracle<I> membershipOracle;
    private final SearchStrategy searchStrategy;
    private final boolean searchSpuriousWords;
    private final Function<? super S, ?> projection;
    private final Logger logger;

    private InclusionOracle(final Builder<S, I, P> builder) {
        this.analysis = checkNotNull(builder.analysis);
        this.prec = checkNotNull(builder.prec);
        this.membershipOracle = checkNotNull(builder.membershipOracle);
        this.searchStrategy = checkNotNull(builder.searchStrategy);
        this.searchSpuriousWords = builder.searchSpuriousWords;
        this.projection = checkNotNull(builder.projection);
        this.logger = checkNotNull(builder.logger);
    }

    public static <S extends State, I, P extends Prec> Builder<S, I, P> builder(
            final Analysis<S, ? super SymbolAction<I>, ? super P> analysis,
            final P prec,
            final MembershipOracle.DFAMembershipOracle<I> membershipOracle) {
        return new Builder<>(analysis, prec, membershipOracle);
    }

    @Override
    public DefaultQuery<I, Boolean> findCounterExample(
            final DFA<?, I> hypothesis, final Collection<? extends I> inputs) {
        return search(hypothesis, inputs);
    }

    private <Q> DefaultQuery<I, Boolean> search(final DFA<Q, I> hypothesis, final Collection<? extends I> inputs) {
        final DfaAnalysis<Q, SymbolAction<I>, I> hypothesisAnalysis =
                DfaAnalysis.create(hypothesis, inputs, SymbolAction.toSymbol());
        final PartialOrd<S> ord = analysis.getPartialOrd();
        final UnitPrec unitPrec = UnitPrec.getInstance();

        final Waitlist<Node<Q, S, I>> waitlist = createWaitlist();
        final Map<List<Object>, List<S>> passed = new LinkedHashMap<>();
        for (final DfaState<Q> hInit : hypothesisAnalysis.getInitFunc().getInitStates(unitPrec)) {
            for (final S tInit : analysis.getInitFunc().getInitStates(prec)) {
                if (!tInit.isBottom() && !hInit.isBottom()) {
                    waitlist.add(new Node<>(hInit, tInit, null, null));
                }
            }
        }

        while (!waitlist.isEmpty()) {
            final Node<Q, S, I> node = waitlist.remove();
            final List<Object> key = List.of(node.hState(), projection.apply(node.tState()));
            final List<S> coverers = passed.getOrDefault(key, Collections.emptyList());
            if (coverers.stream().anyMatch(coverer -> ord.isLeq(node.tState(), coverer))) {
                continue;
            }
            passed.computeIfAbsent(key, k -> new ArrayList<>()).add(node.tState());

            for (final I symbol : inputs) {
                final SymbolAction<I> action = SymbolAction.of(symbol);
                final List<DfaState<Q>> hSuccs =
                        live(hypothesisAnalysis.getTransFunc().getSuccStates(node.hState(), action, unitPrec));
                final List<S> tSuccs = live(analysis.getTransFunc().getSuccStates(node.tState(), action, prec));

                if (hSuccs.isEmpty() && !tSuccs.isEmpty()) {
                    final Word<I> missing = node.word().append(symbol);
                    logger.write(Logger.Level.SUBSTEP, "    [Inclusion] word of length %d is missing from H%n",
                            missing.size());
                    return new DefaultQuery<>(missing, true);
                }
                if (searchSpuriousWords && !hSuccs.isEmpty() && tSuccs.isEmpty()) {
                    final Word<I> candidate = node.word().append(symbol);
                    if (!accepts(candidate)) {
                        logger.write(Logger.Level.SUBSTEP, "    [Inclusion] word of length %d is spurious in H%n",
                                candidate.size());
                        return new DefaultQuery<>(candidate, false);
                    }
                }
                for (final DfaState<Q> hSucc : hSuccs) {
                    for (final S tSucc : tSuccs) {
                        waitlist.add(new Node<>(hSucc, tSucc, node, symbol));
                    }
                }
            }
        }
        return null;
    }

    private boolean accepts(final Word<I> word) {
        final DefaultQuery<I, Boolean> query = new DefaultQuery<>(word);
        membershipOracle.processQuery(query);
        return Boolean.TRUE.equals(query.getOutput());
    }

    private static <T extends State> List<T> live(final Collection<? extends T> states) {
        final List<T> result = new ArrayList<>();
        for (final T state : states) {
            if (!state.isBottom()) {
                result.add(state);
            }
        }
        return result;
    }

    private <T> Waitlist<T> createWaitlist() {
        return switch (searchStrategy) {
            case BFS -> FifoWaitlist.create();
            case DFS -> LifoWaitlist.create();
            case RANDOM -> RandomWaitlist.create();
        };
    }

    private record Node<Q, S, I>(DfaState<Q> hState, S tState, Node<Q, S, I> parent, I symbol) {
        Word<I> word() {
            final List<I> symbols = new ArrayList<>();
            for (Node<Q, S, I> node = this; node.parent() != null; node = node.parent()) {
                symbols.add(node.symbol());
            }
            Collections.reverse(symbols);
            return Word.fromList(symbols);
        }
    }

    public static final class Builder<S extends State, I, P extends Prec> {
        private final Analysis<S, ? super SymbolAction<I>, ? super P> analysis;
        private final P prec;
        private final MembershipOracle.DFAMembershipOracle<I> membershipOracle;
        private SearchStrategy searchStrategy;
        private boolean searchSpuriousWords;
        private Function<? super S, ?> projection;
        private Logger logger;

        private Builder(
                final Analysis<S, ? super SymbolAction<I>, ? super P> analysis,
                final P prec,
                final MembershipOracle.DFAMembershipOracle<I> membershipOracle) {
            this.analysis = analysis;
            this.prec = prec;
            this.membershipOracle = membershipOracle;
            this.searchStrategy = SearchStrategy.BFS;
            this.searchSpuriousWords = true;
            this.projection = state -> 0;
            this.logger = NullLogger.getInstance();
        }

        public Builder<S, I, P> searchStrategy(final SearchStrategy searchStrategy) {
            this.searchStrategy = searchStrategy;
            return this;
        }

        public Builder<S, I, P> searchSpuriousWords(final boolean searchSpuriousWords) {
            this.searchSpuriousWords = searchSpuriousWords;
            return this;
        }

        public Builder<S, I, P> projection(final Function<? super S, ?> projection) {
            this.projection = projection;
            return this;
        }

        public Builder<S, I, P> logger(final Logger logger) {
            this.logger = logger;
            return this;
        }

        public InclusionOracle<S, I, P> build() {
            return new InclusionOracle<>(this);
        }
    }
}
