package hu.bme.mit.theta.xta.learning.compositional.dfa;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.AbstractorResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.LifoWaitlist;
import hu.bme.mit.theta.analysis.waitlist.RandomWaitlist;
import hu.bme.mit.theta.analysis.waitlist.Waitlist;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import java.util.function.Predicate;

public class XtaDfaCheckerFactory {

    private XtaDfaCheckerFactory() {}

    public static<S extends State, P extends Prec, DfaState> SafetyChecker<XtaDfaState<S, DfaState>, XtaAction, P> create(
            XtaDfaCheckerStrategy strategy,
            Analysis<XtaDfaState<S, DfaState>, XtaAction, P> analysis,
            LTS<XtaDfaState<S, DfaState>, XtaAction> lts,
            Predicate<XtaDfaState<S, DfaState>> targetPred
    ) {
        Waitlist<ArgNode<XtaDfaState<S, DfaState>, XtaAction>> waitlist = switch (strategy) {
            case RANDOM -> RandomWaitlist.create();
            case BFS -> FifoWaitlist.create();
            case DFS -> LifoWaitlist.create();
            default -> throw new IllegalArgumentException("Unsupported strategy");
        };

        ArgBuilder<XtaDfaState<S, DfaState>, XtaAction, P> argBuilder =
                ArgBuilder.create(lts, analysis, targetPred);

        Abstractor<XtaDfaState<S, DfaState>, XtaAction, P> abstractor = BasicAbstractor
                .builder(argBuilder)
                .waitlist(waitlist)
                .projection(state -> state)
                .build();

        return prec -> {
            ARG<XtaDfaState<S, DfaState>, XtaAction> arg = abstractor.createArg();
            AbstractorResult result = abstractor.check(arg, prec);

            if (result.isSafe()) {
                return SafetyResult.safe(arg);
            }
            else {
                ArgTrace<XtaDfaState<S, DfaState>, XtaAction> cex = arg.getCexs().findFirst().get();
                return SafetyResult.unsafe(cex.toTrace(), arg);
            }
        };
    }
}
