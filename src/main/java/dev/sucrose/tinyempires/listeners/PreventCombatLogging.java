package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.commands.arena.Atlantis;
import dev.sucrose.tinyempires.commands.arena.Yggdrasil;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class PreventCombatLogging implements Listener {

    private final static Set<UUID> recentlyHitPlayers = new HashSet<>();
    private final static Map<UUID, Integer> playerToGiveCombatLogTaskId = new HashMap<>();

    @EventHandler
    public static void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // add player to last hit if they were hit by a player or projectile shot by player
        final Player killer =
            event.getDamager() instanceof Player
                ? (Player) event.getDamager()
                : event.getDamager() instanceof Projectile
                    ? (Player) ((Projectile) event.getDamager()).getShooter()
                    : null;
        if (killer == null)
            return;

        final UUID killerUUID = killer.getUniqueId();
        final TEPlayer killerTePlayer = TEPlayer.getTEPlayer(killerUUID);
        if (killerTePlayer == null)
            throw new NullPointerException("Could not get TEPlayer for " + killerUUID);

        final Player player = (Player) event.getEntity();
        final TEPlayer victimTePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        if (victimTePlayer == null)
            throw new NullPointerException("Could not get TEPlayer for " + player.getUniqueId());

        // return if player is in an arena
        if (Yggdrasil.isPlayerInGame(player.getUniqueId())
                || Atlantis.isPlayerInGame(player.getUniqueId()))
            return;

        // return if in the same empire
        if (((killerTePlayer.getEmpire() == null && victimTePlayer.getEmpire() == null)
                || (killerTePlayer.getEmpire() != null
                        && killerTePlayer.getEmpire().getId().equals(
                            victimTePlayer.getEmpire() == null
                                ? null
                                : victimTePlayer.getEmpire().getId()
                            ))))
            return;

        recentlyHitPlayers.add(player.getUniqueId());
        player.sendMessage(ChatColor.RED
            + "You were hit by an enemy player! Logging off within the next 5 seconds will result in death");

        // reset timer
        if (playerToGiveCombatLogTaskId.containsKey(player.getUniqueId()))
            Bukkit.getScheduler().cancelTask(playerToGiveCombatLogTaskId.get(player.getUniqueId()));

        playerToGiveCombatLogTaskId.put(
            player.getUniqueId(),
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                TinyEmpires.getInstance(),
                () -> {
                    recentlyHitPlayers.remove(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "You can now log off");
                },
                20 * 5 /* five seconds */
            )
        );
    }

    @EventHandler
    public static void onPlayerLeaveEvent(PlayerQuitEvent event) {
        if (recentlyHitPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().setHealth(0);
            Bukkit.broadcastMessage(ChatColor.RED + String.format(
                "%s logged off within 5 seconds of being hit and has died!",
                ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RED
            ));
        }
    }

}
