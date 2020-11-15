package dev.sucrose.tinyempires.models;

public class LastChunkEntry {

    private final TEChunk chunk;
    private final String world;
    private final int x;
    private final int z;

    public LastChunkEntry(TEChunk chunk, String world, int x, int z) {
        this.chunk = chunk;
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public TEChunk getChunk() {
        return chunk;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

}
