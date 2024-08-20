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
import kotlin.io.path.exists
import io.github.oshai.kotlinlogging.KotlinLogging

data class ExperimentConfig(
    val grammarPath: String,
    val evaluator: IPredicateEvaluator,
    val exampleTracesPath: String,
    val counterExampleTracesPath: String,
    val mappings: List<AbstractStateToRobustnessMapping> = emptyList()
) // ExperimentConfig

fun main(args: Array<String>) {

    val logger = KotlinLogging.logger("ExperimentLauncher")

    // Assumptions on Input size and file extension.
    // Check if help command is requested or if the number of arguments is incorrect
    require(args.size == 1) {
        errorAndUsageMessage("Incorrect number of arguments")
    }

    // Parse args[0] as the path to the .json config file
    val configFile = args[0]
    // Validate if the path points to a .bnf file
    require (configFile.endsWith(".json")) {
        errorAndUsageMessage("The first argument must be a path to a .json file")
    }

    logger.info { "Reading configuration from $configFile" }
    check(Path(configFile).exists()) {
        errorAndUsageMessage("File $configFile does not exist")
    }

    val config = loadClassFromFile<ExperimentConfig>(configFile)
    logger.info { "Configuration loaded: $config" }

    logger.info { "Reading grammar from ${config.grammarPath}" }
    val grammar = readGrammar(config.grammarPath)

    logger.info { "Reading example traces from ${config.exampleTracesPath}" }
    val exampleTraceData = readTraces(Path(config.exampleTracesPath), config.mappings)

    logger.info { "Reading counterexample traces from ${config.counterExampleTracesPath}" }
    val counterExampleTraceData = readTraces(Path(config.counterExampleTracesPath), config.mappings)
    if(counterExampleTraceData.traces.isNotEmpty()) {
        check(exampleTraceData.atomicPredicates == counterExampleTraceData.atomicPredicates) {
            "Mismatch between atomic predicates in example:\n" +
                    "(${exampleTraceData.atomicPredicates})\n" +
                    "and counterexample traces:\n" +
                    "[${counterExampleTraceData.atomicPredicates}]"
        }
    }

    // Populate the grammar with literals inferred from the traces
    // Grammar files are generally specified without literal rules to keep them generic,
    // so we need to add them here with our domain information in mind.
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
            search.tick()
            search.debugTickResults()
        }

        val sol = search.getBestSolutions().first()
        logger.info { "Splitting by predicate [${sol}]" }

        // transfer elements that satisfy the predicate from the examples list to the counters list
        val modelledPredicates = examples.filter {
            evaluator.evaluateTrace(sol, it)["progress"]!! >= 1.0
        }
        counters.addAll(modelledPredicates)
        examples.removeAll(modelledPredicates)
    }
} // main

private const val usage = "Usage: java ExperimentLauncher <jsonFilePath>\n" +
        "   <jsonFilePath>          : Absolute path to the .json file with the experiment's configuration.\n" +
        "  -help, --help           : Display this help message."

private fun errorAndUsageMessage(errorMsg: String?) {
    if (errorMsg != null) "Error: $errorMsg\n$usage" else usage
} // printUsage
