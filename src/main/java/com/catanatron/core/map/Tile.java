package com.catanatron.core.map;

import com.catanatron.core.model.Resource;

import java.util.Map;

public sealed interface Tile permits LandTile, Port, Water {}

final class LandTile implements Tile {
    public final int id;
    public final Resource resource; // null => desert
    public final Integer number; // null if desert
    public final Map<NodeRef, Integer> nodes;
    public final Map<EdgeRef, Edge> edges;
    LandTile(int id, Resource resource, Integer number, Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges) {
        this.id = id; this.resource = resource; this.number = number; this.nodes = nodes; this.edges = edges;
    }
}

final class Port implements Tile {
    public final int id;
    public final Resource resource; // null => 3:1
    public final Direction direction;
    public final Map<NodeRef, Integer> nodes;
    public final Map<EdgeRef, Edge> edges;
    Port(int id, Resource resource, Direction direction, Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges) {
        this.id = id; this.resource = resource; this.direction = direction; this.nodes = nodes; this.edges = edges;
    }
}

final class Water implements Tile {
    public final Map<NodeRef, Integer> nodes;
    public final Map<EdgeRef, Edge> edges;
    Water(Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges) { this.nodes = nodes; this.edges = edges; }
}

record Edge(int a, int b) {}
