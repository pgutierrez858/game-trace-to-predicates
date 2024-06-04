package predcompiler.compilation.mcts;

import org.moeaframework.util.grammar.Symbol;

import java.util.List;

public class GrammarProductionTreeNode {

    /**
     * Symbol used for his tree's node. This defines the actions that a search can take based on the possible productions
     * that can be taken from it, as well as whether this is a terminal node.
     */
    public Symbol symbol;

    /**
     * List of tree nodes added to this node as children after choosing a production for the rule.
     */
    public List<GrammarProductionTreeNode> children;

    public GrammarProductionTreeNode(Symbol symbol, List<GrammarProductionTreeNode> children) {
        this.symbol = symbol;
        this.children = children;
    }

} // GrammarProductionTreeNode
