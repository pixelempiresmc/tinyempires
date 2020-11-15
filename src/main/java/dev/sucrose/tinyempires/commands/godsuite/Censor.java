package dev.sucrose.tinyempires.commands.godsuite;

import dev.sucrose.tinyempires.listeners.PlayerChat;
import dev.sucrose.tinyempires.utils.CensorUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class Censor implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /censor <option> <word>
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/censor <option> [word]");
            return false;
        }

        final String option = args[0];
        String censor;
        switch (option) {
            case "list":
                sender.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Censored Curses");
                for (final String curse : CensorUtils.getCensors())
                    sender.sendMessage(ChatColor.GREEN + " - " + curse);
                return true;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/censor add <word>");
                    return false;
                }

                censor = args[1];
                CensorUtils.addCensor(censor);
                sender.sendMessage(ChatColor.GREEN + String.format(
                    "Added censor %s",
                    ChatColor.BOLD + censor
                ));
                return true;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/censor remove <word>");
                    return false;
                }

                censor = args[1];
                CensorUtils.removeCensor(censor);
                sender.sendMessage(ChatColor.GREEN + String.format(
                    "Removed censor %s",
                    ChatColor.BOLD + censor
                ));
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "/censor <option> [word]");
                return true;
        }

    }
}
