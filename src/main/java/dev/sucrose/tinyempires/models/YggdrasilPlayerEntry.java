package dev.sucrose.tinyempires.models;

import org.bukkit.inventory.ItemStack;

public class YggdrasilPlayerEntry extends ArenaPlayerEntry {

    private YggdrasilTeam team;
    public YggdrasilPlayerEntry(YggdrasilTeam team, ItemStack[] inventoryContents, int experience) {
        super(inventoryContents, experience);
        this.team = team;
    }

    public YggdrasilTeam getTeam() {
        return team;
    }

    public void setTeam(YggdrasilTeam team) {
        this.team = team;
    }
}
