package predcompiler.compilation.evaluation.statevariables

/**
 * Atomic evaluation over system state f: R^n -> [-1, 1], which provides a
 * robustness value for a given function. Functions can accept building
 * parameters, to allow for greater flexibility during predicate
 * exploration: (RobustnessOverState, parameters) -> robustness function over
 * state.
 */
abstract class AbstractStateToRobustnessMapping {
    /**
     * Produces a floating value in the [-1, 1] interval representing the current
     * robustness of this predicate over the game state.
     */
    abstract fun mapRobustness(state: HashMap<String, Double>): Double

    /**
     * Represents this function over state as a predicate-safe String, i.e. a string
     * that starts with a letter and only contains alphanumeric characters.
     */
	abstract val predicateRepresentation: String
} // AbstractStateToRobustnessMapping
