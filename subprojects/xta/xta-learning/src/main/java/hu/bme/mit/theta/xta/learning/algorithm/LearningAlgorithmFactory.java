package hu.bme.mit.theta.xta.learning.algorithm;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.lstar.dfa.ClassicLStarDFABuilder;
import de.learnlib.algorithm.kv.dfa.KearnsVaziraniDFABuilder;
import de.learnlib.algorithm.ttt.dfa.TTTLearnerDFABuilder;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.Alphabet;


public class LearningAlgorithmFactory {
    private LearningAlgorithmFactory() {}

    public static <I> LearningAlgorithm.DFALearner<I> create(
            LearningAlgorithmType algorithmType,
            Alphabet<I> alphabet,
            MembershipOracle.DFAMembershipOracle<I> oracle) {

        return switch (algorithmType) {
            case LSTAR -> new ClassicLStarDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(oracle)
                    .create();
            case KANDV -> new KearnsVaziraniDFABuilder<I>()
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
