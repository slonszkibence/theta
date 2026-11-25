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
package hu.bme.mit.theta.analysis.algorithm.compositional.learning;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.kv.dfa.KearnsVaziraniDFABuilder;
import de.learnlib.algorithm.lstar.dfa.ClassicLStarDFABuilder;
import de.learnlib.algorithm.ttt.dfa.TTTLearnerDFABuilder;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.SupportsGrowingAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;

public final class AssumptionLearner<I> {
    public enum LearningAlgorithmType {
        LSTAR, KV, TTT
    }

    private final LearningAlgorithmType algorithmType;
    private final MembershipOracle.DFAMembershipOracle<I> membershipOracle;
    private final GrowingMapAlphabet<I> alphabet;
    private GrowingLearner<I> learner;
    private int membershipQueries;
    private int equivalenceQueries;
    private int refinements;

    private AssumptionLearner(
            final LearningAlgorithmType algorithmType,
            final MembershipOracle.DFAMembershipOracle<I> oracle
    ) {
        checkNotNull(oracle);
        this.algorithmType = checkNotNull(algorithmType);
        this.membershipOracle = queries -> {
            membershipQueries += queries.size();
            oracle.processQueries(queries);
        };
        this.alphabet = new GrowingMapAlphabet<>();
        this.learner = null;
        this.membershipQueries = 0;
        this.equivalenceQueries = 0;
        this.refinements = 0;
    }

    public static <I> AssumptionLearner<I> create(
            final LearningAlgorithmType algorithmType,
            final MembershipOracle.DFAMembershipOracle<I> membershipOracle
    ) {
        return new AssumptionLearner<>(algorithmType, membershipOracle);
    }

    public Alphabet<I> getAlphabet() {
        return alphabet;
    }

    public boolean isStarted() {
        return learner != null;
    }

    public void addSymbols(final Collection<? extends I> symbols) {
        for (final I symbol : symbols) {
            if (alphabet.containsSymbol(symbol)) {
                continue;
            }
            if (isStarted()) {
                learner.alphabet().addAlphabetSymbol(symbol);
            }
            if (!alphabet.containsSymbol(symbol)) {
                alphabet.addSymbol(symbol);
            }
        }
    }

    public void learn(final EquivalenceOracle.DFAEquivalenceOracle<I> equivalenceOracle) {
        checkState(isStarted(), "The learner has not been started yet");
        while (true) {
            equivalenceQueries++;
            final DefaultQuery<I, Boolean> cex =
                    equivalenceOracle.findCounterExample(learner.learner().getHypothesisModel(), alphabet);
            if (cex == null) {
                return;
            }
            learner.learner().refineHypothesis(cex);
            refinements++;
        }
    }

    public boolean refine(final Word<I> word) {
        checkNotNull(word);
        if (!isStarted()) {
            learner = createLearner();
            learner.learner().startLearning();
        }
        if (!learner.learner().getHypothesisModel().accepts(word)) {
            return false;
        }
        return learner.learner().refineHypothesis(new DefaultQuery<>(word, false));
    }

    public int getMembershipQueries() {
        return membershipQueries;
    }

    public int getEquivalenceQueries() {
        return equivalenceQueries;
    }

    public int getRefinements() {
        return refinements;
    }

    public FastDFA<I> getAssumption() {
        checkState(isStarted(), "The learner has not been started yet");
        return copy(learner.learner().getHypothesisModel());
    }

    private <S> FastDFA<I> copy(final DFA<S, I> hypothesis) {
        final List<I> symbols = List.copyOf(alphabet);
        final FastDFA<I> copy = new FastDFA<>(Alphabets.fromCollection(symbols));
        final S init = hypothesis.getInitialState();
        if (init == null) {
            return copy;
        }
        final Map<S, FastDFAState> copied = new LinkedHashMap<>();
        final Queue<S> queue = new ArrayDeque<>();
        copied.put(init, copy.addInitialState(hypothesis.isAccepting(init)));
        queue.add(init);
        while (!queue.isEmpty()) {
            final S state = queue.remove();
            if (!hypothesis.isAccepting(state)) {
                continue;
            }
            for (final I symbol : symbols) {
                final S succ = hypothesis.getTransition(state, symbol);
                if (succ == null) {
                    continue;
                }
                if (!copied.containsKey(succ)) {
                    copied.put(succ, copy.addState(hypothesis.isAccepting(succ)));
                    queue.add(succ);
                }
                copy.addTransition(copied.get(state), symbol, copied.get(succ));
            }
        }
        return copy;
    }

    private GrowingLearner<I> createLearner() {
        return switch (algorithmType) {
            case LSTAR -> GrowingLearner.of(new ClassicLStarDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(membershipOracle)
                    .create());
            case KV -> GrowingLearner.of(new KearnsVaziraniDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(membershipOracle)
                    .create());
            case TTT -> GrowingLearner.of(new TTTLearnerDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(membershipOracle)
                    .create());
        };
    }

    private record GrowingLearner<I>(LearningAlgorithm.DFALearner<I> learner, SupportsGrowingAlphabet<I> alphabet) {
        static <I, L extends LearningAlgorithm.DFALearner<I> & SupportsGrowingAlphabet<I>> GrowingLearner<I> of(
                final L learner) {
            return new GrowingLearner<>(learner, learner);
        }
    }
}
