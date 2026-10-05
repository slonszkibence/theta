package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.common.visualization.EdgeAttributes;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.NodeAttributes;
import hu.bme.mit.theta.common.visualization.Shape;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.learning.common.UntimedXsts.Flow;
import hu.bme.mit.theta.xsts.learning.common.UntimedXsts.Phase;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;


public final class UntimedXstsGraph {

    private UntimedXstsGraph() {
    }
    public static Graph create(final UntimedXsts.Result result, final boolean groupByDataAction) {
        checkNotNull(result);

        final Graph graph = new Graph("timeless_xsts",
                "Timeless XSTS (A) - flows: " + result.totalFlows());

        addNode(graph, Phase.INIT.name(), "pre-init", true);
        addNode(graph, Phase.ENV.name(), "env enabled", false);
        addNode(graph, Phase.TRAN.name(), "tran enabled", false);

        addFlows(graph, Phase.INIT.name(), Phase.ENV.name(), result.flowsOf(Phase.INIT), groupByDataAction);
        addFlows(graph, Phase.ENV.name(), Phase.TRAN.name(), result.flowsOf(Phase.ENV), groupByDataAction);
        addFlows(graph, Phase.TRAN.name(), Phase.ENV.name(), result.flowsOf(Phase.TRAN), groupByDataAction);

        return graph;
    }

    private static void addNode(final Graph graph, final String id, final String label,
                                final boolean initial) {
        graph.addNode(id, NodeAttributes.builder()
                .label(label)
                .shape(Shape.RECTANGLE)
                .peripheries(initial ? 2 : 1)
                .build());
    }

    private static void addFlows(final Graph graph, final String from, final String to,
                                 final List<Flow> flows, final boolean groupByDataAction) {
        if (!groupByDataAction) {
            for (final Flow flow : flows) {
                addEdge(graph, from, to, label(flow.dataAction()));
            }
            return;
        }

        final Map<XstsAction, Integer> counts = new LinkedHashMap<>();
        for (final Flow flow : flows) {
            counts.merge(flow.dataAction(), 1, Integer::sum);
        }
        counts.forEach((dataAction, count) -> addEdge(graph, from, to,
                count == 1 ? label(dataAction) : label(dataAction) + "\n[x" + count + "]"));
    }

    private static void addEdge(final Graph graph, final String from, final String to,
                                final String label) {
        graph.addEdge(from, to, EdgeAttributes.builder()
                .label(label)
                .color(Color.BLACK)
                .build());
    }

    private static String label(final XstsAction dataAction) {
        if (dataAction.getStmts().isEmpty()) {
            return "(no data effect)";
        }
        return dataAction.getStmts().stream()
                .map(stmt -> stmt.toString().replace('"', '\''))
                .collect(Collectors.joining("\n"));
    }
}
