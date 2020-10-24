package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.EmpireCommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DeclareWar implements EmpireCommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e war <empire>
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

        if (tePlayer.getPosition().hasPermission(Permission.WAR)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.WAR));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e war <empire>");
            return;
        }

        final String empireName = args[0];
        final Empire defender = Empire.getEmpire(empireName);
        if (defender == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire '%s' does not exist",
                empireName
            ));
            return;
        }

        if (empire.getAtWarWith() != null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "You are currently in war with %s and cannot start a new one",
                empire.getAtWarWith().getName()
            ));
            return;
        }

        int defenderPlayersOnline = 0;
        for (TEPlayer member : defender.getMembers()) {
            // check if player is online
            if (Bukkit.getPlayer(member.getPlayerUUID()) != null)
                defenderPlayersOnline++;
        }

        final int defenderPlayerOnlineRequirement = defender.getMembers().size() > 1 ? 2 : 1;
        if (defenderPlayersOnline < defenderPlayerOnlineRequirement) {
            sender.sendMessage(ChatColor.RED + String.format(
                "At least %d players from this empire must be online to declare war against them. (%d currently " +
                    "online)",
                defenderPlayersOnline,
                defenderPlayerOnlineRequirement
            ));
        }

        empire.setAtWarWith(defender, true);
        empire.broadcast(ChatColor.DARK_GREEN, String.format(
            "%s has made the empire declare war against %s! You have %d minutes to conquer as much as you can!",
            sender.getName(),
            empireName,
            Empire.WAR_TIME_MINUTES
        ));

        defender.setAtWarWith(empire, false);
        defender.broadcast(ChatColor.DARK_RED, String.format(
            "%s declared war against the Empire! You have %d minutes to defend them off, and conquer some chunks if " +
                "you can!",
            empire.getName(),
            Empire.WAR_TIME_MINUTES
        ));

        Bukkit.getScheduler().scheduleSyncDelayedTask(TinyEmpires.getInstance(), new Runnable() {
            @Override
            public void run() {
                empire.endWar();
                defender.endWar();
                empire.broadcast(ChatColor.GREEN, String.format(
                    "The war against %s has ended!",
                    empireName
                ));
                defender.broadcast(ChatColor.GREEN, String.format(
                    "The war against %s has ended!",
                    empire.getName()
                ));
            }
        }, Empire.WAR_TIME_MINUTES * 60 * 20);
    }

}
