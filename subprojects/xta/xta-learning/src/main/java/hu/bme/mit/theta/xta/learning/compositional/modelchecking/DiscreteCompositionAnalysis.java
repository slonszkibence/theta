package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscreteCompositionAnalysis<S extends State, prodS, P extends Prec>
        implements Analysis<DiscreteCompositionState<S, prodS>, XtaAction, P> {
    private final PartialOrd<DiscreteCompositionState<S, prodS>> partialOrd;
    private final InitFunc<DiscreteCompositionState<S, prodS>, P> initFunc;
    private final TransFunc<DiscreteCompositionState<S, prodS>, XtaAction, P> transFunc;

    private DiscreteCompositionAnalysis(
            PartialOrd<DiscreteCompositionState<S, prodS>> partialOrd,
            Alphabet<String> alphabet,
            DFA<prodS, String> compositionDfa,
            InitFunc<XtaState<S>, P> innerInitFunc,
            TransFunc<S, ? super StmtAction, ? super P> dataTransFunc,
            TimedStepper stepper,
            DiscreteCompositionTransFunc.UnknownSymbolHandler unknownSymbolHandler,
            prodS topState
    ) {
        checkNotNull(alphabet);
        checkNotNull(compositionDfa);
        checkNotNull(innerInitFunc);
        checkNotNull(dataTransFunc);
        this.partialOrd = checkNotNull(partialOrd);
        this.transFunc = DiscreteCompositionTransFunc.create(dataTransFunc, compositionDfa,
                alphabet, stepper, unknownSymbolHandler, topState);
        this.initFunc  = DiscreteCompositionInitFunc.create(innerInitFunc, compositionDfa);
    }

    public static <S extends State, prodS, P extends Prec> DiscreteCompositionAnalysis<S, prodS, P> create(
            PartialOrd<DiscreteCompositionState<S, prodS>> partialOrd,
            Alphabet<String> alphabet,
            DFA<prodS, String> compositionDfa,
            InitFunc<XtaState<S>, P> innerInitFunc,
            TransFunc<S, ? super StmtAction, ? super P> dataTransFunc,
            TimedStepper stepper,
            DiscreteCompositionTransFunc.UnknownSymbolHandler unknownSymbolHandler,
            prodS topState
    ) {
        return new DiscreteCompositionAnalysis<>(
                partialOrd, alphabet, compositionDfa, innerInitFunc, dataTransFunc,
                stepper, unknownSymbolHandler, topState);
    }

    @Override
    public PartialOrd<DiscreteCompositionState<S, prodS>> getPartialOrd() {
        return partialOrd;
    }

    @Override
    public InitFunc<DiscreteCompositionState<S, prodS>, P> getInitFunc() {
        return initFunc;
    }

    @Override
    public TransFunc<DiscreteCompositionState<S, prodS>, XtaAction, P> getTransFunc() {
        return transFunc;
    }
}
