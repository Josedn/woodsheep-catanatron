package com.catanatron.core.engine;

import com.catanatron.core.board.Board;
import com.catanatron.core.map.CatanMap;
import com.catanatron.core.model.PlayerColor;
import com.catanatron.core.util.Decks;
import java.util.*;

public class State {
  public final List<Player> players;
  public final List<PlayerColor> colors;

  public int[] bank = Decks.startingResourceBank();
  public final Map<String, Integer> playerState = new HashMap<>();
  public int currentPlayerIndex = 0;
  public int currentTurnIndex = 0;
  public int numTurns = 0;
  public ActionPrompt currentPrompt = ActionPrompt.BUILD_INITIAL_SETTLEMENT;

  public boolean isInitialBuildPhase = true;
  public boolean isDiscarding = false;
  public boolean isMovingKnight = false;

  public final CatanMap map;
  public final Board board;
  public int robberTileId;
  public int discardLimit = 7;
  public final java.util.Map<PlayerColor, Integer> lastInitialSettlement =
      new java.util.HashMap<>();
  // Dev cards
  public java.util.List<com.catanatron.core.model.DevCard> developmentDeck;
  public boolean isRoadBuilding = false;
  public int freeRoadsAvailable = 0;

  public State(List<Player> players) {
    this.players = new ArrayList<>(players);
    this.colors = players.stream().map(p -> p.color).toList();
    this.map = CatanMap.base();
    this.board = new Board(map);
    this.robberTileId = map.getDesertTileId();
    // Initialize development deck
    this.developmentDeck = new java.util.ArrayList<>();
    for (int i = 0; i < 14; i++) developmentDeck.add(com.catanatron.core.model.DevCard.KNIGHT);
    for (int i = 0; i < 2; i++)
      developmentDeck.add(com.catanatron.core.model.DevCard.YEAR_OF_PLENTY);
    for (int i = 0; i < 2; i++)
      developmentDeck.add(com.catanatron.core.model.DevCard.ROAD_BUILDING);
    for (int i = 0; i < 2; i++) developmentDeck.add(com.catanatron.core.model.DevCard.MONOPOLY);
    for (int i = 0; i < 5; i++)
      developmentDeck.add(com.catanatron.core.model.DevCard.VICTORY_POINT);
    java.util.Collections.shuffle(developmentDeck);
    for (int i = 0; i < colors.size(); i++) {
      String key = "P" + i;
      playerState.put(key + "_VICTORY_POINTS", 0);
      playerState.put(key + "_ACTUAL_VICTORY_POINTS", 0);
      playerState.put(key + "_HAS_ROLLED", 0);
      playerState.put(key + "_ROADS_AVAILABLE", 15);
      playerState.put(key + "_SETTLEMENTS_AVAILABLE", 5);
      playerState.put(key + "_CITIES_AVAILABLE", 4);
      playerState.put(key + "_HAS_ROAD", 0);
      playerState.put(key + "_LONGEST_ROAD_LENGTH", 0);
      playerState.put(key + "_HAS_ARMY", 0);
      playerState.put(key + "_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 0);
      // Dev cards in hand
      playerState.put(key + "_KNIGHT_IN_HAND", 0);
      playerState.put(key + "_YEAR_OF_PLENTY_IN_HAND", 0);
      playerState.put(key + "_ROAD_BUILDING_IN_HAND", 0);
      playerState.put(key + "_MONOPOLY_IN_HAND", 0);
      playerState.put(key + "_VICTORY_POINT_IN_HAND", 0);
      // Played dev counters
      playerState.put(key + "_PLAYED_KNIGHT", 0);
      playerState.put(key + "_PLAYED_YEAR_OF_PLENTY", 0);
      playerState.put(key + "_PLAYED_ROAD_BUILDING", 0);
      playerState.put(key + "_PLAYED_MONOPOLY", 0);
      // Owned at start flags
      playerState.put(key + "_KNIGHT_OWNED_AT_START", 0);
      playerState.put(key + "_YEAR_OF_PLENTY_OWNED_AT_START", 0);
      playerState.put(key + "_ROAD_BUILDING_OWNED_AT_START", 0);
      playerState.put(key + "_MONOPOLY_OWNED_AT_START", 0);
      playerState.put(key + "_WOOD_IN_HAND", 0);
      playerState.put(key + "_BRICK_IN_HAND", 0);
      playerState.put(key + "_SHEEP_IN_HAND", 0);
      playerState.put(key + "_WHEAT_IN_HAND", 0);
      playerState.put(key + "_ORE_IN_HAND", 0);
    }
  }

  public Player currentPlayer() {
    return players.get(currentPlayerIndex);
  }

  public PlayerColor currentColor() {
    return colors.get(currentPlayerIndex);
  }

  public State copy() {
    State s = new State(this.players); // players are references; ok for read-only
    s.bank = Arrays.copyOf(this.bank, this.bank.length);
    s.playerState.clear();
    s.playerState.putAll(this.playerState);
    s.currentPlayerIndex = this.currentPlayerIndex;
    s.currentTurnIndex = this.currentTurnIndex;
    s.numTurns = this.numTurns;
    s.currentPrompt = this.currentPrompt;
    s.isInitialBuildPhase = this.isInitialBuildPhase;
    s.isDiscarding = this.isDiscarding;
    s.isMovingKnight = this.isMovingKnight;
    return s;
  }
}
