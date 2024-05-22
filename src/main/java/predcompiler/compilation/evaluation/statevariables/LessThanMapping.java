package predcompiler.compilation.evaluation.statevariables;

import java.util.HashMap;

public class LessThanMapping extends AbstractStateToRobustnessMapping {
	/**
	 * Variable that will be transformed into a Column within a real valuation set.
	 */
	private String targetVariable;

	/**
	 * Value that target variable will be compared to.
	 */
	private float c;

	/**
	 * Lower bound value for the comparison. Any target variable value under this
	 * threshold will be mapped to a robustness of +1.
	 */
	private float lowerBound;

	/**
	 * Upper bound value for the comparison. Any target variable value beyond this
	 * threshold will be mapped to a robustness of -1.
	 */
	private float upperBound;

	public LessThanMapping(String targetVariable, float c, float lowerBound, float upperBound) {
		this.targetVariable = targetVariable;
		this.c = c;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	@Override
	public float mapRobustness(HashMap<String, Float> state) {
		float value = state.get(targetVariable);
		if (value > c) {
			return (value - c) / (c - upperBound);
		} else {
			return (value - c) / (lowerBound - c);
		}
	}

	@Override
	public String getPredicateRepresentation() {
        String cString = Float.toString(c);
        String safeCString = cString.replace('.', 'p');
		return targetVariable + "LT" + safeCString;
	}

	@Override
	public String[] getPossibleParams() {
		return new String[0];
	}

} // LessThanMapping
