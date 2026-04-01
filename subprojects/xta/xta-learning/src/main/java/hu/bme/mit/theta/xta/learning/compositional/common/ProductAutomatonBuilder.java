package hu.bme.mit.theta.xta.learning.compositional.common;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public class ProductAutomatonBuilder {

    public static <S1, S2> FastDFA<String> buildProduct(
            DFA<S1, String> untimedA,
            DFA<S2, String> learnedH,
            Alphabet<String> alphabet) {

        FastDFA<String> productDfa = new FastDFA<>(alphabet);
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

            for (String symbol : alphabet) {
                S1 succA = untimedA.getSuccessor(curr.s1, symbol);
                S2 succH = learnedH.getSuccessor(curr.s2, symbol);

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

    private ProductAutomatonBuilder() {}

    private static class StatePair<S1, S2> {
        public final S1 s1;
        public final S2 s2;

        public StatePair(S1 s1, S2 s2) {
            this.s1 = s1;
            this.s2 = s2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StatePair<?, ?> that = (StatePair<?, ?>) o;
            return Objects.equals(s1, that.s1) && Objects.equals(s2, that.s2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(s1, s2);
        }
    }
}