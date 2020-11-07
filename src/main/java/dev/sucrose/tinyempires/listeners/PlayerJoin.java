package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.commands.godsuite.Invisible;
import dev.sucrose.tinyempires.models.TEPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = event.getPlayer().getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(playerId);

        player.sendMessage(ChatColor.GREEN + "Welcome to the Tiny Empires server!");
        if (tePlayer == null) {
            final TEPlayer p = TEPlayer.createPlayer(player.getUniqueId(), player.getName());
            p.updatePlayerScoreboard();
            return;
        }
        tePlayer.updatePlayerScoreboard();

        player.setInvisible(false);

        // invisible players
        for (final UUID uuid : Invisible.getInvisiblePlayers()) {
            final Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                player.hidePlayer(TinyEmpires.getInstance(), p);
        }
    }

}
