package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.common.visualization.EdgeAttributes;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.NodeAttributes;
import hu.bme.mit.theta.common.visualization.Shape;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.learning.common.UntimedXsts.Flow;
import hu.bme.mit.theta.xsts.learning.common.UntimedXsts.Phase;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TxstsGraph {

    private static final Color TIMED_EDGE = new Color(0xC0, 0x39, 0x2B);
    private static final Color DATA_EDGE = Color.BLACK;

    private TxstsGraph() {
    }

    public static Graph create(final UntimedXsts.Result result) {
        checkNotNull(result);

        final Graph graph = new Graph("txsts",
                "Original TXSTS (T) - flows: " + result.totalFlows());

        addNode(graph, Phase.INIT.name(), "pre-init", true);
        addNode(graph, Phase.ENV.name(), "env enabled", false);
        addNode(graph, Phase.TRAN.name(), "tran enabled", false);

        addFlows(graph, Phase.INIT.name(), Phase.ENV.name(), result.flowsOf(Phase.INIT));
        addFlows(graph, Phase.ENV.name(), Phase.TRAN.name(), result.flowsOf(Phase.ENV));
        addFlows(graph, Phase.TRAN.name(), Phase.ENV.name(), result.flowsOf(Phase.TRAN));

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
                                 final List<Flow> flows) {
        for (final Flow flow : flows) {
            graph.addEdge(from, to, EdgeAttributes.builder()
                    .label(label(flow))
                    .color(flow.isTimingRelevant() ? TIMED_EDGE : DATA_EDGE)
                    .build());
        }
    }

    private static String label(final Flow flow) {
        final List<Stmt> stmts = flatten(flow.action());
        if (stmts.isEmpty()) {
            return "(empty)";
        }
        final List<Stmt> clockStmts = flow.clockAction().getStmts();
        return stmts.stream()
                .map(stmt -> (clockStmts.contains(stmt) ? "[clock] " : "") + text(stmt))
                .collect(Collectors.joining("\n"));
    }

    private static String text(final Stmt stmt) {
        return stmt.toString().replace('"', '\'');
    }

    private static List<Stmt> flatten(final XstsAction action) {
        final List<Stmt> flat = new ArrayList<>();
        action.getStmts().forEach(stmt -> flattenInto(stmt, flat));
        return flat;
    }

    private static void flattenInto(final Stmt stmt, final List<Stmt> flat) {
        if (stmt instanceof SequenceStmt sequence) {
            sequence.getStmts().forEach(subStmt -> flattenInto(subStmt, flat));
        } else if (!(stmt instanceof SkipStmt)) {
            flat.add(stmt);
        }
    }
}
