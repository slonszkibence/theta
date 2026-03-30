package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.analysis.Action;

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
