package predcompiler.compilation.evaluation.statevariables

class IdentityMapping(
    /**
     * Variable that will be transformed into a Column within a real valuation set.
     */
    override val predicateRepresentation: String
) : AbstractStateToRobustnessMapping() {
    override fun mapRobustness(state: HashMap<String, Float>): Float {
        return state[predicateRepresentation] ?: -1f
    }
} // IdentityMapping

