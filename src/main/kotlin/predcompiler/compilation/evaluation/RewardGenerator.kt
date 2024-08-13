package predcompiler.compilation.evaluation

import predcompiler.compilation.evaluation.DTRAUtils.bestImprovingRobustness
import predcompiler.compilation.evaluation.DTRAUtils.isAcceptanceState
import predcompiler.compilation.evaluation.DTRAUtils.maxOutgoingRobustnessToTerminalState
import predcompiler.compilation.evaluation.DTRAUtils.maxRobustness
import rabinizer.automata.DTRA
import rabinizer.automata.ProductDegenState
import kotlin.math.max
import kotlin.math.min

object RewardGenerator {
    /**
     * Returns a Hash Map indicating the minimum number of steps needed to connect a
     * given state to any acceptance state. This will be 0 if the queried state is
     * already an acceptance state, and the size of the automaton if it is a trap
     * state (Note that no minimal path can ever have a length equal to the number
     * of nodes in the automaton).
     */
    private fun computeShortestDistancesToAcceptance(automaton: DTRA): HashMap<ProductDegenState, Int> {
        val acceptanceStates = HashSet<ProductDegenState>()

        val dist = HashMap<ProductDegenState, HashMap<ProductDegenState, Int>>()
        for (state in automaton.states) {
            val adjacentNodes = HashMap<ProductDegenState, Int>()
            adjacentNodes[state] = 0
            dist[state] = adjacentNodes

            // cache acceptance states
            if (isAcceptanceState(state, automaton)) acceptanceStates.add(state)
        }

        for ((state, value) in automaton.transitions) {
            val stateTransitions = dist[state]!!
            for (edgeData in value.values) {
                if (edgeData != state) stateTransitions[edgeData] = 1 // endState
            }
        }

        for (stateK in automaton.states) {
            val distK = dist[stateK]!!
            for (stateI in automaton.states) {
                val distI = dist[stateI]!!
                for (stateJ in automaton.states) {
                    distI.putIfAbsent(stateK, automaton.size())
                    distK.putIfAbsent(stateJ, automaton.size())
                    val distIK = distI[stateK]!!
                    val distKJ = distK[stateJ]!!

                    distI.putIfAbsent(stateJ, automaton.size())
                    if (distI[stateJ]!! > distIK + distKJ) {
                        distI[stateJ] = distIK + distKJ
                    }
                }
            }
        }

        val distanceToAcceptance = HashMap<ProductDegenState, Int>()
        for (state in automaton.states) {
            var bestDistance = automaton.size()
            distanceToAcceptance[state] = bestDistance
            val stateDistances = dist[state]!!
            for (accState in acceptanceStates) {
                val aux = stateDistances[accState]!! // distance to this particular acceptance state
                if (aux < bestDistance) {
                    bestDistance = aux
                }
            }
            distanceToAcceptance[state] = bestDistance
        }

        return distanceToAcceptance
    } // computeShortestDistancesToAcceptance

