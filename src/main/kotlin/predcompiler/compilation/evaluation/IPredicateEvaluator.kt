package predcompiler.compilation.evaluation

/**
 * Interface base for all predicate evaluators. A predicate evaluator can be
 * understood as a function that takes a predicate in String form and lists of
 * real-valued example and counter-example traces and outputs a single fitness
 * value representing how well the predicate models the traces according to the
 * function's criteria.
 */
interface IPredicateEvaluator {
    /**
     * Outputs a single fitness value representing how well the given predicate
     * models the sample and counter-example traces according to the function's
     * criteria.
     */
    fun evaluatePredicate(
        predicate: String,
        examples: List<Array<RealValuation>>,
        counterExamples: List<Array<RealValuation>>
    ): Float
}
