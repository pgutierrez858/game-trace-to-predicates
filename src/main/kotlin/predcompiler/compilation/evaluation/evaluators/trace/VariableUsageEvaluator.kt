package predcompiler.compilation.evaluation.evaluators.trace

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.EvaluationResult
import predcompiler.compilation.evaluation.evaluators.countOccurrences

/**
 * Evaluator that assigns a score to a trace based on the number of abused variables in the predicate (the more a variable
 * is abused, the lower the score). A variable is considered abused if it appears more than once in the predicate.
 */
class VariableUsageEvaluator: AbstractTraceEvaluator() {
    override fun evaluateTrace(predicate: String, trace: List<RealValuation>): EvaluationResult {
        val variableCounts = trace[0].keys.map { key -> countOccurrences(predicate, key) }
        val abusedVariablesRatio = variableCounts.count { it > 1 } / variableCounts.size.toDouble()
        val unusedVariablesRatio = variableCounts.count { it == 0 } / variableCounts.size.toDouble()
        val abusedVariableScore = 1f - abusedVariablesRatio
        val unusedVariableScore = 1f - unusedVariablesRatio
        return hashMapOf(
            "abusedVariableScore" to abusedVariableScore,
            "unusedVariableScore" to unusedVariableScore)
    } // evaluateTrace
} // AbusedVariablesEvaluator