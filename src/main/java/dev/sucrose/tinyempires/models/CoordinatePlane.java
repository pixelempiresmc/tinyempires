package dev.sucrose.tinyempires.models;

public class CoordinatePlane {

    private final int x1;
    private final int y1;
    private final int z1;
    private final int x2;
    private final int y2;
    private final int z2;

    public CoordinatePlane(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    private boolean isInBounds(int min, int num, int max) {
        // inclusive
        return min <= num && num <= max;
    }

    public boolean isInPlane(int x, int y, int z) {
        return isInBounds(x1, x, x2)
            && isInBounds(y1, y, y2)
            && isInBounds(z1, z, z2);
    }

}
