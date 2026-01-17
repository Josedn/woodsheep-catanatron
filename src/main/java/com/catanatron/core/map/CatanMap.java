package com.catanatron.core.map;

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
        MapTemplate tpl = BaseTemplate.build();
        return fromTemplate(tpl);
    }

    public static CatanMap fromTemplate(MapTemplate tpl) {
        CatanMap m = new CatanMap();
        buildTiles(m, tpl);
        m.rebuildCaches();
        return m;
    }

    private static void buildTiles(CatanMap m, MapTemplate tpl) {
        int nodeAutoinc = 0;
        int landIndex = 0;
        int idAutoinc = 0;
        // Maintain insertion order for deterministic ids
        for (Map.Entry<Coordinate, Object> e : tpl.topology().entrySet()) {
            Coordinate c = e.getKey();
            Object kind = e.getValue();
            // Initialize node/edge maps for this tile
            EnumMap<NodeRef, Integer> nodes = new EnumMap<>(NodeRef.class);
            EnumMap<EdgeRef, Edge> edges = new EnumMap<>(EdgeRef.class);
            for (NodeRef nr : NodeRef.values()) nodes.put(nr, null);
            for (EdgeRef er : EdgeRef.values()) edges.put(er, null);

            // Share with neighbors if present
            for (Direction d : Direction.values()) {
                Coordinate nc = add(c, d);
                Tile neighbor = m.tiles.get(nc);
                if (neighbor == null) continue;
                if (neighbor instanceof LandTile lt) {
                    switch (d) {
                        case EAST -> {
                            nodes.put(NodeRef.NORTHEAST, lt.nodes.get(NodeRef.NORTHWEST));
                            nodes.put(NodeRef.SOUTHEAST, lt.nodes.get(NodeRef.SOUTHWEST));
                            edges.put(EdgeRef.EAST, lt.edges.get(EdgeRef.WEST));
                        }
                        case SOUTHEAST -> {
                            nodes.put(NodeRef.SOUTH, lt.nodes.get(NodeRef.NORTHWEST));
                            nodes.put(NodeRef.SOUTHEAST, lt.nodes.get(NodeRef.NORTH));
                            edges.put(EdgeRef.SOUTHEAST, lt.edges.get(EdgeRef.NORTHWEST));
                        }
                        case SOUTHWEST -> {
                            nodes.put(NodeRef.SOUTH, lt.nodes.get(NodeRef.NORTHEAST));
                            nodes.put(NodeRef.SOUTHWEST, lt.nodes.get(NodeRef.NORTH));
                            edges.put(EdgeRef.SOUTHWEST, lt.edges.get(EdgeRef.NORTHEAST));
                        }
                        case WEST -> {
                            nodes.put(NodeRef.NORTHWEST, lt.nodes.get(NodeRef.NORTHEAST));
                            nodes.put(NodeRef.SOUTHWEST, lt.nodes.get(NodeRef.SOUTHEAST));
                            edges.put(EdgeRef.WEST, lt.edges.get(EdgeRef.EAST));
                        }
                        case NORTHWEST -> {
                            nodes.put(NodeRef.NORTH, lt.nodes.get(NodeRef.SOUTHEAST));
                            nodes.put(NodeRef.NORTHWEST, lt.nodes.get(NodeRef.SOUTH));
                            edges.put(EdgeRef.NORTHWEST, lt.edges.get(EdgeRef.SOUTHEAST));
                        }
                        case NORTHEAST -> {
                            nodes.put(NodeRef.NORTH, lt.nodes.get(NodeRef.SOUTHWEST));
                            nodes.put(NodeRef.NORTHEAST, lt.nodes.get(NodeRef.SOUTH));
                            edges.put(EdgeRef.NORTHEAST, lt.edges.get(EdgeRef.SOUTHWEST));
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

            Tile t;
            if (kind == LandTile.class) {
                // assign resource/number in order, desert gets null/none
                Resource res = tpl.tileResources().get(landIndex);
                Integer num = (res == null ? null : tpl.numbers().get(landIndex - countDesertsBefore(tpl.tileResources(), landIndex)));
                t = new LandTile(idAutoinc, res, num, nodes, edges);
                m.landTiles.put(c, (LandTile) t);
                m.tilesById.put(idAutoinc, (LandTile) t);
                landIndex++;
            } else {
                t = new Water(nodes, edges);
            }
            m.tiles.put(c, t);
            idAutoinc++;
        }
    }

    private static int countDesertsBefore(List<Resource> tileResources, int idx) {
        int count = 0;
        for (int i = 0; i < idx; i++) if (tileResources.get(i) == null) count++;
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
            case EAST -> new Coordinate(c.q+1, c.r-1, c.s);
            case SOUTHEAST -> new Coordinate(c.q, c.r-1, c.s+1);
            case SOUTHWEST -> new Coordinate(c.q-1, c.r, c.s+1);
            case WEST -> new Coordinate(c.q-1, c.r+1, c.s);
            case NORTHWEST -> new Coordinate(c.q, c.r+1, c.s-1);
            case NORTHEAST -> new Coordinate(c.q+1, c.r, c.s-1);
        };
    }

    private void rebuildCaches() {
        // land nodes
        for (LandTile lt : landTiles.values()) {
            landNodes.addAll(lt.nodes.values());
            for (Edge e : lt.edges.values()) {
                int a = Math.min(e.a(), e.b());
                int b = Math.max(e.a(), e.b());
                long key = (((long)a)<<32) | (b & 0xffffffffL);
                edgeKeys.add(key);
                nodeNeighbors.computeIfAbsent(a, k-> new HashSet<>()).add(b);
                nodeNeighbors.computeIfAbsent(b, k-> new HashSet<>()).add(a);
            }
            tileNumberById.put(lt.id, lt.number);
            tileResourceById.put(lt.id, lt.resource);
            tileNodesById.put(lt.id, new HashSet<>(lt.nodes.values()));
        }
        // adjacent tiles by node
        for (LandTile lt : landTiles.values()) {
            for (Integer nodeId : lt.nodes.values()) {
                adjacentTiles.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(lt);
            }
        }
    }

    // ===== BASE template definition
    static class BaseTemplate {
        static MapTemplate build() {
            List<Integer> numbers = Arrays.asList(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12);
            List<Resource> ports = Arrays.asList(
                    Resource.WOOD, Resource.BRICK, Resource.SHEEP, Resource.WHEAT, Resource.ORE,
                    null, null, null, null
            );
            List<Resource> tiles = Arrays.asList(
                    Resource.WOOD, Resource.WOOD, Resource.WOOD, Resource.WOOD,
                    Resource.BRICK, Resource.BRICK, Resource.BRICK,
                    Resource.SHEEP, Resource.SHEEP, Resource.SHEEP, Resource.SHEEP,
                    Resource.WHEAT, Resource.WHEAT, Resource.WHEAT, Resource.WHEAT,
                    Resource.ORE, Resource.ORE, Resource.ORE,
                    null
            );
            Map<Coordinate, Object> topology = new LinkedHashMap<>();
            // center
            topology.put(new Coordinate(0,0,0), LandTile.class);
            // first ring
            topology.put(new Coordinate(1,-1,0), LandTile.class);
            topology.put(new Coordinate(0,-1,1), LandTile.class);
            topology.put(new Coordinate(-1,0,1), LandTile.class);
            topology.put(new Coordinate(-1,1,0), LandTile.class);
            topology.put(new Coordinate(0,1,-1), LandTile.class);
            topology.put(new Coordinate(1,0,-1), LandTile.class);
            // second ring
            topology.put(new Coordinate(2,-2,0), LandTile.class);
            topology.put(new Coordinate(1,-2,1), LandTile.class);
            topology.put(new Coordinate(0,-2,2), LandTile.class);
            topology.put(new Coordinate(-1,-1,2), LandTile.class);
            topology.put(new Coordinate(-2,0,2), LandTile.class);
            topology.put(new Coordinate(-2,1,1), LandTile.class);
            topology.put(new Coordinate(-2,2,0), LandTile.class);
            topology.put(new Coordinate(-1,2,-1), LandTile.class);
            topology.put(new Coordinate(0,2,-2), LandTile.class);
            topology.put(new Coordinate(1,1,-2), LandTile.class);
            topology.put(new Coordinate(2,0,-2), LandTile.class);
            topology.put(new Coordinate(2,-1,-1), LandTile.class);
            return new MapTemplate(numbers, ports, tiles, topology);
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
