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
        
        sender.sendMessage(ChatColor.RED + "We're fixing tpa! Please be patient!");
 
        return false;
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
