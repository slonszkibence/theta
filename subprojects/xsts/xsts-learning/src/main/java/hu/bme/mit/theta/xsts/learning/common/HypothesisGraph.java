package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.common.visualization.EdgeAttributes;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.NodeAttributes;
import hu.bme.mit.theta.common.visualization.Shape;
import hu.bme.mit.theta.xsts.analysis.XstsAction;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;


public final class HypothesisGraph {

    private static final Color ACCEPTING_FILL = new Color(0xE8, 0xF4, 0xE8);
    private static final Color REJECTING_FILL = new Color(0xF6, 0xD8, 0xD8);
    private static final Color LINE = Color.BLACK;

    private HypothesisGraph() {
    }

    public static Graph create(final DFA<?, XstsAction> hypothesis,
                               final Alphabet<XstsAction> alphabet) {
        checkNotNull(hypothesis);
        checkNotNull(alphabet);
        return render(hypothesis, alphabet);
    }

    public static <S> Map<S, String> stateIds(final DFA<S, XstsAction> dfa,
                                              final Alphabet<XstsAction> alphabet) {
        final S initial = dfa.getInitialState();
        return initial == null ? Map.of() : reachableStateIds(dfa, alphabet, initial);
    }

    private static <S> Graph render(final DFA<S, XstsAction> dfa,
                                    final Alphabet<XstsAction> alphabet) {
        final Graph graph = new Graph("hypothesis",
                "Hypothesis H - " + dfa.size() + " states, " + alphabet.size() + " symbols");

        final S initial = dfa.getInitialState();
        if (initial == null) {
            return graph;
        }

        final Map<S, String> ids = reachableStateIds(dfa, alphabet, initial);

        for (final Map.Entry<S, String> entry : ids.entrySet()) {
            final S state = entry.getKey();
            final boolean accepting = dfa.isAccepting(state);
            final boolean sink = isRejectingSink(dfa, alphabet, state);

            graph.addNode(entry.getValue(), NodeAttributes.builder()
                    .label(nodeLabel(entry.getValue(), accepting, sink))
                    .shape(Shape.CIRCLE)
                    .peripheries(state.equals(initial) ? 2 : 1)
                    .fillColor(accepting ? ACCEPTING_FILL : REJECTING_FILL)
                    .lineColor(LINE)
                    .build());
        }

        for (final Map.Entry<S, String> entry : ids.entrySet()) {
            final S source = entry.getKey();
            if (isRejectingSink(dfa, alphabet, source)) {
                continue;
            }

            final Map<S, List<XstsAction>> bySuccessor = new LinkedHashMap<>();
            for (final XstsAction symbol : alphabet) {
                final S target = dfa.getTransition(source, symbol);
                if (target != null && ids.containsKey(target)) {
                    bySuccessor.computeIfAbsent(target, t -> new ArrayList<>()).add(symbol);
                }
            }

            bySuccessor.forEach((target, symbols) -> graph.addEdge(
                    entry.getValue(), ids.get(target), EdgeAttributes.builder()
                            .label(edgeLabel(symbols))
                            .color(LINE)
                            .build()));
        }

        return graph;
    }

    private static <S> Map<S, String> reachableStateIds(final DFA<S, XstsAction> dfa,
                                                        final Alphabet<XstsAction> alphabet,
                                                        final S initial) {
        final Map<S, String> ids = new LinkedHashMap<>();
        final List<S> queue = new ArrayList<>();
        ids.put(initial, "q0");
        queue.add(initial);

        for (int i = 0; i < queue.size(); i++) {
            final S state = queue.get(i);
            for (final XstsAction symbol : alphabet) {
                final S target = dfa.getTransition(state, symbol);
                if (target != null && !ids.containsKey(target)) {
                    ids.put(target, "q" + ids.size());
                    queue.add(target);
                }
            }
        }
        return ids;
    }

    private static <S> boolean isRejectingSink(final DFA<S, XstsAction> dfa,
                                               final Alphabet<XstsAction> alphabet,
                                               final S state) {
        if (dfa.isAccepting(state)) {
            return false;
        }
        for (final XstsAction symbol : alphabet) {
            final S target = dfa.getTransition(state, symbol);
            if (target != null && !target.equals(state)) {
                return false;
            }
        }
        return true;
    }

    private static String nodeLabel(final String id, final boolean accepting, final boolean sink) {
        if (sink) {
            return id + "\ntiming-infeasible";
        }
        return accepting ? id : id + "\n(rejecting)";
    }

    private static String edgeLabel(final List<XstsAction> symbols) {
        return symbols.stream().map(HypothesisGraph::symbolLabel)
                .collect(Collectors.joining("\n"));
    }

    public static String symbolLabel(final XstsAction clockAction) {
        if (clockAction.getStmts().isEmpty()) {
            return "eps";
        }
        return clockAction.getStmts().stream()
                .map(stmt -> stmt.toString().replace('"', '\''))
                .collect(Collectors.joining(", "));
    }
}
