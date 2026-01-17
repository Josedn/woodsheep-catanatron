package com.catanatron.core.engine;

public final class Costs {
    private Costs() {}

    public static boolean canAffordSettlement(State s, com.catanatron.core.model.PlayerColor c) {
        return has(s,c,"WOOD",1) && has(s,c,"BRICK",1) && has(s,c,"SHEEP",1) && has(s,c,"WHEAT",1);
    }

    public static boolean canAffordCity(State s, com.catanatron.core.model.PlayerColor c) {
        return has(s,c,"WHEAT",2) && has(s,c,"ORE",3);
    }

    public static boolean canAffordRoad(State s, com.catanatron.core.model.PlayerColor c) {
        return has(s,c,"WOOD",1) && has(s,c,"BRICK",1);
    }

    public static void paySettlement(State s, com.catanatron.core.model.PlayerColor c) {
        add(s,c,"WOOD",-1); add(s,c,"BRICK",-1); add(s,c,"SHEEP",-1); add(s,c,"WHEAT",-1);
    }

    public static void payCity(State s, com.catanatron.core.model.PlayerColor c) {
        add(s,c,"WHEAT",-2); add(s,c,"ORE",-3);
    }

    public static void payRoad(State s, com.catanatron.core.model.PlayerColor c) {
        add(s,c,"WOOD",-1); add(s,c,"BRICK",-1);
    }

    private static boolean has(State s, com.catanatron.core.model.PlayerColor c, String res, int amount) {
        int idx = s.colors.indexOf(c);
        return s.playerState.get("P"+idx+"_"+res+"_IN_HAND") >= amount;
    }

    private static void add(State s, com.catanatron.core.model.PlayerColor c, String res, int delta) {
        int idx = s.colors.indexOf(c);
        String key = "P"+idx+"_"+res+"_IN_HAND";
        s.playerState.put(key, s.playerState.get(key) + delta);
    }
}

