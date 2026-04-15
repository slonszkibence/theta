package hu.bme.mit.theta.xta.learning;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.Query;
import de.learnlib.sul.SUL;
import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.mapper.MappedSUL;
import de.learnlib.util.Experiment;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleFactory;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmFactory;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.xta.analysis.XtaAction;
import hu.bme.mit.theta.xta.analysis.XtaAnalysis;
import hu.bme.mit.theta.xta.analysis.expl.XtaExplAnalysis;
import hu.bme.mit.theta.xta.learning.compositional.common.ProductAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.common.UntimedAutomatonBuilder;
import hu.bme.mit.theta.xta.learning.compositional.realizability.*;
import hu.bme.mit.theta.xta.learning.compositional.inclusion.XtaInclusionOracle;
import hu.bme.mit.theta.xta.learning.compositional.sul.XtaTPrimeSul;
import hu.bme.mit.theta.xta.learning.compositional.common.XtaTimingMapper;
import hu.bme.mit.theta.xta.learning.visualization.GraphBuilder;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.rattype.RatType;
import hu.bme.mit.theta.xta.XtaProcess;
import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.dsl.XtaDslManager;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("1. XTA modell betöltése...");
        XtaSystem system = XtaDslManager.createSystem(new FileInputStream("subprojects/xta/xta-learning/src/test/resources/model/Complexbranching.xta"));
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

                if (structuralShield != null) {
                    boolean isStructurallyValid = structuralShield.accepts(query.getInput());
                    if (!isStructurallyValid) {
                        query.answer(true);
                        continue;
                    }
                }

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

        LearningAlgorithm.DFALearner<String> learner = LearningAlgorithmFactory.create(
                LearningAlgorithmType.LSTAR,
                alphabet,
                mqOracle
        );

        XtaInclusionOracle inclusionOracle = XtaInclusionOracle.create(ceilings, mapper);

        EquivalenceOracle<DFA<?, String>, String, Boolean> eqOracle =
                EQOracleFactory.create(EQOracleType.RANDOM_WORDS, mqOracle, inclusionOracle, 2, 10, 1000, 10000);

        System.out.println("5. Tanulási folyamat elindítása...");

        Experiment.DFAExperiment<String> experiment = new Experiment.DFAExperiment<>(learner, eqOracle, alphabet);
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

        FastDFA<String> productDfa = null;
        try {
            if (structuralShield != null) {
                productDfa = ProductAutomatonBuilder.buildProduct(structuralShield, resultDFA, alphabet);
                System.out.println("=== A SZINKRONSZORZAT (A || H) DOT-JA ===");
                System.out.println(GraphBuilder.generateLearnedDfaDot(productDfa, alphabet));
                System.out.println("=================================================\n");
            } else {
                System.out.println("/* Nem sikerült a szorzat generálása, mert az Untimed DFA hiányzik. */");
                return;
            }
        } catch (Exception e) {
            System.out.println("/* Hiba a szorzat DFA generálásakor: " + e.getMessage() + " */");
            e.printStackTrace();
            return;
        }

        // =========================================================================
        // 6. DISZKRÉT MODELLELLENŐRZÉS (BEJÁRÁS) A FELÉPÍTETT DFA-N
        // =========================================================================
        System.out.println("6. Diszkrét modellellenőrzés indítása a szinkronszorzaton...");

        final FastDFA<String> finalProductDfa = productDfa;

        // 6.1. Hibaállapot detektálása a belső XTA helyszínek (Locations) alapján
        Predicate<DiscreteCompositionState<FastDFAState>> targetPred = state -> {
            // Csak a megalkotott LearnLib DFA-t kérdezzük meg, hogy ez egy tiltott állapot-e
            return !finalProductDfa.isAccepting(state.getDfaState());
        };

        // 6.2. Thetás beépített Init/Trans függvények lekérése
        XtaExplAnalysis explAnalysis = XtaExplAnalysis.create(system);
        XtaAnalysis<ExplState, UnitPrec> xtaAnalysis = XtaAnalysis.create(system, explAnalysis);

        // 6.3. Analízis összerakása (JAVÍTVA: Integer helyett FastDFAState)
        DiscreteCompositionAnalysis<FastDFAState, UnitPrec> analysis = DiscreteCompositionAnalysis.create(
                (s1, s2) -> s1.equals(s2),
                alphabet,
                finalProductDfa,
                xtaAnalysis.getInitFunc(),
                xtaAnalysis.getTransFunc()
        );

        // 6.4. LTS
        DiscreteCompositionLts lts = DiscreteCompositionLts.create(system, alphabet, finalProductDfa);

        // 6.5. A Checker felépítése (JAVÍTVA: Integer helyett FastDFAState)
        SafetyChecker<DiscreteCompositionState<FastDFAState>, XtaAction, UnitPrec> checker =
                DiscreteCompositionCheckerFactory.create(
                        DiscreteCompositionStrategy.BFS,
                        analysis,
                        lts,
                        targetPred
                );

        // 6.6. Keresés elindítása
        long checkerStartTime = System.currentTimeMillis();
        SafetyResult<DiscreteCompositionState<FastDFAState>, XtaAction> result = checker.check(UnitPrec.getInstance());
        long checkerEndTime = System.currentTimeMillis();

        System.out.println("\n------------------------------------------------");
        System.out.println("MODELLELLENŐRZÉS BEFEJEZŐDÖTT " + (checkerEndTime - checkerStartTime) + " ms alatt.");

        if (result.isSafe()) {
            System.out.println("EREDMÉNY: A rendszer BIZTONSÁGOS (Nem elérhető hibaállapot a szorzatban).");
        } else {
            System.out.println("EREDMÉNY: A rendszer HIBÁS! Valós ellenpélda (Trace):");
            // (JAVÍTVA: Integer helyett FastDFAState)
            Trace<DiscreteCompositionState<FastDFAState>, XtaAction> trace = result.asUnsafe().getTrace();

            for (XtaAction action : trace.getActions()) {
                System.out.println(" -> " + action.toString());
            }
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