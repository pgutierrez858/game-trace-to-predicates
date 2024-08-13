package predcompiler.compilation.experiments

import predcompiler.compilation.evaluation.evaluators.predicate.IPredicateEvaluator
import predcompiler.compilation.evaluation.evaluators.trace.AutomatonTraceEvaluator
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping
import predcompiler.compilation.io.loadClassFromFile
import predcompiler.compilation.io.populateBlockLiteralProductions
import predcompiler.compilation.io.readGrammar
import predcompiler.compilation.io.readTraces
import predcompiler.compilation.mcts.BasicMCTSSearch
import kotlin.io.path.Path

data class ExperimentConfig(
    val grammarPath: String,
    val evaluator: IPredicateEvaluator,
    val exampleTracesPath: String,
    val counterExampleTracesPath: String,
    val mappings: List<AbstractStateToRobustnessMapping> = emptyList()
) // ExperimentConfig

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

        val config = loadClassFromFile<ExperimentConfig>(configFile)

        val grammar = readGrammar(config.grammarPath)
        val exampleTraceData = readTraces(Path(config.exampleTracesPath), config.mappings)
        val counterExampleTraceData = readTraces(Path(config.counterExampleTracesPath), config.mappings)
        if(counterExampleTraceData.traces.isNotEmpty()) {
            check(exampleTraceData.atomicPredicates == counterExampleTraceData.atomicPredicates) {
                "Mismatch between atomic predicates in example:\n" +
                        "(${exampleTraceData.atomicPredicates})\n" +
                        "and counterexample traces:\n" +
                        "[${counterExampleTraceData.atomicPredicates}]"
            }
        }

        populateBlockLiteralProductions(grammar, exampleTraceData.atomicPredicates)

        // Splitting loop
        val evaluator = AutomatonTraceEvaluator()
        val examples = exampleTraceData.traces.toMutableList()
        val counters = counterExampleTraceData.traces.toMutableList()

        for (i in 0 until 4){
            val search = BasicMCTSSearch(
                grammar,
                config.evaluator,
                examples,
                counters
            )

            while (search.isStillRunning) {
                search.step()
                search.printStepResults()
            }

            val sol = search.getBestSolutions().first()
            println("Splitting by predicate [${sol}]")

            // transfer elements that satisfy the predicate from the examples list to the counters list
            val modelledPredicates = examples.filter {
                evaluator.evaluateTrace(sol, it)["progress"]!! >= 1.0
            }
            counters.addAll(modelledPredicates)
            examples.removeAll(modelledPredicates)
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
