package dev.sucrose.tinyempires.models;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;

import java.util.*;

public class TEChunk {

    final private static Map<String, TEChunk> chunkCache = new HashMap<>();

    private static final MongoCollection<Document> collection = TinyEmpires.getDatabase().getCollection("chunks");

    private final ObjectId id;
    private final String world;
    private final int x;
    private final int z;
    private Empire empire;
    private final Map<String, UUID> chestToPlayerOwner = new HashMap<>();
    private ChunkType type;

    static {
        System.out.println("Filling chunk cache");
        // fill cache from mongo
        for (final Document document : collection.find()) {
            final TEChunk chunk = new TEChunk(document);
            chunkCache.put(chunkToKey(chunk.getWorld(), chunk.getX(), chunk.getZ()), chunk);
        }
    }

    private static String chunkToKey(String world, int x, int z) {
        return world + ' ' + x + ' ' + z;
    }

    public static List<TEChunk> getChunks() {
        final List<TEChunk> chunks = new ArrayList<>();
        for (final Document document : collection.find())
            chunks.add(new TEChunk(document));
        return chunks;
    }

    public static void deleteChunks(ObjectId empire) {
        collection.deleteMany(new Document("empire", empire));
    }

    public static List<TEChunk> getEmpireChunks(ObjectId id) {
        final FindIterable<Document> documents = collection.find(new Document("empire", id));
        final List<TEChunk> chunks = new ArrayList<>();
        for (final Document document : documents)
            chunks.add(new TEChunk(document));
        return chunks;
    }

    public static TEChunk getChunk(String world, int x, int z) {
        // check cache
        final String cacheKey = chunkToKey(world, x, z);
        final TEChunk cacheChunk = chunkCache.get(cacheKey);
        if (cacheChunk != null) {
            return cacheChunk;
        }

        return null;
        // if cache miss fetch from mongo
//        final Document document = collection.find(
//            new Document("world", world)
//                .append("x", x)
//                .append("z", z)
//        ).first();
//        if (document == null)
//            return null;
//        final TEChunk chunk = new TEChunk(document);
//        chunkCache.put(cacheKey, chunk);
    }

    public boolean equalsChunk(TEChunk chunk) {
        if (chunk == null)
            return false;
        return world.equals(chunk.getWorld())
            && empire.getId().equals(chunk.getId())
            && x == chunk.getX()
            && z == chunk.getZ();
    }

    public static TEChunk getChunk(Chunk chunk) {
        return getChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static boolean chunkExists(String world, int x, int z) {
        return getChunk(world, x, z) != null;
    }

    public final static double CHUNK_COST = 0.1;
    public static void createTEChunk(String world, int x, int z, Empire empire) {
        if (getChunk(world, x, z) != null) {
            System.out.println(ChatColor.DARK_RED + String.format(
                "Chunk at %d, %d in %s is already claimed; returning",
                x,
                z,
                StringUtils.worldDirToName(world)
            ));
            return;
        }

        final Document document = new Document("world", world)
            .append("x", x)
            .append("z", z)
            .append("empire", empire.getId())
            .append("chests", new HashMap<>())
            .append("type", ChunkType.NONE.name());
        empire.takeReserveCoins(CHUNK_COST);
        collection.insertOne(document);
    }

    public void delete() {
        collection.deleteOne(
            new Document("world", world)
                .append("x", x)
                .append("z", z)
        );
        DrawEmpire.removeChunk(this);
        chunkCache.remove(this);
    }

    public TEChunk(Document document) {
        id = document.getObjectId("_id");
        world = document.getString("world");
        x = document.getInteger("x");
        z = document.getInteger("z");
        type = ChunkType.valueOf(document.getString("type"));
        empire = Empire.getEmpire(document.getObjectId("empire"));
        final Document chestToPlayer = document.get("chests", Document.class);
        for (String coordinates : chestToPlayer.keySet())
            chestToPlayerOwner.put(coordinates, UUID.fromString(chestToPlayer.getString(coordinates)));
    }

    private void save(Document document) {
        collection.updateOne(new Document("_id", id), new Document("$set", document));
    }

    public ObjectId getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getWorldX() {
        return x * 16;
    }

    public int getWorldZ() {
        return z * 16;
    }

    public Empire getEmpire() {
        return empire;
    }

    public void setEmpire(Empire empire) {
        this.empire = empire;
        save(new Document("empire", empire.getId()));
    }

    private void saveChestToPlayerOwner() {
        Document document = new Document();
        for (String name : chestToPlayerOwner.keySet())
            document.put(name, chestToPlayerOwner.get(name).toString());
        save(new Document("chests", document));
    }

    private String chunkCoordinateToBlock(int x, int y, int z) {
        return String.format("%d %d %d", x, y, z);
    }

    public UUID getChestToPlayerOwner(int x, int y, int z) {
        return chestToPlayerOwner.get(chunkCoordinateToBlock(x, y, z));
    }

    public void putChestToPlayerOwner(int x, int y, int z, UUID owner) {
        chestToPlayerOwner.put(chunkCoordinateToBlock(x, y, z), owner);
        saveChestToPlayerOwner();
    }

    public void removeChestToPlayerOwner(int x, int y, int z) {
        chestToPlayerOwner.remove(chunkCoordinateToBlock(x, y, z));
        saveChestToPlayerOwner();
    }

    public ChunkType getType() {
        return type;
    }

    public void setType(ChunkType type) {
        this.type = type;
        save(new Document("type", type.name()));
    }

    public void remove() {
        chunkCache.remove(chunkToKey(world, x, z));
        collection.deleteOne(new Document("_id", id));
    }

    public static void clearCache() {
        chunkCache.clear();
    }

    public String toString() {
        return world + ' ' + x + ' ' + z;
    }

    public String serialize() {
        return world + ' ' + x + ' ' + z;
    }

    public static String serialize(String world, int x, int z) {
        return world + ' ' + x + ' ' + z;
    }

}
