package dev.sucrose.tinyempires.commands.economy;

import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Gift implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /gift <receiver> <amount>
        final Player player = (Player) sender;
        // operator command
        if (!player.isOp()) {
            player.sendMessage(ErrorUtils.INSUFFICIENT_PERMS);
            return false;
        }

        // validate arguments length
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/gift <receiver> <amount>");
            return false;
        }

        // parse arguments
        String receiverName;
        double amount;
        try {
            receiverName = args[0];
            amount = Double.parseDouble(args[1]);
        } catch (Exception err) {
            player.sendMessage(ChatColor.RED + "/gift <receiver> <amount>");
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

        receiver.giveCoins(amount);
        player.sendMessage(ChatColor.GREEN + String.format(
            "Gifted %.1f coins to %s",
            amount,
            receiverName
        ));

        final Player receiverPlayer = Bukkit.getPlayer(receiverName);
        if (receiverPlayer != null)
            receiverPlayer.sendMessage(ChatColor.GREEN + String.format(
                "You have been gifted %.1f coins from the gods!",
                amount
            ));
        return true;
    }

}
