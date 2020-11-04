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
import org.apache.commons.lang.CharUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AcceptEmpireMember implements EmpireCommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e accept <name>
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

        if (!tePlayer.hasPermission(Permission.INVITES)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.INVITES));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e accept <name>");
            return;
        }

        String player = args[0];
        TEPlayer tePlayerToAccept = TEPlayer.getTEPlayer(player);
        if (tePlayerToAccept == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing player",
                player
            ));
            return;
        }

        if (!Empire.getPlayerToEmpireJoinRequest().containsKey(tePlayerToAccept.getPlayerUUID())
                || !Empire.getPlayerToEmpireJoinRequest().get(tePlayerToAccept.getPlayerUUID()).equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not currently requesting to join",
                player
            ));
            return;
        }

        empire.acceptPlayerJoinRequest(tePlayer);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s accepted %s to the empire. Welcome!",
            sender.getName(),
            player
        ));
    }

}
