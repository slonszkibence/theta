package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionAnalysis<S, P extends Prec>
        implements Analysis<DiscreteCompositionState<S>, XtaAction, P> {
    private final PartialOrd<DiscreteCompositionState<S>> partialOrd;
    private final InitFunc<DiscreteCompositionState<S>, P> initFunc;
    private final TransFunc<DiscreteCompositionState<S>, XtaAction, P> transFunc;

    private DiscreteCompositionAnalysis(PartialOrd<DiscreteCompositionState<S>> partialOrd,
                                        Alphabet<String> alphabet,
                                        DFA<S, String> productDFA,
                                        InitFunc<XtaState<ExplState>, P> innerInitFunc,
                                        TransFunc<XtaState<ExplState>, XtaAction, P> innerTransFunc) {

        checkNotNull(alphabet);
        checkNotNull(productDFA);
        checkNotNull(innerInitFunc);
        checkNotNull(innerTransFunc);
        this.partialOrd = checkNotNull(partialOrd);
        this.transFunc = DiscreteCompositionTransFunc.create(innerTransFunc, productDFA, alphabet);
        this.initFunc = DiscreteCompositionInitFunc.create(innerInitFunc, productDFA);
    }

    public static <S extends State, P extends Prec> DiscreteCompositionAnalysis<S, P>
    create(PartialOrd<DiscreteCompositionState<S>> partialOrd,
           Alphabet<String> alphabet,
           DFA<S, String> productDFA,
           InitFunc<XtaState<ExplState>, P> innerInitFunc,
           TransFunc<XtaState<ExplState>, XtaAction, P> innerTransFunc) {

        return new DiscreteCompositionAnalysis<>(partialOrd, alphabet, productDFA, innerInitFunc, innerTransFunc);
    }

    @Override
    public PartialOrd<DiscreteCompositionState<S>> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<DiscreteCompositionState<S>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<DiscreteCompositionState<S>, XtaAction, P> getTransFunc() {
        return transFunc;
    }
}
