package predcompiler.compilation.mcts

import java.util.*
import java.util.stream.Collectors
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.asKotlinRandom

class BasicTreeNode(
    // Parameters guiding the search
    private val search: BasicMCTSSearch,
    // Parent of this node
    private val parent: BasicTreeNode?,
    val state: GrammarProductionState,
    private val rnd: Random
) {
    // Depth of this node
    private val depth: Int

    // Root node of tree
    private val root: BasicTreeNode

    // Children of this node
    private val children: MutableMap<GrammarProductionAction, BasicTreeNode?> = HashMap()

    // Total value of this node
    private var totValue = 0.0

    // Number of visits
    private var nVisits = 0

    // Number of FM calls and State copies up until this node
    private var fmCallsCount = 0

    init {
        this.root = parent?.root ?: this
        updateState(state)
        depth = if (parent != null) {
            parent.depth + 1
        } else {
            0
        }
    }

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    fun mctsSearch() {
        val params = search.parameters

        // Variables for tracking time budget
        var avgTimeTaken: Double
        var acumTimeTaken = 0.0
        var remaining: Long
        val remainingLimit = params.breakMS
        val elapsedTimer = ElapsedCpuTimer()
        if (params.budgetType == MCTSSearchConstants.BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget)
        }

        // Tracking number of iterations for iteration budget
        var numIters = 0

        var stop = false

        while (!stop) {
            // New timer for this iteration
            val elapsedTimerIteration = ElapsedCpuTimer()

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            val selected = treePolicy()
            // Monte carlo rollout: return value of MC rollout from the newly added node
            val delta = selected.rollOut()
            // Back up the value of the rollout through the tree
            selected.backUp(delta)
            // Finished iteration
            numIters++

            // Check stopping condition
            val budgetType = params.budgetType
            when (budgetType) {
                MCTSSearchConstants.BUDGET_TIME -> {
                    // Time budget
                    acumTimeTaken += elapsedTimerIteration.elapsedMillis().toDouble()
                    avgTimeTaken = acumTimeTaken / numIters
                    remaining = elapsedTimer.remainingTimeMillis()
                    stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit!!
                }
                MCTSSearchConstants.BUDGET_ITERATIONS -> {
                    // Iteration budget
                    stop = numIters >= params.budget
                }
                MCTSSearchConstants.BUDGET_FM_CALLS -> {
                    // FM calls budget
                    stop = fmCallsCount > params.budget
                }
                else -> {
                    // TODO
                }
            }
        }
    } // mctsSearch

    /**
     * Selection + expansion steps. - Tree is traversed until a node not fully expanded is found. - A new child of this
     * node is added to the tree.
     *
     * @return - new node added to the tree.
     */
    private fun treePolicy(): BasicTreeNode {
        var cur: BasicTreeNode = this

        // Keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (cur.state.isNotTerminal() && cur.depth < search.parameters.maxTreeDepth) {
            if (cur.unexpandedActions().isNotEmpty()) {
                // We have an unexpanded action
                cur = cur.expand()
                return cur
            } else {
                // Move to next child given by UCT function
                val actionChosen = cur.ucb()
                cur = cur.children[actionChosen]!!
            }
        }
        return cur
    } // treePolicy

    private fun updateState(newState: GrammarProductionState) {
        if (newState.isNotTerminal()) for (action in newState.possibleActions()) {
            children[action] = null // mark a new node to be expanded
        }
    } // setState

    /**
     * @return A list of the unexpanded Actions from this State
     */
    private fun unexpandedActions(): List<GrammarProductionAction> {
        return children.keys.stream().filter { a: GrammarProductionAction -> children[a] == null }
            .collect(Collectors.toList())
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     *
     * @return - new child node.
     */
    private fun expand(): BasicTreeNode {
        // Find random child not already created
        val r = Random(search.parameters.seed!!)
        // pick a random un-chosen action
        val notChosen = unexpandedActions()
        val chosen = notChosen[r.nextInt(notChosen.size)]

        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any state changes
        val nextState = state.copy()
        advance(nextState, chosen)

        // then instantiate a new node
        val tn = BasicTreeNode(search, this, nextState, rnd)
        children[chosen] = tn
        return tn
    } // expand

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    private fun advance(gs: GrammarProductionState, act: GrammarProductionAction): GrammarProductionState {
        root.fmCallsCount++
        return gs.applyProduction(act)
    } // advance

    private fun ucb(): GrammarProductionAction {
        // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
        var bestAction: GrammarProductionAction? = null
        var bestValue = -Double.MAX_VALUE
        val params = search.parameters

        for (action in children.keys) {
            val child = children[action]
            if (child == null) throw AssertionError("Should not be here")
            else if (bestAction == null) bestAction = action

            // Find child value
            val hvVal = child.totValue
            val childValue = hvVal / (child.nVisits + params.epsilon)

            // default to standard UCB
            val explorationTerm =
                params.K * sqrt(ln((this.nVisits + 1).toDouble()) / (child.nVisits + params.epsilon))

            // unless we are using a variant

            // Find 'UCB' value
            var uctValue = childValue
            uctValue += explorationTerm

            // Apply small noise to break ties randomly
            uctValue = noise(uctValue, params.epsilon, search.rnd.nextDouble())

            // Assign value
            if (uctValue > bestValue) {
                bestAction = action
                bestValue = uctValue
            }
        }

        if (bestAction == null) throw AssertionError("We have a null value in UCT : shouldn't really happen!")

        root.fmCallsCount++ // log one iteration complete
        return bestAction
    } // ucb

    /**
     * Perform a Monte Carlo rollout from this node.
     *
     * @return - value of rollout.
     */
    private fun rollOut(): Double {
        var rolloutDepth = 0 // counting from end of tree

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        var rolloutState = state.copy()
        if (search.parameters.rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                val next: GrammarProductionAction = rolloutState.possibleActions().random(rnd.asKotlinRandom())
                rolloutState = advance(rolloutState, next)
                rolloutDepth++
            }
        }
        // Evaluate final state and return normalised score
        val value: Double = search.parameters.heuristic.invoke(rolloutState)
        if (java.lang.Double.isNaN(value)) throw AssertionError("Illegal heuristic value - should be a number")
        return value
    }

    /**
     * Checks if rollout is finished. Rollouts end on maximum length, or if game ended.
     *
     * @param rollerState - current state
     * @param depth       - current depth
     * @return - true if rollout finished, false otherwise
     */
    private fun finishRollout(rollerState: GrammarProductionState, depth: Int): Boolean {
        if (depth >= search.parameters.rolloutLength) return true

        // End of game
        return !rollerState.isNotTerminal()
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to back up
     */
    private fun backUp(result: Double) {
        var n: BasicTreeNode? = this
        while (n != null) {
            n.nVisits++
            n.totValue += result
            n = n.parent
        }
    } // backUp

    /**
     * Calculates the best action from the root according to the most visited node
     *
     * @return - the best GrammarProductionAction
     */
    fun bestAction(): GrammarProductionAction {
        var bestValue = -Double.MAX_VALUE
        var bestAction: GrammarProductionAction? = null

        for (action in children.keys) {
            if (children[action] != null) {
                val node = children[action]
                var childValue = node!!.nVisits.toDouble()

                // Apply small noise to break ties randomly
                childValue = noise(childValue, search.parameters.epsilon, search.rnd.nextDouble())

                // Save best value (highest visit count)
                if (childValue > bestValue) {
                    bestValue = childValue
                    bestAction = action
                }
            }
        }

        if (bestAction == null) {
            throw AssertionError("Unexpected - no selection made.")
        }

        return bestAction
    } // bestAction

    companion object {
        /**
         * Applies random noise to input.
         *
         * @param input   - value to apply noise to.
         * @param epsilon - how much should the noise weigh in returned value.
         * @param random  - how much noise should be applied.
         * @return - new value with noise applied.
         */
        fun noise(input: Double, epsilon: Double, random: Double): Double {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5))
        }
    }
} // BasicTreeNode
