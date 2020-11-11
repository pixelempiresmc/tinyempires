package dev.sucrose.tinyempires.commands;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.CommandOption;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LinkDiscordAccount implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();
        final String existingLinkCode = DiscordBot.getPlayerLinkCode(uuid);
        if (existingLinkCode != null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "You already have a link code (%s)",
                ChatColor.BOLD + existingLinkCode + ChatColor.RED
            ));
            return false;
        }

        final String code = DiscordBot.addPendingLinkCode(uuid);
        sender.sendMessage(ChatColor.LIGHT_PURPLE + String.format(
            "Sent link request! DM %s to PixelBot on the Pixel Empires Discord Server to link your Discord account. " +
                "This code will be invalidated in 60 seconds if it is not linked with your account.",
            ChatColor.BOLD + code + ChatColor.LIGHT_PURPLE
        ));
        DiscordBot.addPendingLinkCode(uuid);
        return true;
    }

}
