package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.LifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.RandomWaitlist;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.List;
import java.util.function.Predicate;

/**
 * Factory class for constructing the safety checker used in the discrete
 * finite-state model checking phase.
 * <p>
 * In the learning-based compositional model checking framework, this factory
 * assembles the core components (Analysis, LTS, Waitlist) into a fully functional
 * Theta {@link SafetyChecker} capable of verifying the parallel composition
 * {@code A || H}.
 * <p>
 * It strictly separates the construction of the state space exploration logic
 * (the {@link Abstractor}) from the execution strategy (e.g., Plain or CEGAR),
 * allowing flexible runtime configuration.
 */
public class DiscreteCompositionCheckerFactory {

    /**
     * Defines the exploration strategy used to traverse the composite state space.
     */
    public enum SearchStrategy {
        /** Breadth-First Search (explores shortest paths first). */
        BFS,
        /** Depth-First Search (explores deep paths first, uses less memory). */
        DFS,
        /** Randomized Search (useful for finding bugs in large state spaces). */
        RANDOM
    }

    private DiscreteCompositionCheckerFactory() {}

    /**
     * Assembles and returns the final safety checker.
     *
     * @param modelCheckingStrategy The execution strategy (e.g., Plain or CEGAR).
     * @param searchStrategy        The state space exploration strategy (BFS, DFS, RANDOM).
     * @param analysis              The bundled analysis components (Init, Trans, Ord).
     * @param lts                   The Labeled Transition System defining enabled actions.
     * @param targetPred            The predicate defining the unsafe (error) states.
     * @param <S>                   The inner data state type of the XTA.
     * @param <prodS>               The state type of the product DFA.
     * @param <P>                   The precision type.
     * @return A fully configured {@link SafetyChecker} ready to verify the system.
     */
    public static <S extends State, prodS, P extends Prec> SafetyChecker<DiscreteCompositionState<S, prodS>, XtaAction, P> create(
            ModelCheckingStrategy<S, prodS, P> modelCheckingStrategy,
            SearchStrategy searchStrategy,
            DiscreteCompositionAnalysis<S, prodS, P> analysis,
            LTS<DiscreteCompositionState<S, prodS>, XtaAction> lts,
            Predicate<DiscreteCompositionState<S, prodS>> targetPred
    ) {
        Abstractor<DiscreteCompositionState<S, prodS>, XtaAction, P> abstractor =
                createAbstractor(searchStrategy, analysis, lts, targetPred);

        // Delegate the final construction to the selected strategy (e.g., wrapping it in a CegarChecker)
        return modelCheckingStrategy.build(abstractor);
    }

    /**
     * Builds the abstractor responsible for generating the Abstract Reachability Graph (ARG).
     * <p>
     * <b>Optimization Note:</b> This method configures a state projection heuristic.
     * By projecting the state onto its discrete control locations (the DFA state
     * and the network's XTA locations), the ARG builder can group similar states
     * together in the covering data structure. This drastically optimizes the
     * subsumption (covering) checks, as the algorithm only needs to compare data
     * variables between states that share the exact same control flow position.
     *
     * @param searchStrategy The search strategy determining the waitlist queue type.
     * @param analysis       The analysis bundle.
     * @param lts            The LTS.
     * @param targetPred     The target predicate.
     * @return A configured {@link BasicAbstractor}.
     */
    private static <S extends State, prodS, P extends Prec>
    Abstractor<DiscreteCompositionState<S, prodS>, XtaAction, P> createAbstractor(
            SearchStrategy searchStrategy,
            DiscreteCompositionAnalysis<S, prodS, P> analysis,
            LTS<DiscreteCompositionState<S, prodS>, XtaAction> lts,
            Predicate<DiscreteCompositionState<S, prodS>> targetPred
    ) {
        ArgBuilder<DiscreteCompositionState<S, prodS>, XtaAction, P> argBuilder =
                ArgBuilder.create(lts, analysis, targetPred, true);

        return BasicAbstractor.builder(argBuilder)
                .waitlist(createWaitlist(searchStrategy))
                // Crucial optimization: Group states in the ARG by their discrete control locations
                .projection(state -> List.of(
                        state.getDfaState(),
                        state.getXtaState().getLocs()))
                .build();
    }

    /**
     * Maps the {@link SearchStrategy} enum to the corresponding Theta {@link Waitlist} implementation.
     *
     * @param searchStrategy The desired search strategy.
     * @return The instantiated waitlist.
     */
    private static <D extends State, prodS>
    Waitlist<ArgNode<DiscreteCompositionState<D, prodS>, XtaAction>> createWaitlist(
            SearchStrategy searchStrategy
    ) {
        return switch (searchStrategy) {
            case BFS    -> FifoWaitlist.create();
            case DFS    -> LifoWaitlist.create();
            case RANDOM -> RandomWaitlist.create();
        };
    }
}