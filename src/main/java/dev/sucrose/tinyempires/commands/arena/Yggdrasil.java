package dev.sucrose.tinyempires.commands.arena;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class Yggdrasil implements Listener, CommandOption {

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

    static {
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

        final Team yellow = scoreboard.getMainScoreboard().registerNewTeam("YGGD_YELLOW");
        yellow.setColor(ChatColor.YELLOW);
        yellow.setAllowFriendlyFire(false);
        scoreboardTeams.put(YggdrasilTeam.YELLOW, yellow);

        final Team green = scoreboard.getMainScoreboard().registerNewTeam("YGGD_GREEN");
        green.setColor(ChatColor.DARK_GREEN);
        green.setAllowFriendlyFire(false);
        scoreboardTeams.put(YggdrasilTeam.GREEN, green);

        final Team red = scoreboard.getMainScoreboard().registerNewTeam("YGGD_RED");
        red.setColor(ChatColor.RED);
        red.setAllowFriendlyFire(false);
        scoreboardTeams.put(YggdrasilTeam.RED, red);

        final Team blue = scoreboard.getMainScoreboard().registerNewTeam("YGGD_BLUE");
        blue.setColor(ChatColor.BLUE);
        blue.setAllowFriendlyFire(false);
        scoreboardTeams.put(YggdrasilTeam.BLUE, blue);

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
    public void execute(Player sender, String[] args) {
        // /arena yggdrasil <team-name|options>
        final UUID uuid = sender.getUniqueId();
        if (!arenaPlayerEntryMap.containsKey(uuid)) {
            sender.sendMessage(ChatColor.RED + "You must be in the Yggdrasil arena to run this command");
            return;
        }

        final YggdrasilPlayerEntry arenaPlayerEntry = arenaPlayerEntryMap.get(uuid);
        // commands require player to be in arena
        if (arenaPlayerEntry == null) {
            sender.sendMessage(ChatColor.RED + "You must be in the Yggdrasil arena to run this command");
            return;
        }

        final String option = args[0];
        try {
            final YggdrasilTeam team = YggdrasilTeam.valueOf(option.toLowerCase());
            if (active || countingDown) {
                sender.sendMessage(ChatColor.RED + "You cannot switch teams while the match is ongoing");
                return;
            }

            sender.teleport(spawnLocations.get(team));
            // add player uuid to team list
            teams.get(arenaPlayerEntry.getTeam()).remove(uuid);
            teams.get(team).add(uuid);
            broadcast(ChatColor.YELLOW + String.format(
                "%s changed to the %s team",
                ChatColor.BOLD + sender.getName() + ChatColor.YELLOW,
                "" + ChatColor.valueOf(team.name()) + ChatColor.BOLD + team.name().toLowerCase() + ChatColor.YELLOW
            ));
            return;
        } catch (IllegalArgumentException ignore) {}

        switch (option) {
            case "start":
                if (countingDown) {
                    sender.sendMessage(ChatColor.RED + "The match is already counting down");
                    return;
                }

                if (active) {
                    sender.sendMessage(ChatColor.RED + "The match is current ongoing");
                    return;
                }

                if (teams.keySet().size() == 1) {
                    sender.sendMessage(ChatColor.RED + "At least two teams must be in the arena to start");
                    return;
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
                                    "" + ChatColor.GREEN + ChatColor.BOLD + "The match has " + "started!",
                                    "Start!"
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
                                "" + ChatColor.YELLOW + ChatColor.BOLD + timer--,
                                ChatColor.GREEN + String.format(
                                    "%d second%s until match starts...",
                                    timer,
                                    timer > 1 ? "s" : ""
                                )
                            );
                        }
                    },
                    0,
                    20
                );
                countingDown = true;
                return;
            case "cancel":
                if (!countingDown) {
                    sender.sendMessage(ChatColor.GOLD + "The match must be counting down to cancel it");
                    return;
                }

                Bukkit.getScheduler().cancelTask(countDownTask);
                broadcast(ChatColor.RED + String.format(
                    "%s cancelled the match",
                    ChatColor.BOLD + sender.getName() + ChatColor.RED
                ));
                return;
            case "leave":
                teams.remove(arenaPlayerEntry.getTeam());
                arenaPlayerEntry.restore(sender);
                sender.teleport(START_LOCATION);

                broadcast(ChatColor.RED + String.format(
                    "%s has left the arena",
                    ChatColor.BOLD + sender.getName() + ChatColor.RED
                ));
                if (active) {
                    world.strikeLightningEffect(sender.getLocation());
                    if (teams.get(arenaPlayerEntry.getTeam()).size() > 1) {
                        teams.get(arenaPlayerEntry.getTeam()).remove(uuid);
                    } else {
                        teams.remove(arenaPlayerEntry.getTeam());
                        broadcast(ChatColor.YELLOW + String.format(
                            "Team %s has conceded!",
                            "" + yggdrasilTeamToChatColor(arenaPlayerEntry.getTeam()) + ChatColor.BOLD + arenaPlayerEntry.getTeam().name() + ChatColor.YELLOW
                        ));
                    }
                }
        }
    }

    private ChatColor yggdrasilTeamToChatColor(YggdrasilTeam team) {
        return ChatColor.valueOf(team.name());
    }

    private String yggdrasilTeamsToList(Set<YggdrasilTeam> t) {
        final StringBuilder stringBuilder = new StringBuilder();
        final YggdrasilTeam[] ts = (YggdrasilTeam[]) t.toArray();
        for (int i = 0; i < ts.length - 1; i++)
            stringBuilder.append(yggdrasilTeamToChatColor(ts[i]))
                .append(ChatColor.BOLD)
                .append(ts[i].name())
                .append(ChatColor.GREEN)
                .append(", ");
        return stringBuilder.append("and ")
            .append(yggdrasilTeamToChatColor(ts[ts.length - 1]))
            .append(ts[ts.length - 1].name())
            .append(ChatColor.GREEN)
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
            p.teleport(START_LOCATION);
        });

        for (final FillPlane plane : fillPlanes)
            plane.fill(Material.IRON_BARS);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        final Player victim = (Player) event.getEntity();
        final UUID uuid = victim.getUniqueId();
        final YggdrasilPlayerEntry playerEntry = arenaPlayerEntryMap.get(uuid);
        if (playerEntry == null)
            return;
        final Player killer = (Player) event.getDamager();
        final int teamsLeftCount = teamsLeft.keySet().size();
        broadcast(ChatColor.GREEN + String.format(
            "%s of %s was defeated by %s! %s",
            ChatColor.BOLD + victim.getName() + ChatColor.GREEN,
            "" + YggdrasilTeam.valueOf(playerEntry.getTeam().name()) + ChatColor.BOLD + playerEntry.getTeam().name()
                + ChatColor.GREEN,
            ChatColor.BOLD + killer.getName() + ChatColor.GREEN,
            teamsLeftCount > 1
                ? String.format(
                    "%s and %d players remain",
                    yggdrasilTeamsToList(teamsLeft.keySet()),
                    teamsLeft.values().stream().map(List::size).reduce(0, Integer::sum)
                )
                : ""
        ));

        // lightning effect
        world.strikeLightningEffect(killer.getLocation());

        // remove glowing and from scoreboard team
        scoreboardTeams.get(playerEntry.getTeam()).removeEntry(victim.getName());
        victim.setGlowing(false);

        final YggdrasilTeam playerTeam = playerEntry.getTeam();
        if (teams.get(playerTeam).size() > 1) {
            teams.get(playerTeam).remove(uuid);
        } else {
            teams.remove(playerTeam);
            broadcast(ChatColor.YELLOW + String.format(
                "Team %s has been defeated!",
                "" + yggdrasilTeamToChatColor(playerTeam) + ChatColor.BOLD + playerTeam.name() + ChatColor.YELLOW
            ));
        }

        final YggdrasilPlayerEntry killerEntry = arenaPlayerEntryMap.get(killer.getUniqueId());
        if (teamsLeft.keySet().size() == 1)
            onWinnerDetermined(killerEntry);

        // go into spectator if match is not over
        victim.setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Location location = event.getTo();
        if (location == null)
            throw new NullPointerException("Location found as null when fetching from PlayerMoveEvent");

        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            // prevent spectator from going inside blocks and committing x-ray
            if (location.getBlock().getType() != Material.AIR) {
                event.setCancelled(true);
                return;
            }
        }

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
                    ArenaType.YGGDRASIL,
                    player.getInventory().getContents(),
                    player.getTotalExperience()
                )
            );

            setPlayerInventory(player);

            // add player to scoreboard team for colored glowing effect
            scoreboardTeams.get(team).addEntry(player.getName());
            player.setGlowing(true);

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

        final ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE, 1);
        final PlayerInventory inventory = player.getInventory();
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
    }

}