package hu.bme.mit.theta.xta.learning.compositional.modelchecking;

import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ItpRefToExplPrec;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.RefutationToPrec;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.zone.ZonePrec;

import java.util.Set;

/**
 * Strategy for converting an interpolant-based refutation into a product precision
 * containing both explicit and zone precisions.
 * <p>
 * In the learning-based compositional model checking framework, the discrete
 * finite-state phase ({@code A || H}) may utilize a combined state space
 * (e.g., {@code Prod2State<ExplState, ZoneState>}) depending on the configuration.
 * Consequently, the required precision is a {@link Prod2Prec}.
 * <p>
 * However, the underlying SMT-based trace checker (which evaluates the feasibility
 * of counterexamples) only reasons about the discrete data variables, yielding an
 * {@link ItpRefutation} (interpolants). This class bridges that gap: it delegates
 * the discrete precision extraction to {@link ItpRefToExplPrec} and pairs the
 * resulting {@link ExplPrec} with an empty {@link ZonePrec}.
 * <p>
 * The zone precision remains empty/unchanged because the refinement in this specific
 * CEGAR loop targets only the discrete data abstractions, while timing abstractions
 * are handled separately by the DFA learning algorithm.
 */
public class RefutationToProd2ExplPrec
        implements RefutationToPrec<Prod2Prec<ExplPrec, ZonePrec>, ItpRefutation> {
    private final ItpRefToExplPrec inner = new ItpRefToExplPrec();

    /**
     * Extracts the precision from the given interpolant refutation at the specified index.
     *
     * @param ref   The interpolant-based refutation provided by the trace checker.
     * @param index The index along the trace.
     * @return A product precision containing the extracted explicit variables and an empty zone precision.
     */
    @Override
    public Prod2Prec<ExplPrec, ZonePrec> toPrec(ItpRefutation ref, int index) {
        return Prod2Prec.of(inner.toPrec(ref, index), ZonePrec.of(Set.of()));
    }

    /**
     * Joins (merges) two product precisions.
     * <p>
     * The explicit precisions are joined using the underlying explicit join logic
     * (taking the union of tracked variables). The zone precision is carried over
     * from the first argument (which is typically the accumulated precision so far).
     *
     * @param p1 The first precision.
     * @param p2 The second precision.
     * @return The joined product precision.
     */
    @Override
    public Prod2Prec<ExplPrec, ZonePrec> join(Prod2Prec<ExplPrec, ZonePrec> p1,
                                              Prod2Prec<ExplPrec, ZonePrec> p2) {
        return Prod2Prec.of(inner.join(p1.getPrec1(), p2.getPrec1()), p1.getPrec2());
    }
}