package predcompiler.compilation.evaluation

data class StateRobustnessInfo (
    /**
     * Robustness of the self loop transition, if any. Defaults to
     * Float.NEGATIVE_INFINITY if no self loop exists.
     */
    val selfLoopRobustness: Double = 0.0,

    /**
     * Information on the edge with the highest robustness of the outgoing edges.
     */
    val bestOutgoingEdgeData: EdgeRobustnessInfo? = null
) // StateRobustnessInfo
