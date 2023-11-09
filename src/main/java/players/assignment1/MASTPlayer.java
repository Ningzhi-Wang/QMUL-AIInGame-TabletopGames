package players.assignment1;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

public class MASTPlayer extends AbstractPlayer {

    Random rnd;
    Map<AbstractAction, Pair<Integer, Double>> MASTStatistics;
    double epsilon;
    double temperature;
    double defaultValue;

    public MASTPlayer() {
        this(System.currentTimeMillis());
    }

    public MASTPlayer(long seed) {
        this(seed, 0.2, 5.0, 0.0);
    }

    public MASTPlayer(double epsilon, double temperature, double defaultValue) {
        this.epsilon = epsilon;
        this.temperature = temperature;
        this.defaultValue = defaultValue;
        rnd = new Random(System.currentTimeMillis());
        this.MASTStatistics = new HashMap<>();
        setName("MAST");
    }

    public MASTPlayer(long seed, double epsilon, double temperature, double defaultValue) {
        this.epsilon = epsilon;
        this.temperature = temperature;
        this.defaultValue = defaultValue;
        rnd = new Random(seed);
        this.MASTStatistics = new HashMap<>();
        setName("MAST");
    }

    public MASTPlayer(GroupKParams params) {
        rnd = new Random(params.getRandomSeed());
        this.MASTStatistics = new HashMap<>();
        setName("MAST");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Sample from actions with probilities equal to their value.
        Map<AbstractAction, Double> actionValues = new HashMap<>();
        for (AbstractAction action : actions) {
            Pair<Integer, Double> stat = this.MASTStatistics.get(action);
            if (stat == null || stat.a <= 0) {
                actionValues.put(action, defaultValue);
            } else {
                actionValues.put(action, stat.b / stat.a);
            }
        }
        return Utils.sampleFrom(actionValues, temperature, epsilon, rnd);
    }

    public void setStat(Map<AbstractAction, Pair<Integer, Double>> MASTStat) {
        this.MASTStatistics = MASTStat;
    }

    @Override
    public String toString() {
        return "Group K - MAST";
    }

    @Override
    public MASTPlayer copy() {
        return this;
    }
}
