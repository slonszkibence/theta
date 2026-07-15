package hu.bme.mit.theta.xsts.learning.membership;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import net.automatalib.word.Word;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimedXstsMembershipOracle implements MembershipOracle.DFAMembershipOracle<XstsAction> {
    private final TimedXstsSul sul;

    private TimedXstsMembershipOracle(TimedXstsSul sul) {
        this.sul = checkNotNull(sul);
    }

    public static TimedXstsMembershipOracle create(TimedXstsSul sul) {
        return new TimedXstsMembershipOracle(sul);
    }

    @Override
    public void processQueries(Collection<? extends Query<XstsAction, Boolean>> queries) {
        for (Query<XstsAction, Boolean> query : queries) {
            query.answer(runMembershipQuery(query.getInput()));
        }
    }

    private boolean runMembershipQuery(Word<XstsAction> word) {
        sul.pre();
        try {
            for (XstsAction action : word) {
                Boolean result = sul.step(action);
                if (result == null || !result)
                    return false;
            }
            return true;
        } finally {
            sul.post();
        }
    }
}
