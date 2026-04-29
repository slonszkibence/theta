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

public class ZoneDfaTransFunc<S, P extends Prec> implements TransFunc<ZoneDfaState<S>, LearnLibAction<String>, P> {
    private final DFA<S, String> hypothesis;
    private final Map<VarDecl<RatType>, Integer> ceilings;
    private final XtaTimingMapper<?, ?> mapper;

    private ZoneDfaTransFunc(DFA<S, String> hypothesis, Map<VarDecl<RatType>, Integer> ceilings, XtaTimingMapper<?, ?> mapper) {
        this.hypothesis = checkNotNull(hypothesis);
        this.mapper = checkNotNull(mapper);
        this.ceilings = checkNotNull(ceilings);
    }

    public static<S, P extends Prec> ZoneDfaTransFunc<S, P> create
            (DFA<S, String> hypothesis, Map<VarDecl<RatType>, Integer> ceilings, XtaTimingMapper<?, ?> mapper) {
        return new ZoneDfaTransFunc<>(hypothesis, ceilings, mapper);
    }


    @Override
    public Collection<ZoneDfaState<S>> getSuccStates(ZoneDfaState<S> state, LearnLibAction<String> action, P prec) {
        String symbol = action.getSymbol();

        S nextDfaState = hypothesis.getTransition(state.getDfaState(), symbol);
        if (nextDfaState == null) {
            return Collections.emptyList();
        }

        TransitionConstraints constraints = mapper.mapInput(symbol);
        if (constraints == null) {
            throw new RuntimeException("Invalid symbol: " + symbol);
        }

        ZoneState currentZone = state.getZoneState();
        ZoneState.Builder builder = currentZone.transform();

        for (Guard.ClockGuard inv : constraints.getSourceInvariants()) {
            builder.and(inv.getClockConstr());
        }
        if (builder.build().isBottom()) {
            return Collections.singleton(ZoneDfaState.create(builder.build(), nextDfaState));
        }

        for (Guard.ClockGuard guard : constraints.getClockGuards()) {
            builder.and(guard.getClockConstr());
        }
        if (builder.build().isBottom()) {
            return Collections.singleton(ZoneDfaState.create(builder.build(), nextDfaState));
        }

        for (Update reset : constraints.getResets()) {
            if (reset.isClockUpdate()) {
                builder.execute(reset.asClockUpdate().getClockOp());
            }
        }

        builder.up();

        for (Guard.ClockGuard inv : constraints.getTargetInvariants()) {
            builder.and(inv.getClockConstr());
        }
        if (builder.build().isBottom()) {
            return Collections.singleton(ZoneDfaState.create(builder.build(), nextDfaState));
        }

        builder.norm(ceilings);

        ZoneState nextZone = builder.build();
        if (nextZone.isBottom()) {
            return Collections.singleton(ZoneDfaState.create(nextZone, nextDfaState));
        }

        ZoneDfaState<S> nextState = ZoneDfaState.create(nextZone, nextDfaState);
        return Collections.singleton(nextState);
    }
}
