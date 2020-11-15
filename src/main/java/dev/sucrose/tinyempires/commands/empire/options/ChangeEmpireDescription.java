package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChangeEmpireDescription implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e desc <description>
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

        if (!tePlayer.hasPermission(Permission.EDIT)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.EDIT));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String description = StringUtils.buildWordsFromArray(args, 0);
        empire.setDescription(description);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s changed the empire description to %s",
            sender.getName(),
            ChatColor.ITALIC + description
        ));
    }

    @Override
    public String getDescription() {
        return "Set empire description";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.EDIT;
    }

    @Override
    public String getUsage() {
        return "/e desc <description>";
    }

}
