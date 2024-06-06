package predcompiler.compilation.evaluation

data class EvaluationReport(
    /**
     * Number of steps that were NOT processed by the evaluation algorithm. A non-zero
     * value here denotes an early termination (i.e. the trace was rejected by
     * reaching a trap state, or accepted by reaching an acceptance state with a
     * single transition into itself tagged with a True predicate).
     */
    val remainingSteps: Int = 0,

    /**
     * % of the specification that was fulfilled by the trace provided. Set to 0f if
     * the trace leads the automaton to a trap state at any point, and is only 1f if
     * the execution ends in an acceptance state or, if no acceptance states are
     * available, if it ends in a non-trap state.
     */
    val endProgress: Float = 0f,

    /**
     * Accumulated reward during the execution of the trace against the predicate.
     * This takes optimization goals into account, as well as stay-away-from-traps
     * goals, but is not scaled in any way and does not directly represent
     * satisfaction of the trace in a meaningful way. However, assuming endProgress
     * = 1f, then it is often the case that a higher value of endReward is
     * associated with a better trace. Note that early termination stops reward
     * processing (this is useful for inferring predicates that make the most out of
     * the information provided in the trace, but should be tweaked when using with
     * training frameworks to ensure a proper final reward is granted for early
     * successful completion of the task at hand).
     */
    val endReward: Float = 0f
) // EvaluationReport
