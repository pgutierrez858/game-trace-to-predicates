package predcompiler.compilation.evaluation

import rabinizer.automata.AccTR
import rabinizer.automata.DTRA
import rabinizer.automata.ProductDegenState
import rabinizer.automata.TranSet
import rabinizer.bdd.ValuationSet
import rabinizer.exec.Main
import rabinizer.exec.Main.AutomatonType
import java.io.OutputStream
import java.io.PrintStream

object DTRAUtils {

    private fun transformPredicate(predicate: String): String {
        // regex for SSEQ[args], where args is a sequence of strings separated by commas.
        return predicate.replace("SSEQ\\[(.*?)]".toRegex()) {
            // sequence of args included in the SSEQ[] label.
            val args = it.groupValues[1].split(",")

            // case 0: no arguments, return empty string.
            if(args.isEmpty()) return@replace ""

            // at least one argument, select the last one to start the sequence from right to left
            var sequenceResult = "F(${args[args.size - 1]})"

            // only one element, establish that we simply wish for that element to hold at some point.
            if(args.size == 1) return@replace sequenceResult

            // we have more than one element, iteratively build a sequence with restrictions with until clauses
            val orderResults = mutableListOf<String>()
            for(i in args.size - 2 downTo 0){
                // build rule: F(p & X(F(q)))
                sequenceResult = "F((${args[i]}) & X(${sequenceResult}))"
                // build rule: !p U q
                orderResults.add("(!(${args[i + 1]}) U (${args[i]}))")
            }
            "${sequenceResult}&${orderResults.joinToString("&")}"
        }
    } // transformPredicate

    /**
     * Convenience method for evaluators which need to make use of a Rabin
     * Automaton for computing fitness. Computes a DTRA in silent mode for a
     * specified predicate.
     */
    fun computeAutomaton(predicate: String?): DTRA {
        val originalStream = System.out

        val dummyStream = PrintStream(object : OutputStream() {
            override fun write(b: Int) {
                // NO-OP
            }
        })

        System.setOut(dummyStream)
        val automaton = Main.computeAutomaton(transformPredicate(predicate ?: ""), AutomatonType.TR, true, false, true) as DTRA
        System.setOut(originalStream)

        return automaton
    } // computeAutomaton

