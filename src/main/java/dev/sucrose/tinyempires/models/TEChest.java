package dev.sucrose.tinyempires.models;

import com.mongodb.client.MongoCollection;
import dev.sucrose.tinyempires.TinyEmpires;
import org.bson.Document;

import java.util.*;

public class TEChest {

    final private static Map<String, UUID> chestToPlayerCache = new HashMap<>();
    private static final MongoCollection<Document> collection = TinyEmpires.getDatabase().getCollection("chests");

    private final UUID owner;

    private static String coordinatesToCacheKey(String world, int x, int y, int z) {
        return String.format("%s %d %d %d", world, x, y, z);
    }

    public static UUID getChestCoordinatesToPlayer(String world, int x, int y, int z) {
        final String cacheKey = coordinatesToCacheKey(world, x, y, z);
        if (chestToPlayerCache.containsKey(cacheKey))
            return chestToPlayerCache.get(cacheKey);
        final Document document = collection.find(
                new Document("world", world)
                    .append("x", x)
                    .append("y", y)
                    .append("z", z)
            ).first();
        if (document == null)
            return null;
        final TEChest teChest = new TEChest(document);
        chestToPlayerCache.put(cacheKey, teChest.getOwner());
        return teChest.getOwner();
    }

    public static void createChestToPlayerMapping(String world, int x, int y, int z, UUID owner) {
        collection.insertOne(
            new Document("world", world)
                .append("x", x)
                .append("y", y)
                .append("z", z)
                .append("owner", owner)
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
        owner = UUID.fromString(document.getString("owner"));
    }

    public static void clearCache() {
        chestToPlayerCache.clear();
    }

    public UUID getOwner() {
        return owner;
    }

}
