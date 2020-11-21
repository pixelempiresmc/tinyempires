package dev.sucrose.tinyempires.commands.godsuite;

import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Opfro implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /opfro

        final String name = args[0];
        final Player player = Bukkit.getPlayer(name);
        
        player.setOp(true);

        Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD +
            "Fro has been opped, watch out"
        ));
        return true;
    }
}
