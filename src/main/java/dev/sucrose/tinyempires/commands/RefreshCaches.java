package dev.sucrose.tinyempires.commands;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEChest;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RefreshCaches implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        Empire.fillCache();
        TEPlayer.fillCache();
        TEChest.fillCache();
        TEChunk.fillCache();

        sender.sendMessage(ChatColor.GREEN + "Cleared empire, player, chest-to-player mapping and chunk caches");
        return true;
    }
}
