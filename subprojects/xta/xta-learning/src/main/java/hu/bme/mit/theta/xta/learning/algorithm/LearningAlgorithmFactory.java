package hu.bme.mit.theta.xta.learning.algorithm;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.lstar.dfa.ClassicLStarDFABuilder;
import de.learnlib.algorithm.kv.dfa.KearnsVaziraniDFABuilder;
import de.learnlib.algorithm.ttt.dfa.TTTLearnerDFABuilder;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.Alphabet;

/**
 * A factory class for active automaton learning. This factory creates the active DFA learning algorithm
 * for building the hypothesis automaton with membership queries and equivalence queries. These algorithms learn
 * regular languages.
 */
public class LearningAlgorithmFactory {
    private LearningAlgorithmFactory() {}

    /**
     *
     * @param algorithmType - name of the learning algorithm.
     * @param alphabet - alphabet is a collection of symbols.
     * @param oracle - Membership oracle interface. A membership oracle provides an elementary abstraction to a System
     *                 Under Learning (SUL), by allowing to pose queries: A query is a sequence of input symbols.
     * @return - the selected learning algorithm.
     * @param <I> - input symbol type
     */
    public static <I> LearningAlgorithm.DFALearner<I> create(
            LearningAlgorithmType algorithmType,
            Alphabet<I> alphabet,
            MembershipOracle.DFAMembershipOracle<I> oracle) {

        return switch (algorithmType) {
            /*
             *  L* algorithm by Dana Angluin. This algorithm creates an observation table from which it builds a
             *  consistent and closed DFA. The memory need is large.
             */
            case LSTAR -> new ClassicLStarDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(oracle)
                    .create();
            /*
             * The Kearns/Vazirani algorithm for learning DFA, as described in the book "An Introduction to Computational
             * Learning Theory" by Michael Kearns and Umesh Vazirani. This algorithm uses a discrimination tree and is
             * more efficient than L*, by using fewer membership queries.
             */
            case KANDV -> new KearnsVaziraniDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(oracle)
                    .create();
            /*
             * The TTT algorithm is the most efficient of the three. This algorithm uses a combination of a discrimination tree
             * and an inner consistency checker.
             */
            case TTT -> new TTTLearnerDFABuilder<I>()
                    .withAlphabet(alphabet)
                    .withOracle(oracle)
                    .create();
        };
    }

}