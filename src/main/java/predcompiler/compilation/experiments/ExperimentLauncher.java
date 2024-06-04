package predcompiler.compilation.experiments;

import predcompiler.compilation.AbstractPredicateSearch;
import predcompiler.compilation.io.JSONUtils;

public class ExperimentLauncher {

	public static void main(String[] args) {

		// Check if help command is requested or if the number of arguments is incorrect
		if (args.length != 1) {
			printUsage();
			return;
		}

		try {
			// Parse args[0] as the path to the .json config file
			String configFile = args[0];
			// Validate if the path points to a .bnf file
			if (!configFile.endsWith(".json")) {
				System.err.println("Error: The first argument must be a path to a .json file.");
				printUsage();
				return;
			}

			AbstractPredicateSearch search = JSONUtils.loadClassFromFile(configFile);
			search.initialize();

			while (search.isStillRunning()) {
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
		System.out.println("Usage: java ExperimentLauncher <jsonFilePath>");
		System.out.println("  <jsonFilePath>          : Absolute path to the .json file with the experiment's configuration.");
		System.out.println("  -help, --help           : Display this help message.");
	} // printUsage
} // ExperimentLauncher
