package dev.sucrose.tinyempires.models;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.*;

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

    private final ChatColor color;
    private final List<Location> spawnLocations;
    private final List<Integer> remainingSpawnLocationIndexes = new ArrayList<>();
    private final Map<UUID, Integer> playerToSpawnLocationIndex = new HashMap<>();
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> playersLeft = new ArrayList<>();
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

    public List<UUID> getPlayers() {
        return players;
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public void removePlayerFromOngoingMatch(UUID uuid) {
        playersLeft.remove(uuid);
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

    public List<UUID> getPlayersLeft() {
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
    }

}
