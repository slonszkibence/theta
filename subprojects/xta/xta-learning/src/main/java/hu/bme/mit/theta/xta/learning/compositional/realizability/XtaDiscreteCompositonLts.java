package hu.bme.mit.theta.xta.learning.compositional.realizability;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.expl.ExplState;
 import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;

import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import net.automatalib.automaton.fsa.DFA;

import java.util.ArrayList;
import java.util.Collection;



public class XtaDiscreteCompositonLts <S> implements LTS<CompositionState<S>, XtaAction> {
    private final XtaSystem xtaSystem;
    private final DFA<S, String> hypothesis;

    private XtaDiscreteCompositonLts(XtaSystem xtaSystem,  DFA<S, String> hypothesis) {
        this.xtaSystem = checkNotNull(xtaSystem);
        this.hypothesis = checkNotNull(hypothesis);
    }

    public static<S> XtaDiscreteCompositonLts<S> create(XtaSystem xtaSystem, DFA<S, String> hypothesis) {
        return new XtaDiscreteCompositonLts<>(xtaSystem, hypothesis);
    }

    @Override
    public Collection<XtaAction> getEnabledActionsFor(final CompositionState<S> compositionState) {
        final Collection<XtaAction> validDiscreteActions = new ArrayList<>();

        XtaState<ExplState> xtaState = compositionState.getXtaState();
        S dfaState = compositionState.getDfaState();

        for (XtaProcess xtaProcess : xtaSystem.getProcesses()) {
            XtaProcess.Loc currentLoc = xtaState.getLocs().get(xtaSystem.getProcesses().indexOf(xtaProcess));

            for (XtaProcess.Edge edge : currentLoc.getOutEdges()) {
                boolean isDiscrete = true;
                for (Guard guard : edge.getGuards()) {
                    if (guard.isDataGuard() && !eval(guard.asDataGuard(), xtaState.getState())) {
                        isDiscrete = false;
                        break;
                    }
                }

                if (!isDiscrete) {
                    continue;
                }
                String symbol = XtaTimingMapper.generateSymbolForEdge(edge);
                S nextDfaState = hypothesis.getTransition(dfaState, symbol);

                if (nextDfaState != null) {
                    validDiscreteActions.add(XtaAction.basic(xtaSystem, xtaState.getLocs(), edge));
                }
            }
        }

        return validDiscreteActions;
    }

    private boolean eval(Guard guard, ExplState state) {
        Expr<BoolType> guardExpr = guard.toExpr();
        Expr<BoolType> simplifiedExpr = guardExpr.eval(state);
        return simplifiedExpr.equals(BoolExprs.True());
    }
}
