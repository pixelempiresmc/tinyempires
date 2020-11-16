package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.TinyEmpires;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;

public class WorldBorder implements Listener {

    private static void circumnavigate(Player player, Location loc) {
        // get highest block at x and y coords
        for (int y = 255; y > 0; y--) {
            if (player.getWorld().getBlockAt((int)loc.getX(), y, (int)loc.getZ()).getType() != Material.AIR) {
                loc.setY(y + 2);
                break;
            }
        }

        // account for vehicles
        if (player.getVehicle() != null) {
            EntityType vehicleType = player.getVehicle().getType();
            if (vehicleType == EntityType.BOAT) {
                player.getVehicle().remove();
                String woodTypeString = ((Boat) player.getVehicle()).getWoodType().toString();
                player.getInventory().addItem(new ItemStack(Material.valueOf((woodTypeString.equals("GENERIC") ? "OAK" : woodTypeString) + "_BOAT")));
            }
            if (vehicleType == EntityType.MINECART) {
                player.getVehicle().remove();
                player.getInventory().addItem(new ItemStack(Material.MINECART));
            }
        }
        player.teleport(loc);
        player.sendMessage("Circumnavigated!");
    }

    // prevent portal lighting outside nether limits
    @EventHandler
    public static void onPortalLight(PortalCreateEvent event) {
        if (event.getBlocks().get(0).getWorld().getName().equals("world_nether")
                && event.getBlocks().stream().anyMatch(b -> {
            final Location location = b.getLocation();
            return location.getBlockX() > TinyEmpires.WORLD_BORDER_RIGHT_X / 8
                || location.getBlockX() < TinyEmpires.WORLD_BORDER_LEFT_X / 8
                || location.getBlockZ() < TinyEmpires.WORLD_BORDER_TOP_Z / 8
                || location.getBlockZ() > TinyEmpires.WORLD_BORDER_BOTTOM_Z / 8;
        })) {
            event.setCancelled(true);
            // if player lit portal tell them
            if (event.getEntity() != null
                    && event.getEntity() instanceof Player)
                event.getEntity().sendMessage(ChatColor.RED +
                    "Portal cannot be lit outside of overworld-to-nether limits (check the map)");
        }
    }

    @EventHandler
    public static void playerMove(PlayerMoveEvent e) {
        final Player player = e.getPlayer();
        if (!player.getWorld().getName().equals("world"))
            return;

        final Location playerLoc = player.getLocation();
        final double playerX = playerLoc.getX();
        final double playerZ = playerLoc.getZ();
        if (TinyEmpires.WORLD_BORDER_BOTTOM_Z - TinyEmpires.MARGIN <= playerZ) {
            playerLoc.setZ(TinyEmpires.WORLD_BORDER_TOP_Z + 5 + TinyEmpires.MARGIN);
            circumnavigate(player, playerLoc);
            return;
        }

        if (TinyEmpires.WORLD_BORDER_TOP_Z + TinyEmpires.MARGIN >= playerZ) {
            playerLoc.setZ(TinyEmpires.WORLD_BORDER_BOTTOM_Z - 5 - TinyEmpires.MARGIN);
            circumnavigate(player, playerLoc);
            return;
        }

        if (TinyEmpires.WORLD_BORDER_LEFT_X + TinyEmpires.MARGIN >= playerX) {
            playerLoc.setX(TinyEmpires.WORLD_BORDER_RIGHT_X - 5 - TinyEmpires.MARGIN);
            circumnavigate(player, playerLoc);
            return;
        }

        if (TinyEmpires.WORLD_BORDER_RIGHT_X - TinyEmpires.MARGIN <= playerX) {
            playerLoc.setX(TinyEmpires.WORLD_BORDER_LEFT_X + 5 + TinyEmpires.MARGIN);
            circumnavigate(player, playerLoc);
        }
    }

}
