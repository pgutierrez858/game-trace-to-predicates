package predcompiler.compilation

import org.moeaframework.util.grammar.*
import predcompiler.compilation.evaluation.evaluators.predicate.IPredicateEvaluator
import predcompiler.compilation.evaluation.RealValuation
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Base class for any search for a predicate that models a set of sample and counter-example traces while maximizing
 * fitness metrics for a given evaluator.
 */
abstract class AbstractPredicateSearch
    (
    /**
     * Complete grammar to be used for the search process, including literals from trace files.
     */
    protected val grammar: ContextFreeGrammar,
    /**
     * The evaluation function to use to compute fitness values for predicates.
     */
    protected val evaluator: IPredicateEvaluator,
    /**
     * Real-valued robustness traces that the sought predicate SHOULD model.
     */
    protected val exampleTraces: List<List<RealValuation>>,

    /**
     * Real-valued robustness traces that the sought predicate should NOT model.
     */
    protected val counterExampleTraces: List<List<RealValuation>> = emptyList(),
) {
    // region run information for current search
    /**
     * Set including the predicates with the highest fitness according to our chosen evaluator.
     */
    protected var bestSolutions: HashSet<String>

    /**
     * Fitness value of all the individuals included in bestSolutions.
     */
    var bestFitness: Double = 0.0
        protected set

    /**
     * Whether search has terminated.
     */
    protected var terminated: Boolean = false

    /**
     * How many search steps have been performed so far.
     */
    private var steps: Int = 0

    /**
     * How many ticks have been performed within the current search step.
     */
    private var stepTicks: Int = 0

    // endregion

    init {
        // init run information fields for current search
        this.bestFitness = Double.NEGATIVE_INFINITY
        this.bestSolutions = HashSet()
        this.terminated = false
        this.steps = 0
        this.stepTicks = 0
    } // init

    fun tick() {
        if (terminated) return
        tickSearch()
        steps++
    } // step

    fun getBestSolutions(): Set<String> {
        return bestSolutions
    } // getBestSolutions

    val isStillRunning: Boolean
        /**
         * Whether a final solution has been found.
         */
        get() = !this.terminated // isTerminated

    fun debugTickResults() {
        logger.info {
            """
            Search Progress:
            -> Step: $steps
            -> Tick: $stepTicks
            -> Best Fitness: $bestFitness
            -> Best Individuals:
            ${bestSolutions.joinToString("\n", "\t-> ")}
            """.trimIndent()
        }
    } // debugTickResults

    /**
     * Perform a single tick within the current search process.
     */
    protected abstract fun tickSearch()
} // AbstractPredicateSearch

