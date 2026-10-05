package hu.bme.mit.theta.xsts.learning.inclusion;

import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.analysis.zone.ZoneStmtTransFunc;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.learning.common.XstsTimingMapper;
import hu.bme.mit.theta.xsts.learning.common.LearnLibAction;
import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ZoneDfaTransFunc<S, I> implements TransFunc<ZoneDfaState<S>, LearnLibAction<I>, ZonePrec> {
    private final DFA<S, I> hypothesis;
    private final Map<VarDecl<ClockType>, Integer> ceilings;
    private final XstsTimingMapper<I, ?, ?> mapper;
    private final Map<ZoneStep<I>, ZoneState> zoneCache;

    private record ZoneStep<I>(ZoneState zone, I symbol) {}

    private ZoneDfaTransFunc(DFA<S, I> hypothesis, Map<VarDecl<ClockType>, Integer> ceilings,
                             XstsTimingMapper<I, ?, ?> mapper, Map<ZoneStep<I>, ZoneState> zoneCache) {
        this.hypothesis = checkNotNull(hypothesis);
        this.mapper = checkNotNull(mapper);
        this.ceilings = checkNotNull(ceilings);
        this.zoneCache = checkNotNull(zoneCache);
    }

    public static<S, I> ZoneDfaTransFunc<S, I> create
    (DFA<S, I> hypothesis, Map<VarDecl<ClockType>, Integer> ceilings, XstsTimingMapper<I, ?, ?> mapper) {
        return new ZoneDfaTransFunc<>(hypothesis, ceilings, mapper, new java.util.HashMap<>());
    }

    public static<S, I> ZoneDfaTransFunc<S, I> create
    (DFA<S, I> hypothesis, Map<VarDecl<ClockType>, Integer> ceilings, XstsTimingMapper<I, ?, ?> mapper,
     Map<?, ?> sharedZoneCache) {
        @SuppressWarnings("unchecked")
        final Map<ZoneStep<I>, ZoneState> cache = (Map<ZoneStep<I>, ZoneState>) sharedZoneCache;
        return new ZoneDfaTransFunc<>(hypothesis, ceilings, mapper, cache);
    }


    @Override
    public Collection<ZoneDfaState<S>> getSuccStates(ZoneDfaState<S> state, LearnLibAction<I> action, ZonePrec prec) {
        S nextDfaState = hypothesis.getTransition(state.getDfaState(), action.getSymbol());
        if (nextDfaState == null) {
            return Collections.emptyList();
        }

        final ZoneState currentZone = state.getZoneState();
        if (currentZone.isBottom()) {
            return Collections.emptyList();
        }

        final ZoneStep<I> key = new ZoneStep<>(currentZone, action.getSymbol());
        ZoneState succZone = zoneCache.get(key);
        if (succZone == null) {
            final XstsAction clockAction = mapper.mapInput(action.getSymbol());
            succZone = ZoneStmtTransFunc
                    .getInstance().
                    getSuccStates(currentZone, clockAction, prec).
                    iterator().
                    next();

            if (!succZone.isBottom()) {
                succZone = succZone.transform().norm(ceilings).build();
            }
            zoneCache.put(key, succZone);
        }

        return Collections.singleton(ZoneDfaState.create(succZone, nextDfaState));
    }
}