package dev.sucrose.tinyempires.models;

public class BoundsPlane extends Plane {

    public BoundsPlane(int x1, int y1, int z1, int x2, int y2, int z2) {
        super(x1, y1, z1, x2, y2, z2);
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
