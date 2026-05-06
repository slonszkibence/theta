package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.analysis.Action;

/**
 * Adapter class that wraps a LearnLib alphabet symbol as a Theta {@link Action}.
 * In the learning-based compositional model checking framework, the alphabet
 * symbols used by the DFA learning algorithm (LearnLib) must be representable
 * as Theta actions so they can be processed in the analysis pipeline.
 * This class provides that bridge between the two frameworks.
 *
 * @param <S> the type of the wrapped alphabet symbol
 */
public class LearnLibAction<S>  implements Action {
    private final S symbol;

    private LearnLibAction(S symbol) {
        this.symbol = symbol;
    }

    public static <S> LearnLibAction<S> create(S symbol) {
        return new LearnLibAction<>(symbol);
    }

    public S getSymbol() {
        return symbol;
    }
}
