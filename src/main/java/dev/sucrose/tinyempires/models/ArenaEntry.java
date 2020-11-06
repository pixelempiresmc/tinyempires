package dev.sucrose.tinyempires.models;

import dev.sucrose.tinyempires.commands.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArenaEntry {

    /*
    private final static Float BASE_ARENA_COST = 0.2f;
    private final static Map<ArenaType, ChatColor> ARENA_COLORS = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, List<Location>> ARENA_SPAWN_LOCATIONS = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, Integer> ARENA_PLAYER_LIMITS = new EnumMap<>(ArenaType.class);

    private final static Map<ArenaType, List<UUID>> arenaPlayerParticipants = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, Integer> arenaPlayersLeft = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, List<Integer>> spawnLocationsLeftInArena = new EnumMap<>(ArenaType.class);
    private final static Set<ArenaType> activeArenas = new HashSet<>();
    private final static Map<ArenaType, Integer> arenaCountDownTasks = new EnumMap<>(ArenaType.class);
    private final static Map<UUID, Integer> playerToArenaSpawnLocation = new HashMap<>();
    private final static Map<ArenaType, CoordinatePlane> ENTER_ARENA_COORDS = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, Location> ARENA_TO_START_LOCATION = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, Float> arenaToRewardCoins = new EnumMap<>(ArenaType.class);

    private final static Map<UUID, ArenaPlayerEntry> playerToOriginalState = new HashMap<>();
    private final static Random RANDOM = new Random();
     */

    public final static float ARENA_COST = 0.2f;

    private final ChatColor color;
    private final List<Location> spawnLocations;
    private final List<Integer> remainingSpawnLocationIndexes = new ArrayList<>();
    private final List<UUID> players = new ArrayList<>();
    private final int playerLimit;
    private final CoordinatePlane entrancePlane;
    private final Location startLocation;
    private int countdownTask;
    private boolean isCountingDown = false;
    private boolean isActive = false;
    private int playersLeft;

    public ArenaEntry(ChatColor color, List<Location> spawnLocations, int playerLimit,
                      CoordinatePlane entrancePlane, Location startLocation) {
        this.color = color;
        this.spawnLocations = spawnLocations;
        resetRemainingSpawnLocationIndexes();
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

    public List<Location> getSpawnLocations() {
        return spawnLocations;
    }

    public CoordinatePlane getEntrancePlane() {
        return entrancePlane;
    }

    public ChatColor getColor() {
        return color;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public boolean isCountingDown() {
        return isCountingDown;
    }

    public void setCountingDown(boolean countingDown) {
        isCountingDown = countingDown;
    }

    public int getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(int countdownTask) {
        this.countdownTask = countdownTask;
    }

    public boolean isActive() {
        return isActive;
    }

    public void decrementPlayersLeft() {
        playersLeft--;
    }

    public int getPlayersLeft() {
        return playersLeft;
    }

    public List<Integer> getRemainingSpawnLocationIndexes() {
        return remainingSpawnLocationIndexes;
    }

    public void addRemainingSpawnLocationIndex(int index) {
        this.remainingSpawnLocationIndexes.add(index);
    }

    public void removeRemainingSpawnLocationIndex(int index) {
        // use object wrapper to remove object and not at index
        this.remainingSpawnLocationIndexes.remove(Integer.valueOf(index));
    }

    public void resetRemainingSpawnLocationIndexes() {
        remainingSpawnLocationIndexes.clear();
        for (int i = 0; i < spawnLocations.size(); i++)
            remainingSpawnLocationIndexes.add(i);
    }

    public void start() {
        isActive = true;
        playersLeft = players.size();
    }

}
