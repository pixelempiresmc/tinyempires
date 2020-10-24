package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.EmpireCommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class JoinEmpire implements EmpireCommandOption {

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
            sender.sendMessage(ChatColor.RED + "/e join <empire>");
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

        String acceptCommandText = "/e accept " + tePlayer.getName();
        TextComponent acceptCommand = new TextComponent(acceptCommandText);
        acceptCommand.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptCommand.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, acceptCommandText));

        String rejectCommandText = "/e reject " + tePlayer.getName();
        TextComponent rejectCommand = new TextComponent(rejectCommandText);
        acceptCommand.setColor(net.md_5.bungee.api.ChatColor.DARK_RED);
        acceptCommand.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, rejectCommandText));

        empireToJoin.broadcast(ChatColor.GREEN, String.format(
            "%s has requested to join the empire!",
            tePlayer.getName()
        ));
        empireToJoin.broadcastText(ChatColor.GREEN + String.format(
            "You have 60 seconds to run %s to accept, or %s to reject (%sinvite%s permission required)",
            acceptCommand,
            rejectCommand,
            ChatColor.YELLOW,
            ChatColor.GREEN
        ));

        empireToJoin.addPlayerJoinRequest(tePlayer);
        Bukkit.getScheduler().scheduleSyncDelayedTask(TinyEmpires.getInstance(), new Runnable() {
            @Override
            public void run() {
                Empire.getPlayerToEmpireJoinRequest().remove(tePlayer.getPlayerUUID());
                empireToJoin.broadcast(ChatColor.GOLD, String.format(
                    "Join request to accept %s expired",
                    tePlayer.getName()
                ));
            }
        }, 60 * 20);
    }

}
