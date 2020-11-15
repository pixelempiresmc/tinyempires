package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class SetChunkType implements CommandOption {

    private static final double TEMPLE_AND_TRADING_CHUNK_COST = 10;
    private static final String types = Arrays.stream(ChunkType.values())
        .map(p -> p.name().toLowerCase())
        .collect(Collectors.joining("/"));

    @Override
    public void execute(Player sender, String[] args) {
        // /e type <type>
        final UUID senderUUID = sender.getUniqueId();
        final TEPlayer tePlayer = TEPlayer.getTEPlayer(senderUUID);
        if (tePlayer == null) {
            sender.sendMessage(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
            return;
        }

        final Empire empire = tePlayer.getEmpire();
        if (empire == null) {
            sender.sendMessage(ErrorUtils.YOU_MUST_BE_IN_AN_EMPIRE);
            return;
        }

        if (!tePlayer.hasPermission(Permission.CHUNKS)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.CHUNKS));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String typeName = args[0];
        ChunkType type;
        try {
            type = ChunkType.valueOf(typeName.toUpperCase());
        } catch (Exception ignore) {
            sender.sendMessage(ChatColor.RED + String.format(
                "'%s' is not a valid chunk type (%s)",
                typeName,
                types
            ));
            return;
        }

        final TEChunk chunk = TEChunk.getChunk(sender.getLocation().getChunk());
        if (chunk == null
                || !chunk.getEmpire().getId().equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED
                + "You can only set the type of a chunk in your own empire");
            return;
        }

        if (chunk.getType().equals(type)) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Chunk type is already %s",
                ChatColor.BOLD + type.name()
            ));
            return;
        }

        if (empire.getReserve() < TEMPLE_AND_TRADING_CHUNK_COST) {
            sender.sendMessage(ChatColor.RED + String.format(
                "Empire lacks enough coins to change chunk type. (%.1f required, %.1f in reserve)",
                TEMPLE_AND_TRADING_CHUNK_COST,
                empire.getReserve()
            ));
            return;
        }

        chunk.setType(type);
        DrawEmpire.setMarkerType(chunk.getWorld(), chunk.getX(), chunk.getZ(), type);
        empire.broadcast(ChatColor.GREEN, String.format(
            "%s set the type of the chunk at %d, %d in %s to be %s",
            sender.getName(),
            chunk.getWorldX(),
            chunk.getWorldZ(),
            StringUtils.worldDirToName(chunk.getWorld()),
            ChatColor.BOLD + type.name().toLowerCase()
        ));
    }

    @Override
    public String getDescription() {
        return "Set current chunk to type";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.CHUNKS;
    }

    @Override
    public String getUsage() {
        return "/e type <type>";
    }

}
