package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.EmpireCommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Forgive implements EmpireCommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e forgive <player> <amount>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to relieve member debt");
            return;
        }

        if (!tePlayer.hasPermission(Permission.RESERVE)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.RESERVE));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/e forgive <player> <amount>");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (Exception ignore) {
            sender.sendMessage(ChatColor.RED + "/e forgive <player> <amount>");
            return;
        }

        final String debtorName = args[0];
        final TEPlayer debtor = TEPlayer.getTEPlayer(debtorName);
        if (debtor == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing player",
                debtorName
            ));
            return;
        }

        if (!debtor.isInEmpire()
                || !debtor.getEmpire().getId().equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not in the same empire",
                ChatColor.BOLD + debtorName + ChatColor.WHITE
            ));
            return;
        }

        // forgive debt
        empire.removeDebt(debtor.getPlayerUUID(), amount);
        final double debt = empire.getDebt(debtor.getPlayerUUID());
        sender.sendMessage(ChatColor.GREEN + String.format(
            "%s has been forgiven %.1f coins of debt by %s! (%.1f coins still indebted)",
            ChatColor.BOLD + debtorName + ChatColor.GREEN,
            amount,
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            debt
        ));
    }

}
