package dev.sucrose.tinyempires.models;

import org.bukkit.entity.Player;

public interface CommandOption {
    void execute(Player sender, String[] args);
}
