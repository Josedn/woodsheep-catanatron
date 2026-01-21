package com.catanatron.core.map;

import com.catanatron.core.map.tiles.LandTile;
import com.catanatron.core.model.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MapTemplate(
    List<Integer> numbers,
    List<Resource> portResources, // null => 3:1
    List<Resource> tileResources, // includes null for desert
    Map<Coordinate, Object> topology // LandTile.class, Water.class, or (Port.class, Direction)
    ) {
  public static MapTemplate buildBaseTemplate() {
    List<Integer> numbers =
        Arrays.asList(11, 3, 6, 5, 4, 9, 10, 8, 4, 11, 12, 9, 10, 8, 3, 6, 2, 5);
    List<Resource> ports =
        Arrays.asList(
            Resource.WOOD,
            Resource.BRICK,
            Resource.SHEEP,
            Resource.WHEAT,
            Resource.ORE,
            null,
            null,
            null,
            null);
    List<Resource> tiles =
        Arrays.asList(
            Resource.WOOD,
            Resource.WOOD,
            Resource.WOOD,
            Resource.WOOD,
            Resource.BRICK,
            Resource.BRICK,
            Resource.BRICK,
            Resource.SHEEP,
            Resource.SHEEP,
            Resource.SHEEP,
            Resource.SHEEP,
            Resource.WHEAT,
            Resource.WHEAT,
            Resource.WHEAT,
            Resource.WHEAT,
            Resource.ORE,
            Resource.ORE,
            Resource.ORE,
            null);
    Collections.shuffle(tiles);
    Map<Coordinate, Object> topology = new LinkedHashMap<>();
    // center
    topology.put(new Coordinate(0, 0, 0), LandTile.class);
    // first ring
    topology.put(new Coordinate(1, -1, 0), LandTile.class);
    topology.put(new Coordinate(0, -1, 1), LandTile.class);
    topology.put(new Coordinate(-1, 0, 1), LandTile.class);
    topology.put(new Coordinate(-1, 1, 0), LandTile.class);
    topology.put(new Coordinate(0, 1, -1), LandTile.class);
    topology.put(new Coordinate(1, 0, -1), LandTile.class);
    // second ring
    topology.put(new Coordinate(2, -2, 0), LandTile.class);
    topology.put(new Coordinate(1, -2, 1), LandTile.class);
    topology.put(new Coordinate(0, -2, 2), LandTile.class);
    topology.put(new Coordinate(-1, -1, 2), LandTile.class);
    topology.put(new Coordinate(-2, 0, 2), LandTile.class);
    topology.put(new Coordinate(-2, 1, 1), LandTile.class);
    topology.put(new Coordinate(-2, 2, 0), LandTile.class);
    topology.put(new Coordinate(-1, 2, -1), LandTile.class);
    topology.put(new Coordinate(0, 2, -2), LandTile.class);
    topology.put(new Coordinate(1, 1, -2), LandTile.class);
    topology.put(new Coordinate(2, 0, -2), LandTile.class);
    topology.put(new Coordinate(2, -1, -1), LandTile.class);
    // third (water) layer
    return new MapTemplate(numbers, ports, tiles, topology);
  }
}
