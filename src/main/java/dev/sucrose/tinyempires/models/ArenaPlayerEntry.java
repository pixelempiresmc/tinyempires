package dev.sucrose.tinyempires.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ArenaPlayerEntry {

    final private ItemStack[] inventoryContents;
    final int experience;
    final ArenaType arena;
    final int arenaSpawnLocationIndex;

    public ArenaPlayerEntry(ArenaType arena, ItemStack[] inventoryContents, int experience, int arenaSpawnLocationIndex) {
        this.arena = arena;
        this.inventoryContents = inventoryContents;
        this.experience = experience;
        this.arenaSpawnLocationIndex = arenaSpawnLocationIndex;
    }

    public ArenaType getArena() {
        return arena;
    }

    public int getArenaSpawnLocationIndex() {
        return arenaSpawnLocationIndex;
    }

    public void restore(Player player) {
        System.out.println("Restoring player inventory");
        player.getInventory().setContents(inventoryContents);
        System.out.println("Gave contents:" + Arrays.toString(inventoryContents));
        player.updateInventory();
    }

}
