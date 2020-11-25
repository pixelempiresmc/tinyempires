package dev.sucrose.tinyempires.models;

public class Bounds {

    private final String world;
    private final int leftX;
    private final int rightX;
    private final int bottomZ;
    private final int topZ;

    public Bounds(String world, int leftX, int rightX, int bottomZ, int topZ) {
        this.world = world;
        this.leftX = leftX;
        this.rightX = rightX;
        this.bottomZ = bottomZ;
        this.topZ = topZ;
    }

    public boolean inBounds(String world, int x, int z) {
        return this.world.equals(world)
            && x >= leftX
            && x <= rightX
            && z <= topZ
            && z >= bottomZ;
    }

}
