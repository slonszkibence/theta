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

import hu.bme.mit.theta.analysis.algorithm.Statistics;

public final class CompositionalStatistics extends Statistics {

    private final long algorithmTimeMs;
    private final long learningTimeMs;
    private final long modelCheckingTimeMs;
    private final long cexAnalysisTimeMs;
    private final int outerIterations;
    private final int hypothesisRefinements;
    private final int componentRefinements;
    private final int membershipQueries;
    private final int equivalenceQueries;
    private final int alphabetSize;
    private final int hypothesisStates;

    public CompositionalStatistics(
            final long algorithmTimeMs,
            final long learningTimeMs,
            final long modelCheckingTimeMs,
            final long cexAnalysisTimeMs,
            final int outerIterations,
            final int hypothesisRefinements,
            final int componentRefinements,
            final int membershipQueries,
            final int equivalenceQueries,
            final int alphabetSize,
            final int hypothesisStates) {
        this.algorithmTimeMs = algorithmTimeMs;
        this.learningTimeMs = learningTimeMs;
        this.modelCheckingTimeMs = modelCheckingTimeMs;
        this.cexAnalysisTimeMs = cexAnalysisTimeMs;
        this.outerIterations = outerIterations;
        this.hypothesisRefinements = hypothesisRefinements;
        this.componentRefinements = componentRefinements;
        this.membershipQueries = membershipQueries;
        this.equivalenceQueries = equivalenceQueries;
        this.alphabetSize = alphabetSize;
        this.hypothesisStates = hypothesisStates;

        addStat("AlgorithmTimeMs", this::getAlgorithmTimeMs);
        addStat("LearningTimeMs", this::getLearningTimeMs);
        addStat("ModelCheckingTimeMs", this::getModelCheckingTimeMs);
        addStat("CexAnalysisTimeMs", this::getCexAnalysisTimeMs);
        addStat("OuterIterations", this::getOuterIterations);
        addStat("HypothesisRefinements", this::getHypothesisRefinements);
        addStat("ComponentRefinements", this::getComponentRefinements);
        addStat("MembershipQueries", this::getMembershipQueries);
        addStat("EquivalenceQueries", this::getEquivalenceQueries);
        addStat("AlphabetSize", this::getAlphabetSize);
        addStat("HypothesisStates", this::getHypothesisStates);
    }

    public long getAlgorithmTimeMs() {
        return algorithmTimeMs;
    }

    public long getLearningTimeMs() {
        return learningTimeMs;
    }

    public long getModelCheckingTimeMs() {
        return modelCheckingTimeMs;
    }

    public long getCexAnalysisTimeMs() {
        return cexAnalysisTimeMs;
    }

    public int getOuterIterations() {
        return outerIterations;
    }

    public int getHypothesisRefinements() {
        return hypothesisRefinements;
    }

    public int getComponentRefinements() {
        return componentRefinements;
    }

    public int getMembershipQueries() {
        return membershipQueries;
    }

    public int getEquivalenceQueries() {
        return equivalenceQueries;
    }

    public int getAlphabetSize() {
        return alphabetSize;
    }

    public int getHypothesisStates() {
        return hypothesisStates;
    }
}
