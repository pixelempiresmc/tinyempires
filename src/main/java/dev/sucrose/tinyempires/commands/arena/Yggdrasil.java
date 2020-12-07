package dev.sucrose.tinyempires.commands.arena;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.*;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
    private static final ScoreboardManager scoreboardManager;
    private static final List<Location> woolBlocksPlaced = new ArrayList<>();

    static {
        scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null)
            throw new NullPointerException("ScoreboardManager as null when fetching it from Bukkit");

        yggdrasilScoreboard = scoreboardManager.getMainScoreboard();
        world = Bukkit.getWorld("world");
        START_LOCATION = new Location(world, 1394.5, 79, -2585.5);
        spawnLocations.put(YggdrasilTeam.RED, new Location(world, 1378.5, 105, -2514.5, -90, 0));
        spawnLocations.put(YggdrasilTeam.GREEN, new Location(world, 1401.5, 105, -2537.5, 0, 0));
        spawnLocations.put(YggdrasilTeam.YELLOW, new Location(world, 1424.5, 105, -2514.5, 90, 0));
        spawnLocations.put(YggdrasilTeam.BLUE, new Location(world, 1401.5, 105, -2491.5, 180, 0));

        fillPlanes.add(new FillPlane(1399, 105, -2494, 1403, 107, -2494));
        fillPlanes.add(new FillPlane(1380, 105, -2517, 1380, 107, -2513));
        fillPlanes.add(new FillPlane(1399, 105, -2536, 1403, 107, -2536));
        fillPlanes.add(new FillPlane(1422, 105, -2517, 1422, 107, -2513));

        resetTeamOptionList();
        final ScoreboardManager scoreboard = Bukkit.getScoreboardManager();
        if (scoreboard == null)
            throw new NullPointerException("ScoreboardManager as null when fetching it from Bukkit");

        for (final YggdrasilTeam team : YggdrasilTeam.values())
            registerScoreboardTeamAndPutInMap(team);
    }

    private static void resetTeamOptionList() {
        teamOptionsLeft.addAll(Arrays.asList(YggdrasilTeam.values()));
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
            player.sendTitle(title, "", 10, 30, 20);
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

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/yggdrasil <[red/blue/yellow/green]/start/cancel/list/leave>");
            return false;
        }

        final String option = args[0];
        try {
            final YggdrasilTeam team = YggdrasilTeam.valueOf(option.toUpperCase());
            if (active || countingDown) {
                sender.sendMessage(ChatColor.RED + "You cannot switch teams while the match is ongoing");
                return false;
            }

            // add player uuid to team list
            final YggdrasilTeam originalTeam = playerEntry.getTeam();
            if (team == originalTeam) {
                sender.sendMessage(ChatColor.RED + String.format(
                    "You're already in %s",
                    "" + ChatColor.BOLD + yggdrasilTeamToChatColor(team) + team.name()
                ));
                return false;
            }

            player.teleport(spawnLocations.get(team));
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
                "" + ChatColor.valueOf(team.name()) + ChatColor.BOLD + team.name() + ChatColor.YELLOW
            ));
            setPlayerInventory(team, player);
            return true;
        } catch (IllegalArgumentException ignore) {}

        switch (option) {
            case "start":
                if (countingDown) {
                    sender.sendMessage(ChatColor.RED + "The match is already counting down");
                    return false;
                }

                if (active) {
                    sender.sendMessage(ChatColor.RED + "The match is currently ongoing");
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
                                for (final Map.Entry<YggdrasilTeam, List<UUID>> entry : teams.entrySet())
                                    teamsLeft.put(entry.getKey(), new ArrayList<>(entry.getValue()));
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
                countingDown = false;
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

    private static Material yggdrasilTeamToWool(YggdrasilTeam team) {
        switch (team) {
            case RED:
                return Material.RED_WOOL;
            case YELLOW:
                return Material.YELLOW_WOOL;
            case GREEN:
                return Material.LIME_WOOL;
            case BLUE:
                return Material.BLUE_WOOL;
            default:
                throw new NullPointerException("Could not get wool type for YggdrasilTeam " + team);
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
            p.setAllowFlight(false);
            p.setCollidable(true);
            p.setGlowing(false);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setSaturation(20);

            p.teleport(START_LOCATION);
            p.setScoreboard(scoreboardManager.getNewScoreboard());
            p.setGameMode(GameMode.SURVIVAL);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            arenaPlayerEntryMap.remove(p.getUniqueId());
            scoreboardTeams.get(pEntry.getTeam()).removeEntry(p.getName());
        });

        teams.clear();
        teamsLeft.clear();

        for (final Iterator<Location> itr = woolBlocksPlaced.iterator(); itr.hasNext();) {
            itr.next().getBlock().setType(Material.AIR);
            itr.remove();
        }

        for (final FillPlane plane : fillPlanes)
            plane.fill(Material.IRON_BARS);

        active = false;
        resetTeamOptionList();
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        final Player player = (Player) event.getEntity();
        final YggdrasilPlayerEntry playerEntry = arenaPlayerEntryMap.get(player.getUniqueId());
        if (playerEntry != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        final Player victim = (Player) event.getEntity();

        // nerf crossbows from 5.5 to 2.5
        if (event instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) event;
            if (entityDamageByEntityEvent.getDamager().getType() == EntityType.ARROW) {
                event.setDamage(6);
            } else if (entityDamageByEntityEvent.getDamager() instanceof Player) {
                final Player player = (Player) entityDamageByEntityEvent.getDamager();
                final UUID uuid = player.getUniqueId();
                final YggdrasilPlayerEntry playerEntry = arenaPlayerEntryMap.get(uuid);
                // return if killer is spectator (has entry but not in teamsLeft)
                if (playerEntry != null
                        && (!teamsLeft.containsKey(playerEntry.getTeam())
                        || !teamsLeft.get(playerEntry.getTeam()).contains(uuid))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // return if player is not dead
        if (victim.getHealth() - event.getFinalDamage() > 0)
            return;

        final UUID uuid = victim.getUniqueId();
        final YggdrasilPlayerEntry playerEntry = arenaPlayerEntryMap.get(uuid);
        // return if player has no entry in arena or if they still have an entry but are not in the teams remaining
        if (playerEntry == null)
            return;

        Player killer = null;
        // if player was killed by player/arrow/entity then make killer, else keep it null (e.g. died by fall damage)
        if (event instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) event;
            // if melee than killer is player else arrow shooter
            killer = entityDamageByEntityEvent.getDamager().getType() == EntityType.ARROW
                ? (Player) ((Arrow) entityDamageByEntityEvent.getDamager()).getShooter()
                : (Player) entityDamageByEntityEvent.getDamager();
        }

        // prevent spectators from hurting remaining players
        if (killer != null) {
            final YggdrasilPlayerEntry killerEntry = arenaPlayerEntryMap.get(killer.getUniqueId());
            if (killerEntry != null
                    && !teamsLeft.get(killerEntry.getTeam()).contains(killer.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        // lightning effect
        world.strikeLightningEffect(victim.getLocation());

        // remove glowing and from scoreboard team
        scoreboardTeams.get(playerEntry.getTeam()).removeEntry(victim.getName());

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
            "%s! %s",
            killer != null
                ? killer.getType() == EntityType.PLAYER
                        ? String.format(
                            "%s of %s was defeated by %s",
                            ChatColor.BOLD + victim.getName() + ChatColor.YELLOW,
                            "" + yggdrasilTeamToChatColor(playerEntry.getTeam()) + ChatColor.BOLD + playerEntry.getTeam().name()
                            + ChatColor.YELLOW,
                            "" + yggdrasilTeamToChatColor(playerEntry.getTeam()) + ChatColor.BOLD + killer.getName() + ChatColor.YELLOW
                        )
                        : String.format(
                            "%s fell to their death",
                            "" + yggdrasilTeamToChatColor(playerEntry.getTeam()) + ChatColor.BOLD + victim.getName() + ChatColor.YELLOW
                        )
                /* death by ender pearl */
                : String.format(
                    "%s died",
                    "" + yggdrasilTeamToChatColor(playerEntry.getTeam()) + ChatColor.BOLD + victim.getName() + ChatColor.YELLOW
                ),
            teamsLeftCount > 1
                ? String.format(
                    "Teams %s with %d players remain!",
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

        event.setCancelled(true);
        final YggdrasilPlayerEntry killerEntry =
            killer != null
                ? arenaPlayerEntryMap.get(killer.getUniqueId())
                : arenaPlayerEntryMap.get(teamsLeft.values().iterator().next().get(0));
        if (teamsLeft.keySet().size() == 1) {
            onWinnerDetermined(killerEntry);
        } else {
            // go into flying mode if match is not over
            victim.setGameMode(GameMode.ADVENTURE);
            victim.setHealth(20);
            victim.setFoodLevel(20);
            victim.getInventory().clear();
            victim.setAllowFlight(true);
            victim.setGlowing(false);
            // can't nudge players or block arrows
            victim.setCollidable(false);
            // give spectator invisibility
            victim.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 255, false,
                false));
        }
    }

    private void removePlayerFromPresentTeams(YggdrasilTeam team, UUID uuid) {
        if (teams.get(team).size() == 1) {
            teamOptionsLeft.add(team);
            teams.remove(team);
        } else {
            teams.get(team).remove(uuid);
        }
    }

    private void onPlayerLeave(Player player, YggdrasilPlayerEntry arenaPlayerEntry) {
        final UUID uuid = player.getUniqueId();
        final YggdrasilTeam yggdrasilTeam = arenaPlayerEntry.getTeam();
        arenaPlayerEntryMap.remove(uuid);
        player.teleport(START_LOCATION);
        player.setGameMode(GameMode.SURVIVAL);
        arenaPlayerEntry.restore(player);
        scoreboardTeams.get(yggdrasilTeam).removeEntry(player.getName());
        // to avoid seeing player colors
        player.setScoreboard(scoreboardManager.getNewScoreboard());

        // if player is in the arena but spectating
        if (!teamsLeft.containsKey(yggdrasilTeam)
                || !teamsLeft.get(yggdrasilTeam).contains(uuid)
                && active) {
            broadcast(ChatColor.YELLOW + String.format(
                "%s has stopped spectating the arena",
                ChatColor.BOLD + player.getDisplayName() + ChatColor.YELLOW
            ));
            removePlayerFromPresentTeams(yggdrasilTeam, uuid);
            return;
        }

        player.setGlowing(false);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        final ChatColor teamColor = yggdrasilTeamToChatColor(yggdrasilTeam);
        if (active) {
            broadcast(ChatColor.YELLOW + String.format(
                "%s of team %s has left the arena",
                "" + teamColor + ChatColor.BOLD + player.getDisplayName() +
                    ChatColor.YELLOW,
                "" + teamColor + ChatColor.BOLD + yggdrasilTeam.name() + ChatColor.YELLOW
            ));
            world.strikeLightningEffect(player.getLocation());
            if (teamsLeft.get(arenaPlayerEntry.getTeam()).size() > 1) {
                teamsLeft.get(arenaPlayerEntry.getTeam()).remove(uuid);
            } else {
                teamsLeft.remove(arenaPlayerEntry.getTeam());
                broadcast(ChatColor.YELLOW + String.format(
                    "Team %s has conceded!",
                    "" + teamColor + ChatColor.BOLD + yggdrasilTeam.name() + ChatColor.YELLOW
                ));
                if (teamsLeft.size() == 1)
                    onWinnerDetermined(arenaPlayerEntryMap.values().iterator().next());
            }
            player.setAllowFlight(false);
            player.setCollidable(true);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        } else {
            broadcast(ChatColor.YELLOW + String.format(
                "%s has left the arena",
                ChatColor.BOLD + player.getDisplayName() + ChatColor.YELLOW
            ));
            removePlayerFromPresentTeams(yggdrasilTeam, uuid);
        }
    }

    @EventHandler
    public static void onPlayerRegeneration(EntityRegainHealthEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        // prevent in-arena health regeneration
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                && arenaPlayerEntryMap.containsKey(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public static void onPlayerBreakBlock(BlockBreakEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        // let players destroy wool but nothing else
        if (arenaPlayerEntryMap.containsKey(uuid)) {
            if (!event.getBlock().getType().name().contains("WOOL"))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public static void onPlayerPlaceBlock(BlockPlaceEvent event) {
        // players can't place wool unless the game has started
        if (arenaPlayerEntryMap.containsKey(event.getPlayer().getUniqueId())
                && !active) {
            event.setCancelled(true);
            return;
        }
        woolBlocksPlaced.add(event.getBlockPlaced().getLocation());
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
    public void onPlayerEnderPearlThrow(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getItem() != null
                && event.getItem().getType() == Material.ENDER_PEARL
                && arenaPlayerEntryMap.containsKey(player.getUniqueId())
                && !active)
            event.setCancelled(true);
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
            if (teamOptionsLeft.size() == 0)
                teamOptionsLeft.addAll(teams.keySet());
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

            setPlayerInventory(team, player);

            // add player to scoreboard team for colored glowing effect
            scoreboardTeams.get(team).addEntry(player.getName());
            player.setScoreboard(yggdrasilScoreboard);

            // add ChatColor.RESET so player name color doesn't change
            player.setDisplayName(ChatColor.RESET + player.getName());

            // health and hunger
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);

            // glowing
            player.setGlowing(true);

            broadcast(ChatColor.GREEN + String.format(
                "%s joined the arena in %s team!",
                ChatColor.BOLD + player.getName() + ChatColor.GREEN,
                "" + yggdrasilTeamToChatColor(team) + ChatColor.BOLD + team.name() + ChatColor.GREEN
            ));
        }
    }

    public static boolean isPlayerInGame(UUID uuid) {
        return arenaPlayerEntryMap.containsKey(uuid);
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

    private Color teamToColor(YggdrasilTeam team) {
        switch (team) {
            case GREEN:
                return Color.GREEN;
            case RED:
                return Color.RED;
            case BLUE:
                return Color.BLUE;
            case YELLOW:
                return Color.YELLOW;
        }
        return null;
    }

    private void setLeatherArmorColor(ItemStack armor, YggdrasilTeam team) {
        final LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        if (meta == null)
            throw new NullPointerException("Could not get item meta for leather armor when setting color");
        meta.setColor(teamToColor(team));
        armor.setItemMeta(meta);
    }

    private void setLeatherArmorPiecesColors(YggdrasilTeam team, ItemStack ...items) {
        for (final ItemStack item : items)
            setLeatherArmorColor(item, team);
    }

    private void setPlayerInventory(YggdrasilTeam team, Player player) {
        final ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        final ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        final ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        final ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        setLeatherArmorPiecesColors(team, helmet, chestplate, leggings, boots);

        final ItemStack shield = new ItemStack(Material.SHIELD);
        final ItemMeta shieldMeta = shield.getItemMeta();
        if (shieldMeta == null)
            throw new NullPointerException("ShieldMeta was null when fetched from crossbow in setting player " +
                "inventory");
        ((Damageable) shieldMeta).setDamage(10);

        final ItemStack sword = new ItemStack(Material.STONE_SWORD);
        final ItemStack axe = new ItemStack(Material.STONE_AXE);
        final ItemStack crossbow = new ItemStack(Material.CROSSBOW);

        // add pre-loaded crossbow arrow
        final CrossbowMeta crossbowMeta = (CrossbowMeta) crossbow.getItemMeta();
        if (crossbowMeta == null)
            throw new NullPointerException("CrossbowMeta was null when fetched from crossbow in setting player " +
                "inventory");

        crossbowMeta.addChargedProjectile(new ItemStack(Material.ARROW));
        crossbow.setItemMeta(crossbowMeta);

        final ItemStack bow = new ItemStack(Material.BOW);
        final ItemStack arrows = new ItemStack(Material.ARROW, 16);
        setItemsUnbreakable(helmet, chestplate, leggings, boots, sword, axe, crossbow, bow);

        final ItemStack potion = new ItemStack(Material.POTION);
        final PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        if (potionMeta == null)
            throw new NullPointerException("Could not get PotionMeta for potion when giving Yggdrasil inventory");
        potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 10, 1), true);
        potionMeta.setDisplayName(ChatColor.RESET + "Potion of Healing");
        potionMeta.setColor(Color.FUCHSIA);
        potion.setItemMeta(potionMeta);

        final ItemStack blocks = new ItemStack(yggdrasilTeamToWool(team), 16);
        final ItemStack enderpearl = new ItemStack(Material.ENDER_PEARL);
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
        inventory.setItem(6, blocks);
        inventory.setItem(7, enderpearl);
        player.updateInventory();
    }

}