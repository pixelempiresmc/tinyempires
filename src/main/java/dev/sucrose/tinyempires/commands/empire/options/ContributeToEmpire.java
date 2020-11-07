package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ContributeToEmpire implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e contribute <amount>
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

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e contribute <amount>");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (Exception ignore) {
            sender.sendMessage(ChatColor.RED + "/e contribute <amount>");
            return;
        }

        if (tePlayer.getBalance() < amount) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%.1f' more funds required to make this contribution (%.1f in balance)",
                amount - tePlayer.getBalance(),
                tePlayer.getBalance()
            ));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be above zero");
            return;
        }

        // checks passed, distribute coins
        tePlayer.takeCoins(amount);
        empire.giveReserveCoins(amount);
        if (empire.getDebt(tePlayer.getPlayerUUID()) != null)
            empire.removeDebt(tePlayer.getPlayerUUID(), amount);

        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s contributed %.1f coins to the empire's reserve%s",
            sender.getName(),
            amount,
            empire.getDebt(tePlayer.getPlayerUUID()) != null
                ? String.format(" (%.1f coins still indebted)", empire.getDebt(tePlayer.getPlayerUUID()))
                : ""
        ));
    }

}
