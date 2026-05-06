package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Analysis implementation for the discrete finite-state model checking phase.
 * <p>
 * In the learning-based compositional model checking framework, once the learning
 * algorithm proposes a hypothesis DFA {@code H}, the finite-state model checking
 * oracle must verify the safety of the parallel composition {@code A || H}, where
 * {@code A} is the purely discrete abstraction of the timed system.
 * <p>
 * <ul>
 *   <li>The partial order ({@link PartialOrd}) for subsumption checking.</li>
 *   <li>The initial state function (delegated to {@link DiscreteCompositionInitFunc}).</li>
 *   <li>The transition function (delegated to {@link DiscreteCompositionTransFunc})
 *       that synchronously advances both the discrete system {@code A} and the DFA {@code H}.</li>
 * </ul>
 * Following the separation of concerns principle, this analysis should operate entirely
 * in the discrete domain (i.e., without Zone/DBM computations).
 *
 * @param <S>     The inner data state type of the XTA (e.g., {@code ExplState}).
 * @param <prodS> The state type of the product DFA (representing the learned hypothesis).
 * @param <P>     The precision type used by the underlying discrete analysis.
 */
public class DiscreteCompositionAnalysis<S extends State, prodS, P extends Prec>
        implements Analysis<DiscreteCompositionState<S, prodS>, XtaAction, P> {

    private final PartialOrd<DiscreteCompositionState<S, prodS>> partialOrd;
    private final InitFunc<DiscreteCompositionState<S, prodS>, P> initFunc;
    private final TransFunc<DiscreteCompositionState<S, prodS>, XtaAction, P> transFunc;

    private DiscreteCompositionAnalysis(
            PartialOrd<DiscreteCompositionState<S, prodS>> partialOrd,
            Alphabet<String> alphabet,
            DFA<prodS, String> productDFA,
            InitFunc<XtaState<S>, P> innerInitFunc,
            TransFunc<XtaState<S>, XtaAction, P> innerTransFunc
    ) {
        checkNotNull(alphabet);
        checkNotNull(productDFA);
        checkNotNull(innerInitFunc);
        checkNotNull(innerTransFunc);
        this.partialOrd = checkNotNull(partialOrd);
        this.transFunc = DiscreteCompositionTransFunc.create(innerTransFunc, productDFA, alphabet);
        this.initFunc  = DiscreteCompositionInitFunc.create(innerInitFunc, productDFA);
    }

    /**
     * Creates a new {@link DiscreteCompositionAnalysis}.
     *
     * @param partialOrd     The partial order used to check subsumption between composite states.
     * @param alphabet       The abstract alphabet of the learning algorithm.
     * @param productDFA     The DFA representing the learned hypothesis {@code H}.
     * @param innerInitFunc  The initialization function for the inner discrete XTA states.
     * @param innerTransFunc The transition function for the inner discrete XTA states.
     * @param <S>            The inner data state type.
     * @param <prodS>        The state type of the product DFA.
     * @param <P>            The precision type.
     * @return A new {@link DiscreteCompositionAnalysis} instance.
     */
    public static <S extends State, prodS, P extends Prec> DiscreteCompositionAnalysis<S, prodS, P> create(
            PartialOrd<DiscreteCompositionState<S, prodS>> partialOrd,
            Alphabet<String> alphabet,
            DFA<prodS, String> productDFA,
            InitFunc<XtaState<S>, P> innerInitFunc,
            TransFunc<XtaState<S>, XtaAction, P> innerTransFunc
    ) {
        return new DiscreteCompositionAnalysis<>(
                partialOrd, alphabet, productDFA, innerInitFunc, innerTransFunc);
    }

    @Override
    public PartialOrd<DiscreteCompositionState<S, prodS>> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<DiscreteCompositionState<S, prodS>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<DiscreteCompositionState<S, prodS>, XtaAction, P> getTransFunc() {
        return transFunc;
    }
}
