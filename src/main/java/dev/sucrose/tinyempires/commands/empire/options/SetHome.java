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

public class SetHome implements EmpireCommandOption {

    private static final double MOVE_HOME_COST = 50;

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

        if (!tePlayer.hasPermission(Permission.HOME)) {
            sender.sendMessage(ErrorUtils.generatePermissionError(Permission.HOME));
            return;
        }

        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "/e sethome");
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
                || !chunk.getId().equals(empire.getId())) {
            sender.sendMessage(ChatColor.RED + "Location must be owned by empire to set home");
            return;
        }

        final Location originalHome = empire.getHomeLocation();
        final World originalHomeWorld = originalHome.getWorld();
        if (originalHomeWorld == null) {
            sender.sendMessage(ChatColor.RED +
                "Could not fetch the world for the original home location. This is unexpected behavior. " +
                "Please contact a developer and we will attend to it promptly."
            );
            return;
        }

        if (empire.getReserve() <= MOVE_HOME_COST) {
            sender.sendMessage(ChatColor.RED + String.format(
                "%.1f more funds needed to move the empire home. (%.1f needed, %.1f in reserve)",
                MOVE_HOME_COST - empire.getReserve(),
                MOVE_HOME_COST,
                empire.getReserve()
            ));
            return;
        }

        // checks passed, set location and send message
        empire.broadcastText(ChatColor.GREEN + String.format(
            "%s set the home to %d, %d in %s from %d, %d in %s for %.1f coins",
            ChatColor.BOLD + sender.getName() + ChatColor.GREEN,
            senderLocation.getBlockX(),
            senderLocation.getBlockZ(),
            StringUtils.worldDirToName(world.getName()),
            originalHome.getBlockX(),
            originalHome.getBlockZ(),
            StringUtils.worldDirToName(originalHome.getWorld().getName()),
            MOVE_HOME_COST
        ));
        empire.setHomeLocation(senderLocation);
        DrawEmpire.moveEmpireHomeMarker(
            empire.getId(),
            senderLocation.getWorld().getName(),
            senderLocation.getBlockX(),
            senderLocation.getBlockZ()
        );
        empire.takeReserveCoins(MOVE_HOME_COST);
    }

}
