package predcompiler.compilation.evaluation.evaluators

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.RewardGenerator

/**
 * Predicate Evaluator class responsible for evaluating predicates based on various tunable parameters.
 * Extends the AbstractAutomatonBasedPredicateEvaluator class.
 *
 * @property upperLengthBound The upper bound for the length of the predicate.
 * @property lowerLengthBound The lower bound for the length of the predicate.
 * @property predicateLengthWeight The weight given to the length of the predicate. The length fitness is scored based on the discrepancy observed between the predicate length and the desired range from the bounds parameters.
 * @property exampleTraceWasteWeight The weight given to the sample steps not processed by the generated automaton. Waste is computed as the average % of unprocessed steps per trace over trace length.
 * @property exampleTraceCorrectnessWeight The weight given to the average trace correctness (progress).
 * @property counterExamplesRejectedWeight The weight given to the NUMBER of counter example traces that are not modelled by the given predicate.
 */
class ParameterizedPredicateEvaluator(
    private val upperLengthBound: Int,
    private val lowerLengthBound: Int,
    private val predicateLengthWeight: Float,
    private val exampleTraceWasteWeight: Float,
    private val exampleTraceCorrectnessWeight: Float,
    private val counterExamplesRejectedWeight: Float,
) : AbstractAutomatonBasedPredicateEvaluator() {

    override fun evaluatePredicate(
        predicate: String, examples: List<Array<RealValuation>>, counterExamples: List<Array<RealValuation>>
    ): Float {
        // compile an automaton accepting the same set of words as the input
        // predicate generated by the codon.
        val automaton = this.computeAutomaton(predicate)

        /**
         * Compute average example trace correctness and waste from the given predicate.
         * Waste is computed as the proportion of unused steps in the trace after the resulting automaton
         * decides to accept or reject a word.
         */
        val (avgExamplesCorrectness, avgExamplesWaste) = examples.fold(
            Pair(
                0.0f, 0.0f
            )
        ) { (accCorrectness, accWaste), example ->
            val progressData = RewardGenerator.evaluateTrace(automaton, example)
            Pair(
                accCorrectness + progressData.endProgress / examples.size.toFloat(),
                accWaste + progressData.remainingSteps / example.size.toFloat()
            )
        }

        /**
         * Gives a score of 1 to a 0-waste predicate, and gradually decreases this value down to 0 as the waste
         * approaches the entirety of the samples.
         */
        val exampleTraceWasteScore = 1f - avgExamplesWaste

        /**
         * Gives a score of 1 to a predicate that rejects all counter examples (that is, that does NOT reach a progress
         * value of 1f after execution on any of the given counter examples). Decreases this value down to 0 per each
         * counter example that ends up being modelled by the predicate.
         */
        val counterExamplesRejectedScore = counterExamples.count {
            RewardGenerator.evaluateTrace(
                automaton, it
            ).endProgress < 1f
        } / counterExamples.size.toFloat()

        /**
         * Gives a score of 1 to any predicate length included in the objective range, and increasingly lower scores
         * the further away predicate length is from any of the range extremes.
         */
        val predicateLengthScore = when {
            (predicate.length in lowerLengthBound..upperLengthBound) -> 1f
            predicate.length < lowerLengthBound -> 1f - (lowerLengthBound - predicate.length).toFloat() / (upperLengthBound - lowerLengthBound)
            else -> 1f - (predicate.length - upperLengthBound).toFloat() / (upperLengthBound - lowerLengthBound)
        }

        return (predicateLengthWeight * predicateLengthScore) +
                (exampleTraceWasteWeight * exampleTraceWasteScore) +
                (counterExamplesRejectedWeight * counterExamplesRejectedScore) +
                (exampleTraceCorrectnessWeight * avgExamplesCorrectness)
    } // evaluatePredicate
} // ParameterizedPredicateEvaluator