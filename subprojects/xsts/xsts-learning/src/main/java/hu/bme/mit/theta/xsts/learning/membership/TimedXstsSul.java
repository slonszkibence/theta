package hu.bme.mit.theta.xsts.learning.membership;

import de.learnlib.sul.SUL;

import hu.bme.mit.theta.analysis.zone.ZoneInitFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.analysis.zone.ZoneStmtTransFunc;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimedXstsSul implements SUL<XstsAction, Boolean> {
    private final ZonePrec prec;
    private final TimedXstsActionProjections actionProjections;
    private ZoneState currentZone;

    private TimedXstsSul(TimedXstsActionProjections actionProjections,
                         ZonePrec prec) {
        this.prec = checkNotNull(prec);
        this.actionProjections = checkNotNull(actionProjections);
    }

    public static TimedXstsSul create(TimedXstsActionProjections actionProjections, ZonePrec prec) {
        return new TimedXstsSul(actionProjections, prec);
    }



    @Override
    public void pre() {
        currentZone = ZoneInitFunc.getInstance().getInitStates(prec).iterator().next();
    }

    @Override
    public Boolean step(XstsAction action) {
        if (currentZone.isBottom()) {
            return false;
        }

        XstsAction clockAction = actionProjections.clockProjection(action);
        currentZone = ZoneStmtTransFunc
            .getInstance().
            getSuccStates(currentZone, clockAction, prec).
            iterator().
            next();

        return !currentZone.isBottom();
    }

    @Override
    public void post() {
        currentZone = ZoneState.bottom();
    }
}
