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

        player.getWorld().strikeLightning(player.getLocation());
        if (args.length > 1) {
            // parse damage
            if (args[1].equals("half")) {
                // set player to half a heart
                player.setHealth(1);
            } else {
                int damage;
                try {
                    damage = Integer.parseInt(args[1]);
                } catch (Exception ignore) {
                    sender.sendMessage(ChatColor.RED + "/smite <player> [hearts|\"half\"]");
                    return false;
                }

                if (damage < 1 || damage > 20) {
                    sender.sendMessage(ChatColor.RED + "Damage must be between 1 and 20 hearts");
                    return false;
                }

                player.setHealth(player.getHealth() - Math.min(damage, player.getHealth()));
            }
        }

        Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + String.format(
            "%s was smitten by the god %s!",
            name,
            sender.getName()
        ));
        return true;
    }
}
