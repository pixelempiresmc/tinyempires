package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
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
        event.setCancelled(true);
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        if (tePlayer == null) {
            player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (player.isOp()) {
            event.setCancelled(true);
            Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + String.format(
                "[GOD] <%s> %s",
                player.getName(),
                event.getMessage()
            ));
        } else {
            Bukkit.broadcastMessage(String.format(
                "%s <%s> %s",
                (empire == null
                    ? ChatColor.GRAY + "Unaffiliated"
                    : "[" + empire.getChatColor() + empire.getName() + "]") + ChatColor.WHITE,
                tePlayer.getName(),
                event.getMessage()
            ));
        }

        final String message = String.format(
            "[%s] %s » %s",
            empire == null
                ? "Unaffiliated"
                : empire.getName(),
            player.getName(),
            event.getMessage()
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
        );
        DiscordBot.sendMessageInBridgeChat(player.isOp() ? "**" + message + "**" : message);
    }

}
