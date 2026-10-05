package hu.bme.mit.theta.xsts.learning;

import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.dsl.XstsDslManager;
import hu.bme.mit.theta.xsts.learning.common.HypothesisGraph;
import hu.bme.mit.theta.xsts.learning.common.UntimedXsts;
import hu.bme.mit.theta.xsts.learning.common.UntimedXstsGraph;
import hu.bme.mit.theta.xsts.learning.learnalgo.LearningAlgorithmType;

import net.automatalib.automaton.fsa.DFA;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class  HypothesisLearningDemoTest {

    private static final Path OUT_DIR = Path.of("build", "timeless-xsts");
    private static final int MAX_ITERATIONS = 50;

    @Test
    public void loop() throws Exception {
        demo("loop.xsts", "loop.prop");
    }

    @Test
    public void xtaToTxsts() throws Exception {
        demo("xta_to_txsts.xsts", "xta_to_txsts.prop");
    }

    @Test
    public void clocksIsRejectedForItsDiagonalConstraints() throws Exception {
        final XSTS xsts = load("clocks.xsts", "clocks.prop");
        try {
            runLearning(xsts, hu.bme.mit.theta.common.logging.NullLogger.getInstance());
            fail("expected the learner to reject a model with diagonal clock constraints");
        } catch (final UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("Diagonal clock constraint"));
        }
    }

    private void demo(final String model, final String prop) throws IOException {
        final XSTS xsts = load(model, prop);
        final Logger logger = new ConsoleLogger(Logger.Level.SUBSTEP);
        final Solver solver = Z3SolverFactory.getInstance().createSolver();

        System.out.printf("%n================ %s ================%n", model);

        final long start = System.currentTimeMillis();
        final XstsLearningCheckerConfig.CheckResult<?> result = runLearning(xsts, logger);
        final long elapsed = System.currentTimeMillis() - start;

        System.out.printf("%n  ALPHABET (%d symbols):%n", result.alphabet().size());
        int i = 0;
        for (final XstsAction symbol : result.alphabet()) {
            System.out.printf("    %-3d %s%n", i++, HypothesisGraph.symbolLabel(symbol));
        }
        System.out.printf("%n  H: %d states, %d refinements, %d SUL queries, %d ms%n",
                result.statistics().getHypothesisStates(), result.statistics().getRefinements(), result.statistics().getMembershipQueries(), elapsed);

        printTransitionTable(result.hypothesis(), result);
        writeDot(model, xsts, solver, result);

        assertNotNull(result.hypothesis().getInitialState());
        assertTrue("a converged hypothesis must have at least one state",
                result.statistics().getHypothesisStates() >= 1);
    }

    private <S> void printTransitionTable(final DFA<S, XstsAction> dfa,
                                          final XstsLearningCheckerConfig.CheckResult<?> result) {
        final Map<S, String> ids = HypothesisGraph.stateIds(dfa, result.alphabet());

        System.out.printf("%n  TRANSITIONS:%n");
        for (final Map.Entry<S, String> entry : ids.entrySet()) {
            final S state = entry.getKey();
            System.out.printf("    %-4s %s%s%n", entry.getValue(),
                    state.equals(dfa.getInitialState()) ? "[initial] " : "",
                    dfa.isAccepting(state) ? "" : "(rejecting - timing-infeasible)");
            for (final XstsAction symbol : result.alphabet()) {
                final S target = dfa.getTransition(state, symbol);
                System.out.printf("         --[%s]--> %s%n",
                        HypothesisGraph.symbolLabel(symbol),
                        target == null ? "-" : ids.getOrDefault(target, "?"));
            }
        }
    }

    private void writeDot(final String model, final XSTS xsts, final Solver solver,
                          final XstsLearningCheckerConfig.CheckResult<?> result) throws IOException {
        Files.createDirectories(OUT_DIR);
        final String base = model.replace(".xsts", "");

        final Graph hypothesisGraph = HypothesisGraph.create(result.hypothesis(), result.alphabet());
        final Path hypothesisPath = OUT_DIR.resolve(base + "-hypothesis.dot");
        Files.writeString(hypothesisPath, GraphvizWriter.getInstance().writeString(hypothesisGraph));

        final UntimedXsts.Result timeless = UntimedXsts.extract(xsts, solver);
        final Graph timelessGraph = UntimedXstsGraph.create(timeless, true);
        final Path timelessPath = OUT_DIR.resolve(base + "-timeless.dot");
        Files.writeString(timelessPath, GraphvizWriter.getInstance().writeString(timelessGraph));

        System.out.printf("%n  written: %s%n           %s%n",
                hypothesisPath.toAbsolutePath(), timelessPath.toAbsolutePath());
    }

    private static XstsLearningCheckerConfig.CheckResult<?> runLearning(
            final XSTS xsts, final Logger logger) {
        return XstsLearningCheckerConfigFactory.create(xsts, Z3SolverFactory.getInstance())
                .learningAlgorithmType(LearningAlgorithmType.TTT)
                .searchStrategy(SearchStrategy.BFS)
                .maxOuterIterations(MAX_ITERATIONS)
                .maxLearningIterations(MAX_ITERATIONS)
                .logger(logger)
                .build()
                .checkWithStats();
    }

    private static XSTS load(final String model, final String prop) throws IOException {
        try (InputStream in = new SequenceInputStream(
                new FileInputStream("src/test/resources/model/" + model),
                new FileInputStream("src/test/resources/property/" + prop))) {
            return XstsDslManager.createXsts(in);
        }
    }
}
