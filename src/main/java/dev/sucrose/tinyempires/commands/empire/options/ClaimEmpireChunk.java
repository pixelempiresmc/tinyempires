package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.BoundUtils;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ClaimEmpireChunk implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e claim
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

        // all checks passed, make empire
        final Location location = sender.getLocation();
        final Chunk chunk = location.getChunk();
        final World world = location.getWorld();
        if (world == null)
            throw new NullPointerException("World when getting player location is undefined");

        final String worldName = world.getName();
        final TEChunk currentTEChunk = TEChunk.getChunk(worldName, chunk.getX(), chunk.getZ());
        if (currentTEChunk != null) {
            if (currentTEChunk.getEmpire().getId().equals(empire.getId())) {
                sender.sendMessage(ChatColor.RED + "Your empire already owns this chunk");
            } else {
                sender.sendMessage(ChatColor.RED + "This chunk is already owned by another empire");
            }
            return;
        }

        if (BoundUtils.isChunkInBoundsOfSpecialTerritory(chunk)) {
            sender.sendMessage(ChatColor.RED + "You cannot claim this section of the map");
            return;
        }

        if (BoundUtils.InBoundsOfMainEndIsland(location.getWorld().getName(), location.getBlockX(),
            location.getBlockZ())) {
            sender.sendMessage(ChatColor.RED + "You cannot claim the Main End Island!");
            return;
        }

        if (empire.getReserve() < TEChunk.CHUNK_COST) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire lacks enough coins for a new chunk. (%.1f required, %.1f in reserve)",
                TEChunk.CHUNK_COST,
                empire.getReserve()
            ));
            return;
        }

        if (BoundUtils.isChunkInBoundsOfSpecialTerritory(chunk)) {
            sender.sendMessage(ChatColor.RED + "You cannot claim a chunk that's within a special map structure");
            return;
        }

        empire.takeReserveCoins(TEChunk.CHUNK_COST);
        TEChunk.createTEChunk(worldName, chunk.getX(), chunk.getZ(), empire);
        DrawEmpire.drawChunk(empire, worldName, chunk.getX(), chunk.getZ());
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s claimed a new chunk for %.1f coins at %d, %d",
            sender.getName(),
            TEChunk.CHUNK_COST,
            chunk.getX() * 16,
            chunk.getZ() * 16
        ));
    }

    @Override
    public String getDescription() {
        return "Claim wilderness chunk for empire";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.CHUNKS;
    }

    @Override
    public String getUsage() {
        return "/e claim";
    }

}
