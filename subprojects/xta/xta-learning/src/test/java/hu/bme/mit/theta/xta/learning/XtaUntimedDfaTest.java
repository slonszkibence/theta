package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.compositional.dfa.XtaUntimedDfaBuilder;

import hu.bme.mit.theta.xta.learning.visualization.GraphBuilder;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

public class XtaUntimedDfaTest {

    @Test
    public void testDfaBuildingFromXtaProcess() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/model/csma-2.xta");
        Assert.assertNotNull("A teszt modell nem található a resources mappában!", stream);
        XtaSystem xtaSystem = XtaDslManager.createSystem(stream);

        XtaTimingMapper<Boolean, Boolean> mapper = XtaTimingMapper.create(xtaSystem, b -> b);
        Set<String> alphabetSymbols = mapper.getAlphabet();
        Alphabet<String> alphabet = Alphabets.fromCollection(alphabetSymbols);

        XtaProcess process = xtaSystem.getProcesses().get(0);
        int expectedStateCount = process.getLocs().size();

        DFA<?, String> dfa = XtaUntimedDfaBuilder.build(process, alphabet);

        Assert.assertNotNull("A generált DFA nem lehet null", dfa);

        Assert.assertEquals("Az állapotok számának pontosan egyeznie kell az XTA lokációk számával", expectedStateCount, dfa.size());

        Assert.assertNotNull("A DFA-nak rendelkeznie kell kezdőállapottal", dfa.getInitialState());

        for (Object state : dfa.getStates()) {
            @SuppressWarnings("unchecked")
            boolean isAccepting = ((DFA<Object, String>)dfa).isAccepting(state);
            Assert.assertTrue("Minden állapotnak elfogadónak (accepting) kell lennie", isAccepting);
        }

        System.out.println("=== Sikeres (és absztrakt!) DFA építés ===");
        System.out.println("Processz neve: " + process.getName());
        System.out.println("Létrehozott állapotok száma: " + dfa.size());
        System.out.println("Ábécé mérete: " + alphabet.size());

        System.out.println("\n=== Generált DOT Gráf ===");

        DFA<Object, String> castedDfa = (DFA<Object, String>) dfa;

        String dotGraph = GraphBuilder.generateLearnedDfaDot(castedDfa, alphabet);
        System.out.println(dotGraph);
    }
}
