/*
 *  Copyright 2026 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.analysis.algorithm.compositional;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.compositional.learning.AssumptionLearner;
import hu.bme.mit.theta.analysis.dfa.ActionToSymbolCollector;
import hu.bme.mit.theta.analysis.dfa.DfaAnalysis;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;

/**
 * Compositional verification by assumption learning. The model is split into a hard part T, whose
 * language is learned as an assumption H, and an easy part A, which is checked composed with H.
 * The asymmetric rule L(T) ⊆ L(H) and A ∥ H ⊨ P gives A ∥ T ⊨ P.
 *
 * <p>Each outer iteration learns H until the equivalence oracle accepts it, checks A ∥ H with the
 * checker built for H, and analyses a counterexample: a realizable one is a violation, an
 * imprecise one refines the hard part and restarts the learner, which gets the counterexample right
 * away if the refined hard part excludes it, a spurious one is excluded from H.
 * The checker of A ∥ H must only report counterexamples that are feasible in A ∥ H.
 */
public final class CompositionalChecker<S extends State, A extends Action, I> implements SafetyChecker<S, A, UnitPrec> {

    private final LearnedComponent<A, I> initialComponent;
    private final Function<? super DfaAnalysis<FastDFAState, A, I>, ? extends SafetyChecker<S, A, UnitPrec>> checkerFactory;
    private final AssumptionLearner.LearningAlgorithmType learningAlgorithmType;
    private final Logger logger;

    private CompositionalChecker(
            final LearnedComponent<A, I> initialComponent,
            final Function<? super DfaAnalysis<FastDFAState, A, I>, ? extends SafetyChecker<S, A, UnitPrec>> checkerFactory,
            final AssumptionLearner.LearningAlgorithmType learningAlgorithmType,
            final Logger logger) {
        this.initialComponent = checkNotNull(initialComponent);
        this.checkerFactory = checkNotNull(checkerFactory);
        this.learningAlgorithmType = checkNotNull(learningAlgorithmType);
        this.logger = checkNotNull(logger);
    }

    public static <S extends State, A extends Action, I> CompositionalChecker<S, A, I> create(
            final LearnedComponent<A, I> component,
            final Function<? super DfaAnalysis<FastDFAState, A, I>, ? extends SafetyChecker<S, A, UnitPrec>> checkerFactory,
            final AssumptionLearner.LearningAlgorithmType learningAlgorithmType) {
        return new CompositionalChecker<>(component, checkerFactory, learningAlgorithmType, NullLogger.getInstance());
    }

    public static <S extends State, A extends Action, I> CompositionalChecker<S, A, I> create(
            final LearnedComponent<A, I> component,
            final Function<? super DfaAnalysis<FastDFAState, A, I>, ? extends SafetyChecker<S, A, UnitPrec>> checkerFactory,
            final AssumptionLearner.LearningAlgorithmType learningAlgorithmType,
            final Logger logger) {
        return new CompositionalChecker<>(component, checkerFactory, learningAlgorithmType, logger);
    }

    @Override
    public SafetyResult<S, A> check(final UnitPrec prec) {
        final long start = System.currentTimeMillis();
        long learningTime = 0;
        long modelCheckingTime = 0;
        long cexAnalysisTime = 0;
        int componentRefinements = 0;
        int hypothesisRefinements = 0;
        int membershipQueries = 0;
        int equivalenceQueries = 0;

        LearnedComponent<A, I> component = initialComponent;
        AssumptionLearner<I> learner = AssumptionLearner.create(learningAlgorithmType, component.getMembershipOracle());
        int iteration = 0;

        while (true) {
            ++iteration;
            logger.write(Logger.Level.MAINSTEP, "Outer iteration %d%n", iteration);

            final long learningStart = System.currentTimeMillis();
            final ActionToSymbolCollector<A, I> actionToSymbol =
                    ActionToSymbolCollector.create(component.getActionToSymbol(), learner.getAlphabet());
            final DfaAnalysis<FastDFAState, A, I> assumption;
            final int assumptionStates;
            if (learner.isStarted()) {
                learner.learn(component.getEquivalenceOracle());
                final FastDFA<I> dfa = learner.getAssumption();
                assumption = DfaAnalysis.create(dfa, learner.getAlphabet(), actionToSymbol);
                assumptionStates = dfa.size();
            } else {
                assumption = DfaAnalysis.universal(actionToSymbol);
                assumptionStates = 1;
            }
            learningTime += System.currentTimeMillis() - learningStart;
            logger.write(Logger.Level.SUBSTEP, "| Assumption: %d state(s), %d symbol(s), %d MQ, %d EQ so far%n",
                    assumptionStates, learner.getAlphabet().size(), learner.getMembershipQueries(),
                    learner.getEquivalenceQueries());

            final long checkStart = System.currentTimeMillis();
            final SafetyResult<S, A> result = checkerFactory.apply(assumption).check(UnitPrec.getInstance());
            modelCheckingTime += System.currentTimeMillis() - checkStart;
            logger.write(Logger.Level.SUBSTEP, "| Checking the composition done, result: %s%n", result);

            final Map<I, A> discovered = actionToSymbol.getDiscoveredSymbols();
            if (!discovered.isEmpty()) {
                learner.addSymbols(discovered.keySet());
            }

            if (result.isSafe()) {
                return SafetyResult.safe(result.getArg(), new CompositionalStatistics(
                        System.currentTimeMillis() - start, learningTime, modelCheckingTime, cexAnalysisTime,
                        iteration, hypothesisRefinements + learner.getRefinements(), componentRefinements,
                        membershipQueries + learner.getMembershipQueries(),
                        equivalenceQueries + learner.getEquivalenceQueries(),
                        learner.getAlphabet().size(), assumptionStates));
            }

            final long analysisStart = System.currentTimeMillis();
            final List<A> cexActions = result.asUnsafe().getTrace().getActions();
            final CexStatus<A, I> status = component.checkCex(cexActions);
            cexAnalysisTime += System.currentTimeMillis() - analysisStart;
            logger.write(Logger.Level.SUBSTEP, "| Counterexample analysis done, result: %s%n", status);

            if (status.isRealizable()) {
                return SafetyResult.unsafe(result.asUnsafe().getTrace(), result.getArg(), new CompositionalStatistics(
                        System.currentTimeMillis() - start, learningTime, modelCheckingTime, cexAnalysisTime,
                        iteration, hypothesisRefinements + learner.getRefinements(), componentRefinements,
                        membershipQueries + learner.getMembershipQueries(),
                        equivalenceQueries + learner.getEquivalenceQueries(),
                        learner.getAlphabet().size(), assumptionStates));
            } else if (status.isImprecise()) {
                hypothesisRefinements += learner.getRefinements();
                membershipQueries += learner.getMembershipQueries();
                equivalenceQueries += learner.getEquivalenceQueries();
                ++componentRefinements;
                component = status.asImprecise().getRefinedComponent();
                learner = AssumptionLearner.create(learningAlgorithmType, component.getMembershipOracle());

                final long refinedAnalysisStart = System.currentTimeMillis();
                final CexStatus<A, I> refinedStatus = component.checkCex(cexActions);
                cexAnalysisTime += System.currentTimeMillis() - refinedAnalysisStart;
                logger.write(Logger.Level.SUBSTEP, "| Counterexample with the refined component: %s%n", refinedStatus);
                if (refinedStatus.isSpurious()) {
                    final Word<I> word = refinedStatus.asSpurious().getWord();
                    learner.addSymbols(word.asList());
                    learner.refine(word);
                }
            } else {
                learner.refine(status.asSpurious().getWord());
            }
        }
    }
}
