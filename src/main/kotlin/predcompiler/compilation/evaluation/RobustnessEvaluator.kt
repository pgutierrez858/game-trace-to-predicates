package predcompiler.compilation.evaluation

import rabinizer.formulas.*
import kotlin.math.max
import kotlin.math.min

object RobustnessEvaluator {
    fun evaluateRobustness(formula: Formula, valuation: RealValuation): Double {
        val type: Class<*> = formula.javaClass
        if (type == Literal::class.java) {
            val literal = formula as Literal
            return (if (literal.negated) -1.0 else 1.0) * valuation[literal.atom]!!
        }

        if (type == BooleanConstant::class.java) {
            val bool = formula as BooleanConstant
            return if (bool.value) 1.0 else -1.0
        }

        if (type == Negation::class.java) {
            val negation = formula as Negation
            return -evaluateRobustness(negation.operand, valuation)
        }

        if (type == Conjunction::class.java) {
            val conjunction = formula as Conjunction
            val pA = evaluateRobustness(conjunction.left, valuation)
            val pB = evaluateRobustness(conjunction.right, valuation)

            val weakestGoal = min(pA, pB)
            val strongestGoal = max(pA, pB)

            /*
			 * how much impact the weakest goal has in the final result. Setting this to 1
			 * is equivalent to taking the minimum between the two values, while setting it
			 * to 0 simply selects the strongest goal. 0.5 returns the average of the two.
			 */
            val weakestGoalStrength = 0.85

            // same sign robustness
            if (weakestGoal * strongestGoal >= 0.0)
                return weakestGoalStrength * weakestGoal + (1.0 - weakestGoalStrength) * strongestGoal

            /*
			  different sign robustness: one of the conditions does not hold. However, if
			  we just take the minimum of the two, it will essentially ignore all
			  information about the more robust predicate. In order to circumvent this to
			  an extent, we can make it so that the robustness value becomes slightly less
			  negative as the positive objective improves, while never allowing the result
			  to exceed 0 if at least one objective is still negative.
			 */
            /*
			  We start at -1 and fill two sections of the interval [-1, 0] depending on the
			  value of each sub goal. The negative, weakest goal fills the section given by
			  the interval [-1, -1 + weakestGoalStrength]: when it is -1, then nothing is
			  filled, whereas when this is 0, the entirety of the section should be
			  completed. The positive, strongest goal fills the remaining part of the
			  interval: when it is 0, then nothing is filled and so this becomes a relaxed
			  version of the minimum; when this is 1, then the entire sub-interval is filled
			  with a positive value.
			 */
            return -1.0 + weakestGoalStrength * (1.0 + weakestGoal) + (1.0 - weakestGoalStrength) * strongestGoal
        }

        if (type == Disjunction::class.java) {
            val disjunction = formula as Disjunction
            val pA = evaluateRobustness(disjunction.left, valuation)
            val pB = evaluateRobustness(disjunction.right, valuation)
            return max(pA, pB)
        }

        // TODO safety check
        return -1.0
    } // evaluateRobustness
} // RobustnessEvaluator
