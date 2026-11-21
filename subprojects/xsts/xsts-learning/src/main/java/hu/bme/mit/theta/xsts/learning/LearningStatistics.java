package hu.bme.mit.theta.xsts.learning;

import hu.bme.mit.theta.analysis.algorithm.Statistics;

public final class LearningStatistics extends Statistics {
    private final long algorithmTimeMs;
    private final long learningTimeMs;
    private final long modelCheckingTimeMs;
    private final long refinerTimeMs;
    private final long realizabilityTimeMs;
    private final long membershipQueries;
    private final long equivalenceQueries;
    private final int outerIterations;
    private final int alphabetSize;
    private final int hypothesisStates;
    private final int productDfaStates;
    private final int iterations;
    private final long setupMs;
    private final int refinements;

    public LearningStatistics(
            final long algorithmTimeMs,
            final long learningTimeMs,
            final long modelCheckingTimeMs,
            final long realizabilityTimeMs,
            final long membershipQueries,
            final long equivalenceQueries,
            final int outerIterations,
            final int alphabetSize,
            final int hypothesisStates,
            final int productDfaStates,
            final int iterations,
            final long refinerTimeMs,
            final long setupMs,
            final int refinements
    ) {
        this.algorithmTimeMs = algorithmTimeMs;
        this.learningTimeMs = learningTimeMs;
        this.modelCheckingTimeMs = modelCheckingTimeMs;
        this.refinerTimeMs =  refinerTimeMs;
        this.iterations = iterations;
        this.realizabilityTimeMs = realizabilityTimeMs;
        this.membershipQueries = membershipQueries;
        this.equivalenceQueries = equivalenceQueries;
        this.outerIterations = outerIterations;
        this.hypothesisStates = hypothesisStates;
        this.alphabetSize = alphabetSize;
        this.productDfaStates = productDfaStates;
        this.setupMs = setupMs;
        this.refinements = refinements;

        addStat("AlgorithmTimeMs", this::getAlgorithmTimeMs);
        addStat("SetupTimeMs", this::getSetupMs);
        addStat("Refinements", this::getRefinements);
        addStat("LearningTimeMs", this::getLearningTimeMs);
        addStat("ModelCheckingTimeMs", this::getModelCheckingTimeMs);
        addStat("Iterations", this::getIterations);
        addStat("RefinerTimeMs", this::getRefinerTimeMs);
        addStat("RealizabilityTimeMs", this::getRealizabilityTimeMs);
        addStat("MembershipQueries", this::getMembershipQueries);
        addStat("EquivalenceQueries", this::getEquivalenceQueries);
        addStat("OuterIterations", this::getOuterIterations);
        addStat("AlphabetSize", this::getAlphabetSize);
        addStat("HypothesisStates", this::getHypothesisStates);
        addStat("ProductDfaStates", this::getProductDfaStates);
    }

    public long getAlgorithmTimeMs() {
        return algorithmTimeMs;
    }
    public long getModelCheckingTimeMs() {
        return modelCheckingTimeMs;
    }

    public long getLearningTimeMs() {
        return learningTimeMs;
    }

    public long getRealizabilityTimeMs() {
        return realizabilityTimeMs;
    }
    public long getMembershipQueries() {
        return membershipQueries;
    }
    public long getEquivalenceQueries() {
        return equivalenceQueries;
    }
    public int getOuterIterations() {
        return outerIterations;
    }
    public int getAlphabetSize() {
        return alphabetSize;
    }
    public int getHypothesisStates() {
        return hypothesisStates;
    }
    public int getProductDfaStates() {
        return productDfaStates;
    }
    public int getIterations() {
        return iterations;
    }
    public long getRefinerTimeMs() {
        return refinerTimeMs;
    }
    public long getSetupMs() {
        return setupMs;
    }

    public int getRefinements() {
        return refinements;
    }
}
