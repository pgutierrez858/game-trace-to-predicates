package predcompiler.compilation.grammarevolution

import org.moeaframework.algorithm.single.GeneticAlgorithm
import org.moeaframework.core.PRNG
import org.moeaframework.util.grammar.ContextFreeGrammar
import predcompiler.compilation.AbstractPredicateSearch
import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.predicate.IPredicateEvaluator

class GrammarEvolutionGrammarSearch(
    grammar: ContextFreeGrammar,
    evaluator: IPredicateEvaluator,
    exampleTraces: List<List<RealValuation>>,
    counterExampleTraces: List<List<RealValuation>>,
    initialPopulationSize: Int,
    maxGenerations: Int
) : AbstractPredicateSearch(
    grammar, evaluator, exampleTraces, counterExampleTraces
) {
    // region private search fields
    /**
     * Maximum number of population iterations that the algorithm will go through.
     */
    private val maxGenerations: Int

    /**
     * Index of the current evolution generation
     */
    private var generation: Int

    /**
     * Reference to our problem solver algorithm.
     */
    private val algorithm: GeneticAlgorithm

    /**
     * Reference to the problem that we are trying to solve.
     */
    private val problem: GrammarRegression
    // endregion

    init {
        PRNG.setRandom(SynchronizedMersenneTwister.instance)

        this.maxGenerations = maxGenerations

        // setup and construct the GP solver
        problem = GrammarRegression(exampleTraces, counterExampleTraces, grammar, evaluator)
        algorithm = GeneticAlgorithm(problem)
        algorithm.initialPopulationSize = initialPopulationSize
        generation = 0
    } // GrammarEvolutionGrammarSearch

    override fun tickSearch() {
        if (generation >= maxGenerations) return

        algorithm.step()

        for (solution in algorithm.result) {
            problem.getPredicate(solution)?.let { bestSolutions.add(it) }
        }
        // record the fitness of the members from the solution set
        // (note that there will always be at least one)
        bestFitness = algorithm.result[0].getObjective(0)

        generation++

        if (generation >= maxGenerations) {
            algorithm.terminate()
        }
    } // stepSearch

} // GrammarEvolutionGrammarSearch

