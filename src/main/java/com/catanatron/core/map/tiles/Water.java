package com.catanatron.core.map.tiles;

import com.catanatron.core.map.EdgeRef;
import com.catanatron.core.map.NodeRef;
import java.util.Map;

public record Water(Map<NodeRef, Integer> nodes, Map<EdgeRef, Edge> edges) implements Tile {}
