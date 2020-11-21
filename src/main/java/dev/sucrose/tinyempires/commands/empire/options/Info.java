package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bson.types.ObjectId;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class Info implements CommandOption {

    private static String getPositionPermissionsString(Position position) {
        return position
            .getPermissions()
            .stream()
            .map(p -> p.name().toLowerCase())
            .collect(Collectors.joining(", "));
    }

    @Override
    public void execute(Player sender, String[] args) {
        // /e info
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        sender.sendMessage(ChatColor.BOLD + "=== Info ===");
        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            // if player is not in an empire
            sender.sendMessage(tePlayer.getName());
            sender.sendMessage("Unaffiliated with empire");
            sender.sendMessage("" + tePlayer.getBalance() + " coins");
            return;
        }

        sender.sendMessage("Empire: " + empire.getChatColor() + ChatColor.BOLD + empire.getName());
        sender.sendMessage(String.format("Reserve: %.1f coins", empire.getReserve()));
        sender.sendMessage(ChatColor.ITALIC + (empire.getDescription() == null ? "[No description]" :
            empire.getDescription()));
        sender.sendMessage("");
        sender.sendMessage("" + ChatColor.BOLD + "Members: ");
        for (final TEPlayer member : empire.getMembers())
            sender.sendMessage(String.format(
                " - %s%s: %s",
                member.isOwner()
                    ? "[OWNER] "
                    : "",
                member.getName(),
                member.getPositionName() == null
                    ? ChatColor.GRAY + "Unaffiliated"
                    : member.getPositionName()
                        + (member.getPosition().getPermissions().size() > 0
                            ? " (" + getPositionPermissionsString(member.getPosition()) + ")"
                            : "")
            ));

        sender.sendMessage("");
        sender.sendMessage("" + ChatColor.BOLD + "Positions");
        if (empire.getPositionMap().size() == 0) {
            sender.sendMessage(ChatColor.GRAY + " - No existing positions");
        } else {
            final List<String> sortedPositionKeys = new ArrayList<>(empire.getPositionMap().keySet());
            // negate Position#compare so list is sorted left to right and is displayed top to bottom
            sortedPositionKeys.sort((string1, string2) ->
                -Position.compare(
                    empire.getPosition(string1),
                    empire.getPosition(string2)
                ));

            for (final String name : sortedPositionKeys)
                sender.sendMessage(String.format(
                    " - %s %s",
                    name,
                    empire.getPosition(name).getPermissions().size() > 1
                        ? '(' + getPositionPermissionsString(empire.getPosition(name)) + ')'
                        : ""
                ));
        }

        sender.sendMessage("");
        sender.sendMessage("" + ChatColor.BOLD + "Alliances");
        if (empire.getAllies().size() > 0) {
            for (final ObjectId empireId : empire.getAllies()) {
                final Empire ally = Empire.getEmpire(empireId);
                if (ally == null)
                    throw new NullPointerException("Could not get empire ally with ID " + empireId);
                sender.sendMessage(String.format(
                    " - %s",
                    ally.getChatColor() + ally.getName()
                ));
            }
        } else {
            sender.sendMessage(ChatColor.GREEN + " - None");
        }

        sender.sendMessage("");
        sender.sendMessage("" + ChatColor.BOLD + "Laws");
        if (empire.getLaws().size() > 0) {
            for (final Map.Entry<String, Law> entry : empire.getLaws())
                sender.sendMessage(String.format(
                    " - %s (%s)",
                    entry.getKey(),
                    entry.getValue().getAuthor()
                ));
        } else {
            sender.sendMessage(ChatColor.GRAY + " - No existing laws");
        }

        final Set<Map.Entry<UUID, Double>> debtors = empire.getDebtEntries();
        if (debtors.size() > 0) {
            sender.sendMessage("");
            sender.sendMessage("" + ChatColor.BOLD + "Debt");
            for (final Map.Entry<UUID, Double> debtor : debtors) {
                final TEPlayer debtorTEPlayer = TEPlayer.getTEPlayer(debtor.getKey());
                if (debtorTEPlayer == null)
                    throw new NullPointerException(String.format(
                        "Could not find TEPlayer instance for %s", debtor.getKey()
                    ));
                sender.sendMessage(ChatColor.RED + String.format(
                    " - %s: %.1f",
                    debtorTEPlayer.getName(),
                    debtor.getValue()
                ));
            }
        }
    }

    @Override
    public String getDescription() {
        return "Get empire info";
    }

    @Override
    public Permission getPermissionRequired() {
        return null;
    }

    @Override
    public String getUsage() {
        return "/e info";
    }

}
