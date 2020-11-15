package dev.sucrose.tinyempires.commands;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import net.dv8tion.jda.api.entities.User;
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

        final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
        if (tePlayer == null) {
            sender.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return false;
        }

        if (tePlayer.getDiscordId() != null) {
            final User user = DiscordBot.getDiscordUsernameFromId(tePlayer.getDiscordId());
            if (user != null
                    && args.length > 1
                    && !args[1].equals("relink")) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + String.format(
                    "You already have a Discord account linked (%s)! Run %s/discord relink%s to link your account " +
                        "again.",
                    user.getAsTag(),
                    ChatColor.BOLD,
                    ChatColor.LIGHT_PURPLE
                ));
                return false;
            }
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
