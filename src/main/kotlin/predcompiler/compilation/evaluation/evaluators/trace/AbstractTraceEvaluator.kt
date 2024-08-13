package predcompiler.compilation.evaluation.evaluators.trace

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.EvaluationResult

/**
 * Abstract class base for all trace evaluators, that is, functions that accept a string predicate and a real-valued
 * trace and output a set of fitness values representing how well the predicate models the trace according
 * to the function's criteria.
 */
abstract class AbstractTraceEvaluator {

    /**
     * Outputs a map of fitness values representing how well the base predicate models the sample trace according to the
     * function's criteria.
     */
    abstract fun evaluateTrace(
        predicate: String,
        trace: List<RealValuation>
    ): EvaluationResult
} // ITraceEvaluator