    /**
     * Checks whether a word would be accepted for a given automaton if execution
     * ended at a given state. We choose to call this an "acceptance state" for
     * legibility purposes, but it remains important to remember that Rabin automata
     * have their own set of rules to define acceptance.
     */
    fun isAcceptanceState(state: ProductDegenState, automaton: DTRA): Boolean {
        try {
            // for some reason, accTR, representing the acceptance conditions
            // for a Rabin automaton of type DTRA, is not publicly accessible, so
            // we must resort to un-orthodox methods to gain access to it.
            val acceptanceField = DTRA::class.java.getDeclaredField("accTR")
            acceptanceField.isAccessible = true
            val acceptance = acceptanceField[automaton] as AccTR

            // once we have the acceptance conditions object, we can determine whether
            // a state could be "accepting" if, for any of the acceptance conditions listed,
            // it is included in the difference set between Inf and Fin sets of the
            // condition.
            // That is, if it is expected to be visited Infinitely often but not Finitely
            // often.
            for (rabinPair in acceptance) {
                val finSet = rabinPair.left
                val infSet = rabinPair.right
                val accSet = TranSet<ProductDegenState>()
                accSet.addAll(infSet)

                // another un-orthodox access to private elements, this time from
                // the TranSet class to access the otherwise protected removeAll method
                // which is quite useful to compute the difference set between inf and fin
                val removeAllMethod = TranSet::class.java.getDeclaredMethod("removeAll", TranSet::class.java)
                removeAllMethod.isAccessible = true
                removeAllMethod.invoke(accSet, finSet)

                // accSet will now only have the sets that will grant acceptance if visited
                // infinitely
                // if our state is included in this set, then we can consider it "accepting"
                if (accSet.containsKey(state)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        return false
    } // isAcceptanceState

    /**
     * Checks whether a given state in an automaton has at least one transition
     * leading to a state different to itself.
     */
    private fun hasOutgoingTransition(state: ProductDegenState, automaton: DTRA): Boolean {
        val transitions = automaton.transitions[state]
        if (transitions.isNullOrEmpty()) return false
        for (endState in transitions.values) {
            // iterate over end states looking for an item different from state
            if (endState.toString() != state.toString()) return true
        }
        return false
    } // hasOutgoingTransition

    /**
     * Returns the robustness of the most robust transition from a given state in
     * the automaton leading to a state strictly closer to an acceptance one. For
     * acceptance states, returns the robustness of the best transition that keeps
     * the automaton in an acceptance state (including self transitions).
     */
    fun bestImprovingRobustness(
        state: ProductDegenState, automaton: DTRA,
        distancesToAcceptance: HashMap<ProductDegenState, Int>, valuation: RealValuation
    ): Double {
        var bestRobustness = Double.NEGATIVE_INFINITY

        val transitions = automaton.transitions[state]
        if (transitions.isNullOrEmpty()) return bestRobustness

        val currentDistance = distancesToAcceptance[state]
        require(currentDistance != null) { "State $state not found in distancesToAcceptance map." }

        for ((key, endState) in transitions) {
            // iterate over end states looking for an item with better distance to
            // acceptance
            val newDistance = distancesToAcceptance[endState]
            require(newDistance != null) { "State $endState not found in distancesToAcceptance map." }
            if (newDistance < currentDistance || (newDistance == 0 && currentDistance == 0)) {
                val p = RobustnessEvaluator.evaluateRobustness(key.toFormula(), valuation)
                if (p > bestRobustness) bestRobustness = p
            }
        }
        return bestRobustness
    } // bestImprovingRobustness

    /**
     * Computes the maximum robustness of the self loop, if available, and the best outgoing edge from a given state.
     * Robustness values default to Double.NEGATIVE_INFINITY and best outgoing edge ValuationSet defaults to null if not found.
     */
    fun maxRobustness(state: ProductDegenState, automaton: DTRA, valuation: RealValuation): StateRobustnessInfo {
        val transitions = automaton.transitions[state]
        require(transitions != null) { "State $state not found in automaton transitions." }

        var bestOutgoingEdge: ValuationSet? = null
        var bestOutgoingRobustness = Double.NEGATIVE_INFINITY
        var selfRobustness = Double.NEGATIVE_INFINITY
        for ((key, value) in transitions) {
            val formula = key.toFormula()
            val p = RobustnessEvaluator.evaluateRobustness(formula, valuation)
            if (value == state) selfRobustness = p
            else {
                if (bestOutgoingRobustness < p) {
                    bestOutgoingEdge = key
                    bestOutgoingRobustness = p
                }
            }
        }

        return StateRobustnessInfo(
            selfLoopRobustness = selfRobustness,
            bestOutgoingEdgeData = EdgeRobustnessInfo(
                edgePredicate = bestOutgoingEdge,
                edgeRobustness = bestOutgoingRobustness
            )
        )
    } // maxOutgoingRobustness

    /**
     * Computes the maximum outgoing robustness of an edge leading to a terminal
     * state. This is useful when dealing with automata with no acceptance
     * conditions, where this calculation allows us to infer how far away we are
     * from entering a trap state.
     */
    fun maxOutgoingRobustnessToTerminalState(
        state: ProductDegenState, automaton: DTRA,
        valuation: RealValuation
    ): Double {
        var bestRobustness = Double.NEGATIVE_INFINITY

        val transitions = automaton.transitions[state]
        if (transitions.isNullOrEmpty()) return bestRobustness

        for ((key, endState) in transitions) {
            // iterate over end states looking for an item with no transitions
            val isTerminalTransition = !hasOutgoingTransition(endState, automaton)
            if (isTerminalTransition) {
                val p = RobustnessEvaluator.evaluateRobustness(key.toFormula(), valuation)
                if (p > bestRobustness) bestRobustness = p
            }
        }
        return bestRobustness
    } // maxOutgoingRobustnessToTerminalState
} // DTRAUtils
