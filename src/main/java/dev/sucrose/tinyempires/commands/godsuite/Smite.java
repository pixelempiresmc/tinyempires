package dev.sucrose.tinyempires.commands.godsuite;

import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Smite implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /smite <player> [hearts]
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/smite <player> [hearts]");
            return false;
        }

        final String name = args[0];
        final Player player = Bukkit.getPlayer(name);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not currently online or is not an existing player",
                name
            ));
            return false;
        }

        if (args.length > 1) {
            // parse damage
            int damage;
            try {
                damage = Integer.parseInt(args[1]);
            } catch (Exception ignore) {
                sender.sendMessage(ChatColor.RED + "/smite <player> [hearts]");
                return false;
            }

            if (damage < 1 || damage > 20) {
                sender.sendMessage(ChatColor.RED + "Damage must be between 1 and 20 hearts");
                return false;
            }
            player.damage(damage);
        }

        player.getWorld().strikeLightning(player.getLocation());
        Bukkit.broadcastMessage(ChatColor.GOLD + String.format(
            "%s was smitten by the god %s!",
            name,
            player.getName()
        ));
        return true;
    }
}
