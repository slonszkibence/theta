package hu.bme.mit.theta.xta.learning;

import de.learnlib.algorithm.ttt.dfa.TTTLearnerDFABuilder;
import de.learnlib.query.Query;
import de.learnlib.sul.SUL;
import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.mapper.MappedSUL;
import de.learnlib.util.Experiment;

import hu.bme.mit.theta.xta.learning.compositional.common.ProductAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.UntimedAutomatonBuilder;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;

import hu.bme.mit.theta.xta.learning.compositional.inclusion.XtaInclusionOracle;
import hu.bme.mit.theta.xta.learning.compositional.sul.XtaTPrimeSul;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.visualization.GraphBuilder;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("1. XTA modell betöltése...");
        XtaSystem system = XtaDslManager.createSystem(new FileInputStream("subprojects/xta/xta-learning/src/test/resources/Deadlock2Clock.xta"));
        XtaProcess firstProcess = system.getProcesses().get(0);

        System.out.println("\n=================================================");
        System.out.println("===== AZ EREDETI MODELL SKELETON (A) DOT-JA =====");
        System.out.println("=================================================");
        System.out.println(GraphBuilder.generateXtaProcessDot(firstProcess));
        System.out.println("=================================================\n");

        System.out.println("2. Órakorlátok (Ceilings) kiszámítása...");
        Map<VarDecl<RatType>, Integer> ceilings = computeCeilings(system);

        System.out.println("3. SUL és Mapper inicializálása...");
        XtaTPrimeSul baseSul = XtaTPrimeSul.create(ceilings);
        XtaTimingMapper<Boolean, Boolean> mapper = XtaTimingMapper.create(system, output -> output);
        Alphabet<String> alphabet = Alphabets.fromCollection(mapper.getAlphabet());

        System.out.println("=================================================");
        System.out.println("=== AZ IDŐZÍTÉS NÉLKÜLI (UNTIMED) DFA DOT-JA ===");
        System.out.println("=================================================");
        FastDFA<String> untimedDfa = null;
        try {
            untimedDfa = UntimedAutomatonBuilder.build(firstProcess, alphabet);
            System.out.println(GraphBuilder.generateLearnedDfaDot(untimedDfa, alphabet));
        } catch (Exception e) {
            System.out.println("/* Hiba az Untimed DFA generálásakor: " + e.getMessage() + " */");
        }
        System.out.println("=================================================\n");

        final FastDFA<String> structuralShield = untimedDfa;

        SUL<String, Boolean> mappedSul = new MappedSUL<>(mapper, baseSul);

        System.out.println("4. Orákulumok és Tanuló Algoritmus felépítése...");
        int[] queryCounter = {0};

        MembershipOracle.DFAMembershipOracle<String> mqOracle = queries -> {
            for (Query<String, Boolean> query : queries) {
                queryCounter[0]++;

                mappedSul.pre();
                boolean accepted = true;
                for (String symbol : query.getInput()) {
                    Boolean stepResult = mappedSul.step(symbol);
                    if (stepResult == null || !stepResult) {
                        accepted = false;
                        break;
                    }
                }
                query.answer(accepted);
                mappedSul.post();
            }
        };

        LearningAlgorithm.DFALearner<String> learner = new TTTLearnerDFABuilder<String>()
                .withAlphabet(alphabet)
                .withOracle(mqOracle)
                .create();

        XtaInclusionOracle inclusionOracle = XtaInclusionOracle.create(ceilings, mapper);

        System.out.println("5. Tanulási folyamat elindítása...");

        Experiment.DFAExperiment<String> experiment = new Experiment.DFAExperiment<>(learner, inclusionOracle, alphabet);
        experiment.run();

        System.out.println("------------------------------------------------");
        System.out.println("TANULÁS BEFEJEZŐDÖTT!");
        System.out.println("Finomítási körök (EQ lekérdezések) száma: " + experiment.getRounds().getCount());
        System.out.println("Tagsági (MQ) lekérdezések száma összesen: " + queryCounter[0]);

        DFA<?, String> resultDFA = experiment.getFinalHypothesis();

        System.out.println("\n=================================================");
        System.out.println("==== A MEGTANULT IDŐZÍTÉSI TÉRKÉP (H) DOT-JA ====");
        System.out.println("=================================================");
        System.out.println(GraphBuilder.generateLearnedDfaDot(resultDFA, alphabet));
        System.out.println("=================================================\n");

        try {
            if (structuralShield != null) {
                FastDFA<String> productDfa = ProductAutomatonBuilder.buildProduct(structuralShield, resultDFA, alphabet);
                System.out.println("=== A SZINKRONSZORZAT (A || H) DOT-JA ===");
                System.out.println(GraphBuilder.generateLearnedDfaDot(productDfa, alphabet));
                System.out.println("=================================================\n");
            } else {
                System.out.println("/* Nem sikerült a szorzat generálása, mert az Untimed DFA hiányzik. */");
            }
        } catch (Exception e) {
            System.out.println("/* Hiba a szorzat DFA generálásakor: " + e.getMessage() + " */");
            e.printStackTrace();
        }
    }

    private static Map<VarDecl<RatType>, Integer> computeCeilings(XtaSystem system) {
        Map<VarDecl<RatType>, Integer> ceilings = new HashMap<>();
        for (VarDecl<RatType> clock : system.getClockVars()) {
            ceilings.put(clock, 5);
        }
        return ceilings;
    }
}