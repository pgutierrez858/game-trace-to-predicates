package predcompiler.compilation.mcts

/**
 * Represents an action taken to expand a non-terminal symbol within a [GrammarProductionState].
 * @param symbolToExpandIndex index of the symbol to expand, considering an in-order of the expansion tree
 * leaves that ignores terminal tokens.
 * @param chosenProductionIndex index of the rule to apply to expand the chosen symbol.
 */
data class GrammarProductionAction(
    val symbolToExpandIndex: Int,
    val chosenProductionIndex: Int
)  // GrammarProductionAction

