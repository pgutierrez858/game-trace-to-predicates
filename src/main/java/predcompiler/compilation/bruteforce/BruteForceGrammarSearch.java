package predcompiler.compilation.bruteforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.moeaframework.util.grammar.Production;
import org.moeaframework.util.grammar.Rule;
import org.moeaframework.util.grammar.Symbol;
import predcompiler.compilation.AbstractPredicateSearch;
import predcompiler.compilation.evaluation.IPredicateEvaluator;
import predcompiler.compilation.evaluation.TieredPredicateEvaluator;

public class BruteForceGrammarSearch extends AbstractPredicateSearch {

	/**
	 * Maximum number of productions that this search will look into from the root
	 * node of the grammar tree.
	 */
	private int maxDepth;

	public BruteForceGrammarSearch(String grammarPath, String tracesPath, int maxDepth) throws IOException {
		super(grammarPath, tracesPath);
		this.maxDepth = maxDepth;
	} // BruteForceGrammarSearch

	public HashSet<String> findBestPredicates(IPredicateEvaluator evaluator) {
		HashSet<String> bestPredicates = new HashSet<>();
		double bestFitness = Double.MIN_VALUE;
		Stack<GrammarTree> productionStack = new Stack<>();

		// start with the root rule in the grammar and go down from there
		Rule baseRule = grammar.get(0);
		GrammarTree rootTree = new GrammarTree(baseRule.getSymbol(), null, 0);

		// this will be the base node to process in the stack
		productionStack.add(rootTree);

		// keep processing nodes until we have a complete tree
		while (!productionStack.isEmpty()) {
			// get next tree to process.
			// i.e. <expression> [0]
			GrammarTree currentTree = productionStack.pop();

			if (currentTree.rootSymbol.isTerminal()) {
				// terminal symbol found, meaning it has no further productions to work with
				continue;
			}
			// populate parent tree children with possible productions
			// i.e. <expression> [0] -> { <literal> [1] }, { <compound> [1] }
			// those productions will be added to the production stack for future
			// processing.
			Rule currentRule = grammar.get(currentTree.rootSymbol);
			for (int i = 0; i < currentRule.size(); i++) {
				// a production may be divided into an array of symbols, each
				// of which needs to be processed individually.
				// i.e. { <expression> <binary> <expression> }
				// we call these nested symbols production sections
				Production nextProduction = currentRule.get(i);
				List<GrammarTree> productionSections = new ArrayList<>();

				for (int j = 0; j < nextProduction.size(); j++) {
					// in the example, next element from { <expression> <binary> <expression> }
					Symbol nextSymbol = nextProduction.get(j);

					// only allow new expansions if we have yet to reach max depth.
					if (currentTree.depth < maxDepth) {
						GrammarTree prodTree = new GrammarTree(nextSymbol, currentTree, currentTree.depth + 1);
						productionSections.add(prodTree);
						productionStack.add(prodTree);
					}
				}
				// add the section to the current tree node, assuming it's not empty
				if (productionSections.size() > 0)
					currentTree.productions.add(productionSections);
			}
		}

		for (String opt : rootTree.getTerminals()) {
			double fitness = evaluator.evaluatePredicate(opt, exampleTraces, counterExampleTraces);
			if (fitness > bestFitness) {
				bestFitness = fitness;
				bestPredicates = new HashSet<>();
				bestPredicates.add(opt);
			} else if (fitness == bestFitness) {
				bestPredicates.add(opt);
			}
		}
		return bestPredicates;
	} // findBestPredicates

	public static void main(String[] args) {

		// Check if help command is requested or if the number of arguments is incorrect
		if (args.length != 3 || args[0].equalsIgnoreCase("-help") || args[0].equalsIgnoreCase("--help")) {
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

			// Parse args[2] as the maximum depth parameter
			int maxDepth;
			try {
				maxDepth = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.err.println("Error: The third argument must be an integer representing the maximum depth.");
				printUsage();
				return;
			}

			BruteForceGrammarSearch search = new BruteForceGrammarSearch(bnfFilePath, systemFolderPath, maxDepth);
			HashSet<String> bestPredicates = search.findBestPredicates(new TieredPredicateEvaluator());
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
		System.out.println("  <bnfFilePath>        : Absolute path to the .bnf file.");
		System.out.println("  <systemFolderPath>   : Absolute path to the system folder containing traces.");
		System.out.println("  <maxDepth>           : Maximum depth parameter of the search algorithm.");
		System.out.println("  -help, --help        : Display this help message.");
	} // printUsage

} // BruteForceGrammarSearch
