package predcompiler.compilation.grammarevolution;

import java.io.IOException;
import java.util.HashSet;
import org.moeaframework.algorithm.single.GeneticAlgorithm;
import org.moeaframework.core.PRNG;

import predcompiler.compilation.AbstractPredicateSearch;
import predcompiler.compilation.evaluation.IPredicateEvaluator;
import predcompiler.compilation.evaluation.TieredPredicateEvaluator;
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

	public GrammarEvolutionGrammarSearch(String grammarPath, String tracesPath, int initialPopulationSize,
			int maxGenerations) throws IOException {
		super(grammarPath, tracesPath);
		this.initialPopulationSize = initialPopulationSize;
		this.maxGenerations = maxGenerations;
	} // GrammarEvolutionGrammarSearch

	public HashSet<String> findBestPredicates(IPredicateEvaluator evaluator) {
		HashSet<String> result = new HashSet<>();
		PRNG.setRandom(SynchronizedMersenneTwister.getInstance());

		try {
			// setup and construct the GP solver
			var problem = new GrammarRegression(exampleTraces, counterExampleTraces, grammar,
					new TieredPredicateEvaluator());
			GeneticAlgorithm algorithm = new GeneticAlgorithm(problem);
			algorithm.setInitialPopulationSize(initialPopulationSize);
			int generation = 0;

			try {
				// run the GP solver
				while ((generation < maxGenerations)) {
					algorithm.step();
					generation++;
				}
			} finally {
				if (algorithm != null) {
					algorithm.terminate();
				}

				result.add(problem.getPredicate(algorithm.getResult().get(0)));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	} // findBestPredicates

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

			GrammarEvolutionGrammarSearch search = new GrammarEvolutionGrammarSearch(bnfFilePath, systemFolderPath,
					initialPopulationSize, maxGenerations);
			float[] resourceChecks = new float[] { 0, 1, 2, 3, 4, 5 };
			String[] resourceNames = new String[] { "wood", "iron", "axe" };
			for (String name : resourceNames) {
				for (float check : resourceChecks) {
					search.addMapping(new LessThanMapping(name, check, 0, 5));
				}
			}
			search.initialize();
			HashSet<String> bestPredicates = search.runSearch(new TieredPredicateEvaluator());
			for (var pred : bestPredicates) {
				System.out.println(pred);
			}
		} catch (Exception e) {
			System.err.println("Error: An unexpected error occurred while parsing arguments.");
			e.printStackTrace();
			printUsage();
		}

	} // main

	private static void printUsage() {
		System.out.println("Usage: java Main <bnfFilePath> <systemFolderPath> <maxDepth>");
		System.out.println("  <bnfFilePath>           : Absolute path to the .bnf file.");
		System.out.println("  <systemFolderPath>      : Absolute path to the system folder containing traces.");
		System.out.println("  <initialPopulationSize> : Number of individuals to be included in the population.");
		System.out.println("  <maxGenerations>        : Maximum number of population evolutions.");
		System.out.println("  -help, --help           : Display this help message.");
	} // printUsage
} // GrammarEvolutionGrammarSearch
