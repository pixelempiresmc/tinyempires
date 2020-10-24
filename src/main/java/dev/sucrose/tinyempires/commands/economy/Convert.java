package dev.sucrose.tinyempires.commands.economy;

import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Convert implements CommandExecutor, Listener {

    private static final String INVENTORY_GUI_NAME = "" + ChatColor.GOLD + ChatColor.BOLD + "Convert";
    private static final Material CONVERT_COINS_ITEM_MATERIAL = Material.ANVIL;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        final Player player = (Player) sender;
        final Inventory transferCoinsMenu = Bukkit.getServer().createInventory(player, 27, INVENTORY_GUI_NAME);
        for (int i = 0; i < 27; i++) {
            ItemStack filler = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            assert meta != null;
            meta.setDisplayName("" + ChatColor.WHITE);
            filler.setItemMeta(meta);
            transferCoinsMenu.setItem(i, filler);
        }

        // coin item
        final ItemStack coinItem = new ItemStack(CONVERT_COINS_ITEM_MATERIAL);
        final ItemMeta coinItemMeta = coinItem.getItemMeta();
        assert coinItemMeta != null;
        coinItemMeta.setDisplayName(ChatColor.GOLD + "Convert");
        coinItemMeta.setLore(
            Arrays.asList(ChatColor.LIGHT_PURPLE +
                "Convert coins & netherite to coins",
                "Diamond -> 10 coins",
                "Netherite ingot -> 100 coins"
            )
        );

        coinItem.setItemMeta(coinItemMeta);
        transferCoinsMenu.setItem(13, coinItem);
        player.openInventory(transferCoinsMenu);
        return true;
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent e) {
        // check clicked inventory was converter and if click was in top inventory
        if (e.getView().getTitle().equals(INVENTORY_GUI_NAME)
                && e.getRawSlot() < e.getView().getTopInventory().getSize()) {
            e.setCancelled(true);
            if ((e.getCurrentItem() != null ? e.getCurrentItem().getType() : null) == CONVERT_COINS_ITEM_MATERIAL) {
                if (e.getCursor() == null)
                    return;

                final Player player = (Player) e.getWhoClicked();
                final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
                if (tePlayer == null) {
                    player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
                    return;
                }

                final ItemStack cursor = e.getCursor();
                final int amountOfItemsInCursor = cursor.getAmount();

                if (cursor.getType() != Material.DIAMOND) {
                    player.sendMessage(ChatColor.RED + "Invalid item");
                    return;
                }

                tePlayer.giveCoins(amountOfItemsInCursor * 10);
                player.sendMessage(ChatColor.GREEN + String.format(
                    "Converted %d diamond%s to %d coins",
                    amountOfItemsInCursor,
                    amountOfItemsInCursor > 1 ? "s" : "",
                    amountOfItemsInCursor * 10
                ));
                e.getCursor().setAmount(0);
            }
        }
    }

}
