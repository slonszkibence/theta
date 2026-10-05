package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsCompositionTransFunc <S extends ExprState, prodS, P extends Prec>
        implements TransFunc<XstsCompositionState<S, prodS>, XstsAction, P> {

    private final TransFunc<XstsState<S>, XstsAction, P> transFunc;
    private final DFA<prodS, XstsAction> productDfa;
    private final Alphabet<XstsAction> alphabet;
    private final TimedXstsActionProjections projections;
    private final XstsCompositionLts.UnknownSymbolHandler unknownSymbolHandler;
    private final prodS topState;

    private XstsCompositionTransFunc(
            TransFunc<XstsState<S>, XstsAction, P> transFunc,
            DFA<prodS, XstsAction> productDfa,
            Alphabet<XstsAction> alphabet,
            TimedXstsActionProjections projections,
            XstsCompositionLts.UnknownSymbolHandler unknownSymbolHandler,
            prodS topState) {
        this.topState = topState;
        this.transFunc = checkNotNull(transFunc);
        this.productDfa = checkNotNull(productDfa);
        this.alphabet = checkNotNull(alphabet);
        this.projections = checkNotNull(projections);
        this.unknownSymbolHandler = checkNotNull(unknownSymbolHandler);
    }

    public static <S extends ExprState, prodS, P extends Prec> XstsCompositionTransFunc<S, prodS, P> create(
            TransFunc<XstsState<S>, XstsAction, P> transFunc,
            DFA<prodS, XstsAction> productDfa,
            Alphabet<XstsAction> alphabet,
            TimedXstsActionProjections projections) {
        return create(transFunc, productDfa, alphabet, projections,
                XstsCompositionLts.UnknownSymbolHandler.FAIL);
    }

    public static <S extends ExprState, prodS, P extends Prec> XstsCompositionTransFunc<S, prodS, P> create(
            TransFunc<XstsState<S>, XstsAction, P> transFunc,
            DFA<prodS, XstsAction> productDfa,
            Alphabet<XstsAction> alphabet,
            TimedXstsActionProjections projections,
            XstsCompositionLts.UnknownSymbolHandler unknownSymbolHandler) {
        return create(transFunc, productDfa, alphabet, projections, unknownSymbolHandler, null);
    }

    public static <S extends ExprState, prodS, P extends Prec> XstsCompositionTransFunc<S, prodS, P> create(
            TransFunc<XstsState<S>, XstsAction, P> transFunc,
            DFA<prodS, XstsAction> productDfa,
            Alphabet<XstsAction> alphabet,
            TimedXstsActionProjections projections,
            XstsCompositionLts.UnknownSymbolHandler unknownSymbolHandler,
            prodS topState) {
        return new XstsCompositionTransFunc<>(transFunc, productDfa, alphabet, projections,
                unknownSymbolHandler, topState);
    }

    @Override
    public Collection<? extends XstsCompositionState<S, prodS>> getSuccStates(
            XstsCompositionState<S, prodS> state, XstsAction action, P prec) {
        final Collection<? extends XstsState<S>> succXstsStates =
                transFunc.getSuccStates(state.getXstsState(), action, prec);

        final XstsAction clockAction = projections.clockProjection(action);
        final prodS nextDfaState;
        if (clockAction.getStmts().isEmpty()) {
            nextDfaState = state.getDfaState();
        } else if (!alphabet.containsSymbol(clockAction)) {
            unknownSymbolHandler.onUnknownSymbol(clockAction);
            nextDfaState = topState;
        } else {
            nextDfaState = productDfa.getTransition(state.getDfaState(), clockAction);
            if (nextDfaState == null) {
                return List.of();
            }
        }
        if (!productDfa.isAccepting(nextDfaState)) {
            return List.of();
        }

        return succXstsStates.stream()
                .map(succ -> XstsCompositionState.create(succ, nextDfaState))
                .toList();
    }
}
