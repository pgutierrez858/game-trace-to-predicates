package predcompiler.compilation.evaluation.evaluators.predicate

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.EvaluationResult
import predcompiler.compilation.evaluation.evaluators.trace.AutomatonTraceEvaluator
import predcompiler.compilation.evaluation.evaluators.traceset.evaluateTraceSet
import smile.clustering.kmeans
import kotlin.math.abs

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
        val clusteringResult = kmeans(clusteringData, 2)
        val (cluster1size, cluster2size) = clusteringResult.size

        // for each proposed cluster, compute the proportion of traces that have progress 1.0
        // ideally, one cluster should contain only traces with progress 1.0, and the
        // other cluster should contain only traces with progress < 1.0
        val modelledTracesPerCluster = clusteringData.fold(Pair(0.0, 0.0)) {
            (c1ok, c2ok), point ->
            if (point[2] == 1.0) {
                if (clusteringResult.predict(point) == 0) Pair(c1ok + 1.0 / cluster1size, c2ok)
                else Pair(c1ok, c2ok + 1.0 / cluster2size)
            }
            else Pair(c1ok, c2ok)
        }
        // the progress separation score is the absolute difference between the proportion of traces with progress 1.0
        // in each cluster. ideally, this score should be 1.0, meaning that one cluster contains only traces with progress
        // 1.0, and the other cluster contains only traces with progress < 1.0 (that is, a cluster is completely rejected
        // by the predicate and the other is completely accepted)
        val progressSeparationScore =
            abs(modelledTracesPerCluster.first - modelledTracesPerCluster.second)

        return hashMapOf(
            "kMeansDistortion" to clusteringResult.distortion,
            "progressSeparationScore" to progressSeparationScore
        )
    } // evaluatePredicate

} // ClusterPredicateEvaluator