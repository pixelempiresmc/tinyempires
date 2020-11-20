package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.UUID;

public class Tax implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e tax <amount>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire tax members");
            return;
        }

        if (!tePlayer.hasPermission(Permission.RESERVE)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.RESERVE));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (Exception ignore) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "You can only tax your members a positive amount");
            return;
        }

        empire.tax(amount, senderUUID);
        empire.broadcastText(String.format(
            "%s has taxed all members %.1f coins! (%s/e contribute %.1f%s to pay)",
            ChatColor.BOLD + sender.getName() + ChatColor.WHITE,
            amount,
            ChatColor.YELLOW,
            amount,
            ChatColor.WHITE
        ));
    }

    @Override
    public String getDescription() {
        return "Bestow debt upon every other empire member";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.RESERVE;
    }

    @Override
    public String getUsage() {
        return "/e tax <amount>";
    }

}
