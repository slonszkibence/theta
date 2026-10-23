package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.combinedlazycegar.CombinedLazyCegarXtaCheckerConfig;
import hu.bme.mit.theta.xta.analysis.combinedlazycegar.CombinedLazyCegarXtaCheckerConfigFactory;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.modelchecking.DiscreteCompositionCheckerFactory;
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

@RunWith(Parameterized.class)
public class XtaLearningTest {
    private static final List<Object[]> MODELS = List.of(

            new Object[]{"/model/7.xta", "/property/7.prop", true},
            new Object[]{"/model/fischer-2-32-64.xta", "/property/fischer-2-32-64.prop", true},
            new Object[]{"/model/leader_stateless_a.xta", "/property/leader_stateless_a.prop", true},
            new Object[]{"/model/leader_stateless_b.xta", "/property/leader_stateless_b.prop", true},
            new Object[]{"/model/leader_stateless_c.xta", "/property/leader_stateless_c.prop", true},
            new Object[]{"/model/leader_stateless_d.xta", "/property/leader_stateless_d.prop", true},
            new Object[]{"/model/leader_stay_a.xta",      "/property/leader_stay_a.prop",      true},
            new Object[]{"/model/leader_stay_b.xta",      "/property/leader_stay_b.prop",      true},
            new Object[]{"/model/leader_stay_c.xta",      "/property/leader_stay_c.prop",      true},
            new Object[]{"/model/leader_stay_d.xta",      "/property/leader_stay_d.prop",      true},
            new Object[]{"/model/ftsp-2-abs.xta",         "/property/ftsp-2-abs.prop",         true},
            new Object[]{"/model/ftsp-3-abs.xta",         "/property/ftsp-3-abs.prop",         true},
            new Object[]{"/model/ftsp-4-abs.xta",         "/property/ftsp-4-abs.prop",         true},
            new Object[]{"/model/sts-2.xta",              "/property/sts-2.prop",              true},
            new Object[]{"/model/sts-3.xta",              "/property/sts-3.prop",              true},
            new Object[]{"/model/prio_sched_2a.xta",      "/property/prio_sched_2a.prop",      true},
            new Object[]{"/model/prio_sched_2b.xta",      "/property/prio_sched_2b.prop",      true},
            new Object[]{"/model/prio_sched_3c.xta",      "/property/prio_sched_3c.prop",      true},
            new Object[]{"/model/prio_sched_3d.xta",      "/property/prio_sched_3d.prop",      true},
            new Object[]{"/model/prio_sched_3e.xta",      "/property/prio_sched_3e.prop",      true}
    );

    @Parameter(0)
    public String modelPath;
    @Parameter(1)
    public String propPath;
    @Parameter(2)
    public Boolean expectedSafety;
    @Parameter(3)
    public LearningAlgorithmType learningAlgorithmType;
    @Parameter(4)
    public DiscreteCompositionCheckerFactory.SearchStrategy searchStrategy;

    private XtaSystem xtaSystem;

    @Parameters(name = "{0} | {3} | {4}")
    public static Collection<Object[]> data() {
        List<Object[]> cases = new ArrayList<>();
        for (Object[] model : MODELS) {
            for (LearningAlgorithmType learner : LearningAlgorithmType.values()) {
                for (DiscreteCompositionCheckerFactory.SearchStrategy search : DiscreteCompositionCheckerFactory.SearchStrategy.values()) {
                    cases.add(new Object[]{
                            model[0], model[1], model[2],
                            learner, search
                    });
                }
            }
        }
        return cases;
    }

    private static final boolean VERBOSE_LOGGING = false;

    @BeforeClass
    public static void printHeader() {
        System.out.println("=".repeat(146));
        System.out.printf("%-30s | %-6s | %-6s | %-10s | %-10s | %-8s | %-8s | %-6s | %-10s | %-7s | %-8s%n",
                "Model", "Learner", "Search",
                "Classic(ms)", "Learn(ms)",
                "Classic", "Learning",
                "Hyp.St", "ProdDFA.St", "Refines", "MQ");
        System.out.println("=".repeat(146));
    }

    @Before
    public void initialize() throws IOException {
        InputStream modelStream = getClass().getResourceAsStream(modelPath);
        InputStream propStream  = getClass().getResourceAsStream(propPath);

        if (modelStream == null) throw new FileNotFoundException("model resource not found: " + modelPath);
        if (propStream  == null) throw new FileNotFoundException("property resource not found: " + propPath);

        this.xtaSystem = XtaDslManager.createSystem(new SequenceInputStream(modelStream, propStream));
    }

    @Test
    public void testXtaLearning() throws Exception {
        long classicStart = System.currentTimeMillis();
        CombinedLazyCegarXtaCheckerConfig classicConfig =
                CombinedLazyCegarXtaCheckerConfigFactory.create(xtaSystem).build();
        SafetyResult<?, ?> classicResult = classicConfig.check();
        long classicTime = System.currentTimeMillis() - classicStart;

        long learningStart = System.currentTimeMillis();
        XtaLearningCheckerConfigFactory factory = XtaLearningCheckerConfigFactory
                .create(xtaSystem)
                .learningAlgorithmType(learningAlgorithmType)
                .searchStrategy(searchStrategy);
        if (VERBOSE_LOGGING) {
            factory.consoleLogger(Logger.Level.SUBSTEP);
        }
        XtaLearningCheckerConfig<?, ?> learningConfig = factory.build();
        XtaLearningCheckerConfig.CheckResult<?> checkResult = learningConfig.checkWithStats();
        long learningTime = System.currentTimeMillis() - learningStart;

        SafetyResult<?, ?> learningResult = checkResult.safetyResult();
        LearningStatistics stats = checkResult.statistics();

        String modelName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
        System.out.printf("%-30s | %-6s | %-6s | %-10d | %-10d | %-8s | %-8s | %-6d | %-10d | %-7d | %-8d%n",
                modelName,
                learningAlgorithmType,
                searchStrategy,
                classicTime,
                learningTime,
                PropertyVerdict.describe(xtaSystem, classicResult.isSafe()),
                PropertyVerdict.describe(xtaSystem, learningResult.isSafe()),
                stats.hypothesisStates(),
                stats.productDfaStates(),
                stats.totalRefinements(),
                stats.totalMqQueries());

        Assert.assertEquals(
                "Incorrect expectedSafety: Classic="
                        + PropertyVerdict.holds(xtaSystem, classicResult.isSafe())
                        + " vs expected=" + expectedSafety + " | " + modelPath,
                expectedSafety, PropertyVerdict.holds(xtaSystem, classicResult.isSafe()));

        Assert.assertEquals(
                "Differing result: " + learningAlgorithmType + "+" + searchStrategy
                        + " | " + modelPath,
                PropertyVerdict.holds(xtaSystem, classicResult.isSafe()),
                PropertyVerdict.holds(xtaSystem, learningResult.isSafe()));
    }
}
