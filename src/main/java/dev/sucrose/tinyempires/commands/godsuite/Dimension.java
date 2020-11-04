package dev.sucrose.tinyempires.commands.godsuite;

import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Dimension implements CommandExecutor {

    private static final Map<String, Location> worldNameToLocation = new HashMap<>();

    static {
        final World overworld = Bukkit.getWorld("world");
        if (overworld == null)
            throw new NullPointerException("ERROR: Unable to load world 'world' for Dimension command");
        worldNameToLocation.put("overworld", overworld.getSpawnLocation());

        final World end = Bukkit.getWorld("world_the_end");
        if (end == null)
            throw new NullPointerException("ERROR: Unable to load world 'world_the_end' for Dimension command");
        worldNameToLocation.put("end", end.getSpawnLocation());

        final World nether = Bukkit.getWorld("world_nether");
        if (nether == null)
            throw new NullPointerException("ERROR: Unable to load world 'world_the_nether' for Dimension command");
        worldNameToLocation.put("nether", nether.getSpawnLocation());

        final World chess = Bukkit.getWorld("chess");
        if (chess == null)
            throw new NullPointerException("ERROR: Unable to load world 'chess' for Dimension command");
        worldNameToLocation.put("chess", chess.getSpawnLocation());
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // /e dimension <dimension>
        if (!sender.isOp()) {
            sender.sendMessage(ErrorUtils.YOU_MUST_BE_IN_AN_EMPIRE);
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + String.format(
                "/e dimension (%s)",
                String.join("/", worldNameToLocation.keySet())
            ));
            return false;
        }

        final Player player = (Player) sender;
        final String dimension = args[0];

        if (worldNameToLocation.containsKey(dimension)) {
            player.teleport(worldNameToLocation.get(dimension));
            player.sendMessage(ChatColor.GREEN + String.format(
                "Teleported to world %s",
                ChatColor.BOLD + dimension
            ));
            return true;
        }

        sender.sendMessage(ChatColor.RED + String.format(
            "'%s' is not an existing world (%s)",
            dimension,
            String.join("/", worldNameToLocation.keySet())
        ));
        return false;
    }

}
