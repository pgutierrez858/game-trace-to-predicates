package predcompiler.compilation.evaluation.statevariables

class LessThanMapping(
    /**
     * Variable that will be transformed into a Column within a real valuation set.
     */
    private val targetVariable: String,
    /**
     * Value that target variable will be compared to.
     */
    private val c: Float,
    /**
     * Lower bound value for the comparison. Any target variable value under this
     * threshold will be mapped to a robustness of +1.
     */
    private val lowerBound: Float,
    /**
     * Upper bound value for the comparison. Any target variable value beyond this
     * threshold will be mapped to a robustness of -1.
     */
    private val upperBound: Float
) : AbstractStateToRobustnessMapping() {
    override fun mapRobustness(state: HashMap<String, Float>): Float {
        val value = state[targetVariable] ?: return -1f
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

