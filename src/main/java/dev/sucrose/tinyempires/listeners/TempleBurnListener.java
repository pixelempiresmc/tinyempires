package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.discord.DiscordBot;
import dev.sucrose.tinyempires.models.ChunkType;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.UUID;

public class TempleBurnListener implements Listener {

    private static UUID lastPlayerToThrowItem;

    @EventHandler
    public void onTempleBurnListener(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item))
            return;
        final Location location = event.getEntity().getLocation();
        final TEChunk chunk = TEChunk.getChunk(location.getChunk());
        if (chunk != null
                && chunk.getType() == ChunkType.TEMPLE) {
            event.getEntity().remove();
            DiscordBot.sendPrayerMessage(lastPlayerToThrowItem, location,
                ((Item) event.getEntity()).getItemStack());
        }
    }

    @EventHandler
    public void onPlayerThrowItem(PlayerDropItemEvent event) {
        lastPlayerToThrowItem = event.getPlayer().getUniqueId();
    }

}
