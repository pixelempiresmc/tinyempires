package dev.sucrose.tinyempires.models;

import dev.sucrose.tinyempires.listeners.PlayerMove;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WarClaimChunkTaskOld implements Runnable {

    private int timer = Empire.TIME_TO_CONQUER_CHUNK_SECONDS;
    private final TEChunk chunk;
    private final Empire defender;
    private final Empire attacker;


    private final List<TEPlayer> attackers = new ArrayList<>();
    private final List<TEPlayer> defenders = new ArrayList<>();

    public WarClaimChunkTaskOld(TEChunk chunk, Empire attacker, Empire defender) {
        this.chunk = chunk;
        this.attacker = attacker;
        this.defender = defender;
    }

    private void broadcastPlayerList(List<TEPlayer> players, String message) {
        for (final TEPlayer player : attackers) {
            final Player p = Bukkit.getPlayer(player.getPlayerUUID());
            if (p != null)
                p.sendMessage(message);
        }
    }

    private void broadcastAttackers(String message) {
        broadcastPlayerList(attackers, message);
    }

    private void broadcastDefenders(String message) {
        broadcastPlayerList(defenders, message);
    }

    public void addAttacker(TEPlayer attacker) {
        attackers.add(attacker);
    }

    public void removeAttacker(TEPlayer attacker) {
        for (int i = 0; i < attackers.size(); i++) {
            if (attackers.get(i).getPlayerUUID().equals(attacker.getPlayerUUID())) {
                attackers.remove(i);
                break;
            }
        }

        String message = String.format(
            "Attacker %s was defeated! %d defender%s and %d attacker%s contesting...",
            attacker.getName(),
            defenders.size(),
            defenders.size() > 1 ? "s" : "",
            attackers.size(),
            attackers.size() > 1 ? "s" : ""
        );

        broadcastDefenders(ChatColor.GREEN + message);
        broadcastAttackers(ChatColor.RED + message);

        if (attackers.size() == 0) {
            message = "All attackers have been defeated! Chunk is no longer contested";
            broadcastDefenders(ChatColor.GREEN + message);
            broadcastAttackers(ChatColor.RED + message);
            PlayerMove.cancelChunkWarClaimTask(chunk);
        }
    }

    public void addDefender(TEPlayer defender) {
        defenders.add(defender);
    }

    public void addDefenders(TEPlayer defender) {
        for (int i = 0; i < defenders.size(); i++) {
            if (defenders.get(i).getPlayerUUID().equals(defender.getPlayerUUID())) {
                defenders.remove(i);
                return;
            }
        }
    }


    @Override
    public void run() {
        final List<Player> playersInChunk = new ArrayList<>();
        int defenders = 0, attackers = 0;
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Chunk pGameChunk = player.getLocation().getChunk();

            // return if chunk is different
            if (!pGameChunk.getWorld().getName().equals(chunk.getWorld())
                    || pGameChunk.getX() != chunk.getX()
                    || pGameChunk.getZ() != chunk.getZ())
                return;

            playersInChunk.add(player);
            final TEPlayer pTePlayer = TEPlayer.getTEPlayer(player.getUniqueId());
            if (pTePlayer == null) {
                player.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
                return;
            }

            final Empire playerEmpire = pTePlayer.getEmpire();
            if (playerEmpire == null)
                continue;

            // is a member of attacking empire
            if (playerEmpire.getId().equals(defender.getId())) {
                defenders++;
            } else if (playerEmpire.getId().equals(attacker.getId())) {
                attackers++;
            }
        }

        // no conflict left, cancel task
        if (attackers == 0) {
            PlayerMove.cancelChunkWarClaimTask(chunk);
            if (defenders == 0)
                return;
        }

        // attacker wins chunk
        if (attackers > 0
                && defenders == 0
                && timer == 0) {
            attacker.broadcast(ChatColor.GREEN, String.format(
                "Enemy chunk from %s at %d, %d conquered by %s!",
                defender.getName(),
                chunk.getWorldX(),
                chunk.getWorldZ(),
                playersInChunk
                    .stream()
                    .skip(1)
                    .map(HumanEntity::getName)
                    .collect(Collectors.joining(", "))
                    + (playersInChunk.size() > 1 ? "and" : "") + playersInChunk.get(0).getName()
            ));

            defender.broadcast(ChatColor.GREEN, String.format(
                "Lost chunk at %d, %d to the empire of %s!",
                chunk.getWorldX(),
                chunk.getWorldZ(),
                attacker.getName()
            ));
            chunk.setEmpire(attacker);
            PlayerMove.cancelChunkWarClaimTask(chunk);
            DrawEmpire.setEmpire(chunk.getWorld(), chunk.getX(), chunk.getZ(), attacker);
            return;
        }

        for (final Player player : playersInChunk) {
            // attackers gone but defenders still left
            if (attackers == 0) {
                player.sendMessage(ChatColor.GREEN +
                    "All attackers have been defeated! Chunk is no longer contested");
                continue;
            }

            if (defenders > 0) {
                player.sendMessage(ChatColor.DARK_RED + String.format(
                    "%d defender%s is contesting the chunk!",
                    defenders,
                    defenders > 1 ? "s" : ""
                ));
                continue;
            }

            // attackers are still contesting, no defenders
            player.sendMessage(ChatColor.GREEN + String.format(
                "%d member%s contesting the chunk, %d seconds left",
                attackers,
                attackers > 1 ? "s" : "",
                timer--
            ));
        }
    }

}
