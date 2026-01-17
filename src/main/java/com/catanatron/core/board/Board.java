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

    public Set<Integer> buildableNodeIds(PlayerColor color, boolean initialPhase) {
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
            boolean touchesOurRoad = false;
            for (Integer neighbor : map.nodeNeighbors.getOrDefault(nodeId, Set.of())) {
                if (roads.getOrDefault(edgeKey(nodeId, neighbor), null) == color) {
                    touchesOurRoad = true; break;
                }
            }
            if (touchesOurRoad) candidates.add(nodeId);
        }
        return candidates;
    }

    public List<com.catanatron.core.model.Edge> buildableEdges(PlayerColor color) {
        List<com.catanatron.core.model.Edge> edges = new ArrayList<>();
        // Only edges adjacent to owned nodes OR extend from existing own roads
        Set<Integer> owned = ownedNodes(color);
        // Edges from owned nodes
        for (Integer a : owned) {
            for (Integer b : map.nodeNeighbors.getOrDefault(a, Set.of())) {
                long key = edgeKey(a,b);
                if (!roads.containsKey(key)) edges.add(new com.catanatron.core.model.Edge(Math.min(a,b), Math.max(a,b)));
            }
        }
        // Edges extending owned roads (one endpoint shared)
        for (Map.Entry<Long, PlayerColor> e : roads.entrySet()) {
            if (e.getValue() != color) continue;
            int a = (int)(e.getKey() >> 32);
            int b = (int)(e.getKey().longValue());
            for (Integer nb : map.nodeNeighbors.getOrDefault(a, Set.of())) {
                long key = edgeKey(a, nb);
                if (!roads.containsKey(key)) edges.add(new com.catanatron.core.model.Edge(Math.min(a,nb), Math.max(a,nb)));
            }
            for (Integer nb : map.nodeNeighbors.getOrDefault(b, Set.of())) {
                long key = edgeKey(b, nb);
                if (!roads.containsKey(key)) edges.add(new com.catanatron.core.model.Edge(Math.min(b,nb), Math.max(b,nb)));
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

    public void buildRoad(PlayerColor color, int a, int b) {
        long key = edgeKey(a,b);
        if (roads.containsKey(key)) throw new IllegalArgumentException("road exists");
        roads.put(key, color);
    }

    public Map.Entry<PlayerColor, BuildingType> buildingAt(int nodeId) {
        return buildings.get(nodeId);
    }

    private Set<Integer> ownedNodes(PlayerColor color) {
        Set<Integer> owned = new HashSet<>();
        for (Map.Entry<Integer, Map.Entry<PlayerColor, BuildingType>> e : buildings.entrySet()) {
            if (e.getValue().getKey() == color) owned.add(e.getKey());
        }
        return owned;
    }

    public Set<Integer> ownedSettlementNodes(PlayerColor color) {
        Set<Integer> owned = new HashSet<>();
        for (Map.Entry<Integer, Map.Entry<PlayerColor, BuildingType>> e : buildings.entrySet()) {
            if (e.getValue().getKey() == color && e.getValue().getValue() == BuildingType.SETTLEMENT) owned.add(e.getKey());
        }
        return owned;
    }

    private static long edgeKey(int a, int b) {
        int x = Math.min(a,b), y = Math.max(a,b);
        return (((long)x)<<32) | (y & 0xffffffffL);
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
            int a = (int)(e.getKey() >> 32);
            int b = (int)(e.getKey().longValue());
            adj.computeIfAbsent(a, k-> new ArrayList<>()).add(b);
            adj.computeIfAbsent(b, k-> new ArrayList<>()).add(a);
            nodes.add(a); nodes.add(b);
        }
        int best = 0;
        Set<Long> usedEdges = new HashSet<>();
        for (Integer start : nodes) {
            best = Math.max(best, dfsLongest(color, start, -1, adj, usedEdges));
        }
        return best;
    }

    private int dfsLongest(PlayerColor color, int node, int parent, Map<Integer, List<Integer>> adj, Set<Long> used) {
        // If an enemy building is on this node, cannot expand from here (but reaching here counts via the edge already used)
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
