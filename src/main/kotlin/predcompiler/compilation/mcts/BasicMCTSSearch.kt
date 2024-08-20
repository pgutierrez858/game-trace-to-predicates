package predcompiler.compilation.mcts

import io.github.oshai.kotlinlogging.KotlinLogging
import org.moeaframework.util.grammar.ContextFreeGrammar
import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.predicate.IPredicateEvaluator
import java.util.*
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * Basic MCTS-based searcher using [TAG's BasicMCTSPlayer implementation](https://github.com/GAIGResearch/TabletopGames/blob/master/src/main/java/players/basicMCTS/BasicMCTSPlayer.java) as a reference.
 */
class BasicMCTSSearch(
    grammar: ContextFreeGrammar,
    evaluator: IPredicateEvaluator,
    exampleTraces: List<List<RealValuation>>,
    counterExampleTraces: List<List<RealValuation>>,
) : AbstractPredicateSearch(grammar, evaluator, exampleTraces, counterExampleTraces) {

    val rnd: Random = Random()
    val parameters: BasicMCTSParams
    var state: GrammarProductionState
    private var currentMCTSTree: BasicTreeNode
    private var totalTime: Double = 0.0

    init {
        val heuristic = fun(state: GrammarProductionState): Double {
            // To ignore intermediate heuristic:
            if (state.isNotTerminal()) return 0.0

            val stringPredicate: String = state.buildResultString(ignoreNonTerminals = true)
            return evaluator.evaluatePredicate(
                stringPredicate, exampleTraces,
                counterExampleTraces
            ).values.reduce(Double::times)
        }
        parameters = BasicMCTSParams(
            K = sqrt(2.0),
            rolloutLength = 15,
            maxTreeDepth = 15,
            epsilon = 1e-6,
            budgetType = MCTSSearchConstants.BUDGET_ITERATIONS,
            budget = 300,
            breakMS = 300,
            heuristic = heuristic,
            maxTotalTime = 150000
        )
        state = GrammarProductionState(grammar, listOf(grammar[0].symbol))
        currentMCTSTree = BasicTreeNode(this, null, state, rnd)
    }

    private fun getAction(): GrammarProductionAction {
        val elapsedTimer = ElapsedCpuTimer()

        // mctsSearch does all the hard work
        currentMCTSTree.mctsSearch()

        // Update total time
        totalTime += elapsedTimer.elapsedMillis().toDouble()

        // Return best action
        return currentMCTSTree.bestAction()
    } // getAction

    private fun debugMctsTickResults() {
        logger.info {
            """
            MCTS tick results:
            -> Current MCTS Tree Root Node State: ${state.buildResultString(ignoreNonTerminals = false)}
            -> Best Leaf State: ${currentMCTSTree.bestLeafState()?.buildResultString(ignoreNonTerminals = false)}
            -> Value: ${currentMCTSTree.getBestValue()}
            """.trimIndent()
        }
    } // debugMctsTickResults

    public override fun tickSearch() {
        if (terminated) {
            logger.warn { "Search is already terminated. Ticking it again will have no effect." }
            return
        }

        if (state.isNotTerminal()) {
            val action = getAction()
            val newState = state.applyProduction(action)
            debugMctsTickResults()

            currentMCTSTree.pruneActionsDifferentFrom(action)
            currentMCTSTree = currentMCTSTree.getChildFromAction(action)!!
            state = newState
        }
        else terminated = true

        val bestState = currentMCTSTree.bestLeafState() ?: return
        val bestFitnessReport = evaluator.evaluatePredicate(
            bestState.buildResultString(ignoreNonTerminals = true),
            exampleTraces,
            counterExampleTraces
        )
        val currentFitness = bestFitnessReport.values.average()
        if (currentFitness >= bestFitness) {
            bestSolutions.clear()
            bestSolutions.add(bestState.buildResultString(ignoreNonTerminals = true))
            bestFitness = currentFitness
        }
    } // stepSearch

} // BasicMCTSSearch
