package com.catanatron.core.engine;

import com.catanatron.core.model.PlayerColor;

public class Demo {
  public static void main(String[] args) {
    var players = new java.util.ArrayList<Player>();
    players.add(new RandomPlayer(PlayerColor.RED));
    players.add(new RandomPlayer(PlayerColor.BLUE));
    players.add(new RandomPlayer(PlayerColor.ORANGE));
    players.add(new RandomPlayer(PlayerColor.WHITE));
    var game = new Game((java.util.List<Player>) players);
    for (int i = 0; i < 20; i++) {
      System.out.println(
          i
              + " Turn="
              + game.state.numTurns
              + " current="
              + game.state.currentColor()
              + " prompt="
              + game.state.currentPrompt
              + " actions("
              + game.playableActions.size()
              + ")="
              + game.playableActions);
      game.playTick();
    }
    System.out.println("OK");
  }
}
