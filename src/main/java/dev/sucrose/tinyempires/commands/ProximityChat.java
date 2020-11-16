package dev.sucrose.tinyempires.commands;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProximityChat implements CommandExecutor {

    private static final int PROX_CHAT_RANGE_BLOCKS = 20;

    private static double pythag(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(
            Math.pow(x2 - x1, 2)
            + Math.pow(y2 - y1, 2)
            + Math.pow(z2 - z1, 2)
        );
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // /prox message
        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();
        final String message = StringUtils.buildWordsFromArray(args, 0);
        final Location location = player.getLocation();
        final List<String> playerNamesTold = new ArrayList<>();
        for (final Player p : Bukkit.getOnlinePlayers()) {
            final Location pLocation = p.getLocation();
            if (pythag(
                    location.getX(),
                    pLocation.getX(),
                    location.getY(),
                    pLocation.getY(),
                    location.getZ(),
                    pLocation.getZ()
            ) < PROX_CHAT_RANGE_BLOCKS) {
                final TEPlayer tePlayer = TEPlayer.getTEPlayer(p.getUniqueId());
                if (tePlayer == null)
                    throw new NullPointerException("Could not get " + p.getUniqueId() + " from the database");
                final Empire empire = tePlayer.getEmpire();
                p.sendMessage(ChatColor.ITALIC + String.format(
                    "From proximate chat: %s <%s> %s",
                    (empire == null
                        ? ChatColor.GRAY + "Unaffiliated"
                        : "[" + empire.getChatColor() + empire.getName() + ChatColor.WHITE + "]") + ChatColor.WHITE,
                    tePlayer.getName(),
                    message
                ));
                playerNamesTold.add(p.getName());
            }
        }

        player.sendMessage(
            playerNamesTold.size() > 0
                ? ChatColor.GREEN + String.format(
                    "Sent message to %s",
                    StringUtils.stringListToGrammaticalList(playerNamesTold, ChatColor.GREEN)
                )
                : ChatColor.GRAY + String.format(
                    "No players are within %d blocks!",
                    PROX_CHAT_RANGE_BLOCKS
                )
        );
        return true;
    }

}
