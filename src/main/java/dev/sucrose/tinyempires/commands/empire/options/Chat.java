package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Chat implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e chat <message>
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
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String message = StringUtils.buildWordsFromArray(args, 0);
        empire.broadcastText(ChatColor.ITALIC + String.format(
            "%s sent message to empire: %s",
            ChatColor.BOLD + tePlayer.getName() + ChatColor.ITALIC,
            message
        ));
    }

    @Override
    public String getDescription() {
        return "Send chat message to all members in the empire currently online";
    }

    @Override
    public Permission getPermissionRequired() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/e chat <message>";
    }

}
