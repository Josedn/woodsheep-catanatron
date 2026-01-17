package com.catanatron.core.model;

import java.util.Objects;

public final class Action<V> {
  public final PlayerColor color;
  public final ActionType type;
  public final V value;

  public Action(PlayerColor color, ActionType type, V value) {
    this.color = color;
    this.type = type;
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Action<?> that)) return false;
    return color == that.color && type == that.type && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(color, type, value);
  }

  @Override
  public String toString() {
    return "Action{" + color + ", " + type + ", " + value + '}';
  }
}
