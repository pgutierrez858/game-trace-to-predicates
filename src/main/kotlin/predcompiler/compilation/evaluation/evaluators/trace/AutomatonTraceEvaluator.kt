package predcompiler.compilation.evaluation.evaluators.trace

import predcompiler.compilation.evaluation.DTRAUtils
import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.RewardGenerator
import predcompiler.compilation.evaluation.evaluators.EvaluationResult
import rabinizer.automata.DTRA

class AutomatonTraceEvaluator: AbstractTraceEvaluator() {

    private var automaton: DTRA? = null
    private var predicate: String = ""

    private fun setModelPredicate(predicate: String) {
        this.predicate = predicate
        // compile an automaton accepting the same set of words as the input
        // predicate generated by the codon.
        automaton = DTRAUtils.computeAutomaton(predicate)
    } // setModelPredicate

    override fun evaluateTrace(predicate: String, trace: List<RealValuation>): EvaluationResult {
        // either this evaluator has not been initialized or the predicate has changed.
        if (automaton == null || predicate != this.predicate) {
            // set the new predicate and compute its automaton
            setModelPredicate(predicate)
        }

        val castAutomaton: DTRA? = automaton
        check(castAutomaton != null) { "Automaton could not be computed for predicate: $predicate" }

        val progressData = RewardGenerator.evaluateTrace(castAutomaton, trace)
        return hashMapOf(
            "progress" to progressData.endProgress,
            "waste" to 1f - progressData.remainingSteps / trace.size.toDouble(),
            "reward" to progressData.endReward
        )
    } // evaluateTrace
} // AutomatonTraceEvaluator