package com.catanatron.core.engine;

import com.catanatron.core.model.*;

import java.util.*;

public class Game {
    public static final int TURNS_LIMIT = 1000;

    public final State state;
    public List<Action<?>> playableActions;

    public Game(List<Player> players) {
        this.state = new State(players);
        this.playableActions = MoveGeneration.generatePlayable(state);
    }

    public ActionRecord<?> execute(Action<?> action) {
        // Refresh playable actions based on current state, then validate type presence
        this.playableActions = MoveGeneration.generatePlayable(state);
        boolean ok = playableActions.stream().anyMatch(a -> a.type == action.type);
        if (!ok) throw new IllegalArgumentException("Action not playable now: "+action);
        ActionRecord<?> rec = Reducer.apply(state, action);
        this.playableActions = MoveGeneration.generatePlayable(state);
        return rec;
    }

    public PlayerColor playTick() {
        Player player = state.currentPlayer();
        Action<?> action = player.decide(this, this.playableActions);
        execute(action);
        return winningColor();
    }

    public PlayerColor play() {
        while (winningColor() == null && state.numTurns < TURNS_LIMIT) {
            playTick();
        }
        return winningColor();
    }

    public PlayerColor winningColor() {
        // Basic win rule: 10 VP or all pieces exhausted (simplified)
        for (int i = 0; i < state.colors.size(); i++) {
            int vps = state.playerState.get("P"+i+"_ACTUAL_VICTORY_POINTS");
            if (vps >= 10) return state.colors.get(i);
            int settlementsLeft = state.playerState.get("P"+i+"_SETTLEMENTS_AVAILABLE");
            int citiesLeft = state.playerState.get("P"+i+"_CITIES_AVAILABLE");
            if (settlementsLeft == 0 && citiesLeft == 0) return state.colors.get(i);
        }
        return null;
    }

    public Game copy() {
        Game g = new Game(this.state.players); // shares players
        // replace state and actions
        g.playableActions = new ArrayList<>(this.playableActions);
        return g;
    }
}
