package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.ControlFlowSplitXstsLts;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;
import hu.bme.mit.theta.xsts.learning.common.HypothesisGraph;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsCompositionLts<S extends ExprState, prodS> implements LTS<XstsCompositionState<S, prodS>, XstsAction> {
    @FunctionalInterface
    public interface UnknownSymbolHandler {


        void onUnknownSymbol(XstsAction clockAction);
    }

    private final DFA<prodS, XstsAction> dfa;
    private final Alphabet<XstsAction> alphabet;
    private final ControlFlowSplitXstsLts<S> innerLts;
    private final TimedXstsActionProjections projections;
    private final UnknownSymbolHandler unknownSymbolHandler;
    private final prodS topState;
    private final Logger logger;

    private XstsCompositionLts(DFA<prodS, XstsAction> dfa,
                               Alphabet<XstsAction> alphabet,
                               ControlFlowSplitXstsLts<S> innerLts,
                               TimedXstsActionProjections projections,
                               UnknownSymbolHandler unknownSymbolHandler,
                               prodS topState,
                               Logger logger) {
        this.dfa = checkNotNull(dfa);
        this.alphabet = checkNotNull(alphabet);
        this.innerLts = checkNotNull(innerLts);
        this.projections = checkNotNull(projections);
        this.unknownSymbolHandler = checkNotNull(unknownSymbolHandler);
        this.logger = checkNotNull(logger);
        this.topState = topState;
    }

    public static <S extends ExprState, prodS> XstsCompositionLts<S, prodS> create(
            DFA<prodS, XstsAction> dfa,
            Alphabet<XstsAction> alphabet,
            ControlFlowSplitXstsLts<S> innerLts,
            TimedXstsActionProjections projections,
            UnknownSymbolHandler unknownSymbolHandler,
            prodS topState,
            Logger logger) {
        return new XstsCompositionLts<>(dfa, alphabet, innerLts, projections, unknownSymbolHandler,
                topState, logger);
    }

    @Override
    public Collection<XstsAction> getEnabledActionsFor(XstsCompositionState<S, prodS> state) {
        if (state.isBottom()) {
            return List.of();
        }

        innerLts.reset();
        innerLts.splitControlFlows(state.getXstsState(), false);
        innerLts.updateSourceAbstractState(state.getXstsState());

        final Collection<XstsAction> validActions = new ArrayList<>();
        int flows = 0;
        int noClockEffect = 0;
        int blocked = 0;
        int unknown = 0;

        for (Optional<XstsAction> next = innerLts.getNextEnabledAction();
             next.isPresent();
             next = innerLts.getNextEnabledAction()) {
            final XstsAction action = next.get();
            flows++;
            final XstsAction clockAction = projections.clockProjection(action);
            final prodS nextDfaState;
            if (clockAction.getStmts().isEmpty()) {
                noClockEffect++;
                logSymbol("no clock effect", clockAction);
                nextDfaState = state.getDfaState();
            } else if (!alphabet.containsSymbol(clockAction)) {
                unknown++;
                logSymbol("NEW SYMBOL, not in the alphabet yet", clockAction);
                unknownSymbolHandler.onUnknownSymbol(clockAction);
                nextDfaState = topState;
            } else {
                nextDfaState = dfa.getTransition(state.getDfaState(), clockAction);
                if (nextDfaState == null) {
                    blocked++;
                    logSymbol("blocked by H (no transition)", clockAction);
                    continue;
                }
            }
            if (dfa.isAccepting(nextDfaState)) {
                validActions.add(action);
            } else {
                blocked++;
                logSymbol("blocked by H (rejecting successor)", clockAction);
            }
        }

        logger.write(Logger.Level.DETAIL,
                "      [Composition] %d control flow(s) -> %d enabled (%d no clock effect, %d blocked by H,"
                        + " %d new symbol(s))%n",
                flows, validActions.size(), noClockEffect, blocked, unknown);
        return validActions;
    }

    private void logSymbol(final String verdict, final XstsAction clockAction) {
        logger.write(Logger.Level.VERBOSE, "        [Composition] %-40s %s%n",
                HypothesisGraph.symbolLabel(clockAction), verdict);
    }
}
