package predcompiler.compilation.io

import org.moeaframework.util.grammar.*
import java.io.File

/**
 * Populates the grammar with the productions for the provided set of atomic predicates.
 * @param grammar the grammar to populate.
 * @param atomicPredicates the set of atomic predicates to add to the grammar.
 * @param blockLiterals whether to use block literals or not. If true, the literals will be
 * represented as a block of all available atomic predicates either holding, not holding or
 * not mattering.
 */
fun populateBlockLiteralProductions(
    grammar: ContextFreeGrammar,
    atomicPredicates: Set<String>,
    blockLiterals: Boolean = false
) {
    // add a rule to the grammar for the atomic predicates
    // as grammar literals. This is done by looking at the atomic predicates
    // from the headers of the csv files used as reference.
    val terminalRule = Rule(Symbol("literal", false))

    // <literal> ::= <a> | <b> | ... | <z>
    if(!blockLiterals) {
        for(ap in atomicPredicates){
            val terminalProduction = Production()
            terminalProduction.add(Symbol(ap, true))
            terminalRule.add(terminalProduction)
        }
    }
    else {
        val terminalProduction = Production()
        // <literal> ::= <maybe-a> & <maybe-b> & ... & <maybe-z>
        for ((i, l) in atomicPredicates.withIndex()) {
            val ruleSymbol = "maybe-${l}"
            val atomicPredicateRule = Rule(Symbol(ruleSymbol, false))
            // <maybe-t> ::= t | !t | T (T means we don't care as it will be appended to an & clause)
            // t
            val lHoldsProduction = Production()
            lHoldsProduction.add(Symbol(l, true))
            atomicPredicateRule.add(lHoldsProduction)
            // !t
            val lDoesNotHoldProduction = Production()
            lDoesNotHoldProduction.add(Symbol("!", true))
            lDoesNotHoldProduction.add(Symbol(l, true))
            atomicPredicateRule.add(lDoesNotHoldProduction)
            // T
            val lDoesNotMatterProduction = Production()
            lDoesNotMatterProduction.add(Symbol("1", true))
            atomicPredicateRule.add(lDoesNotMatterProduction)

            if (i > 0) terminalProduction.add(Symbol("&", true))
            terminalProduction.add(Symbol(ruleSymbol, false))

            grammar.add(atomicPredicateRule)
        }
        terminalRule.add(terminalProduction)
    }

    grammar.add(terminalRule)
} // populateBlockLiteralProductions

/**
 * Attempt to produce a grammar object from the specified system path.
 * Throws an exception if provided path is invalid or if grammar could not be parsed properly.
 */
fun readGrammar(grammarPath: String): ContextFreeGrammar {
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
