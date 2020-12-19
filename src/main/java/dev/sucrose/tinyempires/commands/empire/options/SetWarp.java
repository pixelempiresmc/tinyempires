package dev.sucrose.tinyempires.commands.empire.options;

import dev.sucrose.tinyempires.models.*;
import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import dev.sucrose.tinyempires.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.stream.Collectors;

public class SetWarp implements CommandOption {

    private static final double SET_WARP_COST = 50;

    @Override
    public void execute(Player sender, String[] args) {
        // sets warp location
        // /e setwarp <name>
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

        if (!tePlayer.hasPermission(getPermissionRequired())) {
            sender.sendMessage(ErrorUtils.generatePermissionError(getPermissionRequired()));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + getUsage());
            return;
        }

        final String name = StringUtils.buildWordsFromArray(args, 0);

        if (name.length() > 30) {
            sender.sendMessage(ChatColor.RED + "You cannot have a warp name longer than 30 characters");
            return;
        }

        final Location senderLocation = sender.getLocation();
        final World world = senderLocation.getWorld();
        if (world == null) {
            sender.sendMessage(ErrorUtils.COULD_NOT_FETCH_WORLD);
            return;
        }

        final TEChunk chunk = TEChunk.getChunk(senderLocation.getChunk());
        if (chunk == null
                || !chunk.getEmpire().getId().equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + "Location must be owned by empire to set home");
            return;
        }

        if (empire.getReserve() < SET_WARP_COST) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%.1f more funds needed to move the empire home. (%.1f needed, %.1f in reserve)",
                SET_WARP_COST - empire.getReserve(),
                SET_WARP_COST,
                empire.getReserve()
            ));
            return;
        }

        // checks passed, set location and send message
        final Location originalWarp = empire.getWarpLocation(name);
        if (originalWarp == null
                || originalWarp.getWorld() == null) {
            empire.broadcastText(ChatColor.GREEN + String.format(
                "%s set the empire warp %s location to %d, %d, %d in the %s",
                ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
                ChatColor.GOLD + name + ChatColor.GREEN,
                senderLocation.getBlockX(),
                senderLocation.getBlockY(),
                senderLocation.getBlockZ(),
                StringUtils.worldDirToName(senderLocation.getWorld().getName())
            ));
        } else {
            empire.broadcastText(ChatColor.GREEN + String.format(
                "%s changed the empire warp %s location from %d, %d, %d in the %s to %d, %d, %d in the %s",
                ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
                ChatColor.GOLD + name + ChatColor.GREEN,
                originalWarp.getBlockX(),
                originalWarp.getBlockY(),
                originalWarp.getBlockZ(),
                StringUtils.worldDirToName(originalWarp.getWorld().getName()),
                senderLocation.getBlockX(),
                senderLocation.getBlockY(),
                senderLocation.getBlockZ(),
                StringUtils.worldDirToName(senderLocation.getWorld().getName())
            ));
        }
        empire.setWarpLocation(senderLocation, name);
        empire.takeReserveCoins(SET_WARP_COST);
    }

    @Override
    public String getDescription() {
        return "Set empire warp to current location";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.EDIT;
    }

    @Override
    public String getUsage() {
        return "/e setwarp <name>";
    }

}