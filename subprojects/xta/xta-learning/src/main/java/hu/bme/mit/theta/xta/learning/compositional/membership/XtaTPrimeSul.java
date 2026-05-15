package hu.bme.mit.theta.xta.learning.compositional.membership;

import de.learnlib.sul.SUL;

import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Update;
import hu.bme.mit.theta.xta.learning.compositional.common.TransitionConstraints;

import java.util.Map;

/**
 * The System Under Learning (SUL) representing the timing behavior (T') of the timed automaton.
 * It uses zone calculus to determine if a sequence of timing constraints (guards and resets)
 * is physically possible (i.e., does not result in an empty/bottom zone).
 */
public class XtaTPrimeSul implements SUL<TransitionConstraints, Boolean> {
    private ZoneState currentZone;
    private final Map<VarDecl<RatType>, Integer> ceilings;

    private XtaTPrimeSul(final Map<VarDecl<RatType>, Integer> ceilings) {
        this.ceilings = ceilings;
    }

    /**
     * Creates a new instance of the T' SUL.
     *
     * @param ceilings A map of clock variables to their maximum constants (k-extrapolation)
     * to ensure a finite state space.
     * @return A new XtaTPrimeSul object.
     */
    public static XtaTPrimeSul create(final Map<VarDecl<RatType>, Integer> ceilings) {
        return new XtaTPrimeSul(ceilings);
    }

    /**
     * Initializes the SUL before a new query.
     * Resets the zone to the initial zone: all clocks are set to zero,
     * followed by an immediate time elapse (up), representing that time
     * may pass before the first transition.
     */
    @Override
    public void pre() {
        currentZone = ZoneState.zero(ceilings.keySet()).transform().up().build();
    }

    /**
     * Cleans up the SUL after a query has finished.
     */
    @Override
    public void post() {
        currentZone = null;
    }

    /**
     * Applies the following operations in order: source location invariants,
     * clock guards, clock resets, time elapse (up), target location invariants,
     * and finally normalization. Returns {@code false} and stops early
     * if the zone becomes empty (Bottom) at any intermediate step.
     *
     * @param in The timing constraints and resets of the transition.
     * @return {@code true} if the resulting zone is valid (feasible), {@code false} if it is empty (Bottom).
     */
    @Override
    public Boolean step(TransitionConstraints in) {
        if (currentZone.isBottom()) {
            return false;
        }

        ZoneState.Builder builder = currentZone.transform();

        for (Guard.ClockGuard inv : in.getSourceInvariants()) {
            builder.and(inv.getClockConstr());
        }

        if (builder.build().isBottom()) {
            return false;
        }

        for (Guard.ClockGuard guard : in.getClockGuards()) {
            builder.and(guard.getClockConstr());
        }

        if (builder.build().isBottom()) {
            return false;
        }

        for (Update update : in.getResets()) {
            if (update.isClockUpdate()) {
                builder.execute(update.asClockUpdate().getClockOp());
            }
        }
        builder.up();

        for (Guard.ClockGuard inv : in.getTargetInvariants()) {
            builder.and(inv.getClockConstr());
        }

        if (builder.build().isBottom()) {
            return false;
        }

        builder.norm(ceilings);

        currentZone = builder.build();

        if (currentZone.isBottom()) {
            return false;
        }

        return true;
    }
}