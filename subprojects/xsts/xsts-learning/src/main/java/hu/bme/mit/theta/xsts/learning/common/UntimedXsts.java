package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStateValuationExtractor;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.timed.ControlFlowSplitXstsLts;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class UntimedXsts {

    private UntimedXsts() {
    }

    public record Flow(XstsAction action, XstsAction dataAction, XstsAction clockAction) {

        public boolean isTimingRelevant() {
            return !clockAction.getStmts().isEmpty();
        }
    }

    public enum Phase {
        INIT(XstsState.of(ExplState.top(), false, false)),
        ENV(XstsState.of(ExplState.top(), false, true)),
        TRAN(XstsState.of(ExplState.top(), true, true));

        private final XstsState<ExplState> probe;

        Phase(final XstsState<ExplState> probe) {
            this.probe = probe;
        }
    }

    public record Result(Map<Phase, List<Flow>> flows, Alphabet<XstsAction> alphabet) {

        public List<Flow> flowsOf(final Phase phase) {
            return flows.getOrDefault(phase, List.of());
        }

        public int totalFlows() {
            return flows.values().stream().mapToInt(List::size).sum();
        }
    }

    public static Result extract(final XSTS xsts, final Solver solver) {
        return extract(xsts, solver, true);
    }

    public static Result extract(final XSTS xsts, final Solver solver, final boolean feasibleOnly) {
        checkNotNull(xsts);
        checkNotNull(solver);

        final TimedXstsActionProjections projections = TimedXstsActionProjections.create();
        final ControlFlowSplitXstsLts<ExplState> lts = ControlFlowSplitXstsLts.create(
                xsts, solver, ExplStateValuationExtractor.getInstance());

        final Map<Phase, List<Flow>> flows = new EnumMap<>(Phase.class);
        final Set<XstsAction> alphabet = new LinkedHashSet<>();

        for (final Phase phase : Phase.values()) {
            final List<Flow> phaseFlows = new ArrayList<>();
            lts.reset();
            lts.splitControlFlows(phase.probe, true);

            for (Optional<XstsAction> next = lts.getNextEnabledAction();
                 next.isPresent();
                 next = lts.getNextEnabledAction()) {
                final XstsAction action = next.get();
                final Flow flow = new Flow(action,
                        projections.dataProjection(action), projections.clockProjection(action));
                phaseFlows.add(flow);
                if (flow.isTimingRelevant()) {
                    alphabet.add(flow.clockAction());
                }
            }
            flows.put(phase, List.copyOf(phaseFlows));
        }
        
        lts.reset();

        return new Result(Map.copyOf(flows), Alphabets.fromCollection(alphabet));
    }
}
