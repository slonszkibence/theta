package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaState;
import hu.bme.mit.theta.xta.analysis.combinedlazycegar.CombinedLazyCegarXtaCheckerConfig;
import hu.bme.mit.theta.xta.analysis.combinedlazycegar.CombinedLazyCegarXtaCheckerConfigFactory;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;
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
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class XtaLearningTest {
    @Parameter(0)
    public String modelPath;
    @Parameter(1)
    public String propPath;
    @Parameter(2)
    public Boolean expectedSafety;

    private XtaSystem xtaSystem;

    @Parameters(name = "model: {0}, safety: {2}")
    public static Collection<Object[]> data() {
        return List.of(
                //new Object[]{"/model/Deadlock1Clock.xta", "/property/Deadlock1Clock.prop", true},
                //new Object[]{"/model/Deadlock2Clock.xta", "/property/Deadlock2Clock.prop", false},
                //new Object[]{"/model/DeadlockImmediate.xta", "/property/DeadlockImmediate.prop", true},
                //new Object[]{"/model/Desync.xta", "/property/Desync.prop", true},
                //new Object[]{"/model/Diagonal.xta", "/property/Diagonal.prop", true},
                //new Object[]{"/model/PointInterval.xta", "/property/PointInterval.prop", false},
                //new Object[]{"/model/Strict.xta", "/property/Strict.prop", true},
                //new Object[]{"/model/Zeno.xta", "/property/Zeno.prop", false},
                new Object[]{"/model/ComplexBranching.xta", "/property/ComplexBranching.prop", false},
                new Object[]{"/model/leader_stateless_a.xta", "/property/leader_stateless_a.prop", false}
        );
    }
    @BeforeClass
    public static void printHeader() {
        System.out.println("===============================================================================");
        System.out.printf("%-30s | %-10s | %-15s | %-15s%n", "Modell", "Eredmény", "Classic (ms)", "Learning (ms)");
        System.out.println("===============================================================================");
    }

    @Before
    public void initialize() throws IOException {
        InputStream modelStream = getClass().getResourceAsStream(modelPath);
        InputStream propStream = getClass().getResourceAsStream(propPath);

        if (modelStream == null) {
            throw new FileNotFoundException("model resource not found: " + modelPath);
        }
        if (propStream == null) {
            throw new FileNotFoundException("property resource not found: " + propPath);
        }

        final InputStream inputStream = new SequenceInputStream(modelStream, propStream);
        this.xtaSystem = XtaDslManager.createSystem(inputStream);
    }

    @Test
    public void testXtaLearning() throws Exception {
        long learningStartTime = System.currentTimeMillis();

        XtaLearningCheckerConfig learningConfig = XtaLearningCheckerConfigFactory.create(xtaSystem).build();
        SafetyResult<?, XtaAction> learningResult = learningConfig.check();

        long learningTime = System.currentTimeMillis() - learningStartTime;

        // Ellenőrizzük, hogy a Te algoritmusod a helyes, elvárt eredményt adta-e
        Assert.assertEquals("A tanuló algoritmus hibás eredményt adott a " + modelPath + " fájlon!",
                expectedSafety, learningResult.isSafe());

        // --- Eredmények kiírása a konzolra ---
        String modelName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
        String safeStr = learningResult.isSafe() ? "SAFE" : "UNSAFE";

        System.out.printf("%-30s | %-10s | %-15d%n",
                modelName, safeStr, learningTime);
    }
}
