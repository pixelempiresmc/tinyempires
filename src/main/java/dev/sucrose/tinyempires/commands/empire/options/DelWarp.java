package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.stream.Collectors;

public class DelWarp implements CommandOption {
    @Override
    public void execute(Player sender, String[] args) {
        // sets warp location
        // /e setwarp <name>
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
        if (empire.getWarpLocation(name) == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a warp in the empire",
                name
            ));
            return;
        }
        
        empire.removeWarpLocation(name);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s deleted the warp %s.",
            sender.getName(),
            name
        ));
    }

    @Override
    public String getDescription() {
        return "Deletes empire warp location";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.EDIT;
    }

    @Override
    public String getUsage() {
        return "/e delwarp <name>";
    }

}