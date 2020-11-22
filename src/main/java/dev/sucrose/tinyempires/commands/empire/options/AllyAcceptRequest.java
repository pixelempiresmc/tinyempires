package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.discord.DiscordBot;
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

public class AllyAcceptRequest implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e allyaccept <empire>
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
                || !Objects.equals(Empire.getAllyRequestForEmpire(empireRequestingToAlly.getId()), empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not currently requesting to ally",
                "" + ChatColor.BOLD + empireRequestingToAlly.getChatColor() + empireName + ChatColor.RED
            ));
            return;
        }

        // checks passed, make alliance
        empire.addAlliedEmpire(empireRequestingToAlly.getId());
        empireRequestingToAlly.addAlliedEmpire(empire.getId());
        Ally.cancelEmpireAllyRequestExpire(empireRequestingToAlly.getId());
        Empire.removeAllyRequest(empireRequestingToAlly.getId());
        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s accepted the ally request and has made the empire ally with %s!",
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            "" + empireRequestingToAlly.getChatColor() + empireRequestingToAlly.getName() + ChatColor.GREEN
        ));
        empireRequestingToAlly.broadcastText(ChatColor.GREEN + String.format(
            "%s has accepted the request and is now allied with the empire!",
            "" + ChatColor.BOLD + empireRequestingToAlly.getName() + ChatColor.GREEN
        ));
    }

    @Override
    public String getDescription() {
        return "Accept another empire's request to ally";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.ALLIES;
    }

    @Override
    public String getUsage() {
        return "/e allyaccept <empire>";
    }

}
