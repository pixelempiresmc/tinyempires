package dev.sucrose.tinyempires.models;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class Empire {

    public static final int WAR_TIME_MINUTES = 20;
    public static final int TIME_TO_CONQUER_CHUNK_SECONDS = 5;

    private static final Map<ObjectId, Empire> empireCache = new HashMap<>();
    private static final MongoCollection<Document> collection = TinyEmpires.getDatabase().getCollection("empires");
    private static final Map<UUID, ObjectId> playerToEmpireJoinRequest = new HashMap<>();

    private final ObjectId id;
    private String name;
    private double reserve;
    private String description;
    private Color color;
    private final List<TEPlayer> members = new ArrayList<>();
    private final Map<String, Position> positions = new HashMap<>();

    // Wars are only twenty minutes and can be stored in memory
    private Empire atWarWith = null;
    private Boolean isAttackerInWar;

    public static ObjectId createEmpire(String name, TEPlayer tePlayer) {
        final Document document = new Document("name", name)
            .append("reserve", 0.0d)
            .append("description", null)
            .append("color", Color.values()[new Random().nextInt(Color.values().length)].name())
            .append("members", new ArrayList<String>() {{
                add(tePlayer.getPlayerUUID().toString());
            }})
            .append("positions", new HashMap<String, List<String>>() {{
                put(
                    "Creator",
                    new ArrayList<String>() {{
                        add(Permission.ADMIN.name());
                    }}
                );
            }});
        tePlayer.setPositionName("Creator");
        final InsertOneResult result = collection.insertOne(document);
        if (result.getInsertedId() == null)
            throw new NullPointerException("Unable to insert document");
        return result.getInsertedId().asObjectId().getValue();
    }

    public static void clearCache() {
        empireCache.clear();
    }

    public void delete() {
        collection.deleteOne(new Document("_id", id));
        empireCache.remove(id);
    }

    public Empire(Document document) {
        id = document.getObjectId("_id");
        name = document.getString("name");
        reserve = document.getDouble("reserve");
        description = document.getString("description");
        color = Color.valueOf(document.getString("color"));
        for (final String memberUUID : document.getList("members", String.class))
            members.add(TEPlayer.getTEPlayer(UUID.fromString(memberUUID)));
        final Document positionsDocument = document.get("positions", Document.class);
        for (final String positionName : positionsDocument.keySet())
            positions.put(positionName, new Position(positionsDocument.getList(positionName, String.class)));
    }

    public static Empire getEmpire(String name) {
        // check cache for player
        for (Empire empire : empireCache.values()) {
            if (empire.getName().equals(name))
                return empire;
        }
        // if cache miss fetch from mongo
        final Document document = collection.find(new Document("name", name)).first();
        if (document == null)
            return null;
        final Empire empire = new Empire(document);
        empireCache.put(empire.getId(), empire);
        return empire;
    }

    public static Empire getEmpire(ObjectId id) {
        // check cache for player
        if (empireCache.containsKey(id))
            return empireCache.get(id);
        // if cache miss fetch from mongo
        final Document document = collection.find(new Document("_id", id)).first();
        if (document == null)
            return null;
        final Empire empire = new Empire(document);
        empireCache.put(id, empire);
        return empire;
    }

    private void updateMemberScoreboards() {
        for (final TEPlayer player : members)
            player.updatePlayerScoreboard();
    }

    public void setAtWarWith(Empire empire, boolean isAttacker) {
        atWarWith = empire;
        isAttackerInWar = isAttacker;
    }

    public void endWar() {
        atWarWith = null;
        isAttackerInWar = null;
    }

    public Empire getAtWarWith() {
        return atWarWith;
    }

    public boolean isAttackerInWar() {
        return isAttackerInWar;
    }

    public void broadcast(ChatColor color, String text) {
        final String message = getChatColor() + String.format(
            "%s %s",
            ChatColor.BOLD + name + ">" + color,
            text
        );
        broadcastText(message);
    }

    public void broadcastText(String text) {
        for (TEPlayer p : members) {
            final Player player = Bukkit.getPlayer(p.getPlayerUUID());
            if (player != null)
                player.sendMessage(text);
        }
    }

    public void addPlayerJoinRequest(TEPlayer player) {
        playerToEmpireJoinRequest.put(player.getPlayerUUID(), id);
    }

    public void acceptPlayerJoinRequest(TEPlayer player) {
        this.members.add(player);
        player.setEmpireId(id);
        collection.updateOne(
            new Document("_id", id),
            new Document(
                "$addToSet",
                new Document("members", player.getPlayerUUID())
            )
        );
    }

    public void setColor(Color color) {
        this.color = color;
        DrawEmpire.updateEmpireChunksDescription(this);
        save(new Document("color", color.name()));
    }

    private void save(Document document) {
        collection.updateOne(new Document("_id", id), new Document("$set", document));
        updateMemberScoreboards();
        DrawEmpire.updateEmpireChunkDescriptions(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        save(new Document("name", name));
    }

    public ObjectId getId() {
        return id;
    }

    public double getReserve() {
        return reserve;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        DrawEmpire.updateEmpireChunkDescriptions(this);
        save(new Document("description", description));
    }

    public void setReserve(double amount) {
        this.reserve = amount;
        save(new Document("reserve", reserve));
    }

    public void giveReserveCoins(double amount) {
        setReserve(reserve + amount);
    }

    public void takeReserveCoins(double amount) {
        setReserve(reserve - amount);
    }

    public List<TEPlayer> getMembers() {
        return members;
    }

    public void removeMember(TEPlayer player) {
        for (int index = 0; index < members.size(); index++) {
            System.out.println(index);
            if (members.get(index).getPlayerUUID().equals(player.getPlayerUUID())) {
                members.remove(index);
                break;
            }
        }
        collection.updateOne(
            new Document("_id", id),
            new Document(
                "$pull",
                new Document("members", player.getPlayerUUID().toString())
            )
        );
        updateMemberScoreboards();
    }

    public Position getPosition(String name) {
        return positions.get(name);
    }

    public Map<String, Position> getPositionMap() {
        return positions;
    }

    private void savePositions() {
        Document document = new Document();
        for (String name : positions.keySet())
            document.put(name, positions.get(name).toList());
        save(new Document("positions", document));
    }

    public void createPosition(String name) {
        positions.put(name, new Position());
        collection.updateOne(
            new Document("_id", id),
            new Document(
                "$set",
                new Document(
                    "positions." + name,
                    new ArrayList<>()
                )
            )
        );
        updateMemberScoreboards();
    }

    /**
     * Toggles position permission
     * @param name - position name
     * @param permission - permission
     * @return - New state of permission
     */
    public boolean togglePositionPermission(String name, Permission permission) {
        final Position position = positions.get(name);
        if (position.hasPermission(permission)) {
            position.revokePermission(permission);
            return false;
        }
        position.givePermission(permission);
        return true;
    }

    public void removePosition(String position) {
        for (TEPlayer member : members) {
            if (member.getPositionName().equals(position)) {
                member.updatePlayerScoreboard();
                member.setPositionName(null);
            }
        }
        positions.remove(position);
        savePositions();
    }

    public Color getColor() {
        return color;
    }

    public ChatColor getChatColor() {
        return ChatColor.valueOf(color.name());
    }

    public static Map<UUID, ObjectId> getPlayerToEmpireJoinRequest() {
        return playerToEmpireJoinRequest;
    }

}
