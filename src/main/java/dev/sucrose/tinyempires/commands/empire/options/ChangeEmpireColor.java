package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChangeEmpireColor implements EmpireCommandOption {

    private static final String allColorOptions;
    static {
        StringBuilder allColorOptionsBuilder = new StringBuilder();
        for (int index = 0; index < Color.values().length; index++) {
            final Color color = Color.values()[index];
            allColorOptionsBuilder.append(ChatColor.valueOf(color.name())).append(color.name().toLowerCase()).append(index == Color.values().length - 1 ? "" : ChatColor.RED + "/");
        }
        allColorOptions = allColorOptionsBuilder.toString();
    }

    @Override
    public void execute(Player sender, String[] args) {
        // /e color <color>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to run this command");
            return;
        }

        if (!tePlayer.getPosition().hasPermission(Permission.EDIT)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.EDIT));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Color not specified (%s)",
                allColorOptions + ChatColor.RED
            ));
            return;
        }

        final String newColor = args[0];
        boolean isColor = false;
        for (ChatColor color : ChatColor.values()) {
            if (color.name().toLowerCase().equals(newColor)) {
                isColor = true;
                break;
            }
        }

        if (!isColor) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a valid color (%s)",
                newColor,
                allColorOptions + ChatColor.RED
            ));
            return;
        }

        final Color color = Color.valueOf(newColor.toUpperCase());
        empire.setColor(color);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s changed the empire color to %s",
            sender.getName(),
            ChatColor.valueOf(color.name()) + newColor
        ));
    }

}
