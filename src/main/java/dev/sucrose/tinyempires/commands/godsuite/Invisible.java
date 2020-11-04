package dev.sucrose.tinyempires.commands.godsuite;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Invisible implements CommandExecutor {

    private static final Set<UUID> invisiblePlayers = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /invisible <true/false>
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        final Player player = (Player) sender;
        final UUID uuid = player.getUniqueId();
        if (args.length < 1) {
            sender.sendMessage(ChatColor.GOLD + String.format(
                "Player visibility is set to %s",
                ChatColor.BOLD + (invisiblePlayers.contains(uuid) ? "true" : "false")
            ));
            return false;
        }

        final String argument = args[0];
        switch (argument) {
            case "true":
                if (!invisiblePlayers.contains(uuid)) {
                    invisiblePlayers.add(uuid);
                    Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(TinyEmpires.getInstance(), player));
                    // give invisibility particles as a visual effect
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 255,
                        false, true));
                }
                break;
            case "false":
                if (invisiblePlayers.contains(uuid)) {
                    invisiblePlayers.remove(uuid);
                    Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(TinyEmpires.getInstance(), player));
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "/invisible <true/false>");
                return false;
        }

        sender.sendMessage(ChatColor.GREEN + "Player visibility now set to " + ChatColor.BOLD + argument);
        return true;
    }

    public static Set<UUID> getInvisiblePlayers() {
        return invisiblePlayers;
    }

}
