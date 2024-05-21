package predcompiler.compilation.evaluation;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import rabinizer.automata.AccTR;
import rabinizer.automata.DTRA;
import rabinizer.automata.ProductDegenState;
import rabinizer.automata.RabinPair;
import rabinizer.automata.TranSet;
import rabinizer.bdd.ValuationSet;
import rabinizer.formulas.Formula;

public class DTRAUtils {

	/**
	 * Checks whether a word would be accepted for a given automaton if execution
	 * ended at a given state. We choose to call this an "acceptance state" for
	 * legibility purposes, but it remains important to remember that Rabin automata
	 * have their own set of rules to define acceptance.
	 */
	public static boolean isAcceptanceState(ProductDegenState state, DTRA automaton) {
		try {
			// for some reason, accTR, representing the acceptance conditions
			// for a Rabin automaton of type DTRA, is not publicly accessible, so
			// we must resort to un-orthodox methods to gain access to it.
			Field acceptanceField = DTRA.class.getDeclaredField("accTR");
			acceptanceField.setAccessible(true);
			AccTR acceptance = (AccTR) acceptanceField.get(automaton);

			// once we have the acceptance conditions object, we can determine whether
			// a state could be "accepting" if, for any of the acceptance conditions listed,
			// it is included in the difference set between Inf and Fin sets of the
			// condition.
			// That is, if it is expected to be visited Infinitely often but not Finitely
			// often.
			for (RabinPair<ProductDegenState> rabinPair : acceptance) {
				TranSet<ProductDegenState> finSet = rabinPair.left;
				TranSet<ProductDegenState> infSet = rabinPair.right;
				TranSet<ProductDegenState> accSet = new TranSet<ProductDegenState>();
				accSet.addAll(infSet);

				// another un-orthodox access to private elements, this time from
				// the TranSet class to access the otherwise protected removeAll method
				// which is quite useful to compute the difference set between inf and fin
				Method removeAllMethod = TranSet.class.getDeclaredMethod("removeAll", TranSet.class);
				removeAllMethod.setAccessible(true);
				removeAllMethod.invoke(accSet, finSet);

				// accSet will now only have the sets that will grant acceptance if visited
				// infinitely
				// if our state is included in this set, then we can consider it "accepting"
				if (accSet.containsKey(state)) {
					return true;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	} // isAcceptanceState

	/**
	 * Checks whether a given state in an automaton has at least one transition
	 * leading to a state different to itself.
	 */
	public static boolean hasOutgoingTransition(ProductDegenState state, DTRA automaton) {

		Map<ValuationSet, ProductDegenState> transitions = automaton.transitions.get(state);
		if (transitions == null || transitions.size() == 0)
			return false;
		for (ProductDegenState endState : transitions.values()) {
			// iterate over end states looking for an item different from state
			if (!endState.toString().equals(state.toString()))
				return true;
		}
		return false;
	} // hasOutgoingTransition

	/**
	 * Returns the robustness of the most robust transition from a given state in
	 * the automaton leading to a state strictly closer to an acceptance one. For
	 * acceptance states, returns the robustness of the best transition that keeps
	 * the automaton in an acceptance state (including self transitions).
	 */
	public static float bestImprovingRobustness(ProductDegenState state, DTRA automaton,
			HashMap<ProductDegenState, Integer> distancesToAcceptance, RealValuation valuation) {

		float bestRobustness = Float.NEGATIVE_INFINITY;

		Map<ValuationSet, ProductDegenState> transitions = automaton.transitions.get(state);
		if (transitions == null || transitions.size() == 0)
			return bestRobustness;

		int currentDistance = distancesToAcceptance.get(state);
		for (var transitionData : transitions.entrySet()) {
			var endState = transitionData.getValue();
			// iterate over end states looking for an item with better distance to
			// acceptance
			int newDistance = distancesToAcceptance.get(endState);
			if (newDistance < currentDistance || (newDistance == 0 && currentDistance == 0)) {
				float p = RobustnessEvaluator.evaluateRobustness(transitionData.getKey().toFormula(), valuation);
				if (p > bestRobustness)
					bestRobustness = p;
			}
		}
		return bestRobustness;

	} // bestImprovingRobustness

	/**
	 * Computes a tuple whose first parameter is the robustness of the self
	 * transition, if available, and whose second parameter is a second tuple
	 * containing the ValuationSet of the best outgoing edge and its robustness
	 * value. Robustness values default to Float.NEGATIVE_INFINITY and best outgoing
	 * edge ValuationSet defaults to null if not found.
	 * 
	 * @return
	 */
	public static StateRobustnessInfo maxRobustness(ProductDegenState state, DTRA automaton, RealValuation valuation) {
		Map<ValuationSet, ProductDegenState> transitions = automaton.transitions.get(state);
		ValuationSet bestOutgoingEdge = null;
		float bestOutgoingRobustness = Float.NEGATIVE_INFINITY;
		float selfRobustness = Float.NEGATIVE_INFINITY;
		for (Entry<ValuationSet, ProductDegenState> transition : transitions.entrySet()) {
			Formula formula = transition.getKey().toFormula();
			float p = RobustnessEvaluator.evaluateRobustness(formula, valuation);
			if (transition.getValue().equals(state))
				selfRobustness = p;
			else {
				if (bestOutgoingRobustness < p) {
					bestOutgoingEdge = transition.getKey();
					bestOutgoingRobustness = p;
				}
			}
		}

		var result = new StateRobustnessInfo();
		result.selfLoopRobustness = selfRobustness;
		result.bestOutgoingEdgeData = new EdgeRobustnessInfo();
		result.bestOutgoingEdgeData.edgePredicate = bestOutgoingEdge;
		result.bestOutgoingEdgeData.edgeRobustness = bestOutgoingRobustness;

		return result;
	} // maxOutgoingRobustness

	/**
	 * Computes the maximum outgoing robustness of an edge leading to a terminal
	 * state. This is useful when dealing with automata with no acceptance
	 * conditions, where this calculation allows us to infer how far away we are
	 * from entering a trap state.
	 */
	public static float maxOutgoingRobustnessToTerminalState(ProductDegenState state, DTRA automaton,
			RealValuation valuation) {
		float bestRobustness = Float.NEGATIVE_INFINITY;

		Map<ValuationSet, ProductDegenState> transitions = automaton.transitions.get(state);
		if (transitions == null || transitions.size() == 0)
			return bestRobustness;

		for (var transitionData : transitions.entrySet()) {
			var endState = transitionData.getValue();
			// iterate over end states looking for an item with no transitions
			boolean isTerminalTransition = !DTRAUtils.hasOutgoingTransition(endState, automaton);
			if (isTerminalTransition) {
				float p = RobustnessEvaluator.evaluateRobustness(transitionData.getKey().toFormula(), valuation);
				if (p > bestRobustness)
					bestRobustness = p;
			}
		}
		return bestRobustness;
	} // maxOutgoingRobustnessToTerminalState
} // DTRAUtils
