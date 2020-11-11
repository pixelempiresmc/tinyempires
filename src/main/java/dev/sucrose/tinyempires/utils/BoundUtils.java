package dev.sucrose.tinyempires.utils;

import org.bukkit.Chunk;

public class BoundUtils {

    public final static int WATER_ARENA_RIGHT_X = 8764;
    public final static int WATER_ARENA_LEFT_X = 8715;
    public final static int WATER_ARENA_TOP_Z = 742;
    public final static int WATER_ARENA_BOTTOM_Z = 791;

    public final static int OLYMPUS_BOTTOM_Z = -2605;
    public final static int OLYMPUS_TOP_Z = -2447;
    public final static int OLYMPUS_LEFT_X = 1307;
    public final static int OLYMPUS_RIGHT_X = 1472;

    private static boolean inBounds(int x, int z, int leftX, int rightX, int bottomZ, int topZ) {
        return x >= leftX
            && x <= rightX
            && z <= topZ
            && z >= bottomZ;
    }

    public static boolean inBoundsOfWaterArena(int x, int z) {
        return inBounds(x, z, WATER_ARENA_LEFT_X, WATER_ARENA_RIGHT_X, WATER_ARENA_BOTTOM_Z, WATER_ARENA_TOP_Z);
    }

    public static boolean inBoundsOfOlympus(int x, int z) {
        return inBounds(x, z, OLYMPUS_LEFT_X, OLYMPUS_RIGHT_X, OLYMPUS_BOTTOM_Z, OLYMPUS_TOP_Z);
    }

    public static boolean isChunkInBoundsOfSpecialTerritory(Chunk chunk) {
        final int x = chunk.getX() * 16;
        final int z = chunk.getZ() * 16;
        // if any corner is in special territory then return true
        // (assumes all special territory bounds are defined as rectangular)
        return inBoundsOfSpecialChunk(x, z)
            || inBoundsOfSpecialChunk(x + 16, z)
            || inBoundsOfSpecialChunk(x, z + 16)
            || inBoundsOfSpecialChunk(x + 16, z + 16);
    }

    public static boolean inBoundsOfSpecialChunk(int x, int z) {
        return inBoundsOfOlympus(x, z) || inBoundsOfWaterArena(x, z);
    }

}
