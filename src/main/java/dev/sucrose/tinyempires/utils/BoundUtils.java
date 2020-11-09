package dev.sucrose.tinyempires.utils;

public class BoundUtils {

    public final static int WATER_ARENA_RIGHT_X = 8764;
    public final static int WATER_ARENA_LEFT_X = 8715;
    public final static int WATER_ARENA_TOP_Z = 742;
    public final static int WATER_ARENA_BOTTOM_Z = 791;

    public final static int OLYMPUS_BOTTOM_Z = -2605;
    public final static int OLYMPUS_TOP_Z = -2447;
    public final static int OLYMPUS_LEFT_X = 1307;
    public final static int OLYMPUS_RIGHT_X = 1472;

    // MT. OLYMPUS Coords: 1399 64 -2515

    private static boolean inBounds(int x, int z, int leftX, int rightX, int bottomZ, int topZ) {
        return x >= leftX
            && x <= rightX
            && z >= topZ
            && z <= bottomZ;
    }

    public static boolean inBoundsOfWaterArena(int x, int z) {
        return inBounds(x, z, WATER_ARENA_LEFT_X, WATER_ARENA_RIGHT_X, WATER_ARENA_BOTTOM_Z, WATER_ARENA_TOP_Z);
    }

    public static boolean inBoundsOfOlympus(int x, int z) {
        return inBounds(x, z, OLYMPUS_LEFT_X, OLYMPUS_RIGHT_X, OLYMPUS_BOTTOM_Z, OLYMPUS_TOP_Z);
    }

    public static boolean inBoundsOfSpecialChunk(int x, int z) {
        return inBoundsOfOlympus(x, z) || inBoundsOfWaterArena(x, z);
    }

}
