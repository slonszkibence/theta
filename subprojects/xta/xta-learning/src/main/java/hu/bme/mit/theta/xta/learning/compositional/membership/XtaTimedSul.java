package hu.bme.mit.theta.xta.learning.compositional.membership;

import de.learnlib.sul.SUL;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import static com.google.common.base.Preconditions.checkNotNull;

public final class XtaTimedSul implements SUL<String, Boolean> {
    private final TimedStepper stepper;
    private TimedStepper.Config config;
    private boolean dead;

    private XtaTimedSul(final TimedStepper stepper) {
        this.stepper = checkNotNull(stepper);
    }

    public static XtaTimedSul create(final TimedStepper stepper) {
        return new XtaTimedSul(stepper);
    }

    @Override
    public void pre() {
        config = stepper.initial();
        dead = config.zone().isBottom();
    }

    @Override
    public void post() {
        config = null;
        dead = false;
    }

    @Override
    public Boolean step(final String symbol) {
        if (dead) {
            return false;
        }
        final TimedStepper.Config next = stepper.step(config, symbol);
        if (next == null) {
            dead = true;
            return false;
        }
        config = next;
        return true;
    }
}
