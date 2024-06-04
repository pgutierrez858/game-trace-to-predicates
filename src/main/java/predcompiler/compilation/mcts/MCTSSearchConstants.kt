package predcompiler.compilation.mcts

/**
 * Taken from
 * [
 * TAG's MCTS implementation](https://github.com/GAIGResearch/TabletopGames/blob/master/src/main/java/players/PlayerConstants.java).
 * We have five distinct budgets to constrain MCTS:
 * - TIME is measured in milliseconds
 * - ITERATIONS is self-explanatory
 * - FM_CALLS limits the number if times the ForwardModel.next() function can be called
 * - COPY_CALLS limits the number of times a state can be copied (this is frequently quite a bit more time-consuming then
 * ForwardModel.next(), so for some games FM directly may be an unfair comparator
 * - FMANDCOPY_CALLS limits the sum of the calls to copy() or next()
 */
enum class MCTSSearchConstants {
    BUDGET_TIME, BUDGET_ITERATIONS, BUDGET_FM_CALLS, BUDGET_FMANDCOPY_CALLS, BUDGET_COPY_CALLS
} // MCTSSearchConstants

