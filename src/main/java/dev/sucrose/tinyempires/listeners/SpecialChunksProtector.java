package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.utils.BoundUtils;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class SpecialChunksProtector implements Listener {

    @EventHandler
    public static void onPlayerBreakBlock(BlockBreakEvent event) {
        final Location location = event.getPlayer().getLocation();
        if (BoundUtils.inBoundsOfWaterArena(location.getBlockX(), location.getBlockZ()))
            event.setCancelled(true);
    }

    @EventHandler
    public static void onPlayerPlaceBlock(BlockPlaceEvent event) {
        final Location location = event.getBlock().getLocation();
        if (BoundUtils.inBoundsOfWaterArena(location.getBlockX(), location.getBlockZ()))
            event.setCancelled(true);
    }

}
