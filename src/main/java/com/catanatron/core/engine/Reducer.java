package com.catanatron.core.engine;

import com.catanatron.core.model.*;
import java.util.Random;

public final class Reducer {
  private Reducer() {}

  private static final Random RNG = new Random();

  public static ActionRecord<?> apply(State state, Action<?> action) {
    return switch (action.type) {
      case END_TURN -> endTurn(state, action);
      case ROLL -> roll(state, action);
      case BUILD_SETTLEMENT -> initialBuildSettlement(state, action);
      case BUILD_ROAD -> initialBuildRoad(state, action);
      case BUILD_CITY -> buildCity(state, action);
      case DISCARD -> applyDiscard(state, action);
      case MOVE_ROBBER -> applyMoveRobber(state, action);
      case BUY_DEVELOPMENT_CARD -> buyDev(state, action);
      case PLAY_YEAR_OF_PLENTY -> playYearOfPlenty(state, action);
      case PLAY_ROAD_BUILDING -> playRoadBuilding(state, action);
      case PLAY_KNIGHT_CARD -> playKnight(state, action);
      case PLAY_MONOPOLY -> playMonopoly(state, action);
      default -> new ActionRecord<>(action, null);
    };
  }

  private static ActionRecord<?> endTurn(State s, Action<?> a) {
    // Clean per-turn flags
    set(s, s.currentColor(), "_HAS_ROLLED", 0);
    // Reset dev-card per-turn flag and set owned-at-start markers for next turn
    int playerIndex = s.currentPlayerIndex;
    s.playerState.put("P" + playerIndex + "_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 0);
    // Owned-at-start means playable next turn if in hand
    for (var card :
        new DevCard[] {
          DevCard.KNIGHT, DevCard.YEAR_OF_PLENTY, DevCard.ROAD_BUILDING, DevCard.MONOPOLY
        }) {
      String base = "P" + playerIndex + "_" + card.name();
      boolean inHand = s.playerState.get(base + "_IN_HAND") > 0;
      s.playerState.put(base + "_OWNED_AT_START", inHand ? 1 : 0);
    }
    // Advance
    int nextPlayerIndex = (s.currentPlayerIndex + 1) % s.colors.size();
    s.currentPlayerIndex = nextPlayerIndex;
    s.currentTurnIndex = nextPlayerIndex;
    s.numTurns += 1;
    s.currentPrompt = ActionPrompt.PLAY_TURN;
    return new ActionRecord<>(a, null);
  }

  private static ActionRecord<?> buyDev(State s, Action<?> a) {
    if (s.developmentDeck.isEmpty()) throw new IllegalStateException("no dev cards");
    // Cost: 1 sheep, 1 wheat, 1 ore
    if (s.playerState.get("P" + s.currentPlayerIndex + "_SHEEP_IN_HAND") < 1
        || s.playerState.get("P" + s.currentPlayerIndex + "_WHEAT_IN_HAND") < 1
        || s.playerState.get("P" + s.currentPlayerIndex + "_ORE_IN_HAND") < 1) {
      throw new IllegalStateException("cannot afford dev card");
    }
    s.playerState.put(
        "P" + s.currentPlayerIndex + "_SHEEP_IN_HAND",
        s.playerState.get("P" + s.currentPlayerIndex + "_SHEEP_IN_HAND") - 1);
    s.playerState.put(
        "P" + s.currentPlayerIndex + "_WHEAT_IN_HAND",
        s.playerState.get("P" + s.currentPlayerIndex + "_WHEAT_IN_HAND") - 1);
    s.playerState.put(
        "P" + s.currentPlayerIndex + "_ORE_IN_HAND",
        s.playerState.get("P" + s.currentPlayerIndex + "_ORE_IN_HAND") - 1);

    var drawnCard = s.developmentDeck.remove(s.developmentDeck.size() - 1);
    String inHandKey = "P" + s.currentPlayerIndex + "_" + drawnCard.name() + "_IN_HAND";
    s.playerState.put(inHandKey, s.playerState.get(inHandKey) + 1);
    if (drawnCard == DevCard.VICTORY_POINT) {
      addByIndex(s, s.currentPlayerIndex, "_ACTUAL_VICTORY_POINTS", +1);
    }
    return new ActionRecord<>(new Action<>(a.color, a.type, drawnCard), drawnCard);
  }

  private static ActionRecord<?> playYearOfPlenty(State s, Action<?> a) {
    // value is String[]{resourceName} size 1 or 2
    String[] pick = (String[]) a.value;
    if (pick.length == 0 || pick.length > 2)
      throw new IllegalArgumentException("invalid YOP selection");
    for (String r : pick) add(s, a.color, "_" + r + "_IN_HAND", +1);
    markDevPlayed(s, a.color, DevCard.YEAR_OF_PLENTY);
    return new ActionRecord<>(a, null);
  }

  private static ActionRecord<?> playRoadBuilding(State s, Action<?> a) {
    s.isRoadBuilding = true;
    s.freeRoadsAvailable = 2;
    markDevPlayed(s, a.color, DevCard.ROAD_BUILDING);
    return new ActionRecord<>(a, null);
  }

  private static ActionRecord<?> playKnight(State s, Action<?> a) {
    // Set prompt to move robber; increase played knight count; handle largest army later
    s.currentPrompt = ActionPrompt.MOVE_ROBBER;
    int playerIndex = s.colors.indexOf(a.color);
    s.playerState.put(
        "P" + playerIndex + "_PLAYED_KNIGHT",
        s.playerState.get("P" + playerIndex + "_PLAYED_KNIGHT") + 1);
    markDevPlayed(s, a.color, DevCard.KNIGHT);
    maintainLargestArmy(s, playerIndex);
    return new ActionRecord<>(a, null);
  }

  private static void maintainLargestArmy(State s, int idxCurrent) {
    int bestIdx = -1;
    int bestCount = 0;
    for (int i = 0; i < s.colors.size(); i++) {
      int count = s.playerState.get("P" + i + "_PLAYED_KNIGHT");
      if (count > bestCount) {
        bestCount = count;
        bestIdx = i;
      }
    }
    // Threshold of 3 knights
    int prevIdx = -1;
    for (int i = 0; i < s.colors.size(); i++)
      if (s.playerState.get("P" + i + "_HAS_ARMY") == 1) {
        prevIdx = i;
        break;
      }
    for (int i = 0; i < s.colors.size(); i++) s.playerState.put("P" + i + "_HAS_ARMY", 0);
    if (bestCount >= 3 && bestIdx >= 0) {
      s.playerState.put("P" + bestIdx + "_HAS_ARMY", 1);
      if (prevIdx != bestIdx) {
        if (prevIdx >= 0) {
          addByIndex(s, prevIdx, "_VICTORY_POINTS", -2);
          addByIndex(s, prevIdx, "_ACTUAL_VICTORY_POINTS", -2);
        }
        addByIndex(s, bestIdx, "_VICTORY_POINTS", +2);
        addByIndex(s, bestIdx, "_ACTUAL_VICTORY_POINTS", +2);
      }
    } else {
      if (prevIdx >= 0) {
        addByIndex(s, prevIdx, "_VICTORY_POINTS", -2);
        addByIndex(s, prevIdx, "_ACTUAL_VICTORY_POINTS", -2);
      }
    }
  }

  private static ActionRecord<?> playMonopoly(State s, Action<?> a) {
    String resource = (String) a.value;
    int idx = s.colors.indexOf(a.color);
    int total = 0;
    // For all opponents, collect all of that resource
    for (int i = 0; i < s.colors.size(); i++) {
      if (i == idx) continue;
      String key = "P" + i + "_" + resource + "_IN_HAND";
      int have = s.playerState.get(key);
      if (have > 0) {
        s.playerState.put(key, 0);
        total += have;
      }
    }
    s.playerState.put(
        "P" + idx + "_" + resource + "_IN_HAND",
        s.playerState.get("P" + idx + "_" + resource + "_IN_HAND") + total);
    markDevPlayed(s, a.color, DevCard.MONOPOLY);
    return new ActionRecord<>(a, null);
  }

  private static void markDevPlayed(State s, PlayerColor c, DevCard card) {
    int idx = s.colors.indexOf(c);
    s.playerState.put("P" + idx + "_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 1);
    // consume one from hand
    String key = "P" + idx + "_" + card.name() + "_IN_HAND";
    s.playerState.put(key, s.playerState.get(key) - 1);
  }

  private static ActionRecord<?> roll(State s, Action<?> a) {
    set(s, s.currentColor(), "_HAS_ROLLED", 1);
    int d1, d2;
    if (a.value instanceof int[] arr && arr.length == 2) {
      d1 = arr[0];
      d2 = arr[1];
    } else {
      d1 = RNG.nextInt(6) + 1;
      d2 = RNG.nextInt(6) + 1;
    }
    int sum = d1 + d2;
    if (sum == 7) {
      // Check discards
      int nextDiscardIdx = nextDiscardIndex(s);
      if (nextDiscardIdx >= 0) {
        s.currentPlayerIndex = nextDiscardIdx;
        s.currentPrompt = ActionPrompt.DISCARD;
        s.isDiscarding = true;
      } else {
        s.currentPrompt = ActionPrompt.MOVE_ROBBER;
        s.isMovingKnight = true;
      }
    } else {
      // Payout: for each tile with this number, give 1 per settlement, 2 per city to owner if not
      // robbed
      for (int tileId : s.map.getTileIdsByNumber(sum)) {
        if (tileId == s.robberTileId) continue;
        var res = s.map.getTileResource(tileId);
        if (res == null) continue;
        for (Integer nodeId : s.map.getTileNodes(tileId)) {
          var building = s.board.buildingAt(nodeId);
          if (building == null) continue;
          var color = building.getKey();
          var type = building.getValue();
          int amount = (type == com.catanatron.core.model.BuildingType.CITY ? 2 : 1);
          addResource(s, color, res, amount);
        }
      }
      s.currentPrompt = ActionPrompt.PLAY_TURN;
    }
    return new ActionRecord<>(
        new Action<>(a.color, a.type, new int[] {d1, d2}), new int[] {d1, d2});
  }

  private static int nextDiscardIndex(State s) {
    for (int i = 0; i < s.colors.size(); i++) {
      int idx = (s.currentTurnIndex + i) % s.colors.size();
      if (numResources(s, s.colors.get(idx)) > s.discardLimit) return idx;
    }
    return -1;
  }

  private static ActionRecord<?> initialBuildSettlement(State s, Action<?> a) {
    if (s.isInitialBuildPhase) {
      // Advance prompts as in snake placement (simplified)
      // Award 1 VP for settlement and consume piece.
      add(s, s.currentColor(), "_VICTORY_POINTS", 1);
      add(s, s.currentColor(), "_ACTUAL_VICTORY_POINTS", 1);
      add(s, s.currentColor(), "_SETTLEMENTS_AVAILABLE", -1);
      int nodeId = (int) a.value;
      s.board.buildSettlement(s.currentColor(), nodeId);
      // Track last initial settlement to constrain initial road
      s.lastInitialSettlement.put(s.currentColor(), nodeId);
      maintainLongestRoad(s); // settlements can block/cut in full rules; safe to recompute
      s.currentPrompt = ActionPrompt.BUILD_INITIAL_ROAD;
    } else {
      // Pay, place, update availability
      if (!Costs.canAffordSettlement(s, s.currentColor()))
        throw new IllegalStateException("cannot afford settlement");
      Costs.paySettlement(s, s.currentColor());
      int nodeId = (int) a.value;
      s.board.buildSettlement(s.currentColor(), nodeId);
      add(s, s.currentColor(), "_SETTLEMENTS_AVAILABLE", -1);
      add(s, s.currentColor(), "_VICTORY_POINTS", 1);
      add(s, s.currentColor(), "_ACTUAL_VICTORY_POINTS", 1);
      maintainLongestRoad(s);
      s.currentPrompt = ActionPrompt.PLAY_TURN;
    }
    return new ActionRecord<>(a, null);
  }

  private static ActionRecord<?> initialBuildRoad(State s, Action<?> a) {
    if (s.isInitialBuildPhase) {
      int numPlayers = s.colors.size();
      // naive: first round forward, then backward, then start play
      int buildings = 0; // simplified placeholder
      if (buildings < numPlayers) {
        add(s, s.currentColor(), "_ROADS_AVAILABLE", -1);
        com.catanatron.core.model.Edge edge = (com.catanatron.core.model.Edge) a.value;
        int aN = edge.a();
        int bN = edge.b();
        // Initial road must be adjacent to last settlement
        Integer lastNode = s.lastInitialSettlement.get(s.currentColor());
        if (lastNode != null && aN != lastNode && bN != lastNode) {
          throw new IllegalStateException("initial road must touch last settlement");
        }
        // validate
        if (!isEdgeBuildable(s, s.currentColor(), edge))
          throw new IllegalStateException("illegal road placement");
        s.board.buildRoad(s.currentColor(), aN, bN);
        advance(s, +1);
        s.currentPrompt = ActionPrompt.BUILD_INITIAL_SETTLEMENT;
      } else {
        s.isInitialBuildPhase = false;
        s.currentPrompt = ActionPrompt.PLAY_TURN;
      }
    } else {
      com.catanatron.core.model.Edge edge = (com.catanatron.core.model.Edge) a.value;
      int aN = edge.a();
      int bN = edge.b();
      if (!isEdgeBuildable(s, s.currentColor(), edge))
        throw new IllegalStateException("illegal road placement");
      if (s.isRoadBuilding && s.freeRoadsAvailable > 0) {
        s.board.buildRoad(s.currentColor(), aN, bN);
        add(s, s.currentColor(), "_ROADS_AVAILABLE", -1);
        s.freeRoadsAvailable -= 1;
        if (s.freeRoadsAvailable == 0) s.isRoadBuilding = false;
      } else {
        if (!Costs.canAffordRoad(s, s.currentColor()))
          throw new IllegalStateException("cannot afford road");
        Costs.payRoad(s, s.currentColor());
        s.board.buildRoad(s.currentColor(), aN, bN);
        add(s, s.currentColor(), "_ROADS_AVAILABLE", -1);
      }
      maintainLongestRoad(s);
      s.currentPrompt = ActionPrompt.PLAY_TURN;
    }
    return new ActionRecord<>(a, null);
  }

  private static ActionRecord<?> applyDiscard(State s, Action<?> a) {
    // Minimal: discard half at random not implemented; for now just clear excess to limit
    int idx = s.currentPlayerIndex;
    PlayerColor c = s.colors.get(idx);
    int total = numResources(s, c);
    int toKeep = s.discardLimit; // keep exactly limit
    if (total > toKeep) {
      // naive: reduce WOOD first, then others
      int[] order = {0, 1, 2, 3, 4};
      String[] keys = {"WOOD", "BRICK", "SHEEP", "WHEAT", "ORE"};
      int reduce = total - toKeep;
      for (int i = 0; i < order.length && reduce > 0; i++) {
        String key = "P" + idx + "_" + keys[i] + "_IN_HAND";
        int have = s.playerState.get(key);
        int take = Math.min(have, reduce);
        s.playerState.put(key, have - take);
        reduce -= take;
      }
    }
    // advance to next discarder or robber
    int next = nextDiscardIndex(s);
    if (next >= 0) {
      s.currentPlayerIndex = next;
      s.currentPrompt = ActionPrompt.DISCARD;
    } else {
      s.currentPlayerIndex = s.currentTurnIndex;
      s.currentPrompt = ActionPrompt.MOVE_ROBBER;
      s.isDiscarding = false;
      s.isMovingKnight = true;
    }
    return new ActionRecord<>(a, null);
  }

  private static ActionRecord<?> applyMoveRobber(State s, Action<?> a) {
    int newTileId = (int) a.value;
    s.robberTileId = newTileId;

    // Attempt to steal from a victim with resources on this tile (adjacent building)
    java.util.List<PlayerColor> victims = new java.util.ArrayList<>();
    for (Integer nodeId : s.map.getTileNodes(newTileId)) {
      var b = s.board.buildingAt(nodeId);
      if (b == null) continue;
      var victim = b.getKey();
      if (victim == a.color) continue;
      if (numResources(s, victim) > 0) victims.add(victim);
    }
    PlayerColor stolenFrom = null;
    com.catanatron.core.model.Resource stolenRes = null;
    if (!victims.isEmpty()) {
      stolenFrom = victims.get(RNG.nextInt(victims.size()));
      // Choose a random resource from victim's hand proportionally to counts
      java.util.List<com.catanatron.core.model.Resource> bag = new java.util.ArrayList<>();
      for (var r : com.catanatron.core.model.Resource.values()) {
        int idx = s.colors.indexOf(stolenFrom);
        int count = s.playerState.get("P" + idx + "_" + r.name() + "_IN_HAND");
        for (int i = 0; i < count; i++) bag.add(r);
      }
      if (!bag.isEmpty()) {
        stolenRes = bag.get(RNG.nextInt(bag.size()));
        addResource(s, stolenFrom, stolenRes, -1);
        addResource(s, a.color, stolenRes, +1);
      }
    }

    s.currentPrompt = ActionPrompt.PLAY_TURN;
    s.isMovingKnight = false;
    return new ActionRecord<>(a, stolenRes);
  }

  private static ActionRecord<?> buildCity(State s, Action<?> a) {
    int nodeId = (int) a.value;
    if (!Costs.canAffordCity(s, s.currentColor()))
      throw new IllegalStateException("cannot afford city");
    Costs.payCity(s, s.currentColor());
    s.board.buildCity(s.currentColor(), nodeId);
    add(s, s.currentColor(), "_CITIES_AVAILABLE", -1);
    add(s, s.currentColor(), "_SETTLEMENTS_AVAILABLE", +1);
    add(s, s.currentColor(), "_VICTORY_POINTS", +1);
    add(s, s.currentColor(), "_ACTUAL_VICTORY_POINTS", +1);
    maintainLongestRoad(s);
    return new ActionRecord<>(a, null);
  }

  private static void maintainLongestRoad(State s) {
    int bestLength = 0;
    int bestPlayerIndex = -1;
    for (int i = 0; i < s.colors.size(); i++) {
      var color = s.colors.get(i);
      int length = s.board.longestRoadLength(color);
      s.playerState.put("P" + i + "_LONGEST_ROAD_LENGTH", length);
      if (length > bestLength) {
        bestLength = length;
        bestPlayerIndex = i;
      }
    }
    // Find previous holder
    int previousHolderIndex = -1;
    for (int i = 0; i < s.colors.size(); i++) {
      if (s.playerState.get("P" + i + "_HAS_ROAD") == 1) {
        previousHolderIndex = i;
        break;
      }
    }
    // Clear flags
    for (int i = 0; i < s.colors.size(); i++) s.playerState.put("P" + i + "_HAS_ROAD", 0);

    if (bestLength >= 5 && bestPlayerIndex >= 0) {
      s.playerState.put("P" + bestPlayerIndex + "_HAS_ROAD", 1);
      if (previousHolderIndex != bestPlayerIndex) {
        if (previousHolderIndex >= 0) {
          addByIndex(s, previousHolderIndex, "_VICTORY_POINTS", -2);
          addByIndex(s, previousHolderIndex, "_ACTUAL_VICTORY_POINTS", -2);
        }
        addByIndex(s, bestPlayerIndex, "_VICTORY_POINTS", +2);
        addByIndex(s, bestPlayerIndex, "_ACTUAL_VICTORY_POINTS", +2);
      }
    } else {
      // No valid longest road; revoke from previous holder if any
      if (previousHolderIndex >= 0) {
        addByIndex(s, previousHolderIndex, "_VICTORY_POINTS", -2);
        addByIndex(s, previousHolderIndex, "_ACTUAL_VICTORY_POINTS", -2);
      }
    }
  }

  private static void addByIndex(State s, int idx, String suffix, int delta) {
    String key = "P" + idx + suffix;
    s.playerState.put(key, s.playerState.get(key) + delta);
  }

  private static void advance(State s, int step) {
    int next = (s.currentPlayerIndex + step + s.colors.size()) % s.colors.size();
    s.currentPlayerIndex = next;
    s.currentTurnIndex = next;
    s.numTurns += 1;
  }

  private static void set(State s, PlayerColor c, String suffix, int v) {
    int playerIndex = s.colors.indexOf(c);
    s.playerState.put("P" + playerIndex + suffix, v);
  }

  private static void add(State s, PlayerColor c, String suffix, int delta) {
    int playerIndex = s.colors.indexOf(c);
    String key = "P" + playerIndex + suffix;
    s.playerState.put(key, s.playerState.get(key) + delta);
  }

  private static void addResource(
      State s, PlayerColor c, com.catanatron.core.model.Resource r, int amount) {
    int playerIndex = s.colors.indexOf(c);
    String key = "P" + playerIndex + "_" + r.name() + "_IN_HAND";
    s.playerState.put(key, s.playerState.get(key) + amount);
    // Bank accounting omitted for now
  }

  private static int numResources(State s, PlayerColor c) {
    int playerIndex = s.colors.indexOf(c);
    int wood = s.playerState.get("P" + playerIndex + "_WOOD_IN_HAND");
    int brick = s.playerState.get("P" + playerIndex + "_BRICK_IN_HAND");
    int sheep = s.playerState.get("P" + playerIndex + "_SHEEP_IN_HAND");
    int wheat = s.playerState.get("P" + playerIndex + "_WHEAT_IN_HAND");
    int ore = s.playerState.get("P" + playerIndex + "_ORE_IN_HAND");
    return wood + brick + sheep + wheat + ore;
  }

  private static boolean isEdgeBuildable(
      State s, PlayerColor color, com.catanatron.core.model.Edge edge) {
    long requestedKey =
        (((long) Math.min(edge.a(), edge.b())) << 32)
            | (((int) Math.max(edge.a(), edge.b())) & 0xffffffffL);
    for (var allowedEdge : s.board.buildableEdges(color)) {
      long allowedKey =
          (((long) Math.min(allowedEdge.a(), allowedEdge.b())) << 32)
              | (((int) Math.max(allowedEdge.a(), allowedEdge.b())) & 0xffffffffL);
      if (allowedKey == requestedKey) return true;
    }
    return false;
  }
}
