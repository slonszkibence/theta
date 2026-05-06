package hu.bme.mit.theta.xta.learning.compositional.inclusion;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.xta.learning.compositional.common.LearnLibAction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Analysis implementation for the zone-based DFA exploration used by the
 * inclusion oracle ({@link XtaInclusionOracle}).
 * <p>
 * In the learning-based compositional model checking framework, the original timed
 * system is decomposed into a discrete finite automaton {@code A} and a pure-timing
 * timed automaton {@code T'}. After the learning phase produces a hypothesis DFA
 * {@code H} (which overapproximates {@code T'}), the inclusion oracle must answer
 * an equivalence query by checking whether {@code L(T') ⊆ L(H)} holds.
 * <p>
 *
 * @param <S> The state type of the DFA (typically representing the hypothesis or its complement).
 * @param <P> The precision type used by the underlying zone analysis (e.g., ZonePrec).
 */
public class ZoneDfaAnalysis<S, P extends Prec> implements Analysis<ZoneDfaState<S>, LearnLibAction<String>, P> {
    private final InitFunc<ZoneDfaState<S>, P> initFunc;
    private final TransFunc<ZoneDfaState<S>, LearnLibAction<String>, P> transFunc;
    private final PartialOrd<ZoneDfaState<S>> partialOrd;

    private ZoneDfaAnalysis(final InitFunc<ZoneDfaState<S>, P> initFunc, final TransFunc<ZoneDfaState<S>,
            LearnLibAction<String>, P> transFunc,  final PartialOrd<ZoneDfaState<S>> partialOrd) {
        this.initFunc = checkNotNull(initFunc);
        this.transFunc = checkNotNull(transFunc);
        this.partialOrd = checkNotNull(partialOrd);
    }

    /**
     * Creates a new {@link ZoneDfaAnalysis} from the given components.
     *
     * @param initFunc   The function computing the initial states of the analysis.
     * @param transFunc  The transition function advancing the zone-DFA state on an input symbol.
     * @param partialOrd The partial order used for subsumption checks during ARG exploration.
     */
    public static<S, P extends Prec> ZoneDfaAnalysis<S, P> create(final InitFunc<ZoneDfaState<S>, P> initFunc, final TransFunc<ZoneDfaState<S>,
            LearnLibAction<String>, P> transFunc,  final PartialOrd<ZoneDfaState<S>> partialOrd) {
        return new ZoneDfaAnalysis<>(initFunc, transFunc, partialOrd);
    }

    @Override
    public InitFunc<ZoneDfaState<S>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<ZoneDfaState<S>, LearnLibAction<String>, P> getTransFunc() {
        return transFunc;
    }
    @Override
    public PartialOrd<ZoneDfaState<S>> getPartialOrd() {
        return partialOrd;
    }
}