package dev.sucrose.tinyempires.commands.economy;

import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.apache.commons.lang.CharUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Pay implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /pay <receiver> <amount>
        final Player player = (Player) sender;
        // validate arguments length
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/pay <receiver> <amount>");
            return false;
        }

        // parse arguments
        String receiverName;
        double amount;
        try {
            receiverName = args[0];
            amount = Double.parseDouble(args[1]);
        } catch (Exception err) {
            player.sendMessage(ChatColor.RED + "/pay <receiver> <amount>");
            return false;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than 0");
            return false;
        }

        // check player exists
        final TEPlayer receiver = TEPlayer.getTEPlayer(receiverName);
        if (receiver == null) {
            player.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing player",
                receiverName
            ));
            return false;
        }

        final TEPlayer senderTEPlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        if (senderTEPlayer == null) {
            player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return false;
        }

        senderTEPlayer.pay(receiver, amount);
        player.sendMessage(ChatColor.GREEN + String.format(
            "Paid %.1f coins to %s",
            amount,
            receiverName
        ));
        return true;
    }

}
