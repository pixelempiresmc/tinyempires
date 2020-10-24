package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.discord.DiscordBot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChat implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()) {
            event.setCancelled(true);
            Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + String.format(
                "[GOD] <%s> %s",
                player.getName(),
                event.getMessage()
            ));
        }

        final String message = String.format(
            "[Minecraft] %s » %s",
            player.getName(),
            event.getMessage()
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
        );
        DiscordBot.sendMessageInBridgeChat(player.isOp() ? "**" + message + "**" : message);
    }

}
