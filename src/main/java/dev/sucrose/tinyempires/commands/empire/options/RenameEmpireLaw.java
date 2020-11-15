package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RenameEmpireLaw implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e renamelaw <law>/<new_name>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to rename a law");
            return;
        }

        if (!tePlayer.hasPermission(Permission.LAWS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.LAWS));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        // use '/' to deliminate arguments as both have spaces
        final String argumentString = StringUtils.buildWordsFromArray(args, 0);
        if (!argumentString.contains("/")) {
            sender.sendMessage(ChatColor.RED + "Law name arguments must be delimited with a slash (e.g. /law My Law's" +
                " Name/My New Law Name");
            return;
        }

        final String[] argumentStringArray = argumentString.split("/");
        final String originalLawName = argumentStringArray[0];
        final String newLawName = argumentStringArray[1];

        final Law law = empire.getLaw(originalLawName);
        if (law == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a law in your empire (%s)",
                originalLawName,
                empire.getLaws().size() > 0
                    ? empire.getLaws()
                        .stream()
                        .map(Map.Entry::getKey)
                        .collect(Collectors.joining(", "))
                    : ChatColor.GRAY + "No laws" + ChatColor.WHITE
            ));
            return;
        }

        final Law lawWithNewName = empire.getLaw(newLawName);
        if (lawWithNewName != null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' by %s is already a law in your empire!",
                newLawName,
                ChatColor.BOLD + lawWithNewName.getAuthor() + ChatColor.RED
            ));
            return;
        }

        // checks passed, rename law
        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s renamed the law of %s by %s to %s",
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            ChatColor.BOLD + originalLawName + ChatColor.GREEN,
            ChatColor.BOLD + law.getAuthor() + ChatColor.GREEN,
            ChatColor.BOLD + newLawName
        ));
        empire.renameLaw(originalLawName, newLawName);
    }

    @Override
    public String getDescription() {
        return "Rename law";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.LAWS;
    }

    @Override
    public String getUsage() {
        return "/e renamelaw <law>/<new_name>";
    }

}
