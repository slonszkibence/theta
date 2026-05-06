package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transition function implementation for the discrete finite-state model checking phase.
 * <p>
 * In the learning-based compositional model checking framework, this class computes
 * the successor states for the parallel composition {@code A || H}, where {@code A}
 * is the purely discrete abstraction of the timed system and {@code H} is the product
 * DFA representing the learned hypothesis.
 * <p>
 * Given a current composite state and a discrete action, it advances both components
 * synchronously:
 * <ol>
 *   <li><b>Discrete XTA Step:</b> It calculates the successor states for the
 *       underlying data variables using the inner XTA transition function.</li>
 *   <li><b>DFA Step:</b> It maps the executed {@link XtaAction} to an abstract alphabet
 *       symbol. If the symbol is part of the learning alphabet, the DFA transitions
 *       to its next state. If the symbol is not in the alphabet, the DFA "stutters"
 *       (remains in its current state). If the DFA rejects the transition (i.e., no
 *       valid next state is defined), the path is blocked and an empty collection is returned.</li>
 * </ol>
 *
 * @param <S>     The inner data state type of the XTA (e.g., {@code ExplState}).
 * @param <prodS> The state type of the product DFA (representing the learned hypothesis).
 * @param <P>     The precision type used by the underlying discrete analysis.
 */
public class DiscreteCompositionTransFunc<S extends State, prodS, P extends Prec>
        implements TransFunc<DiscreteCompositionState<S, prodS>, XtaAction, P> {

    private final TransFunc<XtaState<S>, XtaAction, P> innerTransFunc;
    private final DFA<prodS, String> productDfa;
    private final Alphabet<String> alphabet;

    private DiscreteCompositionTransFunc(TransFunc<XtaState<S>, XtaAction, P> innerTransFunc,
                                         DFA<prodS, String> productDfa,
                                         Alphabet<String> alphabet) {
        this.innerTransFunc = checkNotNull(innerTransFunc);
        this.productDfa = checkNotNull(productDfa);
        this.alphabet = checkNotNull(alphabet);
    }

    /**
     * Creates a new {@link DiscreteCompositionTransFunc}.
     *
     * @param innerTransFunc The transition function for the inner discrete XTA states.
     * @param productDfa     The product DFA representing the learned hypothesis {@code H}.
     * @param alphabet       The abstract alphabet used by the learning algorithm.
     * @param <S>            The inner data state type.
     * @param <prodS>        The state type of the product DFA.
     * @param <P>            The precision type.
     * @return A new {@link DiscreteCompositionTransFunc} instance.
     */
    public static <S extends State, prodS, P extends Prec> DiscreteCompositionTransFunc<S, prodS, P> create(
            TransFunc<XtaState<S>, XtaAction, P> innerTransFunc,
            DFA<prodS, String> productDfa,
            Alphabet<String> alphabet) {
        return new DiscreteCompositionTransFunc<>(innerTransFunc, productDfa, alphabet);
    }

    @Override
    public Collection<DiscreteCompositionState<S, prodS>> getSuccStates(
            DiscreteCompositionState<S, prodS> state, XtaAction action, P prec) {
        // 1. Advance the inner discrete XTA state
        Collection<? extends XtaState<S>> nextXtaStates =
                innerTransFunc.getSuccStates(state.getXtaState(), action, prec);
        // 2. Extract the abstract symbol corresponding to this action
        String symbol = extractSymbolForAction(action);
        // 3. Advance the DFA state based on the symbol
        prodS nextDfaState = state.getDfaState();
        if (symbol != null && alphabet.containsSymbol(symbol)) {
            prodS candidate = productDfa.getTransition(state.getDfaState(), symbol);
            if (candidate != null) {
                nextDfaState = candidate;
            }
            else {
                // If the DFA restricts this transition, the path is blocked.
                return Collections.emptyList();
            }
        }
        // 4. Combine the resulting states
        Collection<DiscreteCompositionState<S, prodS>> result = new ArrayList<>();
        for (XtaState<S> xtaState : nextXtaStates) {
            result.add(DiscreteCompositionState.create(xtaState, nextDfaState));
        }
        return result;
    }

    /**
     * Extracts the abstract alphabet symbol corresponding to a given discrete XTA action.
     * <p>
     * Since a single {@link XtaAction} may represent a complex synchronization (e.g.,
     * binary or broadcast channels) involving multiple physical edges, this method
     * deconstructs the action into its constituent edges. It then checks if any of
     * these edges map to a symbol present in the learning alphabet.
     *
     * @param xtaAction The discrete action executed by the XTA.
     * @return The corresponding abstract symbol if it exists in the alphabet, or {@code null} otherwise.
     */
    private String extractSymbolForAction(XtaAction xtaAction) {
        List<XtaProcess.Edge> edges = new ArrayList<>();

        if (xtaAction.isBasic()) {
            edges.add(xtaAction.asBasic().getEdge());
        } else if (xtaAction.isBinary()) {
            edges.add(xtaAction.asBinary().getEmitEdge());
            edges.add(xtaAction.asBinary().getRecvEdge());
        } else if (xtaAction.isBroadcast()) {
            edges.add(xtaAction.asBroadcast().getEmitEdge());
            edges.addAll(xtaAction.asBroadcast().getRecvEdges());
        }

        for (XtaProcess.Edge edge : edges) {
            String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
            if (alphabet.containsSymbol(symbol)) {
                return symbol;
            }
        }
        return null;
    }
}