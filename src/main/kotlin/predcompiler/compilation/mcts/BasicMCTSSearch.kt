package predcompiler.compilation.mcts

import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.evaluation.IPredicateEvaluator
import predcompiler.compilation.evaluation.TieredPredicateEvaluator
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

    init {
        val heuristic = fun(state: GrammarProductionState): Double {
            if (state.isNotTerminal()) return 0.0
            val stringPredicate: String = state.buildResultString()
            return TieredPredicateEvaluator().evaluatePredicate(
                stringPredicate, exampleTraces,
                counterExampleTraces
            ).toDouble()
        }
        parameters = BasicMCTSParams(
            K = sqrt(2.0),
            rolloutLength = 100,
            maxTreeDepth = 15,
            epsilon = 1e-6,
            budgetType = MCTSSearchConstants.BUDGET_ITERATIONS,
            budget = 100,
            heuristic = heuristic
        )
        state = GrammarProductionState(grammar, listOf(grammar[0].symbol))
    }

    private fun getAction(state: GrammarProductionState): GrammarProductionAction {
        // Search for best action from the root
        val currentMCTSTree = BasicTreeNode(this, null, state, rnd)

        // mctsSearch does all of the hard work
        currentMCTSTree.mctsSearch()

        // Return best action
        return currentMCTSTree.bestAction()
    } // getAction

    public override fun stepSearch() {
        while (state.isNotTerminal()) {
            val action = getAction(state)
            val newState = state.applyProduction(action)
            if (newState == state) break
            state = newState
        }
        bestSolutions.add(state.buildResultString())
        bestFitness = evaluator.evaluatePredicate(state.buildResultString(), exampleTraces, counterExampleTraces)
        terminated = true
    } // stepSearch

} // BasicMCTSSearch
