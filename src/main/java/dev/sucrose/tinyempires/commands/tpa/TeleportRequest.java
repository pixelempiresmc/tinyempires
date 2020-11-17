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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportRequest implements CommandOption, Listener {

    private static final Map<UUID, UUID> playerTeleportationRequests = new HashMap<>();

    @Override
    public void execute(Player sender, String[] args) {
        // teleports player to another player in their empire after staying in place for 5s
        // /e tpa <player>
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

        if (args.length < 1) {
            sender.sendMessage(getUsage());
            return;
        }

        final String requestReceiverName = args[0];
        final Player requestReceiver = Bukkit.getPlayer(requestReceiverName);
        if (requestReceiver == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not currently online",
                ChatColor.BOLD + requestReceiverName
            ));
            return;
        }

        requestReceiver.sendMessage(ChatColor.YELLOW + String.format(
            "%s has requested to teleport to you! You have 30 seconds to accept (%s/tpaccept %s%s to accept, " +
                "%s/tpreject %s%s to reject)",
            ChatColor.BOLD + sender.getName() + ChatColor.YELLOW,
            ChatColor.BOLD,
            ChatColor.YELLOW,
            ChatColor.BOLD,
            ChatColor.YELLOW
        ));
        sender.sendMessage(ChatColor.GREEN + String.format(
            "Requested to teleport to %s",
            ChatColor.BOLD + requestReceiverName
        ));
        playerTeleportationRequests.put(senderUUID, requestReceiver.getUniqueId());
    }

    @Override
    public String getDescription() {
        return "Request to teleport to another player";
    }

    @Override
    public Permission getPermissionRequired() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/e tpa <player>";
    }

}
