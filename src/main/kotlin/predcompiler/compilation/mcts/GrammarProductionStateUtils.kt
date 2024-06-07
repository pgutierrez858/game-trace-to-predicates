package predcompiler.compilation.mcts

import org.moeaframework.util.grammar.Symbol

/**
 * Produce a list of all possible grammar production actions that can be taken from the calling state.
 */
fun GrammarProductionState.possibleActions(): List<GrammarProductionAction> {
    return buildList {
        for ((symbolIndex, symbol) in resultSequence.withIndex()) {
            // a terminal symbol can't be expanded anymore.
            if (symbol.isTerminal) continue
            val rule = grammar[symbol]
            if (rule != null) {
                // add a possible expansion for each of the available productions for this symbol.
                for (productionIndex in 0 until rule.size()) {
                    add(GrammarProductionAction(symbolIndex, productionIndex))
                }
            }
        }
    }
} // GrammarProductionState.possibleActions

/**
 * Generate a new [GrammarProductionState] by applying [action] to the calling state. If [action] is invalid, then this
 * returns a reference to the current state.
 */
fun GrammarProductionState.applyProduction(action: GrammarProductionAction): GrammarProductionState {
    // do not allow actions that target invalid result positions
    if (action.symbolToExpandIndex !in 0 until resultSequence.count()) return this

    val symbolToExpand = resultSequence[action.symbolToExpandIndex]
    // do not allow actions that target terminal symbols
    if (symbolToExpand.isTerminal) return this

    val chosenRule = grammar[symbolToExpand]
    // do not allow invalid chosen rule indices
    if (chosenRule == null || action.chosenProductionIndex !in 0 until chosenRule.size()) return this

    // select the production specified by the action object
    val takenProduction = chosenRule[action.chosenProductionIndex]

    val productionResultSegment: List<Symbol> = buildList {
        // a production specifies the symbols that will replace the selected action symbol in the result sequence.
        for (i in 0 until takenProduction.size()) {
            // collect the next symbol from the production's tokens
            add(takenProduction[i])
        }
    }

    val leftSide = resultSequence.slice(0 until action.symbolToExpandIndex)
    val rightSide = resultSequence.slice(
        action.symbolToExpandIndex + 1 until resultSequence.count()
    )
    return GrammarProductionState(
        grammar,
        leftSide + productionResultSegment + rightSide
    )
} // GrammarProductionState.applyProduction

/**
 * Returns true if at least one of the tokens from the result sequence happens to be a non-terminal unexpanded symbol.
 */
fun GrammarProductionState.isNotTerminal(): Boolean {
    return resultSequence.any { symbol -> !symbol.isTerminal }
} // GrammarProductionState.isNotTerminal()

/**
 * Produces a String representation of the result by joining the token values of all symbols in the result sequence.
 */
fun GrammarProductionState.buildResultString(ignoreNonTerminals: Boolean): String {
    if (ignoreNonTerminals) return resultSequence.filter { it.isTerminal }.joinToString("") { it.value }
    return resultSequence.joinToString("") { symbol -> symbol.value }
} // GrammarProductionState.buildResultString