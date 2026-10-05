package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.bmc.BmcChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

public interface ModelCheckingStrategy <S extends ExprState, prodS, P extends Prec> {
        SafetyChecker<XstsCompositionState<S, prodS>, XstsAction, P> build(
                Abstractor<XstsCompositionState<S, prodS>, XstsAction, P> abstractor
        );

    static <S extends ExprState, prodS, P extends Prec> ModelCheckingStrategy<S, prodS, P> cegar(
            Refiner<XstsCompositionState<S, prodS>, XstsAction, P> refiner
    ) {
        return cegar(refiner, NullLogger.getInstance());
    }

    static <S extends ExprState, prodS, P extends Prec> ModelCheckingStrategy<S, prodS, P> cegar(
            Refiner<XstsCompositionState<S, prodS>, XstsAction, P> refiner,
            Logger logger
    ) {
        return abstractor -> CegarChecker.create(abstractor, refiner, logger);
    }
    /*static <S extends ExprState, prodS, P extends  Prec> ModelCheckingStrategy<S, prodS, P> bmc(

    ) {
        return abstractor -> BmcChecker.create(

        );
    }*/
}
