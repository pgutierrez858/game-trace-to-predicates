package predcompiler.compilation.evaluation.evaluators.traceset

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.trace.AbstractTraceEvaluator

/**
 * Given a collection of traces and a collection of trace evaluators, evaluates all traces using each evaluator and
 * combines the results into a map of metric names to evaluation results for the trace set.
 * @param predicate The predicate to evaluate the traces against.
 * @param traces The traces to evaluate.
 * @param evaluators The evaluators to use to evaluate the traces.
 * @return A map of metric names to aggregated evaluation results for the trace set.
 */
fun evaluateTraceSet(
    predicate: String,
    traces: Collection<List<RealValuation>>,
    evaluators: Collection<AbstractTraceEvaluator>
    ): HashMap<String, List<Double>> {

    // Evaluate all traces using all evaluators.
    // Each evaluator will output a list of evaluation results for each of the traces in the set.
    val evaluatorResults = evaluators.map { evaluator ->
        // compute the evaluation scores for each trace using the current evaluator.
        traces.map { trace -> evaluator.evaluateTrace(predicate, trace) }
    }

    // combine all evaluator results into a map of [metric name -> list of metric values] from all traces.
    return evaluatorResults.fold(hashMapOf()){
        acc: HashMap<String, List<Double>>, evaluationsByTrace ->
        evaluationsByTrace.forEach { traceEvaluation ->
            traceEvaluation.forEach { (metricName, metricValue) ->
                acc[metricName] = acc.getOrDefault(metricName, mutableListOf()) + metricValue
            }
        }
        acc
    }
} // evaluateTraceSet