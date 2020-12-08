package dev.sucrose.tinyempires.commands.debug;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEChest;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DumpCache implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // dumps plugin cache into Mongo
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        try {
            Empire.writeCache();
            TEPlayer.writeCache();
            TEChest.writeCache();
            TEChunk.writeCache();
        } catch (Exception err) {
            err.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Failed to encode and write caches, check the server logs");
        }

        sender.sendMessage(ChatColor.GREEN + "Wrote caches to disk");
        return true;
    }

}
