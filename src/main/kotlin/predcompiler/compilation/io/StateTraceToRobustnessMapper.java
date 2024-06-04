package predcompiler.compilation.io;

import java.util.HashMap;
import java.util.List;

import predcompiler.compilation.evaluation.RealValuation;
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping;
import rabinizer.bdd.BDDForVariables;

public class StateTraceToRobustnessMapper {

	private final AbstractStateToRobustnessMapping[] mappingFunctions;

	public StateTraceToRobustnessMapper(AbstractStateToRobustnessMapping[] mappingFunctions) {
		this.mappingFunctions = mappingFunctions;
	} // StateTraceToRobustnessMapper

	public RealValuation[] mapStateTraceToRobustness(List<HashMap<String, Float>> stateTrace) {
		RealValuation[] result = new RealValuation[stateTrace.size()];

		for (int i = 0; i < result.length; i++) {
			result[i] = new RealValuation();

			for (var mappingFunc : mappingFunctions) {
				// f(s) \in [-1, 1]
				float mappingRobustness = mappingFunc.mapRobustness(stateTrace.get(i));
				// name of the mapping function, will act as a new atomic proposition
				String mappingName = mappingFunc.getPredicateRepresentation();

				int mappingId = BDDForVariables.bijectionIdAtom.id(mappingName);
				result[i].set(mappingId, mappingRobustness);
			}
		}
		return result;
	} // mapStateTraceToRobustness

} // StateTraceToRobustnessMapper
