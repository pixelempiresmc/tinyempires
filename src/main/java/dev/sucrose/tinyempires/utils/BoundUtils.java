package dev.sucrose.tinyempires.utils;

public class BoundUtils {

    public final static int WATER_ARENA_RIGHT_X = 8764;
    public final static int WATER_ARENA_LEFT_X = 8715;
    public final static int WATER_ARENA_TOP_Z = 742;
    public final static int WATER_ARENA_BOTTOM_Z = 791;

    // MT. OLYMPUS Coords: 1399 64 -2515

    public static boolean inBoundsOfWaterArena(int x, int z) {
        return x >= WATER_ARENA_LEFT_X
            && x <= WATER_ARENA_RIGHT_X
            && z >= WATER_ARENA_TOP_Z
            && z <= WATER_ARENA_BOTTOM_Z;
    }

    public static void inBoundsOfOlympus(int x, int z) {

    }

}
