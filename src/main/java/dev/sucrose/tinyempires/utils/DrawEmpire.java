package dev.sucrose.tinyempires.utils;

import dev.sucrose.tinyempires.models.*;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PolyLineMarker;

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

    // worldborder
    private static int borderLineIncr = 0;
    private static void drawBorderLine(String world, String message, String description, int color, int x1, int x2, int z1, int z2) {
        PolyLineMarker marker = markerSet.createPolyLineMarker(
            "__Worldborder" + borderLineIncr++,
            message,
            true,
            world,
            new double[] { x1, x2 },
            new double[] { 64, 64 }, // arbitrary Y coordinates
            new double[] { z1, z2 },
            false
        );
        marker.setLineStyle(1, 1, color);
        marker.setDescription(description);
    }

    private static void drawBorder(String world, String label, String desc, int color, int leftX, int rightX, int bottomZ, int topZ) {
        drawBorderLine(world, label, desc, color, leftX, rightX, topZ, topZ);
        drawBorderLine(world, label, desc, color, leftX, leftX, topZ, bottomZ);
        drawBorderLine(world, label, desc, color, leftX, rightX, bottomZ, bottomZ);
        drawBorderLine(world, label, desc, color, rightX, rightX, topZ, bottomZ);
    }

    public static void drawBorders(int leftX, int rightX, int bottomZ, int topZ) {
        drawBorder(
            "world",
            "Worldborder",
            "Border of the world: reaching it will circumnavigate the player across the globe.",
            0xff0000, /* red */
            leftX,
            rightX,
            bottomZ,
            topZ
        );

        drawBorder(
            "world_nether",
            "Nether-to-Overworld Limits",
            "Portals cannot be made outside of this border.",
            0xffff00, /* yellow */
            leftX / 8,
            rightX / 8,
            bottomZ / 8,
            topZ / 8
        );
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
        System.out.printf("Erasing chunk marker border at %d %d in %s direction %s\n", x, z, world, direction.name());
        if (marker != null && marker.hasBorder(direction)) {
            System.out.println("Deleting border");
            marker.removeBorder(direction);
        } else {
            System.out.println("Chunk marker does not have specified border");
        }
    }

    public static void drawChunk(Empire empire, String world, int x, int z) {
        final ChunkMarker marker = new ChunkMarker(empire, world, x, z);
        chunkMarkers.put(
            TEChunk.serialize(world, x, z),
            marker
        );

        final TEChunk chunk = TEChunk.getChunk(world, x, z);
        if (chunk == null)
            throw new NullPointerException("Could not fetch TEChunk for chunk empire comparison");

        System.out.println("Right chunk differs: " + chunkEmpiresDiffer(chunk, x + 1, z));
        if (chunkEmpiresDiffer(chunk, x + 1, z)) {
            marker.makeBorder(Direction.RIGHT);
        } else {
            eraseChunkBorderIfExists(world, x + 1, z, Direction.LEFT);
        }

        System.out.println("Left chunk differs: " + chunkEmpiresDiffer(chunk, x - 1, z));
        if (chunkEmpiresDiffer(chunk, x - 1, z)) {
            marker.makeBorder(Direction.LEFT);
        } else {
            eraseChunkBorderIfExists(world, x - 1, z, Direction.RIGHT);
        }

        System.out.println("Down chunk differs: " + chunkEmpiresDiffer(chunk, x, z + 1));
        if (chunkEmpiresDiffer(chunk, x, z + 1)) {
            marker.makeBorder(Direction.DOWN);
        } else {
            eraseChunkBorderIfExists(world, x, z + 1, Direction.UP);
        }

        System.out.println("Up chunk differs: " + chunkEmpiresDiffer(chunk, x, z - 1));
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
        final List<String> markerKeys = empireChunkMarkers.get(empire.getId());
        if (markerKeys == null)
            return;
        for (final String key : markerKeys)
            chunkMarkers.get(key).updateDescription();
    }

    public static void removeChunk(TEChunk chunk) {
        final String world = chunk.getWorld();
        final int x = chunk.getX();
        final int z = chunk.getZ();
        final String key = TEChunk.serialize(world, x, z);
        chunkMarkers.get(key).erase();
        chunkMarkers.remove(key);

        // borders
        // right
        if (TEChunk.chunkExists(world, x + 1, z))
            getChunkMarker(world, x + 1, z).makeBorder(Direction.LEFT);

        // left
        if (TEChunk.chunkExists(world, x - 1, z))
            getChunkMarker(world, x - 1, z).makeBorder(Direction.RIGHT);

        // below
        if (TEChunk.chunkExists(world, x, z + 1))
            getChunkMarker(world, x, z + 1).makeBorder(Direction.UP);

        // above
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

    public static void setEmpire(String world, int x, int z, Empire empire) {
        final ChunkMarker marker = getChunkMarker(world, x, z);
        marker.setEmpire(empire);
        marker.deleteBorders();

        final TEChunk chunk = TEChunk.getChunk(world, x, z);
        if (chunk == null)
            throw new NullPointerException(String.format(
                "ERROR: Chunk at %s %d, %d doesn't exist",
                world,
                x,
                z
            ));

        if (!chunk.isAdjacentChunkTheSameEmpire(Direction.UP))
            marker.makeBorder(Direction.UP);

        if (!chunk.isAdjacentChunkTheSameEmpire(Direction.DOWN))
            marker.makeBorder(Direction.DOWN);

        if (!chunk.isAdjacentChunkTheSameEmpire(Direction.RIGHT))
            marker.makeBorder(Direction.RIGHT);

        if (!chunk.isAdjacentChunkTheSameEmpire(Direction.LEFT))
            marker.makeBorder(Direction.LEFT);
    }

}
