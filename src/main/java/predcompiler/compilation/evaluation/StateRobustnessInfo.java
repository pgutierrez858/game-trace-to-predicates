package predcompiler.compilation.evaluation;

public class StateRobustnessInfo {

	/**
	 * Robustness of the self loop transition, if any. Defaults to
	 * Float.NEGATIVE_INFINITY if no self loop exists.
	 */
	public float selfLoopRobustness;

	/**
	 * Information on the edge with the highest robustness of the outgoing edges.
	 */
	public EdgeRobustnessInfo bestOutgoingEdgeData;

} // StateRobustnessInfo