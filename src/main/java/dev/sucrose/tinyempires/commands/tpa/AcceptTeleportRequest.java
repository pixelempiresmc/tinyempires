package dev.sucrose.tinyempires.commands.tpa;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.commands.arena.Atlantis;
import dev.sucrose.tinyempires.commands.arena.Yggdrasil;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AcceptTeleportRequest implements CommandExecutor, Listener {

    private static final Map<UUID, Integer> teleportationTaskIds = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /e tpreject
        final Player player = (Player) sender;
        final UUID senderUUID = player.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return false;
        }

        final UUID requesterUUID = TeleportRequest.getReceiverToSenderTpRequest(senderUUID);
        if (requesterUUID == null) {
            sender.sendMessage(ChatColor.RED + "You do not currently have any teleport requests");
            return false;
        }

        final Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester == null) {
            sender.sendMessage(ChatColor.RED + "The tpa requester is no longer online");
            return false;
        }

        if (Atlantis.isPlayerInGame(requesterUUID) || Yggdrasil.isPlayerInGame(requesterUUID)) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is currently in an arena and cannot be teleported to",
                ChatColor.BOLD + requester.getName() + ChatColor.RED
            ));
            return false;
        }

        final Location senderLocation = player.getLocation().clone();
        requester.sendMessage(ChatColor.GREEN + String.format(
            "TPA accepted! Teleporting to %s in 5 seconds, don't move...",
            ChatColor.BOLD + player.getDisplayName() + ChatColor.GREEN
        ));
        teleportationTaskIds.put(
            requesterUUID,
            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                TinyEmpires.getInstance(),
                new Runnable() {
                    private int timer = 4;

                    @Override
                    public void run() {
                        if (timer == 0) {
                            requester.teleport(senderLocation);
                            requester.sendMessage(ChatColor.GREEN + String.format(
                                "Teleported to %s!",
                                ChatColor.BOLD + player.getDisplayName() + ChatColor.GREEN
                            ));
                            Bukkit.getScheduler().cancelTask(teleportationTaskIds.get(requesterUUID));
                            TeleportRequest.removeReceiverToSenderTpRequest(senderUUID);
                            TeleportRequest.removeSenderToReceiverTpRequest(requesterUUID);
                            return;
                        }
                        requester.sendMessage(ChatColor.GREEN + String.format(
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
        return true;
    }

    @EventHandler
    public static void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (to == null)
            return;

        if ((from.getX() != to.getX()
                || from.getY() != to.getY()
                || from.getZ() != to.getZ())
                && teleportationTaskIds.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(teleportationTaskIds.get(uuid));

            final UUID tpReceiverUUID = TeleportRequest.getSenderToReceiverTpRequest(uuid);
            final Player receiver = Bukkit.getPlayer(tpReceiverUUID);
            if (receiver != null) {
                receiver.sendMessage(ChatColor.RED + String.format(
                    "%s failed to teleport to your location, accept or reject request again",
                    ChatColor.BOLD + player.getDisplayName() + ChatColor.RED
                ));
                player.sendMessage(ChatColor.RED + String.format(
                    "You moved! Teleport cancelled. %s must accept your request again for you to teleport",
                    ChatColor.RED + receiver.getName() + ChatColor.RED
                ));
                return;
            }

            player.sendMessage(ChatColor.RED +
                "You moved! Cancelling teleport. The request receiver is not currently online so your teleport " +
                "request cannot be accepted again"
            );
        }
    }

}
