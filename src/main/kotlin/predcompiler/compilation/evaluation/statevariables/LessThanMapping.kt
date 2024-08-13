package predcompiler.compilation.evaluation.statevariables

class LessThanMapping(
    /**
     * Variable that will be transformed into a Column within a real valuation set.
     */
    private val targetVariable: String,
    /**
     * Value that target variable will be compared to.
     */
    private val c: Double,
    /**
     * Lower bound value for the comparison. Any target variable value under this
     * threshold will be mapped to a robustness of +1.
     */
    private val lowerBound: Double,
    /**
     * Upper bound value for the comparison. Any target variable value beyond this
     * threshold will be mapped to a robustness of -1.
     */
    private val upperBound: Double
) : AbstractStateToRobustnessMapping() {
    override fun mapRobustness(state: HashMap<String, Double>): Double {
        val value = state[targetVariable] ?: return -1.0
        return if (value > c) {
            (value - c) / (c - upperBound)
        } else {
            (value - c) / (lowerBound - c)
        }
    }

    override val predicateRepresentation: String
        get() {
            val cString = c.toString()
            val safeCString = cString.replace('.', 'p')
            return targetVariable + "LT" + safeCString
        }
} // LessThanMapping

