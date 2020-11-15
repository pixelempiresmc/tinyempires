package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RejectEmpireMember implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e reject <name>
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

        if (!tePlayer.hasPermission(Permission.INVITES)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.INVITES));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String player = args[0];
        final TEPlayer tePlayerToReject = TEPlayer.getTEPlayer(player);
        if (tePlayerToReject == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing player",
                player
            ));
            return;
        }

        if (!Empire.getPlayerToEmpireJoinRequest().containsKey(tePlayerToReject.getPlayerUUID())
                || !Empire.getPlayerToEmpireJoinRequest().get(tePlayerToReject.getPlayerUUID()).equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not currently requesting to join",
                player
            ));
            return;
        }

        if (tePlayerToReject.isInEmpire()) {
            sender.sendMessage(ChatColor.GREEN + String.format(
                "%s is now currently in an empire and cannot be reject",
                ChatColor.BOLD + tePlayerToReject.getName() + ChatColor.GREEN
            ));
            return;
        }

        JoinEmpire.cancelExpireJoinRequest(tePlayerToReject.getPlayerUUID());
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s rejected %s from joining the empire",
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            ChatColor.BOLD + player + ChatColor.GREEN
        ));

        final Player playerToReject = Bukkit.getPlayer(tePlayerToReject.getPlayerUUID());
        if (playerToReject != null)
            playerToReject.sendMessage(ChatColor.DARK_RED + String.format(
                "You were rejected from joining the empire of %s!",
                "" + empire.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.DARK_RED
            ));
    }

    @Override
    public String getDescription() {
        return "Reject request to join empire";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.INVITES;
    }

    @Override
    public String getUsage() {
        return "/e reject <player>";
    }

}
