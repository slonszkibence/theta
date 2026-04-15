package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.xta.analysis.XtaState;

import net.automatalib.automaton.fsa.DFA;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class XtaDfaInitFunc<S extends State, P extends Prec, DfaState> implements InitFunc<XtaDfaState<S, DfaState>, P> {
    private final InitFunc<XtaState<S>, P> xtaInitFunc;
    private final DFA<DfaState, String> dfa;

    private XtaDfaInitFunc(InitFunc<XtaState<S>, P> xtaInitFunc, DFA<DfaState, String> dfa) {
        this.xtaInitFunc = checkNotNull(xtaInitFunc);
        this.dfa = checkNotNull(dfa);
    }

    public static<S extends State, P extends Prec, DfaState> XtaDfaInitFunc<S, P, DfaState> create(
            InitFunc<XtaState<S>, P> xtaInitFunc,
            DFA<DfaState, String> dfa
    ) {
        return new XtaDfaInitFunc<>(xtaInitFunc, dfa);
    }

    @Override
    public Collection<XtaDfaState<S, DfaState>> getInitStates(P prec) {
        Collection<? extends XtaState<S>> xtaStates = xtaInitFunc.getInitStates(prec);
        DfaState dfaState = dfa.getInitialState();

        return xtaStates.stream()
                .map(xtaState -> XtaDfaState.create(xtaState, dfaState))
                .collect(Collectors.toList());
    }
}
