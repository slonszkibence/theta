package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;
import hu.bme.mit.theta.xta.learning.compositional.common.TimedStepper;
import hu.bme.mit.theta.xta.learning.compositional.common.ZoneSemantics;
import hu.bme.mit.theta.xta.learning.compositional.membership.XtaTimedSul;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class XtaTimedSulTest {
    private record Fixture(XtaSystem system, TimedStepper stepper) {
        static Fixture of(final String xta) {
            final XtaSystem system;
            try {
                system = XtaDslManager.createSystem(
                        new ByteArrayInputStream(xta.getBytes(StandardCharsets.UTF_8)));
            } catch (final Exception e) {
                throw new AssertionError("the test model could not be parsed", e);
            }
            return new Fixture(system, TimedStepper.of(system, ZoneSemantics.of(system)));
        }

        String symbol(final String source, final String target) {
            for (final XtaProcess process : system.getProcesses()) {
                for (final XtaProcess.Edge edge : process.getEdges()) {
                    if (edge.getSource().getName().endsWith("_" + source)
                            && edge.getTarget().getName().endsWith("_" + target)) {
                        return stepper.register(List.of(edge));
                    }
                }
            }
            throw new AssertionError("no such edge: " + source + "->" + target);
        }

        boolean feasible(final String... edges) {
            final XtaTimedSul sul = XtaTimedSul.create(stepper);
            sul.pre();
            try {
                for (final String edge : edges) {
                    final String[] parts = edge.split("->");
                    if (!sul.step(symbol(parts[0], parts[1]))) {
                        return false;
                    }
                }
                return true;
            } finally {
                sul.post();
            }
        }
    }

    private static final String SIMPLE = """
            clock x;
            process P() {
                state l0, l1, l2, bounded { x <= 3 };
                init l0;
                trans
                    l0 -> l1 { guard x >= 5; },
                    l0 -> bounded { assign x = 0; },
                    bounded -> l2 { guard x >= 5; },
                    bounded -> l1 { guard x >= 2; };
            }
            system P;
            """;

    @Test
    public void emptyWordIsAlwaysFeasible() {
        Assert.assertTrue(Fixture.of(SIMPLE).feasible());
    }

    @Test
    public void lowerBoundGuardIsSatisfiableAfterDelay() {
        Assert.assertTrue(Fixture.of(SIMPLE).feasible("l0->l1"));
    }

    @Test
    public void targetInvariantBlocksALaterLowerBoundGuard() {
        Assert.assertFalse(Fixture.of(SIMPLE).feasible("l0->bounded", "bounded->l2"));
    }

    @Test
    public void targetInvariantStillAllowsASmallerGuard() {
        Assert.assertTrue(Fixture.of(SIMPLE).feasible("l0->bounded", "bounded->l1"));
    }

    private static final String RESET = """
            clock x;
            process P() {
                state l0, l1, l2;
                init l0;
                trans
                    l0 -> l1 { guard x >= 5; assign x = 0; },
                    l1 -> l2 { guard x >= 5; };
            }
            system P;
            """;

    @Test
    public void resetMakesTheFollowingLowerBoundGuardFeasibleAgain() {
        Assert.assertTrue(Fixture.of(RESET).feasible("l0->l1", "l1->l2"));
    }

    private static final String CONTRADICTORY = """
            clock x;
            process P() {
                state l0, l1;
                init l0;
                trans
                    l0 -> l1 { guard x >= 5 && x <= 3; };
            }
            system P;
            """;

    @Test
    public void contradictoryGuardsOnTheSameStepAreInfeasible() {
        Assert.assertFalse(Fixture.of(CONTRADICTORY).feasible("l0->l1"));
    }

    private static final String URGENT_TARGET = """
            clock x;
            process P() {
                state l0, hurry, done;
                urgent hurry;
                init l0;
                trans
                    l0 -> hurry { assign x = 0; },
                    hurry -> done { guard x >= 5; };
            }
            system P;
            """;

    private static final String NORMAL_TARGET = """
            clock x;
            process P() {
                state l0, hurry, done;
                init l0;
                trans
                    l0 -> hurry { assign x = 0; },
                    hurry -> done { guard x >= 5; };
            }
            system P;
            """;

    @Test
    public void noDelayInUrgentTargetLocation() {
        Assert.assertFalse("time cannot elapse in an urgent location",
                Fixture.of(URGENT_TARGET).feasible("l0->hurry", "hurry->done"));
        Assert.assertTrue("the same model with a normal location is feasible",
                Fixture.of(NORMAL_TARGET).feasible("l0->hurry", "hurry->done"));
    }

    private static final String COMMITTED_TARGET = """
            clock x;
            process P() {
                state l0, hurry, done;
                commit hurry;
                init l0;
                trans
                    l0 -> hurry { assign x = 0; },
                    hurry -> done { guard x >= 5; };
            }
            system P;
            """;

    @Test
    public void noDelayInCommittedTargetLocation() {
        Assert.assertFalse(Fixture.of(COMMITTED_TARGET).feasible("l0->hurry", "hurry->done"));
    }

    private static final String URGENT_INIT = """
            clock x;
            process P() {
                state start, done;
                urgent start;
                init start;
                trans
                    start -> done { guard x >= 5; };
            }
            system P;
            """;

    private static final String NORMAL_INIT = """
            clock x;
            process P() {
                state start, done;
                init start;
                trans
                    start -> done { guard x >= 5; };
            }
            system P;
            """;

    @Test
    public void noInitialDelayWhenTheInitialLocationIsUrgent() {
        Assert.assertFalse("not satisfiable with an urgent initial location",
                Fixture.of(URGENT_INIT).feasible("start->done"));
        Assert.assertTrue("satisfiable with a normal initial location",
                Fixture.of(NORMAL_INIT).feasible("start->done"));
    }

    private static final String BOUNDED_INIT = """
            clock x;
            process P() {
                state start { x <= 3 }, done;
                init start;
                trans
                    start -> done { guard x >= 5; };
            }
            system P;
            """;

    @Test
    public void initialInvariantsBoundTheInitialZone() {
        Assert.assertFalse("x >= 5 is not satisfiable under the initial invariant x <= 3",
                Fixture.of(BOUNDED_INIT).feasible("start->done"));
        Assert.assertTrue("satisfiable without an initial invariant",
                Fixture.of(NORMAL_INIT).feasible("start->done"));
    }

    private static final String TWO_CLOCKS = """
            clock x, y;
            process P() {
                state l0, l1, l2;
                init l0;
                trans
                    l0 -> l1 { guard y >= 5; assign x = 0; },
                    l1 -> l2 { guard x <= 0 && y >= 5; };
            }
            system P;
            """;

    @Test
    public void clocksAdvanceTogether() {
        Assert.assertTrue(Fixture.of(TWO_CLOCKS).feasible("l0->l1", "l1->l2"));
    }

    @Test
    public void preResetsTheStateBetweenQueries() {
        final Fixture fixture = Fixture.of(SIMPLE);
        Assert.assertFalse(fixture.feasible("l0->bounded", "bounded->l2"));
        Assert.assertTrue("the previous query must not leak", fixture.feasible("l0->l1"));
    }

    @Test
    public void theAlphabetStartsEmptyAndGrowsWhenEdgesAreRegistered() {
        final Fixture fixture = Fixture.of(SIMPLE);
        Assert.assertEquals("nothing is enumerated in advance",
                0, fixture.stepper().alphabet().size());

        int edges = 0;
        for (final XtaProcess process : fixture.system().getProcesses()) {
            for (final XtaProcess.Edge edge : process.getEdges()) {
                fixture.stepper().register(List.of(edge));
                edges++;
            }
        }
        Assert.assertEquals("without synchronisation every edge is its own action",
                edges, fixture.stepper().alphabet().size());

        final String repeated = fixture.symbol("l0", "l1");
        Assert.assertEquals("registering the same edge again must not grow the alphabet",
                edges, fixture.stepper().alphabet().size());
        Assert.assertTrue(fixture.stepper().alphabet().containsSymbol(repeated));
    }
}
