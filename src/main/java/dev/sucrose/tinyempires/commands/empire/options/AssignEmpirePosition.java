package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AssignEmpirePosition implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e assign <position> <player>
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

        if (!tePlayer.hasPermission(Permission.POSITIONS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.POSITIONS));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/e assign <position> <player>");
            return;
        }

        final String positionName = StringUtils.buildWordsFromArray(args, 0, args.length - 1);
        final Position position = empire.getPosition(positionName);
        if (position == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a position in the empire (%s)",
                positionName,
                String.join(", ", empire.getPositionMap().keySet())
            ));
            return;
        }

        final String assigneeName = args[args.length - 1];
        final TEPlayer assignee = TEPlayer.getTEPlayer(assigneeName);
        if (assignee == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing player",
                assigneeName
            ));
            return;
        }

        final Empire assigneeEmpire = assignee.getEmpire();
        if (assigneeEmpire == null
                || !assigneeEmpire.getId().equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%s is not in the same empire (%s)",
                assignee,
                assigneeEmpire == null
                    ? ChatColor.GRAY + "Unaffiliated"
                    : assigneeEmpire.getChatColor() + assigneeEmpire.getName() + ChatColor.RED
            ));
            return;
        }

        final Position tePlayerPosition = tePlayer.getPosition();
        if (!tePlayer.isOwner()
                && (tePlayerPosition != null
                    && Position.compare(tePlayerPosition, position) < 0)) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Your position (%s) must have admin or have the same or greater number of permissions as %s to " +
                    "assign it to another member",
                ChatColor.BOLD + tePlayer.getPositionName() + ChatColor.RED,
                ChatColor.BOLD + positionName + ChatColor.RED
            ));
            return;
        }

        assignee.setPositionName(positionName);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s set %s's position to %s",
            sender.getName(),
            assigneeName,
            positionName
        ));
    }

}
