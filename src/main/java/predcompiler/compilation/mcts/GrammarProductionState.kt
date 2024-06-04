package predcompiler.compilation.mcts

import org.moeaframework.util.grammar.ContextFreeGrammar
import org.moeaframework.util.grammar.Symbol

data class GrammarProductionState(
    internal val grammar: ContextFreeGrammar,
    internal val resultSequence: List<Symbol>
) // GrammarProductionState