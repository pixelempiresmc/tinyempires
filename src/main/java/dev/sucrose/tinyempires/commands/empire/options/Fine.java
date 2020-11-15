package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Fine implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e fine <player> <amount>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to levy a member fine");
            return;
        }

        if (!tePlayer.hasPermission(getPermissionRequired())) {
            sender.sendMessage(ErrorUtils.generatePermissionError(getPermissionRequired()));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (Exception ignore) {
            sender.sendMessage(ChatColor.RED + getUsage());
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
        empire.addDebt(debtor.getPlayerUUID(), amount);
        final Double debt = empire.getDebt(debtor.getPlayerUUID());
        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s has fined %s %.1f coins! (%.1f coins now indebted)",
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            amount,
            debt == null ? 0 : debt
        ));
    }

    @Override
    public String getDescription() {
        return "Give member debt";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.RESERVE;
    }

    @Override
    public String getUsage() {
        return "/e fine <player> <amount>";
    }

}
