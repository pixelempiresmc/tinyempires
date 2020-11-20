package dev.sucrose.tinyempires.commands.debug;

import dev.sucrose.tinyempires.discord.DiscordBot;
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

public class CloseBot implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        DiscordBot.close();
        sender.sendMessage(ChatColor.GREEN + "Shutdown bot");
        return true;
    }

}
