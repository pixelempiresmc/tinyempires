package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class AllyRejectRequest implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e allyreject <empire>
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

        if (!tePlayer.hasPermission(getPermissionRequired())) {
            sender.sendMessage(ErrorUtils.generatePermissionError(getPermissionRequired()));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String empireName = StringUtils.buildWordsFromArray(args, 0);
        final Empire empireRequestingToAlly = Empire.getEmpire(empireName);
        if (empireRequestingToAlly == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing empire",
                empireName
            ));
            return;
        }

        if (Empire.getAllyRequestForEmpire(empireRequestingToAlly.getId()) == null
                || Objects.equals(Empire.getAllyRequestForEmpire(empireRequestingToAlly.getId()), empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not currently requesting to join",
                "" + ChatColor.BOLD + empireRequestingToAlly.getChatColor() + empireName + ChatColor.RED
            ));
            return;
        }

        // checks passed, reject alliance
        Ally.cancelEmpireAllyRequestExpire(empireRequestingToAlly.getId());
        Empire.removeAllyRequest(empireRequestingToAlly.getId());
        empire.broadcastText(ChatColor.RED + String.format(
            "%s has rejected the request to ally with %s",
            ChatColor.BOLD + sender.getName() + ChatColor.RED,
            "" + empireRequestingToAlly.getChatColor() + empireRequestingToAlly.getName() + ChatColor.RED
        ));
        empireRequestingToAlly.broadcastText(ChatColor.RED + String.format(
            "%s has rejected the request to ally with the empire",
            "" + ChatColor.BOLD + empireRequestingToAlly.getName() + ChatColor.RED
        ));
    }

    @Override
    public String getDescription() {
        return "Reject an empire's request to ally";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.ALLIES;
    }

    @Override
    public String getUsage() {
        return "/e allyreject <empire>";
    }

}
