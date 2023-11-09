package players.assignment1;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.AbstractPlayer;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;
import utilities.Pair;

import java.util.*;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static utilities.Utils.noise;

class GroupKTreeNode {
    // Root node of tree
    GroupKTreeNode root;
    // Parent of this node
    GroupKTreeNode parent;
    // Children of this node
    Map<AbstractAction, GroupKTreeNode> children = new HashMap<>();
    // Statistics for MAST
    Map<AbstractAction, Pair<Integer, Double>> MASTStat;
    // Depth of this node
    final int depth;

    protected List<AbstractAction> actionsInRollout;

    // Total value of this node
    private double totValue;
    private double amafValue;
    private double alpha;
    // Number of visits
    private int nVisits;
    private int amafVisit;
    // Number of FM calls and State copies up until this node
    private int fmCallsCount;
    // Parameters guiding the search
    private GroupKPlayer player;
    private Random rnd;
    private AbstractPlayer rolloutStrategy;

    // State in this node (closed loop)
    private AbstractGameState state;

    protected GroupKTreeNode(GroupKPlayer player, GroupKTreeNode parent, AbstractGameState state,
            Map<AbstractAction, Pair<Integer, Double>> MASTStat, Random rnd) {
        this.player = player;
        this.fmCallsCount = 0;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        totValue = 0.0;
        amafValue = 0.0;
        alpha = 0.0;
        nVisits = 0;
        amafVisit = 0;
        setState(state);
        if (parent != null) {
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
        this.rnd = rnd;
        // Use MASTPlayer as rollout Strategy.
        this.rolloutStrategy = new MASTPlayer(this.player.params.MASTEpsilon, this.player.params.MASTTemperature,
                this.player.params.MASTDefaultValue);
        this.rolloutStrategy.setForwardModel(player.getForwardModel());
        this.MASTStat = MASTStat == null ? new HashMap<>() : MASTStat;
        if (this.rolloutStrategy instanceof MASTPlayer) {
            ((MASTPlayer) this.rolloutStrategy).setStat(MASTStat);
        }
    }

    void setStat(Map<AbstractAction, Pair<Integer, Double>> MASTAT) {
        this.MASTStat = MASTAT;
        if (this.rolloutStrategy instanceof MASTPlayer) {
            ((MASTPlayer) this.rolloutStrategy).setStat(MASTStat);
        }
    }

    /**
     * Performs full MCTS search, using the defined budget limits.
     */
    void mctsSearch() {

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = player.params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (player.params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(player.params.budget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;

        boolean stop = false;

        while (!stop) {
            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            actionsInRollout = new ArrayList<>();

            // Selection + expansion: navigate tree until a node not fully expanded is
            // found, add a new node to the tree
            GroupKTreeNode selected = treePolicy();
            // Monte carlo rollout: return value of MC rollout from the newly added node
            double delta = selected.rollOut();
            // Back up the value of the rollout through the tree
            selected.backUp(delta);
            AMAF(null, actionsInRollout, delta);
            for (AbstractAction action : actionsInRollout) {
                Pair<Integer, Double> stat = MASTStat.getOrDefault(action, new Pair<>(0, 0.0));
                stat.a++;
                stat.b += delta;
                MASTStat.put(action, stat);
            }
            // Finished iteration
            numIters++;

            // Check stopping condition
            PlayerConstants budgetType = player.params.budgetType;
            if (budgetType == BUDGET_TIME) {
                // Time budget
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            } else if (budgetType == BUDGET_ITERATIONS) {
                // Iteration budget
                stop = numIters >= player.params.budget;
            } else if (budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = fmCallsCount > player.params.budget;
            }
        }
    }

    /**
     * Selection + expansion steps.
     * - Tree is traversed until a node not fully expanded is found.
     * - A new child of this node is added to the tree.
     *
     * @return - new node added to the tree.
     */
    private GroupKTreeNode treePolicy() {

        GroupKTreeNode cur = this;

        // Keep iterating while the state reached is not terminal and the depth of the
        // tree is not exceeded
        while (cur.state.isNotTerminal() && cur.depth < player.params.maxTreeDepth) {
            if (!cur.unexpandedActions().isEmpty()) {
                // We have an unexpanded action
                cur = cur.expand();
                return cur;
            } else {
                // Move to next child given by UCT function
                AbstractAction actionChosen = cur.ucb();
                // AbstractAction actionChosen = cur.ucb();
                cur = cur.children.get(actionChosen);
            }
        }

        return cur;
    }

    private void setState(AbstractGameState newState) {
        state = newState;
        if (newState.isNotTerminal())
            for (AbstractAction action : player.getForwardModel().computeAvailableActions(state,
                    player.params.actionSpace)) {
                children.put(action, null); // mark a new node to be expanded
            }
    }

    /**
     * @return A list of the unexpanded Actions from this State
     */
    private List<AbstractAction> unexpandedActions() {
        return children.keySet().stream().filter(a -> children.get(a) == null).collect(toList());
    }

    /**
     * Expands the node by creating a new random child node and adding to the tree.
     *
     * @return - new child node.
     */
    private GroupKTreeNode expand() {
        // Find random child not already created
        Random r = new Random(player.params.getRandomSeed());
        // pick a random unchosen action
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = notChosen.get(r.nextInt(notChosen.size()));

        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any
        // state changes
        AbstractGameState nextState = state.copy();
        advance(nextState, chosen.copy());

        // then instantiate a new node
        GroupKTreeNode tn = new GroupKTreeNode(player, this, nextState, this.MASTStat, rnd);
        children.put(chosen, tn);
        return tn;
    }

    /**
     * Advance the current game state with the given action, count the FM call and
     * compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    private void advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
        root.fmCallsCount++;
    }

    private AbstractAction ucb() {
        // Find child with highest UCB value, maximising for ourselves and minimizing
        // for opponent
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;

        for (AbstractAction action : children.keySet()) {
            GroupKTreeNode child = children.get(action);
            if (child == null)
                throw new AssertionError("Should not be here");
            else if (bestAction == null)
                bestAction = action;

            // Find child value
            // Set the Q value to be a combination of simulation result and AMAF statistics.
            double V = this.player.params.V;
            child.alpha = Math.max(0, (V - nVisits) / V);
            double hvVal = child.totValue;
            double childValue = hvVal / (child.nVisits + player.params.epsilon);
            double amaftotal = child.amafValue;
            double amafVal = amaftotal / (child.amafVisit + player.params.epsilon);
            double finalValue = childValue * (1 - alpha) + alpha * amafVal;
            // double finalValue = childValue;

            // default to standard UCB
            double explorationTerm = player.params.K
                    * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + player.params.epsilon));
            // unless we are using a variant

            // Find 'UCB' value
            // If 'we' are taking a turn we use classic UCB
            // If it is an opponent's turn, then we assume they are trying to minimise our
            // score (with exploration)
            boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
            double uctValue = iAmMoving ? finalValue : -finalValue;
            uctValue += explorationTerm;

            // Apply small noise to break ties randomly
            uctValue = noise(uctValue, player.params.epsilon, player.rnd.nextDouble());

            // Assign value
            if (uctValue > bestValue) {
                bestAction = action;
                bestValue = uctValue;
            }
        }

        if (bestAction == null)
            throw new AssertionError("We have a null value in UCT : shouldn't really happen!");

        root.fmCallsCount++; // log one iteration complete
        return bestAction;
    }

    /**
     * Perform a Monte Carlo rollout from this node.
     *
     * @return - value of rollout.
     */
    private double rollOut() {
        int rolloutDepth = 0; // counting from end of tree

        // If rollouts are enabled, select actions for the rollout in line with the
        // rollout policy
        AbstractGameState rolloutState = state.copy();
        if (player.params.rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                List<AbstractAction> availActions = rolloutStrategy.getForwardModel()
                        .computeAvailableActions(rolloutState, rolloutStrategy.parameters.actionSpace);
                AbstractAction next = rolloutStrategy.getAction(rolloutState, availActions);
                root.actionsInRollout.add(next);
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }
        // Evaluate final state and return normalised score
        double value = player.params.getHeuristic().evaluateState(rolloutState, player.getPlayerID());
        if (Double.isNaN(value))
            throw new AssertionError("Illegal heuristic value - should be a number");

        return value;
    }

    /**
     * Checks if rollout is finished. Rollouts end on maximum length, or if game
     * ended.
     *
     * @param rollerState - current state
     * @param depth       - current depth
     * @return - true if rollout finished, false otherwise
     */
    private boolean finishRollout(AbstractGameState rollerState, int depth) {
        if (depth >= player.params.rolloutLength)
            return true;

        // End of game
        return !rollerState.isNotTerminal();
    }

    /**
     * Back up the value of the child through all parents. Increase number of visits
     * and total value.
     *
     * @param result - value of rollout to backup
     */

    private void backUp(double result) {
        GroupKTreeNode n = this;
        while (n != null) {
            n.nVisits++;
            n.totValue += result;
            n = n.parent;
        }
    }

    // Use separate field and function to track statitics for AMAF.
    private void raveBackUp(double result) {
        GroupKTreeNode n = this;
        while (n != null) {
            n.amafVisit++;
            n.amafValue += result;
            n = n.parent;
        }
    }

    /**
     * Calculates the best action from the root according to the most visited node
     *
     * @return - the best AbstractAction
     */
    AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                GroupKTreeNode node = children.get(action);
                double childValue = node.nVisits;

                // Apply small noise to break ties randomly
                childValue = noise(childValue, player.params.epsilon, player.rnd.nextDouble());

                // Save best value (highest visit count)
                if (childValue > bestValue) {
                    bestValue = childValue;
                    bestAction = action;
                }
            }
        }

        if (bestAction == null) {
            throw new AssertionError("Unexpected - no selection made.");
        }

        return bestAction;
    }

    // Use recursion to update the AMAF statistics for all nodes in the tree.
    private void AMAF(AbstractAction action, List<AbstractAction> selectedActions, double result) {
        if (selectedActions.stream().anyMatch(x -> x.equals(action))) {
            raveBackUp(result);
        }
        for (AbstractAction childAction : children.keySet()) {
            if (children.get(childAction) != null) {
                GroupKTreeNode node = children.get(childAction);
                node.AMAF(childAction, selectedActions, result);
            }
        }
    }

}
