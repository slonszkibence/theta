package hu.bme.mit.theta.xta.learning.compositional.common;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Builds the synchronous product automaton {@code A ‖ H} from two DFAs.
 * <p>
 * In the learning-based compositional model checking algorithm, once the
 * learning algorithm has found a hypothesis {@code H} that satisfies
 * {@code L(T) ⊆ L(H)}, the product {@code A ‖ H} is constructed and
 * handed to the finite-state model checker to verify whether
 * {@code L(A ‖ H) ⊆ Spec} holds.
 * <p>
 * The product is computed by a standard BFS over reachable state pairs
 * {@code (s_A, s_H)}. A state in the product is accepting if and only if
 * both component states are accepting, corresponding to the language
 * identity {@code L(A ‖ H) = L(A) ∩ L(H)}.
 */
public class ProductAutomatonBuilder {

    private ProductAutomatonBuilder() {}


    public static <S1, S2, I> FastDFA<I> buildProduct(
            DFA<S1, I> untimedA,
            DFA<S2, I> learnedH,
            Alphabet<I> alphabet
    ) {

        FastDFA<I> productDfa = new FastDFA<>(alphabet);
        Map<StatePair<S1, S2>, FastDFAState> visited = new HashMap<>();
        Queue<StatePair<S1, S2>> queue = new ArrayDeque<>();

        S1 initA = untimedA.getInitialState();
        S2 initH = learnedH.getInitialState();

        if (initA == null || initH == null) return productDfa;

        StatePair<S1, S2> initPair = new StatePair<>(initA, initH);

        boolean initAccepting = untimedA.isAccepting(initA) && learnedH.isAccepting(initH);
        FastDFAState prodInitState = productDfa.addState(initAccepting);
        productDfa.setInitialState(prodInitState);

        visited.put(initPair, prodInitState);
        queue.add(initPair);

        while (!queue.isEmpty()) {
            StatePair<S1, S2> curr = queue.poll();
            FastDFAState prodCurrState = visited.get(curr);

            for (I symbol : alphabet) {
                S1 succA = untimedA.getSuccessor(curr.s1(), symbol);
                S2 succH = learnedH.getSuccessor(curr.s2(), symbol);

                if (succA != null && succH != null) {
                    StatePair<S1, S2> succPair = new StatePair<>(succA, succH);

                    FastDFAState prodSuccState = visited.get(succPair);

                    if (prodSuccState == null) {
                        boolean isAccepting = untimedA.isAccepting(succA) && learnedH.isAccepting(succH);
                        prodSuccState = productDfa.addState(isAccepting);
                        visited.put(succPair, prodSuccState);
                        queue.add(succPair);
                    }

                    productDfa.addTransition(prodCurrState, symbol, prodSuccState);
                }
            }
        }

        return productDfa;
    }

    private record StatePair<S1, S2>(S1 s1, S2 s2) {
    }
}