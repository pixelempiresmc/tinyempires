package dev.sucrose.tinyempires.listeners;

import com.mongodb.client.MongoCollection;
import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.CensorUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

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

        final String content = CensorUtils.censorCurses(event.getMessage());
        final Empire empire = tePlayer.getEmpire();
        if (player.isOp()) {
            event.setCancelled(true);
            Bukkit.broadcastMessage("" + ChatColor.GOLD + ChatColor.BOLD + String.format(
                "[GOD] <%s> %s",
                player.getDisplayName(),
                content
            ));
        } else {
            Bukkit.broadcastMessage(String.format(
                "%s <%s> %s",
                (empire == null
                    ? ChatColor.GRAY + "Unaffiliated"
                    : "[" + empire.getChatColor() + empire.getName() + ChatColor.WHITE + "]") + ChatColor.WHITE,
                player.getDisplayName(),
                content
            ));
        }

        final String message = String.format(
            "[%s] %s » %s",
            player.isOp()
                ? "GOD"
                : empire == null
                    ? "Unaffiliated"
                    : empire.getName(),
            player.getDisplayName(),
            CensorUtils.censorCurses(
                event.getMessage()
                    .replace("`", "\\`")
                    .replace("*", "\\*")
                    .replace("_", "\\_")
                    .replace("@everyone", "everyone")
                    .replace("@here", "here")
            )
        );
        DiscordBot.sendMessageInBridgeChat(player.isOp() ? "**" + message + "**" : message);
    }

}
