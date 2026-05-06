package hu.bme.mit.theta.xta.learning;

public record LearningStatistics(
        int hypothesisStates,
        int productDfaStates,
        int totalRefinements,
        int outerIterations,
        long totalMqQueries
) {}
