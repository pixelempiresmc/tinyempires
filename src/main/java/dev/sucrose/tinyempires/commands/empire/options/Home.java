package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.listeners.PlayerLeave;
import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Home implements CommandOption, Listener {

    private static final Map<UUID, Integer> playerToTeleportationTask = new HashMap<>();

    @Override
    public void execute(Player sender, String[] args) {
        // teleports player to home after staying in place for 5s
        // /e home
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_MUST_BE_IN_AN_EMPIRE);
            return;
        }

        // clone so player is teleported to original location if home is changed mid-teleport-countdown
        final Location homeLocation = empire.getHomeLocation().clone();
        sender.sendMessage(ChatColor.GREEN + "Teleporting to home in 5 seconds, don't move...");
        playerToTeleportationTask.put(
            senderUUID,
            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                TinyEmpires.getInstance(),
                new Runnable() {
                    private int timer = 4;

                    @Override
                    public void run() {
                        if (timer == 0) {
                            sender.teleport(homeLocation);
                            sender.sendMessage(ChatColor.GREEN + "Teleported to home!");
                            cancelPlayerTeleport(senderUUID);
                            return;
                        }
                        sender.sendMessage(ChatColor.GREEN + String.format(
                            "Teleporting in %d second%s...",
                            timer,
                            timer > 1 ? "s" : ""
                        ));
                        timer--;
                    }
                },
                20,
                20
            )
        );
    }

    @EventHandler
    public static void onPlayerLeave(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        cancelPlayerTeleport(uuid);
    }

    @EventHandler
    public static void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (to == null)
            return;

        if (from.getX() != to.getZ()
                && from.getY() != to.getY()
                && from.getZ() != to.getZ()
                && playerToTeleportationTask.containsKey(uuid)) {
            cancelPlayerTeleport(uuid);
            player.sendMessage(ChatColor.RED + "You moved! Cancelling teleport...");
        }
    }

    private static void cancelPlayerTeleport(UUID uuid) {
        if (!playerToTeleportationTask.containsKey(uuid))
            return;
        Bukkit.getScheduler().cancelTask(playerToTeleportationTask.get(uuid));
        playerToTeleportationTask.remove(uuid);
    }

}
