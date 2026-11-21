package hu.bme.mit.theta.xsts.learning.common;

import de.learnlib.sul.SULMapper;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsTimingMapper<AI, AO, CO> implements SULMapper<AI, AO, XstsAction, CO> {
    private final Function<CO, AO> outputFunction;

    private XstsTimingMapper(Function<CO, AO> outputFunction) {
        this.outputFunction = checkNotNull(outputFunction);
    }

    public static <AO, CO> XstsTimingMapper<XstsAction, AO, CO> identity(
            final Function<CO, AO> outputFunction) {
        return new XstsTimingMapper<>(outputFunction);
    }

    @Override
    public XstsAction mapInput(AI input) {
        return (XstsAction) input;
    }

    @Override
    public AO mapOutput(CO concreteOutput) {
        return outputFunction.apply(concreteOutput);
    }
}
