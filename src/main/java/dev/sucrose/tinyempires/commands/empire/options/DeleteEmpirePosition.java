package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DeleteEmpirePosition implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e delpos <name>
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

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e delpos <name>");
            return;
        }

        final String position = StringUtils.buildWordsFromArray(args, 0);
        if (empire.getPosition(position) == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a position in the empire (%s)",
                position,
                String.join(", ", empire.getPositionMap().keySet())
            ));
            return;
        }

        final StringBuilder membersWithPosition = new StringBuilder();
        int index = 0;
        for (TEPlayer player : empire.getMembers()) {
            if (player.getPositionName() != null
                    && player.getPositionName().equals(position))
                membersWithPosition.append(player.getName()).append(index < empire.getMembers().size() - 1 ? ", " :
                    "");
            index++;
        }

        empire.removePosition(position);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s deleted the %s position; everyone with it (%s) is now unassigned",
            sender.getName(),
            position,
            membersWithPosition.toString().length() == 0
                ? "No-one"
                : membersWithPosition.toString()
        ));
    }

}
