package dev.sucrose.tinyempires.models;

import org.bukkit.entity.Player;

public interface EmpireCommandOption {
    void execute(Player sender, String[] args);
}
