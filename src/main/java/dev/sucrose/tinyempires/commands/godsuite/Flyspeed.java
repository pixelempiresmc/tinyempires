package dev.sucrose.tinyempires.commands.godsuite;

import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Flyspeed implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /flyspeed 0-10
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e flyspeed 0-10");
            return false;
        }

        try {
            final float speed = Float.parseFloat(args[0]);
            if (speed < 0 || speed > 10) {
                sender.sendMessage(ChatColor.RED + "/e flyspeed 0-10");
                return false;
            }
            ((Player) sender).setFlySpeed(speed / 10);
        } catch (Exception err) {
            sender.sendMessage(ChatColor.RED + "/e flyspeed 0-10");
        }
        return true;
    }
}
