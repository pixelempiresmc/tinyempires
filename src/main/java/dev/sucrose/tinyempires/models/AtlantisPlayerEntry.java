package dev.sucrose.tinyempires.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AtlantisPlayerEntry extends ArenaPlayerEntry {

    private UUID tridentId;

    public AtlantisPlayerEntry(ItemStack[] inventoryContents, int experience) {
        super(inventoryContents, experience);
    }

    public void setTridentId(UUID tridentId) {
        this.tridentId = tridentId;
    }

    public void deleteTridentIfExists() {
        if (tridentId == null)
            return;
        final Entity trident = Bukkit.getEntity(tridentId);
        // if trident exists delete so player can't get free trident after match
        if (trident != null)
            trident.remove();
    }

}
