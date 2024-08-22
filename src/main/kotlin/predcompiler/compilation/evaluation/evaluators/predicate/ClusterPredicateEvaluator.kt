package predcompiler.compilation.evaluation.evaluators.predicate

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.EvaluationResult
import predcompiler.compilation.evaluation.evaluators.trace.AutomatonTraceEvaluator
import predcompiler.compilation.evaluation.evaluators.traceset.evaluateTraceSet

class ClusterPredicateEvaluator: IPredicateEvaluator {

    override fun evaluatePredicate(
        predicate: String, examples: List<List<RealValuation>>, counterExamples: List<List<RealValuation>>
    ): EvaluationResult {

        val evaluators = listOf(
            AutomatonTraceEvaluator()
        )

        val examplesEvaluation = evaluateTraceSet(predicate, examples, evaluators)
        val wasteEntries = examplesEvaluation["waste"]
        val rewardEntries = examplesEvaluation["reward"]
        val progressEntries = examplesEvaluation["progress"]
        check(wasteEntries != null && rewardEntries != null && progressEntries != null) {
            "Evaluation result must contain waste, reward and progress entries."
        }

        val clusteringData = Array(examples.size) { i ->
            doubleArrayOf(wasteEntries[i], rewardEntries[i], progressEntries[i])
        }
        val clusters = progressEntries.map { if (it >= 1.0) 0 else 1 }.toIntArray()

        val fitness = silhouetteCoefficient(clusteringData, clusters)

        return hashMapOf(
            "clusterFitness" to fitness
        )
    } // evaluatePredicate

} // ClusterPredicateEvaluator