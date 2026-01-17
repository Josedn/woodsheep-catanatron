package com.catanatron.core.map.tiles;

public sealed interface Tile permits LandTile, Port, Water {}
