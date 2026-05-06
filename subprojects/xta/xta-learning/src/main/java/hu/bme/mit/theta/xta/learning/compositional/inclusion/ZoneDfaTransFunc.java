package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Update;
import hu.bme.mit.theta.xta.learning.compositional.common.TransitionConstraints;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transition function implementation for the zone-based DFA exploration used by
 * the inclusion oracle ({@link XtaInclusionOracle}).
 * <p>
 * In the learning-based compositional model checking framework, this class computes
 * the successor states during the emptiness check of the parallel composition
 * {@code T' || H^c}.
 * <p>
 * Given a current {@link ZoneDfaState} and an input symbol, it advances both
 * systems synchronously:
 * <ol>
 *   <li><b>Discrete step:</b> It determines the next state in the complemented
 *       hypothesis DFA ({@code H^c}).</li>
 *   <li><b>Continuous step:</b> It retrieves the timing constraints associated with
 *       the symbol (via {@link XtaTimingMapper}) and applies standard timed
 *       automaton semantics to the current Zone. The strict operational order is:
 *       <i>Source Invariants; Clock Guards; Resets; Time Elapse (up);
 *       Target Invariants; LU-Extrapolation (norm).</i></li>
 * </ol>
 * If the physical timing constraints cannot be satisfied at any point during the
 * continuous step, the resulting zone becomes empty (Bottom). The function returns
 * this "Bottom" state to safely cut off the exploration branch.
 *
 * @param <S> The state type of the DFA (typically representing the hypothesis or its complement).
 * @param <P> The precision type used by the underlying analysis.
 */
public class ZoneDfaTransFunc<S, P extends Prec> implements TransFunc<ZoneDfaState<S>, LearnLibAction<String>, P> {
    private final DFA<S, String> hypothesis;
    private final Map<VarDecl<RatType>, Integer> ceilings;
    private final XtaTimingMapper<?, ?> mapper;

    private ZoneDfaTransFunc(DFA<S, String> hypothesis, Map<VarDecl<RatType>, Integer> ceilings, XtaTimingMapper<?, ?> mapper) {
        this.hypothesis = checkNotNull(hypothesis);
        this.mapper = checkNotNull(mapper);
        this.ceilings = checkNotNull(ceilings);
    }

    /**
     * Creates a new {@link ZoneDfaTransFunc}.
     *
     * @param hypothesis The DFA representing {@code H^c} (the complemented hypothesis).
     * @param ceilings   A map assigning maximum constants to clock variables, required
     *                   for the k-extrapolation (normalization) of zones.
     * @param mapper     The mapper used to decode the abstract string symbol back into
     *                   concrete {@link TransitionConstraints}.
     * @return A new {@link ZoneDfaTransFunc} instance.
     */
    public static<S, P extends Prec> ZoneDfaTransFunc<S, P> create
            (DFA<S, String> hypothesis, Map<VarDecl<RatType>, Integer> ceilings, XtaTimingMapper<?, ?> mapper) {
        return new ZoneDfaTransFunc<>(hypothesis, ceilings, mapper);
    }


    @Override
    public Collection<ZoneDfaState<S>> getSuccStates(ZoneDfaState<S> state, LearnLibAction<String> action, P prec) {
        String symbol = action.getSymbol();

        // 1. Discrete step: Advance the DFA.
        S nextDfaState = hypothesis.getTransition(state.getDfaState(), symbol);
        if (nextDfaState == null) {
            return Collections.emptyList();
        }

        // 2. Map symbol to physical timing constraints.
        TransitionConstraints constraints = mapper.mapInput(symbol);
        if (constraints == null) {
            throw new RuntimeException("Invalid symbol: " + symbol);
        }

        ZoneState currentZone = state.getZoneState();
        ZoneState.Builder builder = currentZone.transform();

        // 3. Apply continuous semantics strictly in order.

        // 3a. Source invariants
        for (Guard.ClockGuard inv : constraints.getSourceInvariants()) {
            builder.and(inv.getClockConstr());
        }
        if (builder.build().isBottom()) {
            return Collections.singleton(ZoneDfaState.create(builder.build(), nextDfaState));
        }
        // 3b. Clock guards
        for (Guard.ClockGuard guard : constraints.getClockGuards()) {
            builder.and(guard.getClockConstr());
        }
        if (builder.build().isBottom()) {
            return Collections.singleton(ZoneDfaState.create(builder.build(), nextDfaState));
        }
        // 3c. Clock resets
        for (Update reset : constraints.getResets()) {
            if (reset.isClockUpdate()) {
                builder.execute(reset.asClockUpdate().getClockOp());
            }
        }
        // 3d. Time elapse (delay)
        builder.up();
        // 3e. Target invariants
        for (Guard.ClockGuard inv : constraints.getTargetInvariants()) {
            builder.and(inv.getClockConstr());
        }
        if (builder.build().isBottom()) {
            return Collections.singleton(ZoneDfaState.create(builder.build(), nextDfaState));
        }
        // 3f. K-extrapolation (Normalization) to guarantee finiteness
        builder.norm(ceilings);

        ZoneState nextZone = builder.build();
        if (nextZone.isBottom()) {
            return Collections.singleton(ZoneDfaState.create(nextZone, nextDfaState));
        }

        ZoneDfaState<S> nextState = ZoneDfaState.create(nextZone, nextDfaState);
        return Collections.singleton(nextState);
    }
}
