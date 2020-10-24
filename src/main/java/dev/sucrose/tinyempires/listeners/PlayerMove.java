package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.commands.empire.options.AutoClaimEmpireChunk;
import dev.sucrose.tinyempires.models.TEChunk;
import dev.sucrose.tinyempires.models.TEPlayer;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayerMove implements Listener {

    private static final Map<UUID, TEChunk> playerToLastChunk = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Chunk gameChunk = player.getLocation().getChunk();
        final TEChunk currentChunk = TEChunk.getChunk(gameChunk);
        final TEChunk lastChunk = playerToLastChunk.get(uuid);

        // return if last and current chunks are equal
        if (currentChunk == null && lastChunk == null)
            return;
        if (currentChunk != null && currentChunk.getEmpire().getId().equals(lastChunk == null ?
            null : lastChunk.getEmpire().getId()))
            return;
        playerToLastChunk.put(uuid, currentChunk);
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
        if (tePlayer == null)
            throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);

        tePlayer.updatePlayerScoreboard();
        if (currentChunk == null) {
            // autoclaiming
            if (AutoClaimEmpireChunk.isAutoclaiming(uuid)) {
                if (tePlayer.getEmpire().getReserve() < TEChunk.CHUNK_COST) {
                    AutoClaimEmpireChunk.removeAutoclaimer(uuid);
                    player.sendMessage(ChatColor.RED + String.format(
                        "Empire lacks enough coins for a new chunk. (%.1f required, %.1f in reserve)",
                        TEChunk.CHUNK_COST,
                        tePlayer.getEmpire().getReserve()
                    ));
                }
                AutoClaimEmpireChunk.claimChunkForEmpire(player.getName(), lastChunk.getWorld(), gameChunk.getX(),
                    gameChunk.getZ(), tePlayer.getEmpire());
                return;
            }
            player.sendTitle(ChatColor.BOLD + "Wilderness", "", 10, 70, 20);
            return;
        }
        playerToLastChunk.put(uuid, currentChunk);
        player.sendTitle("" + currentChunk.getEmpire().getChatColor() + ChatColor.BOLD + currentChunk.getEmpire().getName()
            , "",
            10, 70, 20);
    }

}
