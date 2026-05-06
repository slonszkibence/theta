package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;

import net.automatalib.automaton.fsa.DFA;

import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Initial state function for the zone-based DFA exploration used by the
 * inclusion oracle ({@link XtaInclusionOracle}).
 * <p>
 * In the learning-based compositional model checking framework, this function
 * computes the initial state for the emptiness check of the parallel composition
 * {@code T' || H^c}.
 * <p>
 * The initial state is constructed by pairing:
 * <ul>
 *   <li>The initial state of the continuous time domain (a {@link ZoneState} where all
 *       clocks are zero, advanced by an initial time elapse using {@code up()}).</li>
 *   <li>The initial discrete state of the DFA representing {@code H^c} (the complement
 *       of the hypothesis).</li>
 * </ul>
 *
 * @param <S> The state type of the DFA (typically representing the hypothesis or its complement).
 * @param <P> The precision type used by the underlying analysis.
 */

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
        ZoneState initZone = ZoneState.zero(clocks).transform().up().build();
        ZoneDfaState<S> initState = ZoneDfaState.create(initZone, initDfaState);

        return Collections.singleton(initState);
    }
}