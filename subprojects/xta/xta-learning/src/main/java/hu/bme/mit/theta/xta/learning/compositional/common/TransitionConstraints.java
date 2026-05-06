package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Guard.ClockGuard;
import hu.bme.mit.theta.xta.Update;

import java.util.List;

/**
 * A data structure holding the timing constraints associated with a single
 * transition of the timed automaton {@code T}.
 * <p>
 * In the learning-based model checking framework, the separation of concerns
 * principle requires that timing information is handled exclusively by the
 * timed automaton component. This class captures all clock-related constraints
 * of an edge — guards, resets, and location invariants — so that they can be
 * evaluated independently of the discrete logic during membership and
 * realizability queries.
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
     * @param sourceInvariants The list of source invariants on the source location.
     * @param targetInvariants The list of target invariants on the target location.
     * @return A new TransitionConstraints object.
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

    /**
     * @return The list of source invariants.
     */
    public List<Guard.ClockGuard> getSourceInvariants() {
        return sourceInvariants;
    }

    /**
     * @return The list of target invariants.
     */
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