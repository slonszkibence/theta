package hu.bme.mit.theta.xsts.learning.membership;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import de.learnlib.sul.SUL;
import net.automatalib.word.Word;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimedXstsMembershipOracle<I> implements MembershipOracle.DFAMembershipOracle<I> {
    private final SUL<I, Boolean> sul;

    private TimedXstsMembershipOracle(SUL<I, Boolean> sul) {
        this.sul = checkNotNull(sul);
    }

    public static <I> TimedXstsMembershipOracle<I> create(SUL<I, Boolean> sul) {
        return new TimedXstsMembershipOracle<>(sul);
    }

    @Override
    public void processQueries(Collection<? extends Query<I, Boolean>> queries) {
        for (Query<I, Boolean> query : queries) {
            query.answer(runMembershipQuery(query.getInput()));
        }
    }

    private boolean runMembershipQuery(Word<I> word) {
        sul.pre();
        try {
            for (I symbol : word) {
                Boolean result = sul.step(symbol);
                if (result == null || !result)
                    return false;
            }
            return true;
        } finally {
            sul.post();
        }
    }
}
