package predcompiler.compilation.evaluation;

import java.util.List;

import rabinizer.automata.DTRA;

/**
 * Evaluates a predicate giving the highest priority to example trace
 * satisfaction, followed by rejection of counter example traces, and finally
 * reward on example traces. More specifically, this will start by just looking
 * at the trace satisfaction rate, which will be used as the fitness value if it
 * is any lower than 1 (full satisfaction); once this reaches the maximum value,
 * it will begin looking at counter example satisfaction, adding a value of 1 -
 * average counter example satisfaction to the fitness level. Finally, and
 * assuming all traces are properly modeled by the predicate, a last tier of
 * fitness is added to include the reward granted by the chosen automaton reward
 * algorithm to the predicate over the sample traces.
 */
public class TieredPredicateEvaluator extends AbstractAutomatonBasedPredicateEvaluator {

	public float evaluatePredicate(String predicate, List<RealValuation[]> examples,
			List<RealValuation[]> counterExamples) {

		float totalGoodProgress = 0f;
		float totalBadProgress = 0f;
		float totalGoodReward = 0f;

		// compile an automaton accepting the same set of words as the input
		// predicate generated by the codon.
		DTRA automaton = this.computeAutomaton(predicate);

		/*
		 * compute how well the predicate matches the reference traces using one of the
		 * available reward generation algorithms. We need to do this for all counter
		 * examples and all examples, and then reward the predicate based on how well it
		 * fits the example predicates and how well it rejects the counter examples.
		 */
		float totalExampleProgress = 0f;
		float totalExampleReward = 0f;
		float totalCounterExampleProgress = 0f;

		for (var example : examples) {
			var progressData = RewardGenerator.evaluateTrace(automaton, example);
			totalExampleProgress += progressData.endProgress;
			totalExampleReward += progressData.endReward;
		}

		for (var counterExample : counterExamples) {
			var progressData = RewardGenerator.evaluateTrace(automaton, counterExample);
			totalCounterExampleProgress += progressData.endProgress;
		}

		// Individual Value:
		// example progress counts positively
		// counter example progress counts negatively
		// Assuming traces are fit with 100% acceptance on examples and 0% acceptance
		// on counter examples, then we need to take into account the length of the
		// solution
		// and the reward it manages to gather.
		float goodProgress = totalExampleProgress / examples.size();
		float badProgress = totalCounterExampleProgress / counterExamples.size();

		totalGoodProgress += goodProgress;
		totalBadProgress += badProgress;
		totalGoodReward += totalExampleReward;

		float fitness = 0f;
		if (totalGoodProgress < 1f)
			fitness = totalGoodProgress;
		else if (totalBadProgress > 0f)
			fitness = totalGoodProgress + (1 - totalBadProgress);
		else
			fitness = totalGoodProgress + (1 - totalBadProgress) + totalGoodReward;

		return fitness;
	} // evaluatePredicate
	
} // TieredPredicateEvaluator
