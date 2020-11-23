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
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Bukkit event listener that prevents players from damaging or interacting with chests, doors, buttons or levers other empires' territories
 */
public class TerritoryProtection implements Listener {

    private boolean playerInChunkOwnerOrAllyAndUnopped(TEPlayer player, TEChunk chunk) {
        if (chunk == null)
            return true;
        final Empire owner = chunk.getEmpire();
        if (owner == null)
            throw new NullPointerException("Owner for chunk found as null");
        final Empire playerEmpire = player.getEmpire();
        // true if player is not in the same empire as the owner of
        // the chunk and is not in an empire allying with them
        return owner.getId().equals(playerEmpire == null ? null : playerEmpire.getId())
            || (player.getEmpire() != null && owner.getAllies().contains(player.getEmpire().getId()));
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

        if (player.isOp()) {
            player.sendMessage(ChatColor.YELLOW +
                "Block break would be cancelled but player is opped, returning"
            );
            return;
        }

        if (playerInChunkOwnerOrAllyAndUnopped(tePlayer, teChunk))
            return;

        // different empire, cancel event
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + String.format(
            "You are in the empire of %s and cannot destroy blocks",
            teChunk.getEmpire().getName()
        ));
    }

    private void handleBlockPlaceEvent(Cancellable placeEvent, Chunk chunk, Player placer) {
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(placer.getUniqueId());
        final TEChunk teChunk = TEChunk.getChunk(chunk);

        if (tePlayer == null) {
            placer.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        if (placer.isOp()) {
            placer.sendMessage(ChatColor.YELLOW +
                "Block break would be cancelled but player is opped, returning"
            );
            return;
        }

        // return if no empire owns chunk
        if (playerInChunkOwnerOrAllyAndUnopped(tePlayer, teChunk))
            return;

        // different empire, cancel event
        placeEvent.setCancelled(true);
        placer.sendMessage(ChatColor.RED + String.format(
            "You are in the empire of %s and cannot place blocks",
            teChunk.getEmpire().getName()
        ));
    }

    @EventHandler
    public void onPlayerPlaceLava(PlayerBucketEmptyEvent event) {
        handleBlockPlaceEvent(event, event.getBlock().getChunk(), event.getPlayer());
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        handleBlockPlaceEvent(event, event.getBlock().getChunk(), event.getPlayer());
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
        if (playerInChunkOwnerOrAllyAndUnopped(tePlayer, teChunk))
            return;

        if (player.isOp()) {
            player.sendMessage(ChatColor.YELLOW +
                "Block break would be cancelled but player is opped, returning"
            );
            return;
        }

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
        } else if (block.getState() instanceof Furnace) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "open furnaces");
        } else if (block.getState() instanceof Barrel) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "open barrels");
        } else if (event.getAction() == Action.PHYSICAL
                && block.getState() instanceof Farmland) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "trample crops");
        } else if (block.getState() instanceof Gate) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "interact with gates");
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK
                && block.getState() instanceof ItemFrame) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "interact with item-frames");
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getItem() != null
                && event.getItem().getType() == Material.FLINT_AND_STEEL) {
            event.setCancelled(true);
            player.sendMessage(startOfResponse + "light fires");
        }
    }

    @EventHandler
    public static void onCreeperExplode(EntityExplodeEvent event) {
        // prevent creepers from exploding in empire chunks
        if (event.getEntityType() == EntityType.CREEPER
                && TEChunk.getChunk(event.getEntity().getLocation().getChunk()) != null)
            event.setCancelled(true);
    }

    @EventHandler
    public static void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;

        final Player player = (Player) event.getDamager();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
        if (tePlayer == null)
            throw new NullPointerException("Could not get TEPlayer for " + player.getUniqueId());

        final Location location = event.getEntity().getLocation();
        final TEChunk teChunk = TEChunk.getChunk(location.getChunk());
        if (teChunk != null
                && !teChunk.getEmpire().getId().equals(
                    tePlayer.getEmpire() == null
                        ? null
                        : tePlayer.getEmpire().getId()
                    )
        ) {
            player.sendMessage(ChatColor.RED + "You cannot damage entities belonging to other empires!");
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
