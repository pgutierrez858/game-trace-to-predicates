package predcompiler.compilation.evaluation;

import rabinizer.formulas.BooleanConstant;
import rabinizer.formulas.Conjunction;
import rabinizer.formulas.Disjunction;
import rabinizer.formulas.Formula;
import rabinizer.formulas.Literal;
import rabinizer.formulas.Negation;

public class RobustnessEvaluator {

	public static float evaluateRobustness(Formula formula, RealValuation valuation) {
		@SuppressWarnings("rawtypes")
		Class type = formula.getClass();
		if (type == Literal.class) {
			Literal literal = (Literal) formula;
			return (literal.negated ? -1f : 1f) * valuation.get(literal.atomId);
		}

		if (type == BooleanConstant.class) {
			BooleanConstant bool = (BooleanConstant) formula;
			return bool.value ? 1f : -1f;
		}

		if (type == Negation.class) {
			Negation negation = (Negation) formula;
			return -RobustnessEvaluator.evaluateRobustness(negation.operand, valuation);
		}

		if (type == Conjunction.class) {
			Conjunction conjunction = (Conjunction) formula;
			float pA = RobustnessEvaluator.evaluateRobustness(conjunction.left, valuation);
			float pB = RobustnessEvaluator.evaluateRobustness(conjunction.right, valuation);

			float weakestGoal = Math.min(pA, pB);
			float strongestGoal = Math.max(pA, pB);

			/*
			 * how much impact the weakest goal has in the final result. Setting this to 1
			 * is equivalent to taking the minimum between the two values, while setting it
			 * to 0 simply selects the strongest goal. 0.5 returns the average of the two.
			 */
			float weakestGoalStrength = 0.75f;

			// same sign robustness
			if (weakestGoal * strongestGoal >= 0)
				return weakestGoalStrength * weakestGoal + (1 - weakestGoalStrength) * strongestGoal;

			/**
			 * different sign robustness: one of the conditions does not hold. However, if
			 * we just take the minimum of the two, it will essentially ignore all
			 * information about the more robust predicate. In order to circumvent this to
			 * an extent, we can make it so that the robustness value becomes slightly less
			 * negative as the positive objective improves, while never allowing the result
			 * to exceed 0 if at least one objective is still negative.
			 */
			/**
			 * We start at -1 and fill two sections of the interval [-1, 0] depending on the
			 * value of each subgoal. The negative, weakest goal fills the section given by
			 * the interval [-1, -1 + weakestGoalStrength]: when it is -1, then nothing is
			 * filled, whereas when this is 0, the entirety of the section should be
			 * completed. The positive, strongest goal fills the remaining part of the
			 * interval: when it is 0, then nothing is filled and so this becomes a relaxed
			 * version of the minimum; when this is 1, then the entire subinterval is filled
			 * with a positive value.
			 */
			return -1 + weakestGoalStrength * (1 + weakestGoal) + (1 - weakestGoalStrength) * strongestGoal;
		}

		if (type == Disjunction.class) {
			Disjunction disjunction = (Disjunction) formula;
			float pA = RobustnessEvaluator.evaluateRobustness(disjunction.left, valuation);
			float pB = RobustnessEvaluator.evaluateRobustness(disjunction.right, valuation);
			return Math.max(pA, pB);
		}

		// TODO safety check
		return -1f;
	} // evaluateRobustness

} // RobustnessEvaluator
