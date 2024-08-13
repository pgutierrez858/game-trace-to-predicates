package predcompiler.compilation.evaluation

import rabinizer.bdd.ValuationSet

data class EdgeRobustnessInfo (
    /**
     * Predicate whose robustness is evaluated in edgeRobustness.
     */
    val edgePredicate: ValuationSet? = null,

    /**
     * Numerical robustness of edgePredicate in the current state of the game.
     */
    val edgeRobustness: Double = 0.0
) // EdgeRobustnessInfo
