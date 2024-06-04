package predcompiler.compilation.bruteforce

import org.moeaframework.util.grammar.Symbol

/**
 * Representation of a production tree for a given grammar. A tree is formed by
 * a root symbol, the depth of the representing node in the root tree in which
 * it is embedded, a reference to the parent node that produced it, and a list
 * of productions that it can be replaced by during grammar expansion.
 */
class GrammarTree(
    /**
     * Either a terminal symbol (e.g. '&', 'a', '|') or a rule tag (e.g. 'expr',
     * 'literal').
     */
    var rootSymbol: Symbol,
    /**
     * How many production rules have been taken from the root of the original
     * production tree before reaching this node.
     */
    var depth: Int
) {
    /**
     * Possible productions in which this node can be derived. Each production is
     * given by a list of grammar trees to represent situations like <expr> ::=
     * <literal> <binary> <literal>, where a symbol is transformed into a sequence
     * of arbitrary compounds.
    </literal></binary></literal></expr> */
    var productions: MutableList<List<GrammarTree>> = ArrayList()

    val terminals: List<String>
        /**
         * Returns a list of all terminal productions that can be formed from this node.
         */
        get() {
            val result: MutableList<String> = ArrayList()

            if (rootSymbol.isTerminal) {
                // base case, just add the root symbol to the options
                result.add(rootSymbol.value)
            } else {
                // example for prod: { <expression> <binary> <expression> }
                for (prod in productions) {
                    // each production in the list can resolve to its own
                    // list of productions, which can then be used to complete
                    // the parent productions in a combinatorial way.
                    var prodOptions: List<String> = prod[0].terminals

                    for (i in 1 until prod.size) {
                        val subtree = prod[i]
                        val newProdOptions: MutableList<String> = ArrayList()
                        // compute the terminal combinations of this specific subtree
                        for (option in prodOptions) {
                            val terminals: List<String> = subtree.terminals
                            for (substring in terminals) {
                                newProdOptions.add(option + substring)
                            }
                        }
                        prodOptions = newProdOptions
                    }
                    result.addAll(prodOptions)
                }
            }
            return result
        } // getTerminals
} // GrammarTree

