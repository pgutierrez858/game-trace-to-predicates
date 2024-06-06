package predcompiler.compilation.bruteforce

import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.evaluation.evaluators.IPredicateEvaluator
import predcompiler.compilation.evaluation.evaluators.TieredPredicateEvaluator
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping
import predcompiler.compilation.evaluation.statevariables.LessThanMapping
import java.io.File
import java.util.*

class BruteForceGrammarSearch(
    grammarPath: String, tracesPath: String, evaluator: IPredicateEvaluator,
    mappings: List<AbstractStateToRobustnessMapping>,
    /**
     * Maximum number of productions that this search will look into from the root
     * node of the grammar tree.
     */
    private val maxDepth: Int
) : AbstractPredicateSearch(grammarPath, tracesPath, evaluator, mappings) {
    private val terminals: List<String>

    private var processedTerminals: Int

    init {
        terminals = buildGrammarTree()
        processedTerminals = 0
    } // BruteForceGrammarSearch

    public override fun stepSearch() {
        if (processedTerminals >= terminals.size) return

        val opt = terminals[processedTerminals]
        val fitness = evaluator.evaluatePredicate(opt, exampleTraces, counterExampleTraces)
        if (fitness > bestFitness) {
            bestFitness = fitness
            bestSolutions = HashSet()
            bestSolutions.add(opt)
        } else if (fitness == bestFitness) {
            bestSolutions.add(opt)
        }

        processedTerminals++
        if (processedTerminals >= terminals.size) terminated = true
    } // stepSearch

    private fun buildGrammarTree(): List<String> {
        val productionStack = Stack<GrammarTree>()

        // start with the root rule in the grammar and go down from there
        val baseRule = grammar[0]
        val rootTree = GrammarTree(baseRule.symbol, 0)

        // this will be the base node to process in the stack
        productionStack.add(rootTree)

        // keep processing nodes until we have a complete tree
        while (!productionStack.isEmpty()) {
            // get next tree to process.
            // i.e. <expression> [0]
            val currentTree = productionStack.pop()

            if (currentTree.rootSymbol.isTerminal) {
                // terminal symbol found, meaning it has no further productions to work with
                continue
            }
            // populate parent tree children with possible productions
            // i.e. <expression> [0] -> { <literal> [1] }, { <compound> [1] }
            // those productions will be added to the production stack for future
            // processing.
            val currentRule = grammar[currentTree.rootSymbol]
            for (i in 0 until currentRule.size()) {
                // a production may be divided into an array of symbols, each
                // of which needs to be processed individually.
                // i.e. { <expression> <binary> <expression> }
                // we call these nested symbols production sections
                val nextProduction = currentRule[i]
                val productionSections: MutableList<GrammarTree> = ArrayList()

                for (j in 0 until nextProduction.size()) {
                    // in the example, next element from { <expression> <binary> <expression> }
                    val nextSymbol = nextProduction[j]

                    // only allow new expansions if we have yet to reach max depth.
                    if (currentTree.depth < maxDepth) {
                        val prodTree = GrammarTree(nextSymbol, currentTree.depth + 1)
                        productionSections.add(prodTree)
                        productionStack.add(prodTree)
                    }
                }
                // add the section to the current tree node, assuming it's not empty
                if (productionSections.isNotEmpty()) currentTree.productions.add(productionSections)
            }
        }

        return rootTree.terminals
    } // buildGrammarTree

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Check if help command is requested or if the number of arguments is incorrect

            if (args.size != 3 || args[0].equals("-help", ignoreCase = true) || args[0].equals(
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

                // Parse args[2] as the maximum depth parameter
                val maxDepth: Int
                try {
                    maxDepth = args[2].toInt()
                } catch (e: NumberFormatException) {
                    System.err.println("Error: The third argument must be an integer representing the maximum depth.")
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
                val search = BruteForceGrammarSearch(
                    bnfFilePath,
                    systemFolderPath,
                    TieredPredicateEvaluator(),
                    mappings,
                    maxDepth
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
            println("Usage: java BruteForceGrammarSearch <bnfFilePath> <systemFolderPath> <maxDepth>")
            println("  <bnfFilePath>        : Absolute path to the .bnf file.")
            println("  <systemFolderPath>   : Absolute path to the system folder containing traces.")
            println("  <maxDepth>           : Maximum depth parameter of the search algorithm.")
            println("  -help, --help        : Display this help message.")
        } // printUsage
    }
} // BruteForceGrammarSearch

