package dev.sucrose.tinyempires.models;

import com.mongodb.client.MongoCollection;
import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.utils.BoundUtils;
import org.bson.Document;
import org.bukkit.Chunk;

import java.util.*;
import java.util.stream.Collectors;

public class TEChest {

    final private static Map<String, UUID> chestToPlayerCache = new HashMap<>();
    private static final MongoCollection<Document> collection = TinyEmpires.getDatabase().getCollection("chests");

    private final UUID owner;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    static {
        fillCache();
    }

    public static void writeCache() {
        final List<Document> documents =
            chestToPlayerCache.entrySet()
                .stream()
                .map(entry -> {
                    final String[] words = entry.getKey().split(" ");
                    final String world = words[0];
                    final int x = Integer.parseInt(words[1]);
                    final int y = Integer.parseInt(words[2]);
                    final int z = Integer.parseInt(words[3]);
                    return new Document("world", world)
                        .append("x", x)
                        .append("y", y)
                        .append("z", z)
                        .append("owner", entry.getValue().toString());
                })
                .collect(Collectors.toList());
        collection.deleteMany(new Document());
        collection.insertMany(documents);
    }

    public static void fillCache() {
        chestToPlayerCache.clear();
        for (final Document document : collection.find()) {
            final TEChest chest = new TEChest(document);
            chestToPlayerCache.put(
                coordinatesToCacheKey(
                    chest.getWorld(),
                    chest.getX(),
                    chest.getY(),
                    chest.getZ()
                ),
                chest.getOwner()
            );
        }

    }

    public static void removeChestMappingsInChunk(TEChunk chunk) {
        for (final String coordinateString : chestToPlayerCache.keySet()) {
            final String[] words = coordinateString.split(" ");
            final String world = words[0];
            final int x = Integer.parseInt(words[1]);
            final int z = Integer.parseInt(words[3]);
            // if world, x, and z are within chunk then remove mapping
            if (BoundUtils.coordsInChunk(world, x, z, chunk)) {
                // only need y if removing chest mapping
                final int y = Integer.parseInt(words[2]);
                removeChestToPlayerMapping(world, x, y, z);
            }
        }
    }

    private static String coordinatesToCacheKey(String world, int x, int y, int z) {
        return String.format("%s %d %d %d", world, x, y, z);
    }

    public static UUID getChestCoordinatesToPlayer(String world, int x, int y, int z) {
        final String cacheKey = coordinatesToCacheKey(world, x, y, z);
        if (chestToPlayerCache.containsKey(cacheKey))
            return chestToPlayerCache.get(cacheKey);
        return null;
    }

    public static void createChestToPlayerMapping(String world, int x, int y, int z, UUID owner) {
        collection.insertOne(
            new Document("world", world)
                .append("x", x)
                .append("y", y)
                .append("z", z)
                .append("owner", owner.toString())
        );
        chestToPlayerCache.put(coordinatesToCacheKey(world, x, y, z), owner);
    }

    public static void removeChestToPlayerMapping(String world, int x, int y, int z) {
        collection.deleteOne(
            new Document("world", world)
                .append("x", x)
                .append("y", y)
                .append("z", z)
        );
        chestToPlayerCache.remove(coordinatesToCacheKey(world, x, y, z));
    }

    public TEChest(Document document) {
        world = document.getString("world");
        x = document.getInteger("x");
        y = document.getInteger("y");
        z = document.getInteger("z");
        owner = UUID.fromString(document.getString("owner"));
    }

    public static void clearCache() {
        chestToPlayerCache.clear();
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public UUID getOwner() {
        return owner;
    }

}
