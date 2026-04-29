package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.analysis.XtaAction;
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
                new Object[]{"/model/Deadlock1Clock.xta", "/property/Deadlock1Clock.prop", true},
                new Object[]{"/model/Deadlock2Clock.xta", "/property/Deadlock2Clock.prop", true},
                new Object[]{"/model/DeadlockImmediate.xta", "/property/DeadlockImmediate.prop", true},
                new Object[]{"/model/TimingDeadlockDemo.xta", "/property/TimingDeadlockDemo.prop", true},
                //new Object[]{"/model/Desync.xta", "/property/Desync.prop", true},
                //new Object[]{"/model/Diagonal.xta", "/property/Diagonal.prop", true},
                //new Object[]{"/model/PointInterval.xta", "/property/PointInterval.prop", true},
                //new Object[]{"/model/Strict.xta", "/property/Strict.prop", true},
                //new Object[]{"/model/Zeno.xta", "/property/Zeno.prop", true},
                //new Object[]{"/model/ComplexBranching.xta", "/property/ComplexBranching.prop", true},
                new Object[]{"/model/leader_stateless_a.xta", "/property/leader_stateless_a.prop", true},
                new Object[]{"/model/leader_stateless_b.xta", "/property/leader_stateless_b.prop", true},
                new Object[]{"/model/leader_stateless_c.xta", "/property/leader_stateless_c.prop", true},
                new Object[]{"/model/leader_stateless_d.xta", "/property/leader_stateless_d.prop", true},
                new Object[]{"/model/leader_stay_a.xta", "/property/leader_stay_a.prop", true},
                new Object[]{"/model/leader_stay_b.xta", "/property/leader_stay_b.prop", true},
                new Object[]{"/model/leader_stay_c.xta", "/property/leader_stay_c.prop", true},
                new Object[]{"/model/leader_stay_d.xta", "/property/leader_stay_d.prop", true},
                new Object[]{"/model/ftsp-2-abs.xta", "/property/ftsp-2-abs.prop", true},
                new Object[]{"/model/ftsp-3-abs.xta", "/property/ftsp-3-abs.prop", true},
                new Object[]{"/model/ftsp-4-abs.xta", "/property/ftsp-4-abs.prop", true},
                new Object[]{"/model/sts-2.xta", "/property/sts-2.prop", true},
                //new Object[]{"/model/sts-3.xta", "/property/sts-3.prop", true}, //sts-3.xta | SAFE | SAFE | 195813 | 629504
                new Object[]{"/model/prio_sched_2a.xta", "/property/prio_sched_2a.prop", true},
                new Object[]{"/model/prio_sched_2b.xta", "/property/prio_sched_2b.prop", true},
                new Object[]{"/model/prio_sched_3c.xta", "/property/prio_sched_3c.prop", true},
                new Object[]{"/model/prio_sched_3d.xta", "/property/prio_sched_3d.prop", true},
                new Object[]{"/model/prio_sched_3e.xta", "/property/prio_sched_3e.prop", true}
        );
    }

    @BeforeClass
    public static void printHeader() {
        System.out.println("==========================================================================================");
        System.out.printf("%-30s | %-12s | %-12s | %-12s | %-12s%n", "Modell", "Classic", "Learning", "Classic (ms)", "Learning (ms)");
        System.out.println("==========================================================================================");
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
        // --- 1. CLASSIC THETA FUTTATÁSA ---
        long classicStartTime = System.currentTimeMillis();
        // A gyári beépített modellellenőrző példányosítása a fájljaid alapján
        CombinedLazyCegarXtaCheckerConfig classicConfig = CombinedLazyCegarXtaCheckerConfigFactory.create(xtaSystem).build();
        SafetyResult<?, ?> classicResult = classicConfig.check();
        long classicTime = System.currentTimeMillis() - classicStartTime;

        // --- 2. LEARNING ALGORITMUS FUTTATÁSA ---
        long learningStartTime = System.currentTimeMillis();
        XtaLearningCheckerConfig learningConfig = XtaLearningCheckerConfigFactory.create(xtaSystem)
                .consoleLogger(hu.bme.mit.theta.common.logging.Logger.Level.SUBSTEP)
                .build();
        SafetyResult<?, XtaAction> learningResult = learningConfig.check();
        long learningTime = System.currentTimeMillis() - learningStartTime;

        // --- Eredmények kiírása a konzolra ---
        String modelName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
        String classicSafeStr = classicResult.isSafe() ? "SAFE" : "UNSAFE";
        String learningSafeStr = learningResult.isSafe() ? "SAFE" : "UNSAFE";

        System.out.printf("%-30s | %-12s | %-12s | %-12d | %-12d%n",
                modelName, classicSafeStr, learningSafeStr, classicTime, learningTime);

        // --- 3. ASSERTIONOK (ELLENŐRZÉS) ---

        // Első teszt: Vajon jól találtuk ki az expectedSafety értékeket?
        Assert.assertEquals("Hiba a teszt beállításában! A gyári Classic Theta (" + classicSafeStr +
                        ") mást adott, mint a beégetett expectedSafety (" + expectedSafety + ") a " + modelPath + " fájlon!",
                expectedSafety, classicResult.isSafe());

        // Második teszt: A Te algoritmusod azt adja-e, mint a gyári algoritmus?
        Assert.assertEquals("A tanuló algoritmus (" + learningSafeStr + ") eltér a Classic Theta (" + classicSafeStr + ") eredményétől a " + modelPath + " fájlon!",
                classicResult.isSafe(), learningResult.isSafe());
    }
}