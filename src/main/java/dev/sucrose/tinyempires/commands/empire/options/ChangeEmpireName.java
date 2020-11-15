package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChangeEmpireName implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e name <name>
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

        final String name = StringUtils.buildWordsFromArray(args, 0);
        if (name.length() > 30) {
            sender.sendMessage(ChatColor.RED + "You cannot have an empire name longer than 30 characters");
            return;
        }

        empire.broadcast(ChatColor.GREEN, String.format(
            "%s changed the empire's name to %s",
            sender.getName(),
            "" + ChatColor.BOLD + empire.getChatColor() + name
        ));
        Bukkit.broadcastMessage(ChatColor.GREEN + String.format(
            "The empire of %s changed its name to %s",
            "" + ChatColor.BOLD + empire.getChatColor() + empire.getName() + ChatColor.GREEN,
            "" + ChatColor.BOLD + empire.getChatColor() + name
        ));
        empire.setName(name);
    }

    @Override
    public String getDescription() {
        return "Change empire name";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.EDIT;
    }

    @Override
    public String getUsage() {
        return "/e name <name>";
    }

}
