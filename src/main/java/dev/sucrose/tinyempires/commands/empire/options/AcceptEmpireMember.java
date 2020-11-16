package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AcceptEmpireMember implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e accept <name>
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
        final TEPlayer tePlayerToAccept = TEPlayer.getTEPlayer(player);
        if (tePlayerToAccept == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing player",
                player
            ));
            return;
        }

        if (!Empire.getPlayerToEmpireJoinRequest().containsKey(tePlayerToAccept.getPlayerUUID())
                || !Empire.getPlayerToEmpireJoinRequest().get(tePlayerToAccept.getPlayerUUID()).equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not currently requesting to join",
                player
            ));
            return;
        }

        if (tePlayerToAccept.isInEmpire()) {
            sender.sendMessage(ChatColor.GREEN + String.format(
                "%s is now currently in an empire and cannot be accepted",
                ChatColor.BOLD + tePlayerToAccept.getName() + ChatColor.GREEN
            ));
            return;
        }

        empire.acceptPlayerJoinRequest(tePlayerToAccept);
        JoinEmpire.cancelExpireJoinRequest(tePlayerToAccept.getPlayerUUID());
        Empire.getPlayerToEmpireJoinRequest().remove(tePlayerToAccept.getPlayerUUID());
        if (tePlayer.getDiscordId() != null)
            DiscordBot.giveUserEmpireDiscordRole(tePlayer, empire);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s accepted %s to the empire. Welcome!",
            sender.getName(),
            player
        ));
    }

    @Override
    public String getDescription() {
        return "Accept request to join empire";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.INVITES;
    }

    @Override
    public String getUsage() {
        return "/e accept <player>";
    }

}
