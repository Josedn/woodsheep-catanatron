package com.catanatron.core.map;

import com.catanatron.core.map.tiles.Edge;
import com.catanatron.core.map.tiles.LandTile;
import com.catanatron.core.map.tiles.Tile;
import com.catanatron.core.map.tiles.Water;
import com.catanatron.core.model.Resource;
import java.util.*;

public class CatanMap {
  public final Map<Coordinate, Tile> tiles = new LinkedHashMap<>();
  public final Map<Coordinate, LandTile> landTiles = new LinkedHashMap<>();
  public final Map<Integer, List<LandTile>> adjacentTiles = new HashMap<>(); // nodeId -> tiles
  public final Map<Integer, LandTile> tilesById = new HashMap<>();

  public final Set<Integer> landNodes = new HashSet<>();
  public final Set<Long> edgeKeys = new HashSet<>(); // encoded (min,max)
  public final Map<Integer, Set<Integer>> nodeNeighbors = new HashMap<>();
  public final Map<Integer, Integer> tileNumberById = new HashMap<>();
  public final Map<Integer, com.catanatron.core.model.Resource> tileResourceById = new HashMap<>();
  public final Map<Integer, Set<Integer>> tileNodesById = new HashMap<>();

  public static CatanMap base() {
    return fromTemplate(MapTemplate.buildBaseTemplate());
  }

  private static CatanMap fromTemplate(MapTemplate mapTemplate) {
    CatanMap m = new CatanMap();
    buildTiles(m, mapTemplate);
    m.rebuildCaches();
    return m;
  }

  private static void buildTiles(CatanMap catanMap, MapTemplate mapTemplate) {
    int nodeAutoinc = 0;
    int landIndex = 0;
    int idAutoinc = 0;
    // Maintain insertion order for deterministic ids
    for (Map.Entry<Coordinate, Object> entry : mapTemplate.topology().entrySet()) {
      Coordinate coordinate = entry.getKey();
      Object kind = entry.getValue();
      // Initialize node/edge maps for this tile
      EnumMap<NodeRef, Integer> nodes = new EnumMap<>(NodeRef.class);
      EnumMap<EdgeRef, Edge> edges = new EnumMap<>(EdgeRef.class);

      for (NodeRef nodeRef : NodeRef.values()) {
        nodes.put(nodeRef, null);
      }
      for (EdgeRef edgeRef : EdgeRef.values()) {
        edges.put(edgeRef, null);
      }

      // Share with neighbors if present
      for (Direction direction : Direction.values()) {
        Coordinate nodeCoordinate = add(coordinate, direction);
        Tile neighbor = catanMap.tiles.get(nodeCoordinate);
        if (neighbor == null) {
          continue;
        }
        if (neighbor instanceof LandTile neighborTile) {
          switch (direction) {
            case EAST -> {
              nodes.put(NodeRef.NORTHEAST, neighborTile.nodes().get(NodeRef.NORTHWEST));
              nodes.put(NodeRef.SOUTHEAST, neighborTile.nodes().get(NodeRef.SOUTHWEST));
              edges.put(EdgeRef.EAST, neighborTile.edges().get(EdgeRef.WEST));
            }
            case SOUTHEAST -> {
              nodes.put(NodeRef.SOUTH, neighborTile.nodes().get(NodeRef.NORTHWEST));
              nodes.put(NodeRef.SOUTHEAST, neighborTile.nodes().get(NodeRef.NORTH));
              edges.put(EdgeRef.SOUTHEAST, neighborTile.edges().get(EdgeRef.NORTHWEST));
            }
            case SOUTHWEST -> {
              nodes.put(NodeRef.SOUTH, neighborTile.nodes().get(NodeRef.NORTHEAST));
              nodes.put(NodeRef.SOUTHWEST, neighborTile.nodes().get(NodeRef.NORTH));
              edges.put(EdgeRef.SOUTHWEST, neighborTile.edges().get(EdgeRef.NORTHEAST));
            }
            case WEST -> {
              nodes.put(NodeRef.NORTHWEST, neighborTile.nodes().get(NodeRef.NORTHEAST));
              nodes.put(NodeRef.SOUTHWEST, neighborTile.nodes().get(NodeRef.SOUTHEAST));
              edges.put(EdgeRef.WEST, neighborTile.edges().get(EdgeRef.EAST));
            }
            case NORTHWEST -> {
              nodes.put(NodeRef.NORTH, neighborTile.nodes().get(NodeRef.SOUTHEAST));
              nodes.put(NodeRef.NORTHWEST, neighborTile.nodes().get(NodeRef.SOUTH));
              edges.put(EdgeRef.NORTHWEST, neighborTile.edges().get(EdgeRef.SOUTHEAST));
            }
            case NORTHEAST -> {
              nodes.put(NodeRef.NORTH, neighborTile.nodes().get(NodeRef.SOUTHWEST));
              nodes.put(NodeRef.NORTHEAST, neighborTile.nodes().get(NodeRef.SOUTH));
              edges.put(EdgeRef.NORTHEAST, neighborTile.edges().get(EdgeRef.SOUTHWEST));
            }
          }
        }
      }

      // Create new nodes/edges for unset
      for (NodeRef nr : NodeRef.values()) {
        if (nodes.get(nr) == null) {
          nodes.put(nr, nodeAutoinc++);
        }
      }
      for (EdgeRef er : EdgeRef.values()) {
        if (edges.get(er) == null) {
          int a = edgeA(er, nodes);
          int b = edgeB(er, nodes);
          edges.put(er, new Edge(a, b));
        }
      }

      // create and save tile
      Tile tile;
      if (kind == LandTile.class) {
        // assign resource/number in order, desert gets null/none
        Resource resource = mapTemplate.tileResources().get(landIndex);
        Integer number =
            (resource == null
                ? null
                : mapTemplate
                    .numbers()
                    .get(landIndex - countDesertsBefore(mapTemplate.tileResources(), landIndex)));
        tile = new LandTile(idAutoinc, resource, number, nodes, edges);
        catanMap.landTiles.put(coordinate, (LandTile) tile);
        catanMap.tilesById.put(idAutoinc, (LandTile) tile);
        landIndex++;
      } else {
        tile = new Water(nodes, edges);
      }
      catanMap.tiles.put(coordinate, tile);
      idAutoinc++;
    }
  }

