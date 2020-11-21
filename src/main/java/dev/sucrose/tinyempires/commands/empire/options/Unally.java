package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Unally implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e unally <empire>
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
        if (empireName.equals(empire.getName())) {
            sender.sendMessage(ChatColor.RED + "You cannot un-ally with your own empire");
            return;
        }

        final Empire empireToUnally = Empire.getEmpire(empireName);
        if (empireToUnally == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing empire",
                empireName
            ));
            return;
        }

        if (!empire.getAllies().contains(empireToUnally.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "You are not currently allies with %s",
                "" + empireToUnally.getChatColor() + ChatColor.BOLD + empireToUnally.getName()
            ));
            return;
        }

        if (!empireToUnally.isAnyMemberOnline()) {
            sender.sendMessage(ChatColor.RED + String.format(
                "No-one on %s is currently online and can accept your alliance request",
                "" + empire.getChatColor() + ChatColor.BOLD + empire.getName()
            ));
            return;
        }

        // checks passed, un-ally with empire
        empire.removeAlliedEmpire(empireToUnally.getId());
        empireToUnally.removeAlliedEmpire(empire.getId());

        empireToUnally.broadcast(ChatColor.RED, String.format(
            "The empire of %s has un-allied with the empire!",
            "" + empire.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.RED
        ));

        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s has un-allied the empire with %s",
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            "" + empireToUnally.getChatColor() + ChatColor.BOLD + empireToUnally.getName() + ChatColor.GREEN
        ));
    }

    @Override
    public String getDescription() {
        return "Un-ally with an empire";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.ALLIES;
    }

    @Override
    public String getUsage() {
        return "/e unally <empire>";
    }

}
