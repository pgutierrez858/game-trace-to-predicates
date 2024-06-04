package predcompiler.compilation.grammarevolution

import org.moeaframework.algorithm.single.GeneticAlgorithm
import org.moeaframework.core.PRNG
import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.evaluation.IPredicateEvaluator
import predcompiler.compilation.evaluation.TieredPredicateEvaluator
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping
import predcompiler.compilation.evaluation.statevariables.LessThanMapping
import java.io.File

class GrammarEvolutionGrammarSearch(
    grammarPath: String, tracesPath: String, evaluator: IPredicateEvaluator,
    mappings: List<AbstractStateToRobustnessMapping>, initialPopulationSize: Int, maxGenerations: Int
) : AbstractPredicateSearch(
    grammarPath, tracesPath, evaluator, mappings
) {
    // region private search fields
    /**
     * Maximum number of population iterations that the algorithm will go through.
     */
    private val maxGenerations: Int

    /**
     * Index of the current evolution generation
     */
    private var generation: Int

    /**
     * Reference to our problem solver algorithm.
     */
    private val algorithm: GeneticAlgorithm

    /**
     * Reference to the problem that we are trying to solve.
     */
    private val problem: GrammarRegression
    // endregion

    init {
        PRNG.setRandom(SynchronizedMersenneTwister.instance)

        this.maxGenerations = maxGenerations

        // setup and construct the GP solver
        problem = GrammarRegression(exampleTraces, counterExampleTraces, grammar, TieredPredicateEvaluator())
        algorithm = GeneticAlgorithm(problem)
        algorithm.initialPopulationSize = initialPopulationSize
        generation = 0
    } // GrammarEvolutionGrammarSearch

    override fun stepSearch() {
        if (generation >= maxGenerations) return

        algorithm.step()

        for (solution in algorithm.result) {
            problem.getPredicate(solution)?.let { bestSolutions.add(it) }
        }
        // record the fitness of the members from the solution set
        // (note that there will always be at least one)
        bestFitness = algorithm.result[0].getObjective(0).toFloat()

        generation++

        if (generation >= maxGenerations) {
            algorithm.terminate()
        }
    } // stepSearch

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Check if help command is requested or if the number of arguments is incorrect

            if (args.size != 4 || args[0].equals("-help", ignoreCase = true) || args[0].equals(
                    "--help",
                    ignoreCase = true
                )
            ) {
                printUsage()
                return
            }

            try {
                // Parse args[0] as the path to the .bnf file
                val bnfFilePath = args[0]
                // Validate if the path points to a .bnf file
                if (!bnfFilePath.endsWith(".bnf")) {
                    System.err.println("Error: The first argument must be a path to a .bnf file.")
                    printUsage()
                    return
                }

                // Parse args[1] as the path to the system folder
                val systemFolderPath = args[1]
                // Validate if the path is a directory
                val folder = File(systemFolderPath)
                if (!folder.isDirectory) {
                    System.err.println("Error: The second argument must be a path to a directory.")
                    printUsage()
                    return
                }

                // Parse args[2] as the initialPopulationSize parameter
                val initialPopulationSize: Int
                try {
                    initialPopulationSize = args[2].toInt()
                } catch (e: NumberFormatException) {
                    System.err.println(
                        "Error: The third argument must be an integer representing the initial Population Size."
                    )
                    printUsage()
                    return
                }

                // Parse args[3] as the maxGenerations parameter
                val maxGenerations: Int
                try {
                    maxGenerations = args[3].toInt()
                } catch (e: NumberFormatException) {
                    System.err.println("Error: The third argument must be an integer representing the max Generations.")
                    printUsage()
                    return
                }

                // sample mappings setting for testing
                val mappings: MutableList<AbstractStateToRobustnessMapping> = ArrayList()
                val resourceChecks = floatArrayOf(1f, 2f, 3f, 4f, 5f)
                val resourceNames = arrayOf("wood", "iron", "axe")
                for (name in resourceNames) {
                    for (check in resourceChecks) {
                        mappings.add(LessThanMapping(name, check, 0f, 5f))
                    }
                }
                val search = GrammarEvolutionGrammarSearch(
                    bnfFilePath, systemFolderPath,
                    TieredPredicateEvaluator(), mappings,
                    initialPopulationSize, maxGenerations
                )


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
            println(
                "Usage: java GrammarEvolutionGrammarSearch <bnfFilePath> <systemFolderPath> <initialPopulationSize> <maxGenerations>"
            )
            println("  <bnfFilePath>           : Absolute path to the .bnf file.")
            println("  <systemFolderPath>      : Absolute path to the system folder containing traces.")
            println("  <initialPopulationSize> : Number of individuals to be included in the population.")
            println("  <maxGenerations>        : Maximum number of population evolutions.")
            println("  -help, --help           : Display this help message.")
        } // printUsage
    }
} // GrammarEvolutionGrammarSearch

