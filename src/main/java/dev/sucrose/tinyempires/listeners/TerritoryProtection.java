package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.models.ChunkType;
import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.BoundUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.PressureSensor;

/**
 * Bukkit event listener that prevents players from damaging or interacting with chests, doors, buttons or levers other empires' territories
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());

        if (tePlayer == null) {
            player.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        // return if event is not block-related
        final Block block = event.getClickedBlock();
        if (block == null)
            return;
        final Chunk chunk = block.getLocation().getChunk();
        final TEChunk teChunk = TEChunk.getChunk(chunk);

        // return if player not in empire
        if (playerInChunkOwner(tePlayer, teChunk))
            return;

        final String startOfResponse = ChatColor.RED + String.format(
            "You are in the empire of %s and you cannot ",
            teChunk.getEmpire().getName()
        );
        if (block.getType().name().contains("TRAPDOOR")) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "open trapdoors");
        } else if (block.getType().name().contains("DOOR")) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "interact with doors");
        } else if (block.getState() instanceof Chest
                && teChunk.getType() != ChunkType.TRADING) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "open chests");
        } else if (block.getType() == Material.LEVER) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "flip levers");
        } else if (block.getType().name().contains("BUTTON")) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "press buttons");
        } else if (block.getType().name().contains("PRESSURE_PLATE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public static void onMobSpawn(EntitySpawnEvent event) {
        final Location location = event.getLocation();
        // prevent monster (hostile mob) spawning in special chunks
        if (event.getEntity() instanceof Monster
                && location.getWorld() != null
                && BoundUtils.inBoundsOfSpecialChunk(location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockZ()
        )) {
            event.setCancelled(true);
            System.out.println("Cancelling mob spawn (TerritoryProtection:onMobSpawn)");
        }
    }

}