  private static int countDesertsBefore(List<Resource> tileResources, int idx) {
    int count = 0;
    for (int i = 0; i < idx; i++) {
      if (tileResources.get(i) == null) {
        count++;
      }
    }
    return count;
  }

  private static int edgeA(EdgeRef er, EnumMap<NodeRef, Integer> n) {
    return switch (er) {
      case EAST -> n.get(NodeRef.NORTHEAST);
      case SOUTHEAST -> n.get(NodeRef.SOUTHEAST);
      case SOUTHWEST -> n.get(NodeRef.SOUTH);
      case WEST -> n.get(NodeRef.SOUTHWEST);
      case NORTHWEST -> n.get(NodeRef.NORTHWEST);
      case NORTHEAST -> n.get(NodeRef.NORTH);
    };
  }

  private static int edgeB(EdgeRef er, EnumMap<NodeRef, Integer> n) {
    return switch (er) {
      case EAST -> n.get(NodeRef.SOUTHEAST);
      case SOUTHEAST -> n.get(NodeRef.SOUTH);
      case SOUTHWEST -> n.get(NodeRef.SOUTHWEST);
      case WEST -> n.get(NodeRef.NORTHWEST);
      case NORTHWEST -> n.get(NodeRef.NORTH);
      case NORTHEAST -> n.get(NodeRef.NORTHEAST);
    };
  }

  private static Coordinate add(Coordinate c, Direction d) {
    return switch (d) {
      case EAST -> new Coordinate(c.q + 1, c.r - 1, c.s);
      case SOUTHEAST -> new Coordinate(c.q, c.r - 1, c.s + 1);
      case SOUTHWEST -> new Coordinate(c.q - 1, c.r, c.s + 1);
      case WEST -> new Coordinate(c.q - 1, c.r + 1, c.s);
      case NORTHWEST -> new Coordinate(c.q, c.r + 1, c.s - 1);
      case NORTHEAST -> new Coordinate(c.q + 1, c.r, c.s - 1);
    };
  }

  private void rebuildCaches() {
    // land nodes
    for (LandTile lt : landTiles.values()) {
      landNodes.addAll(lt.nodes().values());
      for (Edge e : lt.edges().values()) {
        int a = Math.min(e.a(), e.b());
        int b = Math.max(e.a(), e.b());
        long key = (((long) a) << 32) | (b & 0xffffffffL);
        edgeKeys.add(key);
        nodeNeighbors.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        nodeNeighbors.computeIfAbsent(b, k -> new HashSet<>()).add(a);
      }
      tileNumberById.put(lt.id(), lt.number());
      tileResourceById.put(lt.id(), lt.resource());
      tileNodesById.put(lt.id(), new HashSet<>(lt.nodes().values()));
    }
    // adjacent tiles by node
    for (LandTile lt : landTiles.values()) {
      for (Integer nodeId : lt.nodes().values()) {
        adjacentTiles.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(lt);
      }
    }
  }

  public Set<Integer> getTileIdsByNumber(int number) {
    Set<Integer> ids = new HashSet<>();
    for (Map.Entry<Integer, Integer> e : tileNumberById.entrySet()) {
      if (e.getValue() != null && e.getValue() == number) ids.add(e.getKey());
    }
    return ids;
  }

  public com.catanatron.core.model.Resource getTileResource(int tileId) {
    return tileResourceById.get(tileId);
  }

  public Set<Integer> getTileNodes(int tileId) {
    return tileNodesById.getOrDefault(tileId, Set.of());
  }

  public int getDesertTileId() {
    for (Map.Entry<Integer, com.catanatron.core.model.Resource> e : tileResourceById.entrySet()) {
      if (e.getValue() == null) return e.getKey();
    }
    // fallback (shouldn't happen in BASE)
    return 0;
  }
}
