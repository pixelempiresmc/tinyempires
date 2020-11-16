package dev.sucrose.tinyempires.listeners;

import com.mongodb.lang.Nullable;
import dev.sucrose.tinyempires.models.ChunkType;
import dev.sucrose.tinyempires.models.TEChest;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bson.types.ObjectId;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import sun.jvm.hotspot.debugger.NoSuchSymbolException;

import java.util.UUID;

public class ChestShopListener implements Listener {

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        // store chest as belonging to player if chunk exists
        final Player player = event.getPlayer();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        if (tePlayer == null) {
            player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Location location = event.getBlockPlaced().getLocation();
        if (!(event.getBlock().getType() == Material.CHEST))
            return;

        final World world = location.getWorld();
        if (world == null) {
            player.sendMessage(ErrorUtils.COULD_NOT_FETCH_WORLD);
            return;
        }

        TEChest.createChestToPlayerMapping(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ(),
            player.getUniqueId()
        );
    }

    @Nullable
    public static Block getAdjacentChestBlock(Location location) {
        final Block chestInFrontOfSign = location.clone().add(1, 0, 0).getBlock();
        if (chestInFrontOfSign.getState() instanceof Chest)
            return chestInFrontOfSign;

        final Block chestBehindOfSign = location.clone().add(-1, 0, 0).getBlock();
        if (chestBehindOfSign.getState() instanceof Chest)
            return chestBehindOfSign;

        final Block chestRightOfSign = location.clone().add(0, 0, 1).getBlock();
        if (chestRightOfSign.getState() instanceof Chest)
            return chestRightOfSign;

        final Block chestLeftOfSign = location.clone().add(0, 0, -1).getBlock();
        if (chestLeftOfSign.getState() instanceof Chest)
            return chestLeftOfSign;
        return null;
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        // store chest as belonging to player if chunk exists
        final Player player = event.getPlayer();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        if (tePlayer == null) {
            player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Location location = event.getBlock().getLocation();
        final World world = location.getWorld();
        if (world == null) {
            player.sendMessage(ErrorUtils.COULD_NOT_FETCH_WORLD);
            return;
        }

        if (event.getBlock().getState() instanceof Chest) {
            final UUID chestOwnerId = TEChest.getChestCoordinatesToPlayer(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
            );

            final TEChunk teChunk = TEChunk.getChunk(player.getLocation().getChunk());
            final TEPlayer chestOwner = TEPlayer.getTEPlayer(chestOwnerId);
            if (chestOwner == null)
                throw new NullPointerException("Could not get TEPlayer for chest owner " + chestOwnerId);

            if (teChunk != null
                    && !event.getPlayer().getUniqueId().equals(chestOwnerId)) {
                event.getPlayer().sendMessage(ChatColor.RED + String.format(
                    "You cannot destroy someone else's chest in an empire chunk! (Chest owned by %s)",
                    ChatColor.BOLD + chestOwner.getName() + ChatColor.RED
                ));
                event.setCancelled(true);
                return;
            }

            TEChest.removeChestToPlayerMapping(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
            );
        } else if (event.getBlock().getState() instanceof Sign) {
            final Block adjacentChestBlock = getAdjacentChestBlock(event.getBlock().getLocation());
            if (adjacentChestBlock != null) {
                final UUID chestOwnerId = TEChest.getChestCoordinatesToPlayer(
                    adjacentChestBlock.getWorld().getName(),
                    adjacentChestBlock.getLocation().getBlockX(),
                    adjacentChestBlock.getLocation().getBlockY(),
                    adjacentChestBlock.getLocation().getBlockZ()
                );

                if (chestOwnerId == null)
                    throw new NullPointerException("Could not get ID for chest coordinates");

                final TEPlayer chestOwner = TEPlayer.getTEPlayer(chestOwnerId);
                if (chestOwner == null)
                    throw new NullPointerException("Could not get TEPlayer for chest owner " + chestOwnerId);

                if (!chestOwnerId.equals(player.getUniqueId())) {
                    event.getPlayer().sendMessage(ChatColor.RED + String.format(
                        "You cannot destroy someone else's chest's sign! (Chest owned by %s)",
                        ChatColor.BOLD + chestOwner.getName() + ChatColor.RED
                    ));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onItemMoveInInventory(InventoryInteractEvent event) {
        final Location inventoryLocation = event.getInventory().getLocation();
        if (inventoryLocation == null
                || inventoryLocation.getWorld() == null)
            return;

        final Player player = (Player) event.getWhoClicked();
        final Block chest = inventoryLocation.getBlock();
        if (!(chest.getState() instanceof Chest))
            return;

        final UUID chestOwnerId = TEChest.getChestCoordinatesToPlayer(
            inventoryLocation.getWorld().getName(),
            inventoryLocation.getBlockX(),
            inventoryLocation.getBlockY(),
            inventoryLocation.getBlockZ()
        );

        if (chestOwnerId != null
                && !chestOwnerId.equals(player.getUniqueId())) {
            final TEPlayer tePlayer = TEPlayer.getTEPlayer(chestOwnerId);
            if (tePlayer == null)
                throw new NullPointerException("Could not get TEPlayer for UUID " + chestOwnerId);

            player.sendMessage(ChatColor.RED + String.format(
                "You cannot modify the inventory of someone else's chest shop! (Owned by %s)",
                ChatColor.BOLD + tePlayer.getName() + ChatColor.RED
            ));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final Inventory inventory = event.getInventory();

        if (inventory.getType() != InventoryType.CHEST
                && inventory.getLocation() != null)
            return;

        final Location location = inventory.getLocation();
        if (location == null)
            return;

        final TEChunk chunk = TEChunk.getChunk(location.getChunk());
        if (chunk == null
                || chunk.getType() != ChunkType.TRADING)
            return;

        // fetch/check slot price for single and double chest cases
        final BlockState blockState = location.getBlock().getState();
        // return if clicked block wasn't a chest
        if (!(blockState instanceof Chest))
            return;

        final Chest chest = (Chest) blockState;
        final Vector directionVector = ((Directional) chest.getBlockData()).getFacing().getDirection();
        double costPerSlot;
        if (inventory.getHolder() instanceof DoubleChest) {
            final DoubleChest doubleChest = (DoubleChest) inventory.getHolder();

            final Chest leftChest = (Chest) doubleChest.getLeftSide();
            if (leftChest == null) {
                player.sendMessage(ChatColor.RED + "Could not get right side of chest while trying to purchase slot, " +
                    "please a notify a developer of this and we will tend to this shortly.");
                return;
            }
            final Block leftSignBlock = leftChest.getBlock().getLocation().add(directionVector).getBlock();

            final Chest rightChest = (Chest) doubleChest.getRightSide();
            if (rightChest == null) {
                player.sendMessage(ChatColor.RED + "Could not get right side of chest while trying to purchase slot, " +
                    "please a notify a developer of this and we will tend to this shortly.");
                return;
            }
            final Block rightSignBlock = rightChest.getBlock().getLocation().add(directionVector).getBlock();

            Sign rightSign = null;
            if (rightSignBlock.getState() instanceof Sign)
                rightSign = (Sign) rightSignBlock.getState();

            Sign leftSign = null;
            if (leftSignBlock.getState() instanceof Sign)
                leftSign = (Sign) leftSignBlock.getState();

            Double rightSignPrice = null;
            Double leftSignPrice = null;
            if (rightSign != null) {
                try {
                    rightSignPrice = Double.parseDouble(rightSign.getLine(3));
                } catch (Exception ignore) {}
            }

            if (leftSign != null) {
                try {
                    leftSignPrice = Double.parseDouble(leftSign.getLine(3));
                } catch (Exception ignore) {}
            }

            if (rightSignPrice != null
                    && leftSignPrice != null
                    && !rightSignPrice.equals(leftSignPrice)) {
                player.sendMessage(ChatColor.RED + "Double-chest cannot have two signs with different prices! Please " +
                    "contact the shop owner.");
                return;
            }

            if (rightSignPrice == null
                    && leftSignPrice == null)
                return;

            // there must be one valid price
            costPerSlot = rightSignPrice == null
                ? leftSignPrice
                : rightSignPrice;
        } else if (inventory.getHolder() instanceof Chest) {
            // add chest direction vector to location to get would-be sign location
            // Location#add mutates location but returns original
            final Location signLocation = chest.getLocation().add(directionVector);
            final BlockState sign = signLocation.getBlock().getState();

            if (!(sign instanceof Sign))
                return;

            try {
                costPerSlot = Double.parseDouble(((Sign) sign).getLine(3));
            } catch (Exception ignore) {
                return;
            }
        } else {
            return;
        }

        // fetch and check if player is owner
        final World world = location.getWorld();
        if (world == null) {
            player.sendMessage(ErrorUtils.COULD_NOT_FETCH_WORLD);
            return;
        }

        final UUID chestOwnerId = TEChest.getChestCoordinatesToPlayer(
            world.getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );

        if (chestOwnerId == null) {
            player.sendMessage(ChatColor.RED
                + "ERROR: Chest owner could not be fetched. Please notify a developer and we will tend to this " +
                "promptly."
            );
            return;
        }

        if (chestOwnerId.equals(player.getUniqueId()))
            return;

        // cancel event to prevent non-owners from putting in items
        event.setCancelled(true);

        // fetch slot and return if no items clicked
        final int clickedSlotIndex = event.getSlot();
        final ItemStack itemsInSlot = inventory.getItem(clickedSlotIndex);
        if (itemsInSlot == null)
            return;
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        if (tePlayer == null) {
            player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        // return if player can't afford slot
        if (tePlayer.getBalance() < costPerSlot) {
            player.sendMessage(ChatColor.RED + String.format(
                "Insufficient funds to purchase slot (%.1f coins required)",
                costPerSlot
            ));
            return;
        }

        // clear slot, give player items and charge player cost
        inventory.clear(clickedSlotIndex);
        player.getInventory().addItem(itemsInSlot);
        tePlayer.takeCoins(costPerSlot);
        final TEPlayer owner = TEPlayer.getTEPlayer(chestOwnerId);
        if (owner == null)
            throw new NullPointerException("Could not get owner of chest shop (" + chestOwnerId + ")");
        owner.giveCoins(costPerSlot);

        // send success message to player
        final int itemsInSlotAmount = itemsInSlot.getAmount();
        player.sendMessage(ChatColor.GREEN + String.format(
            "Successfully purchased %d %s for %.1f coins",
            itemsInSlot.getAmount(),
            itemsInSlot.getType().name().toLowerCase().replace('_', ' ') + (itemsInSlotAmount > 1 ? "s" : ""),
            costPerSlot
        ));

        final Player ownerPlayer = Bukkit.getPlayer(chestOwnerId);
        if (ownerPlayer != null)
            ownerPlayer.sendMessage(ChatColor.GREEN + String.format(
                "%s purchased %d %s for %.1f coins from one of your chest shops",
                ChatColor.BOLD + player.getName() + ChatColor.GREEN,
                itemsInSlot.getAmount(),
                itemsInSlot.getType().name().toLowerCase().replace('_', ' ') + (itemsInSlotAmount > 1 ? "s" : ""),
                costPerSlot
            ));
    }

}
