package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.commands.arena.Yggdrasil;
import dev.sucrose.tinyempires.utils.BoundUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

/**
 * Bukkit event listener that prevents players from damaging map structures (arenas, Mt. Olympus, etc..)
 */
public class StructureProtection implements Listener {

    private void cancelIfInBounds(Cancellable event, Location location) {
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        final World world = location.getWorld();
        if (world == null)
            throw new NullPointerException("Unable to get player world when checking if in bounds of protected area");
        if (BoundUtils.inBoundsOfSpecialChunk(world.getName(), x, z))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()
                || Yggdrasil.isPlayerInGame(player.getUniqueId()))
            return;
        cancelIfInBounds(event, event.getBlock().getLocation());
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if (event.getPlayer().isOp()
                || Yggdrasil.isPlayerInGame(event.getPlayer().getUniqueId()))
            return;
        cancelIfInBounds(event, event.getBlock().getLocation());
    }

    @EventHandler
    public void onEntityExplosion(ExplosionPrimeEvent event) {
        // prevent explosions from damaging special territory
        final Location location = event.getEntity().getLocation();
        if (BoundUtils.isChunkInBoundsOfSpecialTerritory(location.getChunk()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplosion(BlockExplodeEvent event) {
        // prevent explosions from damaging special territory
        final Location location = event.getBlock().getLocation();
        if (BoundUtils.isChunkInBoundsOfSpecialTerritory(location.getChunk()))
            event.setCancelled(true);
    }

}
