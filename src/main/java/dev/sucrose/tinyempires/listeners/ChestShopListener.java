package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.models.ChunkType;
import dev.sucrose.tinyempires.models.TEChest;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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
        if (!(event.getBlock().getType() == Material.CHEST))
            return;

        final World world = location.getWorld();
        if (world == null) {
            player.sendMessage(ErrorUtils.COULD_NOT_FETCH_WORLD);
            return;
        }

        TEChest.removeChestToPlayerMapping(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final Inventory inventory = event.getClickedInventory();
        // event triggers even if cursor was not on slot, return
        if (inventory == null) {
            System.out.println("Inventory was null");
            return;
        }

        System.out.println(inventory.getType());
        if (inventory.getType() != InventoryType.CHEST
                && inventory.getLocation() != null) {
            System.out.println("Inventory clicked was not a chest");
            return;
        }

        final Location location = inventory.getLocation();
        if (location == null)
            return;

        final TEChunk chunk = TEChunk.getChunk(location.getChunk());
        if (chunk == null
                || chunk.getType() != ChunkType.TRADING)
            return;

        System.out.println("Clicked on a chest in a trading chunk!");
        // fetch/check slot price for single and double chest cases
        final BlockState blockState = location.getBlock().getState();
        // return if clicked block wasn't a chest
        if (!(blockState instanceof Chest))
            return;

        final Chest chest = (Chest) blockState;
        final Vector directionVector = ((Directional) chest.getBlockData()).getFacing().getDirection();
        double costPerSlot;
        System.out.println("Vector: " + directionVector.toString());
        if (inventory.getHolder() instanceof DoubleChest) {
            System.out.println("Chest is a double chest");

            final DoubleChest doubleChest = (DoubleChest) inventory.getHolder();

            if (doubleChest.getRightSide() == null
                    || doubleChest.getRightSide().getInventory().getLocation() == null) {
                player.sendMessage(ChatColor.RED + "ERROR: Could not fetch left side or location of double chest. " +
                    "Please contact a developer and we will tend to this shortly.");
                return;
            }

            final Block rightSignBlock = doubleChest
                .getRightSide()
                .getInventory()
                .getLocation()
                .add(directionVector)
                .getBlock();

            if (doubleChest.getLeftSide() == null
                    || doubleChest.getLeftSide().getInventory().getLocation() == null) {
                player.sendMessage(ChatColor.RED + "ERROR: Could not fetch left side or location of double chest. " +
                    "Please contact" +
                    " " +
                    "a developer and we will tend to this shortly.");
                return;
            }

            final Block leftSignBlock = doubleChest
                .getLeftSide()
                .getInventory()
                .getLocation()
                .add(directionVector)
                .getBlock();

            Sign rightSign = null;
            if (rightSignBlock instanceof Sign)
                rightSign = (Sign) rightSignBlock.getState();

            Sign leftSign = null;
            if (leftSignBlock instanceof Sign)
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
            System.out.println("Inventory holder is a single chest");

            // add chest direction vector to location to get would-be sign location
            // Location#add mutates location but returns original
            final Location signLocation = chest.getLocation().add(directionVector);
            final BlockState sign = signLocation.getBlock().getState();

            if (!(sign instanceof Sign)) {
                System.out.println("Chest did not have a sign");
                return;
            }

            try {
                costPerSlot = Double.parseDouble(((Sign) sign).getLine(3));
            } catch (Exception ignore) {
                return;
            }
        } else {
            // inventory holder is not a chest type
            System.out.println("Inventory holder is not a chest");

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

        if (chestOwnerId.equals(player.getUniqueId())) {
            System.out.printf("Chest owner ID (%s) equals clicker ID (%s)\n", chestOwnerId, player.getUniqueId());
            return;
        }

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
