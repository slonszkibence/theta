package hu.bme.mit.theta.xsts.learning.modelchecking;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class XstsCompositionTraces {

    private XstsCompositionTraces() {
    }

    public static Optional<Trace<XstsState<?>, XstsAction>> projectToData(final Trace<?, ?> trace) {
        if (trace.getStates().isEmpty()
                || !(trace.getState(0) instanceof XstsCompositionState<?, ?>)) {
            return Optional.empty();
        }

        final TimedXstsActionProjections projections = TimedXstsActionProjections.create();

        final List<XstsState<?>> states = trace.getStates().stream()
                .map(state -> (XstsState<?>) ((XstsCompositionState<?, ?>) state).getXstsState())
                .collect(Collectors.toList());
        final List<XstsAction> actions = trace.getActions().stream()
                .map(XstsAction.class::cast)
                .map(projections::dataProjection)
                .toList();

        return Optional.of(Trace.of(states, actions));
    }
}
