package players.assignment1;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

/**
 * This is a simple version of MCTS that may be useful for newcomers to TAG and
 * MCTS-like algorithms
 * It strips out some of the additional configuration of MCTSPlayer. It uses
 * BasicTreeNode in place of
 * SingleTreeNode.
 */
public class GroupKPlayer extends AbstractPlayer {

    Random rnd;
    GroupKParams params;
    // Statistics for MAST.
    // Keep a copy here to memorise all the statistics during the game.
    Map<AbstractAction, Pair<Integer, Double>> MASTStat;

    public GroupKPlayer() {
        this(System.currentTimeMillis());
    }

    public GroupKPlayer(long seed) {
        this.params = new GroupKParams(seed);
        rnd = new Random(seed);
        setName("Group K");

        // These parameters can be changed, and will impact the Basic MCTS algorithm
        this.params.K = Math.sqrt(2);
        this.params.rolloutLength = 10;
        this.params.maxTreeDepth = 5;
        this.params.epsilon = 1e-6;
        this.params.V = 1;
        this.params.MASTTemperature = 5.0;
        this.params.MASTDefaultValue = 0.0;
        this.params.MASTEpsilon = 0.2;
        this.params.MASTGamma = 0.5;
    }

    public GroupKPlayer(GroupKParams params) {
        this.params = params;
        rnd = new Random(params.getRandomSeed());
        setName("Group K");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Search for best action from the root
        GroupKTreeNode root = new GroupKTreeNode(this, null, gameState, this.MASTStat, rnd);
        // Decay the rollout bias from previous simulations by 0.5.
        if (this.MASTStat != null) {
            root.setStat(Utils.decay(MASTStat, this.params.MASTGamma));
        }

        // mctsSearch does all of the hard work
        root.mctsSearch();

        // Update the MAST statistics with current simulation result;
        this.MASTStat = root.MASTStat;
        // Return best action
        return root.bestAction();
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        this.params.heuristic = heuristic;
    }

    @Override
    public String toString() {
        return "Group K";
    }

    @Override
    public GroupKPlayer copy() {
        return this;
    }
}