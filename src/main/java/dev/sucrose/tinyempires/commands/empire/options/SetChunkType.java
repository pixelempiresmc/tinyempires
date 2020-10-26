package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class SetChunkType implements EmpireCommandOption {

    private static final double templeAndTradingChunkCost = 10;
    private static final String types = Arrays.stream(ChunkType.values())
        .map(p -> p.name().toLowerCase())
        .collect(Collectors.joining("/"));

    @Override
    public void execute(Player sender, String[] args) {
        // /e type <type>
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

        if (!tePlayer.getPosition().hasPermission(Permission.CHUNKS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.CHUNKS));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + String.format(
                "/e type <%s>",
                types
            ));
            return;
        }

        String typeName = args[0];
        ChunkType type;
        try {
            type = ChunkType.valueOf(typeName.toUpperCase());
        } catch (Exception ignore) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a chunk type (%s)",
                typeName,
                types
            ));
            return;
        }

        // check if chunk is owned by empire
        TEChunk chunk = TEChunk.getChunk(sender.getLocation().getChunk());
        if (chunk == null
                || !chunk.getEmpire().getId().equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Chunk is %s and not your empire",
                chunk == null
                    ? "in the wilderness"
                    : "owned by " + chunk.getEmpire().getName()
            ));
            return;
        }

        final World world = sender.getLocation().getWorld();
        if (world == null) {
            sender.sendMessage(ErrorUtils.COULD_NOT_FETCH_WORLD);
            return;
        }

        if (type != ChunkType.NONE
                && empire.getReserve() < templeAndTradingChunkCost) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire needs %.1f more coins to make %s chunk (%.1f required, %.1f in reserve)",
                templeAndTradingChunkCost - empire.getReserve(),
                typeName,
                templeAndTradingChunkCost,
                empire.getReserve()
            ));
        }

        if (type == chunk.getType()) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Chunk is already of type '%s'",
                typeName
            ));
            return;
        }

        chunk.setType(type);
        DrawEmpire.setMarkerType(world.getName(), chunk.getX(), chunk.getZ(), type);

        empire.broadcast(ChatColor.GREEN, String.format(
            "%s%s set the chunk at %d, %d in the %s to be a %s chunk",
            sender.getName(),
            type != ChunkType.NONE
                ? String.format(" spent %.1f coins to", templeAndTradingChunkCost)
                : "",
            chunk.getWorldX(),
            chunk.getWorldZ(),
            StringUtils.worldDirToName(world.getName()),
            typeName
        ));
    }

}
