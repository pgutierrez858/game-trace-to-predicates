package predcompiler.compilation.bruteforce

import org.moeaframework.util.grammar.ContextFreeGrammar
import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.predicate.IPredicateEvaluator
import java.util.*

class BruteForceGrammarSearch(
    grammar: ContextFreeGrammar,
    evaluator: IPredicateEvaluator,
    exampleTraces: List<List<RealValuation>>,
    counterExampleTraces: List<List<RealValuation>>,
    /**
     * Maximum number of productions that this search will look into from the root
     * node of the grammar tree.
     */
    private val maxDepth: Int
) : AbstractPredicateSearch(grammar, evaluator, exampleTraces, counterExampleTraces) {
    private val terminals: List<String>

    private var processedTerminals: Int

    init {
        terminals = buildGrammarTree()
        processedTerminals = 0
    } // BruteForceGrammarSearch

    public override fun tickSearch() {
        if (processedTerminals >= terminals.size) return

        val opt = terminals[processedTerminals]
        val fitness = evaluator.evaluatePredicate(opt, exampleTraces, counterExampleTraces).values.average()
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

} // BruteForceGrammarSearch

