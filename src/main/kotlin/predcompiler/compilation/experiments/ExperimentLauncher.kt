package predcompiler.compilation.experiments

import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.io.JSONUtils

fun main(args: Array<String>) {
    // Check if help command is requested or if the number of arguments is incorrect
    if (args.size != 1) {
        printUsage()
        return
    }

    try {
        // Parse args[0] as the path to the .json config file
        val configFile = args[0]
        // Validate if the path points to a .bnf file
        if (!configFile.endsWith(".json")) {
            System.err.println("Error: The first argument must be a path to a .json file.")
            printUsage()
            return
        }

        val search = JSONUtils.loadClassFromFile<AbstractPredicateSearch>(configFile)

        while (search.isStillRunning) {
            search.step()
            search.printStepResults()
        }
    } catch (e: Exception) {
        System.err.println("Error: An unexpected error occurred while parsing arguments.")
        e.printStackTrace()
        printUsage()
    }
} // main

private fun printUsage() {
    println("Usage: java ExperimentLauncher <jsonFilePath>")
    println("  <jsonFilePath>          : Absolute path to the .json file with the experiment's configuration.")
    println("  -help, --help           : Display this help message.")
} // printUsage
