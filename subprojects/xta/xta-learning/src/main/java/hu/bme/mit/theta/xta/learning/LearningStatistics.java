package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.analysis.algorithm.Statistics;

public final class LearningStatistics extends Statistics {

    private final int hypothesisStates;
    private final int productDfaStates;
    private final int alphabetSize;
    private final int totalRefinements;
    private final int outerIterations;
    private final int cegarIterations;
    private final long totalMqQueries;
    private final long totalSulRuns;
    private final long totalEqQueries;
    private final long totalMs;
    private final long setupMs;
    private final long learningMs;
    private final long modelCheckingMs;
    private final long refinerMs;
    private final long realizabilityMs;

    public LearningStatistics(
            final int hypothesisStates,
            final int productDfaStates,
            final int alphabetSize,
            final int totalRefinements,
            final int outerIterations,
            final int cegarIterations,
            final long totalMqQueries,
            final long totalSulRuns,
            final long totalEqQueries,
            final long totalMs,
            final long setupMs,
            final long learningMs,
            final long modelCheckingMs,
            final long refinerMs,
            final long realizabilityMs) {
        this.hypothesisStates = hypothesisStates;
        this.productDfaStates = productDfaStates;
        this.alphabetSize = alphabetSize;
        this.totalRefinements = totalRefinements;
        this.outerIterations = outerIterations;
        this.cegarIterations = cegarIterations;
        this.totalMqQueries = totalMqQueries;
        this.totalSulRuns = totalSulRuns;
        this.totalEqQueries = totalEqQueries;
        this.totalMs = totalMs;
        this.setupMs = setupMs;
        this.learningMs = learningMs;
        this.modelCheckingMs = modelCheckingMs;
        this.refinerMs = refinerMs;
        this.realizabilityMs = realizabilityMs;

        addStat("HypothesisStates", this::hypothesisStates);
        addStat("CompositionDfaStates", this::productDfaStates);
        addStat("AlphabetSize", this::alphabetSize);
        addStat("Refinements", this::totalRefinements);
        addStat("OuterIterations", this::outerIterations);
        addStat("CegarIterations", this::cegarIterations);
        addStat("MembershipQueries", this::totalMqQueries);
        addStat("SulRuns", this::totalSulRuns);
        addStat("EquivalenceQueries", this::totalEqQueries);
        addStat("TotalTimeMs", this::totalMs);
        addStat("SetupTimeMs", this::setupMs);
        addStat("LearningTimeMs", this::learningMs);
        addStat("ModelCheckingTimeMs", this::modelCheckingMs);
        addStat("RefinerTimeMs", this::refinerMs);
        addStat("RealizabilityTimeMs", this::realizabilityMs);
    }

    public int hypothesisStates() {
        return hypothesisStates;
    }

    public int productDfaStates() {
        return productDfaStates;
    }

    public int alphabetSize() {
        return alphabetSize;
    }

    public int totalRefinements() {
        return totalRefinements;
    }

    public int outerIterations() {
        return outerIterations;
    }

    public int cegarIterations() {
        return cegarIterations;
    }

    public long totalMqQueries() {
        return totalMqQueries;
    }

    public long totalSulRuns() {
        return totalSulRuns;
    }

    public long totalEqQueries() {
        return totalEqQueries;
    }

    public long totalMs() {
        return totalMs;
    }

    public long setupMs() {
        return setupMs;
    }

    public long learningMs() {
        return learningMs;
    }

    public long modelCheckingMs() {
        return modelCheckingMs;
    }

    public long refinerMs() {
        return refinerMs;
    }

    public long realizabilityMs() {
        return realizabilityMs;
    }
}
