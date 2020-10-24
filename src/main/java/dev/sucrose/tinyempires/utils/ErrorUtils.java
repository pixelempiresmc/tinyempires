package dev.sucrose.tinyempires.utils;

import dev.sucrose.tinyempires.models.Permission;
import org.bukkit.ChatColor;

public class ErrorUtils {

    public final static String YOU_DO_NOT_EXIST_IN_THE_DATABASE = ChatColor.RED + "ERROR: You do not exist in the " +
    "database. This is unexpected behavior. Please contact a developer and we will attend to it promptly.";
    public final static String INSUFFICIENT_PERMS = ChatColor.RED + "You do not have sufficient permissions to run " +
        "this command";
    public final static String YOU_MUST_BE_IN_AN_EMPIRE = ChatColor.RED + "You must be in an empire to run this " +
        "command";
    public final static String COULD_NOT_FETCH_WORLD = ChatColor.RED + "Could not fetch player world for chest " +
        "mapping. This is unexpected behavior. Please contact a developer and we will attend to it promptly.";

    public static String generatePermissionError(Permission permission) {
        return ChatColor.RED + String.format(
            "You do not have the %s permission required to run this command.",
            ChatColor.YELLOW + permission.name().toLowerCase() + ChatColor.RED
        );
    }

}
