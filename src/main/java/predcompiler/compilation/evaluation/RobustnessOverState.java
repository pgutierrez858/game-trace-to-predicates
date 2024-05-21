package predcompiler.compilation.evaluation;

/**
 * Atomic evaluation over system state f: R^n -> [-1, 1], which provides a
 * robustness value for a given function. Functions can accept building
 * parameters, so as to allow for greater flexibility during predicate
 * exploration: (RobustnessOverState, parameters) -> robustness function over
 * state.
 */
public abstract class RobustnessOverState {

	/**
	 * Produces a floating value in the [-1, 1] interval representing the current
	 * robustness of this predicate over the game state.
	 */
	public abstract float evaluate(float[] stateVariables, String[] variableNames);

	/**
	 * Represents this function over state as a predicate-safe String, i.e. a string
	 * that starts with a letter and only contains alphanumeric characters.
	 */
	public abstract String getPredicateRepresentation();

	/**
	 * Represents all the parameters than can be accepted by this
	 */
	public abstract String[] getPossibleParams();
}
