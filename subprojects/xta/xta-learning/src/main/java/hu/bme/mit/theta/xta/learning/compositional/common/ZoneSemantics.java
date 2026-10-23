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
        List<Guard.ClockGuard> initialInvariants
) {
    public static ZoneSemantics of(final XtaSystem system) {
        final boolean diagonalFree = isDiagonalFree(system);
        return new ZoneSemantics(
                computeCeilings(system),
                diagonalFree ? computeLuBounds(system) : null,
                initialDelayAllowed(system),
                diagonalFree,
                computeInitialInvariants(system));
    }

    public ZoneState initialZone() {
        final ZoneState.Builder builder = ZoneState.zero(ceilings.keySet()).transform();
        if (initialDelayAllowed) {
            builder.up();
        }
        for (final Guard.ClockGuard invariant : initialInvariants) {
            builder.and(invariant.getClockConstr());
        }
        return builder.build();
    }

    private static List<Guard.ClockGuard> computeInitialInvariants(final XtaSystem system) {
        final List<Guard.ClockGuard> result = new ArrayList<>();
        for (final XtaProcess process : system.getProcesses()) {
            for (final Guard invariant : process.getInitLoc().getInvars()) {
                if (invariant.isClockGuard()) {
                    result.add(invariant.asClockGuard());
                }
            }
        }
        return List.copyOf(result);
    }

    public boolean exactInclusion() {
        return luBounds == null;
    }

    private static boolean isDiagonalFree(final XtaSystem system) {
        for (final ClockConstr constr : allClockConstrs(system)) {
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

    private static List<ClockConstr> allClockConstrs(final XtaSystem system) {
        final List<ClockConstr> result = new ArrayList<>();
        for (final XtaProcess process : system.getProcesses()) {
            for (final XtaProcess.Edge edge : process.getEdges()) {
                for (final Guard guard : edge.getGuards()) {
                    if (guard.isClockGuard()) {
                        result.add(guard.asClockGuard().getClockConstr());
                    }
                }
            }
            for (final XtaProcess.Loc loc : process.getLocs()) {
                for (final Guard invar : loc.getInvars()) {
                    if (invar.isClockGuard()) {
                        result.add(invar.asClockGuard().getClockConstr());
                    }
                }
            }
        }
        return result;
    }

    private static Map<VarDecl<RatType>, Integer> computeCeilings(final XtaSystem system) {
        final Map<VarDecl<RatType>, Integer> ceilings = new HashMap<>();
        for (final VarDecl<RatType> clock : system.getClockVars()) {
            ceilings.put(clock, 0);
        }
        for (final ClockConstr constr : allClockConstrs(system)) {
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

    private static BoundFunc computeLuBounds(final XtaSystem system) {
        final BoundFunc.Builder builder = BoundFunc.builder();
        for (final ClockConstr constr : allClockConstrs(system)) {
            builder.add(constr);
        }
        return builder.build();
    }

    private static boolean initialDelayAllowed(final XtaSystem system) {
        for (final XtaProcess process : system.getProcesses()) {
            if (process.getInitLoc().getKind() != XtaProcess.LocKind.NORMAL) {
                return false;
            }
        }
        return true;
    }
}
