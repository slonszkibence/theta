/*
 *  Copyright 2026 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.xta.analysis.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.zone.ClockBounds;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.op.ClockOp;
import hu.bme.mit.theta.core.clock.op.ClockOps;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Update;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaProcess.Edge;
import hu.bme.mit.theta.xta.XtaProcess.Loc;
import hu.bme.mit.theta.xta.XtaProcess.LocKind;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.compositional.XtaClockProjection.EdgeClock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class XtaTimedStepper {

    private static final int MAX_SLOTS = 8_000_000;
    private static final int MIN_CACHE_ENTRIES = 1024;

    private final XtaTimingPrec prec;
    private final ClockBounds bounds;
    private final List<VarDecl<ClockType>> clocks;
    private final boolean[] trackedProcess;
    private final Map<Loc, Integer> locToIndex;
    private final List<Loc> initialLocs;
    private final Map<Edge, EdgeClock> edgeClocks;
    private final Map<Loc, List<ClockConstr>> invariants;
    private final Map<XtaClockProjection, Integer> symbolIndex;
    private final Map<Config, List<Optional<Config>>> succCache;
    private int maxCacheEntries;

    private XtaTimedStepper(final XtaSystem system, final XtaTimingPrec prec) {
        this.prec = checkNotNull(prec);
        final List<XtaProcess> processes = system.getProcesses();
        this.trackedProcess = new boolean[processes.size()];
        this.locToIndex = new LinkedHashMap<>();
        final List<Loc> init = new ArrayList<>();
        for (int i = 0; i < processes.size(); i++) {
            trackedProcess[i] = prec.tracks(processes.get(i));
            for (final Loc loc : processes.get(i).getLocs()) {
                locToIndex.put(loc, i);
            }
            init.add(processes.get(i).getInitLoc());
        }
        this.initialLocs = List.copyOf(init);
        this.clocks = system.getClockVars().stream().filter(prec.getClocks()::contains).toList();
        this.bounds = ClockBounds.of(clocks, clockConstrs(system, prec));
        this.edgeClocks = new HashMap<>();
        this.invariants = new HashMap<>();
        this.symbolIndex = new HashMap<>();
        this.maxCacheEntries = MIN_CACHE_ENTRIES;
        this.succCache = new LinkedHashMap<>(MIN_CACHE_ENTRIES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<Config, List<Optional<Config>>> eldest) {
                return size() > maxCacheEntries;
            }
        };
    }

    public static XtaTimedStepper create(final XtaSystem system, final XtaTimingPrec prec) {
        return new XtaTimedStepper(checkNotNull(system), prec);
    }

    public XtaTimingPrec getPrec() {
        return prec;
    }

    public ClockBounds getBounds() {
        return bounds;
    }

    public XtaClockProjection project(final XtaAction action) {
        final List<EdgeClock> projection = new ArrayList<>();
        for (final Edge edge : edgesOf(action)) {
            projection.add(edgeClocks.computeIfAbsent(edge, this::edgeClockOf));
        }
        if (projection.size() > 1) {
            projection.sort(Comparator.comparing(EdgeClock::toString));
        }
        return XtaClockProjection.of(projection);
    }

    public Config initial() {
        final ZoneState.Builder builder = ZoneState.zero(clocks).transform();
        if (delayAllowed(initialLocs)) {
            builder.up();
        }
        addInvariants(builder, initialLocs);
        return new Config(initialLocs, builder.build());
    }

    public Optional<Config> step(final Config from, final XtaClockProjection symbol) {
        final int index = symbolIndex.computeIfAbsent(symbol, s -> {
            maxCacheEntries = Math.max(MIN_CACHE_ENTRIES, MAX_SLOTS / (symbolIndex.size() + 1));
            return symbolIndex.size();
        });
        final List<Optional<Config>> succs = succCache.computeIfAbsent(from, config -> new ArrayList<>());
        while (succs.size() <= index) {
            succs.add(null);
        }
        Optional<Config> succ = succs.get(index);
        if (succ == null) {
            succ = compute(from, symbol).filter(config -> !config.isBottom());
            succs.set(index, succ);
        }
        return succ;
    }

    private Optional<Config> compute(final Config from, final XtaClockProjection symbol) {
        final List<Loc> nextLocs = new ArrayList<>(from.getLocs());
        for (final EdgeClock edge : symbol.getEdgeClocks()) {
            if (edge.isTracked()) {
                final Loc source = edge.source().get();
                final int index = locToIndex.get(source);
                if (!from.getLocs().get(index).equals(source)) {
                    return Optional.empty();
                }
                nextLocs.set(index, edge.target().get());
            }
        }

        final ZoneState.Builder builder = from.getZone().transform();
        addInvariants(builder, from.getLocs());
        for (final EdgeClock edge : symbol.getEdgeClocks()) {
            edge.guards().forEach(builder::and);
        }
        for (final EdgeClock edge : symbol.getEdgeClocks()) {
            edge.updates().forEach(builder::execute);
        }
        addInvariants(builder, nextLocs);
        if (delayAllowed(nextLocs)) {
            builder.up();
        }
        return Optional.of(new Config(nextLocs, bounds.normalize(builder.build())));
    }

    private void addInvariants(final ZoneState.Builder builder, final List<Loc> locs) {
        for (int i = 0; i < locs.size(); i++) {
            if (trackedProcess[i]) {
                invariants.computeIfAbsent(locs.get(i), loc -> clockInvariantsOf(loc, prec)).forEach(builder::and);
            }
        }
    }

    private boolean delayAllowed(final List<Loc> locs) {
        for (int i = 0; i < locs.size(); i++) {
            if (trackedProcess[i] && locs.get(i).getKind() != LocKind.NORMAL) {
                return false;
            }
        }
        return true;
    }

    private EdgeClock edgeClockOf(final Edge edge) {
        final List<ClockConstr> guards = new ArrayList<>();
        for (final Guard guard : edge.getGuards()) {
            if (guard.isClockGuard()) {
                prec.restrict(guard.asClockGuard().getClockConstr()).ifPresent(guards::add);
            }
        }
        guards.sort(Comparator.comparing(Object::toString));

        final List<ClockOp> updates = new ArrayList<>();
        for (final Update update : edge.getUpdates()) {
            if (update.isClockUpdate()) {
                prec.restrict(ClockOps.fromStmt(update.asClockUpdate().toStmt())).ifPresent(updates::add);
            }
        }

        final boolean tracked = trackedProcess[locToIndex.get(edge.getSource())];
        return new EdgeClock(
                tracked ? Optional.of(edge.getSource()) : Optional.empty(),
                tracked ? Optional.of(edge.getTarget()) : Optional.empty(),
                guards,
                updates);
    }

    private static List<ClockConstr> clockInvariantsOf(final Loc loc, final XtaTimingPrec prec) {
        final List<ClockConstr> result = new ArrayList<>();
        for (final Guard invariant : loc.getInvars()) {
            if (invariant.isClockGuard()) {
                prec.restrict(invariant.asClockGuard().getClockConstr()).ifPresent(result::add);
            }
        }
        return List.copyOf(result);
    }

    private static List<ClockConstr> clockConstrs(final XtaSystem system, final XtaTimingPrec prec) {
        final List<ClockConstr> result = new ArrayList<>();
        for (final XtaProcess process : system.getProcesses()) {
            for (final Edge edge : process.getEdges()) {
                for (final Guard guard : edge.getGuards()) {
                    if (guard.isClockGuard()) {
                        prec.restrict(guard.asClockGuard().getClockConstr()).ifPresent(result::add);
                    }
                }
            }
            if (prec.tracks(process)) {
                for (final Loc loc : process.getLocs()) {
                    result.addAll(clockInvariantsOf(loc, prec));
                }
            }
        }
        return result;
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
            return List.copyOf(edges);
        }
        throw new AssertionError(String.format("Unknown action kind: %s", action.getClass().getSimpleName()));
    }

    public static final class Config implements State {

        private final List<Loc> locs;
        private final ZoneState zone;
        private final int hashCode;

        private Config(final List<Loc> locs, final ZoneState zone) {
            this.locs = List.copyOf(checkNotNull(locs));
            this.zone = checkNotNull(zone);
            this.hashCode = 31 * this.locs.hashCode() + zone.hashCode();
        }

        public List<Loc> getLocs() {
            return locs;
        }

        public ZoneState getZone() {
            return zone;
        }

        @Override
        public boolean isBottom() {
            return zone.isBottom();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof Config that
                    && hashCode == that.hashCode
                    && locs.equals(that.locs)
                    && zone.equals(that.zone);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return Utils.lispStringBuilder(getClass().getSimpleName()).add(locs).add(zone).toString();
        }
    }
}
