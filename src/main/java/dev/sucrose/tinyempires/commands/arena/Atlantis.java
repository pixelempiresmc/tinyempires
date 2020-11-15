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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.List;

public class Atlantis implements CommandExecutor, Listener {

    private final static String ERROR_OPTIONS = ChatColor.RED + "/atlantis <join/start/list/cancel>";

    private final static Map<UUID, ArenaPlayerEntry> playerArenaEntries = new HashMap<>();
    private final static Map<UUID, UUID> playerToLastThrownTridentEntityId = new HashMap<>();
    private final static ArenaEntry arena;
    private final static Random random = new Random();

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
        arena = new ArenaEntry(
            ChatColor.AQUA,
            waterArenaSpawnLocations,
            7,
            new BoundsPlane(8736, 75, 765, 8740, 75, 769),
            new Location(world, 8738.5, 77, 764.5)
        );
    }

    private void sendArenaPlayerListMessage(Player player) {
        for (final UUID pUUID : arena.getPlayers()) {
            final Player p = Bukkit.getPlayer(pUUID);
            if (p == null)
                throw new NullPointerException("Could not fetch player with UUID " + pUUID);
            player.sendMessage(ChatColor.GREEN + " - " + p.getName());
        }
    }

    private void broadcastToPlayerUUIDList(Iterable<UUID> uuids, String message) {
        for (final UUID uuid : uuids) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                throw new NullPointerException("Could not fetch player with UUID " + uuid);
            player.sendMessage(message);
        }
    }

    private void sendPlayerTitleAndChatMessage(Iterable<UUID> uuids, String title, String message) {
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

    private void setPlayerArenaInventory(Player player) {
        // clear inventory (original player inventory is restored later)
        final PlayerInventory inventory = player.getInventory();
        inventory.clear();

        // give unbreakable trident with loyalty so it comes back
        final ItemStack trident = new ItemStack(Material.TRIDENT);
        trident.addEnchantment(Enchantment.LOYALTY, 3);
        setItemUnbreakable(trident);
        inventory.setItem(0, trident);
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
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
        final ArenaPlayerEntry playerEntry = playerArenaEntries.get(uuid);
        if (playerEntry == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "You must be in the %s arena to run this command",
                "" + ChatColor.AQUA + ChatColor.BOLD + "atlantis" + ChatColor.RED
            ));
            return false;
        }

        final Set<UUID> playersInArena = arena.getPlayers();
        switch (option) {
            case "list":
                sender.sendMessage(ChatColor.GREEN + String.format(
                        "%d player%s in arena",
                        playersInArena.size(),
                        playersInArena.size() > 1 ? "s" : ""
                ));
                sendArenaPlayerListMessage(player);
                return false;
            case "cancel":
                if (!arena.isActive()) {
                    sender.sendMessage(ChatColor.RED + "Your match must be starting to cancel it");
                    return false;
                }
                Bukkit.getScheduler().cancelTask(arena.getCountdownTask());
                broadcastToPlayerUUIDList(playersInArena, ChatColor.RED + String.format(
                        "%s cancelled the match",
                        ChatColor.BOLD + player.getName() + ChatColor.RED
                ));
                return false;
            case "leave":
                onPlayerLeave(player);
                return false;
            case "start":
                if (!playersInArena.contains(uuid)) {
                    sender.sendMessage(ChatColor.RED + String.format(
                            "You must be in the %s arena to start its event!",
                            "" + arena.getColor() + ChatColor.BOLD + "atlantis" + ChatColor.RED
                    ));
                    return false;
                }

                if (arena.isCountingDown()) {
                    sender.sendMessage(ChatColor.RED + "Arena match is already counting down");
                    return false;
                }

                if (playersInArena.size() == 1) {
                    sender.sendMessage(ChatColor.RED + "At least 2 people must be in the arena to start");
                    return false;
                }

                if (arena.isActive()) {
                    sender.sendMessage(ChatColor.RED + "The match is currently ongoing");
                    return false;
                }

                // checks passed, start arena battle
                broadcastToPlayerUUIDList(playersInArena, ChatColor.BOLD + String.format(
                        "%s has started the match!",
                        player.getName()
                ));

                arena.setCountdownTask(
                        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                                TinyEmpires.getInstance(),
                                new Runnable() {
                                    private int timer = 5;

                                    @Override
                                    public void run() {
                                        if (timer == 0) {
                                            Bukkit.getScheduler().cancelTask(arena.getCountdownTask());
                                            arena.start();
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
                                                            false
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
    }

    private String getKillMessage() {
        // <player> %s <killer>
        switch (random.nextInt(5)) {
            case 0: return "was slain by";
            case 1: return "was killed by";
            case 2: return "died at the hands of";
            case 3: return "was defeated by";
            case 4: return "was skewered by";
            default: throw new NullPointerException("Bound was not caught by switch in getting a kill message");
        }
    }

    @EventHandler
    public void pickUpTrident(EntityPickupItemEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        if (event.getItem().getType() == EntityType.TRIDENT
                && playerArenaEntries.containsKey(uuid)
                && !arena.getPlayers().contains(uuid)) {
            event.setCancelled(true);
        }
    }

    public void onPlayerLeave(Player player) {
        final UUID uuid = player.getUniqueId();
        if (arena.isActive())
            arena.removePlayerFromPlayersLeft(uuid);
        arena.removePlayer(uuid);
        final ArenaPlayerEntry playerEntry = playerArenaEntries.get(uuid);
        // delete trident if exists
        deleteLastTridentFromPlayerIfExists(uuid);
        playerEntry.restore(player);
        playerArenaEntries.remove(uuid);
        player.teleport(arena.getStartLocation());
        player.setGameMode(GameMode.SURVIVAL);
        broadcastToPlayerUUIDList(arena.getPlayers(), ChatColor.YELLOW + String.format(
            "%s has left the arena",
            ChatColor.BOLD + player.getName() + ChatColor.YELLOW
        ));
    }

    private void deleteLastTridentFromPlayerIfExists(UUID uuid) {
        if (playerToLastThrownTridentEntityId.containsKey(uuid)) {
            final Entity lastTridentThrown = Bukkit.getPlayer(
                playerToLastThrownTridentEntityId.get(uuid)
            );
            if (lastTridentThrown != null)
                lastTridentThrown.remove();
            playerToLastThrownTridentEntityId.remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        final Entity victim = event.getEntity();
        System.out.println("Victim: " + victim.getType() + ", " + victim.getName());
        final Location location = victim.getLocation();
        // return if damage not done in arena
        if (location.getWorld() != null
                && !BoundUtils.inBoundsOfWaterArena(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockZ()
                )
        )
            return;
        System.out.println("EntityDamageByEntityEvent in water arena bounds");

        // don't kill squids, fishes or other in-arena mobs
        if (!(victim instanceof Player)) {
            event.setCancelled(true);
            System.out.println("Cancelling event because victim is not player");
            return;
        }

        // cancel if damager was not a trident or player (e.g. pufferfish)
        final Entity damager = event.getDamager();
        if (damager.getType() != EntityType.TRIDENT
                && damager.getType() != EntityType.PLAYER) {
            System.out.println("Damager entity was not a trident or player");
            event.setCancelled(true);
            return;
        }

        final Player player = (Player) victim; // get player that was damaged
        System.out.println("Player Victim: " + player.getName());
        final UUID uuid = player.getUniqueId();
        final ArenaPlayerEntry playerEntry = playerArenaEntries.get(uuid);
        // return if player is in bounds but doesn't have player entry (isn't registered in arena)
        if (playerEntry == null)
            return;

        if (!arena.isActive()) {
            // cancel event if player is waiting on coral block for event to start
            event.setCancelled(true);
            return;
        }

        // player is the shooter of the trident or directly the damager if melee
        final Player killer = damager.getType() == EntityType.TRIDENT
            ? (Player) ((Trident) damager).getShooter()
            : (Player) damager;
        if (killer == null)
            throw new NullPointerException("Killer fetched as null: " + damager.toString());
        // return if player still has health left
        final double newHealth = player.getHealth() - event.getFinalDamage();
        killer.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 15, 1);
        if (newHealth > 0) {
            System.out.println("Player did not die, returning");
            killer.sendMessage(ChatColor.GREEN + String.format(
                "%s has %.1f hearts left",
                ChatColor.BOLD + player.getName() + ChatColor.GREEN,
                newHealth
            ));
            return;
        }

        System.out.println(playerEntry);
        System.out.println(player);

        // remove from players
        arena.removePlayerFromPlayersLeft(uuid);

        // delete trident
        deleteLastTridentFromPlayerIfExists(killer.getUniqueId());

        // prevent player from dying
        event.setCancelled(true);

        // lightning effect
        player.getWorld().strikeLightningEffect(player.getLocation());

        // teleport to top
        playerEntry.restore(player);
        player.teleport(arena.getStartLocation());

        // restore health to full
        player.setHealth(20);

        final int playersLeft = arena.getPlayersLeft().size();
        broadcastToPlayerUUIDList(arena.getPlayers(), ChatColor.YELLOW + String.format(
            "%s %s %s! %s",
            ChatColor.BOLD + player.getName() + ChatColor.YELLOW,
            getKillMessage(),
            ChatColor.BOLD + killer.getName() + ChatColor.YELLOW,
            playersLeft > 1
                ? playersLeft + " players left!"
                : ""
        ));

        if (playersLeft == 1) {
            broadcastToPlayerUUIDList(arena.getPlayers(), ChatColor.GREEN + String.format(
                "%s has won the match!",
                ChatColor.BOLD + killer.getName() + ChatColor.GREEN
            ));

            final UUID killerUUID = killer.getUniqueId();

            playerArenaEntries.get(killerUUID).restore(killer);
            playerArenaEntries.remove(killerUUID);
            arena.removePlayer(killerUUID);
            killer.teleport(arena.getStartLocation());
            killer.setGameMode(GameMode.SURVIVAL);
            arena.end();

            // use explicit iterator notation to avoid concurrent modification
            for (Iterator<UUID> it = arena.getPlayers().iterator(); it.hasNext();) {
                final UUID pUUID = it.next();
                it.remove();
                playerArenaEntries.remove(pUUID);
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
        onPlayerLeave(player);
    }

    private void restoreFractionOfBoostCharge(Player player) {
        player.setExp(Math.min(1f, player.getExp() + 0.99F / 60));
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        // boosts for water arena
        final Player player = event.getPlayer();
        final ArenaPlayerEntry ArenaPlayerEntry = playerArenaEntries.get(player.getUniqueId());
        if (ArenaPlayerEntry == null)
            return;

        // only for water arena and players remaining in match
        final UUID uuid = player.getUniqueId();
        final ArenaPlayerEntry playerEntry = playerArenaEntries.get(uuid);
        if (playerEntry != null
                && arena.getPlayersLeft().contains(uuid)
                && arena.isActive()) {
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
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        final ProjectileSource source = event.getEntity().getShooter();
        if (!(source instanceof Player))
            return;
        final Player player = (Player) source;
        playerToLastThrownTridentEntityId.put(
            player.getUniqueId(),
            event.getEntity().getUniqueId()
        );
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        // prevent player from accidentally dropping item
        if (playerArenaEntries.containsKey(player.getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Location to = event.getTo();
        final Location from = event.getFrom();
        // #getFrom is not null if #getTo isn't
        if (to == null)
            throw new NullPointerException("Could not get player location");

        final ArenaPlayerEntry ArenaPlayerEntry = playerArenaEntries.get(event.getPlayer().getUniqueId());
        if (ArenaPlayerEntry != null) {
            if (!arena.isActive()
                    && (to.getX() != from.getX()
                    || to.getY() != from.getY()
                    || to.getZ() != from.getZ()))
                event.setCancelled(true);
            return;
        }

        if (arena.getEntrancePlane().isInPlane(to.getBlockX(), to.getBlockY(), to.getBlockZ())) {
            final int playerLimit = arena.getPlayerLimit();
            final Set<UUID> playersInArena = arena.getPlayers();
            final Player player = event.getPlayer();
            if (playersInArena.size() == playerLimit) {
                player.sendMessage(ChatColor.RED + String.format(
                    "The %s arena is full! (%d/%d)",
                    "" + ChatColor.AQUA + ChatColor.BOLD + "atlantis" + ChatColor.RED,
                    playerLimit,
                    playerLimit
                ));
                player.teleport(arena.getStartLocation());
                return;
            }

            if (arena.isActive()
                    || arena.isCountingDown()) {
                player.sendMessage(ChatColor.RED + "You cannot join while the match is currently ongoing!");
                player.teleport(arena.getStartLocation());
                return;
            }

            // cost
            final UUID uuid = player.getUniqueId();
            final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
            if (tePlayer == null)
                throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);

            // teleport
            player.teleport(arena.getRandomSpawnLocationForPlayer(uuid));

            // health
            player.setHealth(20);
            player.setFoodLevel(20);

            // adventure mode
            player.setGameMode(GameMode.ADVENTURE);

            // put original player data before teleporting, inventory, exp, location
            playerArenaEntries.put(
                uuid,
                new ArenaPlayerEntry(
                    player.getInventory().getContents(),
                    player.getTotalExperience()
                )
            );

            // delete trident if they threw one before game begun
            deleteLastTridentFromPlayerIfExists(uuid);

            // set arena inventory and exp
            setPlayerArenaInventory(player);
            player.setLevel(0);
            player.setExp(0.99F);

            // add player
            arena.addPlayer(player.getUniqueId());

            // broadcast join message in arena
            broadcastToPlayerUUIDList(playersInArena, ChatColor.GREEN + String.format(
                "%s has joined the %s arena! (%d/%d)",
                ChatColor.BOLD + player.getName() + ChatColor.GREEN,
                "" + ChatColor.AQUA + ChatColor.BOLD + "atlantis" + ChatColor.GREEN,
                playersInArena.size(),
                arena.getPlayerLimit()
            ));

            // send player list of arena participants
            player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Players in arena:");
            sendArenaPlayerListMessage(player);
        }
    }

}