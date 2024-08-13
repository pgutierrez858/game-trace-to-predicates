package predcompiler.compilation.io

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping

class StateTraceToRobustnessMapper(
    private val mappingFunctions: List<AbstractStateToRobustnessMapping>
) {
    fun mapStateTraceToRobustness(stateTrace: List<HashMap<String, Double>>): List<RealValuation> {
        return stateTrace.map { state ->
            RealValuation().also {
                mappingFunctions.forEach { mappingFunc ->
                    // f(s) \in [-1, 1]
                    val mappingRobustness = mappingFunc.mapRobustness(state)
                    // name of the mapping function, will act as a new atomic proposition
                    val mappingName = mappingFunc.predicateRepresentation
                    it.set(mappingName, mappingRobustness)
                }
            }
        }
    } // mapStateTraceToRobustness
} // StateTraceToRobustnessMapper

