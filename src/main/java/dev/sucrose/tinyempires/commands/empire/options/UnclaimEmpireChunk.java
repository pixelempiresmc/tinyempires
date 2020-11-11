package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnclaimEmpireChunk implements CommandOption {

    @Override
    public void execute(Player sender, String[] args) {
        // /e unclaim
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
        final TEChunk teChunk = TEChunk.getChunk(worldName, chunk.getX(), chunk.getZ());
        // return if chunk is null or owned by a different empire
        if (teChunk == null
                || !teChunk.getEmpire().getId().equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + "Your empire must own this chunk to unclaim it");
            return;
        }

        TEChunk.deleteChunk(teChunk);
        DrawEmpire.removeChunk(teChunk, empire);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s unclaimed a chunk at %d, %d in the %s",
            sender.getName(),
            chunk.getX() * 16,
            chunk.getZ() * 16,
            StringUtils.worldDirToName(teChunk.getWorld())
        ));
    }

}
