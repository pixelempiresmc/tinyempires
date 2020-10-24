package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.commands.empire.options.AutoClaimEmpireChunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerLeave implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (AutoClaimEmpireChunk.isAutoclaiming(uuid))
            AutoClaimEmpireChunk.removeAutoclaimer(uuid);
    }

}
