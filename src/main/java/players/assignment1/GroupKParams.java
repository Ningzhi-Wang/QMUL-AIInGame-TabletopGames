package players.assignment1;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

import java.util.Arrays;

public class GroupKParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10; // assuming we have a good heuristic
    public int maxTreeDepth = 100; // effectively no limit
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    public int V = 1;
    public double MASTTemperature = 5.0;
    public double MASTDefaultValue = 0;
    public double MASTEpsilon = 0.2;
    public double MASTGamma = 0.5;

    public GroupKParams() {
        this(System.currentTimeMillis());
    }

    public GroupKParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("maxTreeDepth", 100, Arrays.asList(1, 3, 10, 30, 100));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("V", 1, Arrays.asList(0, 1, 10, 100));
        addTunableParameter("MASTTemperature", 5.0, Arrays.asList(0, 0.5, 1, 5, 10));
        addTunableParameter("MASTDefaultValue", 0.0, Arrays.asList(-10, -1, 0, 1, 10));
        addTunableParameter("MASTEpsilon", 0.2, Arrays.asList(0.1, 0.2, 0.5, 0.9));
        addTunableParameter("MASTGamma", 0.5, Arrays.asList(0.1, 0.2, 0.5, 0.9));
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        V = (int) getParameterValue("V");
        MASTTemperature = (double) getParameterValue("MASTTemperature");
        MASTDefaultValue = (double) getParameterValue("MASTDefaultValue");
        MASTEpsilon = (double) getParameterValue("MASTEpsilon");
        MASTGamma = (double) getParameterValue("MASTGamma");
    }

    @Override
    protected GroupKParams _copy() {
        // All the copying is done in TunableParameters.copy()
        // Note that any *local* changes of parameters will not be copied
        // unless they have been 'registered' with setParameterValue("name", value)
        return new GroupKParams(System.currentTimeMillis());
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    public GroupKPlayer instantiate() {
        return new GroupKPlayer((GroupKParams) this.copy());
    }

}
