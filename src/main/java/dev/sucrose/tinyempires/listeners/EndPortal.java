package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.BoundUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.*;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class EndPortal implements Listener {

    private static final World theEnd;

    private static final int[] portalLoc = new int[] { 514, 60, 365 };

    static {
        theEnd = Bukkit.getWorld("world_the_end");
        if (theEnd == null)
            throw new NullPointerException("'world_the_end' fetched as null when initializing EndPortal listener");
    }

    @EventHandler
    public static void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_AIR
            || event.getAction() == Action.LEFT_CLICK_BLOCK)
            return;

        if (event.hasItem()) {
            if (event.getItem() != null
                    && event.getItem().getType() == Material.ENDER_EYE) {
                event.setCancelled(true);
                if (!player.getWorld().getName().equals("world_nether")) {
                    player.sendMessage(ChatColor.GOLD + "The eye hints towards a different dimension...");
                    return;
                }
                player.getInventory().removeItem(new ItemStack(Material.ENDER_EYE));
                final EnderSignal enderSignal = (EnderSignal) player.getWorld().spawnEntity(player.getLocation(), EntityType.ENDER_SIGNAL);
                enderSignal.setTargetLocation(new Location(player.getWorld(), portalLoc[0], portalLoc[1], portalLoc[2]));
            }
        }
    }

    @EventHandler
    public static void onEntitySpawn(EntitySpawnEvent e) {
        Location loc = e.getLocation();
        // prevent entity spawning in nether castle
        if (loc.getWorld() != null
                && BoundUtils.inBoundsOfPortalCastle(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ())
                && e.getEntityType() != EntityType.ENDER_SIGNAL) {
            e.setCancelled(true);
        }
    }

    public static final int PIT_TOP_Y = 41;
    @EventHandler
    public static void onPlayerMove(PlayerMoveEvent e) {
        final Location loc = e.getTo();
        final Player player = e.getPlayer();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        // check if player is near nether castle
        if (loc != null
                && loc.getWorld() != null
                && BoundUtils.inBoundsOfPit(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ())) {
            if (tePlayer == null) {
                player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
                return;
            }

            // "jumped in" nether castle message
            if (!tePlayer.getJumpedInAdvancement()) {
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.ITALIC + "You feel an urge to jump in...");
                tePlayer.setJumpInAdvancement(true);
            }

            // nether castle end portal pit
            if (loc.getY() < PIT_TOP_Y)
                e.getPlayer().teleport(theEnd.getSpawnLocation());
        }
    }

}
