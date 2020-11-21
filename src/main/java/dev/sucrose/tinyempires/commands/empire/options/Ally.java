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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Ally implements CommandOption {

    private static final Map<ObjectId, Integer> empireAllyAcceptCancelIds = new HashMap<>();

    @Override
    public void execute(Player sender, String[] args) {
        // /e ally <empire>
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

        final String empireName = StringUtils.buildWordsFromArray(args, 0);
        if (empireName.equals(empire.getName())) {
            sender.sendMessage(ChatColor.RED + "You cannot ally with your own empire");
            return;
        }

        final Empire empireToAlly = Empire.getEmpire(empireName);
        if (empireToAlly == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing empire",
                empireName
            ));
            return;
        }

        if (empire.getAllies().contains(empireToAlly.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "You are already allies with %s",
                "" + empireToAlly.getChatColor() + ChatColor.BOLD + empireToAlly.getName()
            ));
            return;
        }

        if (!empireToAlly.isAnyMemberOnline()) {
            sender.sendMessage(ChatColor.RED + String.format(
                "No-one on %s is currently online and can accept your alliance request",
                "" + empire.getChatColor() + ChatColor.BOLD + empire.getName()
            ));
            return;
        }

        if (Empire.getAllyRequestForEmpire(empire.getId()) != null) {
            sender.sendMessage(ChatColor.RED + "You can only request to ally with an empire one at a time");
            return;
        }

        // checks passed, request to ally with empire
        empireToAlly.broadcast(ChatColor.GREEN, String.format(
            "The empire of %s has requested to make an alliance!",
            "" + empire.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.GREEN
        ));

        empireToAlly.broadcastText(ChatColor.GREEN + String.format(
            "You have 60 seconds to run %s to accept, or %s to reject (%sallies%s permission required)",
            ChatColor.BOLD + "/e allyaccept " + empire.getName() + ChatColor.GREEN,
            ChatColor.BOLD + "/e allyreject " + empire.getName() + ChatColor.GREEN,
            ChatColor.BOLD,
            ChatColor.GREEN
        ));

        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s has requested the empire of %s to ally! They have 60 seconds to accept",
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            "" + empireToAlly.getChatColor() + ChatColor.BOLD + empireToAlly.getName() + ChatColor.GREEN
        ));

        Empire.putAllyRequest(empireToAlly.getId(), empire.getId());
        empireAllyAcceptCancelIds.put(
            empire.getId(),
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                TinyEmpires.getInstance(),
                () -> {
                    Empire.removeAllyRequest(empire.getId());

                    empire.broadcast(ChatColor.RED, String.format(
                        "Request to ally with %s expired",
                        "" + ChatColor.BOLD + empireToAlly.getChatColor() + ChatColor.RED
                    ));

                    empireToAlly.broadcastText(ChatColor.RED + String.format(
                        "%s's request to ally has expired",
                        "" + empire.getChatColor() + ChatColor.BOLD + empire.getName() + ChatColor.RED
                    ));
                },
                60 * 20
            )
        );
    }

    public static void cancelEmpireAllyRequestExpire(ObjectId empireId) {
        Bukkit.getScheduler().cancelTask(empireAllyAcceptCancelIds.get(empireId));
        empireAllyAcceptCancelIds.remove(empireId);
    }

    @Override
    public String getDescription() {
        return "Ally with an empire";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.ALLIES;
    }

    @Override
    public String getUsage() {
        return "/e ally <empire>";
    }

}
