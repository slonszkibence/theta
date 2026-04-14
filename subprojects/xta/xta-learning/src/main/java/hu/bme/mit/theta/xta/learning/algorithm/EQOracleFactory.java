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