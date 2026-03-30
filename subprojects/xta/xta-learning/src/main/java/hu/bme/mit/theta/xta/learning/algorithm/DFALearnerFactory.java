package hu.bme.mit.theta.xta.learning.algorithm;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.lstar.dfa.ClassicLStarDFABuilder;
import de.learnlib.algorithm.kv.dfa.KearnsVaziraniDFABuilder;
import de.learnlib.algorithm.ttt.dfa.TTTLearnerDFABuilder;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.Alphabet;


public class DFALearnerFactory {
    public static LearningAlgorithm.DFALearner<String> createLearner(
            AlgorithmType algorithmType,
            Alphabet<String> alphabet,
            MembershipOracle.DFAMembershipOracle<String> oracle) {

        switch (algorithmType) {
            case LSTAR:
                return new ClassicLStarDFABuilder<String>()
                        .withAlphabet(alphabet)
                        .withOracle(oracle)
                        .create();
            case KandV:
                return new KearnsVaziraniDFABuilder<String>()
                        .withAlphabet(alphabet)
                        .withOracle(oracle)
                        .create();
            case TTT:
                return new TTTLearnerDFABuilder<String>()
                        .withAlphabet(alphabet)
                        .withOracle(oracle)
                        .create();

            default: throw new IllegalArgumentException("Unknown algorithm type: " + algorithmType);
        }
    }

}
