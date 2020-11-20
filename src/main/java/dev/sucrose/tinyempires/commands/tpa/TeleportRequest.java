package dev.sucrose.tinyempires.commands.tpa;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.commands.arena.Atlantis;
import dev.sucrose.tinyempires.commands.arena.Yggdrasil;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.Permission;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportRequest implements CommandExecutor {

    // use two maps for bi-directionality
    private static final Map<UUID, UUID> senderToReceiverTeleportationRequests = new HashMap<>();
    private static final Map<UUID, UUID> receiverToSenderTeleportationRequests = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /e tpa <player>
        final Player player = (Player) sender;
        final UUID senderUUID = player.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/tpa <player>");
            return false;
        }

        final String requestReceiverName = args[0];
        final Player requestReceiver = Bukkit.getPlayer(requestReceiverName);
        if (requestReceiver == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not currently online",
                ChatColor.BOLD + requestReceiverName
            ));
            return false;
        }

        if (senderToReceiverTeleportationRequests.containsKey(senderUUID)) {
            sender.sendMessage(ChatColor.RED + String.format(
                "You have already requested to %s",
                ChatColor.BOLD + requestReceiverName
            ));
            return false;
        }

        requestReceiver.sendMessage(ChatColor.YELLOW + String.format(
            "%s has requested to teleport to you! You have 30 seconds to accept (%s/tpaccept %s%s to accept, " +
                "%s/tpreject %s%s to reject)",
            ChatColor.BOLD + sender.getName() + ChatColor.YELLOW,
            ChatColor.BOLD,
            sender.getName(),
            ChatColor.YELLOW,
            ChatColor.BOLD,
            sender.getName(),
            ChatColor.YELLOW
        ));
        sender.sendMessage(ChatColor.GREEN + String.format(
            "Requested to teleport to %s",
            ChatColor.BOLD + requestReceiverName
        ));
        receiverToSenderTeleportationRequests.put(requestReceiver.getUniqueId(), senderUUID);
        senderToReceiverTeleportationRequests.put(senderUUID, requestReceiver.getUniqueId());
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            TinyEmpires.getInstance(),
            () -> {
                receiverToSenderTeleportationRequests.remove(requestReceiver.getUniqueId());
                receiverToSenderTeleportationRequests.remove(senderUUID);
                requestReceiver.sendMessage(ChatColor.RED + String.format(
                    "You took too long to accept or reject %s's teleport request and it has expired",
                    ChatColor.BOLD + player.getName() + ChatColor.RED
                ));
                requestReceiver.sendMessage(ChatColor.RED + String.format(
                    "%s took too long to accept or reject your teleport request and it has expired",
                    ChatColor.BOLD + requestReceiverName + ChatColor.RED
                ));
            },
            20 * 30
        );
        return true;
    }

    public static UUID getReceiverToSenderTpRequest(UUID uuid) {
        return receiverToSenderTeleportationRequests.get(uuid);
    }

    public static void removeReceiverToSenderTpRequest(UUID uuid) {
        receiverToSenderTeleportationRequests.remove(uuid);
    }

    public static UUID getSenderToReceiverTpRequest(UUID uuid) {
        return senderToReceiverTeleportationRequests.get(uuid);
    }

    public static void removeSenderToReceiverTpRequest(UUID uuid) {
        senderToReceiverTeleportationRequests.remove(uuid);
    }

}
