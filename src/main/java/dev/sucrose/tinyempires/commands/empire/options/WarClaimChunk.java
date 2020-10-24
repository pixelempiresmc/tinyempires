package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.TinyEmpires;
import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WarClaimChunk implements EmpireCommandOption {

    private static final Map<String, Integer> conquerTaskIds = new HashMap<>();

    @Override
    public void execute(Player sender, String[] args) {
        // /e warclaim
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + ErrorUtils.YOU_MUST_BE_IN_AN_EMPIRE);
            return;
        }

        // no permission check, any player can war claim

        if (args.length > 0) {
            sender.sendMessage(ChatColor.RED + "/e warclaim");
            return;
        }

        final Empire defender = empire.getAtWarWith();
        if (defender == null) {
            sender.sendMessage(ChatColor.RED + "Your empire must be at war to run this command");
            return;
        }

        final Chunk gameChunk = sender.getLocation().getChunk();
        final TEChunk chunk = TEChunk.getChunk(gameChunk);
        // check if chunk is owned by defender
        if (!chunk.getEmpire().getId().equals(defender.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "This chunk is not owned by who your empire is at war with (owned by %s)",
                defender.getName()
            ));
            return;
        }

        conquerTaskIds.put(
            chunk.toString(),
            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                TinyEmpires.getInstance(),
                new Runnable() {
                    // +1 because task runs instantly
                    private int timer = Empire.TIME_TO_CONQUER_CHUNK_SECONDS + 1;

                    @Override
                    public void run() {
                        final List<Player> playersInChunk = new ArrayList<>();
                        int defenders = 0;
                        int attackers = 0;
                        for (final Player player : Bukkit.getOnlinePlayers()) {
                            final Chunk pGameChunk = player.getLocation().getChunk();

                            // return if chunk is different
                            if (!pGameChunk.getWorld().getName().equals(gameChunk.getWorld().getName())
                                    || pGameChunk.getX() == gameChunk.getX()
                                    || pGameChunk.getZ() == gameChunk.getZ())
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
                            } else if (playerEmpire.getId().equals(empire.getId())) {
                                attackers++;
                            }
                        }

                        // no conflict left, send nothing to defenders
                        if (defenders == 0 && attackers == 0) {
                            Bukkit.getScheduler().cancelTask(conquerTaskIds.get(chunk.toString()));
                            return;
                        }

                        if (attackers > 0
                                && defenders == 0
                                && timer == 0) {
                            empire.broadcast(ChatColor.GREEN, String.format(
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
                                empire.getName()
                            ));
                            chunk.setEmpire(empire);
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
                },
            20,
            0
            )
        );
    }

}
