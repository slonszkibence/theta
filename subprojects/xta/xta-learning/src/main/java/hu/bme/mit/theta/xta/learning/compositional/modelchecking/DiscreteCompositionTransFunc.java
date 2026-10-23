package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionTransFunc<S extends State, prodS, P extends Prec>
        implements TransFunc<DiscreteCompositionState<S, prodS>, XtaAction, P> {
    public interface UnknownSymbolHandler {
        void onUnknownSymbol(String symbol, XtaAction action);
    }

    private static final int MAX_CACHE = 200_000;

    private final TransFunc<S, ? super StmtAction, ? super P> dataTransFunc;
    private final DFA<prodS, String> compositionDfa;
    private final Alphabet<String> alphabet;
    private final TimedStepper stepper;
    private final UnknownSymbolHandler unknownSymbolHandler;
    private final prodS topState;

    private final Map<XtaAction, List<Stmt>> dataStmtCache =
            new LinkedHashMap<>(1024, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<XtaAction, List<Stmt>> eldest) {
                    return size() > MAX_CACHE;
                }
            };

    private DiscreteCompositionTransFunc(TransFunc<S, ? super StmtAction, ? super P> dataTransFunc,
                                         DFA<prodS, String> compositionDfa,
                                         Alphabet<String> alphabet, TimedStepper stepper,
                                         UnknownSymbolHandler unknownSymbolHandler,
                                         prodS topState) {
        this.dataTransFunc = checkNotNull(dataTransFunc);
        this.compositionDfa = checkNotNull(compositionDfa);
        this.alphabet = checkNotNull(alphabet);
        this.stepper = checkNotNull(stepper);
        this.unknownSymbolHandler = checkNotNull(unknownSymbolHandler);
        this.topState = checkNotNull(topState);
    }

    public static <S extends State, prodS, P extends Prec> DiscreteCompositionTransFunc<S, prodS, P> create(
            TransFunc<S, ? super StmtAction, ? super P> dataTransFunc,
            DFA<prodS, String> compositionDfa,
            Alphabet<String> alphabet,
            TimedStepper stepper,
            UnknownSymbolHandler unknownSymbolHandler,
            prodS topState) {
        return new DiscreteCompositionTransFunc<>(dataTransFunc, compositionDfa, alphabet, stepper,
                unknownSymbolHandler, topState);
    }

    @Override
    public Collection<DiscreteCompositionState<S, prodS>> getSuccStates(
            DiscreteCompositionState<S, prodS> state, XtaAction action, P prec) {
        final XtaState<S> xtaState = state.getXtaState();
        checkArgument(xtaState.getLocs().equals(action.getSourceLocs()));

        final prodS nextDfaState = nextDfaState(state.getDfaState(), action);
        if (nextDfaState == null) {
            return Collections.emptyList();
        }

        List<Stmt> dataStmts = dataStmtCache.get(action);
        if (dataStmts == null) {
            dataStmts = dataStmtsOf(action);
            dataStmtCache.put(action, dataStmts);
        }

        final Collection<? extends S> nextDataStates =
                dataTransFunc.getSuccStates(xtaState.getState(), new DataAction(dataStmts), prec);

        final Collection<DiscreteCompositionState<S, prodS>> result = new ArrayList<>();
        for (final S nextDataState : nextDataStates) {
            result.add(DiscreteCompositionState.create(
                    XtaState.of(action.getTargetLocs(), nextDataState), nextDfaState));
        }
        return result;
    }

    private prodS nextDfaState(final prodS dfaState, final XtaAction action) {
        final String symbol = stepper.symbolOf(action);
        final prodS next;
        if (!alphabet.containsSymbol(symbol)) {
            unknownSymbolHandler.onUnknownSymbol(symbol, action);
            next = topState;
        } else {
            next = compositionDfa.getTransition(dfaState, symbol);
            if (next == null) {
                return null;
            }
        }
        return compositionDfa.isAccepting(next) ? next : null;
    }

    public static StmtAction dataActionOf(final XtaAction action) {
        return new DataAction(dataStmtsOf(action));
    }

    private static List<Stmt> dataStmtsOf(final XtaAction action) {
        final List<Stmt> result = new ArrayList<>();
        for (final Stmt stmt : action.getStmts()) {
            if (isDataRelevant(stmt)) {
                result.add(stmt);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isDataRelevant(final Stmt stmt) {
        if (stmt instanceof AssumeStmt) {
            return !mentionsClock(ExprUtils.getVars(((AssumeStmt) stmt).getCond()));
        }
        if (stmt instanceof AssignStmt<?>) {
            return !isClock(((AssignStmt<?>) stmt).getVarDecl());
        }
        if (stmt instanceof HavocStmt<?>) {
            return !isClock(((HavocStmt<?>) stmt).getVarDecl());
        }
        return true;
    }

    private static boolean mentionsClock(final Iterable<? extends VarDecl<?>> vars) {
        for (final VarDecl<?> var : vars) {
            if (isClock(var)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClock(final VarDecl<?> var) {
        return var.getType() instanceof RatType;
    }

    private static final class DataAction extends StmtAction {
        private final List<Stmt> stmts;

        private DataAction(final List<Stmt> stmts) {
            this.stmts = stmts;
        }

        @Override
        public List<Stmt> getStmts() {
            return stmts;
        }
    }
}
