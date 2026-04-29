package hu.bme.mit.theta.xta.learning.compositional.realizability;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.AbstractorResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.LifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.RandomWaitlist;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.function.Predicate;

public class DiscreteCompositionCheckerFactory {
    private DiscreteCompositionCheckerFactory() {}


    public static <S, P extends Prec> SafetyChecker<DiscreteCompositionState<S>, XtaAction, P> create(
            DiscreteCompositionStrategy strategy,
            DiscreteCompositionAnalysis<S, P> analysis,
            LTS<DiscreteCompositionState<S>, XtaAction> lts,
            Predicate<DiscreteCompositionState<S>> targetPred
    ) {
        Waitlist<ArgNode<DiscreteCompositionState<S>, XtaAction>> waitlist = switch (strategy) {
            case RANDOM -> RandomWaitlist.create();
            case BFS -> FifoWaitlist.create();
            case DFS -> LifoWaitlist.create();
            default -> throw new IllegalArgumentException("Unsupported strategy");
        };

        ArgBuilder<DiscreteCompositionState<S>, XtaAction, P> argBuilder =
                ArgBuilder.create(lts, analysis, targetPred, true);

        Abstractor<DiscreteCompositionState<S>, XtaAction, P> abstractor = BasicAbstractor
                .builder(argBuilder)
                .waitlist(waitlist)
                .projection(state -> state)
                .build();

        return prec -> {
            ARG<DiscreteCompositionState<S>, XtaAction> arg = abstractor.createArg();
            AbstractorResult result = abstractor.check(arg, prec);

            if (result.isSafe()) {
                return SafetyResult.safe(arg);
            }
            else {
                ArgTrace<DiscreteCompositionState<S>, XtaAction> cex = arg.getCexs().findFirst().get();
                return SafetyResult.unsafe(cex.toTrace(), arg);
            }
        };
    }

    public static <S> SafetyChecker<DiscreteCompositionState<S>, XtaAction, ExplPrec> createCegar(
            DiscreteCompositionStrategy strategy,
            DiscreteCompositionAnalysis<S, ExplPrec> analysis,
            LTS<DiscreteCompositionState<S>, XtaAction> lts,
            Predicate<DiscreteCompositionState<S>> targetPred,
            Refiner<DiscreteCompositionState<S>, XtaAction, ExplPrec> refiner
    ) {
        Waitlist<ArgNode<DiscreteCompositionState<S>, XtaAction>> waitlist = switch (strategy) {
            case RANDOM -> RandomWaitlist.create();
            case BFS, CEGAR_CHECKER -> FifoWaitlist.create();
            case DFS -> LifoWaitlist.create();
        };

        ArgBuilder<DiscreteCompositionState<S>, XtaAction, ExplPrec> argBuilder =
                ArgBuilder.create(lts, analysis, targetPred, true);

        Abstractor<DiscreteCompositionState<S>, XtaAction, ExplPrec> abstractor = BasicAbstractor
                .builder(argBuilder)
                .waitlist(waitlist)
                .projection(state -> state)
                .build();

        return CegarChecker.create(abstractor, refiner);
    }
}
