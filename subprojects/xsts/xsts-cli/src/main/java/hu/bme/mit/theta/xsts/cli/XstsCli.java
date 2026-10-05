package hu.bme.mit.theta.xsts.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarStatistics;
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.analysis.utils.TraceVisualizer;
import hu.bme.mit.theta.common.CliUtils;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.common.table.BasicTableWriter;
import hu.bme.mit.theta.common.table.TableWriter;
import hu.bme.mit.theta.common.visualization.Graph;
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import hu.bme.mit.theta.core.type.clocktype.ClockType;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.XstsAction;
import hu.bme.mit.theta.xsts.analysis.XstsState;
import hu.bme.mit.theta.xsts.analysis.concretizer.XstsStateSequence;
import hu.bme.mit.theta.xsts.analysis.concretizer.XstsTraceConcretizerUtil;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfig;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfigBuilder;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfigBuilder.*;
import hu.bme.mit.theta.xsts.analysis.config.TimedXstsConfigBuilder;
import hu.bme.mit.theta.xsts.analysis.config.TimedXstsConfigBuilder.ControlFlowSplitting;
import hu.bme.mit.theta.xsts.analysis.config.TimedXstsConfigBuilder.ZoneRefinement;
import hu.bme.mit.theta.xsts.dsl.XstsDslManager;
import hu.bme.mit.theta.xsts.learning.LearningStatistics;
import hu.bme.mit.theta.xsts.learning.XstsLearningCheckerConfig;
import hu.bme.mit.theta.xsts.learning.XstsLearningCheckerConfigFactory;
import hu.bme.mit.theta.xsts.learning.learnalgo.LearningAlgorithmType;
import hu.bme.mit.theta.xsts.learning.modelchecking.XstsCompositionTraces;
import hu.bme.mit.theta.xsts.pnml.PnmlParser;
import hu.bme.mit.theta.xsts.pnml.PnmlToXSTS;
import hu.bme.mit.theta.xsts.pnml.elements.PnmlNet;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

public class XstsCli {
	private static final String JAR_NAME = "theta-xsts-cli.jar";
	private final String[] args;
	private final TableWriter writer;

	enum AlgorithmType {
		/**
		 * CEGAR pipeline
		 * Lazy abstraction when the model has clocks
		 */
		CEGAR,
		/**
		 * Compositional verification with automata learning.
		 * TXSTS split into an XSTS and a clock alphabet, and a DFA over that alphabet is learned
		 */
		LEARNING
	}

	@Parameter(names = {"--algorithm"}, description = "Verification Algorithm type")
	AlgorithmType algorithm = AlgorithmType.CEGAR;


	@Parameter(names = {"--domain"}, description = "Abstract domain")
	Domain domain = Domain.PRED_CART;

	@Parameter(names = {"--refinement"}, description = "Refinement strategy")
	Refinement refinement = Refinement.SEQ_ITP;

	@Parameter(names = {"--search"}, description = "Search strategy")
	Search search = Search.BFS;

	@Parameter(names = {"--predsplit"}, description = "Predicate splitting")
	PredSplit predSplit = PredSplit.WHOLE;

	@Parameter(names = {"--model"}, description = "Path of the input XSTS model", required = true)
	String model;

	@Parameter(names = {"--property"}, description = "Input property as a string or a file (*.prop)", required = true)
	String property;

	@Parameter(names = {"--initialmarking"}, description = "Initial marking of the Petri net")
	String initialMarking="";

	@Parameter(names = "--maxenum", description = "Maximal number of explicitly enumerated successors (0: unlimited)")
	Integer maxEnum = 0;

	@Parameter(names = "--autoexpl", description = "Predicate to explicit switching strategy")
 	AutoExpl autoExpl = AutoExpl.NEWOPERANDS;

	@Parameter(names = {"--initprec"}, description = "Initial precision")
	InitPrec initPrec = InitPrec.EMPTY;

	@Parameter(names = "--prunestrategy", description = "Strategy for pruning the ARG after refinement")
	PruneStrategy pruneStrategy = PruneStrategy.LAZY;

	@Parameter(names = "--optimizestmts", description = "Turn statement optimization on or off")
	OptimizeStmts optimizeStmts = OptimizeStmts.ON;

	@Parameter(names = {"--loglevel"}, description = "Detailedness of logging")
	Logger.Level logLevel = Logger.Level.SUBSTEP;

