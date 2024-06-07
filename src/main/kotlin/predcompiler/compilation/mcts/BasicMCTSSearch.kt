package predcompiler.compilation.mcts

import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.evaluation.evaluators.IPredicateEvaluator
import predcompiler.compilation.evaluation.evaluators.TieredPredicateEvaluator
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping
import java.util.*
import kotlin.math.sqrt

/**
 * Basic MCTS-based searcher using [TAG's BasicMCTSPlayer implementation](https://github.com/GAIGResearch/TabletopGames/blob/master/src/main/java/players/basicMCTS/BasicMCTSPlayer.java) as a reference.
 */
class BasicMCTSSearch(
    grammarPath: String, tracesPath: String, evaluator: IPredicateEvaluator,
    mappings: List<AbstractStateToRobustnessMapping>
) : AbstractPredicateSearch(grammarPath, tracesPath, evaluator, mappings) {

    val rnd: Random = Random()
    val parameters: BasicMCTSParams
    var state: GrammarProductionState
    private var currentMCTSTree: BasicTreeNode

    init {
        val heuristic = fun(state: GrammarProductionState): Double {
            // To ignore intermediate heuristic:
            if (state.isNotTerminal()) return 0.0

            val stringPredicate: String = state.buildResultString(ignoreNonTerminals = true)
            // if (stringPredicate.isEmpty() || stringPredicate.contains("()") || stringPredicate.contains(")(")) return 0.0
            return evaluator.evaluatePredicate(
                stringPredicate, exampleTraces,
                counterExampleTraces
            ).toDouble()
        }
        parameters = BasicMCTSParams(
            K = sqrt(2.0),
            rolloutLength = 20,
            maxTreeDepth = 20,
            epsilon = 1e-6,
            budgetType = MCTSSearchConstants.BUDGET_ITERATIONS,
            budget = 1500,
            heuristic = heuristic
        )
        state = GrammarProductionState(grammar, listOf(grammar[0].symbol))
        currentMCTSTree = BasicTreeNode(this, null, state, rnd)
    }

    private fun getAction(state: GrammarProductionState): GrammarProductionAction {
        // mctsSearch does all of the hard work
        currentMCTSTree.mctsSearch()

        // Return best action
        return currentMCTSTree.bestAction()
    } // getAction

    public override fun stepSearch() {
        while (state.isNotTerminal()) {
            val action = getAction(state)
            val newState = state.applyProduction(action)
            println(newState.buildResultString(ignoreNonTerminals = false))

            currentMCTSTree.pruneActionsDifferentFrom(action)
            currentMCTSTree = currentMCTSTree.getChildFromAction(action)!!
            if (newState == state) break
            state = newState
        }
        bestSolutions.add(state.buildResultString(ignoreNonTerminals = true))
        bestFitness = evaluator.evaluatePredicate(
            state.buildResultString(ignoreNonTerminals = true),
            exampleTraces,
            counterExampleTraces
        )
        terminated = true
    } // stepSearch

} // BasicMCTSSearch
