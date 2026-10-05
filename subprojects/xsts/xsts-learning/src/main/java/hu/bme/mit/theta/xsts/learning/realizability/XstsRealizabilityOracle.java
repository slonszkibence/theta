package hu.bme.mit.theta.xsts.learning.realizability;

import de.learnlib.sul.SUL;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import net.automatalib.word.Word;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsRealizabilityOracle {
    private final SUL<XstsAction, Boolean> sul;
    private final Logger logger;

    private XstsRealizabilityOracle(
            SUL<XstsAction, Boolean> sul,
            Logger logger
    ) {
        this.sul = checkNotNull(sul);
        this.logger = checkNotNull(logger);
    }

    public static XstsRealizabilityOracle create(
            SUL<XstsAction, Boolean> sul,
            Logger logger
    ) {
        return new XstsRealizabilityOracle(sul, logger);
    }

    public boolean isRealizable(Word<XstsAction> cexWord) {
        long start = System.currentTimeMillis();

        boolean realizable = runSulFeasibility(cexWord);
        logger.write(Logger.Level.MAINSTEP,
                "  [Realizability] %s in %d ms%n",
                realizable ? "REALIZABLE (true bug)" : "SPURIOUS (refining hypothesis)",
                System.currentTimeMillis() - start);
        return realizable;
    }

    public boolean runSulFeasibility(Word<XstsAction> cexWord) {
        sul.pre();

        try {
            for (XstsAction symbol : cexWord) {
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