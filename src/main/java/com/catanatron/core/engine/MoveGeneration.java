package com.catanatron.core.engine;

import com.catanatron.core.model.*;
import java.util.ArrayList;
import java.util.List;

public final class MoveGeneration {
  private MoveGeneration() {}

  public static List<Action<?>> generatePlayable(State state) {
    List<Action<?>> actions = new ArrayList<>();
    PlayerColor color = state.currentColor();
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
            for (Integer nodeId : state.board.buildableNodeIds(color, false)) {
              actions.add(new Action<>(color, ActionType.BUILD_SETTLEMENT, nodeId));
            }
          }
          if (get(state, color, "_CITIES_AVAILABLE") > 0 && Costs.canAffordCity(state, color)) {
            for (Integer n : state.board.ownedSettlementNodes(color)) {
              actions.add(new Action<>(color, ActionType.BUILD_CITY, n));
            }
          }
          boolean hasFreeRoads = state.isRoadBuilding && state.freeRoadsAvailable > 0;
          if (hasFreeRoads || Costs.canAffordRoad(state, color)) {
            for (var edge : state.board.buildableEdges(color)) {
              actions.add(new Action<>(color, ActionType.BUILD_ROAD, edge));
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
            for (var resource : new String[] {"WOOD", "BRICK", "SHEEP", "WHEAT", "ORE"}) {
              actions.add(
                  new Action<>(color, ActionType.PLAY_YEAR_OF_PLENTY, new String[] {resource}));
            }
            // Also allow two-card variant
            String[] resources = new String[] {"WOOD", "BRICK", "SHEEP", "WHEAT", "ORE"};
            for (int i = 0; i < resources.length; i++) {
              for (int j = i; j < resources.length; j++) {
                actions.add(
                    new Action<>(
                        color,
                        ActionType.PLAY_YEAR_OF_PLENTY,
                        new String[] {resources[i], resources[j]}));
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
            for (var resource : new String[] {"WOOD", "BRICK", "SHEEP", "WHEAT", "ORE"}) {
              actions.add(new Action<>(color, ActionType.PLAY_MONOPOLY, resource));
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
        // Only offer roads adjacent to the last initial settlement for this player
        Integer lastSettlementNodeId = state.lastInitialSettlement.get(color);
        for (var edge : state.board.buildableEdges(color)) {
          if (lastSettlementNodeId == null
              || edge.a() == lastSettlementNodeId
              || edge.b() == lastSettlementNodeId) {
            actions.add(new Action<>(color, ActionType.BUILD_ROAD, edge));
          }
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

  static int get(State state, PlayerColor color, String suffix) {
    int playerIndex = state.colors.indexOf(color);
    return state.playerState.get("P" + playerIndex + suffix);
  }

  static boolean canPlayDev(State state, PlayerColor color, DevCard card) {
    int playerIndex = state.colors.indexOf(color);
    String base = "P" + playerIndex + "_";
    boolean notPlayedThisTurn =
        state.playerState.get(base + "HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN") == 0;
    boolean ownedAtStart = state.playerState.get(base + card.name() + "_OWNED_AT_START") == 1;
    boolean inHand = state.playerState.get(base + card.name() + "_IN_HAND") > 0;
    return notPlayedThisTurn && ownedAtStart && inHand;
  }
}
