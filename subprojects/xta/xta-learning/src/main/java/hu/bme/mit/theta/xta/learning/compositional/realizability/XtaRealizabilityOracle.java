package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.common.logging.Logger;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.word.Word;

import static com.google.common.base.Preconditions.checkNotNull;

public final class XtaRealizabilityOracle {
    private final MembershipOracle.DFAMembershipOracle<String> mqOracle;
    private final Logger logger;

    private XtaRealizabilityOracle(final MembershipOracle.DFAMembershipOracle<String> mqOracle,
                                   final Logger logger) {
        this.mqOracle = checkNotNull(mqOracle);
        this.logger = checkNotNull(logger);
    }

    public static XtaRealizabilityOracle create(
            final MembershipOracle.DFAMembershipOracle<String> mqOracle, final Logger logger) {
        return new XtaRealizabilityOracle(mqOracle, logger);
    }

    public boolean isRealizable(final Word<String> cexWord) {
        final long start = System.currentTimeMillis();
        final DefaultQuery<String, Boolean> query = new DefaultQuery<>(cexWord);
        mqOracle.processQuery(query);
        final boolean realizable = Boolean.TRUE.equals(query.getOutput());

        logger.write(Logger.Level.MAINSTEP, "  [Realizability] %s in %d ms%n",
                realizable ? "REALIZABLE (true counterexample)" : "SPURIOUS (refining)",
                System.currentTimeMillis() - start);
        return realizable;
    }
}
