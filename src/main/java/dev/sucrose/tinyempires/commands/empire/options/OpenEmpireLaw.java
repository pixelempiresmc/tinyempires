package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OpenEmpireLaw implements EmpireCommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e law <name>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "You must be in an empire to view a law");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/e law <name>");
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

        // if player is holding book write law to item, otherwise open view-only book GUI
        final ItemStack item = sender.getInventory().getItemInMainHand();
        if (item.hasItemMeta()
                && item.getItemMeta() instanceof BookMeta) {
            final BookMeta meta = (BookMeta) item.getItemMeta();
            meta.setGeneration(BookMeta.Generation.COPY_OF_COPY);
            meta.setAuthor(law.getAuthor());
            meta.setPages(law.getContent());
            item.setItemMeta(meta);
            sender.sendMessage(ChatColor.GREEN + String.format(
                "Wrote law %s to book in main hand",
                ChatColor.BOLD + lawName + ChatColor.GREEN
            ));
            return;
        }

        final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (!(book.getItemMeta() instanceof BookMeta)) {
            sender.sendMessage("ERROR: Could not get BookMeta from book, please notify a developer of this and we " +
                "will tend to it promptly.");
            return;
        }

        final BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setPages(law.getContent());
        meta.setTitle(lawName);
        meta.setAuthor(law.getAuthor());
        meta.setGeneration(BookMeta.Generation.COPY_OF_ORIGINAL);
        book.setItemMeta(meta);
        sender.openBook(book);
    }

}
