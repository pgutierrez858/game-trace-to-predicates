package predcompiler.compilation.evaluation;

import java.io.OutputStream;
import java.io.PrintStream;
import rabinizer.automata.DTRA;
import rabinizer.exec.Main;
import rabinizer.exec.Main.AutomatonType;

/**
 * Convenience base class for evaluators which need to make use of a Rabin
 * Automaton for computing fitness. Includes a computeAutomaton method to create
 * this conveniently in silent mode and get access to it from the corresponding
 * evaluation function.
 */
public abstract class AbstractAutomatonBasedPredicateEvaluator implements IPredicateEvaluator {

	/**
	 * Computes a deterministic Rabin automaton for the specified predicate, in
	 * silent mode.
	 */
	protected DTRA computeAutomaton(String predicate) {
		PrintStream originalStream = System.out;

		PrintStream dummyStream = new PrintStream(new OutputStream() {
			public void write(int b) {
				// NO-OP
			}
		});

		System.setOut(dummyStream);
		DTRA automaton = (DTRA) Main.computeAutomaton(predicate, AutomatonType.TR, true, false, true);
		System.setOut(originalStream);

		return automaton;
	} // computeAutomaton

} // AbstractAutomatonBasedPredicateEvaluator
