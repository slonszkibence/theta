package hu.bme.mit.theta.xta.learning.compositional.realizability;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaLts;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Labeled Transition System (LTS) implementation for the discrete finite-state
 * model checking phase.
 * <p>
 * In the learning-based compositional model checking framework, this LTS defines
 * the enabled actions for the parallel composition {@code A || H}. It ensures that
 * the exploration algorithm only considers paths that are simultaneously valid in
 * both the purely discrete timed system abstraction ({@code A}) and the product
 * DFA hypothesis ({@code H}).
 * <p>
 * The logic works as an intersection filter:
 * <ol>
 *   <li>First, it queries the underlying discrete {@link XtaLts} for all structurally
 *       enabled discrete actions (ignoring clocks).</li>
 *   <li>Then, it simulates each action on the DFA. Since a single {@link XtaAction}
 *       might involve multiple synchronization edges (e.g., binary or broadcast
 *       channels), it extracts all constituent edges, maps them to their abstract
 *       alphabet symbols, and steps through the DFA.</li>
 *   <li>An action is only considered enabled if the DFA accepts the resulting
 *       sequence of symbols.</li>
 * </ol>
 *
 * @param <S>     The inner data state type of the XTA (e.g., {@code ExplState}).
 * @param <prodS> The state type of the product DFA (representing the learned hypothesis).
 */
public class DiscreteCompositionLts<S extends State, prodS>
        implements LTS<DiscreteCompositionState<S, prodS>, XtaAction> {

    private final Alphabet<String> alphabet;
    private final DFA<prodS, String> productDfa;
    private final LTS<XtaState<?>, XtaAction> innerLts;

    private DiscreteCompositionLts(XtaSystem xtaSystem, Alphabet<String> alphabet,
                                   DFA<prodS, String> productDfa) {
        this.alphabet = checkNotNull(alphabet);
        this.productDfa = checkNotNull(productDfa);
        this.innerLts = XtaLts.create(xtaSystem);
    }

    /**
     * Creates a new {@link DiscreteCompositionLts}.
     *
     * @param xtaSystem  The timed automaton system, used to create the underlying discrete LTS.
     * @param alphabet   The abstract alphabet of the learning algorithm.
     * @param productDfa The product DFA representing the learned hypothesis {@code H}.
     * @param <D>        The inner data state type.
     * @param <prodS>    The state type of the product DFA.
     * @return A new {@link DiscreteCompositionLts} instance.
     */
    public static <D extends State, prodS> DiscreteCompositionLts<D, prodS> create(
            XtaSystem xtaSystem, Alphabet<String> alphabet, DFA<prodS, String> productDfa) {
        return new DiscreteCompositionLts<>(xtaSystem, alphabet, productDfa);
    }

    @Override
    public Collection<XtaAction> getEnabledActionsFor(final DiscreteCompositionState<S, prodS> compositionState) {
        // 1. Get all structurally enabled discrete actions from the underlying XTA LTS.
        final Collection<XtaAction> xtaActions =
                innerLts.getEnabledActionsFor(compositionState.getXtaState());
        // 2. Filter actions by intersecting them with the learned hypothesis DFA.
        Collection<XtaAction> validDiscreteActions = new ArrayList<>();
        for (XtaAction xtaAction : xtaActions) {
            if (isAccepting(xtaAction, compositionState.getDfaState())) {
                validDiscreteActions.add(xtaAction);
            }
        }
        return validDiscreteActions;
    }

    /**
     * Simulates the execution of a given XTA action on the product DFA to check
     * if the hypothesis permits this behavior.
     *
     * @param xtaAction The discrete action to simulate.
     * @param dfaState  The current state in the DFA.
     * @return {@code true} if the DFA transitions to an accepting state, {@code false} otherwise.
     */
    private boolean isAccepting(final XtaAction xtaAction, prodS dfaState) {
        List<XtaProcess.Edge> edges = new ArrayList<>();

        // Deconstruct the action into its fundamental edges (for sync/broadcast).
        if (xtaAction.isBasic()) {
            edges.add(xtaAction.asBasic().getEdge());
        } else if (xtaAction.isBinary()) {
            edges.add(xtaAction.asBinary().getEmitEdge());
            edges.add(xtaAction.asBinary().getRecvEdge());
        } else if (xtaAction.isBroadcast()) {
            edges.add(xtaAction.asBroadcast().getEmitEdge());
            edges.addAll(xtaAction.asBroadcast().getRecvEdges());
        }

        prodS currDfaState = dfaState;
        for (XtaProcess.Edge edge : edges) {
            String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
            // If the symbol is part of the alphabet, we must follow the transition in the DFA.
            // If it is not in the alphabet, the DFA does not restrict it (stuttering).
            if (alphabet.containsSymbol(symbol)) {
                currDfaState = productDfa.getTransition(currDfaState, symbol);
                // If there is no transition defined, the action is blocked.
                if (currDfaState == null) {
                    return false;
                }
            }
        }
        // Action is only valid if we land in an accepting state.
        return productDfa.isAccepting(currDfaState);
    }
}
