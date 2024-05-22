package predcompiler.compilation.evaluation.statevariables;

import java.util.HashMap;

public class IdentityMapping extends AbstractStateToRobustnessMapping {

	/**
	 * Variable that will be transformed into a Column within a real valuation set.
	 */
	private String targetVariable;

	public IdentityMapping(String targetVariable) {
		this.targetVariable = targetVariable;
	}

	@Override
	public float mapRobustness(HashMap<String, Float> state) {
		return state.get(targetVariable);
	}

	@Override
	public String getPredicateRepresentation() {
		return targetVariable;
	}

	@Override
	public String[] getPossibleParams() {
		return new String[0];
	}

} // IdentityMapping
