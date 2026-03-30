package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;

import net.automatalib.automaton.fsa.DFA;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class ZoneDfaInitFunc<S, P extends Prec> implements InitFunc<ZoneDfaState<S>, P> {
    private final DFA<S, String> hypothesis;
    private final Set<VarDecl<RatType>> clocks;

    private ZoneDfaInitFunc(DFA<S, String> hypothesis, Map<VarDecl<RatType>, Integer> ceilings) {
        this.hypothesis = checkNotNull(hypothesis);
        this.clocks = checkNotNull(ceilings).keySet();
    }

    public static<S, P extends Prec> ZoneDfaInitFunc<S, P> create(DFA<S, String> hypothesis, Map<VarDecl<RatType>, Integer> ceilings) {
        return new ZoneDfaInitFunc<>(hypothesis, ceilings);
    }

    @Override
    public Collection<ZoneDfaState<S>> getInitStates(P prec) {
        S initDfaState = hypothesis.getInitialState();
        ZoneState initZone = ZoneState.zero(clocks);
        ZoneDfaState<S> initState = ZoneDfaState.create(initZone, initDfaState);

        return Collections.singleton(initState);
    }
}
