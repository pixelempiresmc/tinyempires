package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Bukkit event listener that prevents players from damaging other empires' territories
 */
public class TerritoryProtection implements Listener {

    private boolean playerInChunkOwner(TEPlayer player, TEChunk chunk) {
        if (chunk == null)
            return true;
        final Empire owner = chunk.getEmpire();
        if (owner == null)
            throw new NullPointerException("Owner for chunk found as null");
        final Empire playerEmpire = player.getEmpire();
        return player.getEmpire() != null && owner.getId().equals(playerEmpire.getId());
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        final Chunk chunk = event.getBlock().getChunk();
        final TEChunk teChunk = TEChunk.getChunk(chunk);

        if (tePlayer == null) {
            player.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        // return if no empire owns chunk
        if (playerInChunkOwner(tePlayer, teChunk))
            return;

        // different empire, cancel event
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + String.format(
            "You are in the empire of %s and cannot destroy blocks",
            teChunk.getEmpire().getName()
        ));
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        final Chunk chunk = event.getBlock().getChunk();
        final TEChunk teChunk = TEChunk.getChunk(chunk);

        if (tePlayer == null) {
            player.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        // return if no empire owns chunk
        if (playerInChunkOwner(tePlayer, teChunk))
            return;

        // different empire, cancel event
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + String.format(
            "You are in the empire of %s and cannot place blocks",
            teChunk.getEmpire().getName()
        ));
    }

//    @EventHandler
//    public void onPlayerInteract(PlayerInteractEvent event) {
//        final Player player = event.getPlayer();
//        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
//        final Chunk chunk = player.getLocation().getChunk();
//        final TEChunk teChunk = TEChunk.getChunk(chunk);
//
//        if (tePlayer == null) {
//            player.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
//            return;
//        }
//
//        // return if no empire owns chunk
//        if (playerInChunkOwner(tePlayer, teChunk))
//            return;
//
//        // different empire, cancel event
//        event.setCancelled(true);
//        player.sendMessage(ChatColor.RED + String.format(
//            "You are in the empire of %s and you cannot interact with blocks",
//            teChunk.getEmpire().getName()
//        ));
//    }

}
