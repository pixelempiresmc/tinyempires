package dev.sucrose.tinyempires.utils;

public class BoundUtils {

    public final static int WATER_ARENA_RIGHT_X = 8764;
    public final static int WATER_ARENA_LEFT_X = 8715;
    public final static int WATER_ARENA_TOP_Z = 742;
    public final static int WATER_ARENA_BOTTOM_Z = 791;

    public static boolean inBoundsOfWaterArena(int x, int z) {
        return x >= WATER_ARENA_LEFT_X
            && x <= WATER_ARENA_RIGHT_X
            && z >= WATER_ARENA_TOP_Z
            && z <= WATER_ARENA_BOTTOM_Z;
    }

}
