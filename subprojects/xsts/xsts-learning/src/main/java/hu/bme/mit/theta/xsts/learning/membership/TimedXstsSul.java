package hu.bme.mit.theta.xsts.learning.membership;

import de.learnlib.sul.SUL;

import hu.bme.mit.theta.analysis.zone.ZoneInitFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.analysis.zone.ZoneStmtTransFunc;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TimedXstsSul implements SUL<XstsAction, Boolean> {
    private static final int MAX_CACHE = 200_000;

    private record Step(ZoneState zone, XstsAction clockAction) {}

    private final Map<Step, ZoneState> stepCache =
            new LinkedHashMap<>(1024, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<Step, ZoneState> eldest) {
                    return size() > MAX_CACHE;
                }
            };

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

        final XstsAction clockAction = actionProjections.clockProjection(action);
        final Step key = new Step(currentZone, clockAction);
        ZoneState succZone = stepCache.get(key);
        if (succZone == null) {
            succZone = ZoneStmtTransFunc
                .getInstance().
                getSuccStates(currentZone, clockAction, prec).
                iterator().
                next();
            stepCache.put(key, succZone);
        }
        currentZone = succZone;

        return !currentZone.isBottom();
    }

    @Override
    public void post() {
        currentZone = null;
    }
}
