package hu.bme.mit.theta.xsts.learning;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfigBuilder;
import hu.bme.mit.theta.xsts.dsl.XstsDslManager;
import hu.bme.mit.theta.xsts.learning.learnalgo.LearningAlgorithmType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(Parameterized.class)
public class TimedXstsLearningTest {

    private static final long TIMEOUT_MINUTES = 1;
    private static final long CANCEL_GRACE_SECONDS = 30;

    private static final List<LearningAlgorithmType> LEARNERS = List.of(
            LearningAlgorithmType.TTT
            //LearningAlgorithmType.KV,
            //LearningAlgorithmType.LSTAR
    );
    private static final List<SearchStrategy> SEARCHES = List.of(
            SearchStrategy.BFS
            //SearchStrategy.DFS,
            //SearchStrategy.RANDOM
    );

    private static final List<Object[]> MODELS = buildModels();

    private static List<Object[]> buildModels() {
        final List<Object[]> models = new ArrayList<>();
        //addFamily(models, "crossroad", 17, "safety");
        addFamily(models, "antival", 10, "safety", "trans");
        return List.copyOf(models);
    }

    private static void addFamily(final List<Object[]> out, final String family,
                                  final int propCount, final String... extras) {
        final List<String> properties = new ArrayList<>();
        for (int i = 0; i < propCount; i++) {
            properties.add("prop" + i);
        }
        properties.addAll(List.of(extras));
        for (final String property : properties) {
            out.add(new Object[]{"/model/" + family + "-" + property + ".xsts", null, null});
            out.add(new Object[]{"/model/" + family + "-" + property + "-deadline.xsts", null, null});
        }
    }

    @Parameter(0)
    public String modelPath;
    @Parameter(1)
    public String propPath;
    @Parameter(2)
    public Boolean expectedSafety;
    @Parameter(3)
    public LearningAlgorithmType learningAlgorithmType;
    @Parameter(4)
    public SearchStrategy searchStrategy;

    private XSTS xsts;

    @Parameters(name = "{0} [{3}/{4}]")
    public static Collection<Object[]> data() {
        final List<Object[]> cases = new ArrayList<>();
        for (final Object[] model : MODELS) {
            for (final LearningAlgorithmType learner : LEARNERS) {
                for (final SearchStrategy search : SEARCHES) {
                    cases.add(new Object[]{model[0], model[1], model[2], learner, search});
                }
            }
        }
        return cases;
    }

    private static final boolean VERBOSE_LOGGING = false;

