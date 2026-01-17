package com.catanatron.core.engine;

import com.catanatron.core.model.*;
import java.util.ArrayList;
import java.util.List;

public final class MoveGeneration {
  private MoveGeneration() {}

  public static List<Action<?>> generatePlayable(State state) {
    var actions = new ArrayList<Action<?>>();
    var color = state.currentColor();
    switch (state.currentPrompt) {
      case PLAY_TURN -> {
        // Minimal: allow ROLL if not rolled, else END_TURN
        if (get(state, color, "_HAS_ROLLED") == 0) {
          actions.add(new Action<>(color, ActionType.ROLL, null));
        } else {
          actions.add(new Action<>(color, ActionType.END_TURN, null));
          // allow building options if connected and enough pieces and affordable
          if (get(state, color, "_SETTLEMENTS_AVAILABLE") > 0
              && Costs.canAffordSettlement(state, color)) {
            for (Integer n : state.board.buildableNodeIds(color, false)) {
              actions.add(new Action<>(color, ActionType.BUILD_SETTLEMENT, n));
            }
          }
          if (get(state, color, "_CITIES_AVAILABLE") > 0 && Costs.canAffordCity(state, color)) {
            for (Integer n : state.board.ownedSettlementNodes(color)) {
              actions.add(new Action<>(color, ActionType.BUILD_CITY, n));
            }
          }
          boolean freeRoads = state.isRoadBuilding && state.freeRoadsAvailable > 0;
          if (freeRoads || Costs.canAffordRoad(state, color)) {
            for (var e : state.board.buildableEdges(color)) {
              actions.add(new Action<>(color, ActionType.BUILD_ROAD, e));
            }
          }
          if (state.developmentDeck.size() > 0
              && state.playerState.get("P" + state.currentPlayerIndex + "_SHEEP_IN_HAND") >= 1
              && state.playerState.get("P" + state.currentPlayerIndex + "_WHEAT_IN_HAND") >= 1
              && state.playerState.get("P" + state.currentPlayerIndex + "_ORE_IN_HAND") >= 1) {
            actions.add(new Action<>(color, ActionType.BUY_DEVELOPMENT_CARD, null));
          }
          // Dev card plays (simplified constraints)
          if (canPlayDev(state, color, DevCard.YEAR_OF_PLENTY)) {
            // Enumerate single-card choices; for test simplicity
            for (var r : new String[] {"WOOD", "BRICK", "SHEEP", "WHEAT", "ORE"}) {
              actions.add(new Action<>(color, ActionType.PLAY_YEAR_OF_PLENTY, new String[] {r}));
            }
            // Also allow two-card variant
            String[] R = new String[] {"WOOD", "BRICK", "SHEEP", "WHEAT", "ORE"};
            for (int i = 0; i < R.length; i++) {
              for (int j = i; j < R.length; j++) {
                actions.add(
                    new Action<>(color, ActionType.PLAY_YEAR_OF_PLENTY, new String[] {R[i], R[j]}));
              }
            }
          }
          if (canPlayDev(state, color, DevCard.ROAD_BUILDING)
              && !state.board.buildableEdges(color).isEmpty()) {
            actions.add(new Action<>(color, ActionType.PLAY_ROAD_BUILDING, null));
          }
          if (canPlayDev(state, color, DevCard.KNIGHT)) {
            actions.add(new Action<>(color, ActionType.PLAY_KNIGHT_CARD, null));
          }
          if (canPlayDev(state, color, DevCard.MONOPOLY)) {
            for (var r : new String[] {"WOOD", "BRICK", "SHEEP", "WHEAT", "ORE"}) {
              actions.add(new Action<>(color, ActionType.PLAY_MONOPOLY, r));
            }
          }
        }
      }
      case BUILD_INITIAL_SETTLEMENT -> {
        for (Integer nodeId : state.board.buildableNodeIds(color, true)) {
          actions.add(new Action<>(color, ActionType.BUILD_SETTLEMENT, nodeId));
        }
      }
      case BUILD_INITIAL_ROAD -> {
        for (var e : state.board.buildableEdges(color)) {
          actions.add(new Action<>(color, ActionType.BUILD_ROAD, e));
        }
      }
      case DISCARD -> actions.add(new Action<>(color, ActionType.DISCARD, null));
      case MOVE_ROBBER -> {
        // Minimal: move robber to any tile (id) that is not current
        for (Integer tileId : state.map.tilesById.keySet()) {
          if (!tileId.equals(state.robberTileId)) {
            actions.add(new Action<>(color, ActionType.MOVE_ROBBER, tileId));
          }
        }
      }
    }
    return actions;
  }

  static int get(State s, PlayerColor c, String suffix) {
    int idx = s.colors.indexOf(c);
    return s.playerState.get("P" + idx + suffix);
  }

  static boolean canPlayDev(State s, PlayerColor c, DevCard card) {
    int idx = s.colors.indexOf(c);
    String base = "P" + idx + "_";
    boolean notPlayedThisTurn =
        s.playerState.get(base + "HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN") == 0;
    boolean ownedAtStart = s.playerState.get(base + card.name() + "_OWNED_AT_START") == 1;
    boolean inHand = s.playerState.get(base + card.name() + "_IN_HAND") > 0;
    return notPlayedThisTurn && ownedAtStart && inHand;
  }
}
