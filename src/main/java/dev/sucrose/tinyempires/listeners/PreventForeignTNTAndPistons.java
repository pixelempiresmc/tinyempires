package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.models.Empire;
import dev.sucrose.tinyempires.models.TEChunk;
import org.bson.types.ObjectId;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.HashMap;
import java.util.Map;
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
        if (!empiresEqual)
            event.setCancelled(true);
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
        // if exploding chunk has the same ID as the original chunk let TNT explode
        if ((originalEmpireId == null && empireToCompare == null)
                || (originalEmpireId != null && originalEmpireId.equals(empireToCompare)))
            return;

        // otherwise cancel event
        event.setCancelled(true);
    }

}
