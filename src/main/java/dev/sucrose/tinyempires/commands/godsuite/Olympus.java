package dev.sucrose.tinyempires.commands.godsuite;

import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Olympus implements CommandExecutor {

    private static final Location olympus = new Location(Bukkit.getWorld("world"), 1399, 64, -2515);

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /e dimension <dimension>
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        ((Player) sender).teleport(olympus);
        return true;
    }

}
