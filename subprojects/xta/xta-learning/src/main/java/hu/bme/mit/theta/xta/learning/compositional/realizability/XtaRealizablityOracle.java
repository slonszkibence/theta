package hu.bme.mit.theta.xta.learning.compositional.realizability;

import de.learnlib.sul.SUL;
import hu.bme.mit.theta.common.logging.Logger;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.word.Word;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Realizability Check
 * <p>
 * Given a counterexample word {@code w} produced by the finite-state model
 * checking oracle (i.e. {@code w ∈ L(A‖H) \ Spec}), this check decides
 * whether {@code w ∈ L(T)}, that is, whether {@code w} is actually
 * realizable in the original timed automaton {@code T}.
 * <ul>
 *   <li>If realizable, the counterexample is a true bug and the system is unsafe.</li>
 *   <li>If not realizable, the counterexample is spurious; the hypothesis
 *       {@code H} must be refined to exclude {@code w} from {@code L(H)}.</li>
 * </ul>
 */
public class XtaRealizablityOracle {
    private final SUL<String, Boolean> sul;
    private final FastDFA<String> untimedAutomaton;
    private final Logger logger;

    private XtaRealizablityOracle(
            SUL<String, Boolean> sul,
            FastDFA<String> untimedAutomaton,
            Logger logger
    ) {
        this.sul = checkNotNull(sul);
        this.untimedAutomaton = checkNotNull(untimedAutomaton);
        this.logger = checkNotNull(logger);
    }

    public static XtaRealizablityOracle create(
            SUL<String, Boolean> sul,
            FastDFA<String> untimedAutomaton,
            Logger logger
    ) {
        return new XtaRealizablityOracle(sul, untimedAutomaton, logger);
    }

    /**
     * Decides whether {@code cexWord} is realizable in the underlying timed
     * automaton (i.e. {@code cexWord ∈ L(T)}).
     *
     * @return {@code true} if the counterexample is realizable (a true bug),
     *         {@code false} if spurious (the hypothesis should be refined).
     */
    public boolean isRealizable(Word<String> cexWord) {
        long start = System.currentTimeMillis();

        if (!untimedAutomaton.accepts(cexWord)) {
            logger.write(Logger.Level.MAINSTEP,
                    "  [Realizability] SPURIOUS (untimed automaton rejects cex) in %d ms%n",
                    System.currentTimeMillis() - start);
            return false;
        }

        boolean realizable = runSulFeasibility(cexWord);
        logger.write(Logger.Level.MAINSTEP,
                "  [Realizability] %s in %d ms%n",
                realizable ? "REALIZABLE (true bug)" : "SPURIOUS (refining hypothesis)",
                System.currentTimeMillis() - start);
        return realizable;
    }

    public boolean runSulFeasibility(Word<String> cexWord) {
        sul.pre();

        try {
            for (String symbol : cexWord) {
                Boolean result = sul.step(symbol);
                if (result == null || !result) {
                    return false;
                }
            }
            return true;
        }
        finally {
            sul.post();
        }
    }
}
