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