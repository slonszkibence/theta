import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.ResetStmt;
import hu.bme.mit.theta.core.stmt.Stmts;
import hu.bme.mit.theta.core.type.clocktype.ClockExprs;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.timed.TimedXstsActionProjections;
import hu.bme.mit.theta.xsts.learning.membership.TimedXstsSul;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static hu.bme.mit.theta.core.type.clocktype.ClockExprs.Clock;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;

public class TimedXstsSulTest {
  private VarDecl<ClockType> x, y;
  private ZonePrec zonePrec;
  private TimedXstsSul sul;

  @Before
  public void setUp() {
    x = Decls.Var("x", Clock());
    y = Decls.Var("y", Clock());
    zonePrec = ZonePrec.of(List.of(x, y));
    sul = TimedXstsSul.create(TimedXstsActionProjections.create(), zonePrec);
  }

  @Test
  public void conflictingGuardsAreInfeasible() {
    sul.pre();
    Assert.assertTrue(sul.step(XstsAction.create(Stmts.Assume(ClockExprs.Lt(x.getRef(), Int(5))))));
    boolean result = sul.step(XstsAction.create(Stmts.Assume(ClockExprs.Gt(x.getRef(), Int(10)))));
    Assert.assertFalse("After x < 5, x > 10 cannot be true", result);
  }

  @Test
  public void resetMakesLaterGuardFeasible() {
    sul.pre();
    sul.step(XstsAction.create(Stmts.Assume(ClockExprs.Gt(x.getRef(), Int(5)))));
    sul.step(XstsAction.create(ResetStmt.of(x, 0)));
    boolean result = sul.step(XstsAction.create(Stmts.Assume(ClockExprs.Lt(x.getRef(), Int(2)))));
    Assert.assertTrue("After reset x < 2 can be true", result);
  }
}