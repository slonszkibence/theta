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

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class XtaInclusionOracleTest {

    private XtaSystem xtaSystem;
    private Alphabet<String> alphabet;
    private XtaProcess targetProcess; // M1 (Bus)
    private Set<String> shieldSymbols;

    @Before
    public void setUp() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/model/csma-2.xta");
        Assert.assertNotNull("A modell nem található!", stream);
        xtaSystem = XtaDslManager.createSystem(stream);

        XtaTimingMapper<Boolean, Boolean> mapper = XtaTimingMapper.create(xtaSystem, b -> b);
        alphabet = Alphabets.fromCollection(mapper.getAlphabet());

        targetProcess = xtaSystem.getProcesses().stream()
                .filter(p -> p.getName().equals("Bus"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Nem található Bus folyamat!"));

        shieldSymbols = new HashSet<>();
        for (XtaProcess.Loc loc : targetProcess.getLocs()) {
            for (XtaProcess.Edge edge : loc.getOutEdges()) {
                shieldSymbols.add(XtaTimingMapper.generateSymbolForEdge(edge));
            }
        }
    }

    @Test
    public void testInclusionSafe() {
        System.out.println("--- Futtatás: SAFE Teszt ---");

        var builder = AutomatonBuilders.newDFA(alphabet).withInitial(0).withAccepting(0);
        for (String symbol : alphabet) {
            builder.from(0).on(symbol).to(0); // Mindenre self-loop
        }
        DFA<?, String> universalDfa = builder.create();

        XtaInclusionOracle oracle = XtaInclusionOracle.create(
                xtaSystem, targetProcess, alphabet, shieldSymbols, XtaDfaCheckerStrategy.DFS
        );

        DefaultQuery<String, Boolean> result = oracle.findCounterExample(universalDfa, alphabet);

        Assert.assertNull("Az univerzális DFA-ra nem találhat ellenpéldát (SAFE kell legyen)!", result);
        System.out.println("Eredmény: Helyesen SAFE (null)");
    }

    @Test
    public void testInclusionUnsafe() {
        System.out.println("--- Futtatás: UNSAFE Teszt ---");

        String forbiddenSymbol = alphabet.stream()
                .findFirst()
                .orElseThrow();
        System.out.println("Tiltott szimbólum az M2 számára: " + forbiddenSymbol);

        var builder = AutomatonBuilders.newDFA(alphabet).withInitial(0).withAccepting(0);
        for (String symbol : alphabet) {
            if (!symbol.equals(forbiddenSymbol)) {
                builder.from(0).on(symbol).to(0);
            }
        }
        DFA<?, String> strictDfa = builder.create();

        XtaInclusionOracle oracle = XtaInclusionOracle.create(
                xtaSystem, targetProcess, alphabet, shieldSymbols, XtaDfaCheckerStrategy.DFS
        );

        DefaultQuery<String, Boolean> result = oracle.findCounterExample(strictDfa, alphabet);

        Assert.assertNotNull("A szigorú DFA-ra ellenpéldát (DefaultQuery) kell adnia!", result);
        Assert.assertFalse("Az ellenpélda query-nek 'elutasított' (false) flaggel kell visszatérnie!", result.getOutput());

        System.out.println("Talált Ellenpélda (Trace): " + result.getInput());
        Assert.assertTrue("Az ellenpéldának tartalmaznia kell szimbólumokat!", result.getInput().length() > 0);
    }
}