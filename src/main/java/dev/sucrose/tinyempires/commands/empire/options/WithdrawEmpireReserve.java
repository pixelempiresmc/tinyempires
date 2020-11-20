package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WithdrawEmpireReserve implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e withdraw <amount>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_MUST_BE_IN_AN_EMPIRE);
            return;
        }

        if (!tePlayer.hasPermission(getPermissionRequired())) {
            sender.sendMessage(ErrorUtils.generatePermissionError(getPermissionRequired()));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (Exception err) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        if (empire.getReserve() <= 0) {
            sender.sendMessage(ChatColor.RED + "You can only withdraw a positive amount from your empire reserve");
            return;
        }

        if (empire.getReserve() < amount) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire has insufficent coins for withdraw (%.1f requested, %.1f in reserve)",
                amount,
                empire.getReserve()
            ));
            return;
        }

        tePlayer.giveCoins(amount);
        empire.takeReserveCoins(amount);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s has taken %.1f from the Empire reserve",
            sender.getName(),
            amount
        ));
    }

    @Override
    public String getDescription() {
        return "Withdraw empire funds from reserve";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.RESERVE;
    }

    @Override
    public String getUsage() {
        return "/e <withdraw> <amount>";
    }

}
