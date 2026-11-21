package hu.bme.mit.theta.xsts.learning.modelchecking;


import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import java.util.function.Predicate;

public class XstsCompositionCheckerFactory {
    private XstsCompositionCheckerFactory() {}

    public static <S extends ExprState, prodS, P extends Prec> SafetyChecker<XstsCompositionState<S, prodS>, XstsAction, P> create(
            ModelCheckingStrategy<S, prodS, P> modelCheckingStrategy,
            SearchStrategy searchStrategy,
            XstsCompositionAnalysis<S, prodS, P> analysis,
            LTS<XstsCompositionState<S, prodS>, XstsAction> lts,
            Predicate<XstsCompositionState<S, prodS>> targetPred,
            Logger logger
    ) {
        Abstractor<XstsCompositionState<S, prodS>, XstsAction, P> abstractor =
                createAbstractor(searchStrategy, analysis, lts, targetPred, logger);

        return modelCheckingStrategy.build(abstractor);
    }

    private static <S extends ExprState, prodS, P extends Prec> Abstractor<XstsCompositionState<S, prodS>, XstsAction, P> createAbstractor(
            SearchStrategy searchStrategy,
            XstsCompositionAnalysis<S, prodS, P> analysis,
            LTS<XstsCompositionState<S, prodS>, XstsAction> lts,
            Predicate<XstsCompositionState<S, prodS>> targetPred,
            Logger logger
    ) {
        ArgBuilder<XstsCompositionState<S, prodS>, XstsAction, P> argBuilder =
                ArgBuilder.create(lts, analysis, targetPred, true);

        return BasicAbstractor.builder(argBuilder)
                .waitlist(searchStrategy.createWaitlist())
                .projection(XstsCompositionState::getDfaState)
                .logger(logger)
                .build();
    }
}
