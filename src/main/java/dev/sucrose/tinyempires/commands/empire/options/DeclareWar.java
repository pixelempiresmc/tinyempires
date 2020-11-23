package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.discord.DiscordBot;
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

import java.util.*;

public class DeclareWar implements CommandOption {

    final static Map<ObjectId, Integer> empireWarTaskIds = new HashMap<>();
    final static int WAR_START_DELAY_SECONDS = 300;
    final static float WAR_START_COST = 50;

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

//        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 2
//                && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) <= 7) {
//            sender.sendMessage(ChatColor.RED + "You cannot declare war between the hours of 2AM and 7AM PST");
//            return;
//        }

        final Empire defender = Empire.getEmpire(empireName);
        if (defender == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire '%s' does not exist",
                empireName
            ));
            return;
        }

        if (empire.getAllies().contains(defender.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "You cannot go to war against your own allies. (%s/e unally%s to unally)",
                ChatColor.BOLD,
                ChatColor.RED
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

        if (empire.getReserve() < WAR_START_COST) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Your empire needs %.1f more coins to start a war (%.1f coins required)",
                WAR_START_COST - empire.getReserve(),
                WAR_START_COST
            ));
            return;
        }

        int defenderPlayersOnline = 0;
        for (final TEPlayer member : defender.getMembers()) {
            // check if player is online
            final Player p = Bukkit.getPlayer(member.getPlayerUUID());
            if (p != null)
                defenderPlayersOnline++;
        }
        
        final int defenderPlayerOnlineRequirement = (int) Math.ceil((float) defender.getMembers().size() / 2);
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
            "%s spent %.1f coins and has made the empire declare war against %s! It will start in %d seconds",
            sender.getName(),
            WAR_START_COST,
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

        DiscordBot.sendMessageInBridgeChat(String.format(
            "**The empire of %s has declared war against %s!**",
            empire.getName(),
            defender.getName()
        ));

        Bukkit.broadcastMessage(ChatColor.GREEN + String.format(
            "The empire of %s has declared war against %s!",
            "" + empire.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.GREEN,
            "" + defender.getChatColor() + ChatColor.BOLD + defender.getName() + ChatColor.GREEN
        ));

        // take coins
        empire.takeReserveCoins(WAR_START_COST);
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
                                        Bukkit.broadcastMessage(ChatColor.GREEN + String.format(
                                            "The war between %s and %s has ended!",
                                            "" + empire.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.GREEN,
                                            "" + defender.getChatColor() + ChatColor.BOLD + defender.getName() + ChatColor.GREEN
                                        ));
                                        DiscordBot.sendMessageInBridgeChat(String.format(
                                            "**The war between %s and %s has ended!**",
                                            empire.getName(),
                                            defender.getName()
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