	@Parameter(names = {"--benchmark"}, description = "Benchmark mode (only print metrics)")
	Boolean benchmarkMode = false;

	@Parameter(names = {"--cex"}, description = "Write concrete counterexample to a file")
	String cexfile = null;

	@Parameter(names = {"--header"}, description = "Print only a header (for benchmarks)", help = true)
	boolean headerOnly = false;

	@Parameter(names = "--metrics", description = "Print metrics about the XSTS without running the algorithm")
	boolean metrics = false;

	@Parameter(names = "--stacktrace", description = "Print full stack trace in case of exception")
	boolean stacktrace = false;

	@Parameter(names = "--version", description = "Display version", help = true)
	boolean versionInfo = false;

	@Parameter(names = {"--visualize"}, description = "Write proof or counterexample to file in dot format")
	String dotfile = null;

	@Parameter(names = {"--clockreplacement"}, description = "Clock replacement type")
	ClockReplacer clockReplacer = ClockReplacer.NONE;

	@Parameter(names = {"--controlflowsplitting"}, description = "Control flow splitting type")
	ControlFlowSplitting controlFlowSplitting = ControlFlowSplitting.OFF;

	@Parameter(names = {"--zonerefinement"}, description = "Refinement strategy for zone states")
	ZoneRefinement zoneRefinement = ZoneRefinement.BW_ITP;

	@Parameter(names = {"--learningalgorithm"}, description = "Learning algorithm type")
	LearningAlgorithmType learningAlgorithmType = LearningAlgorithmType.TTT;

	@Parameter(names = {"--learning-search"}, description = "Search strategy for the learning algorithm")
	SearchStrategy learningSearchStrategy = SearchStrategy.BFS;

	@Parameter(names = {"--maxouteriterations"}, description = "Maximal number of outer iterations for the learning algorithm (0: unlimited)")
	Integer maxOuterIterations = 0;

	@Parameter(names = {"--learningiterations"}, description = "Maximal number of learning iterations for the learning algorithm (0: unlimited)")
	Integer maxLearningIterations = 0;

	@Parameter(names = {"--learning-metrics"}, description = "Print metrics about the learning algorithm without running it")
	boolean learningMetrics = false;

	@Parameter(names = {"--learning-visualize"}, description = "Directory to write the learning artifacts to in dot format: the TXSTS, the timeless XSTS, the hypothesis and the final ARG")
	String learningDotDir = null;

	private XstsLearningCheckerConfig<?, ?> learningConfig;


	private Logger logger;

	public XstsCli(final String[] args) {
		this.args = args;
		writer = new BasicTableWriter(System.out, ",", "\"", "\"");
	}

	public static void main(final String[] args) {
		final XstsCli mainApp = new XstsCli(args);
		mainApp.run();
	}

	private void run() {
		try {
			JCommander.newBuilder().addObject(this).programName(JAR_NAME).build().parse(args);
			logger = benchmarkMode ? NullLogger.getInstance() : new ConsoleLogger(logLevel);
		} catch (final ParameterException ex) {
			System.out.println("Invalid parameters, details:");
			System.out.println(ex.getMessage());
			ex.usage();
			return;
		}

		if (headerOnly) {
			printHeader();
			return;
		}

		if (versionInfo) {
			CliUtils.printVersion(System.out);
			return;
		}

		try {
			final Stopwatch sw = Stopwatch.createStarted();
			final XSTS xsts = loadModel();

			if (metrics) {
				XstsMetrics.printMetrics(logger, xsts);
				return;
			}

			final XstsConfig<?, ?, ?> configuration = buildConfiguration(xsts);
			final SafetyResult<?, ?> status = check(configuration);
			sw.stop();
			printResult(status, xsts, sw.elapsed(TimeUnit.MILLISECONDS));
			if (status.isUnsafe() && cexfile != null) {
				writeCex(status.asUnsafe(), xsts);
			}
			if (dotfile != null) {
				writeVisualStatus(status, dotfile);
			}
			if (learningDotDir != null && learningConfig != null) {
				learningConfig.writeArtifacts(Path.of(learningDotDir), baseNameOf(model));
			}
		} catch (final Throwable ex) {
			printError(ex);
			System.exit(1);
		}
	}

	private SafetyResult<?, ?> check(XstsConfig<?, ?, ?> configuration) throws Exception {
		try {
			return configuration.check();
		} catch (final Exception ex) {
			String message = ex.getMessage() == null ? "(no message)" : ex.getMessage();
			throw new Exception("Error while running algorithm: " + ex.getClass().getSimpleName() + " " + message, ex);
		}
	}

