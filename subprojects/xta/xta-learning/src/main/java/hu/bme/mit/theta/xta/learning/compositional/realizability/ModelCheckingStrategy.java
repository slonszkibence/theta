package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.AbstractorResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.xta.analysis.XtaAction;

/**
 * Defines the core verification strategy for the discrete finite-state model checking phase.
 * <p>
 * In the learning-based compositional model checking framework, once the parallel
 * composition {@code A || H} is defined via its LTS and Analysis, this interface
 * determines <i>how</i> the safety checking is performed. It encapsulates the decision
 * of whether to use a single-pass exploration or an iterative Counterexample-Guided
 * Abstraction Refinement (CEGAR) loop.
 *
 * @param <S> The inner data state type of the XTA (e.g., {@code ExplState}).
 * @param <prodS> The state type of the product DFA (representing the learned hypothesis).
 * @param <P> The precision type used by the underlying discrete analysis.
 */
public interface ModelCheckingStrategy<S extends State, prodS, P extends Prec> {

    /**
     * Builds the final {@link SafetyChecker} using the provided abstractor.
     *
     * @param abstractor The abstractor responsible for generating the Abstract Reachability Graph (ARG).
     * @return A fully configured {@link SafetyChecker} ready to verify the composite system.
     */
    SafetyChecker<DiscreteCompositionState<S, prodS>, XtaAction, P> build(
            Abstractor<DiscreteCompositionState<S, prodS>, XtaAction, P> abstractor
    );

    /**
     * Creates a plain (single-pass) model checking strategy.
     * <p>
     * This strategy simply runs the abstractor once to explore the state space. It does
     * not perform any refinement. This is particularly useful when the initial analysis
     * is known to be exact (e.g., using full explicit precision where all variables are
     * tracked), meaning any counterexample found is guaranteed to be a true logical path
     * in the discrete domain.
     *
     * @param <S> The inner data state type of the XTA.
     * @param <prodS> The state type of the product DFA.
     * @param <P> The precision type.
     * @return A plain model checking strategy.
     */
    static <S extends State, prodS, P extends Prec> ModelCheckingStrategy<S, prodS, P> plain() {
        return abstractor -> prec -> {
            ARG<DiscreteCompositionState<S, prodS>, XtaAction> arg = abstractor.createArg();
            AbstractorResult result = abstractor.check(arg, prec);

            if (result.isSafe()) {
                return SafetyResult.safe(arg);
            } else {
                ArgTrace<DiscreteCompositionState<S, prodS>, XtaAction> cex = arg.getCexs()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Unsafe result but no counterexample found in ARG"));
                return SafetyResult.unsafe(cex.toTrace(), arg);
            }
        };
    }

    /**
     * Creates a CEGAR-based model checking strategy.
     * <p>
     * This strategy wraps the abstractor and the provided {@link Refiner} into a
     * standard {@link CegarChecker} loop. If the abstractor finds a counterexample in
     * the abstract state space, the refiner evaluates its feasibility. If the path is
     * spurious, the precision is refined, and the exploration restarts.
     *
     * @param refiner The refiner used to validate counterexamples and compute new precisions.
     * @param <S>     The inner data state type of the XTA.
     * @param <prodS>     The state type of the product DFA.
     * @param <P>     The precision type.
     * @return A CEGAR-based model checking strategy.
     */
    static <S extends State, prodS, P extends Prec> ModelCheckingStrategy<S, prodS, P> cegar(
            Refiner<DiscreteCompositionState<S, prodS>, XtaAction, P> refiner
    ) {
        return abstractor -> CegarChecker.create(abstractor, refiner);
    }
}
