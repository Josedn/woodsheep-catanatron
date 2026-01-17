package com.catanatron.core.util;

import com.catanatron.core.model.Resource;

public final class Decks {
  private Decks() {}

  public static final int[] ROAD_COST = {1, 1, 0, 0, 0};
  public static final int[] SETTLEMENT_COST = {1, 1, 1, 1, 0};
  public static final int[] CITY_COST = {0, 0, 0, 2, 3};

  public static int[] startingResourceBank() {
    return new int[] {19, 19, 19, 19, 19};
  }

  public static int index(Resource r) {
    return switch (r) {
      case WOOD -> 0;
      case BRICK -> 1;
      case SHEEP -> 2;
      case WHEAT -> 3;
      case ORE -> 4;
    };
  }

  public static boolean contains(int[] freq, int[] need) {
    for (int i = 0; i < need.length; i++) if (freq[i] < need[i]) return false;
    return true;
  }

  public static int[] add(int[] a, int[] b) {
    int[] out = new int[a.length];
    for (int i = 0; i < a.length; i++) out[i] = a[i] + b[i];
    return out;
  }

  public static int[] sub(int[] a, int[] b) {
    int[] out = new int[a.length];
    for (int i = 0; i < a.length; i++) out[i] = a[i] - b[i];
    return out;
  }
}
