package com.catanatron.core.board;

import com.catanatron.core.map.CatanMap;
import com.catanatron.core.model.BuildingType;
import com.catanatron.core.model.PlayerColor;
import java.util.*;

// Board backed by CatanMap: basic buildability and placement
public class Board {
  private final CatanMap map;
  // nodeId -> (color, building)
  private final Map<Integer, Map.Entry<PlayerColor, BuildingType>> buildings = new HashMap<>();
  // edge (min,max) -> color
  private final Map<Long, PlayerColor> roads = new HashMap<>();
  private final Set<Integer> blockedNodes = new HashSet<>(); // distance-1 rule

  public Board(CatanMap map) {
    this.map = map;
  }

  public Set<Integer> buildableNodeIds(PlayerColor playerColor, boolean initialPhase) {
    if (initialPhase) {
      // any unblocked, empty land node
      Set<Integer> out = new HashSet<>(map.landNodes);
      out.removeAll(blockedNodes);
      out.removeAll(buildings.keySet());
      return out;
    }
    // Connected to existing road or building, still respecting distance-1
    Set<Integer> candidates = new HashSet<>();
    for (Integer nodeId : map.landNodes) {
      if (blockedNodes.contains(nodeId) || buildings.containsKey(nodeId)) continue;
      // must touch at least one of our roads
      boolean touchesOwnedRoad = false;
      for (Integer neighbor : map.nodeNeighbors.getOrDefault(nodeId, Set.of())) {
        if (roads.getOrDefault(edgeKey(nodeId, neighbor), null) == playerColor) {
          touchesOwnedRoad = true;
          break;
        }
      }
      if (touchesOwnedRoad) candidates.add(nodeId);
    }
    return candidates;
  }

  public List<com.catanatron.core.model.Edge> buildableEdges(PlayerColor playerColor) {
    List<com.catanatron.core.model.Edge> edges = new ArrayList<>();
    // Only edges adjacent to owned nodes OR extend from existing own roads
    Set<Integer> ownedNodes = ownedNodes(playerColor);
    // Edges from owned nodes
    for (Integer nodeA : ownedNodes) {
      for (Integer nodeB : map.nodeNeighbors.getOrDefault(nodeA, Set.of())) {
        long key = edgeKey(nodeA, nodeB);
        if (!roads.containsKey(key))
          edges.add(
              new com.catanatron.core.model.Edge(Math.min(nodeA, nodeB), Math.max(nodeA, nodeB)));
      }
    }
    // Edges extending owned roads (one endpoint shared)
    for (Map.Entry<Long, PlayerColor> roadEntry : roads.entrySet()) {
      if (roadEntry.getValue() != playerColor) continue;
      int nodeA = (int) (roadEntry.getKey() >> 32);
      int nodeB = (int) (roadEntry.getKey().longValue());
      for (Integer neighborOfA : map.nodeNeighbors.getOrDefault(nodeA, Set.of())) {
        long key = edgeKey(nodeA, neighborOfA);
        if (!roads.containsKey(key))
          edges.add(
              new com.catanatron.core.model.Edge(
                  Math.min(nodeA, neighborOfA), Math.max(nodeA, neighborOfA)));
      }
      for (Integer neighborOfB : map.nodeNeighbors.getOrDefault(nodeB, Set.of())) {
        long key = edgeKey(nodeB, neighborOfB);
        if (!roads.containsKey(key))
          edges.add(
              new com.catanatron.core.model.Edge(
                  Math.min(nodeB, neighborOfB), Math.max(nodeB, neighborOfB)));
      }
    }
    return edges;
  }

  public void buildSettlement(PlayerColor color, int nodeId) {
    if (buildings.containsKey(nodeId)) throw new IllegalArgumentException("occupied");
    buildings.put(nodeId, Map.entry(color, BuildingType.SETTLEMENT));
    // distance-1 rule: block neighbors
    blockedNodes.add(nodeId);
    for (Integer n : map.nodeNeighbors.getOrDefault(nodeId, Set.of())) blockedNodes.add(n);
  }

  public void buildCity(PlayerColor color, int nodeId) {
    var entry = buildings.get(nodeId);
    if (entry == null || entry.getKey() != color || entry.getValue() != BuildingType.SETTLEMENT) {
      throw new IllegalArgumentException("no player settlement to upgrade");
    }
    buildings.put(nodeId, Map.entry(color, BuildingType.CITY));
  }

  public void buildRoad(PlayerColor color, int nodeA, int nodeB) {
    long key = edgeKey(nodeA, nodeB);
    if (roads.containsKey(key)) throw new IllegalArgumentException("road exists");
    roads.put(key, color);
  }

  public Map.Entry<PlayerColor, BuildingType> buildingAt(int nodeId) {
    return buildings.get(nodeId);
  }

  private Set<Integer> ownedNodes(PlayerColor color) {
    Set<Integer> nodesOwned = new HashSet<>();
    for (Map.Entry<Integer, Map.Entry<PlayerColor, BuildingType>> buildingEntry :
        buildings.entrySet()) {
      if (buildingEntry.getValue().getKey() == color) nodesOwned.add(buildingEntry.getKey());
    }
    return nodesOwned;
  }

  public Set<Integer> ownedSettlementNodes(PlayerColor color) {
    Set<Integer> owned = new HashSet<>();
    for (Map.Entry<Integer, Map.Entry<PlayerColor, BuildingType>> e : buildings.entrySet()) {
      if (e.getValue().getKey() == color && e.getValue().getValue() == BuildingType.SETTLEMENT)
        owned.add(e.getKey());
    }
    return owned;
  }

  private static long edgeKey(int nodeA, int nodeB) {
    int min = Math.min(nodeA, nodeB), max = Math.max(nodeA, nodeB);
    return (((long) min) << 32) | (max & 0xffffffffL);
  }

  private boolean isEnemyNode(int nodeId, PlayerColor color) {
    var entry = buildings.get(nodeId);
    return entry != null && entry.getKey() != color;
  }

  public int longestRoadLength(PlayerColor color) {
    // Build adjacency for this player's roads
    Map<Integer, List<Integer>> adj = new HashMap<>();
    Set<Integer> nodes = new HashSet<>();
    for (Map.Entry<Long, PlayerColor> e : roads.entrySet()) {
      if (e.getValue() != color) continue;
      int a = (int) (e.getKey() >> 32);
      int b = (int) (e.getKey().longValue());
      adj.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
      adj.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
      nodes.add(a);
      nodes.add(b);
    }
    int best = 0;
    Set<Long> usedEdges = new HashSet<>();
    for (Integer start : nodes) {
      best = Math.max(best, dfsLongest(color, start, -1, adj, usedEdges));
    }
    return best;
  }

  private int dfsLongest(
      PlayerColor color, int node, int parent, Map<Integer, List<Integer>> adj, Set<Long> used) {
    // If an enemy building is on this node, cannot expand from here (but reaching here counts via
    // the edge already used)
    if (isEnemyNode(node, color)) return 0;
    int best = 0;
    for (Integer nb : adj.getOrDefault(node, List.of())) {
      if (nb == parent) {
        // We cannot rely only on parent because graph can have cycles; use edge-used set
      }
      long key = edgeKey(node, nb);
      if (used.contains(key)) continue;
      used.add(key);
      int candidate = 1 + dfsLongest(color, nb, node, adj, used);
      best = Math.max(best, candidate);
      used.remove(key);
    }
    return best;
  }
}