    private static final List<String> STAT_KEYS =
            List.copyOf(new LearningStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).keySet());

    private static final Map<String, String> STAT_LABELS = Map.ofEntries(
            Map.entry("AlgorithmTimeMs", "AlgoMs"),
            Map.entry("SetupTimeMs", "SetupMs"),
            Map.entry("Refinements", "Refines"),
            Map.entry("LearningTimeMs", "LearnMs"),
            Map.entry("ModelCheckingTimeMs", "McMs"),
            Map.entry("Iterations", "CegarIt"),
            Map.entry("RefinerTimeMs", "RefMs"),
            Map.entry("RealizabilityTimeMs", "RealizMs"),
            Map.entry("MembershipQueries", "MQ"),
            Map.entry("EquivalenceQueries", "EQ"),
            Map.entry("OuterIterations", "Outer"),
            Map.entry("AlphabetSize", "Sigma"),
            Map.entry("HypothesisStates", "HypSt"),
            Map.entry("ProductDfaStates", "ProdSt"));

    private static final String[] FIXED_HEADERS = {"Model", "Learner", "Search", "Result", "Wall(ms)"};
    private static final int[] FIXED_WIDTHS = {30, 6, 6, 8, 9};
    private static final int STAT_WIDTH = 8;

    @BeforeClass
    public static void printHeader() {
        final String header = row(FIXED_HEADERS, key -> STAT_LABELS.getOrDefault(key, key));
        System.out.println("=".repeat(header.length()));
        System.out.println(header);
        System.out.println("=".repeat(header.length()));
    }

    private static String pad(final Object value, final int width) {
        final String s = String.valueOf(value);
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }

    private static String row(final Object[] fixed,
                              final java.util.function.Function<String, Object> statValue) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fixed.length; i++) {
            sb.append(pad(fixed[i], FIXED_WIDTHS[i])).append(" | ");
        }
        for (final String key : STAT_KEYS) {
            sb.append(pad(statValue.apply(key), STAT_WIDTH)).append(" | ");
        }
        return sb.toString();
    }

    @Before
    public void initialize() throws IOException {
        final InputStream modelStream = getClass().getResourceAsStream(modelPath);
        if (modelStream == null) throw new FileNotFoundException("model resource not found: " + modelPath);

        if (propPath == null) {
            this.xsts = XstsDslManager.createXsts(modelStream);
            return;
        }

        final InputStream propStream = getClass().getResourceAsStream(propPath);
        if (propStream == null) throw new FileNotFoundException("property resource not found: " + propPath);

        this.xsts = XstsDslManager.createXsts(new SequenceInputStream(modelStream, propStream));
    }

    @Test
    public void testXstsLearning() throws Exception {
        final XstsLearningCheckerConfigFactory factory = XstsLearningCheckerConfigFactory
                .create(xsts, Z3SolverFactory.getInstance(), XstsConfigBuilder.Domain.EXPL_PRED_SPLIT)
                .learningAlgorithmType(learningAlgorithmType)
                .searchStrategy(searchStrategy)
                .refinement(XstsLearningCheckerConfigFactory.Refinement.FW_BIN_ITP)
                .autoExpl(XstsConfigBuilder.AutoExpl.NEWOPERANDS)
                .predSplit(XstsLearningCheckerConfigFactory.PredSplit.CONJUNCTS)
                .pruneStrategy(PruneStrategy.FULL)
                .maxOuterIterations(5000)
                .maxLearningIterations(5000);
        if (VERBOSE_LOGGING) {
            factory.consoleLogger(Logger.Level.MAINSTEP);
        }
        final XstsLearningCheckerConfig<?, ?> learningConfig = factory.build();

        final String modelName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
        final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "learning-" + modelName);
            thread.setDaemon(true);
            return thread;
        });

        final long start = System.currentTimeMillis();
        try {
            final Callable<XstsLearningCheckerConfig.CheckResult<?>> task =
                    () -> learningConfig.checkWithStats();
            final Future<XstsLearningCheckerConfig.CheckResult<?>> future = executor.submit(task);

            final XstsLearningCheckerConfig.CheckResult<?> checkResult;
            try {
                checkResult = future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (final TimeoutException timeout) {
                future.cancel(true);
                executor.shutdownNow();
                // Report only once the thread is really gone, so the next model is not measured
                // against a core still working on this one.
                final boolean stopped =
                        executor.awaitTermination(CANCEL_GRACE_SECONDS, TimeUnit.SECONDS);
                System.out.println(row(
                        new Object[]{modelName, learningAlgorithmType, searchStrategy,
                                stopped ? "TIMEOUT" : "TIMEOUT*",
                                System.currentTimeMillis() - start},
                        key -> "-"));
                if (!stopped) {
                    System.out.printf("  WARNING: %s did not stop within %d s of being cancelled -"
                                    + " probably inside a single long solver call, so the rows"
                                    + " after this one may be optimistic%n",
                            modelName, CANCEL_GRACE_SECONDS);
                }
                return;
            }
            final long wallTime = System.currentTimeMillis() - start;

            final SafetyResult<?, ?> result = checkResult.safetyResult();
            final LearningStatistics stats = checkResult.statistics();

            System.out.println(row(
                    new Object[]{modelName, learningAlgorithmType, searchStrategy,
                            result.isSafe() ? "SAFE" : "UNSAFE", wallTime},
                    stats::get));

            if (expectedSafety != null) {
                Assert.assertEquals(
                        "Incorrect verdict for " + modelPath + " [" + learningAlgorithmType
                                + "/" + searchStrategy + "]",
                        expectedSafety, result.isSafe());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
