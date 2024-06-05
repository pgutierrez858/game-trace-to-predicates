package predcompiler.compilation

import org.moeaframework.util.grammar.*
import predcompiler.compilation.evaluation.IPredicateEvaluator
import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping
import predcompiler.compilation.io.GameStateTraceFileReader
import predcompiler.compilation.io.GameTraceFileReader
import predcompiler.compilation.io.StateTraceToRobustnessMapper
import java.io.File
import java.io.IOException

/**
 * Base class for any search for a predicate that models a set of sample and counter-example traces while maximizing
 * fitness metrics for a given evaluator.
 */
abstract class AbstractPredicateSearch @Throws(IOException::class) constructor // AbstractPredicateSearch
    (
    /**
     * System path for the .bnf file representing the grammar to use as a search space.
     */
    private val grammarPath: String,
    /**
     * System path for the folder containing the traces to be modeled.
     */
    private val tracesPath: String,
    /**
     * The evaluation function to use to compute fitness values for predicates.
     */
    protected val evaluator: IPredicateEvaluator,
    /**
     * Array of mapping functions that will be used to transform the contents of the file to robustness entries.
     */
    private val mappings: List<AbstractStateToRobustnessMapping>
) {

    data class TracesReadResult(
        val atomicPredicates: HashSet<String>,
        val exampleTraces: List<Array<RealValuation>>,
        val counterExampleTraces: List<Array<RealValuation>>
    )

    // region Computed data from files
    /**
     * List of atomic predicates that can be referenced in suggested solutions.
     */
    private val atomicPredicates: HashSet<String>

    /**
     * Real-valued robustness traces that the sought predicate SHOULD model.
     */
    protected val exampleTraces: List<Array<RealValuation>>

    /**
     * Real-valued robustness traces that the sought predicate should NOT model.
     */
    protected val counterExampleTraces: List<Array<RealValuation>>

    /**
     * Complete grammar to be used for the search process, including literals from trace files.
     */
    protected val grammar: ContextFreeGrammar

    // endregion

    // region run information for current search
    /**
     * Set including the predicates with the highest fitness according to our chosen evaluator.
     */
    protected var bestSolutions: HashSet<String>

    /**
     * Fitness value of all the individuals included in bestSolutions.
     */
    protected var bestFitness: Float = 0f

    /**
     * Whether search has terminated.
     */
    protected var terminated: Boolean = false

    /**
     * How many search steps have been performed so far.
     */
    private var steps: Int = 0

    // endregion

    @Throws(IOException::class)
    protected fun readRobustnessTraces(): TracesReadResult {
        // file reader to process the game traces to model from csv files.
        val traceReader = GameTraceFileReader(tracesPath)
        // attempt to process the file traces. throws if there is an error while reading
        // the files
        traceReader.readTraces()

        // store the results of the trace reading and parsing.
        return TracesReadResult(
            traceReader.atomicPredicates,
            traceReader.exampleTraces,
            traceReader.counterExampleTraces
        )
    } // readRobustnessTraces

    @Throws(IOException::class)
    protected fun readMappedTraces(
    ): TracesReadResult {
        // file reader to process the game STATE traces to model from csv files.
        val stateTraceReader = GameStateTraceFileReader(tracesPath)
        // attempt to process the file traces. This will cache traces as lists of hash
        // maps.
        stateTraceReader.readTraces()

        // mapper object to bulk-transform states into RealValuations based on provided
        // mappings
        val mapper = StateTraceToRobustnessMapper(mappings.toTypedArray())
        val exampleTraces = mutableListOf<Array<RealValuation>>()
        for (exampleTrace in stateTraceReader.exampleTraces) {
            // create a new valuation by applying all mapping functions to the game state.
            exampleTraces.add(mapper.mapStateTraceToRobustness(exampleTrace))
        }

        val counterExampleTraces = mutableListOf<Array<RealValuation>>()
        for (counterExampleTrace in stateTraceReader.counterExampleTraces) {
            // create a new valuation by applying all mapping functions to the game state.
            counterExampleTraces.add(mapper.mapStateTraceToRobustness(counterExampleTrace))
        }

        // now, in this case, atomic predicates will be the string representations of
        // the mappings that we decide to use
        val atomicPredicates = HashSet<String>()
        for (mapping in mappings) {
            atomicPredicates.add(mapping.predicateRepresentation)
        }

        return TracesReadResult(atomicPredicates, exampleTraces, counterExampleTraces)
    } // readMappedTraces

    /**
     * Attempt to produce a grammar object from the specified system path. Call this only after loading the reference
     * traces to ensure that the grammar includes the corresponding <literal> rule containing the symbols used as
     * headers in the trace files.
     */
    @Throws(IOException::class)
    protected fun readGrammar(): ContextFreeGrammar {
        // get a reader for the file containing the grammar to search on
        val grammarReader = File(grammarPath).reader()

        // attempt to parse an actual grammar for the file.
        // note that we are expecting the grammar to include ONLY the
        // basic building blocks and not the atomic predicates, which
        // will be included later on programmatically based on the contents
        // of the trace files and their headers.
        val grammar = Parser.load(grammarReader)

        // close the reader to free resources
        grammarReader.close()

        return grammar
    } // readGrammar

    /**
     * Adds all the symbols from atomic predicates as terminal productions
     * to the grammar under the production with tag <literal>.
     */
    private fun populateLiteralProductions() {
        // add a rule to the grammar for the atomic predicates
        // as grammar literals. This is done by looking at the atomic predicates
        // from the headers of the csv files used as reference.
        val terminalRule = Rule(Symbol("literal", false))
        for (l in atomicPredicates) {
            val terminalProduction = Production()
            terminalProduction.add(Symbol(l, true))
            terminalRule.add(terminalProduction)
        }
        grammar.add(terminalRule)
    } // populateLiteralProductions

    /**
     * Initialize the Predicate Search with the information from the system paths where samples and grammars are
     * located. If no mappings have been provided, it is assumed that the target files are purely robustness based and
     * the default GameTraceReader is used to parse them. If at least one mapping is provided, then the files will be
     * processed by a GameStateTraceFileReader, and the result will ONLY include the output of the mapping functions
     * applied to the game states.
     */
    init {

        grammar = this.readGrammar()

        val readTracesData: TracesReadResult = if (mappings.isEmpty()) {
            this.readRobustnessTraces()
        } else {
            this.readMappedTraces()
        }

        atomicPredicates = readTracesData.atomicPredicates
        exampleTraces = readTracesData.exampleTraces
        counterExampleTraces = readTracesData.counterExampleTraces

        populateLiteralProductions()

        // init run information fields for current search
        this.bestFitness = Float.NEGATIVE_INFINITY
        this.bestSolutions = HashSet()
        this.terminated = false
        this.steps = 0
    } // init

    fun step() {
        stepSearch()
        steps++
    } // step

    val isStillRunning: Boolean
        /**
         * Whether a final solution has been found.
         */
        get() = !this.terminated // isTerminated

    fun printStepResults() {
        println("-------------------------------")
        println("Step $steps")
        println("-------------------------------")

        println("Best Fitness Value: $bestFitness")

        for (pred in bestSolutions) {
            println("    $pred")
        }
        println("-------------------------------")
        println()
    } // printStepResults

    protected abstract fun stepSearch()
} // AbstractPredicateSearch

