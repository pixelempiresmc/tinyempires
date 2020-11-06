package dev.sucrose.tinyempires.models;

import dev.sucrose.tinyempires.utils.DrawEmpire;
import org.dynmap.markers.Marker;

public class Icon {

    final private Marker icon;

    public Icon(Empire empire, String name, String icon, String world, int x, int z) {
        this.icon = DrawEmpire.getMarkerSet().createMarker(
            "__icon_marker" + empire.getId().toString(),
            name,
            world,
            x,
            64,
            z,
            DrawEmpire.getMarkerAPI().getMarkerIcon(icon),
            false
        );
    }

    public void move(String world, int x, int z) {
        icon.setLocation(world, x, 64, z);
    }

    public void delete() {
        icon.deleteMarker();
    }

}
