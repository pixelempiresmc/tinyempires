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
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.dynmap.DynmapAPI;

import java.util.UUID;

public class PlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = event.getPlayer().getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(playerId);

        final PermissionAttachment permissionAttachment =
            player.addAttachment(TinyEmpires.getDynmapPlugin());
        permissionAttachment.setPermission("dynmap.hide.self", false);
        permissionAttachment.setPermission("dynmap.hide.others", false);
        permissionAttachment.setPermission("dynmap.show.self", false);
        permissionAttachment.setPermission("dynmap.show.others", false);

        player.sendMessage(ChatColor.GREEN + "Welcome to the Pixel Empires server!");
        if (tePlayer == null) {
            final TEPlayer p = TEPlayer.createPlayer(player.getUniqueId(), player.getName());
            p.updatePlayerScoreboard();
            return;
        }
        tePlayer.updatePlayerScoreboard();

        // invisible players
        for (final UUID uuid : Invisible.getInvisiblePlayers()) {
            final Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                player.hidePlayer(TinyEmpires.getInstance(), p);
        }

        // debug for arenas not working properly
        player.setGlowing(false);
    }

}
