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

    private static final String INVENTORY_GUI_NAME = "Convert to Coins";

    private static void fillInventory(Inventory inventory) {
        final ItemStack coinItem = new ItemStack(Material.ANVIL);
        final ItemMeta coinItemMeta = coinItem.getItemMeta();
        if (coinItemMeta == null)
            throw new NullPointerException("Could not get item meta for anvil conversion item when creating convert " +
                "inventory GUI");

        coinItemMeta.setDisplayName(INVENTORY_GUI_NAME);
        coinItemMeta.setLore(
            Arrays.asList(ChatColor.LIGHT_PURPLE +
                    "Convert coins & netherite to coins",
                "Diamond -> 10 coins",
                "Netherite ingot -> 100 coins"
            )
        );
        coinItem.setItemMeta(coinItemMeta);

        inventory.setContents(
            new ItemStack[] {
                /* == Row 1 == */
                new ItemStack(Material.PURPLE_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                new ItemStack(Material.YELLOW_STAINED_GLASS_PANE),
                new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.PURPLE_STAINED_GLASS_PANE),
                /* == == == */

                /* == Row 2 == */
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                new ItemStack(Material.YELLOW_STAINED_GLASS_PANE),
                coinItem,
                new ItemStack(Material.YELLOW_STAINED_GLASS_PANE),
                new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                /* == == == */

                /* == Row 3 == */
                new ItemStack(Material.PURPLE_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                new ItemStack(Material.YELLOW_STAINED_GLASS_PANE),
                new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                new ItemStack(Material.PURPLE_STAINED_GLASS_PANE)
                /* == == == */
            }
        );
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        final Player player = (Player) sender;
        final Inventory transferCoinsMenu = Bukkit.getServer().createInventory(player, 27, INVENTORY_GUI_NAME);
        fillInventory(transferCoinsMenu);
        player.openInventory(transferCoinsMenu);
        return true;
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent e) {
        // check clicked inventory was converter and if click was in top inventory
        if (e.getView().getTitle().equals(INVENTORY_GUI_NAME)
                && e.getRawSlot() < e.getView().getTopInventory().getSize()) {
            e.setCancelled(true);
            if ((e.getCurrentItem() != null ? e.getCurrentItem().getType() : null) == Material.ANVIL) {
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

                switch (cursor.getType()) {
                    case DIAMOND:
                        tePlayer.giveCoins(amountOfItemsInCursor * 10);
                        player.sendMessage(ChatColor.GREEN + String.format(
                            "Converted %d diamond%s to %d coins",
                            amountOfItemsInCursor,
                            amountOfItemsInCursor > 1 ? "s" : "",
                            amountOfItemsInCursor * 10
                        ));
                        e.getCursor().setAmount(0);
                        break;
                    case NETHERITE_INGOT:
                        tePlayer.giveCoins(amountOfItemsInCursor * 100);
                        player.sendMessage(ChatColor.GREEN + String.format(
                            "Converted %d netherite ingot%s to %d coins",
                            amountOfItemsInCursor,
                            amountOfItemsInCursor > 1 ? "s" : "",
                            amountOfItemsInCursor * 100
                        ));
                        e.getCursor().setAmount(0);
                        break;
                    default:
                        player.sendMessage(ChatColor.RED + "Invalid item");
                }
            }
        }
    }

}
