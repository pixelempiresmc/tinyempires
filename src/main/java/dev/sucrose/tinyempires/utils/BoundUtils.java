package dev.sucrose.tinyempires.utils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

public class BoundUtils {

    public final static int WATER_ARENA_RIGHT_X = 8764;
    public final static int WATER_ARENA_LEFT_X = 8715;
    public final static int WATER_ARENA_TOP_Z = 791;
    public final static int WATER_ARENA_BOTTOM_Z = 742;

    public final static int OLYMPUS_BOTTOM_Z = -2605;
    public final static int OLYMPUS_TOP_Z = -2447;
    public final static int OLYMPUS_LEFT_X = 1307;
    public final static int OLYMPUS_RIGHT_X = 1472;

    private static final int PIT_LEFT_X = 461;
    private static final int PIT_RIGHT_X = 569;
    private static final int PIT_BOTTOM_Z = 303;
    private static final int PIT_TOP_Z = 419;

    private static final int PORTAL_CASTLE_LEFT_X = 495;
    private static final int PORTAL_CASTLE_RIGHT_X = 531;
    private static final int PORTAL_CASTLE_BOTTOM_Z = 348;
    private static final int PORTAL_CASTLE_TOP_Z = 387;

    private static final int PIXEL_LEFT_X = -4643;
    private static final int PIXEL_RIGHT_X = -4628;
    private static final int PIXEL_BOTTOM_Z = -1086;
    private static final int PIXEL_TOP_Z = -1070;

    private static boolean inBounds(int x, int z, int leftX, int rightX, int bottomZ, int topZ) {
        return x >= leftX
            && x <= rightX
            && z <= topZ
            && z >= bottomZ;
    }

    public static boolean inBoundsOfPit(String world, int x, int z) {
        return world.equals("world_nether")
            && inBounds(x, z, PIT_LEFT_X, PIT_RIGHT_X, PIT_BOTTOM_Z, PIT_TOP_Z);
    }

    public static boolean inBoundsOfPixel(String world, int x, int z) {
        return world.equals("world")
            && inBounds(x, z, PIXEL_LEFT_X, PIXEL_RIGHT_X, PIXEL_BOTTOM_Z, PIXEL_TOP_Z);
    }

    public static boolean inBoundsOfWaterArena(String world, int x, int z) {
        return world.equals("world")
            && inBounds(x, z, WATER_ARENA_LEFT_X, WATER_ARENA_RIGHT_X, WATER_ARENA_BOTTOM_Z, WATER_ARENA_TOP_Z);
    }

    public static boolean inBoundsOfOlympus(String world, int x, int z) {
        return world.equals("world")
            && inBounds(x, z, OLYMPUS_LEFT_X, OLYMPUS_RIGHT_X, OLYMPUS_BOTTOM_Z, OLYMPUS_TOP_Z);
    }

    public static boolean inBoundsOfPortalCastle(String world, int x, int z) {
        return world.equals("world_nether")
            && inBounds(x, z, PORTAL_CASTLE_LEFT_X, PORTAL_CASTLE_RIGHT_X, PORTAL_CASTLE_BOTTOM_Z, PORTAL_CASTLE_TOP_Z);
    }

    public static boolean isChunkInBoundsOfSpecialTerritory(Chunk chunk) {
        final int x = chunk.getX() * 16;
        final int z = chunk.getZ() * 16;
        // if any corner is in special territory then return true
        // (assumes all special territory bounds are defined as rectangular)
        final String world = chunk.getWorld().getName();
        return inBoundsOfSpecialChunk(world, x, z)
            || inBoundsOfSpecialChunk(world,x + 16, z)
            || inBoundsOfSpecialChunk(world, x, z + 16)
            || inBoundsOfSpecialChunk(world,x + 16, z + 16);
    }

    public static boolean inBoundsOfSpecialChunk(String world, int x, int z) {
        return inBoundsOfOlympus(world, x, z)
            || inBoundsOfWaterArena(world, x, z)
            || inBoundsOfPortalCastle(world, x, z)
            || inBoundsOfPixel(world, x, z);
    }

}