    /**
     * Computes the % of a specification that has been fulfilled on a given trace by
     * the end of execution. Output values < 1f reflect a lack of acceptance of the
     * trace. Rejection via trap state is represented by a progress of 0f. Progress
     * metrics do NOT take into account any optimization goals and just focus on
     * distance to good final states. In the case that no acceptance states are
     * present in the automaton, then progress will be reported as 1 if the trace
     * manages to stay away from trap states, and 0 in any other case.
     */
    fun evaluateTrace(automaton: DTRA, trace: List<RealValuation>): EvaluationReport {
        // calculate how far away each state is from an acceptance state (any).

        val distancesToAcceptance = computeShortestDistancesToAcceptance(automaton)
        /*
		 * biggest distance to an acceptance state. it could be the case that
		 * maxDistance >= automaton.size() this means that the automaton does NOT have
		 * acceptance states, which in turn implies that we can be satisfied as long as
		 * we are not rejecting a trace. In these cases, we can assume that base
		 * progress is always 1 unless a trap state is reached.
		 */
        var maxDistance = 0
        var terminalStates = 0
        for (d in distancesToAcceptance.values) {
            terminalStates += if (d == automaton.size()) 1 else 0
            if (d < automaton.size() && d > maxDistance) maxDistance = d
        }

        if (terminalStates == automaton.size()) maxDistance = automaton.size()

        /*
		 * quick calculation of the acceptance condition property check. that is,
		 * whether this automaton will have at least an acceptance state. This
		 * influences some edge cases where we are forced to process progress
		 * differently to accommodate this scenario.
		 */
        val hasAcceptanceConditions = maxDistance < automaton.size()

        // initialize automaton state and base metrics.
        var automatonState = automaton.initialState
        var endReward = 0.0
        var meaningfulStepCount = 0.0
        var lastStepProgress = 0.0
        var endProgress = 0.0
        var t = 0

        while (t < trace.size) {
            // value of the observed predicates at step t, after taking the action.
            val st = trace[t]
            val robustnessInfo = maxRobustness(
                automatonState, automaton, st
            )

            // cache whether the state has an outgoing transition to an external node.
            // this will be needed in more than one case below
            val hasOutgoingTransition = robustnessInfo.bestOutgoingEdgeData?.edgePredicate != null

            /*
             * this condition essentially translates to: we have found an outgoing edge with
             * a positive robustness value (true, qualitatively speaking), so we can just
             * take that transition.
             */
            if (robustnessInfo.bestOutgoingEdgeData != null &&
                robustnessInfo.bestOutgoingEdgeData.edgeRobustness > 0.0) {
                // there is an outgoing transition to take => take it
                automatonState = automaton.transitions[automatonState]!![robustnessInfo.bestOutgoingEdgeData.edgePredicate]
            }

            /*
			 * we can compute our current progress within the specification by looking at
			 * the new distance to goal states. This also tells us if we have fallen into a
			 * trap state or landed in an acceptance one. Note that in the case where no
			 * acceptance states are present in the automaton, it suffices to check whether
			 * we are in a trap state, but the check in that case is no longer distance
			 * based: we need to check if it only has self transitions.
			 */
            if (hasAcceptanceConditions) {
                // current distance to acceptance condition
                val currentDistance = distancesToAcceptance[automatonState]
                require(currentDistance != null) { "Current distance to acceptance state is null" }

                // ----------------------------------------------------------------------------------
                // CHECK FOR TERMINAL STATES
                // ----------------------------------------------------------------------------------
                // A) is this a trap state?
                if (currentDistance == automaton.size()) {
                    /*
					 * ready to terminate the episode with an unfavorable result. trap states have a
					 * progress of 0f, but accumulated reward is still reported.
					 */
                    endProgress = 0.0
                    break
                } else if (currentDistance == 0 && !hasOutgoingTransition) {
                    // terminate with a good result.
                    endProgress = 1.0
                    endReward += 1.0
                    break
                } // success state


                // ----------------------------------------------------------------------------------

                // ----------------------------------------------------------------------------------
                // COMPUTE PROGRESS FROM CURRENT POINT
                // ----------------------------------------------------------------------------------

                // % progress that automaton has just by being on this state.
                val baseProgress = (1.0 - currentDistance.toDouble() / max(1.0, maxDistance.toDouble()))

                /*
				 * out of the transitions that we could take from this new state, which one is
				 * the one that would lead us closer to a goal state?
				 */
                val bestImprovingRobustness = bestImprovingRobustness(
                    automatonState, automaton,
                    distancesToAcceptance, st
                )

                if (currentDistance == 0 && bestImprovingRobustness < 0.0) {
                    // penalize transitions away from acceptance state
                    // this edgeRobustness value will be positive as we are
                    // going to be transitioning away from this state for certain.
                    endReward -= robustnessInfo.bestOutgoingEdgeData?.edgeRobustness!!
                    // technically at this step, the progress is maximal...
                    endProgress = 1.0
                } else {
                    /*
					 * if this best improving robustness is already > 0, then we are at a position
					 * to transition in the next step (if things remain the way they are at st, at
					 * least). Otherwise, we can estimate how much has already been progressed based
					 * on how far this robustness is from 0.
					 */
                    val subtaskProgress =
                        ((1.0 + min(0.0, bestImprovingRobustness)) / maxDistance)

                    // update end progress with current progress
                    endProgress = min(1.0, (baseProgress + subtaskProgress).toDouble())

                    // update endReward with best improving robustness
                    endReward += bestImprovingRobustness
                }

                // ----------------------------------------------------------------------------------
            } else {
                /*
				 * no acceptance conditions in this case; this means progress is always binary:
				 * 0 if we are in a trap state and 1 elsewhere. We can check for terminal states
				 * by seeing if there are no outgoing edges from the current state.
				 */
                val isTrapState = !hasOutgoingTransition
                if (isTrapState) {
                    /*
					 * same case as before, but now this is always unfavorable.
					 */
                    endProgress = 0.0
                    break
                }

                // any other case has 1f as endProgress.
                endProgress = 1.0

                /*
				 * we now need to know the robustness of outgoing edges leading to trap states.
				 * If there are none, then we are safe, and can grant a reward of +1. If there
				 * is at least one, then we can potentially land in an unsafe state, so the
				 * reward is inversely proportional to that edge's strength, meaning that
				 * "we are still safe, but we want to remain as far away as we can from the condition that will trigger a trap"
				 */
                val closestTrappingRobustness = maxOutgoingRobustnessToTerminalState(
                    automatonState,
                    automaton, st
                )
                endReward += -max(-1.0, closestTrappingRobustness)
            }

            // update the step count
            meaningfulStepCount += if (lastStepProgress < endProgress) 1 else 0
            lastStepProgress = endProgress
            // t = t + 1
            ++t
        }

        return EvaluationReport(
            trace.size - t,
            endProgress,
            meaningfulStepCount / trace.size.toDouble())
    } // computeProgress
} // RewardGenerator
