package com.catanatron.core.map.tiles;

import com.catanatron.core.map.EdgeRef;
import com.catanatron.core.map.NodeRef;
import com.catanatron.core.model.Resource;
import java.util.Map;

/**
 * @param resource null => desert
 * @param number null if desert
 */
public record LandTile(
    int id,
    Resource resource,
    Integer number,
    Map<NodeRef, Integer> nodes,
    Map<EdgeRef, Edge> edges)
    implements Tile {}
