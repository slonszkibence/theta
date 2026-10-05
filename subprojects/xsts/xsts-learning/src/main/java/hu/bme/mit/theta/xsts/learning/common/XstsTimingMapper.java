package hu.bme.mit.theta.xsts.learning.common;

import de.learnlib.sul.SULMapper;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsTimingMapper<AI, AO, CO> implements SULMapper<AI, AO, XstsAction, CO> {
    private final Map<AI, XstsAction> clockActions;
    private final Function<CO, AO> outputFunction;

    private XstsTimingMapper(Map<AI, XstsAction> clockActions,
                             Function<CO, AO> outputFunction) {
        this.clockActions = clockActions;   // null means the identity translation
        this.outputFunction = checkNotNull(outputFunction);
    }

    public static <AI, AO, CO> XstsTimingMapper<AI, AO, CO> create(Map<AI, XstsAction> clockActions,
                                                            Function<CO, AO> outputFunction) {
        return new XstsTimingMapper<>(clockActions, outputFunction);
    }


    public static <AO, CO> XstsTimingMapper<XstsAction, AO, CO> identity(
            final Function<CO, AO> outputFunction) {
        return new XstsTimingMapper<>(null, outputFunction);
    }

    @Override
    public XstsAction mapInput(AI input) {
        return clockActions == null ? (XstsAction) input : clockActions.get(input);
    }

    @Override
    public AO mapOutput(CO concreteOutput) {
        return outputFunction.apply(concreteOutput);
    }

    public Set<AI> getAlphabet() {
        checkNotNull(clockActions, "The identity mapper has no symbol table");
        return clockActions.keySet();
    }
}
