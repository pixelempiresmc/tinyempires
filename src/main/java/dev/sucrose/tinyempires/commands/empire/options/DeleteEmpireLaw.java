package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeleteEmpireLaw implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e repeal <name>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to delete a law");
            return;
        }

        if (!tePlayer.hasPermission(Permission.LAWS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.LAWS));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String lawName = StringUtils.buildWordsFromArray(args, 0);
        final Law law = empire.getLaw(lawName);
        if (law == null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a law in your empire (%s)",
                lawName,
                empire.getLaws().size() > 0
                    ? empire.getLaws()
                        .stream()
                        .map(Map.Entry::getKey)
                        .collect(Collectors.joining(", "))
                    : ChatColor.GRAY + "No laws" + ChatColor.WHITE
            ));
            return;
        }

        empire.removeLaw(lawName);
        empire.broadcastText(String.format(
            "%s repealed the law %s (authored by %s)",
            ChatColor.BOLD + sender.getName() + ChatColor.WHITE,
            ChatColor.BOLD + lawName + ChatColor.WHITE,
            law.getAuthor()
        ));
    }

    @Override
    public String getDescription() {
        return "Delete empire law";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.LAWS;
    }

    @Override
    public String getUsage() {
        return "/e repeal <law>";
    }

}
