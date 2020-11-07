package dev.sucrose.tinyempires.models;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

public class FillPlane extends Plane {

    private static final World world;

    static {
        world = Bukkit.getWorld("world");
        if (world == null)
            throw new NullPointerException("Could not get world 'world' from Bukkit when loading FillPlane");
    }

    public FillPlane(int x1, int y1, int z1, int x2, int y2, int z2) {
        super(x1, y1, z1, x2, y2, z2);
    }

    public void fill(Material material) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    world.getBlockAt(x, y, z).setType(material);
                }
            }
        }
    }

}
