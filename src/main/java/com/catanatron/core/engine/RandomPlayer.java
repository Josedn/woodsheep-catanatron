package com.catanatron.core.engine;

import com.catanatron.core.model.Action;
import java.util.List;
import java.util.Random;

public class RandomPlayer extends Player {
  private final Random rng = new Random();

  public RandomPlayer(com.catanatron.core.model.PlayerColor color) {
    super(color, true);
  }

  @Override
  public Action<?> decide(Game game, List<Action<?>> playable) {
    return playable.get(rng.nextInt(playable.size()));
  }
}
