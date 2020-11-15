package dev.sucrose.tinyempires.models;

import org.bukkit.entity.Player;

public interface CommandOption {

    /**
     * Command execution handler
     * @param sender Player running command
     * @param args Arguments as string
     */
    void execute(Player sender, String[] args);

    /**
     * Get command description
     * @return Command description
     */
    String getDescription();

    /**
     * Get command usage (e.g. /e withdraw <amount>)
     * @return Command usage
     */
    String getUsage();

    /**
     * Get permission required to run command or null if no permission is required
     * @return Required permission
     */
    Permission getPermissionRequired();

}
