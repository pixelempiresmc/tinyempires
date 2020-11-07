package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.utils.BoundUtils;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Bukkit event listener that prevents players from damaging map structures (arenas, Mt. Olympus, etc..)
 */
public class StructureProtection implements Listener {

    private void cancelIfInBounds(Cancellable event, Location location) {
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        if (BoundUtils.inBoundsOfOlympus(x, z)
                || BoundUtils.inBoundsOfWaterArena(x, z))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        cancelIfInBounds(event, event.getBlock().getLocation());
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        cancelIfInBounds(event, event.getBlock().getLocation());
    }

}
