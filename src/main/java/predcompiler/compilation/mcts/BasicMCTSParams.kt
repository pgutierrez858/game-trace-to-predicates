package predcompiler.compilation.mcts

import kotlin.math.sqrt

data class BasicMCTSParams(
    val K: Double = sqrt(2.0),
    val rolloutLength: Int = 10, // assuming we have a good heuristic
    val maxTreeDepth: Int = 100, // effectively no limit
    val epsilon: Double = 1e-6,
    val heuristic: ((GrammarProductionState) -> Double)? = null,
    val breakMS: Int? = 0,
    val seed: Long? = 0,
    val budgetType: MCTSSearchConstants? = null,
    val budget: Long = 0
)
