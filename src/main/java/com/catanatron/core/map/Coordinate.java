package com.catanatron.core.map;

import java.util.Objects;

// Cube coordinates (q, r, s) with q+r+s = 0
public final class Coordinate {
  public final int q, r, s;

  public Coordinate(int q, int r, int s) {
    if (q + r + s != 0) throw new IllegalArgumentException("q+r+s must be 0");
    this.q = q;
    this.r = r;
    this.s = s;
  }

  public Coordinate add(Coordinate o) {
    return new Coordinate(q + o.q, r + o.r, s + o.s);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Coordinate c)) return false;
    return q == c.q && r == c.r && s == c.s;
  }

  @Override
  public int hashCode() {
    return Objects.hash(q, r, s);
  }

  @Override
  public String toString() {
    return "(" + q + "," + r + "," + s + ")";
  }
}
