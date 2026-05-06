package hu.bme.mit.theta.xta.learning.algorithm;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle.DFAMembershipOracle;
import de.learnlib.oracle.equivalence.DFAWMethodEQOracle;
import de.learnlib.oracle.equivalence.DFAWpMethodEQOracle;
import de.learnlib.oracle.equivalence.DFARandomWordsEQOracle;
import de.learnlib.oracle.equivalence.EQOracleChain;

import net.automatalib.automaton.fsa.DFA;

import hu.bme.mit.theta.xta.learning.compositional.inclusion.XtaInclusionOracle;

import java.util.Random;


/**
 * Factory for creating the equivalence oracle used in the active learning loop.
 * The equivalence oracle is responsible for checking, at the end of each learning
 * iteration, whether the current hypothesis DFA is equivalent to the
 * true target language. If not, it returns a counterexample — a word for
 * which {@code H(w) ≠ T(w)} — which the learning algorithm uses to refine the hypothesis.
 *<p>
 * Checks whether the current hypothesis {@code H} is a valid overapproximation
 * of the timed automaton, i.e. whether {@code L(T) ⊆ L(H)} holds. If not,
 * a counterexample word {@code w ∈ L(T) \ L(H)} is returned — a word not yet
 * known to the learning algorithm — which it uses to refine the hypothesis.
 *</p>
 * <p>
 * Built-in LearnLib syntactic equivalence oracles. They do not use model checking —
 * instead they test the structure of the hypothesis or sample random words.
 * They are faster, but incomplete: they do not guarantee that every counterexample
 * will be found.
 *</p>
 * <p>
 * A combined strategy: the cheaper syntactic oracle runs first, and the
 * {@link XtaInclusionOracle} is only invoked if no counterexample is found.
 * This avoids the cost of model checking in cases where a simpler test suffices.
 * </p>
 */
public class EQOracleFactory {

    private EQOracleFactory() {}

    public static EquivalenceOracle<DFA<?, String>, String, Boolean> create(
            EQOracleType eqOracleType,
            DFAMembershipOracle<String> mqOracle,
            XtaInclusionOracle xtaInclusionOracle,
            int lookahead,
            int minLength,
            int maxLength,
            int maxTests) {

        return switch (eqOracleType) {
            case W_METHOD -> new DFAWMethodEQOracle<>(mqOracle, lookahead);

            case WP_METHOD -> new DFAWpMethodEQOracle<>(mqOracle, lookahead);

            case RANDOM_WORDS -> new DFARandomWordsEQOracle<>(mqOracle, minLength, maxLength, maxTests, new Random(42));

            case XTA_INCLUSION -> xtaInclusionOracle;

            case CHAIN_W_INCLUSION -> {
                EQOracleChain<DFA<?, String>, String, Boolean> chain = new EQOracleChain<>();
                chain.addOracle(new DFAWMethodEQOracle<>(mqOracle, lookahead));
                chain.addOracle(xtaInclusionOracle);
                yield chain;
            }

            case CHAIN_WP_INCLUSION -> {
                EQOracleChain<DFA<?, String>, String, Boolean> chain = new EQOracleChain<>();
                chain.addOracle(new DFAWpMethodEQOracle<>(mqOracle, lookahead));
                chain.addOracle(xtaInclusionOracle);
                yield chain;
            }

            case CHAIN_RANDOM_INCLUSION -> {
                EQOracleChain<DFA<?, String>, String, Boolean> chain = new EQOracleChain<>();
                chain.addOracle(new DFARandomWordsEQOracle<>(mqOracle, minLength, maxLength, maxTests, new Random(42)));
                chain.addOracle(xtaInclusionOracle);
                yield chain;
            }
        };
    }
}