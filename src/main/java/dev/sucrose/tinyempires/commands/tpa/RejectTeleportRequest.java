package dev.sucrose.tinyempires.commands.tpa;

import dev.sucrose.tinyempires.TinyEmpires;
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

public class RejectTeleportRequest implements CommandExecutor, Listener {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /e tpareject
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

        requester.sendMessage(ChatColor.GREEN + "Your teleport request was rejected!");
        TeleportRequest.removeReceiverToSenderTpRequest(senderUUID);
        TeleportRequest.removeSenderToReceiverTpRequest(requesterUUID);
        return true;
    }

}
