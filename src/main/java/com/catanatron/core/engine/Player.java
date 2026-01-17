package com.catanatron.core.engine;

import com.catanatron.core.model.Action;
import com.catanatron.core.model.PlayerColor;
import java.util.List;

public abstract class Player {
  public final PlayerColor color;
  public final boolean isBot;

  protected Player(PlayerColor color, boolean isBot) {
    this.color = color;
    this.isBot = isBot;
  }

  public abstract Action<?> decide(Game game, List<Action<?>> playable);

  public void resetState() {}

  @Override
  public String toString() {
    return getClass().getSimpleName() + ":" + color;
  }
}
