package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Guard.ClockGuard;
import hu.bme.mit.theta.xta.Update;

import java.util.List;

/**
 * A data structure holding the clock guards and clock updates (resets) associated with a transition.
 * It is used to separate timing constraints from discrete logic during the learning process.
 */
public class TransitionConstraints {
    private final List<ClockGuard> clockGuards;
    private final List<Update> resets;
    private final List<Guard.ClockGuard> sourceInvariants;
    private final List<Guard.ClockGuard> targetInvariants;


    private TransitionConstraints(final List<ClockGuard> clockGuards,
                                  final List<Update> resets,
                                  final List<Guard.ClockGuard> sourceInvariants,
                                  final List<Guard.ClockGuard> targetInvariants) {
        this.clockGuards = clockGuards;
        this.resets = resets;
        this.sourceInvariants = sourceInvariants;
        this.targetInvariants = targetInvariants;
    }

    /**
     * Creates a new {@link TransitionConstraints} instance.
     *
     * @param clockGuards The list of clock guards on the edge.
     * @param resets      The list of clock updates (resets) on the edge.
     * @return A new GuardResetPair object.
     */
    public static TransitionConstraints create(final List<ClockGuard> clockGuards,
                                               final List<Update> resets,
                                               final List<Guard.ClockGuard> sourceInvariants,
                                               final List<Guard.ClockGuard> targetInvariants) {
        return new TransitionConstraints(clockGuards, resets, sourceInvariants, targetInvariants);
    }

    /**
     * @return The list of clock guards.
     */
    public List<ClockGuard> getClockGuards() {
        return clockGuards;
    }

    /**
     * @return The list of clock updates (resets).
     */
    public List<Update> getResets() {
        return resets;
    }

    public List<Guard.ClockGuard> getSourceInvariants() {
        return sourceInvariants;
    }
    public List<Guard.ClockGuard> getTargetInvariants() {
        return targetInvariants;
    }

    @Override
    public String toString() {
        return "Guards:" + clockGuards.toString() + " | Resets:" + resets.toString()
                + " | SourceInvariants:" + sourceInvariants.toString()
                + " | TargetInvariants:" + targetInvariants.toString();
    }
}