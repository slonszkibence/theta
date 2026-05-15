package hu.bme.mit.theta.xta.learning;

/*import hu.bme.mit.theta.core.clock.constr.ClockConstrs;
import hu.bme.mit.theta.core.clock.op.ClockOps;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatExprs;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.Guard;
import hu.bme.mit.theta.xta.Update;
import hu.bme.mit.theta.xta.learning.compositional.common.TransitionConstraints;
import hu.bme.mit.theta.xta.learning.compositional.membership.XtaTPrimeSul;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XtaTPrimeSulTest {
    private XtaTPrimeSul membership;
    private VarDecl<RatType> clockX;
    private VarDecl<RatType> clockY;

    @Before
    public void setUp() {
        clockX = Decls.Var("x", RatExprs.Rat());
        clockY = Decls.Var("y", RatExprs.Rat());

        Map<VarDecl<RatType>, Integer> ceilings = new HashMap<>();
        ceilings.put(clockX, 10);
        ceilings.put(clockY, 10);

        membership = XtaTPrimeSul.create(ceilings);
    }

    @After
    public void tearDown() {
        if (membership != null) {
            membership.post();
        }
    }

    @Test
    public void testSimpleFeasibleStep() {
        membership.pre();

        TransitionConstraints step1 = TransitionConstraints.create(
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList()
        );

        boolean result = membership.step(step1);
        Assert.assertTrue("Az üres tranzíciónak végrehajthatónak kell lennie", result);
    }

    @Test
    public void testInfeasibleStepWithConflictingGuards() {
        membership.pre();

        TransitionConstraints step1 = TransitionConstraints.create(
                List.of(Guard.clockGuard(ClockConstrs.Lt(clockX, 5).toExpr())),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );
        Assert.assertTrue(membership.step(step1));

        TransitionConstraints step2 = TransitionConstraints.create(
                List.of(
                        Guard.clockGuard(ClockConstrs.Lt(clockX, 5).toExpr()),
                        Guard.clockGuard(ClockConstrs.Gt(clockX, 10).toExpr())
                ),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        boolean result = membership.step(step2);
        Assert.assertFalse("Egymásnak ellentmondó őrfeltételek esetén false-t kell adnia", result);
    }

    @Test
    public void testClockResetMakesStepFeasible() {
        membership.pre();

        TransitionConstraints step1 = TransitionConstraints.create(
                List.of(Guard.clockGuard(ClockConstrs.Gt(clockX, 5).toExpr())),
                List.of(new Update.ClockUpdate(ClockOps.Reset(clockX, 0))),
                Collections.emptyList(), Collections.emptyList()
        );
        Assert.assertTrue(membership.step(step1));

        TransitionConstraints step2 = TransitionConstraints.create(
                List.of(Guard.clockGuard(ClockConstrs.Lt(clockX, 2).toExpr())),
                Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList()
        );

        boolean result = membership.step(step2);
        Assert.assertTrue("Az x < 2 feltétel nem teljesülhet, mert a guard x > 5 zónán kerül kiértékelésre (guard ELŐBB fut, mint a reset)", result);
    }

    @Test
    public void testTargetInvariantFails() {
        membership.pre();

        TransitionConstraints step1 = TransitionConstraints.create(
                List.of(Guard.clockGuard(ClockConstrs.Gt(clockX, 5).toExpr())),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(Guard.clockGuard(ClockConstrs.Leq(clockX, 2).toExpr()))
        );

        boolean result = membership.step(step1);
        Assert.assertFalse("Az x > 5 guard és az x <= 2 target invariáns ütközik", result);
    }
}*/