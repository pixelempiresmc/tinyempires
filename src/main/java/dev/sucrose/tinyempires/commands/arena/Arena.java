package dev.sucrose.tinyempires.commands.arena;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.BoundUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Arena implements CommandExecutor, Listener {

    private final static String ERROR_OPTIONS = ChatColor.RED + String.format(
        "/arena <join/start/list/cancel> <%s>",
        Arrays.stream(ArenaType.values())
            .map(a -> a.name().toLowerCase())
            .collect(Collectors.joining("/"))
    );

    private final static Float BASE_ARENA_COST = 0.2f;
    private final static Map<UUID, ArenaPlayerEntry> playerArenaEntries = new HashMap<>();
    private final static Map<ArenaType, ArenaEntry> arenaEntries = new HashMap<>();
    private final static Random RANDOM = new Random();

    static {
        // water arena
        final World world = Bukkit.getWorld("world");
        final List<Location> waterArenaSpawnLocations = new ArrayList<>();
        waterArenaSpawnLocations.add(new Location(world, 8746.5, 48, 776.5, 142, 0));
        waterArenaSpawnLocations.add(new Location(world, 8734.5, 47, 782.5, -163, 0));
        waterArenaSpawnLocations.add(new Location(world, 8724.5, 47, 767.5, -90, 0));
        waterArenaSpawnLocations.add(new Location(world, 8734.5, 48, 754.5, -21, 0));
        waterArenaSpawnLocations.add(new Location(world, 8750.5, 48, 758.5, 52, 0));
        waterArenaSpawnLocations.add(new Location(world, 8739.5, 54, 767.5, 0, 0));
        arenaEntries.put(
            ArenaType.WATER,
            new ArenaEntry(
                ChatColor.AQUA,
                waterArenaSpawnLocations,
                7,
                new CoordinatePlane(8736, 74, 765, 8750, 75, 769),
                new Location(world, 8738.5, 77, 764.5)
            )
        );
    }

    private void sendArenaPlayerListMessage(ArenaType arena, Player player) {
        for (final UUID pUUID : arenaEntries.get(arena).getPlayers()) {
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

    private void setItemUnbreakable(ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null)
            throw new NullPointerException("Could not get meta of item");

        meta.setUnbreakable(true);
        item.setItemMeta(meta);
    }

    private void setItemsUnbreakable(ItemStack... items) {
        for (final ItemStack item : items)
            setItemUnbreakable(item);
    }

    private void setPlayerArenaInventory(ArenaType arena, Player player) {
        final PlayerInventory inventory = player.getInventory();
        inventory.clear();
        switch (arena) {
            case WATER:
                final ItemStack trident = new ItemStack(Material.TRIDENT);
                trident.addEnchantment(Enchantment.LOYALTY, 3);
                setItemUnbreakable(trident);
                inventory.setItem(0, trident);
                player.getInventory().setHeldItemSlot(0);
                player.updateInventory();
                break;
            case MOUNTAIN:
                final ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                final ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
                final ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
                final ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                final ItemStack shield = new ItemStack(Material.SHIELD);
                final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                final ItemStack axe = new ItemStack(Material.DIAMOND_SWORD);
                final ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                final ItemStack bow = new ItemStack(Material.BOW);
                final ItemStack arrows = new ItemStack(Material.ARROW, 8);
                setItemsUnbreakable(helmet, chestplate, leggings, boots, shield, sword, axe, crossbow, bow, arrows);

                final ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE, 1);
                inventory.setHelmet(helmet);
                inventory.setChestplate(chestplate);
                inventory.setLeggings(leggings);
                inventory.setBoots(boots);
                inventory.setItemInOffHand(shield);
                inventory.setItem(0, sword);
                inventory.setItem(1, axe);
                inventory.setItem(2, bow);
                inventory.setItem(3, crossbow);
                inventory.setItem(4, goldenApple);
                inventory.setItem(5, arrows);
                player.updateInventory();
                break;
            default:
                throw new IllegalArgumentException(arena.name() + " is not a matching arena type");
        }
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

        if (args.length < 1) {
            sender.sendMessage(ERROR_OPTIONS);
            return false;
        }

        final String option = args[0];
        final ArenaPlayerEntry arenaPlayerEntry = playerArenaEntries.get(uuid);
        if (arenaPlayerEntry == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an arena to run this command");
            return false;
        }

        final ArenaType senderArena = arenaPlayerEntry.getArena();
        final ArenaEntry arenaEntry = arenaEntries.get(senderArena);
        final List<UUID> playersInArena = arenaEntries.get(senderArena).getPlayers();
        switch (option) {
            case "list":
                sender.sendMessage(ChatColor.GREEN + String.format(
                    "%d player%s in arena",
                    playersInArena.size(),
                    playersInArena.size() > 1 ? "s" : ""
                ));
                sendArenaPlayerListMessage(senderArena, player);
                return false;
            case "cancel":
                if (!arenaEntry.isActive()) {
                    sender.sendMessage(ChatColor.RED + "Your match must be starting to cancel it");
                    return false;
                }
                Bukkit.getScheduler().cancelTask(arenaEntry.getCountdownTask());
                broadcastToPlayerUUIDList(playersInArena, ChatColor.RED + String.format(
                    "%s cancelled the match",
                    ChatColor.BOLD + player.getName() + ChatColor.RED
                ));
                return false;
            case "leave":
                if (arenaEntry.isActive()
                        || arenaEntry.isCountingDown()) {
                    sender.sendMessage(ChatColor.RED + "You cannot leave while the match is ongoing");
                    return false;
                }
                broadcastToPlayerUUIDList(playersInArena, ChatColor.YELLOW + String.format(
                    "%s has left the arena",
                    ChatColor.BOLD + sender.getName() + ChatColor.YELLOW
                ));
                arenaEntry.removePlayer(player.getUniqueId());
                playerArenaEntries.get(uuid).restore(player);
                playerArenaEntries.remove(uuid);
                player.teleport(arenaEntry.getStartLocation());
                tePlayer.giveCoins(BASE_ARENA_COST);
                player.setGameMode(GameMode.SURVIVAL);
                return false;
            case "start":
                if (!playersInArena.contains(uuid)) {
                    sender.sendMessage(ChatColor.RED + String.format(
                        "You must be in the %s arena to start its event!",
                        "" + arenaEntry.getColor() + ChatColor.BOLD + senderArena.name().toLowerCase() + ChatColor.RED
                    ));
                    return false;
                }

                if (arenaEntry.isCountingDown()) {
                    sender.sendMessage(ChatColor.RED + "Arena match is already counting down");
                    return false;
                }

                if (playersInArena.size() == 1) {
                    sender.sendMessage(ChatColor.RED + "At least 2 people must be in the arena to start");
                    return false;
                }

                if (arenaEntry.isActive()) {
                    sender.sendMessage(ChatColor.RED + "The match is currently ongoing");
                    return false;
                }

                // checks passed, start arena battle
                broadcastToPlayerUUIDList(playersInArena, ChatColor.BOLD + String.format(
                    "%s has started the match!",
                    player.getName()
                ));

                arenaEntry.setCountdownTask(
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(
                        TinyEmpires.getInstance(),
                        new Runnable() {
                            private int timer = 5;

                            @Override
                            public void run() {
                                if (timer == 0) {
                                    Bukkit.getScheduler().cancelTask(arenaEntry.getCountdownTask());
                                    arenaEntry.start();
                                    sendPlayerTitleAndChatMessage(
                                        playersInArena,
                                        "" + ChatColor.GREEN + ChatColor.BOLD + "Start!",
                                        ChatColor.GREEN + "The match has started!"
                                    );
                                    player.addPotionEffect(
                                        new PotionEffect(
                                            PotionEffectType.DOLPHINS_GRACE,
                                            Integer.MAX_VALUE,
                                            255,
                                            true,
                                            false,
                                            true
                                        )
                                    );
                                    // resistance = 20% * 3 = 60% less damage
                                    player.addPotionEffect(
                                        new PotionEffect(
                                            PotionEffectType.DAMAGE_RESISTANCE,
                                            Integer.MAX_VALUE,
                                            3,
                                            true,
                                            false,
                                            true
                                        )
                                    );
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
                return false;
            default:
                sender.sendMessage(ERROR_OPTIONS);
                return false;
        }
//
//        final String arenaName = args[1];
//        ArenaType arena;
//        try {
//            arena = ArenaType.valueOf(arenaName.toUpperCase());
//        } catch (Exception ignore) {
//            sender.sendMessage(ERROR_OPTIONS);
//            return false;
//        }
//
//        if (activeArenas.contains(arena)) {
//            if (arenaPlayerParticipants.get(arena).contains(uuid))
//                sender.sendMessage(ChatColor.RED + "You're already playing in the arena");
//            else
//                sender.sendMessage(ChatColor.RED + String.format(
//                    "%s is currently in use",
//                    arenaName
//                ));
//            return false;
//        }
    }

//    @Nullable
//    private ArenaType getPlayerArena(UUID uuid) {
//        for (final Map.Entry<ArenaType, List<UUID>> entry : arenaPlayerParticipants.entrySet()) {
//            if (entry.getValue().contains(uuid))
//                return entry.getKey();
//        }
//        return null;
//    }

    @EventHandler
    public static void onPlayerDeath(PlayerDeathEvent event) {
        // cancel drops on player death if in arena, cannot be done with entity damage event
        final Player player = event.getEntity();
        if (playerArenaEntries.containsKey(player.getUniqueId()))
            event.getDrops().clear();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        final Entity victim = event.getEntity();
        System.out.println("Victim: " + victim.getType() + ", " + victim.getName());
        if (!(victim instanceof Player)) {
            if (BoundUtils.inBoundsOfWaterArena(
                victim.getLocation().getBlockX(),
                victim.getLocation().getBlockZ()
            ))
                event.setCancelled(true);
            return;
        }

        // cancel if damager was not a trident or player (e.g. pufferfish)
        if (event.getDamager().getType() != EntityType.TRIDENT
                || event.getDamager().getType() != EntityType.PLAYER)
            event.setCancelled(true);

        final Player player = (Player) victim; // get player that was damaged
        System.out.println("Player Victim: " + player.getName());

        final UUID uuid = player.getUniqueId();
        final ArenaPlayerEntry playerEntry = playerArenaEntries.get(uuid);
        if (playerEntry == null
                || !arenaEntries.get(playerEntry.getArena()).isActive()) {
            // cancel event if player is waiting on coral block for event to start
            event.setCancelled(true);
            return;
        }

        // prevent player from dying
        event.setCancelled(true);

        // lightning effect
        player.getWorld().strikeLightningEffect(player.getLocation());

        final ArenaEntry arenaEntry = arenaEntries.get(playerEntry.getArena());
        // teleport to top
        player.teleport(arenaEntry.getStartLocation());

        // restore health to full
        player.setHealth(20);

        System.out.println("Removing uuid: " + uuid);
        arenaEntry.removePlayerFromOngoingMatch(uuid);
        System.out.println("Players left: " + arenaEntry.getPlayersLeft().toString());
        final int playersLeft = arenaEntry.getPlayersLeft().size();
        System.out.println("Players left count: " + playersLeft);
        broadcastToPlayerUUIDList(arenaEntry.getPlayers(), ChatColor.YELLOW + String.format(
            "%s has died! %s",
            ChatColor.BOLD + player.getName() + ChatColor.YELLOW,
            playersLeft > 1
                ? playersLeft + " players left!"
                : ""
        ));

        if (playersLeft == 1) {
            final float reward = arenaEntry.getPlayers().size() * ArenaEntry.ARENA_COST;
            final UUID winnerUUID = arenaEntry.getPlayersLeft().get(0);
            final Player winner = Bukkit.getPlayer(winnerUUID);
            if (winner == null)
                throw new NullPointerException("Could not get winner with UUID: " + winnerUUID);
            System.out.println("Fetched winner: " + winner.getName());

            broadcastToPlayerUUIDList(arenaEntry.getPlayers(), ChatColor.GREEN + String.format(
                "%s has won the match and has won %.1f coins!",
                winner.getName(),
                reward
            ));

            final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
            if (tePlayer == null)
                throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            tePlayer.giveCoins(reward);

            for (final UUID pUUID : arenaEntry.getPlayers()) {
                final Player p = Bukkit.getPlayer(pUUID);
                if (p == null)
                    throw new NullPointerException("Could not fetch player with UUID " + pUUID);
                playerArenaEntries.get(uuid).restore(player);
                playerArenaEntries.remove(uuid);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    @EventHandler
    public void onPlayerHunger(FoodLevelChangeEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        final ArenaPlayerEntry playerEntry = playerArenaEntries.get(uuid);
        // if player is in an arena and lost hunger then cancel
        if (playerEntry != null
                && ((Player) event.getEntity()).getFoodLevel() - event.getFoodLevel() > 0)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (!playerArenaEntries.containsKey(uuid))
            return;
        final ArenaPlayerEntry playerEntry = playerArenaEntries.get(uuid);
        final ArenaEntry arenaEntry = arenaEntries.get(playerEntry.getArena());

        broadcastToPlayerUUIDList(arenaEntry.getPlayers(), ChatColor.YELLOW + String.format(
            "%s has left the arena!",
            ChatColor.BOLD + player.getName() + ChatColor.YELLOW
        ));

        // restore original location, inventory and exp
        playerArenaEntries.get(uuid).restore(player);
        playerArenaEntries.remove(uuid);
        arenaEntry.removePlayer(uuid);
        player.setGameMode(GameMode.SURVIVAL);

        final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
        if (tePlayer == null)
            throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
        tePlayer.giveCoins(BASE_ARENA_COST);
    }

    private void restoreFractionOfBoostCharge(Player player) {
        player.setExp(player.getExp() + 0.99F / 60);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        // boosts for water arena
        final Player player = event.getPlayer();
        final ArenaPlayerEntry arenaEntry = playerArenaEntries.get(player.getUniqueId());
        if (arenaEntry == null)
            return;

        // only for water arena and players remaining in match
        if (arenaEntry.getArena() == ArenaType.WATER
                && arenaEntries.get(arenaEntry.getArena()).getPlayersLeft().contains(player.getUniqueId())) {
            event.setCancelled(true);
            if (Float.compare(player.getExp(), 0.98F) > 0) {
                final Vector velocity = player.getLocation().getDirection().multiply(1.5);
                player.setVelocity(velocity);
                player.setExp(0);

                // 60 ticks = one boost every 3 seconds
                for (int i = 0; i < 60; i++)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(
                        TinyEmpires.getInstance(),
                        () -> restoreFractionOfBoostCharge(player),
                        i + 1
                    );
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Location to = event.getTo();
        final Location from = event.getFrom();
        // #getFrom is not null if #getTo isn't
        if (to == null)
            throw new NullPointerException("Could not get player location");

        final ArenaPlayerEntry arenaPlayerEntry = playerArenaEntries.get(event.getPlayer().getUniqueId());
        if (arenaPlayerEntry != null) {
            final ArenaEntry arenaEntry = arenaEntries.get(arenaPlayerEntry.getArena());
            if (!arenaEntry.isActive()
                    && (to.getX() != from.getX()
                    || to.getY() != from.getY()
                    || to.getZ() != from.getZ()))
                event.setCancelled(true);
            return;
        }

        for (final Map.Entry<ArenaType, ArenaEntry> entry : arenaEntries.entrySet()) {
            final ArenaEntry arenaEntry = entry.getValue();
            if (arenaEntry.getEntrancePlane().isInPlane(to.getBlockX(), to.getBlockY(), to.getBlockZ())) {
                final ArenaType arena = entry.getKey();
                final int playerLimit = arenaEntry.getPlayerLimit();
                final List<UUID> playersInArena = arenaEntry.getPlayers();
                final Player player = event.getPlayer();
                if (playersInArena.size() == playerLimit) {
                    player.sendMessage(ChatColor.RED + String.format(
                        "The %s arena is full! (%d/%d)",
                        "" + arenaEntry.getColor() + ChatColor.BOLD + arena.name().toLowerCase() + ChatColor.RED,
                        playerLimit,
                        playerLimit
                    ));
                    player.teleport(arenaEntry.getStartLocation());
                    return;
                }

                if (arenaEntry.isActive()
                        || arenaEntry.isCountingDown()) {
                    player.sendMessage(ChatColor.RED + "You cannot join while the match is currently ongoing!");
                    player.teleport(arenaEntry.getStartLocation());
                    return;
                }

                // cost
                final UUID uuid = player.getUniqueId();
                final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
                if (tePlayer == null)
                    throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);

                if (tePlayer.getBalance() <= BASE_ARENA_COST) {
                    player.sendMessage(ChatColor.RED + String.format(
                        "You require %.2f more coins to pay the reward fee! %.2f coins required!",
                        BASE_ARENA_COST - tePlayer.getBalance(),
                        BASE_ARENA_COST
                    ));
                    return;
                }

                // reward
                tePlayer.takeCoins(BASE_ARENA_COST);

                // teleport
                player.teleport(arenaEntry.getRandomSpawnLocationForPlayer(uuid));

                // adventure mode
                player.setGameMode(GameMode.ADVENTURE);

                // put original player data before teleporting, inventory, exp, location
                playerArenaEntries.put(
                    uuid,
                    new ArenaPlayerEntry(
                        arena,
                        player.getInventory().getContents(),
                        player.getTotalExperience()
                    )
                );

                // set arena inventory and exp
                setPlayerArenaInventory(arena, player);
                player.setLevel(0);
                player.setExp(0.99F);

                // add player
                arenaEntry.addPlayer(player.getUniqueId());

                // broadcast join message in arena
                broadcastToPlayerUUIDList(playersInArena, ChatColor.GREEN + String.format(
                    "%s has joined the %s arena! (%d/%d)",
                    ChatColor.BOLD + player.getName() + ChatColor.GREEN,
                    "" + arenaEntry.getColor() + ChatColor.BOLD + arena.name().toLowerCase() + ChatColor.GREEN,
                    playersInArena.size(),
                    arenaEntry.getPlayerLimit()
                ));

                // send player list of arena participants
                player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Players in arena:");
                sendArenaPlayerListMessage(arena, player);
                return;
            }
        }
    }

}