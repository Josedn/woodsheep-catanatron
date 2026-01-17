package com.catanatron.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.catanatron.core.model.PlayerColor;
import org.junit.jupiter.api.Test;

public class BasicRulesTest {
  @Test
  public void distanceRuleBlocksNeighbors() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    players.add(new RandomPlayer(PlayerColor.BLUE));
    players.add(new RandomPlayer(PlayerColor.ORANGE));
    players.add(new RandomPlayer(PlayerColor.WHITE));
    var game = new Game(players);

    // RED initial settlement on smallest node id
    int node =
        game.state.board.buildableNodeIds(PlayerColor.RED, true).stream()
            .min(Integer::compare)
            .orElseThrow();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_SETTLEMENT, node));
    // Road (skip specifics)
    // Initial road must connect to last settlement; pick edge that touches node
    com.catanatron.core.model.Edge edge =
        game.state.board.buildableEdges(PlayerColor.RED).stream()
            .filter(e -> e.a() == node || e.b() == node)
            .findFirst()
            .orElseThrow();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_ROAD, edge));

    // BLUE cannot place on node or its neighbors
    var blueNodes = game.state.board.buildableNodeIds(PlayerColor.BLUE, true);
    assertFalse(blueNodes.contains(node), "distance-1 violated: same node allowed");
    for (Integer nb : game.state.map.nodeNeighbors.get(node)) {
      assertFalse(blueNodes.contains(nb), "distance-1 violated: neighbor allowed");
    }
  }

  @Test
  public void roadMustConnectToOwned() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    players.add(new RandomPlayer(PlayerColor.BLUE));
    players.add(new RandomPlayer(PlayerColor.ORANGE));
    players.add(new RandomPlayer(PlayerColor.WHITE));
    var game = new Game(players);

    // RED place a settlement then a road from it
    int node = game.state.board.buildableNodeIds(PlayerColor.RED, true).iterator().next();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_SETTLEMENT, node));
    com.catanatron.core.model.Edge legalEdge =
        game.state.board.buildableEdges(PlayerColor.RED).get(0);
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_ROAD, legalEdge));

    // For BLUE, pick any map edge (BLUE owns nothing yet)
    int anyNode = game.state.map.nodeNeighbors.keySet().iterator().next();
    int neighbor = game.state.map.nodeNeighbors.get(anyNode).iterator().next();
    com.catanatron.core.model.Edge someEdge =
        new com.catanatron.core.model.Edge(
            Math.min(anyNode, neighbor), Math.max(anyNode, neighbor));
    // After initial phase ends, ensure connectivity check still enforced when paying
    game.state.isInitialBuildPhase = false;
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    // Force a roll outcome to skip discards/robber and stay in play
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.BLUE, com.catanatron.core.model.ActionType.ROLL, new int[] {3, 4}));
    game.state.playerState.put("P1_WOOD_IN_HAND", 1);
    game.state.playerState.put("P1_BRICK_IN_HAND", 1);
    boolean thrown = false;
    try {
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.BLUE, com.catanatron.core.model.ActionType.BUILD_ROAD, someEdge));
    } catch (RuntimeException ex) {
      thrown = true;
    }
    assertTrue(thrown, "road built without connectivity");
  }

  @Test
  public void rollPaysOutResourcesAndRobberBlocks() {
    // Single-player setup for simplicity
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    var game = new Game(players);

    // Choose a tile with number 8 and take one of its nodes
    int tileId = game.state.map.getTileIdsByNumber(8).iterator().next();
    int nodeId = game.state.map.getTileNodes(tileId).iterator().next();
    var res = game.state.map.getTileResource(tileId);

    // Place initial settlement at that node and an initial road touching it
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_SETTLEMENT, nodeId));
    com.catanatron.core.model.Edge edge =
        game.state.board.buildableEdges(PlayerColor.RED).stream()
            .filter(e -> e.a() == nodeId || e.b() == nodeId)
            .findFirst()
            .orElseThrow();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_ROAD, edge));

    // End initial phase
    game.state.isInitialBuildPhase = false;
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;

    // Force roll of 8 and verify payout equals number of adjacent tiles with 8
    int before = getRes(game, 0, res.name());
    long contributing =
        game.state.map.getTileIdsByNumber(8).stream()
            .filter(id -> game.state.map.getTileNodes(id).contains(nodeId))
            .count();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.ROLL, new int[] {3, 5}));
    int after = getRes(game, 0, res.name());
    assertEquals(
        before + (int) contributing, after, "settlement yield should match adjacent 8-tiles");

    // End turn so we can continue; directly enter MOVE_ROBBER for this test
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.END_TURN, null));
    game.state.currentPrompt = ActionPrompt.MOVE_ROBBER;
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.MOVE_ROBBER, tileId));
    int before2 = getRes(game, 0, res.name());
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.ROLL, new int[] {3, 5}));
    int after2 = getRes(game, 0, res.name());
    // Robber blocks exactly 1 of the contributing tiles (the moved-to tile)
    assertEquals(
        before2 + (int) contributing - 1, after2, "robber should block payouts on that tile only");
  }

  @Test
  public void robberStealsFromAdjacentVictim() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    players.add(new RandomPlayer(PlayerColor.BLUE));
    var game = new Game(players);

    // Place RED initial settlement/road first to progress prompts
    int redNode = game.state.board.buildableNodeIds(PlayerColor.RED, true).iterator().next();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_SETTLEMENT, redNode));
    com.catanatron.core.model.Edge redEdge =
        game.state.board.buildableEdges(PlayerColor.RED).get(0);
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_ROAD, redEdge));

    // Now pick a valid BLUE node (post-RED) and a tile adjacent to it
    var blueAvail = game.state.board.buildableNodeIds(PlayerColor.BLUE, true);
    Integer nodeId = blueAvail.iterator().next();
    Integer tileId = null;
    for (Integer t : game.state.map.tilesById.keySet()) {
      if (game.state.map.getTileNodes(t).contains(nodeId)) {
        tileId = t;
        break;
      }
    }
    assertNotNull(tileId);

    // BLUE initial settlement at nodeId on chosen tile and a road
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.BLUE, com.catanatron.core.model.ActionType.BUILD_SETTLEMENT, nodeId));
    com.catanatron.core.model.Edge blueEdge =
        game.state.board.buildableEdges(PlayerColor.BLUE).stream()
            .filter(e -> e.a() == nodeId || e.b() == nodeId)
            .findFirst()
            .orElseThrow();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.BLUE, com.catanatron.core.model.ActionType.BUILD_ROAD, blueEdge));

    // End initial phase and move to PLAY_TURN
    game.state.isInitialBuildPhase = false;
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;

    // Give BLUE some resources to steal
    game.state.playerState.put("P1_WOOD_IN_HAND", 1);
    game.state.playerState.put("P1_BRICK_IN_HAND", 1);
    int redBefore = getRes(game, 0, "WOOD") + getRes(game, 0, "BRICK");
    int blueBefore = getRes(game, 1, "WOOD") + getRes(game, 1, "BRICK");

    // Trigger robber move by rolling 7, then move robber to tileId
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.ROLL, new int[] {3, 4}));
    // Now in MOVE_ROBBER
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.MOVE_ROBBER, tileId));

    int redAfter = getRes(game, 0, "WOOD") + getRes(game, 0, "BRICK");
    int blueAfter = getRes(game, 1, "WOOD") + getRes(game, 1, "BRICK");
    assertEquals(redBefore + 1, redAfter, "robber should steal one resource to RED");
    assertEquals(blueBefore - 1, blueAfter, "robber should remove one resource from BLUE");
  }

  @Test
  public void yearOfPlentyGivesResourceAndKnightSetsMoveRobber() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    var game = new Game(players);

    // Seed dev hand so it is playable this turn: mark owned-at-start and in-hand
    game.state.playerState.put("P0_YEAR_OF_PLENTY_IN_HAND", 1);
    game.state.playerState.put("P0_YEAR_OF_PLENTY_OWNED_AT_START", 1);
    int before = getRes(game, 0, "WOOD");
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    game.state.playerState.put("P0_HAS_ROLLED", 1); // after rolling
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED,
            com.catanatron.core.model.ActionType.PLAY_YEAR_OF_PLENTY,
            new String[] {"WOOD"}));
    int after = getRes(game, 0, "WOOD");
    assertEquals(before + 1, after, "YOP should grant 1 wood");

    // Knight
    game.state.playerState.put("P0_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 0);
    game.state.playerState.put("P0_KNIGHT_IN_HAND", 1);
    game.state.playerState.put("P0_KNIGHT_OWNED_AT_START", 1);
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.PLAY_KNIGHT_CARD, null));
    assertEquals(
        ActionPrompt.MOVE_ROBBER, game.state.currentPrompt, "Knight should set MOVE_ROBBER");
  }

  @Test
  public void yearOfPlentyTwoCardsGrantsBoth() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    var game = new Game(players);

    // Seed YOP in-hand and owned at start; ensure playable state
    game.state.playerState.put("P0_YEAR_OF_PLENTY_IN_HAND", 1);
    game.state.playerState.put("P0_YEAR_OF_PLENTY_OWNED_AT_START", 1);
    game.state.playerState.put("P0_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 0);
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    game.state.playerState.put("P0_HAS_ROLLED", 1);

    int woodBefore = getRes(game, 0, "WOOD");
    int brickBefore = getRes(game, 0, "BRICK");
    // Play Year of Plenty with two different resources
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED,
            com.catanatron.core.model.ActionType.PLAY_YEAR_OF_PLENTY,
            new String[] {"WOOD", "BRICK"}));
    int woodAfter = getRes(game, 0, "WOOD");
    int brickAfter = getRes(game, 0, "BRICK");
    assertEquals(woodBefore + 1, woodAfter);
    assertEquals(brickBefore + 1, brickAfter);
  }

  @Test
  public void yearOfPlentyTwoOfSameResourceGrantsTwo() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    var game = new Game(players);

    game.state.playerState.put("P0_YEAR_OF_PLENTY_IN_HAND", 1);
    game.state.playerState.put("P0_YEAR_OF_PLENTY_OWNED_AT_START", 1);
    game.state.playerState.put("P0_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 0);
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    game.state.playerState.put("P0_HAS_ROLLED", 1);

    int wheatBefore = getRes(game, 0, "WHEAT");
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED,
            com.catanatron.core.model.ActionType.PLAY_YEAR_OF_PLENTY,
            new String[] {"WHEAT", "WHEAT"}));
    int wheatAfter = getRes(game, 0, "WHEAT");
    assertEquals(wheatBefore + 2, wheatAfter);
  }

  @Test
  public void cannotPlaySecondDevCardInSameTurn() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    var game = new Game(players);

    // Seed two different devs in hand as owned at start
    game.state.playerState.put("P0_YEAR_OF_PLENTY_IN_HAND", 1);
    game.state.playerState.put("P0_YEAR_OF_PLENTY_OWNED_AT_START", 1);
    game.state.playerState.put("P0_KNIGHT_IN_HAND", 1);
    game.state.playerState.put("P0_KNIGHT_OWNED_AT_START", 1);
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    game.state.playerState.put("P0_HAS_ROLLED", 1);

    // Play first dev
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED,
            com.catanatron.core.model.ActionType.PLAY_YEAR_OF_PLENTY,
            new String[] {"WOOD"}));

    // Attempt to play second dev same turn should fail (not offered / not playable)
    boolean thrown = false;
    try {
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.RED, com.catanatron.core.model.ActionType.PLAY_KNIGHT_CARD, null));
    } catch (RuntimeException ex) {
      thrown = true;
    }
    assertTrue(thrown, "Second dev card should not be playable in same turn");
  }

  @Test
  public void roadBuildingPlacesTwoFreeRoads() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    var game = new Game(players);

    // Place initial settlement to have adjacent road options
    int node = game.state.board.buildableNodeIds(PlayerColor.RED, true).iterator().next();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_SETTLEMENT, node));
    com.catanatron.core.model.Edge e1 =
        game.state.board.buildableEdges(PlayerColor.RED).stream()
            .filter(e -> e.a() == node || e.b() == node)
            .findFirst()
            .orElseThrow();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_ROAD, e1));

    // End initial phase
    game.state.isInitialBuildPhase = false;
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    game.state.playerState.put("P0_HAS_ROLLED", 1);

    int roadsBefore = game.state.playerState.get("P0_ROADS_AVAILABLE");
    // Seed Road Building in hand and playable
    game.state.playerState.put("P0_ROAD_BUILDING_IN_HAND", 1);
    game.state.playerState.put("P0_ROAD_BUILDING_OWNED_AT_START", 1);
    // Play Road Building
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.PLAY_ROAD_BUILDING, null));
    assertTrue(game.state.isRoadBuilding);
    // Place two free roads
    com.catanatron.core.model.Edge r1 = game.state.board.buildableEdges(PlayerColor.RED).get(0);
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_ROAD, r1));
    com.catanatron.core.model.Edge r2 = game.state.board.buildableEdges(PlayerColor.RED).get(0);
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUILD_ROAD, r2));
    assertFalse(game.state.isRoadBuilding);
    int roadsAfter = game.state.playerState.get("P0_ROADS_AVAILABLE");
    assertEquals(roadsBefore - 2, roadsAfter, "should have consumed two road pieces");
  }

  @Test
  public void buyDevelopmentCardConsumesResourcesAndAddsToHand() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    var game = new Game(players);

    game.state.isInitialBuildPhase = false;
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    game.state.playerState.put("P0_HAS_ROLLED", 1);
    // Fund resources to buy
    game.state.playerState.put("P0_SHEEP_IN_HAND", 1);
    game.state.playerState.put("P0_WHEAT_IN_HAND", 1);
    game.state.playerState.put("P0_ORE_IN_HAND", 1);
    int deckBefore = game.state.developmentDeck.size();
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.BUY_DEVELOPMENT_CARD, null));
    int deckAfter = game.state.developmentDeck.size();
    assertEquals(deckBefore - 1, deckAfter);
    // Ensure resources paid
    assertEquals(0, game.state.playerState.get("P0_SHEEP_IN_HAND"));
    assertEquals(0, game.state.playerState.get("P0_WHEAT_IN_HAND"));
    assertEquals(0, game.state.playerState.get("P0_ORE_IN_HAND"));
    // One of the "_IN_HAND" counters should have increased by 1
    int inHandTotal =
        game.state.playerState.get("P0_KNIGHT_IN_HAND")
            + game.state.playerState.get("P0_YEAR_OF_PLENTY_IN_HAND")
            + game.state.playerState.get("P0_ROAD_BUILDING_IN_HAND")
            + game.state.playerState.get("P0_MONOPOLY_IN_HAND")
            + game.state.playerState.get("P0_VICTORY_POINT_IN_HAND");
    assertEquals(1, inHandTotal);
  }

  @Test
  public void monopolyTransfersAllOfResourceFromOpponents() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    players.add(new RandomPlayer(PlayerColor.BLUE));
    var game = new Game(players);

    // Make BLUE hold some WOOD and BRICK
    game.state.playerState.put("P1_WOOD_IN_HAND", 3);
    game.state.playerState.put("P1_BRICK_IN_HAND", 2);
    // Seed MONOPOLY for RED and mark playable
    game.state.playerState.put("P0_MONOPOLY_IN_HAND", 1);
    game.state.playerState.put("P0_MONOPOLY_OWNED_AT_START", 1);
    game.state.currentPrompt = ActionPrompt.PLAY_TURN;
    game.state.playerState.put("P0_HAS_ROLLED", 1);

    int redWoodBefore = game.state.playerState.get("P0_WOOD_IN_HAND");
    int blueWoodBefore = game.state.playerState.get("P1_WOOD_IN_HAND");
    // Play Monopoly: WOOD
    game.execute(
        new com.catanatron.core.model.Action<>(
            PlayerColor.RED, com.catanatron.core.model.ActionType.PLAY_MONOPOLY, "WOOD"));
    int redWoodAfter = game.state.playerState.get("P0_WOOD_IN_HAND");
    int blueWoodAfter = game.state.playerState.get("P1_WOOD_IN_HAND");
    assertEquals(redWoodBefore + blueWoodBefore, redWoodAfter);
    assertEquals(0, blueWoodAfter);
  }

  @Test
  public void largestArmyAwardsAndSwitchesOnLeadershipChange() {
    java.util.ArrayList<Player> players = new java.util.ArrayList<>();
    players.add(new RandomPlayer(PlayerColor.RED));
    players.add(new RandomPlayer(PlayerColor.BLUE));
    var game = new Game(players);

    // Seed RED with 3 Knights and play across 3 turns
    game.state.isInitialBuildPhase = false;
    for (int i = 0; i < 3; i++) {
      game.state.currentPlayerIndex = 0;
      game.state.currentTurnIndex = 0;
      game.state.currentPrompt = ActionPrompt.PLAY_TURN;
      // Ensure dev timing and in-hand; mark owned-at-start for playability this turn
      game.state.playerState.put(
          "P0_KNIGHT_IN_HAND", game.state.playerState.get("P0_KNIGHT_IN_HAND") + 1);
      game.state.playerState.put("P0_KNIGHT_OWNED_AT_START", 1);
      game.state.playerState.put("P0_HAS_ROLLED", 1);
      // ensure HAS_PLAYED reset for this test iteration
      game.state.playerState.put("P0_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 0);
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.RED, com.catanatron.core.model.ActionType.PLAY_KNIGHT_CARD, null));
      // Move robber somewhere valid
      Integer tileId = game.state.map.tilesById.keySet().iterator().next();
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.RED, com.catanatron.core.model.ActionType.MOVE_ROBBER, tileId));
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.RED, com.catanatron.core.model.ActionType.END_TURN, null));
    }
    assertEquals(1, game.state.playerState.get("P0_HAS_ARMY"));
    assertTrue(game.state.playerState.get("P0_VICTORY_POINTS") >= 2);

    // Now BLUE surpasses with 4 Knights
    for (int i = 0; i < 4; i++) {
      game.state.currentPlayerIndex = 1;
      game.state.currentTurnIndex = 1;
      game.state.currentPrompt = ActionPrompt.PLAY_TURN;
      game.state.playerState.put(
          "P1_KNIGHT_IN_HAND", game.state.playerState.get("P1_KNIGHT_IN_HAND") + 1);
      game.state.playerState.put("P1_KNIGHT_OWNED_AT_START", 1);
      game.state.playerState.put("P1_HAS_ROLLED", 1);
      game.state.playerState.put("P1_HAS_PLAYED_DEVELOPMENT_CARD_IN_TURN", 0);
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.BLUE, com.catanatron.core.model.ActionType.PLAY_KNIGHT_CARD, null));
      Integer tileId = game.state.map.tilesById.keySet().iterator().next();
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.BLUE, com.catanatron.core.model.ActionType.MOVE_ROBBER, tileId));
      game.execute(
          new com.catanatron.core.model.Action<>(
              PlayerColor.BLUE, com.catanatron.core.model.ActionType.END_TURN, null));
    }
    assertEquals(1, game.state.playerState.get("P1_HAS_ARMY"));
    assertEquals(0, game.state.playerState.get("P0_HAS_ARMY"));
  }

  private int getRes(Game game, int playerIndex, String resName) {
    return game.state.playerState.get("P" + playerIndex + "_" + resName + "_IN_HAND");
  }
}
