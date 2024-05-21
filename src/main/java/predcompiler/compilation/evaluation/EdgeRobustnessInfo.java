package predcompiler.compilation.evaluation;

import rabinizer.bdd.ValuationSet;

public class EdgeRobustnessInfo {
	/**
	 * Predicate whose robustness is evaluated in edgeRobustness.
	 */
	public ValuationSet edgePredicate;
	/**
	 * Numerical robustness of edgePredicate in the current state of the game.
	 */
	public float edgeRobustness;
} // EdgeRobustnessInfo