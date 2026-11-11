package hu.bme.mit.theta.xsts.learning.common;

import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.ResetStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;
import hu.bme.mit.theta.xsts.learning.membership.TimedXstsSul;
import net.automatalib.word.Word;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

public final class ClockPrecRefiner {

    private ClockPrecRefiner() {
    }

    public static Set<VarDecl<ClockType>> refine(final Word<XstsAction> fullWord, final Set<VarDecl<ClockType>> tracked) {
        final Set<VarDecl<ClockType>> clocks = new LinkedHashSet<>(tracked);
        clocks.addAll(clocksOf(fullWord));
        checkState(!feasible(fullWord, clocks),
                "the word is realizable, so it is not a spurious counterexample");

        for (final VarDecl<ClockType> clock : List.copyOf(clocks)) {
            if (tracked.contains(clock)) {
                continue;
            }
            clocks.remove(clock);
            if (feasible(fullWord, clocks)) {
                clocks.add(clock);
            }
        }

        checkState(!tracked.containsAll(clocks),
                "the word is infeasible over the tracked clocks, so it needs no clock refinement");
        return Set.copyOf(clocks);
    }

    private static boolean feasible(final Word<XstsAction> word,
                                    final Set<VarDecl<ClockType>> clocks) {
        final TimedXstsSul sul = TimedXstsSul.create(
                TimedXstsActionProjections.create(clocks), ZonePrec.of(clocks));
        sul.pre();
        try {
            for (final XstsAction symbol : word) {
                if (!sul.step(symbol)) {
                    return false;
                }
            }
            return true;
        } finally {
            sul.post();
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<VarDecl<ClockType>> clocksOf(final Word<XstsAction> word) {
        final Set<VarDecl<ClockType>> clocks = new LinkedHashSet<>();
        for (final XstsAction symbol : word) {
            for (final Stmt stmt : symbol.getStmts()) {
                if (stmt instanceof ResetStmt reset) {
                    clocks.add(reset.getClockDecl());
                } else if (stmt instanceof AssumeStmt assume) {
                    for (final VarDecl<?> var : ExprUtils.getVars(assume.getCond())) {
                        if (var.getType() instanceof ClockType) {
                            clocks.add((VarDecl<ClockType>) var);
                        }
                    }
                }
            }
        }
        return clocks;
    }
}
