package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JoinEmpire implements CommandOption {

    private final static Map<UUID, Integer> expireJoinRequestTaskIds = new HashMap<>();

    public static void cancelExpireJoinRequest(UUID playerToBeAccepted) {
        Bukkit.getScheduler().cancelTask(expireJoinRequestTaskIds.get(playerToBeAccepted));
        expireJoinRequestTaskIds.remove(playerToBeAccepted);
    }

    @Override
    public void execute(Player sender, String[] args) {
        // /e join <empire>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire != null) {
            sender.sendMessage(ChatColor.RED + "You must leave your empire first before trying to join one");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        String empireToJoinName = StringUtils.buildWordsFromArray(args, 0);
        Empire empireToJoin = Empire.getEmpire(empireToJoinName);
        if (empireToJoin == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire with name '%s' does not exist",
                empireToJoinName
            ));
            return;
        }

        if (empireToJoin.getMembers().size() == 0) {
            sender.sendMessage(ChatColor.RED + String.format(
                "No-one in the empire of %s is online and can accept invitation you",
                "" + empireToJoin.getChatColor() + ChatColor.BOLD + empireToJoinName + ChatColor.RED
            ));
            return;
        }

//        String acceptCommandText = "/e accept " + tePlayer.getName();
//        TextComponent acceptCommand = new TextComponent(acceptCommandText);
//        acceptCommand.setColor(net.md_5.bungee.api.ChatColor.GREEN);
//        acceptCommand.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, acceptCommandText));
//
//        String rejectCommandText = "/e reject " + tePlayer.getName();
//        TextComponent rejectCommand = new TextComponent(rejectCommandText);
//        acceptCommand.setColor(net.md_5.bungee.api.ChatColor.DARK_RED);
//        acceptCommand.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, rejectCommandText));

        empireToJoin.broadcast(ChatColor.GREEN, String.format(
            "%s has requested to join the empire!",
            tePlayer.getName()
        ));
        empireToJoin.broadcastText(ChatColor.GREEN + String.format(
            "You have 60 seconds to run %s to accept, or %s to reject (%sinvite%s permission required)",
            ChatColor.BOLD + "/e accept " + tePlayer.getName() + ChatColor.GREEN,
            ChatColor.BOLD + "/e reject " + tePlayer.getName() + ChatColor.GREEN,
            ChatColor.BOLD,
            ChatColor.GREEN
        ));
        sender.sendMessage(ChatColor.GREEN + String.format(
            "Sent an invite to join the empire of %s! They have 60 seconds to accept",
            "" + empireToJoin.getChatColor() + ChatColor.BOLD + empireToJoinName + ChatColor.GREEN
        ));

        empireToJoin.addPlayerJoinRequest(tePlayer);
        expireJoinRequestTaskIds.put(
            tePlayer.getPlayerUUID(),
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                TinyEmpires.getInstance(),
                () -> {
                    Empire.getPlayerToEmpireJoinRequest().remove(tePlayer.getPlayerUUID());
                    empireToJoin.broadcast(ChatColor.GOLD, String.format(
                        "Join request to accept %s expired",
                        ChatColor.BOLD + tePlayer.getName() + ChatColor.GOLD
                    ));
                },
                60 * 20
            )
        );
    }

    @Override
    public String getDescription() {
        return "Request to join empire";
    }

    @Override
    public Permission getPermissionRequired() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/e join <empire>";
    }

}
