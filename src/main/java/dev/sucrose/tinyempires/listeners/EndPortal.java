package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class EndPortal implements Listener {

    private static final int minPitX = 461;
    private static final int maxPitX = 569;
    private static final int minPitZ = 303;
    private static final int maxPitZ = 419;
    private static final int maxPitY = 41;

    private static final int portalCastleMinX = 495;
    private static final int portalCastleMaxX = 531;
    private static final int portalCastleMinZ = 348;
    private static final int portalCastleMaxZ = 387;

    private static final World theEnd;

    static {
        theEnd = Bukkit.getWorld("world_the_end");
        if (theEnd == null)
            throw new NullPointerException("'world_the_end' fetched as null when initializing EndPortal listener");
    }

    @EventHandler
    public static void onEntitySpawn(EntitySpawnEvent e) {
        Location loc = e.getLocation();
        // prevent entity spawning in nether castle
        if (loc.getWorld() != null
                && loc.getWorld().getName().equals("world_nether")
                && loc.getX() > portalCastleMinX
                && loc.getX() < portalCastleMaxX
                && loc.getZ() > portalCastleMinZ
                && loc.getZ() < portalCastleMaxZ
                && e.getEntityType() != EntityType.ENDER_SIGNAL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public static void onPlayerMove(PlayerMoveEvent e) {
        Location loc = e.getTo();
        Player player = e.getPlayer();
        TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        // check if player is near nether castle
        if (loc != null
                && loc.getWorld() != null
                && loc.getWorld().getName().equals("world_nether")
                && loc.getX() > minPitX
                && loc.getX() < maxPitX
                && loc.getZ() > minPitZ
                && loc.getZ() < maxPitZ
        ) {
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
            if (loc.getY() < maxPitY)
                e.getPlayer().teleport(theEnd.getSpawnLocation());
        }
    }

}
