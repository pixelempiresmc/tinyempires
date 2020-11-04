package dev.sucrose.tinyempires.commands.arena;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.BoundUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class Arena implements CommandExecutor, Listener {

    enum ArenaType {
        WATER,
        MOUNTAIN
    }

    private final static String ERROR_OPTIONS = ChatColor.RED + String.format(
        "/arena <join|start|list|cancel> <%s>",
        Arrays.stream(ArenaType.values())
            .map(a -> a.name().toLowerCase())
            .collect(Collectors.joining("/"))
    );

    private final static Map<ArenaType, ChatColor> ARENA_COLORS = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, List<Location>> ARENA_SPAWN_LOCATIONS = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, Integer> ARENA_PLAYER_LIMITS = new EnumMap<>(ArenaType.class);

    // TODO: Move arena representation and uuid mappings to classes
    private final static Map<ArenaType, List<UUID>> arenaPlayerParticipants = new EnumMap<>(ArenaType.class);
    private final static Map<ArenaType, Integer> arenaPlayersLeft = new EnumMap<>(ArenaType.class);
    private final static Set<ArenaType> activeArenas = new HashSet<>();
    private final static Map<ArenaType, Integer> arenaCountDownTasks = new EnumMap<>(ArenaType.class);
    private final static Map<UUID, Location> playerToLocationBeforeArena = new HashMap<>();
    private final static Map<UUID, Inventory> playerToLastInventory = new HashMap<>();

    static {
        // arena colors
        ARENA_COLORS.put(ArenaType.WATER, ChatColor.AQUA);
        ARENA_COLORS.put(ArenaType.MOUNTAIN, ChatColor.DARK_GREEN);

        // arena player limits
        ARENA_PLAYER_LIMITS.put(ArenaType.WATER, 7);
        ARENA_PLAYER_LIMITS.put(ArenaType.MOUNTAIN, 4);

        // arena spawn locations
        final World world = Bukkit.getWorld("world");
        final List<Location> waterArenaSpawnLocations = new ArrayList<>();
        waterArenaSpawnLocations.add(new Location(world, 8735, 50, 781));
        waterArenaSpawnLocations.add(new Location(world, 8749, 50, 776));
        waterArenaSpawnLocations.add(new Location(world, 8750, 50, 757));
        waterArenaSpawnLocations.add(new Location(world, 8733, 50, 755));
        waterArenaSpawnLocations.add(new Location(world, 8724, 50, 767));
        waterArenaSpawnLocations.add(new Location(world, 8739, 57, 767));
        waterArenaSpawnLocations.add(new Location(world, 8737, 30, 767));
        ARENA_SPAWN_LOCATIONS.put(
            ArenaType.WATER,
            waterArenaSpawnLocations
        );
    }

    private void sendArenaPlayerListMessage(ArenaType arena, Player player) {
        for (final UUID pUUID : arenaPlayerParticipants.get(arena)) {
            final Player p = Bukkit.getPlayer(pUUID);
            if (p == null)
                throw new NullPointerException("Could not fetch player with UUID " + pUUID);
            player.sendMessage(ChatColor.GREEN + " - " + p.getName());
        }
    }

    private void broadcastToPlayerUUIDList(List<UUID> uuids, String message) {
        for (final UUID uuid : uuids) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                throw new NullPointerException("Could not fetch player with UUID " + uuid);
            player.sendMessage(message);
        }
    }

    private void sendPlayerTitleAndChatMessage(List<UUID> uuids, String title, String message) {
        uuids.forEach(pUUID -> {
            final Player p = Bukkit.getPlayer(pUUID);
            if (p == null)
                throw new NullPointerException("Could not fetch player with UUID " + pUUID);
            p.sendTitle(title, "", 10, 70, 20);
            p.sendMessage(message);
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /arena <option> <arena>
        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
        if (tePlayer == null) {
            sender.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ERROR_OPTIONS);
            return false;
        }

        final String arenaName = args[1];
        ArenaType arena;
        try {
            arena = ArenaType.valueOf(arenaName);
        } catch (Exception ignore) {
            sender.sendMessage(ERROR_OPTIONS);
            return false;
        }

        if (activeArenas.contains(arena)) {
            if (arenaPlayerParticipants.get(arena).contains(uuid))
                sender.sendMessage(ChatColor.RED + "You're already playing in the arena");
            else
                sender.sendMessage(ChatColor.RED + String.format(
                    "%s is currently in use",
                    arenaName
                ));
            return false;
        }

        // arena is not currently in use
        final String option = args[0];
        final List<UUID> playersInArena = arenaPlayerParticipants.getOrDefault(arena, null);
        switch (option) {
            case "join":
                final int playerLimit = ARENA_PLAYER_LIMITS.get(arena);
                if (playersInArena.size() > playerLimit) {
                    sender.sendMessage(ChatColor.RED + String.format(
                        "The %s arena is full! (%d/%d)",
                        "" + ARENA_COLORS.get(arena) + ChatColor.BOLD + arena.name().toLowerCase() + ChatColor.RED,
                        playerLimit,
                        playerLimit
                    ));
                    return false;
                }

                // add player to arena player listing
                playersInArena.add(uuid);
                arenaPlayerParticipants.put(arena, playersInArena);

                // teleport and put location
                playerToLocationBeforeArena.put(uuid, player.getLocation());
                playerToLastInventory.put(uuid, player.getInventory());
                player.getInventory().clear();

                player.teleport(ARENA_SPAWN_LOCATIONS.get(arena).get(playersInArena.size() - 1));

                // broadcast join message in arena
                broadcastToPlayerUUIDList(playersInArena, ChatColor.GREEN + String.format(
                    "%s has joined the arena! (%d/%d)",
                    ChatColor.BOLD + player.getName() + ChatColor.GREEN,
                    playersInArena.size(),
                    ARENA_PLAYER_LIMITS.get(arena)
                ));

                // send player list of arena participants
                player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + String.format(
                    "Joined the %s arena!",
                    "" + ARENA_COLORS.get(arena) + ChatColor.BOLD + arena.name().toLowerCase() + ChatColor.RED
                ));
                player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Players in arena:");
                sendArenaPlayerListMessage(arena, player);
                return false;
            case "list":
                sender.sendMessage(ChatColor.GREEN + String.format(
                    "%d player%s in arena",
                    playersInArena.size(),
                    playersInArena.size() > 1 ? "s" : ""
                ));
                sendArenaPlayerListMessage(arena, player);
                return false;
            case "cancel":
                if (!arenaCountDownTasks.containsKey(arena)) {
                    sender.sendMessage(ChatColor.RED + "Your match must be starting to cancel it");
                    return false;
                }
                Bukkit.getScheduler().cancelTask(arenaCountDownTasks.get(arena));
                broadcastToPlayerUUIDList(playersInArena, ChatColor.RED + String.format(
                    "%s cancelled the match",
                    ChatColor.BOLD + player.getName() + ChatColor.RED
                ));
                return false;
            case "start":
                if (!playersInArena.contains(uuid)) {
                    sender.sendMessage(ChatColor.RED + String.format(
                        "You must be in the %s arena to start its event!",
                        "" + ARENA_COLORS.get(arena) + ChatColor.BOLD + arena.name().toLowerCase() + ChatColor.RED
                    ));
                    return false;
                }

                if (playersInArena.size() == 1) {
                    sender.sendMessage(ChatColor.RED + "At least 2 people must be in the arena to start");
                    return false;
                }

                if (activeArenas.contains(arena)) {
                    sender.sendMessage(ChatColor.RED + "The match is currently ongoing");
                    return false;
                }

                // checks passed, start arena battle
                broadcastToPlayerUUIDList(playersInArena, ChatColor.BOLD + String.format(
                    "%s has started the match!",
                    player.getName()
                ));

                arenaCountDownTasks.put(arena,
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(
                        TinyEmpires.getInstance(),
                        new Runnable() {
                            private int timer = 5;

                            @Override
                            public void run() {
                                broadcastToPlayerUUIDList(playersInArena, ChatColor.GREEN + String.format(
                                    "%d seconds left until the match starts...",
                                    timer
                                ));

                                if (timer == 0) {
                                    Bukkit.getScheduler().cancelTask(arenaCountDownTasks.get(arena));
                                    activeArenas.add(arena);
                                    sendPlayerTitleAndChatMessage(
                                        playersInArena,
                                        "" + ChatColor.GREEN + ChatColor.BOLD + "Start!",
                                        ChatColor.GREEN + "The match has started!"
                                    );
                                    arenaPlayersLeft.put(arena, playersInArena.size());
                                    return;
                                }

                                // send countdown titles and chat messages
                                sendPlayerTitleAndChatMessage(
                                    playersInArena,
                                    "" + ChatColor.YELLOW + ChatColor.BOLD + timer,
                                    ChatColor.GREEN + String.format(
                                        "%d second%s until match starts...",
                                        timer,
                                        timer > 1 ? "s" : ""
                                    )
                                );
                                timer--;
                            }
                        },
                        0,
                        20
                    )
                );
                activeArenas.add(arena);
                return false;
            default:
                sender.sendMessage(ERROR_OPTIONS);
                return false;
        }
    }

    @Nullable
    private ArenaType getPlayerArena(UUID uuid) {
        for (final Map.Entry<ArenaType, List<UUID>> entry : arenaPlayerParticipants.entrySet()) {
            if (entry.getValue().contains(uuid))
                return entry.getKey();
        }
        return null;
    }

    @EventHandler
    public void onPlayerDeath(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            if (BoundUtils.inBoundsOfWaterArena(event.getEntity().getLocation().getBlockX(),
                event.getEntity().getLocation().getBlockZ()))
                event.setCancelled(true);
            return;
        }
        final Player player = (Player) event.getEntity();
        final UUID uuid = player.getUniqueId();

        final ArenaType arena = getPlayerArena(uuid);
        if (arena != null) {
            player.getWorld().strikeLightningEffect(player.getLocation());
            event.setCancelled(true);
            player.setGameMode(GameMode.SPECTATOR);
            final List<UUID> playersInArena = arenaPlayerParticipants.get(arena);
            final int playersLeft = arenaPlayersLeft.get(arena) - 1;
            broadcastToPlayerUUIDList(playersInArena, ChatColor.YELLOW + String.format(
                "%s has died!%s",
                ChatColor.BOLD + player.getName() + ChatColor.YELLOW,
                playersLeft > 1
                    ? playersLeft + " players left!"
                    : ""
            ));

            if (playersLeft == 0) {
                broadcastToPlayerUUIDList(playersInArena, ChatColor.GREEN + String.format(
                    "%s has won the match!",
                    event.getDamager().getName()
                ));

                for (final UUID pUUID : playersInArena) {
                    final Player p = Bukkit.getPlayer(pUUID);
                    if (p == null)
                        throw new NullPointerException("Could not fetch player with UUID " + pUUID);
                    player.teleport(playerToLocationBeforeArena.get(pUUID));
                    playerToLocationBeforeArena.remove(pUUID);
                }
                arenaPlayerParticipants.remove(arena);
                arenaPlayersLeft.remove(arena);
                return;
            }

            arenaPlayersLeft.put(arena, playersLeft);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        final ArenaType arena = getPlayerArena(uuid);
        if (arena == null)
            return;

        broadcastToPlayerUUIDList(arenaPlayerParticipants.get(arena), ChatColor.YELLOW + String.format(
            "%s has left the arena!",
            ChatColor.BOLD + player.getName() + ChatColor.YELLOW
        ));
        player.teleport(playerToLocationBeforeArena.get(uuid));
        // TODO: Player inventory reset
        playerToLocationBeforeArena.remove(uuid);
    }

    @EventHandler
    public static void onPlayerMove(PlayerMoveEvent event) {
        // restrain player movement if waiting in arena
        final Location to = event.getTo();
        final Location from = event.getFrom();
        if (to == null)
            throw new NullPointerException("Could not fetch PlayerMoveEvent#getTo() in Arena PlayerMove listener");

        boolean isPlayerInInactiveArena = false;
        for (final ArenaType arena : ArenaType.values()) {
            if (arenaPlayerParticipants.get(arena).contains(event.getPlayer().getUniqueId())) {
                isPlayerInInactiveArena = !activeArenas.contains(arena);
                break;
            }
        }
        if (isPlayerInInactiveArena
                && (to.getX() != from.getX()
                || to.getY() != from.getY()
                || to.getZ() != from.getZ()))
            event.setCancelled(true);
    }

}
