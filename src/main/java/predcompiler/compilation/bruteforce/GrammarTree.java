package predcompiler.compilation.bruteforce;

import java.util.ArrayList;
import java.util.List;

import org.moeaframework.util.grammar.Symbol;

/**
 * Representation of a production tree for a given grammar. A tree is formed by
 * a root symbol, the depth of the representing node in the root tree in which
 * it is embedded, a reference to the parent node that produced it, and a list
 * of productions that it can be replaced by during grammar expansion.
 */
public class GrammarTree {
	/**
	 * Either a terminal symbol (e.g. '&', 'a', '|') or a rule tag (e.g. 'expr',
	 * 'literal').
	 */
	public Symbol rootSymbol;
	/**
	 * How many production rules have been taken from the root of the original
	 * production tree before reaching this node.
	 */
	public int depth;
	/**
	 * Possible productions in which this node can be derived. Each production is
	 * given by a list of grammar trees to represent situations like <expr> ::=
	 * <literal> <binary> <literal>, where a symbol is transformed into a sequence
	 * of arbitrary compounds.
	 */
	public List<List<GrammarTree>> productions;
	/**
	 * Parent node in the tree hierarchy, or null if this is an origin node.
	 */
	public GrammarTree parent;

	public GrammarTree(Symbol symbol, GrammarTree parent, int depth) {
		productions = new ArrayList<>();
		rootSymbol = symbol;
		this.depth = depth;
		this.parent = parent;
	}

	/**
	 * Returns a list of all terminal productions that can be formed from this node.
	 */
	public List<String> getTerminals() {
		List<String> result = new ArrayList<>();

		if (rootSymbol.isTerminal()) {
			// base case, just add the root symbol to the options
			result.add(rootSymbol.getValue());
		} else {
			// example for prod: { <expression> <binary> <expression> }
			for (List<GrammarTree> prod : productions) {
				// each production in the list can resolve to its own
				// list of productions, which can then be used to complete
				// the parent productions in a combinatorial way.
				List<String> prodOptions = prod.get(0).getTerminals();

				for (int i = 1; i < prod.size(); i++) {
					GrammarTree subtree = prod.get(i);
					List<String> newProdOptions = new ArrayList<>();
					// compute the terminal combinations of this specific subtree
					for (String option : prodOptions) {
						List<String> terminals = subtree.getTerminals();
						for (String substring : terminals) {
							newProdOptions.add(option + substring);
						}
					}
					prodOptions = newProdOptions;
				}
				result.addAll(prodOptions);
			}
		}
		return result;
	} // getTerminals
	
} // GrammarTree
