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

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.kv.dfa.KearnsVaziraniDFABuilder;
import de.learnlib.algorithm.lstar.dfa.ClassicLStarDFABuilder;
import de.learnlib.algorithm.ttt.dfa.TTTLearnerDFABuilder;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.Alphabet;

public class LearningAlgorithmFactory {
    private LearningAlgorithmFactory() {}

    public static <I> LearningAlgorithm.DFALearner<I> create(
            LearningAlgorithmType algoType,
            Alphabet<I> alphabet,
            MembershipOracle.DFAMembershipOracle<I> oracle
    ) {
        return switch (algoType) {
            case LSTAR -> new ClassicLStarDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(oracle)
                    .create();
            case KV -> new KearnsVaziraniDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(oracle)
                    .create();
            case TTT -> new TTTLearnerDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(oracle)
                    .create();
        };
    }
}