	private void printHeader() {
		if (algorithm == AlgorithmType.CEGAR) {
			Stream.of("Result", "TimeMs", "AlgoTimeMs", "AbsTimeMs", "RefTimeMs", "Iterations",
					"ArgSize", "ArgDepth", "ArgMeanBranchFactor", "CexLen", "Vars").forEach(writer::cell);
			writer.newRow();
		}
		if (algorithm == AlgorithmType.LEARNING) {
			Stream.of("Result", "TimeMs", "AlgoTimeMs",
							"SetupMs", "LearningMs", "ModelCheckMs", "RealizMs",
							"OuterIters", "Refinements", "Alphabet", "HypStates", "ProdDfaStates",
							"CegarIterations", "RefinerTimeMs", "MembQueries", "EquivalenceQueries",
							"ArgSize", "ArgDepth", "ArgMeanBranchFactor", "CexLen", "Vars")
					.forEach(writer::cell);
			writer.newRow();
		}
	}

	private XSTS loadModel() throws Exception {
		InputStream propStream = null;
		try {
			if (property.endsWith(".prop")) propStream = new FileInputStream(property);
			else propStream = new ByteArrayInputStream(("prop { " + property + " }").getBytes());

			if (model.endsWith(".pnml")) {
				final PnmlNet pnmlNet = PnmlParser.parse(model,initialMarking);
				return PnmlToXSTS.createXSTS(pnmlNet, propStream);
			} else {

				try (SequenceInputStream inputStream = new SequenceInputStream(new FileInputStream(model), propStream)) {
					return XstsDslManager.createXsts(inputStream);
				}
			}

		} catch (Exception ex) {
			throw new Exception("Could not parse XSTS: " + ex.getMessage(), ex);
		} finally {
			if (propStream != null) propStream.close();
		}
	}

	private XstsConfig<?, ?, ?> buildConfiguration(final XSTS xsts) throws Exception {
		try {
			if (algorithm == AlgorithmType.LEARNING) {
				return buildLearningConfiguration(xsts);
			}
			XstsConfigBuilder xstsConfigBuilder = new XstsConfigBuilder(domain, refinement, Z3SolverFactory.getInstance())
					.maxEnum(maxEnum).autoExpl(autoExpl).initPrec(initPrec).pruneStrategy(pruneStrategy)
					.search(search).predSplit(predSplit).optimizeStmts(optimizeStmts).clockReplacer(clockReplacer)
					.logger(logger);
			final boolean timedXsts = clockReplacer == ClockReplacer.NONE &&
					xsts.getVars().stream().anyMatch(varDecl -> varDecl.getType() instanceof ClockType);
			if (timedXsts) {
				return new TimedXstsConfigBuilder(xstsConfigBuilder)
						.zoneRefinement(zoneRefinement).controlFlowSplitting(controlFlowSplitting)
						.build(xsts);
			} else {
				return xstsConfigBuilder.build(xsts);
			}
		} catch (final Exception ex) {
			throw new Exception("Could not create configuration: " + ex.getMessage(), ex);
		}
	}

	private XstsConfig<?, ?, ?> buildLearningConfiguration(final XSTS xsts) {
		checkArgument(clockReplacer == ClockReplacer.NONE,
				"--algorithm LEARNING works on the clocks themselves: pass --clockreplacement NONE");
		checkArgument(xsts.getVars().stream().anyMatch(v -> v.getType() instanceof ClockType),
				"--algorithm LEARNING needs a model with clocks: this one has none");

		learningConfig = XstsLearningCheckerConfigFactory
				.create(xsts, Z3SolverFactory.getInstance(), domain)
				.learningAlgorithmType(learningAlgorithmType)
				.searchStrategy(learningSearchStrategy)
				.refinement(XstsLearningCheckerConfigFactory.Refinement.valueOf(refinement.name()))
				.autoExpl(autoExpl)
				.predSplit(XstsLearningCheckerConfigFactory.PredSplit.valueOf(predSplit.name()))
				.pruneStrategy(pruneStrategy)
				.maxEnum(maxEnum)
				.maxOuterIterations(unlimitedIfZero(maxOuterIterations))
				.maxLearningIterations(unlimitedIfZero(maxLearningIterations))
				.logger(logger)
				.build();
		return learningConfig.asXstsConfig();
	}

