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
package hu.bme.mit.theta.analysis.algorithm.compositional.realizability;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.compositional.CexStatus;
import hu.bme.mit.theta.analysis.algorithm.compositional.LearnedComponent;
import hu.bme.mit.theta.analysis.dfa.ActionToSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.word.Word;

public final class RealizabilityChecker<P extends Prec, A, I> {

    private final ActionToSymbol<? super A, I> exactActionToSymbol;
    private final MembershipOracle.DFAMembershipOracle<I> exactOracle;
    private final ActionToSymbol<? super A, I> actionToSymbol;
    private final MembershipOracle.DFAMembershipOracle<I> oracle;
    private final Optional<Refinement<P, A, I>> refinement;

    private RealizabilityChecker(
            final ActionToSymbol<? super A, I> exactActionToSymbol,
            final MembershipOracle.DFAMembershipOracle<I> exactOracle,
            final ActionToSymbol<? super A, I> actionToSymbol,
            final MembershipOracle.DFAMembershipOracle<I> oracle,
            final Optional<Refinement<P, A, I>> refinement) {
        this.exactActionToSymbol = checkNotNull(exactActionToSymbol);
        this.exactOracle = checkNotNull(exactOracle);
        this.actionToSymbol = checkNotNull(actionToSymbol);
        this.oracle = checkNotNull(oracle);
        this.refinement = checkNotNull(refinement);
    }

    public static <P extends Prec, A, I> RealizabilityChecker<P, A, I> create(
            final ActionToSymbol<? super A, I> actionToSymbol,
            final MembershipOracle.DFAMembershipOracle<I> oracle
     ) {
        return new RealizabilityChecker<>(actionToSymbol, oracle, actionToSymbol, oracle, Optional.empty());
    }

    public static <P extends Prec, A, I> RealizabilityChecker<P, A, I> create(
            final ActionToSymbol<? super A, I> exactActionToSymbol,
            final MembershipOracle.DFAMembershipOracle<I> exactOracle,
            final ActionToSymbol<? super A, I> actionToSymbol,
            final MembershipOracle.DFAMembershipOracle<I> oracle,
            final P prec,
            final CexToPrec<P, A> cexToPrec,
            final Function<? super P, ? extends LearnedComponent<A, I>> componentFactory
    ) {
        return new RealizabilityChecker<>(exactActionToSymbol, exactOracle, actionToSymbol, oracle,
                Optional.of(new Refinement<>(prec, cexToPrec, componentFactory)));
    }

    public CexStatus<A, I> check(final List<A> cexActions) {
        if (accepts(exactOracle, toWord(cexActions, exactActionToSymbol))) {
            return CexStatus.realizable();
        }
        final Word<I> word = toWord(cexActions, actionToSymbol);
        if (refinement.isPresent() && accepts(oracle, word)) {
            final Refinement<P, A, I> r = refinement.get();
            return CexStatus.imprecise(r.componentFactory().apply(r.cexToPrec().toPrec(r.prec(), cexActions)));
        }
        return CexStatus.spurious(word);
    }

    private static <A, I> Word<I> toWord(final List<A> actions, final ActionToSymbol<? super A, I> actionToSymbol) {
        final List<I> symbols = new ArrayList<>();
        for (final A action : actions) {
            actionToSymbol.toSymbol(action).ifPresent(symbols::add);
        }
        return Word.fromList(symbols);
    }

    private static <I> boolean accepts(final MembershipOracle.DFAMembershipOracle<I> oracle, final Word<I> word) {
        final DefaultQuery<I, Boolean> query = new DefaultQuery<>(word);
        oracle.processQuery(query);
        return Boolean.TRUE.equals(query.getOutput());
    }

    private record Refinement<P extends Prec, A, I>(
            P prec, CexToPrec<P, A> cexToPrec, Function<? super P, ? extends LearnedComponent<A, I>> componentFactory) {}
}
