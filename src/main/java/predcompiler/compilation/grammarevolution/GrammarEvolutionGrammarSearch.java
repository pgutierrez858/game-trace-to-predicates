package predcompiler.compilation.grammarevolution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.moeaframework.algorithm.single.GeneticAlgorithm;
import org.moeaframework.core.PRNG;

import predcompiler.compilation.AbstractPredicateSearch;
import predcompiler.compilation.evaluation.IPredicateEvaluator;
import predcompiler.compilation.evaluation.TieredPredicateEvaluator;
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping;
import predcompiler.compilation.evaluation.statevariables.LessThanMapping;

public class GrammarEvolutionGrammarSearch extends AbstractPredicateSearch {

	/**
	 * Number of individuals that will be included in the first population of the
	 * grammar evolution algorithm.
	 */
	private int initialPopulationSize;
	/**
	 * Maximum number of population iterations that the algorithm will go through.
	 */
	private int maxGenerations;

	/**
	 * Index of the current evolution generation
	 */
	private int generation;

	/**
	 * Reference to our problem solver algorithm.
	 */
	private GeneticAlgorithm algorithm;

	/**
	 * Reference to the problem that we are trying to solve.
	 */
	private GrammarRegression problem;

	public GrammarEvolutionGrammarSearch(String grammarPath, String tracesPath, IPredicateEvaluator evaluator,
			AbstractStateToRobustnessMapping[] mappings, int initialPopulationSize, int maxGenerations)
			throws IOException {
		super(grammarPath, tracesPath, evaluator, mappings);
		this.initialPopulationSize = initialPopulationSize;
		this.maxGenerations = maxGenerations;
	} // GrammarEvolutionGrammarSearch

	public void initialize() throws IOException {
		super.initialize();

		PRNG.setRandom(SynchronizedMersenneTwister.getInstance());

		// setup and construct the GP solver
		problem = new GrammarRegression(exampleTraces, counterExampleTraces, grammar, new TieredPredicateEvaluator());
		algorithm = new GeneticAlgorithm(problem);
		algorithm.setInitialPopulationSize(initialPopulationSize);
		generation = 0;
	} // initialize

	public void stepSearch() {
		if (generation >= maxGenerations)
			return;

		algorithm.step();

		for (var solution : algorithm.getResult()) {
			bestSolutions.add(problem.getPredicate(solution));
		}
		// record the fitness of any of the members from the solution set
		// (note that there will always be at least one)
		bestFitness = (float) algorithm.getResult().get(0).getObjective(0);

		generation++;

		if (generation >= maxGenerations) {
			algorithm.terminate();
		}
	} // step

	public static void main(String[] args) {

		// Check if help command is requested or if the number of arguments is incorrect
		if (args.length != 4 || args[0].equalsIgnoreCase("-help") || args[0].equalsIgnoreCase("--help")) {
			printUsage();
			return;
		}

		try {
			// Parse args[0] as the path to the .bnf file
			String bnfFilePath = args[0];
			// Validate if the path points to a .bnf file
			if (!bnfFilePath.endsWith(".bnf")) {
				System.err.println("Error: The first argument must be a path to a .bnf file.");
				printUsage();
				return;
			}

			// Parse args[1] as the path to the system folder
			String systemFolderPath = args[1];
			// Validate if the path is a directory
			java.io.File folder = new java.io.File(systemFolderPath);
			if (!folder.isDirectory()) {
				System.err.println("Error: The second argument must be a path to a directory.");
				printUsage();
				return;
			}

			// Parse args[2] as the initialPopulationSize parameter
			int initialPopulationSize;
			try {
				initialPopulationSize = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.err.println(
						"Error: The third argument must be an integer representing the initial Population Size.");
				printUsage();
				return;
			}

			// Parse args[3] as the maxGenerations parameter
			int maxGenerations;
			try {
				maxGenerations = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				System.err.println("Error: The third argument must be an integer representing the max Generations.");
				printUsage();
				return;
			}

			// sample mappings setting for testing
			List<AbstractStateToRobustnessMapping> mappings = new ArrayList<>();
			float[] resourceChecks = new float[] { 1, 2, 3, 4, 5 };
			String[] resourceNames = new String[] { "wood", "iron", "axe" };
			for (String name : resourceNames) {
				for (float check : resourceChecks) {
					mappings.add(new LessThanMapping(name, check, 0, 5));
				}
			}
			GrammarEvolutionGrammarSearch search = new GrammarEvolutionGrammarSearch(bnfFilePath, systemFolderPath,
					new TieredPredicateEvaluator(), (AbstractStateToRobustnessMapping[]) mappings.toArray(),
					initialPopulationSize, maxGenerations);

			search.initialize();

			while (!search.isTerminated()) {
				search.step();
				search.printStepResults();
			}
		} catch (Exception e) {
			System.err.println("Error: An unexpected error occurred while parsing arguments.");
			e.printStackTrace();
			printUsage();
		}

	} // main

	private static void printUsage() {
		System.out.println(
				"Usage: java GrammarEvolutionGrammarSearch <bnfFilePath> <systemFolderPath> <initialPopulationSize> <maxGenerations>");
		System.out.println("  <bnfFilePath>           : Absolute path to the .bnf file.");
		System.out.println("  <systemFolderPath>      : Absolute path to the system folder containing traces.");
		System.out.println("  <initialPopulationSize> : Number of individuals to be included in the population.");
		System.out.println("  <maxGenerations>        : Maximum number of population evolutions.");
		System.out.println("  -help, --help           : Display this help message.");
	} // printUsage
} // GrammarEvolutionGrammarSearch
