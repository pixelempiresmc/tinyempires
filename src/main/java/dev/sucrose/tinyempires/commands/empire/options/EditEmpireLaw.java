package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EditEmpireLaw implements EmpireCommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e editlaw <name>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to edit a law");
            return;
        }

        if (!tePlayer.hasPermission(Permission.LAWS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.LAWS));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e editlaw <name>");
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

        // open law
        final ItemStack item = sender.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()
                || !(item.getItemMeta() instanceof BookMeta)) {
            sender.sendMessage(ChatColor.RED + "You must be holding a writable book to edit the law");
            return;
        }

        final BookMeta meta = (BookMeta) item.getItemMeta();
        empire.editLaw(lawName, meta.getPages());
        empire.broadcast(ChatColor.YELLOW, String.format(
            "%s edited the law %s",
            ChatColor.BOLD + sender.getName() + ChatColor.YELLOW,
            ChatColor.BOLD + lawName + ChatColor.YELLOW
        ));
    }

}
