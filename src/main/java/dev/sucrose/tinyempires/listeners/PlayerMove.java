package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.commands.empire.options.AutoClaimEmpireChunk;
import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.BoundUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class PlayerMove implements Listener {

    private static final Map<UUID, TEChunk> playerToLastChunk = new HashMap<>();
    private static final Map<String, Integer> conquerTaskIds = new HashMap<>();

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
        if (currentChunk != null
            && currentChunk.getEmpire().getId().equals(lastChunk == null
            ? null
            : lastChunk.getEmpire().getId()))
            return;
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(uuid);
        if (tePlayer == null)
            throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);

        tePlayer.updatePlayerScoreboard();
        if (currentChunk == null) {
            // autoclaiming
            if (AutoClaimEmpireChunk.isAutoclaiming(uuid)) {
                System.out.println("Player walked into new chunk");
                if (tePlayer.getEmpire().getReserve() < TEChunk.CHUNK_COST) {
                    AutoClaimEmpireChunk.removeAutoclaimer(uuid);
                    player.sendMessage(ChatColor.RED + String.format(
                        "Empire lacks enough coins for a new chunk. (%.1f required, %.1f in reserve)",
                        TEChunk.CHUNK_COST,
                        tePlayer.getEmpire().getReserve()
                    ));
                } else if (!BoundUtils.isChunkInBoundsOfSpecialTerritory(gameChunk)) {
                    AutoClaimEmpireChunk.claimChunkForEmpire(player.getName(), lastChunk.getWorld(), gameChunk.getX(),
                        gameChunk.getZ(), tePlayer.getEmpire());
                    return;
                }
            }
        }

        // automatic war claiming
        final Empire empire = tePlayer.getEmpire();
        if (empire.getAtWarWith() != null) {
            final Empire enemy = empire.getAtWarWith();
            // send leaving message
            if (lastChunk != null
                    && conquerTaskIds.containsKey(lastChunk.toString())) {
                player.sendMessage(ChatColor.DARK_RED + String.format(
                    "Stopped contesting %s chunk at %d, %d in %s",
                    "" + enemy.getChatColor() + ChatColor.BOLD + enemy.getName(),
                    lastChunk.getWorldX(),
                    lastChunk.getWorldZ(),
                    StringUtils.worldDirToName(lastChunk.getWorld())
                ));
            }

            if (currentChunk != null
                    && !conquerTaskIds.containsKey(currentChunk.toString())) {
                // can't conquer unless on perimeter of defender territory
                if (currentChunk.isAdjacentChunkTheSameEmpire(Direction.UP)
                        && currentChunk.isAdjacentChunkTheSameEmpire(Direction.DOWN)
                        && currentChunk.isAdjacentChunkTheSameEmpire(Direction.RIGHT)
                        && currentChunk.isAdjacentChunkTheSameEmpire(Direction.LEFT))
                    return;
                conquerTaskIds.put(
                    currentChunk.toString(),
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(
                        TinyEmpires.getInstance(),
                        new WarClaimChunkTaskOld(currentChunk, empire, enemy),
                        0,
                        20
                    )
                );
            }
        }

        playerToLastChunk.put(uuid, currentChunk);
        player.sendTitle(
            currentChunk == null
                ? ChatColor.BOLD
                    + "Wilderness"
                : ""
                    + currentChunk.getEmpire().getChatColor()
                    + ChatColor.BOLD
                    + currentChunk.getEmpire().getName(),
            "",
            10,
            70,
            20
        );
    }

    public static void cancelChunkWarClaimTask(TEChunk chunk) {
        final String key = chunk.toString();
        Bukkit.getScheduler().cancelTask(conquerTaskIds.get(key));
        conquerTaskIds.remove(key);
    }

}
