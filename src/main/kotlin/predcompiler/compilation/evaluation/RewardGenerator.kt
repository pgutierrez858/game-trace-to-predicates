package predcompiler.compilation.evaluation;

import java.util.HashMap;
import java.util.HashSet;

import rabinizer.automata.DTRA;
import rabinizer.automata.ProductDegenState;

public class RewardGenerator {

	/**
	 * Returns a Hash Map indicating the minimum number of steps needed to connect a
	 * given state to any acceptance state. This will be 0 if the queried state is
	 * already an acceptance state, and the size of the automaton if it is a trap
	 * state (Note that no minimal path can ever have a length equal to the number
	 * of nodes in the automaton).
	 */
	public static HashMap<ProductDegenState, Integer> computeShortestDistancesToAcceptance(DTRA automaton) {

		HashSet<ProductDegenState> acceptanceStates = new HashSet<ProductDegenState>();

		var dist = new HashMap<ProductDegenState, HashMap<ProductDegenState, Integer>>();
		for (ProductDegenState state : automaton.states) {
			var adjacentNodes = new HashMap<ProductDegenState, Integer>();
			adjacentNodes.put(state, 0);
			dist.put(state, adjacentNodes);

			// cache acceptance states
			if (DTRAUtils.isAcceptanceState(state, automaton))
				acceptanceStates.add(state);
		}

		for (var transition : automaton.transitions.entrySet()) {
			ProductDegenState state = transition.getKey(); // sourceState
			var stateTransitions = dist.get(state);
			for (var edgeData : transition.getValue().values()) {
				if (!edgeData.equals(state))
					stateTransitions.put(edgeData, 1); // endState
			}
		}

		for (var stateK : automaton.states) {
			var distK = dist.get(stateK);
			for (var stateI : automaton.states) {
				var distI = dist.get(stateI);
				for (var stateJ : automaton.states) {
					distI.putIfAbsent(stateK, automaton.size());
					distK.putIfAbsent(stateJ, automaton.size());
					int distIK = distI.get(stateK);
					int distKJ = distK.get(stateJ);

					distI.putIfAbsent(stateJ, automaton.size());
					if (distI.get(stateJ) > distIK + distKJ) {
						distI.put(stateJ, distIK + distKJ);
					}
				}
			}
		}

		HashMap<ProductDegenState, Integer> distanceToAcceptance = new HashMap<ProductDegenState, Integer>();
		for (var state : automaton.states) {
			int bestDistance = automaton.size();
			distanceToAcceptance.put(state, bestDistance);
			var stateDistances = dist.get(state);
			for (var accState : acceptanceStates) {
				int aux = stateDistances.get(accState); // distance to this particular acceptance state
				if (aux < bestDistance) {
					bestDistance = aux;
				}
			}
			distanceToAcceptance.put(state, bestDistance);
		}

		return distanceToAcceptance;
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
	public static EvaluationReport evaluateTrace(DTRA automaton, RealValuation[] trace) {

		// calculate how far away each state is from an acceptance state (any).
		var distancesToAcceptance = computeShortestDistancesToAcceptance(automaton);
		/*
		 * biggest distance to an acceptance state. it could be the case that
		 * maxDistance >= automaton.size() this means that the automaton does NOT have
		 * acceptance states, which in turn implies that we can be satisfied as long as
		 * we are not rejecting a trace. In these cases, we can assume that base
		 * progress is always 1 unless a trap state is reached.
		 */
		int maxDistance = 0;
		int terminalStates = 0;
		for (int d : distancesToAcceptance.values()) {
			terminalStates += d == automaton.size() ? 1 : 0;
			if (d < automaton.size() && d > maxDistance)
				maxDistance = d;
		}

		if (terminalStates == automaton.size())
			maxDistance = automaton.size();

		/*
		 * quick calculation of the acceptance condition property check. that is,
		 * whether this automaton will have at least an acceptance state. This
		 * influences some edge cases where we are forced to process progress
		 * differently to accommodate this scenario.
		 */
		boolean hasAcceptanceConditions = maxDistance < automaton.size();

		// initialize automaton state and base metrics.
		ProductDegenState automatonState = automaton.initialState;
		float endReward = 0f;
		float endProgress = 0f;
		int t = 0;

		while (t < trace.length) {
			// value of the observed predicates at step t, after taking the action.
			RealValuation st = trace[t];
			var robustnessInfo = DTRAUtils.maxRobustness(automatonState, automaton, st);

			// cache whether the state has an outgoing transition to an external node.
			// this will be needed in more than one case below
			boolean hasOutgoingTransition = robustnessInfo.bestOutgoingEdgeData.edgePredicate != null;

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
				int currentDistance = distancesToAcceptance.get(automatonState);

				// ----------------------------------------------------------------------------------
				// CHECK FOR TERMINAL STATES
				// ----------------------------------------------------------------------------------
				// A) is this a trap state?
				if (currentDistance == automaton.size()) {
					/*
					 * ready to terminate the episode with an unfavorable result. trap states have a
					 * progress of 0f, but accumulated reward is still reported.
					 */
					endProgress = 0f;
					break;
				}
				// B) is this an acceptance state without outgoing transitions?
				else if (currentDistance == 0 && !hasOutgoingTransition) {
					// terminate with a good result.
					endProgress = 1f;
					endReward += 1f;
					break;
				} // success state
					// ----------------------------------------------------------------------------------

				// ----------------------------------------------------------------------------------
				// COMPUTE PROGRESS FROM CURRENT POINT
				// ----------------------------------------------------------------------------------

				// % progress that automaton has just by being on this state.
				float baseProgress = 1f - (float) currentDistance / Math.max(1, maxDistance);

				/*
				 * out of the transitions that we could take from this new state, which one is
				 * the one that would lead us closer to a goal state?
				 */
				float bestImprovingRobustness = DTRAUtils.bestImprovingRobustness(automatonState, automaton,
						distancesToAcceptance, st);

				if (currentDistance == 0 && bestImprovingRobustness < 0) {
					// penalize transitions away from acceptance state
					// this edgeRobustness value will be positive as we are
					// going to be transitioning away from this state for certain.
					endReward -= robustnessInfo.bestOutgoingEdgeData.edgeRobustness;
					// technically at this step, the progress is maximal...
					endProgress = 1f;
				}

				else {
					/*
					 * if this best improving robustness is already > 0, then we are at a position
					 * to transition in the next step (if things remain the way they are at st, at
					 * least). Otherwise, we can estimate how much has already been progressed based
					 * on how far this robustness is from 0.
					 */
					float subtaskProgress = (1f + Math.min(0f, bestImprovingRobustness)) / maxDistance;

					// update end progress with current progress
					endProgress = Math.min(1f, baseProgress + subtaskProgress);

					// update endReward with best improving robustness
					endReward += bestImprovingRobustness;
				}

				// ----------------------------------------------------------------------------------
			} else {
				/*
				 * no acceptance conditions in this case; this means progress is always binary:
				 * 0 if we are in a trap state and 1 elsewhere. We can check for terminal states
				 * by seeing if there are no outgoing edges from the current state.
				 */
				boolean isTrapState = !hasOutgoingTransition;
				if (isTrapState) {
					/*
					 * same case as before, but now this is always unfavorable.
					 */
					endProgress = 0f;
					break;
				}

				// any other case has 1f as endProgress.
				endProgress = 1f;

				/*
				 * we now need to know the robustness of outgoing edges leading to trap states.
				 * If there are none, then we are safe, and can grant a reward of +1. If there
				 * is at least one, then we can potentially land in an unsafe state, so the
				 * reward is inversely proportional to that edge's strength, meaning that
				 * "we are still safe, but we want to remain as far away as we can from the condition that will trigger a trap"
				 */
				float closestTrappingRobustness = DTRAUtils.maxOutgoingRobustnessToTerminalState(automatonState,
						automaton, st);
				endReward += -Math.max(-1f, closestTrappingRobustness);
			}

			/*
			 * this condition essentially translates to: we have found an outgoing edge with
			 * a positive robustness value (true, qualitatively speaking), so we can just
			 * take that transition.
			 */
			if (robustnessInfo.bestOutgoingEdgeData.edgeRobustness > 0f) {
				// there is an outgoing transition to take => take it
				automatonState = automaton.transitions.get(automatonState)
						.get(robustnessInfo.bestOutgoingEdgeData.edgePredicate);
			}

			// t = t + 1
			++t;
		}

		return new EvaluationReport(trace.length - t, endProgress, endReward);
	} // computeProgress

} // RewardGenerator
