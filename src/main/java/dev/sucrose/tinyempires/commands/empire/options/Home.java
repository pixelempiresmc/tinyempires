package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Home implements CommandOption {

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
                            Bukkit.getScheduler().cancelTask(playerToTeleportationTask.get(senderUUID));
                            playerToTeleportationTask.remove(senderUUID);
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
        sender.teleport(empire.getHomeLocation());
    }

    @EventHandler
    public static void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        if (playerToTeleportationTask.containsKey(uuid)) {
            playerToTeleportationTask.remove(uuid);
            player.sendMessage(ChatColor.RED + "You moved! Cancelling teleport...");
        }
    }

}
