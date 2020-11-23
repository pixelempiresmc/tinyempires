package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEChunk;
import org.bson.types.ObjectId;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PreventForeignTNTAndPistons implements Listener {

    private static final Map<UUID, ObjectId> tntToTriggeredEmpireID = new HashMap<>();

    @EventHandler
    public static void onPistonFire(BlockPistonExtendEvent event) {
        final Location location = event.getBlock().getLocation();
        final Empire originalChunkEmpire = TEChunk.getChunk(location.getChunk()).getEmpire();
        final Empire currentChunkEmpire =
            TEChunk.getChunk(location.add(event.getDirection().getDirection()).getChunk()).getEmpire();
        final boolean empiresEqual =
            (originalChunkEmpire == null && currentChunkEmpire == null)
                || (originalChunkEmpire != null && originalChunkEmpire.getId().equals(
                    currentChunkEmpire == null
                        ? null
                        : currentChunkEmpire.getId()
                ));
        // allow for foreign pistons in war
        if (originalChunkEmpire != null
                && currentChunkEmpire != null
                && Objects.equals(originalChunkEmpire.getAtWarWith(), currentChunkEmpire.getAtWarWith()))
            return;

        if (!empiresEqual)
            return;

        for (final Block block : event.getBlocks()) {
            final TEChunk blockTEChunk = TEChunk.getChunk(block.getLocation().getChunk());
            if (blockTEChunk != null
                    && blockTEChunk.getEmpire().getId().equals(
                        originalChunkEmpire == null
                            ? null
                            : originalChunkEmpire.getId()
                    )
            ) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public static void onTNTTrigger(EntitySpawnEvent event) {
        if (event.getEntityType() != EntityType.PRIMED_TNT)
            return;

        final TEChunk chunk = TEChunk.getChunk(event.getLocation().getChunk());
        tntToTriggeredEmpireID.put(
            event.getEntity().getUniqueId(),
            chunk == null
                ? null
                : chunk.getEmpire().getId()
        );
    }

    @EventHandler
    public static void onTNTExplode(EntityExplodeEvent event) {
        if (event.getEntityType() != EntityType.PRIMED_TNT)
            return;

        final TEChunk chunk = TEChunk.getChunk(event.getLocation().getChunk());
        final ObjectId empireToCompare = chunk == null ? null : chunk.getEmpire().getId();
        final ObjectId originalEmpireId = tntToTriggeredEmpireID.get(event.getEntity().getUniqueId());

        // remove TNT from UUID to Empire map
        tntToTriggeredEmpireID.remove(event.getEntity().getUniqueId());

        final Empire chunkEmpire = Empire.getEmpire(empireToCompare);
        final Empire originalEmpire = Empire.getEmpire(originalEmpireId);
        // allow foreign TNT in war
        if (chunkEmpire != null
                && originalEmpire != null
                && Objects.equals(chunkEmpire.getAtWarWith(), originalEmpire.getAtWarWith()))
            return;

        // if exploding chunk has the same ID as the original chunk let TNT explode
        if ((originalEmpireId == null && empireToCompare == null)
                || (originalEmpireId != null && originalEmpireId.equals(empireToCompare)))
            return;

        // otherwise cancel event
        event.setCancelled(true);
    }

}
