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

public class SetHome implements CommandOption {

    private static final double MOVE_HOME_COST = 10;

    @Override
    public void execute(Player sender, String[] args) {
        // moves empire home location and marker to sender location
        // /e sethome
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

        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + getUsage());
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

        if (empire.getReserve() < MOVE_HOME_COST) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%.1f more funds needed to move the empire home. (%.1f needed, %.1f in reserve)",
                MOVE_HOME_COST - empire.getReserve(),
                MOVE_HOME_COST,
                empire.getReserve()
            ));
            return;
        }

        // checks passed, set location and send message
        final Location originalHome = empire.getHomeLocation();
        if (originalHome == null
                || originalHome.getWorld() == null) {
            empire.broadcastText(ChatColor.GREEN + String.format(
                "%s set the empire home location to %d, %d, %d in the %s",
                ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
                senderLocation.getBlockX(),
                senderLocation.getBlockY(),
                senderLocation.getBlockZ(),
                StringUtils.worldDirToName(senderLocation.getWorld().getName())
            ));
        } else {
            empire.broadcastText(ChatColor.GREEN + String.format(
                "%s changed the empire home location from %d, %d, %d in the %s to %d, %d, %d in the %s",
                ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
                originalHome.getBlockX(),
                originalHome.getBlockY(),
                originalHome.getBlockZ(),
                StringUtils.worldDirToName(originalHome.getWorld().getName()),
                senderLocation.getBlockX(),
                senderLocation.getBlockY(),
                senderLocation.getBlockZ(),
                StringUtils.worldDirToName(senderLocation.getWorld().getName())
            ));
        }
        empire.setHomeLocation(senderLocation);
        DrawEmpire.moveEmpireHomeMarker(
            empire,
            senderLocation.getWorld().getName(),
            senderLocation.getBlockX(),
            senderLocation.getBlockZ()
        );
        empire.takeReserveCoins(MOVE_HOME_COST);
    }

    @Override
    public String getDescription() {
        return "Set empire home to current location";
    }

    @Override
    public Permission getPermissionRequired() {
        return Permission.EDIT;
    }

    @Override
    public String getUsage() {
        return "/e sethome";
    }

}
