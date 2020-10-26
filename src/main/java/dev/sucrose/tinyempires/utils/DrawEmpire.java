package dev.sucrose.tinyempires.utils;

import dev.sucrose.tinyempires.models.*;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawEmpire {

    private static final DynmapAPI dynmap = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("Dynmap");
    private static final MarkerSet markerSet;
    private static final Map<String, ChunkMarker> chunkMarkers = new HashMap<>();
    private static final Map<ObjectId, List<String>> empireChunkMarkers = new HashMap<>();

    static {
        assert dynmap != null;
        markerSet = dynmap.getMarkerAPI().createMarkerSet("tinyempires.empirechunkmarkerset", "Empires",
            dynmap.getMarkerAPI().getMarkerIcons(), false);
    }

    public static void drawChunks() {
        for (TEChunk chunk : TEChunk.getChunks())
            drawChunk(chunk.getEmpire(), chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    private static boolean chunkEmpiresDiffer(TEChunk chunk, int x, int z) {
        final TEChunk chunk1 = TEChunk.getChunk(chunk.getWorld(), x, z);
        return !chunk.getEmpire().getId().equals(chunk1 == null ? null : chunk1.getEmpire().getId());
    }

    private static void eraseChunkBorderIfExists(String world, int x, int z, Direction direction) {
        final ChunkMarker marker = getChunkMarker(world, x, z);
        if (marker != null && marker.hasBorder(direction))
            marker.removeBorder(direction);
    }

    public static void drawChunk(Empire empire, String world, int x, int z) {
        chunkMarkers.put(
            TEChunk.serialize(world, x, z),
            new ChunkMarker(empire, world, x, z)
        );

        final TEChunk chunk = TEChunk.getChunk(world, x, z);
        if (chunk == null)
            throw new NullPointerException("Could not fetch TEChunk for chunk empire comparison");

        final ChunkMarker marker = getChunkMarker(world, x, z);
        if (chunkEmpiresDiffer(chunk, x + 1, z)) {
            marker.makeBorder(Direction.RIGHT);
        } else {
            eraseChunkBorderIfExists(world, x + 1, z, Direction.LEFT);
        }

        if (chunkEmpiresDiffer(chunk, x - 1, z)) {
            marker.makeBorder(Direction.LEFT);
        } else {
            eraseChunkBorderIfExists(world, x - 1, z, Direction.RIGHT);
        }

        if (chunkEmpiresDiffer(chunk, x, z + 1)) {
            marker.makeBorder(Direction.DOWN);
        } else {
            eraseChunkBorderIfExists(world, x, z + 1, Direction.UP);
        }

        if (chunkEmpiresDiffer(chunk, x, z - 1)) {
            marker.makeBorder(Direction.UP);
        } else {
            eraseChunkBorderIfExists(world, x, z - 1, Direction.DOWN);
        }

        putChunkMarker(empire, world, x, z, marker);
    }

    public static void updateEmpireChunksDescription(Empire empire) {
        for (final String key : empireChunkMarkers.get(empire.getId()))
            chunkMarkers.get(key).updateColor();
    }

    public static void updateEmpireChunkDescriptions(Empire empire) {
        for (final String key : empireChunkMarkers.get(empire.getId()))
            chunkMarkers.get(key).updateDescription();
    }

    public static void removeChunk(TEChunk chunk) {
        final String world = chunk.getWorld();
        final int x = chunk.getX();
        final int z = chunk.getZ();
        chunkMarkers.remove(TEChunk.serialize(world, x, z));

        if (TEChunk.chunkExists(world, x + 1, z))
            getChunkMarker(world, x + 1, z).makeBorder(Direction.LEFT);

        if (TEChunk.chunkExists(world, x - 1, z))
            getChunkMarker(world, x - 1, z).makeBorder(Direction.RIGHT);

        if (TEChunk.chunkExists(world, x, z + 1))
            getChunkMarker(world, x, z + 1).makeBorder(Direction.UP);

        if (TEChunk.chunkExists(world, x, z - 1))
            getChunkMarker(world, x, z - 1).makeBorder(Direction.DOWN);
    }

    private static ChunkMarker getChunkMarker(String world, int x, int z) {
        return chunkMarkers.get(TEChunk.serialize(world, x, z));
    }

    private static void putChunkMarker(Empire empire, String world, int x, int z, ChunkMarker marker) {
        final String key = TEChunk.serialize(world, x, z);
        chunkMarkers.put(key, marker);
        if (empireChunkMarkers.containsKey(empire.getId())) {
            empireChunkMarkers.get(empire.getId()).add(key);
            return;
        }
        final List<String> chunkKeys = new ArrayList<>();
        chunkKeys.add(key);
        empireChunkMarkers.put(empire.getId(), chunkKeys);
    }

    private static void removeChunkMarker(Empire empire, String world, int x, int z) {
        final String key = TEChunk.serialize(world, x, z);
        chunkMarkers.remove(key);
        empireChunkMarkers.get(empire.getId()).remove(key);
        if (empireChunkMarkers.get(empire.getId()).size() == 0)
            empireChunkMarkers.remove(empire.getId());
    }

    public static void setMarkerType(String world, int x, int z, ChunkType type) {
        chunkMarkers.get(TEChunk.serialize(world, x, z)).setIcon(type);
    }

    public static MarkerSet getMarkerSet() {
        return markerSet;
    }

    public static MarkerAPI getMarkerAPI() {
        return dynmap.getMarkerAPI();
    }

}
