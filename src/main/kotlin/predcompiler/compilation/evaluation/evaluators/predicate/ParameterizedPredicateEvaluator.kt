package predcompiler.compilation.evaluation.evaluators.predicate

import predcompiler.compilation.evaluation.evaluators.trace.*
import predcompiler.compilation.evaluation.evaluators.traceset.*
import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.EvaluationResult

class ParameterizedPredicateEvaluator : IPredicateEvaluator {

    override fun evaluatePredicate(
        predicate: String, examples: List<List<RealValuation>>, counterExamples: List<List<RealValuation>>
    ): EvaluationResult {

        val evaluators = listOf(
            VariableUsageEvaluator(),
            AutomatonTraceEvaluator())

        val examplesEvaluation = evaluateTraceSet(predicate, examples, evaluators)
        val counterExamplesEvaluation = evaluateTraceSet(predicate, counterExamples, evaluators)

        // hashmap with a report including the average waste for the examples, the ratio of examples with progress 1.0,
        // the ratio of counter examples with progress < 1.0, the average reward score for the examples, the unused
        // variable score and the abused variable score for the predicate
        return hashMapOf(
            "examplesAcceptanceRatio" to
                    examplesEvaluation["progress"]!!.count { it >= 1.0 } / examples.size.toDouble(),
            "counterExamplesRejectionRatio" to
                    (counterExamplesEvaluation["progress"]?.count { it < 1.0 }?.div(counterExamples.size.toDouble()) ?: 1.0),
            "exampleTraceWasteScore" to examplesEvaluation["waste"]!!.average(),
            "avgExamplesCorrectness" to examplesEvaluation["progress"]!!.average(),
            "avgExamplesRewardScore" to examplesEvaluation["reward"]!!.average(),
            "unusedVariableScore" to examplesEvaluation["unusedVariableScore"]!!.average(),
            "abusedVariableScore" to examplesEvaluation["abusedVariableScore"]!!.average()
        )
    } // evaluatePredicate

} // ParameterizedPredicateEvaluator