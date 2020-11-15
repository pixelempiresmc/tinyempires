package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeclareWar implements CommandOption {

    final static Map<ObjectId, Integer> empireWarTaskIds = new HashMap<>();
    final static int WAR_START_DELAY_SECONDS = 60;

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

        if (!tePlayer.hasPermission(Permission.WAR)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.WAR));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String empireName = StringUtils.buildWordsFromArray(args, 0);
        if (empireName.equals(empire.getName())) {
            sender.sendMessage(ChatColor.RED + "You cannot declare a war against your own empire");
            return;
        }

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
        for (final TEPlayer member : defender.getMembers()) {
            // check if player is online
            System.out.println(member.getPlayerUUID());
            System.out.println(member.getName());
            final Player p = Bukkit.getPlayer(member.getPlayerUUID());
            System.out.println(p);
            if (p != null) {
                defenderPlayersOnline++;
                System.out.println("Incrementing players online");
            }
        }
        System.out.println(defenderPlayersOnline);

        final int defenderPlayerOnlineRequirement = defender.getMembers().size() > 1 ? 2 : 1;
        System.out.println(defenderPlayersOnline);

        if (defenderPlayersOnline < defenderPlayerOnlineRequirement) {
            sender.sendMessage(ChatColor.RED + String.format(
                "At least %d players from this empire must be online to declare war against them. (%d currently " +
                    "online)",
                defenderPlayerOnlineRequirement,
                defenderPlayersOnline
            ));
            return;
        }

        empire.setAtWarWith(defender, true);
        empire.broadcast(ChatColor.DARK_GREEN, String.format(
            "%s has made the empire declare war against %s! It will start in %d seconds",
            sender.getName(),
            "" + defender.getChatColor() + ChatColor.BOLD + empireName + ChatColor.DARK_GREEN,
            WAR_START_DELAY_SECONDS
        ));

        defender.setAtWarWith(empire, false);
        defender.broadcast(ChatColor.DARK_RED, String.format(
            "%s declared war against the %s! It will start in %d seconds",
            "" + empire.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.DARK_RED,
            "" + defender.getChatColor() + ChatColor.BOLD + defender.getName() + ChatColor.DARK_RED,
            Empire.WAR_TIME_MINUTES
        ));

        empire.setTimeLeftToWar(WAR_START_DELAY_SECONDS);
        empire.setIsWaitingForWar(true);
        defender.setTimeLeftToWar(WAR_START_DELAY_SECONDS);
        defender.setIsWaitingForWar(true);
        empireWarTaskIds.put(
            empire.getId(),
            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                TinyEmpires.getInstance(),
                () -> {
                    empire.decrementTimeLeftToWar();
                    defender.decrementTimeLeftToWar();
                    if (empire.getTimeLeftToWar() == 0) {
                        empire.setIsWaitingForWar(false);
                        defender.setIsWaitingForWar(false);
                        Bukkit.getScheduler().cancelTask(empireWarTaskIds.get(empire.getId()));
                        empire.setTimeLeftInWar(Empire.WAR_TIME_MINUTES * 60);
                        defender.setTimeLeftInWar(Empire.WAR_TIME_MINUTES * 60);
                        empireWarTaskIds.put(
                            empire.getId(),
                            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                                TinyEmpires.getInstance(),
                                () -> {
                                    empire.decrementTimeLeftInWar();
                                    defender.decrementTimeLeftInWar();
                                    if (empire.getTimeLeftInWar() == 0) {
                                        empire.broadcast(ChatColor.GREEN, String.format(
                                            "The war against %s has ended!",
                                            "" + defender.getChatColor() + ChatColor.BOLD + defender.getName() + ChatColor.GREEN
                                        ));
                                        defender.broadcast(ChatColor.GREEN, String.format(
                                            "The war against %s has ended!",
                                            "" + defender.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.GREEN
                                        ));
                                        endWar(empire, defender);
                                    }
                                    empire.updateMemberScoreboards();
                                    defender.updateMemberScoreboards();
                                },
                                0,
                                20
                            )
                        );
                    }
                    empire.updateMemberScoreboards();
                    defender.updateMemberScoreboards();
                },
                0,
                20
            )
        );
    }

    public static void endWar(Empire attacker, Empire defender) {
        // end wars
        attacker.endWar();
        defender.endWar();

        // cancel task
        Bukkit.getScheduler().cancelTask(empireWarTaskIds.get(attacker.getId()));
    }

    @Override
    public String getDescription() {
        return "Declare war on empire";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.WAR;
    }

    @Override
    public String getUsage() {
        return "/e war <empire>";
    }

}
