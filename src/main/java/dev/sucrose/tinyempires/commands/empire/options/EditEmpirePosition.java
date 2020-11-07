package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.CommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class EditEmpirePosition implements CommandOption {

    private static final String permString;

    static {
        permString = Arrays.stream(Permission.values())
            .map(p -> p.name().toLowerCase())
            .collect(Collectors.joining(", "));
    }

    @Override
    public void execute(Player sender, String[] args) {
        // /e perm|permission <name> <permission>
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
            sender.sendMessage(ChatColor.RED + String.format(
                "/e perm|permission <name> <%s>",
                permString
            ));
            return;
        }

        String position = args[0];
        if (empire.getPosition(position) == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a position in the empire (%s)",
                position,
                String.join(", ", empire.getPositionMap().keySet())
            ));
            return;
        }

        Permission permission;
        try {
            permission = Permission.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException err) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not an existing permission (%s)",
                args[1],
                permString
            ));
            return;
        }

        boolean currentState = empire.togglePositionPermission(position, permission);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s %s the permission %s %s",
            sender.getName(),
            currentState ? "gave" : "removed",
            currentState ? "to" : "from",
            permission.name().toLowerCase()
        ));

        // remove as autoclaiming if lost permission
        if (permission == Permission.CHUNKS) {
            for (final TEPlayer p : empire.getMembers()) {
                if (p.getPositionName() != null
                    && p.getPositionName().equals(position)
                    && AutoClaimEmpireChunk.isAutoclaiming(senderUUID)
                    && !p.getPosition().hasPermission(Permission.CHUNKS)) {
                    AutoClaimEmpireChunk.removeAutoclaimer(senderUUID);

                    final Player gameP = Bukkit.getPlayer(p.getPlayerUUID());
                    if (gameP != null)
                        gameP.sendMessage(ChatColor.DARK_RED + String.format(
                            "You no longer have the %schunks%s permission and have stopped autoclaiming",
                            ChatColor.BOLD,
                            "" + ChatColor.RESET + ChatColor.DARK_RED
                        ));
                }
            }
        }
    }

}
