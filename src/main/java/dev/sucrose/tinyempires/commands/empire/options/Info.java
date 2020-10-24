package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.EmpireCommandOption;
import dev.sucrose.tinyempires.models.Permission;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Info implements EmpireCommandOption {

    private static final Map<Permission, String> permDescriptions = new EnumMap<>(Permission.class);

    static {
        permDescriptions.put(Permission.ADMIN, "All permissions");
        permDescriptions.put(Permission.CHUNKS, "Can claim and sell chunks");
        permDescriptions.put(Permission.EDIT, "Can change color and description");
        permDescriptions.put(Permission.INVITES, "Can accept players");
        permDescriptions.put(Permission.LAWS, "Can modify and publish laws");
        permDescriptions.put(Permission.POSITIONS, "Can edit and create positions");
        permDescriptions.put(Permission.RESERVE, "Can withdraw reserve funds");
        permDescriptions.put(Permission.WAR, "Can declare and end war");
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

        sender.sendMessage(ChatColor.GOLD + "=== Info ===");
        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            // if player is not in an empire
            sender.sendMessage(ChatColor.GOLD + tePlayer.getName());
            sender.sendMessage(ChatColor.GOLD + "Unaffiliated with empire");
            sender.sendMessage(ChatColor.GOLD + "" + tePlayer.getBalance() + " coins");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Empire: " + empire.getChatColor() + empire.getName());
        sender.sendMessage(ChatColor.GOLD + " " + (empire.getDescription() == null ? "[No description]" :
            empire.getDescription()));
        sender.sendMessage(ChatColor.GOLD + "- Reserve: " + empire.getReserve() + " coins");
        sender.sendMessage(ChatColor.GOLD + "Members: ");
        for (TEPlayer member : empire.getMembers())
            sender.sendMessage(ChatColor.GOLD + String.format(
                "%s - %s (%s)",
                member.getName(),
                member.getPositionName(),
                member.getPosition()
                    .getPermissions()
                    .stream()
                    .map(p -> p.name().toLowerCase())
                    .collect(Collectors.joining(", "))
            ));
        for (String name : empire.getPositionMap().keySet()) {
            sender.sendMessage(ChatColor.GOLD + name);
            for (Permission permission : empire.getPositionMap().get(name).getPermissions())
                sender.sendMessage(ChatColor.GOLD + String.format(
                    " - %s : %s",
                    permission.name(),
                    permDescriptions.get(permission)
                ));
        }

    }

}
