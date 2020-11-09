package dev.sucrose.tinyempires.models;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArenaPlayerEntry {

    final private ItemStack[] inventoryContents;
    final int experience;

    public ArenaPlayerEntry(ItemStack[] inventoryContents, int experience) {
        this.inventoryContents = inventoryContents;
        this.experience = experience;
    }

    public void restore(Player player) {
        player.getInventory().setContents(inventoryContents);
        player.updateInventory();
        player.setExp(0);
        player.setTotalExperience(experience);
    }

}
