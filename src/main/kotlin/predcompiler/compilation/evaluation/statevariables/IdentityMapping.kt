package predcompiler.compilation.evaluation.statevariables

class IdentityMapping(
    /**
     * Variable that will be transformed into a Column within a real valuation set.
     */
    private val targetVariable: String,
) : AbstractStateToRobustnessMapping() {
    override fun mapRobustness(state: HashMap<String, Float>): Float {
        return state[targetVariable] ?: return -1f
    }

    override val predicateRepresentation: String
        get() {
            return targetVariable
        }
} // IdentityMapping

