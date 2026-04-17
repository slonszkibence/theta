package hu.bme.mit.theta.xta.learning;

import de.learnlib.query.DefaultQuery;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.compositional.dfa.XtaDfaCheckerStrategy;
import hu.bme.mit.theta.xta.learning.inclusion2_0.XtaInclusionOracle;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.util.automaton.builder.AutomatonBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class XtaInclusionOracleDeadlockTest {

    private XtaSystem xtaSystem;
    private Alphabet<String> alphabet;
    private XtaProcess targetProcess; // M1 (A fiktív folyamat)
    private Set<String> shieldSymbols;

    // A modelled beágyazva, kibővítve egy fiktív M1 folyamattal
    private static final String MODEL =
            "chan i2a, a2c, c2d, a2b, b2c;\n" +
                    "clock x;\n" +
                    "clock y;\n" +
                    "process M1() {\n" +
                    "    state idle;\n" +
                    "    init idle;\n" +
                    "    trans idle -> idle {};\n" +
                    "}\n" +
                    "process Deadlock2ClockTest() {\n" +
                    "    state\n" +
                    "        idle,\n" +
                    "        A { x <= 3 },\n" +
                    "        B { y <= 2 },\n" +
                    "        C { x <= 5 },\n" +
                    "        D;\n" +
                    "    init idle;\n" +
                    "    trans\n" +
                    // JAVÍTÁS: A 'guard' mindig a 'sync' ELŐTT van!
                    "        idle -> A { guard x == 0 and y == 0; sync i2a!; assign x = 0, y = 0; },\n" +
                    "        A -> C { guard x >= 2 and x <= 3; sync a2c!; },\n" +
                    "        C -> D { guard x <= 4; sync c2d!; assign x = 0, y = 0; },\n" +
                    "        A -> B { guard x <= 2 and y <= 1; sync a2b!; },\n" +
                    "        B -> C { guard y >= 4; sync b2c!; };\n" +
                    "}\n" +
                    "system M1, Deadlock2ClockTest;";

    @Before
    public void setUp() throws Exception {
        // Modell beolvasása a stringből
        xtaSystem = XtaDslManager.createSystem(new ByteArrayInputStream(MODEL.getBytes(StandardCharsets.UTF_8)));

        // Ábécé kinyerése
        XtaTimingMapper<Boolean, Boolean> mapper = XtaTimingMapper.create(xtaSystem, b -> b);
        alphabet = Alphabets.fromCollection(mapper.getAlphabet());

        // A fiktív M1 kinevezése célpontnak.
        // Így a te modelled (Deadlock2ClockTest) lesz az M2 (a környezet)!
        targetProcess = xtaSystem.getProcesses().stream()
                .filter(p -> p.getName().equals("M1"))
                .findFirst()
                .orElseThrow();

        // Mivel az M1 üres, a pajzs szimbólumok halmaza is üres lesz
        shieldSymbols = new HashSet<>();
    }

    @Test
    public void test1_InclusionSafe_UniversalDFA() {
        System.out.println("--- 1. Teszt: Mindent megengedő DFA ---");

        // Egyetlen állapot, ami minden lépést elfogad
        var builder = AutomatonBuilders.newDFA(alphabet).withInitial(0).withAccepting(0);
        for (String symbol : alphabet) {
            builder.from(0).on(symbol).to(0);
        }
        DFA<?, String> universalDfa = builder.create();

        XtaInclusionOracle oracle = XtaInclusionOracle.create(xtaSystem, targetProcess, alphabet, shieldSymbols, XtaDfaCheckerStrategy.BFS);
        DefaultQuery<String, Boolean> result = oracle.findCounterExample(universalDfa, alphabet);

        // Várt eredmény: SAFE (null), mert minden lépés meg van engedve
        Assert.assertNull("Nem szabadna ellenpéldát találnia!", result);
        System.out.println("Eredmény: Helyesen SAFE (null) - Futásidő villámgyors!");
    }

    @Test
    public void test2_InclusionUnsafe_ReachableEdge() {
        System.out.println("--- 2. Teszt: Elérhető él tiltása (A -> C) ---");

        // Megkeressük az 'A -> C' élt az ábécében (ez egy valós, elérhető lépés a modelledben)
// Keresés a csatorna nevére (a2c)
        // Keresés az 'a2c' csatornára
        String targetEdge = alphabet.stream()
                .filter(s -> s.equals("a2c"))
                .findFirst()
                .orElseThrow();
        // Olyan DFA-t építünk, ami EZT AZ EGY ÉLT nem engedi meg
        var builder = AutomatonBuilders.newDFA(alphabet).withInitial(0).withAccepting(0);
        for (String symbol : alphabet) {
            if (!symbol.equals(targetEdge)) {
                builder.from(0).on(symbol).to(0);
            }
        }
        DFA<?, String> strictDfa = builder.create();

        XtaInclusionOracle oracle = XtaInclusionOracle.create(xtaSystem, targetProcess, alphabet, shieldSymbols, XtaDfaCheckerStrategy.BFS);
        DefaultQuery<String, Boolean> result = oracle.findCounterExample(strictDfa, alphabet);

        // Várt eredmény: UNSAFE (hibát talál), mert az idő elteltével az M2 fizikailag rá tud lépni az A->C élre!
        Assert.assertNotNull("Ellenpéldát kell találnia!", result);
        Assert.assertFalse(result.getOutput());
        System.out.println("Talált Trace: " + result.getInput());
    }

    @Test
    public void test3_InclusionSafe_DeadlockEdge() {
        System.out.println("--- 3. Teszt: Deadlock él tiltása (B -> C) ---");

        // Megkeressük a 'B -> C' élt. Emlékszel? y <= 2 (invariáns) vs y >= 4 (guard).
        // Keresés a 'b2c' csatornára
        String deadlockEdge = alphabet.stream()
                .filter(s -> s.equals("b2c"))
                .findFirst()
                .orElseThrow();
        var builder = AutomatonBuilders.newDFA(alphabet).withInitial(0).withAccepting(0);
        for (String symbol : alphabet) {
            if (!symbol.equals(deadlockEdge)) {
                builder.from(0).on(symbol).to(0);
            }
        }
        DFA<?, String> strictDfa = builder.create();

        XtaInclusionOracle oracle = XtaInclusionOracle.create(xtaSystem, targetProcess, alphabet, shieldSymbols, XtaDfaCheckerStrategy.BFS);
        DefaultQuery<String, Boolean> result = oracle.findCounterExample(strictDfa, alphabet);

        // VÁRT EREDMÉNY: SAFE (null)! Bár a DFA tiltja ezt a lépést, a Theta zónakalkulusa tudja,
        // hogy a modell az órák miatt sosem tudná ezt meglépni.
        Assert.assertNull("Bár a DFA tiltja, az M2 fizikailag képtelen ezt meglépni a deadlock miatt -> SAFE!", result);
        System.out.println("Eredmény: Helyesen SAFE (null) - A Theta zóna-kalkulusa hibátlanul működik!");
    }
}
