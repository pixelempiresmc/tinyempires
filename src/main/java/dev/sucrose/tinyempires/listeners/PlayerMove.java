package dev.sucrose.tinyempires.listeners;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.commands.empire.options.AutoClaimEmpireChunk;
import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.BoundUtils;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class PlayerMove implements Listener {

    private static final Map<UUID, LastChunkEntry> playerToLastChunk = new HashMap<>();
    private static final Map<String, Integer> conquerTaskIds = new HashMap<>();
    private static final Map<String, WarClaimChunkTask> conquerTasks = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Chunk gameChunk = player.getLocation().getChunk();
        final TEChunk currentChunk = TEChunk.getChunk(gameChunk);
        final LastChunkEntry lastChunkEntry = playerToLastChunk.get(uuid);

        final ObjectId lastChunkEmpireId =
            lastChunkEntry == null
                ? null
                : lastChunkEntry.getChunk() == null
                ? null
                : lastChunkEntry.getChunk().getEmpire().getId();
        final ObjectId currentChunkEmpireId =
            currentChunk == null
                ? null
                : currentChunk.getEmpire().getId();
        final boolean empireIdsEqual = Objects.equals(lastChunkEmpireId, currentChunkEmpireId);
        // return if last and current chunks are equal
        if (lastChunkEntry != null
                && lastChunkEntry.getWorld().equals(gameChunk.getWorld().getName())
                && lastChunkEntry.getX() == gameChunk.getX()
                && lastChunkEntry.getZ() == gameChunk.getZ()
                && empireIdsEqual)
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
                    AutoClaimEmpireChunk.claimChunkForEmpire(
                        player.getName(),
                        gameChunk.getWorld().getName(),
                        gameChunk.getX(),
                        gameChunk.getZ(),
                        tePlayer.getEmpire()
                    );
                    return;
                }
            }
        }

        // automatic war claiming if in war and war has started
        final Empire empire = tePlayer.getEmpire();
        if (empire != null
                && !empire.isWaitingForWar()
                && empire.getAtWarWith() != null
                && empire.getAtWarWith().getId().equals(currentChunk == null ? null :
            currentChunk.getEmpire().getId())) {
            final Empire enemy = empire.getAtWarWith();
            // send leaving message
            if (lastChunkEntry != null
                    && lastChunkEntry.getChunk() != null
                    && conquerTaskIds.containsKey(lastChunkEntry.getChunk().toString())) {
                player.sendMessage(ChatColor.DARK_RED + String.format(
                    "Stopped contesting %s chunk at %d, %d in %s",
                    "" + enemy.getChatColor() + ChatColor.BOLD + enemy.getName(),
                    lastChunkEntry.getChunk().getWorldX(),
                    lastChunkEntry.getChunk().getWorldZ(),
                    StringUtils.worldDirToName(gameChunk.getWorld().getName())
                ));
            }

            if (!conquerTaskIds.containsKey(currentChunk.toString())) {
                // can't conquer unless on perimeter of defender territory
                if (currentChunk.isSurroundedByOppositeEmpireChunks()) {
                    if (lastChunkEntry != null
                            && lastChunkEntry.getChunk().isSurroundedByOppositeEmpireChunks())
                        player.sendMessage(ChatColor.YELLOW + "You cannot contest chunks surrounded by enemy" +
                            " territory");
                    return;
                }
                final Empire atWarWith = empire.getAtWarWith();
                atWarWith.broadcastText(ChatColor.RED + String.format(
                    "%s of attacker %s is contesting a chunk at %s, %s in the %s!",
                    ChatColor.BOLD + player.getName() + ChatColor.RED,
                    "" + atWarWith.getChatColor() + ChatColor.BOLD + atWarWith.getName() + ChatColor.RED,
                    currentChunk.getWorldX(),
                    currentChunk.getWorldZ(),
                    StringUtils.worldDirToName(currentChunk.getWorld())
                ));
                empire.broadcastText(ChatColor.GREEN + String.format(
                    "%s is contesting enemy chunk at %s, %s in the %s!",
                    ChatColor.BOLD + player.getName() + ChatColor.GREEN,
                    currentChunk.getWorldX(),
                    currentChunk.getWorldZ(),
                    StringUtils.worldDirToName(currentChunk.getWorld())
                ));

                final WarClaimChunkTask task = new WarClaimChunkTask(currentChunk, empire, enemy);
                conquerTaskIds.put(
                    currentChunk.toString(),
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(
                        TinyEmpires.getInstance(),
                        task,
                        0,
                        20
                    )
                );
                conquerTasks.put(currentChunk.toString(), task);
            }
        }

        playerToLastChunk.put(
            uuid,
            new LastChunkEntry(
                currentChunk,
                gameChunk.getWorld().getName(),
                gameChunk.getX(),
                gameChunk.getZ()
            )
        );

        if (!empireIdsEqual)
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
        conquerTasks.remove(chunk.toString());
    }

}
