package dev.sucrose.tinyempires.commands.economy;

import dev.sucrose.tinyempires.models.TEPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Take implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /take <receiver> <amount>
        final Player player = (Player) sender;
        // operator command
        if (!player.isOp()) {
            player.sendMessage("You do not have sufficient permissions to run this command!");
            return false;
        }

        // validate arguments length
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/take <receiver> <amount>");
            return false;
        }

        // parse arguments
        String receiverName;
        double amount;
        try {
            receiverName = args[0];
            amount = Double.parseDouble(args[1]);
        } catch (Exception err) {
            player.sendMessage(ChatColor.RED + "/take <receiver> <amount>");
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

        receiver.takeCoins(amount);
        player.sendMessage(ChatColor.GREEN + String.format(
            "Taken %.1f coins from %s",
            amount,
            receiverName
        ));

        final Player receiverPlayer = Bukkit.getPlayer(receiverName);
        if (receiverPlayer != null)
            receiverPlayer.sendMessage(ChatColor.DARK_RED + String.format(
                "The gods have taken away %.1f coins from you!",
                amount
            ));
        return true;
    }

}
