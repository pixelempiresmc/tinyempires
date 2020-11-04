package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// rely on listeners.PlayerMove for adding chunks on move
public class AutoClaimEmpireChunk implements EmpireCommandOption {

    private static final Set<UUID> autoclaimers = new HashSet<>();

    public static void claimChunkForEmpire(String claimer, String world, int x, int z, Empire empire) {
        double start = System.nanoTime();
        TEChunk.createTEChunk(world, x, z, empire);
        double end = System.nanoTime();
        System.out.printf("TEChunk#createTEChunk took %s milliseconds\n", (end - start) / 1000000);

        start = System.nanoTime();
        DrawEmpire.drawChunk(empire, world, x, z);
        end = System.nanoTime();
        System.out.printf("DrawEmpire#drawChunk took %s milliseconds\n", (end - start) / 1000000);

        start = System.nanoTime();
        empire.takeReserveCoins(TEChunk.CHUNK_COST);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s claimed a new chunk for %.1f coins at %d, %d",
            claimer,
            TEChunk.CHUNK_COST,
            x * 16,
            z * 16
        ));
        end = System.nanoTime();
        System.out.printf("empire#broadcast and #takeReserveCoins took %s milliseconds\n", (end - start) / 1000000);
    }

    public static boolean isAutoclaiming(UUID uuid) {
        return autoclaimers.contains(uuid);
    }

    public static void removeAutoclaimer(UUID uuid) {
        autoclaimers.remove(uuid);
    }

    @Override
    public void execute(Player sender, String[] args) {
        // /e autoclaim
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

        if (!tePlayer.hasPermission(Permission.CHUNKS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.CHUNKS));
            return;
        }

        // toggle
        if (autoclaimers.contains(senderUUID)) {
            autoclaimers.remove(senderUUID);
            sender.sendMessage(ChatColor.GREEN + "Stopped autoclaiming");
            return;
        }

        final Location location = sender.getLocation();
        final Chunk chunk = location.getChunk();
        final World world = location.getWorld();
        if (world == null)
            throw new NullPointerException("World when getting player location is undefined");

        if (empire.getReserve() < TEChunk.CHUNK_COST) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire lacks enough coins for a new chunk. (%.1f required, %.1f in reserve)",
                TEChunk.CHUNK_COST,
                empire.getReserve()
            ));
            return;
        }

        // add to autoclaimer set
        autoclaimers.add(senderUUID);
        sender.sendMessage(ChatColor.GREEN + "Started autoclaiming (/e autoclaim to stop)");

        // claim current chunk if available
        final String worldName = world.getName();
        if (TEChunk.getChunk(worldName, chunk.getX(), chunk.getZ()) == null)
            claimChunkForEmpire(sender.getName(), worldName, chunk.getX(), chunk.getZ(), empire);
    }

}
