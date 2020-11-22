package dev.sucrose.tinyempires.models;

import dev.sucrose.tinyempires.listeners.PlayerMove;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.graalvm.compiler.core.phases.EconomyMidTier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WarClaimChunkTask implements Runnable {

    private int timer = Empire.TIME_TO_CONQUER_CHUNK_SECONDS;
    private final TEChunk chunk;
    private final Empire defender;
    private final Empire attacker;

    public WarClaimChunkTask(TEChunk chunk, Empire attacker, Empire defender) {
        this.chunk = chunk;
        this.attacker = attacker;
        this.defender = defender;
    }

    private void broadcastPlayerList(List<TEPlayer> players, String message) {
        for (final TEPlayer player : players) {
            final Player p = Bukkit.getPlayer(player.getPlayerUUID());
            if (p != null)
                p.sendMessage(message);
        }
    }

    private void broadcastToTwoPlayerLists(List<TEPlayer> players1, List<TEPlayer> players2, String message) {
        broadcastPlayerList(players1, message);
        broadcastPlayerList(players2, message);
    }

    @Override
    public void run() {
        final List<TEPlayer> defenders = new ArrayList<>();
        final List<TEPlayer> attackers = new ArrayList<>();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Chunk pGameChunk = player.getLocation().getChunk();

            // return if chunk is different
            if (!pGameChunk.getWorld().getName().equals(chunk.getWorld())
                    || pGameChunk.getX() != chunk.getX()
                    || pGameChunk.getZ() != chunk.getZ())
                continue;

            final TEPlayer pTePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
            if (pTePlayer == null) {
                player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
                continue;
            }

            final Empire playerEmpire = pTePlayer.getEmpire();
            if (playerEmpire == null)
                continue;

            final Empire atWarWith = playerEmpire.getAtWarWith();
            if (atWarWith == null)
                continue;

            if (playerEmpire.getId() == defender.getId()) {
                defenders.add(pTePlayer);
            } else if (playerEmpire.getId() == attacker.getId()) {
                attackers.add(pTePlayer);
            }
        }

        if (defenders.size() > 0) {
            broadcastToTwoPlayerLists(attackers, defenders, ChatColor.YELLOW + String.format(
                "Defender%s %s %s of %s contested the chunk, the claim attempt has been cancelled",
                defenders.size() > 1 ? "s" : "",
                ChatColor.BOLD + StringUtils.stringListToGrammaticalList(
                    defenders.stream().map(TEPlayer::getName).collect(Collectors.toList()),
                    ChatColor.YELLOW
                ) + ChatColor.YELLOW,
                defenders.size() > 1 ? "have" : "has",
                "" + defender.getChatColor() + ChatColor.BOLD + defender.getName() + ChatColor.YELLOW
            ));
            PlayerMove.cancelChunkWarClaimTask(chunk);
            return;
        }

        // no conflict left, cancel task
        if (attackers.size() == 0) {
            defender.broadcastText(ChatColor.GREEN + String.format(
                "Chunk at %d, %d in the %s has stopped being contested by the enemy",
                chunk.getWorldX(),
                chunk.getWorldZ(),
                StringUtils.worldDirToName(chunk.getWorld())
            ));
            attacker.broadcastText(ChatColor.RED + String.format(
                "Enemy chunk at %d, %d in the %s has stopped being contested by the empire",
                chunk.getWorldX(),
                chunk.getWorldZ(),
                StringUtils.worldDirToName(chunk.getWorld())
            ));
            PlayerMove.cancelChunkWarClaimTask(chunk);
            return;
        }

        // attacker wins chunk if previous checks are passed and timer is up
        if (timer == 0) {
            attacker.broadcast(ChatColor.GREEN, String.format(
                "Enemy chunk from %s at %d, %d conquered by %s!",
                "" + ChatColor.BOLD + defender.getChatColor() + defender.getName() + ChatColor.GREEN,
                chunk.getWorldX(),
                chunk.getWorldZ(),
                StringUtils.stringListToGrammaticalList(
                    attackers.stream().map(TEPlayer::getName).collect(Collectors.toList()),
                    ChatColor.GREEN
                )
            ));

            defender.broadcast(ChatColor.GREEN, String.format(
                "Lost chunk at %d, %d to the empire of %s!",
                chunk.getWorldX(),
                chunk.getWorldZ(),
                "" + ChatColor.BOLD + attacker.getChatColor() + attacker.getName() + ChatColor.GREEN
            ));
            chunk.setType(ChunkType.NONE);
            chunk.setEmpire(attacker);
            if (defender.getHomeLocation() != null) {
                final Chunk homeLocationChunk = defender.getHomeLocation().getChunk();
                if (homeLocationChunk.getWorld().getName().equals(chunk.getWorld())
                        && homeLocationChunk.getX() == chunk.getX()
                        && homeLocationChunk.getZ() == chunk.getZ()) {
                    defender.removeHomeLocation();
                    DrawEmpire.deleteEmpireHomeMarker(defender.getId());
                }
            }

            TEChest.removeChestMappingsInChunk(chunk);
            PlayerMove.cancelChunkWarClaimTask(chunk);
            DrawEmpire.setEmpire(chunk.getWorld(), chunk.getX(), chunk.getZ(), attacker);
            return;
        }

        broadcastPlayerList(attackers, ChatColor.GREEN + String.format(
            "%d second%s left to claim...",
            timer,
            timer > 1 ? "s" : ""
        ));
        timer--;
    }

}
