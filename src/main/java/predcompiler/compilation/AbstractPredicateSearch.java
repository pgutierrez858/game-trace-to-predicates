package predcompiler.compilation;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.moeaframework.util.grammar.ContextFreeGrammar;
import org.moeaframework.util.grammar.Parser;
import org.moeaframework.util.grammar.Production;
import org.moeaframework.util.grammar.Rule;
import org.moeaframework.util.grammar.Symbol;
import org.moeaframework.util.io.Resources;
import org.moeaframework.util.io.Resources.ResourceOption;

import predcompiler.compilation.evaluation.IPredicateEvaluator;
import predcompiler.compilation.evaluation.RealValuation;
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping;
import predcompiler.compilation.io.GameStateTraceFileReader;
import predcompiler.compilation.io.GameTraceFileReader;
import predcompiler.compilation.io.StateTraceToRobustnessMapper;

/**
 * Base class for any search for a predicate that models a set of sample and
 * counter-example traces while maximizing fitness metrics for a given
 * evaluator.
 */
public abstract class AbstractPredicateSearch {

	/**
	 * List of atomic predicates that can be referenced in suggested solutions.
	 */
	protected HashSet<String> atomicPredicates;
	/**
	 * Real-valued robustness traces that the sought predicate SHOULD model.
	 */
	protected List<RealValuation[]> exampleTraces;
	/**
	 * Real-valued robustness traces that the sought predicate should NOT model.
	 */
	protected List<RealValuation[]> counterExampleTraces;

	/**
	 * List of mapping functions that will be used to transform the contents of the
	 * file to robustness entries.
	 */
	protected List<AbstractStateToRobustnessMapping> mappings;

	/**
	 * System path for the folder containing the traces to be modeled.
	 */
	protected String tracesPath;

	/**
	 * System path for the .bnf file representing the grammar to use as a search
	 * space.
	 */
	protected String grammarPath;

	/**
	 * Complete grammar to be used for the search process, including literals from
	 * trace files.
	 */
	protected ContextFreeGrammar grammar;

	/**
	 * Whether this search has been properly initialized.
	 */
	protected boolean initialized;

	/**
	 * Set including the predicates with the highest fitness according to our chosen
	 * evaluator.
	 */
	protected HashSet<String> bestSolutions;

	/**
	 * Fitness value of all the individuals included in bestSolutions.
	 */
	protected float bestFitness;

	/**
	 * The evaluation function to use to compute fitness values for predicates.
	 */
	protected IPredicateEvaluator evaluator;

	/**
	 * Whether search has terminated.
	 */
	protected boolean terminated;

	/**
	 * How many search steps have been performed so far.
	 */
	protected int steps;

	protected AbstractPredicateSearch(String grammarPath, String tracesPath, IPredicateEvaluator evaluator)
			throws IOException {
		this.mappings = new ArrayList<>();
		this.tracesPath = tracesPath;
		this.grammarPath = grammarPath;
		this.initialized = false;
		this.evaluator = evaluator;
	} // AbstractPredicateSearch

	public void addMapping(AbstractStateToRobustnessMapping mapping) {
		this.mappings.add(mapping);
	} // addMapping

	/**
	 * Initialize the Predicate Search with the information from the system paths
	 * where samples and grammars are located. If no mappings have been provided, it
	 * is assumed that the target files are purely robustness based and the default
	 * GameTraceReader is used to parse them. If at least one mapping is provided,
	 * then the files will be processed by a GameStateTraceFileReader, and the
	 * result will ONLY include the output of the mapping functions applied to the
	 * game states.
	 * 
	 * @throws IOException if there is an error while accessing any of the files.
	 */
	protected void initialize() throws IOException {
		if (mappings.size() == 0) {
			this.readRobustnessTraces();
		} else {
			this.readMappedTraces();
		}

		this.readGrammar();
		this.bestFitness = Float.NEGATIVE_INFINITY;
		this.bestSolutions = new HashSet<>();
		this.initialized = true;
		this.terminated = false;
		this.steps = 0;
	} // initialize

