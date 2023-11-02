package players.heuristics;

import core.interfaces.IStateHeuristic;
import games.cantstop.CantStopGameState;

import core.AbstractGameState;
import core.CoreConstants;

public class CantStopHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CantStopGameState cs = (CantStopGameState) gs;
        double score = gs.getGameScore(playerId);
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return score * 1.5;
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return score * 0.5;
        return score;
    }

}
