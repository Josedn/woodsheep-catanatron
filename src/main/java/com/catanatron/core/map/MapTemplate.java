package com.catanatron.core.map;

import com.catanatron.core.model.Resource;
import java.util.List;
import java.util.Map;

public record MapTemplate(
    List<Integer> numbers,
    List<Resource> portResources, // null => 3:1
    List<Resource> tileResources, // includes null for desert
    Map<Coordinate, Object> topology // LandTile.class, Water.class, or (Port.class, Direction)
    ) {}