	private static String baseNameOf(final String modelPath) {
		final String fileName = new File(modelPath).getName();
		final int dot = fileName.lastIndexOf('.');
		return dot < 0 ? fileName : fileName.substring(0, dot);
	}

	private static int unlimitedIfZero(final int limit) {
		return limit == 0 ? Integer.MAX_VALUE : limit;
	}

	private void printResult(final SafetyResult<?, ?> status, final XSTS sts, final long totalTimeMs) {
		switch (algorithm) {
			case CEGAR:
				final CegarStatistics stats = (CegarStatistics) status.getStats().get();
				if (benchmarkMode) {
					writer.cell(status.isSafe());
					writer.cell(totalTimeMs);
					writer.cell(stats.getAlgorithmTimeMs());
					writer.cell(stats.getAbstractorTimeMs());
					writer.cell(stats.getRefinerTimeMs());
					writer.cell(stats.getIterations());
					writer.cell(status.getArg().size());
					writer.cell(status.getArg().getDepth());
					writer.cell(status.getArg().getMeanBranchingFactor());
					if (status.isUnsafe()) {
						writer.cell(status.asUnsafe().getTrace().length() + "");
					} else {
						writer.cell("");
					}
					writer.cell(sts.getVars().size());
					writer.newRow();
				}
				break;
			case LEARNING:
				final LearningStatistics learningStats = (LearningStatistics) status.getStats().get();
				if (benchmarkMode) {
					writer.cell(status.isSafe());
					writer.cell(totalTimeMs);
					writer.cell(learningStats.getAlgorithmTimeMs());
					writer.cell(learningStats.getSetupMs());
					writer.cell(learningStats.getLearningTimeMs());
					writer.cell(learningStats.getModelCheckingTimeMs());
					writer.cell(learningStats.getRealizabilityTimeMs());
					writer.cell(learningStats.getOuterIterations());
					writer.cell(learningStats.getRefinements());
					writer.cell(learningStats.getAlphabetSize());
					writer.cell(learningStats.getHypothesisStates());
					writer.cell(learningStats.getProductDfaStates());
					writer.cell(learningStats.getIterations());
					writer.cell(learningStats.getRefinerTimeMs());
					writer.cell(learningStats.getMembershipQueries());
					writer.cell(learningStats.getEquivalenceQueries());
					writer.cell(status.getArg().size());
					writer.cell(status.getArg().getDepth());
					writer.cell(status.getArg().getMeanBranchingFactor());
					if (status.isUnsafe()) {
						writer.cell(status.asUnsafe().getTrace().length() + "");
					} else {
						writer.cell("");
					}
					writer.cell(sts.getVars().size());
					writer.newRow();
				}
				break;
		}
	}

	private void printError(final Throwable ex) {
		final String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (benchmarkMode) {
			writer.cell("[EX] " + ex.getClass().getSimpleName() + ": " + message);
			writer.newRow();
		} else {
			logger.write(Logger.Level.RESULT, "%s occurred, message: %s%n", ex.getClass().getSimpleName(), message);
			if (stacktrace) {
				final StringWriter errors = new StringWriter();
				ex.printStackTrace(new PrintWriter(errors));
				logger.write(Logger.Level.RESULT, "Trace:%n%s%n", errors.toString());
			} else {
				logger.write(Logger.Level.RESULT, "Use --stacktrace for stack trace%n");
			}
		}
	}

	private void writeCex(final SafetyResult.Unsafe<?, ?> status, final XSTS xsts) throws FileNotFoundException {
		@SuppressWarnings("unchecked") final Trace<XstsState<?>, XstsAction> trace =
				XstsCompositionTraces.projectToData(status.getTrace())
						.orElseGet(() -> (Trace<XstsState<?>, XstsAction>) status.getTrace());
		final XstsStateSequence concrTrace = XstsTraceConcretizerUtil.concretize(trace, Z3SolverFactory.getInstance(), xsts);
		final File file = new File(cexfile);
		try (PrintWriter printWriter = new PrintWriter(file)) {
			printWriter.write(concrTrace.toString());
		}
	}

	private void writeVisualStatus(final SafetyResult<?, ?> status, final String filename)
			throws FileNotFoundException {
		final Graph graph = status.isSafe() ? ArgVisualizer.getDefault().visualize(status.asSafe().getArg())
				: TraceVisualizer.getDefault().visualize(status.asUnsafe().getTrace());
		GraphvizWriter.getInstance().writeFile(graph, filename);
	}

}
