package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.op.ClockOp;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Update;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaProcess.Edge;
import hu.bme.mit.theta.xta.XtaProcess.Loc;
import hu.bme.mit.theta.xta.XtaProcess.LocKind;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TimedStepper {
    private static final int MAX_SLOTS = 8_000_000;

    private static final Config DISABLED = new Config(List.of(), null);

    private final ZoneSemantics semantics;
    private final Map<Loc, Integer> locToIndex = new LinkedHashMap<>();
    private final Map<Edge, EdgeClock> edgeClocks = new HashMap<>();

    private final Map<String, List<EdgeClock>> symbolToProjection = new LinkedHashMap<>();
    private final Map<String, Integer> symbolIndex = new LinkedHashMap<>();
    private final GrowingMapAlphabet<String> alphabet = new GrowingMapAlphabet<>();

    private final List<Loc> initialLocs;
    private final Map<Config, Config[]> succCache;
    private int maxCacheEntries = 1024;

    private record EdgeClock(Loc source, Loc target, List<ClockConstr> guards,
                             List<ClockOp> updates, String label) {}

    public static final class Config {
        private final List<Loc> locs;
        private final ZoneState zone;
        private final int hashCode;

        private Config(final List<Loc> locs, final ZoneState zone) {
            this.locs = locs;
            this.zone = zone;
            this.hashCode = 31 * locs.hashCode() + (zone == null ? 0 : zone.hashCode());
        }

        public List<Loc> locs() {
            return locs;
        }

        public ZoneState zone() {
            return zone;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Config)) {
                return false;
            }
            final Config that = (Config) obj;
            if (hashCode != that.hashCode) {
                return false;
            }
            return locs.equals(that.locs)
                    && (zone == null ? that.zone == null : zone.equals(that.zone));
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "Config(" + locs + ", " + zone + ")";
        }
    }

    private TimedStepper(final XtaSystem system, final ZoneSemantics semantics) {
        this.semantics = checkNotNull(semantics);

        final List<XtaProcess> processes = List.copyOf(system.getProcesses());
        for (int i = 0; i < processes.size(); i++) {
            for (final Loc loc : processes.get(i).getLocs()) {
                locToIndex.put(loc, i);
            }
        }

        final List<Loc> init = new ArrayList<>();
        for (final XtaProcess process : processes) {
            init.add(process.getInitLoc());
        }
        this.initialLocs = List.copyOf(init);

        this.succCache = new LinkedHashMap<>(1024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<Config, Config[]> eldest) {
                return size() > maxCacheEntries;
            }
        };
    }

    public static TimedStepper of(final XtaSystem system, final ZoneSemantics semantics) {
        return new TimedStepper(system, semantics);
    }

    public Alphabet<String> alphabet() {
        return alphabet;
    }

    public String symbolOf(final XtaAction action) {
        return labelOf(projectionOf(edgesOf(action)));
    }

    public String register(final XtaAction action) {
        return register(edgesOf(action));
    }

    public String register(final List<Edge> edges) {
        final List<EdgeClock> projection = projectionOf(edges);
        final String symbol = labelOf(projection);
        if (!symbolToProjection.containsKey(symbol)) {
            symbolToProjection.put(symbol, projection);
            symbolIndex.put(symbol, symbolIndex.size());
            alphabet.addSymbol(symbol);
            maxCacheEntries = Math.max(1024, MAX_SLOTS / symbolIndex.size());

            succCache.clear();
        }
        return symbol;
    }

    public Config initial() {
        return new Config(initialLocs, semantics.initialZone());
    }

    public Config step(final Config from, final String symbol) {
        final Integer index = symbolIndex.get(symbol);
        if (index == null) {
            return null;
        }

        Config[] succs = succCache.get(from);
        if (succs == null) {
            succs = new Config[symbolIndex.size()];
            succCache.put(from, succs);
        }

        final Config cached = succs[index];
        if (cached != null) {
            return cached == DISABLED ? null : cached;
        }

        final Config computed = compute(from, symbolToProjection.get(symbol));
        if (computed == null || computed.zone().isBottom()) {
            succs[index] = DISABLED;
            return null;
        }
        succs[index] = computed;
        return computed;
    }

    private Config compute(final Config from, final List<EdgeClock> projection) {
        for (final EdgeClock edge : projection) {
            final Integer index = locToIndex.get(edge.source());
            if (index == null || !from.locs().get(index).equals(edge.source())) {
                return null;
            }
        }

        final List<Loc> nextLocs = new ArrayList<>(from.locs());
        for (final EdgeClock edge : projection) {
            nextLocs.set(locToIndex.get(edge.source()), edge.target());
        }

        final ZoneState.Builder builder = from.zone().transform();
        addInvariants(builder, from.locs());
        for (final EdgeClock edge : projection) {
            for (final ClockConstr guard : edge.guards()) {
                builder.and(guard);
            }
        }
        for (final EdgeClock edge : projection) {
            for (final ClockOp update : edge.updates()) {
                builder.execute(update);
            }
        }
        addInvariants(builder, nextLocs);
        if (nextLocs.stream().allMatch(loc -> loc.getKind() == LocKind.NORMAL)) {
            builder.up();
        }
        if (semantics.normalize()) {
            builder.norm(semantics.ceilings());
        }
        return new Config(List.copyOf(nextLocs), builder.build());
    }

    private void addInvariants(final ZoneState.Builder builder, final List<Loc> locs) {
        for (final Loc loc : locs) {
            for (final Guard invariant : loc.getInvars()) {
                if (invariant.isClockGuard()) {
                    builder.and(invariant.asClockGuard().getClockConstr());
                }
            }
        }
    }

    private List<EdgeClock> projectionOf(final List<Edge> edges) {
        if (edges.size() == 1) {
            return List.of(edgeClockOf(edges.get(0)));
        }
        final List<EdgeClock> projection = new ArrayList<>(edges.size());
        for (final Edge edge : edges) {
            projection.add(edgeClockOf(edge));
        }
        projection.sort((left, right) -> left.label().compareTo(right.label()));
        return List.copyOf(projection);
    }

    private static String labelOf(final List<EdgeClock> projection) {
        if (projection.size() == 1) {
            return projection.get(0).label();
        }
        final StringBuilder builder = new StringBuilder();
        for (final EdgeClock edge : projection) {
            if (builder.length() > 0) {
                builder.append('+');
            }
            builder.append(edge.label());
        }
        return builder.toString();
    }

    private EdgeClock edgeClockOf(final Edge edge) {
        EdgeClock cached = edgeClocks.get(edge);
        if (cached != null) {
            return cached;
        }

        final List<ClockConstr> guards = new ArrayList<>();
        for (final Guard guard : edge.getGuards()) {
            if (guard.isClockGuard()) {
                guards.add(guard.asClockGuard().getClockConstr());
            }
        }
        guards.sort((left, right) -> left.toString().compareTo(right.toString()));

        final List<ClockOp> updates = new ArrayList<>();
        for (final Update update : edge.getUpdates()) {
            if (update.isClockUpdate()) {
                updates.add(update.asClockUpdate().getClockOp());
            }
        }

        final String label = edge.getSource().getName() + "->" + edge.getTarget().getName()
                + "g" + guards + "u" + updates;
        cached = new EdgeClock(edge.getSource(), edge.getTarget(), List.copyOf(guards),
                List.copyOf(updates), label);
        edgeClocks.put(edge, cached);
        return cached;
    }

    public static List<Edge> edgesOf(final XtaAction action) {
        if (action.isBasic()) {
            return List.of(action.asBasic().getEdge());
        }
        if (action.isBinary()) {
            return List.of(action.asBinary().getEmitEdge(), action.asBinary().getRecvEdge());
        }
        if (action.isBroadcast()) {
            final List<Edge> edges = new ArrayList<>();
            edges.add(action.asBroadcast().getEmitEdge());
            edges.addAll(action.asBroadcast().getRecvEdges());
            return Collections.unmodifiableList(edges);
        }
        throw new AssertionError("Unknown action kind: " + action.getClass().getSimpleName());
    }
}
