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
import java.util.stream.Collectors;

public class CreateEmpirePosition implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e position <name>
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

        if (!tePlayer.hasPermission(Permission.POSITIONS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.POSITIONS));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String position = StringUtils.buildWordsFromArray(args, 0);
        if (empire.getPosition(position) != null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Position %s already exists (%s)",
                position,
                empire.getPosition(position)
                    .getPermissions()
                    .stream()
                    .map(p -> p.name().toLowerCase())
                    .collect(Collectors.joining(", "))
            ));
            return;
        }

        if (position.length() > 30) {
            sender.sendMessage(ChatColor.RED + "You cannot have a position name longer than 30 characters");
            return;
        }

        empire.createPosition(position);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s created a new position %s",
            sender.getName(),
            position
        ));
    }

    @Override
    public String getDescription() {
        return "Create new empire position";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.POSITIONS;
    }

    @Override
    public String getUsage() {
        return "/e position <name>";
    }

}
