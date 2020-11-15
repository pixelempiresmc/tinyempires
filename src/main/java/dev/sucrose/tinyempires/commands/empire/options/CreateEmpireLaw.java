package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import java.util.UUID;

public class CreateEmpireLaw implements CommandOption, Listener {

    @Override
    public void execute(Player sender, String[] args) {
        // /e newlaw <name>
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
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String law = StringUtils.buildWordsFromArray(args, 0);
        if (empire.getLaw(law) != null) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Law %s is already an existing law",
                ChatColor.BOLD + law + ChatColor.RED
            ));
            return;
        }

        final ItemStack item = sender.getInventory().getItemInMainHand();
        if (!(item.getItemMeta() instanceof BookMeta)) {
            sender.sendMessage(ChatColor.RED + "You must be holding a book to submit a law");
            return;
        }

        empire.addLaw(
            law,
            sender.getName(),
            ((BookMeta) item.getItemMeta()).getPages()
        );
        empire.broadcastText(String.format(
            "%s created the law %s!",
            ChatColor.BOLD + sender.getName() + ChatColor.WHITE,
            ChatColor.BOLD + law + ChatColor.WHITE
        ));
    }

    @Override
    public String getDescription() {
        return "Create empire law from content in a book in the player's hand";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.LAWS;
    }

    @Override
    public String getUsage() {
        return "/e newlaw <name>";
    }

}
