package dev.sucrose.tinyempires.models;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.*;

public class ArenaEntry {

    private final ChatColor color;
    private final List<Location> spawnLocations;
    private final List<Integer> remainingSpawnLocationIndexes = new ArrayList<>();
    private final Map<UUID, Integer> playerToSpawnLocationIndex = new HashMap<>();
    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> playersLeft = new HashSet<>();
    private final int playerLimit;
    private final BoundsPlane entrancePlane;
    private final Location startLocation;
    private int countdownTask;
    private boolean isCountingDown = false;
    private boolean isActive = false;
    private static final Random RANDOM = new Random();

    public ArenaEntry(ChatColor color, List<Location> spawnLocations, int playerLimit,
                      BoundsPlane entrancePlane, Location startLocation) {
        this.color = color;
        this.spawnLocations = spawnLocations;
        resetRemainingSpawnLocations();
        this.playerLimit = playerLimit;
        this.entrancePlane = entrancePlane;
        this.startLocation = startLocation;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public int getPlayerLimit() {
        return playerLimit;
    }

    public BoundsPlane getEntrancePlane() {
        return entrancePlane;
    }

    public ChatColor getColor() {
        return color;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public void resetRemainingSpawnLocations() {
        for (int i = 0; i < spawnLocations.size(); i++)
            remainingSpawnLocationIndexes.add(i);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
        remainingSpawnLocationIndexes.add(playerToSpawnLocationIndex.get(uuid));
        playerToSpawnLocationIndex.remove(uuid);
    }

    public boolean isCountingDown() {
        return isCountingDown;
    }

    public int getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(int countdownTask) {
        this.countdownTask = countdownTask;
        isCountingDown = true;
    }

    public boolean isActive() {
        return isActive;
    }

    public void removePlayerFromPlayersLeft(UUID uuid) {
        playersLeft.remove(uuid);
    }

    public Set<UUID> getPlayersLeft() {
        return playersLeft;
    }

    public Location getRandomSpawnLocationForPlayer(UUID player) {
        final int index = RANDOM.nextInt(remainingSpawnLocationIndexes.size());
        final int spawnLocationIndex = remainingSpawnLocationIndexes.get(index);
        remainingSpawnLocationIndexes.remove(index);
        playerToSpawnLocationIndex.put(player, spawnLocationIndex);
        return spawnLocations.get(spawnLocationIndex);
    }

    public void start() {
        isActive = true;
        isCountingDown = false;
        playersLeft.addAll(players);
    }

    public void end() {
        isActive = false;
        resetRemainingSpawnLocations();
    }

}
