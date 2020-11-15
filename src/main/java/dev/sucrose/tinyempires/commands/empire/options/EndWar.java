package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class EndWar implements CommandOption {

    final Map<ObjectId, ObjectId> empireAttackerToWarEndOfferer = new HashMap<>();

    @Override
    public void execute(Player sender, String[] args) {
        // /e endwar
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

        if (empire.getAtWarWith() == null) {
            sender.sendMessage(ChatColor.RED + "You must be in a war to offer to end it");
            return;
        }

        final Empire attacker =
            empire.isAttackerInWar()
                ? empire
                : empire.getAtWarWith();
        final Empire defender =
            empire.isAttackerInWar()
                ? empire.getAtWarWith()
                : empire;
        if (empireAttackerToWarEndOfferer.containsKey(attacker.getId())) {
            if (empireAttackerToWarEndOfferer.get(attacker.getId()).equals(empire.getId())) {
                empireAttackerToWarEndOfferer.remove(attacker.getId());
                empire.broadcastText(ChatColor.GREEN + String.format(
                    "%s has retracted the empire's offer to end the war",
                    ChatColor.BOLD + tePlayer.getName() + ChatColor.GREEN
                ));
                return;
            }
            DeclareWar.endWar(
                attacker,
                defender
            );
            empire.getAtWarWith().broadcastText(ChatColor.GREEN + String.format(
                "%s has agreed to end the war, it is now over!",
                "" + ChatColor.BOLD + empire.getChatColor() + empire.getName() + ChatColor.GREEN
            ));
            empire.broadcastText(ChatColor.GREEN + "Both empires have agreed to end the war, it is now over!");
            attacker.updateMemberScoreboards();
            defender.updateMemberScoreboards();
            empireAttackerToWarEndOfferer.remove(attacker.getId());
            return;
        }

        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s made the empire offer to end the war, it will end if the enemy decides to as well",
            ChatColor.BOLD + tePlayer.getName() + ChatColor.GREEN
        ));
        empire.getAtWarWith().broadcastText("" + ChatColor.GREEN + ChatColor.BOLD + String.format(
            "%s has offered to end the war! Run /e endwar to accept their offer",
            empire.getChatColor() + empire.getName() + ChatColor.GREEN
        ));
        empireAttackerToWarEndOfferer.put(attacker.getId(), empire.getId());
    }

    @Override
    public String getDescription() {
        return "Offer to end war";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.WAR;
    }

    @Override
    public String getUsage() {
        return "/e endwar";
    }

}
