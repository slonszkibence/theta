package hu.bme.mit.theta.xta.learning.compositional.common;

import hu.bme.mit.theta.analysis.zone.BoundFunc;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.core.clock.constr.AtomicConstr;
import hu.bme.mit.theta.core.clock.constr.ClockConstr;
import hu.bme.mit.theta.core.clock.constr.UnitConstr;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ZoneSemantics(
        Map<VarDecl<RatType>, Integer> ceilings,
        BoundFunc luBounds,
        boolean initialDelayAllowed,
        boolean normalize,
        List<ClockConstr> initialInvariants
) {
    public static ZoneSemantics of(final XtaSystem system) {
        return of(system, TimingPrec.all(system));
    }

    public static ZoneSemantics of(final XtaSystem system, final TimingPrec prec) {
        final List<ClockConstr> constrs = allClockConstrs(system, prec);
        final boolean diagonalFree = isDiagonalFree(constrs);
        return new ZoneSemantics(
                computeCeilings(system, prec, constrs),
                diagonalFree ? computeLuBounds(constrs) : null,
                initialDelayAllowed(system, prec),
                diagonalFree,
                computeInitialInvariants(system, prec));
    }

    public ZoneState initialZone() {
        final ZoneState.Builder builder = ZoneState.zero(ceilings.keySet()).transform();
        if (initialDelayAllowed) {
            builder.up();
        }
        for (final ClockConstr invariant : initialInvariants) {
            builder.and(invariant);
        }
        return builder.build();
    }

    private static List<ClockConstr> computeInitialInvariants(final XtaSystem system,
                                                              final TimingPrec prec) {
        final List<ClockConstr> result = new ArrayList<>();
        for (final XtaProcess process : system.getProcesses()) {
            if (prec.tracks(process)) {
                result.addAll(clockInvariantsOf(process.getInitLoc(), prec));
            }
        }
        return List.copyOf(result);
    }

    static List<ClockConstr> clockInvariantsOf(final XtaProcess.Loc loc, final TimingPrec prec) {
        final List<ClockConstr> result = new ArrayList<>();
        for (final Guard invariant : loc.getInvars()) {
            if (invariant.isClockGuard()) {
                prec.restrict(invariant.asClockGuard().getClockConstr()).ifPresent(result::add);
            }
        }
        return result;
    }

    public boolean exactInclusion() {
        return luBounds == null;
    }

    private static boolean isDiagonalFree(final List<ClockConstr> constrs) {
        for (final ClockConstr constr : constrs) {
            if (!isDiagonalFree(constr)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDiagonalFree(final ClockConstr constr) {
        if (constr instanceof UnitConstr) {
            return true;
        }
        if (constr instanceof AtomicConstr) {
            return false;
        }

        return constr.getVars().size() <= 1;
    }

    private static List<ClockConstr> allClockConstrs(final XtaSystem system,
                                                     final TimingPrec prec) {
        final List<ClockConstr> result = new ArrayList<>();
        for (final XtaProcess process : system.getProcesses()) {
            for (final XtaProcess.Edge edge : process.getEdges()) {
                for (final Guard guard : edge.getGuards()) {
                    if (guard.isClockGuard()) {
                        prec.restrict(guard.asClockGuard().getClockConstr())
                                .ifPresent(result::add);
                    }
                }
            }
            if (prec.tracks(process)) {
                for (final XtaProcess.Loc loc : process.getLocs()) {
                    result.addAll(clockInvariantsOf(loc, prec));
                }
            }
        }
        return result;
    }

    private static Map<VarDecl<RatType>, Integer> computeCeilings(final XtaSystem system,
                                                                  final TimingPrec prec,
                                                                  final List<ClockConstr> constrs) {
        final Map<VarDecl<RatType>, Integer> ceilings = new HashMap<>();
        for (final VarDecl<RatType> clock : system.getClockVars()) {
            if (prec.clocks().contains(clock)) {
                ceilings.put(clock, 0);
            }
        }
        for (final ClockConstr constr : constrs) {
            updateCeilings(ceilings, constr);
        }
        return ceilings;
    }

    private static void updateCeilings(final Map<VarDecl<RatType>, Integer> ceilings,
                                       final ClockConstr constr) {
        if (constr instanceof AtomicConstr atomic) {
            final int bound = atomic.getBound();
            for (final VarDecl<RatType> variable : constr.getVars()) {
                ceilings.merge(variable, bound, Math::max);
            }
            return;
        }
        if (constr instanceof hu.bme.mit.theta.core.clock.constr.AndConstr and) {
            for (final AtomicConstr conjunct : and.getConstrs()) {
                updateCeilings(ceilings, conjunct);
            }
        }

    }

    private static BoundFunc computeLuBounds(final List<ClockConstr> constrs) {
        final BoundFunc.Builder builder = BoundFunc.builder();
        for (final ClockConstr constr : constrs) {
            builder.add(constr);
        }
        return builder.build();
    }

    private static boolean initialDelayAllowed(final XtaSystem system, final TimingPrec prec) {
        for (final XtaProcess process : system.getProcesses()) {
            if (prec.tracks(process)
                    && process.getInitLoc().getKind() != XtaProcess.LocKind.NORMAL) {
                return false;
            }
        }
        return true;
    }
}
