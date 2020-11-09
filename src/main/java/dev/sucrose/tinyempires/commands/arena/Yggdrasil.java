package dev.sucrose.tinyempires.commands.arena;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.*;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Yggdrasil implements Listener, CommandExecutor {

    private static final Map<UUID, YggdrasilPlayerEntry> arenaPlayerEntryMap = new HashMap<>();
    private static final Map<YggdrasilTeam, Location> spawnLocations = new EnumMap<>(YggdrasilTeam.class);
    private static final BoundsPlane entrancePlane = new BoundsPlane(1390, 77, -2574, 1399, 83, -2574);
    private static final Map<YggdrasilTeam, List<UUID>> teams = new EnumMap<>(YggdrasilTeam.class);
    private static final Map<YggdrasilTeam, List<UUID>> teamsLeft = new EnumMap<>(YggdrasilTeam.class);
    private static final List<YggdrasilTeam> teamOptionsLeft = new ArrayList<>();
    private static final Map<YggdrasilTeam, Team> scoreboardTeams = new EnumMap<>(YggdrasilTeam.class);
    private static final Location START_LOCATION;
    private static boolean countingDown = false;
    private static int countDownTask;
    private static boolean active = false;
    private static final List<FillPlane> fillPlanes = new ArrayList<>();
    private static final Random random = new Random();
    private static final World world;
    private static final Scoreboard yggdrasilScoreboard;

    static {
        final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null)
            throw new NullPointerException("ScoreboardManager as null when fetching it from Bukkit");

        yggdrasilScoreboard = scoreboardManager.getMainScoreboard();
        world = Bukkit.getWorld("world");
        START_LOCATION = new Location(world, 1394.5, 79, -2585.5);
        spawnLocations.put(YggdrasilTeam.RED, new Location(world, 1379.5, 105, -2515.5, -90, 0));
        spawnLocations.put(YggdrasilTeam.GREEN, new Location(world, 1401, 105, -2538.5, 0, 0));
        spawnLocations.put(YggdrasilTeam.YELLOW, new Location(world, 1424.5, 105, -2515.5, 90, 0));
        spawnLocations.put(YggdrasilTeam.BLUE, new Location(world, 1401, 105, -2493.5, -180, 0));

        fillPlanes.add(new FillPlane(1399, 105, -2494, 1403, 107, -2494));
        fillPlanes.add(new FillPlane(1380, 105, -2517, 1380, 107, -2515));
        fillPlanes.add(new FillPlane(1399, 105, -2536, 1403, 107, -2536));
        fillPlanes.add(new FillPlane(1422, 105, -2517, 1422, 107, -2513));

        teamOptionsLeft.addAll(Arrays.asList(YggdrasilTeam.values()));

        final ScoreboardManager scoreboard = Bukkit.getScoreboardManager();
        if (scoreboard == null)
            throw new NullPointerException("ScoreboardManager as null when fetching it from Bukkit");

        for (final YggdrasilTeam team : YggdrasilTeam.values())
            registerScoreboardTeamAndPutInMap(team);
    }

    private static void registerScoreboardTeamAndPutInMap(YggdrasilTeam team) {
        final ChatColor color = yggdrasilTeamToChatColor(team);
        final Team currentTeam = yggdrasilScoreboard.getTeam("YGGD_" + team.name());
        if (currentTeam != null)
            currentTeam.unregister();
        final Team scoreboardTeam = yggdrasilScoreboard.registerNewTeam("YGGD_" + team.name());
        scoreboardTeam.setColor(color);
        scoreboardTeam.setPrefix("" + ChatColor.WHITE);
        scoreboardTeam.setAllowFriendlyFire(false);
        scoreboardTeams.put(team, scoreboardTeam);
    }

    public static void removeYggdrasilScoreboardTeams() {
        scoreboardTeams.values().forEach(Team::unregister);
    }

    private interface PlayerFunctor {
        void run(Player player);
    }

    private void doForEachPlayer(PlayerFunctor functor) {
        for (final List<UUID> uuids : teams.values()) {
            uuids.forEach(uuid -> {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null)
                    throw new NullPointerException("Could not get player of uuid " + uuid + " when running arena-wide" +
                        " player functor in Yggdrasil");
                functor.run(player);
            });
        }
    }

    private void broadcast(String message) {
        doForEachPlayer(player -> player.sendMessage(message));
    }

    private void broadcastWithTextAndTitle(String message, String title) {
        doForEachPlayer(player -> {
            player.sendMessage(message);
            player.sendTitle(title, "", 10, 70, 20);
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /arena yggdrasil <team-name|options>
        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();
        if (!arenaPlayerEntryMap.containsKey(uuid)) {
            sender.sendMessage(ChatColor.RED + "You must be in the Yggdrasil arena to run this command");
            return false;
        }

        final YggdrasilPlayerEntry playerEntry = arenaPlayerEntryMap.get(uuid);
        // commands require player to be in arena
        if (playerEntry == null) {
            sender.sendMessage(ChatColor.RED + "You must be in the Yggdrasil arena to run this command");
            return false;
        }

        final String option = args[0];
        try {
            final YggdrasilTeam team = YggdrasilTeam.valueOf(option.toUpperCase());
            if (active || countingDown) {
                sender.sendMessage(ChatColor.RED + "You cannot switch teams while the match is ongoing");
                return false;
            }

            player.teleport(spawnLocations.get(team));
            // add player uuid to team list
            final YggdrasilTeam originalTeam = playerEntry.getTeam();
            if (teams.get(originalTeam).size() == 1) {
                teams.remove(originalTeam);
            } else {
                teams.get(originalTeam).remove(uuid);
            }
            if (!teams.containsKey(team))
                teams.put(team, new ArrayList<>());
            playerEntry.setTeam(team);
            teams.get(team).add(uuid);
            // remove from original team scoreboard team and add to new selected one
            scoreboardTeams.get(originalTeam).removeEntry(player.getName());
            scoreboardTeams.get(team).addEntry(player.getName());
            broadcast(ChatColor.YELLOW + String.format(
                "%s changed to the %s team",
                ChatColor.BOLD + sender.getName() + ChatColor.YELLOW,
                "" + ChatColor.valueOf(team.name()) + ChatColor.BOLD + team.name().toLowerCase() + ChatColor.YELLOW
            ));
            return true;
        } catch (IllegalArgumentException ignore) {}

        switch (option) {
            case "start":
                if (countingDown) {
                    sender.sendMessage(ChatColor.RED + "The match is already counting down");
                    return false;
                }

                if (active) {
                    sender.sendMessage(ChatColor.RED + "The match is current ongoing");
                    return false;
                }

                if (teams.keySet().size() == 1) {
                    sender.sendMessage(ChatColor.RED + "At least two teams must be in the arena to start");
                    return false;
                }

                // checks passed, start arena battle
                broadcast(ChatColor.BOLD + String.format(
                    "%s has started the match!",
                    sender.getName()
                ));

                countDownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    TinyEmpires.getInstance(),
                    new Runnable() {
                        private int timer = 5;

                        @Override
                        public void run() {
                            if (timer == 0) {
                                Bukkit.getScheduler().cancelTask(countDownTask);
                                broadcastWithTextAndTitle(
                                    "" + ChatColor.GREEN + ChatColor.BOLD + "The match has started!",
                                    "" + ChatColor.YELLOW + ChatColor.BOLD + "Start!"
                                );
                                active = true;
                                countingDown = false;
                                for (final FillPlane plane : fillPlanes)
                                    plane.fill(Material.AIR);
                                teamsLeft.putAll(teams);
                                return;
                            }

                            // send countdown titles, chat messages and decrement timer
                            broadcastWithTextAndTitle(
                                ChatColor.GREEN + String.format(
                                    "%d second%s until match starts...",
                                    timer,
                                    timer > 1 ? "s" : ""
                                ),
                                "" + ChatColor.YELLOW + ChatColor.BOLD + timer--
                            );
                        }
                    },
                    0,
                    20
                );
                countingDown = true;
                return true;
            case "list":
                player.sendMessage("" + ChatColor.DARK_GREEN + ChatColor.BOLD + "Players in Yggdrasil:");
                for (final Map.Entry<YggdrasilTeam, List<UUID>> entry : teams.entrySet()) {
                    player.sendMessage("" + yggdrasilTeamToChatColor(entry.getKey()) + ChatColor.BOLD + entry.getKey().name());
                    for (final UUID pUUID : entry.getValue()) {
                        final Player p = Bukkit.getPlayer(pUUID);
                        if (p == null)
                            throw new NullPointerException("Could not get player with UUID " + pUUID + " when sending" +
                                " list of Yggdrasil participants");
                        player.sendMessage(ChatColor.GREEN + " - " + p.getName());
                    }
                }
                return true;
            case "cancel":
                if (!countingDown) {
                    sender.sendMessage(ChatColor.GOLD + "The match must be counting down to cancel it");
                    return false;
                }

                Bukkit.getScheduler().cancelTask(countDownTask);
                broadcast(ChatColor.RED + String.format(
                    "%s cancelled the match",
                    ChatColor.BOLD + sender.getName() + ChatColor.RED
                ));
                return true;
            case "leave":
                onPlayerLeave(player, playerEntry);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "/yggdrasil <start/cancel/leave/list/[team]");
                return false;
        }
    }

    private static ChatColor yggdrasilTeamToChatColor(YggdrasilTeam team) {
        return ChatColor.valueOf(team.name());
    }

    private String yggdrasilTeamsToList(Set<YggdrasilTeam> t) {
        final StringBuilder stringBuilder = new StringBuilder();
        final String[] ts = t.stream().map(Enum::name).toArray(String[]::new);
        for (int i = 0; i < ts.length - 1; i++)
            stringBuilder.append(ChatColor.valueOf(ts[i]))
                .append(ChatColor.BOLD)
                .append(ts[i])
                .append(ChatColor.YELLOW)
                .append((i == ts.length - 1 ? " " : ", "));
        return stringBuilder.append("and ")
            .append(ChatColor.valueOf(ts[ts.length - 1]))
            .append(ts[ts.length - 1])
            .append(ChatColor.YELLOW)
            .toString();
    }

    private void onWinnerDetermined(YggdrasilPlayerEntry winnerEntry) {
        broadcast(ChatColor.YELLOW + String.format(
            "Team %s has won the match!",
            "" + yggdrasilTeamToChatColor(winnerEntry.getTeam()) + ChatColor.BOLD + winnerEntry.getTeam().name() + ChatColor.YELLOW
        ));

        doForEachPlayer(p -> {
            final YggdrasilPlayerEntry pEntry = arenaPlayerEntryMap.get(p.getUniqueId());
            pEntry.restore(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.setFlying(false);
            p.setInvulnerable(false);
            p.teleport(START_LOCATION);
            arenaPlayerEntryMap.remove(p.getUniqueId());
        });

        teams.clear();
        teamsLeft.clear();

        for (final FillPlane plane : fillPlanes)
            plane.fill(Material.IRON_BARS);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        final Player victim = (Player) event.getEntity();
        // return if player is not dead
        if (victim.getHealth() - event.getFinalDamage() > 0)
            return;

        final UUID uuid = victim.getUniqueId();
        final YggdrasilPlayerEntry playerEntry = arenaPlayerEntryMap.get(uuid);
        if (playerEntry == null)
            return;
        // if player killed by arrow get arrow shooter
        final Player killer = event.getDamager().getType() == EntityType.ARROW
            ? (Player) ((Arrow) event.getDamager()).getShooter()
            : (Player) event.getDamager();
        if (killer == null)
            throw new NullPointerException("Killer as null when victim killed by arrow");

        // lightning effect
        world.strikeLightningEffect(victim.getLocation());

        // remove glowing and from scoreboard team
        scoreboardTeams.get(playerEntry.getTeam()).removeEntry(victim.getName());
        victim.setGlowing(false);

        // remove team from those remaining
        boolean wasTeamDefeated = false;
        final YggdrasilTeam playerTeam = playerEntry.getTeam();
        if (teamsLeft.get(playerTeam).size() > 1) {
            teamsLeft.get(playerTeam).remove(uuid);
        } else {
            teamsLeft.remove(playerTeam);
            wasTeamDefeated = true;
        }

        final int teamsLeftCount = teamsLeft.keySet().size();
        broadcast(ChatColor.YELLOW + String.format(
            "%s of %s was defeated by %s! %s",
            ChatColor.BOLD + victim.getName() + ChatColor.YELLOW,
            "" + yggdrasilTeamToChatColor(playerEntry.getTeam()) + ChatColor.BOLD + playerEntry.getTeam().name()
                + ChatColor.YELLOW,
            ChatColor.BOLD + killer.getName() + ChatColor.YELLOW,
            teamsLeftCount > 1
                ? String.format(
                    "%s and %d players remain!",
                    yggdrasilTeamsToList(teamsLeft.keySet()),
                    teamsLeft.values().stream().map(List::size).reduce(0, Integer::sum)
                )
                : ""
        ));

        // use boolean so teamsLeftCount is accurate but team defeat message is sent after player defeat message
        if (wasTeamDefeated)
            broadcast(ChatColor.YELLOW + String.format(
                "Team %s has been defeated!",
                "" + yggdrasilTeamToChatColor(playerTeam) + ChatColor.BOLD + playerTeam.name() + ChatColor.YELLOW
            ));

        final YggdrasilPlayerEntry killerEntry = arenaPlayerEntryMap.get(killer.getUniqueId());
        if (teamsLeft.keySet().size() == 1)
            onWinnerDetermined(killerEntry);

        // go into flying mode if match is not over
        victim.setInvulnerable(true);
        victim.getInventory().clear();
        victim.setFlying(true);
    }

    private void onPlayerLeave(Player player, YggdrasilPlayerEntry arenaPlayerEntry) {
        broadcast(ChatColor.YELLOW + String.format(
            "%s has left the arena",
            ChatColor.BOLD + player.getName() + ChatColor.YELLOW
        ));
        final UUID uuid = player.getUniqueId();
        final YggdrasilTeam yggdrasilTeam = arenaPlayerEntry.getTeam();
        if (teams.get(yggdrasilTeam).size() == 1) {
            teams.remove(yggdrasilTeam);
        } else {
            teams.get(yggdrasilTeam).remove(uuid);
        }

        scoreboardTeams.get(yggdrasilTeam).removeEntry(player.getName());
        arenaPlayerEntry.restore(player);
        arenaPlayerEntryMap.remove(uuid);
        player.teleport(START_LOCATION);
        player.setGameMode(GameMode.SURVIVAL);

        if (active) {
            world.strikeLightningEffect(player.getLocation());
            if (teams.get(arenaPlayerEntry.getTeam()).size() > 1) {
                teams.get(arenaPlayerEntry.getTeam()).remove(uuid);
            } else {
                teams.remove(arenaPlayerEntry.getTeam());
                broadcast(ChatColor.YELLOW + String.format(
                    "Team %s has conceded!",
                    "" + yggdrasilTeamToChatColor(arenaPlayerEntry.getTeam()) + ChatColor.BOLD + arenaPlayerEntry.getTeam().name() + ChatColor.YELLOW
                ));
                if (teams.size() == 1)
                    onWinnerDetermined(arenaPlayerEntryMap.values().iterator().next());
            }
            player.setFlying(false);
            player.setInvulnerable(false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final YggdrasilPlayerEntry arenaPlayerEntry = arenaPlayerEntryMap.get(player.getUniqueId());
        if (arenaPlayerEntry == null)
            return;
        onPlayerLeave(player, arenaPlayerEntry);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Location location = event.getTo();
        final Location from = event.getFrom();
        if (location == null)
            throw new NullPointerException("Location found as null when fetching from PlayerMoveEvent");
        // return if player didn't change location (e.g. looks around)
        if (from.getBlockX() == location.getBlockX()
                && from.getBlockY() == location.getBlockY()
                && from.getBlockZ() == location.getBlockZ())
            return;

        final Player player = event.getPlayer();
        if (entrancePlane.isInPlane(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
            if (teamOptionsLeft.size() == 0) {
                player.sendMessage(ChatColor.RED + "The Yggdrasil arena is full!");
                player.teleport(START_LOCATION);
                return;
            }
            final int index = random.nextInt(teamOptionsLeft.size());
            final YggdrasilTeam team = teamOptionsLeft.get(index);
            teamOptionsLeft.remove(index);
            final UUID uuid = player.getUniqueId();
            if (!teams.containsKey(team))
                teams.put(team, new ArrayList<>());
            teams.get(team).add(uuid);
            player.teleport(spawnLocations.get(team));
            arenaPlayerEntryMap.put(
                uuid,
                new YggdrasilPlayerEntry(
                    team,
                    player.getInventory().getContents(),
                    player.getTotalExperience()
                )
            );

            setPlayerInventory(player);

            // add player to scoreboard team for colored glowing effect
            scoreboardTeams.get(team).addEntry(player.getName());
            player.setGlowing(true);
            player.setScoreboard(yggdrasilScoreboard);

            // adventure mode so player can't modify arena
            player.setGameMode(GameMode.ADVENTURE);

            // health and hunger
            player.setHealth(20);
            player.setFoodLevel(20);

            broadcast(ChatColor.GREEN + String.format(
                "%s joined the arena!",
                ChatColor.BOLD + player.getName() + ChatColor.GREEN
            ));
        }
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

    private void setPlayerInventory(Player player) {
        final ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        final ItemStack chestplate = new ItemStack(Material.IRON_CHESTPLATE);
        final ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
        final ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        final ItemStack shield = new ItemStack(Material.SHIELD);
        final ItemStack sword = new ItemStack(Material.IRON_SWORD);
        final ItemStack axe = new ItemStack(Material.IRON_AXE);
        final ItemStack crossbow = new ItemStack(Material.CROSSBOW);

        // add pre-loaded crossbow arrow
        final CrossbowMeta crossbowMeta = (CrossbowMeta) crossbow.getItemMeta();
        if (crossbowMeta == null)
            throw new NullPointerException("CrossbowMeta was null when fetched from crossbow in setting player " +
                "inventory");

        crossbowMeta.addChargedProjectile(new ItemStack(Material.ARROW));
        crossbow.setItemMeta(crossbowMeta);

        final ItemStack bow = new ItemStack(Material.BOW);
        final ItemStack arrows = new ItemStack(Material.ARROW, 8);
        setItemsUnbreakable(helmet, chestplate, leggings, boots, shield, sword, axe, crossbow, bow, arrows);

        final ItemStack potion = new ItemStack(Material.POTION);
        final PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        if (potionMeta == null)
            throw new NullPointerException("Could not get PotionMeta for potion when giving Yggdrasil inventory");
        potionMeta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL));

        final PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setHelmet(helmet);
        inventory.setChestplate(chestplate);
        inventory.setLeggings(leggings);
        inventory.setBoots(boots);
        inventory.setItemInOffHand(shield);
        inventory.setItem(0, sword);
        inventory.setItem(1, axe);
        inventory.setItem(2, bow);
        inventory.setItem(3, crossbow);
        inventory.setItem(4, potion);
        inventory.setItem(5, arrows);
        player.updateInventory();
    }

}