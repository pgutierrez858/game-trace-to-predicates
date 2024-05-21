package predcompiler.compilation;

import java.io.IOException;
import java.io.Reader;
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
import predcompiler.compilation.io.GameTraceFileReader;

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
	 * Complete grammar to be used for the search process, including literals from
	 * trace files.
	 */
	protected ContextFreeGrammar grammar;

	protected AbstractPredicateSearch(String grammarPath, String tracesPath) throws IOException {
		this.readTraces(tracesPath);
		this.readGrammar(grammarPath);
	} // AbstractPredicateSearch

	protected void readTraces(String tracesPath) throws IOException {
		// file reader to process the game traces to model from csv files.
		GameTraceFileReader traceReader = new GameTraceFileReader(tracesPath);
		// attempt to process the file traces. throws if there is an error while reading
		// the files
		traceReader.readTraces();

		// store the results of the trace reading and parsing.
		atomicPredicates = traceReader.getAtomicPredicates();
		exampleTraces = traceReader.getExampleTraces();
		counterExampleTraces = traceReader.getCounterExampleTraces();
	} // readTraces

	/**
	 * Attempt to produce a grammar object from the specified system path. Call this
	 * only after loading the reference traces to ensure that the grammar includes
	 * the corresponding <literal> rule containing the symbols used as headers in
	 * the trace files.
	 */
	protected void readGrammar(String grammarPath) throws IOException {
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

	/**
	 * Use the implementing class' search algorithm to look for the best predicates
	 * relative to the provided predicate fitness evaluator.
	 */
	public abstract HashSet<String> findBestPredicates(IPredicateEvaluator evaluator);

} // AbstractPredicateSearch
