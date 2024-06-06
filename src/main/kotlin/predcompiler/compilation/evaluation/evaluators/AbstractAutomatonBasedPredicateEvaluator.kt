package predcompiler.compilation.evaluation.evaluators

import rabinizer.automata.DTRA
import rabinizer.exec.Main
import rabinizer.exec.Main.AutomatonType
import java.io.OutputStream
import java.io.PrintStream

/**
 * Convenience base class for evaluators which need to make use of a Rabin
 * Automaton for computing fitness. Includes a computeAutomaton method to create
 * this conveniently in silent mode and get access to it from the corresponding
 * evaluation function.
 */
abstract class AbstractAutomatonBasedPredicateEvaluator : IPredicateEvaluator {
    /**
     * Computes a deterministic Rabin automaton for the specified predicate, in
     * silent mode.
     */
    protected fun computeAutomaton(predicate: String?): DTRA {
        val originalStream = System.out

        val dummyStream = PrintStream(object : OutputStream() {
            override fun write(b: Int) {
                // NO-OP
            }
        })

        System.setOut(dummyStream)
        val automaton = Main.computeAutomaton(predicate, AutomatonType.TR, true, false, true) as DTRA
        System.setOut(originalStream)

        return automaton
    } // computeAutomaton
} // AbstractAutomatonBasedPredicateEvaluator

