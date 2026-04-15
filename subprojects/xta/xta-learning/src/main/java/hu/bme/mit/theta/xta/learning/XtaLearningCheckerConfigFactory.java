package hu.bme.mit.theta.xta.learning;

import hu.bme.mit.theta.xta.XtaSystem;
import hu.bme.mit.theta.xta.learning.algorithm.EQOracleType;
import hu.bme.mit.theta.xta.learning.algorithm.LearningAlgorithmType;
import hu.bme.mit.theta.xta.learning.compositional.dfa.XtaDfaCheckerStrategy;


public class XtaLearningCheckerConfigFactory {
    private final XtaSystem xtaSystem;
    private LearningAlgorithmType learningAlgorithmType = LearningAlgorithmType.TTT;
    private EQOracleType eqOracleType = EQOracleType.CHAIN_W_INCLUSION;
    private XtaDfaCheckerStrategy checkerStrategy = XtaDfaCheckerStrategy.BFS;
    private int eqMaxDepth = 2;
    private int eqRandomMinLength = 10;
    private int eqRandomMaxLength = 1000;
    private int eqRandomMaxTests = 10000;

    private XtaLearningCheckerConfigFactory(XtaSystem xtaSystem) {
        this.xtaSystem = xtaSystem;
    }

    public static XtaLearningCheckerConfigFactory create(XtaSystem xtaSystem) {
        return new XtaLearningCheckerConfigFactory(xtaSystem);
    }

    public XtaLearningCheckerConfigFactory learningAlgorithmType(LearningAlgorithmType learningAlgorithmType) {
        this.learningAlgorithmType = learningAlgorithmType;
        return this;
    }
    public XtaLearningCheckerConfigFactory eqOracleType(EQOracleType eqOracleType) {
        this.eqOracleType = eqOracleType;
        return this;
    }
    public XtaLearningCheckerConfigFactory checkerStrategy(XtaDfaCheckerStrategy checkerStrategy) {
        this.checkerStrategy = checkerStrategy;
        return this;
    }
    public XtaLearningCheckerConfigFactory eqMaxDepth(int eqMaxDepth) {
        this.eqMaxDepth = eqMaxDepth;
        return this;
    }
    public XtaLearningCheckerConfigFactory eqRandomParameters(int eqMinLength, int eqMaxLength, int eqMaxTests) {
        this.eqRandomMinLength = eqMinLength;
        this.eqRandomMaxLength = eqMaxLength;
        this.eqRandomMaxTests = eqMaxTests;
        return this;
    }

    public XtaLearningCheckerConfig build() {
        return XtaLearningCheckerConfig.create(
                xtaSystem,
                learningAlgorithmType,
                eqOracleType,
                checkerStrategy,
                eqMaxDepth,
                eqRandomMinLength,
                eqRandomMaxLength,
                eqRandomMaxTests
        );
    }
}
