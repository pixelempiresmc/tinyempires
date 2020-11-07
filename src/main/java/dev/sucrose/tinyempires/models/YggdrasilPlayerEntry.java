package dev.sucrose.tinyempires.models;

import org.bukkit.inventory.ItemStack;

public class YggdrasilPlayerEntry extends ArenaPlayerEntry {

    private YggdrasilTeam team;
    public YggdrasilPlayerEntry(YggdrasilTeam team, ArenaType arena, ItemStack[] inventoryContents, int experience) {
        super(arena, inventoryContents, experience);
        this.team = team;
    }

    public YggdrasilTeam getTeam() {
        return team;
    }

    public void setTeam(YggdrasilTeam team) {
        this.team = team;
    }
}