	protected void readRobustnessTraces() throws IOException {
		// file reader to process the game traces to model from csv files.
		GameTraceFileReader traceReader = new GameTraceFileReader(tracesPath);
		// attempt to process the file traces. throws if there is an error while reading
		// the files
		traceReader.readTraces();

		// store the results of the trace reading and parsing.
		atomicPredicates = traceReader.getAtomicPredicates();
		exampleTraces = traceReader.getExampleTraces();
		counterExampleTraces = traceReader.getCounterExampleTraces();
	} // readRobustnessTraces

	protected void readMappedTraces() throws IOException {
		// file reader to process the game STATE traces to model from csv files.
		GameStateTraceFileReader stateTraceReader = new GameStateTraceFileReader(tracesPath);
		// attempt to process the file traces. This will cache traces as lists of hash
		// maps.
		stateTraceReader.readTraces();

		// mapper object to bulk-transform states into RealValuations based on provided
		// mappings
		var mapper = new StateTraceToRobustnessMapper(mappings);
		exampleTraces = new ArrayList<>();
		for (var exampleTrace : stateTraceReader.getExampleTraces()) {
			// create a new valuation by applying all mapping functions to the game state.
			exampleTraces.add(mapper.mapStateTraceToRobustness(exampleTrace));
		}

		counterExampleTraces = new ArrayList<>();
		for (var counterExampleTrace : stateTraceReader.getCounterExampleTraces()) {
			// create a new valuation by applying all mapping functions to the game state.
			counterExampleTraces.add(mapper.mapStateTraceToRobustness(counterExampleTrace));
		}

		// now, in this case, atomic predicates will be the string representations of
		// the mappings that we decide to use
		atomicPredicates = new HashSet<>();
		for (var mapping : mappings) {
			atomicPredicates.add(mapping.getPredicateRepresentation());
		}
	} // readMappedTraces

	/**
	 * Attempt to produce a grammar object from the specified system path. Call this
	 * only after loading the reference traces to ensure that the grammar includes
	 * the corresponding <literal> rule containing the symbols used as headers in
	 * the trace files.
	 */
	protected void readGrammar() throws IOException {
		// get a reader for the file containing the grammar to search on
		Reader grammarReader = Resources.asReader(this.getClass(), grammarPath, ResourceOption.REQUIRED);

		// attempt to parse an actual grammar for the file.
		// note that we are expecting the grammar to include ONLY the
		// basic building blocks and not the atomic predicates, which
		// will be included later on programmatically based on the contents
		// of the trace files and their headers.
		grammar = Parser.load(grammarReader);

		// close the reader to free resources
		grammarReader.close();

		// add an additional rule to the grammar for the atomic predicates
		// as grammar literals. This is done by looking at the atomic predicates
		// from the headers of the csv files used as reference.
		Rule terminalRule = new Rule(new Symbol("literal", false));
		for (String l : atomicPredicates) {
			var terminalProduction = new Production();
			terminalProduction.add(new Symbol(l, true));
			terminalRule.add(terminalProduction);
		}
		grammar.add(terminalRule);
	} // readGrammar

	public void step() {
		if (!initialized) {
			System.out.println("Attempting to run search before calling its initialize() method.");
			return;
		}
		stepSearch();
		steps++;
	} // step

	public HashSet<String> getBestSolutions() {
		return this.bestSolutions;
	} // getBestSolutions

	public float getBestFitness() {
		return this.bestFitness;
	} // getBestFitness

	/**
	 * Whether a final solution has been found.
	 */
	public boolean isTerminated() {
		return this.terminated;
	} // isTerminated

	public void printStepResults() {
		System.out.println("-------------------------------");
		System.out.println("Step " + steps);
		System.out.println("-------------------------------");

		System.out.println("Best Fitness Value: " + bestFitness);

		for (var pred : bestSolutions) {
			System.out.println("    " + pred);
		}
		System.out.println("-------------------------------");
		System.out.println();
	} // printStepResults

	protected abstract void stepSearch();
} // AbstractPredicateSearch
