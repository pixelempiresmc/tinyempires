package dev.sucrose.tinyempires.models;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.lang.Nullable;
import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.commands.empire.options.EmpireCreationCallback;
import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private UUID owner;
    private Location homeLocation;
    private final List<TEPlayer> members = new ArrayList<>();
    private final Map<String, Position> positions = new HashMap<>();
    // name of law to law
    private final Map<String, Law> laws = new HashMap<>();
    private final Map<UUID, Double> memberDebt = new HashMap<>();
    private final String discordRoleId;

    // Wars are only twenty minutes and can be stored in memory
    private Empire atWarWith = null;
    private Boolean isAttackerInWar;

    static {
        fillCache();
    }

    public static void fillCache() {
        empireCache.clear();
        for (final Document document : collection.find()) {
            final Empire empire = new Empire(document);
            empireCache.put(
                empire.getId(),
                empire
            );
        }
    }

    // use synchronous callback to account for Discord role creation
    public static void createEmpire(String name, String homeWorld, double homeX, double homeY, double homeZ,
                                        TEPlayer tePlayer, EmpireCreationCallback callback) {
        final String uuidString = tePlayer.getPlayerUUID().toString();
        final String colorName = Color.values()[new Random().nextInt(Color.values().length - 1)].name();
        DiscordBot.createRoleAction(name, colorName)
            .queue(role -> {
                final Document document = new Document("name", name)
                    .append("reserve", 0.0d)
                    .append("description", null)
                    .append("color", colorName)
                    .append("members", new ArrayList<String>() {{
                        add(uuidString);
                    }})
                    .append("positions", new Document())
                    .append("laws", new Document())
                    .append("debt", new Document())
                    .append("owner", uuidString)
                    .append("home", new Document("world", homeWorld)
                        .append("x", homeX)
                        .append("y", homeY)
                        .append("z", homeZ)
                    )
                    .append("discord_id", role.getId());
                final InsertOneResult result = collection.insertOne(document);
                if (result.getInsertedId() == null)
                    throw new NullPointerException("Unable to insert document");
                final ObjectId id = result.getInsertedId().asObjectId().getValue();
                final Empire empire = new Empire(document);
                empireCache.put(id, empire);
                callback.run(id);
                DiscordBot.giveUserEmpireDiscordRole(tePlayer, empire);
                DiscordBot.giveUserEmpireOwnerRole(tePlayer);
            });
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
        owner = UUID.fromString(document.getString("owner"));
        color = Color.valueOf(document.getString("color"));
        discordRoleId = document.getString("discord_id");

        final Document homeLocationDocument = document.get("home", Document.class);
        homeLocation = new Location(
            Bukkit.getWorld(homeLocationDocument.getString("world")),
            homeLocationDocument.getDouble("x"),
            homeLocationDocument.getDouble("y"),
            homeLocationDocument.getDouble("z")
        );

        final Document lawDocument = document.get("laws", Document.class);
        for (final String lawName : lawDocument.keySet())
            laws.put(lawName, new Law(lawDocument.get(lawName, Document.class)));

        final Document memberDebtDocument = document.get("debt", Document.class);
        for (final Map.Entry<String, Object> entry : memberDebtDocument.entrySet())
            memberDebt.put(UUID.fromString(entry.getKey()), (Double) entry.getValue());

        for (final String memberUUID : document.getList("members", String.class))
            members.add(TEPlayer.getTEPlayer(UUID.fromString(memberUUID)));

        final Document positionsDocument = document.get("positions", Document.class);
        for (final String positionName : positionsDocument.keySet())
            positions.put(positionName, new Position(positionsDocument.getList(positionName, String.class)));
    }

    public static Empire getEmpire(String name) {
        // check cache for empire
        for (Empire empire : empireCache.values()) {
            if (empire.getName().equals(name))
                return empire;
        }
        return null;
    }

    public static Empire getEmpire(ObjectId id) {
        // check cache for empire
        if (empireCache.containsKey(id))
            return empireCache.get(id);
        return null;
    }

    public static Collection<Empire> getEmpires() {
        return empireCache.values();
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
        DrawEmpire.updateEmpireChunkDescriptions(this);
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
        DrawEmpire.updateEmpireChunkDescriptions(this);
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
        DrawEmpire.updateEmpireChunkDescriptions(this);
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
            if (member.getPositionName() != null
                    && member.getPositionName().equals(position)) {
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

    public void setOwner(UUID owner) {
        this.owner = owner;
        save(new Document("owner", owner.toString()));
        DrawEmpire.updateEmpireChunkDescriptions(this);
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<Map.Entry<String, Law>> getLaws() {
        return laws.entrySet();
    }

    public Law getLaw(String name) {
        return laws.get(name);
    }

    private void putLaw(String name, Law law) {
        laws.put(name, law);
        collection.updateOne(
            new Document("_id", id),
            new Document(
                "$set",
                new Document(
                    "laws." + name,
                    law.toDocument()
                )
            )
        );
    }

    public void editLaw(String name, List<String> pages) {
        putLaw(name, new Law(pages, getLaw(name).getAuthor()));
    }

    public void addLaw(String name, String author, List<String> pages) {
        putLaw(name, new Law(pages, author));
    }

    public void removeLaw(String name) {
        laws.remove(name);
        collection.updateOne(
            new Document("_id", id),
            new Document("$unset", new Document("laws." + name, 1))
        );
    }

    public void tax(Double amount) {
        members.forEach(m -> {
            final Double debt = memberDebt.get(m.getPlayerUUID());
            memberDebt.put(
                m.getPlayerUUID(),
                (debt != null ? debt : 0) + amount);
        });

        final Document document = new Document();
        for (final Map.Entry<UUID, Double> entry : memberDebt.entrySet())
            document.put(entry.getKey().toString(), entry.getValue());

        collection.updateOne(
            new Document("_id", id),
            new Document("$set", new Document("debt", document))
        );
    }

    @Nullable
    public Double getDebt(UUID payer) {
        return memberDebt.get(payer);
    }

    public Set<Map.Entry<UUID, Double>> getDebtEntries() {
        return memberDebt.entrySet();
    }

    public void addDebt(UUID payer, double amount) {
        final double debt = (memberDebt.containsKey(payer) ? memberDebt.get(payer) : 0) + amount;
        memberDebt.put(payer, debt);
        collection.updateOne(
            new Document("_id", id),
            new Document("$set", new Document("debt." + payer.toString(), debt))
        );
    }

    public void removeDebt(UUID payer, double amount) {
        double debt = memberDebt.get(payer) - amount;
        if (debt == 0)
            memberDebt.remove(payer);
        else
            memberDebt.put(
                payer,
                debt
            );
        collection.updateOne(
            new Document("_id", id),
            new Document("$set", new Document("debt." + payer.toString(), debt))
        );
    }

    public Location getHomeLocation() {
        return homeLocation;
    }

    public void setHomeLocation(Location homeLocation) throws NullPointerException {
        final World world = homeLocation.getWorld();
        if (world == null)
            throw new NullPointerException("Fetched world as null from argument location");

        this.homeLocation = homeLocation;
        collection.updateOne(
            new Document("_id", id),
            new Document(
                "$set",
                new Document("home.world", homeLocation.getWorld().getName())
                    .append("home.x", homeLocation.getX())
                    .append("home.y", homeLocation.getY())
                    .append("home.z", homeLocation.getZ())
            )
        );
    }

    public String getDiscordRoleId() {
        return discordRoleId;
    }


}
