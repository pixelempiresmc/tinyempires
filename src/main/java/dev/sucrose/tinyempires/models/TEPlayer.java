package dev.sucrose.tinyempires.models;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import dev.sucrose.tinyempires.TinyEmpires;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TEPlayer {

    final private static Map<UUID, TEPlayer> playerCache = new HashMap<>();

    private static final MongoCollection<Document> collection = TinyEmpires.getDatabase().getCollection("players");

    private final UUID playerUUID;
    private String name;
    private double balance;
    private ObjectId empire; // empire document ID
    private String position;
    private boolean jumpedInAdvancement;

    public static TEPlayer createPlayer(UUID uuid, String name) {
        final Document document = new Document();
        document.put("uuid", uuid.toString());
        document.put("name", name);
        document.put("balance", 0d);
        document.put("empire", null);
        document.put("position", null);
        document.put("jumped_in", false);
        collection.insertOne(document);
        return getTEPlayer(uuid);
    }

    public static TEPlayer getTEPlayer(String name) {
        // check cache for player
        for (final TEPlayer p : playerCache.values()) {
            System.out.printf("%s %s", p.getPlayerUUID(), p.getName());
            if (p.getName().equals(name))
                return p;
        }
        // if cache miss fetch from mongo
        final Document document = collection.find(new Document("name", name)).first();
        if (document == null)
            return null;
        final TEPlayer tePlayer = new TEPlayer(document);
        playerCache.put(tePlayer.getPlayerUUID(), tePlayer);
        return tePlayer;
    }

    public static void clearCache() {
        playerCache.clear();
    }

    public static TEPlayer getTEPlayer(UUID uuid) {
        // check cache for player
        if (playerCache.containsKey(uuid))
            return playerCache.get(uuid);
        final Document document = collection.find(new Document("uuid", uuid.toString())).first();
        // if cache miss fetch from mongo
        if (document == null)
            return null;
        final TEPlayer tePlayer = new TEPlayer(document);
        playerCache.put(uuid, tePlayer);
        return tePlayer;
    }

    public TEPlayer(Document document) {
        this.playerUUID = UUID.fromString(document.getString("uuid"));
        this.name = document.getString("name");
        this.balance = document.getDouble("balance");
        this.empire = document.getObjectId("empire");
        this.position = document.getString("position");
        this.jumpedInAdvancement = document.getBoolean("jumped_in");
    }

    private static final ScoreboardManager manager;
    static {
        manager = Bukkit.getScoreboardManager();
        if (manager == null)
            throw new NullPointerException("Bukkit#getScoreboardManager() returned null on initialization");
    }

    public void updatePlayerScoreboard() {
        final Player player = Bukkit.getPlayer(playerUUID);
        if (player == null)
            return;

        final Scoreboard scoreboard = manager.getNewScoreboard();

        final Objective objective = scoreboard.registerNewObjective(
            "title",
            "dummy",
            "" + ChatColor.YELLOW + ChatColor.BOLD + "Tiny Empires"
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        final TEChunk chunk = TEChunk.getChunk(player.getLocation().getChunk());
        int line = 1;
        // website
        objective.getScore(ChatColor.YELLOW + "www.pixelempiresmc.net").setScore(line++);

        // spacing
        objective.getScore("").setScore(line++);

        // chunk type
        if (chunk != null
                && chunk.getType() != ChunkType.NONE)
            objective.getScore((chunk.getType() == ChunkType.TEMPLE ? ChatColor.GREEN :
                ChatColor.GOLD) + chunk.getType().name()).setScore(line++);

        // location empire
        objective.getScore(chunk == null ? ChatColor.GRAY + "Wilderness" :
            chunk.getEmpire().getChatColor() + chunk.getEmpire().getName()).setScore(line++);

        // location header
        objective.getScore(ChatColor.BOLD + "Location").setScore(line++);

        // spacing, color for uniqueness
        objective.getScore(ChatColor.RED + "").setScore(line++);

        // empire reserve
        if (empire != null) {
            objective.getScore("Reserve: " + ChatColor.GREEN + String.format("%.1f coins",
                getEmpire().getReserve())).setScore(line++);
            objective.getScore("Position: " + ChatColor.GREEN + position).setScore(line++);
        }

        // player empire
        objective.getScore(empire == null ? ChatColor.GRAY + "Unaffiliated" :
            "Name: " + getEmpire().getChatColor() + getEmpire().getName()).setScore(line++);
        objective.getScore(ChatColor.BOLD + "Empire").setScore(line++);

        // spacing, color for uniqueness
        objective.getScore("" + ChatColor.DARK_GREEN).setScore(line++);

        // balance
        objective.getScore("Balance: " + ChatColor.GREEN + String.format("%.1f coins", balance + 0.0)).setScore(line++);

        // spacing, color for uniqueness
        objective.getScore("" + ChatColor.AQUA).setScore(line);
        player.setScoreboard(scoreboard);
    }

    private void save(Document document) {
        collection.updateOne(new Document("uuid", playerUUID.toString()), new Document("$set", document));
        updatePlayerScoreboard();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        save(new Document("name", name));
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        save(new Document("balance", balance));
    }

    public void giveCoins(double amount) {
        setBalance(balance + amount);
    }

    public void takeCoins(double amount) {
        setBalance(balance - amount);
    }

    public void pay(TEPlayer player, double amount) {
        player.giveCoins(amount);
        takeCoins(amount);
    }

    public Empire getEmpire() {
        return Empire.getEmpire(empire);
    }

    public void setEmpireId(ObjectId id) {
        this.empire = id;
        save(new Document("empire", id));
    }

    public void leaveEmpire() {
        setEmpireId(null);
    }

    public boolean isInEmpire() {
        return empire != null;
    }

    public String getPositionName() {
        return position;
    }

    public Position getPosition() {
        return getEmpire().getPosition(position);
    }

    public void setPositionName(String position) {
        this.position = position;
        save(new Document("position", position));
    }

    public boolean getJumpedInAdvancement() {
        return jumpedInAdvancement;
    }

    public void setJumpInAdvancement(boolean jumpedInAdvancement) {
        this.jumpedInAdvancement = jumpedInAdvancement;
        save(new Document("jumped_in", jumpedInAdvancement));
    }


}
