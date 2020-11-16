package dev.sucrose.tinyempires.models;

import dev.sucrose.tinyempires.utils.DrawEmpire;
import dev.sucrose.tinyempires.utils.ErrorUtils;
import org.bukkit.ChatColor;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.PolyLineMarker;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class ChunkMarker {

    private static final double OPACITY = 0.4;
    private static final double BORDER_OPACITY = 1.0;
    private static int drawIndex = 0;
    private final Map<Direction, PolyLineMarker> borders = new EnumMap<>(Direction.class);
    private final AreaMarker marker;
    private Empire empire;
    private final int x;
    private final int z;
    private final String world;
    private Marker icon = null;

    private static final Map<Color, int[]> chatColorToIntArray = new EnumMap<>(Color.class);

    static {
        chatColorToIntArray.put(Color.AQUA, new int[] { 23, 153, 181 });
        chatColorToIntArray.put(Color.BLACK, new int[] { 0, 0, 0 });
        chatColorToIntArray.put(Color.BLUE, new int[] { 0, 0, 255 });
        chatColorToIntArray.put(Color.DARK_AQUA, new int[] {57, 82, 79 });
        chatColorToIntArray.put(Color.DARK_BLUE, new int[] { 0, 0, 139 });
        chatColorToIntArray.put(Color.DARK_GRAY, new int[] { 169, 169, 169 });
        chatColorToIntArray.put(Color.DARK_GREEN, new int[] { 0, 100, 0 });
        chatColorToIntArray.put(Color.DARK_PURPLE, new int[] { 128, 0, 128 });
        chatColorToIntArray.put(Color.DARK_RED, new int[] { 139, 0, 0});
        chatColorToIntArray.put(Color.GOLD, new int[] { 255, 223, 0 });
        chatColorToIntArray.put(Color.GRAY, new int[] { 220, 220, 220 });
        chatColorToIntArray.put(Color.GREEN, new int[] { 0, 255, 0 });
        chatColorToIntArray.put(Color.LIGHT_PURPLE, new int[] { 177, 156, 217 });
        chatColorToIntArray.put(Color.RED, new int[] { 255, 0, 0 });
        chatColorToIntArray.put(Color.WHITE, new int[] { 255, 255, 255 });
        chatColorToIntArray.put(Color.YELLOW, new int[] { 255, 255, 0 });
    }

    private static String generateChunkDescription(Empire empire, int x, int z) {
        final TEPlayer owner = TEPlayer.getTEPlayer(empire.getOwner());
        if (owner == null)
            throw new NullPointerException(ErrorUtils.YOU_DO_NOT_EXIST_IN_THE_DATABASE);
        return String.format(
            "<div style=\"font-family:Verdana;line-height:1.5;\">"
            + "<h2 style=\"margin: 2px 0px;\">%s</h2>"
            + "%s<br />"
            + "Owned by %s<br />"
            + "Reserve: %.1f<br />"
            + "%s member%s <br />"
            + "%d, %d"
            + "</div>",
            empire.getName(),
            empire.getDescription() == null ? "<i>No description</i>" : empire.getDescription(),
            owner.getName(),
            empire.getReserve(),
            empire.getMembers().size(),
            empire.getMembers().size() > 1 ? "s" : "",
            x * 16 + 8,
            z * 16 + 8
        );
    }

    public ChunkMarker(Empire empire, String world, int x, int z) {
        this.empire = empire;
        this.world = world;
        this.x = x;
        this.z = z;
        marker = DrawEmpire.getMarkerSet().createAreaMarker(
            String.valueOf(drawIndex++),
            generateChunkDescription(empire, x, z),
            true,
            world,
            new double[] { x * 16, x * 16 + 16 },
            new double[] { z * 16, z * 16 + 16 },
            false
        );
        marker.setLineStyle(0, 0, 0);
        marker.setFillStyle(OPACITY, chatColorToHexInt(empire.getColor()));
    }

    private String generateBorderDescription(Empire empire) {
        return String.format(
            "%s border",
            empire.getName()
        );
    }

    private static int chatColorToHexInt(Color color) {
        int[] rgb = chatColorToIntArray.get(color);
        String hexString = String.format("%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
        return Integer.valueOf(hexString, 16);
    }

    public void makeBorder(Direction direction) {
        double[] xCoords = new double[2];
        double[] zCoords = new double[2];
        final int leftX    = x * 16;
        final int rightX   = leftX + 16;
        final int topZ     = z * 16;
        final int bottomZ  = topZ + 16;

        switch (direction) {
            case UP:
                xCoords = new double[] { leftX, rightX };
                zCoords = new double[] { topZ, topZ };
                break;
            case DOWN:
                xCoords = new double[] { leftX, rightX };
                zCoords = new double[] { bottomZ, bottomZ };
                break;
            case RIGHT:
                xCoords = new double[] { rightX, rightX };
                zCoords = new double[] { topZ, bottomZ };
                break;
            case LEFT:
                xCoords = new double[] { leftX, leftX };
                zCoords = new double[] { topZ, bottomZ };
                break;
        }

        final PolyLineMarker marker = DrawEmpire.getMarkerSet().createPolyLineMarker(
            String.valueOf(drawIndex++),
            generateBorderDescription(empire),
            true,
            world,
            xCoords,
            new double[] { 64, 64 }, // arbitrary Y coordinates
            zCoords,
            false
        );
        marker.setLineStyle(2, BORDER_OPACITY, chatColorToHexInt(empire.getColor()));
        borders.put(
            direction,
            marker
        );
    }

    public void removeBorder(Direction direction) {
        borders.get(direction).deleteMarker();
        borders.remove(direction);
    }

    public boolean hasBorder(Direction direction) {
        return borders.get(direction) != null;
    }

    public void deleteBorders() {
        borders.values().forEach(PolyLineMarker::deleteMarker);
    }

    public void erase() {
        marker.deleteMarker();
        deleteBorders();
    }

    public void updateColor() {
        final int colorAsInt = chatColorToHexInt(empire.getColor());
        marker.setFillStyle(OPACITY, colorAsInt);
        for (PolyLineMarker marker : borders.values())
            marker.setLineStyle(2, BORDER_OPACITY, colorAsInt);
    }

    public void updateDescription() {
        marker.setDescription(generateChunkDescription(empire, x, z));
    }

    public void setIcon(ChunkType type) {
        if (icon != null)
            icon.deleteMarker();
        // leave marker deleted
        if (type == ChunkType.NONE)
            return;
        icon = DrawEmpire.getMarkerSet().createMarker(
            "__chunkMarker" + world + " " + x + " " + z,
            type == ChunkType.TRADING ? "Trading" : "Temple",
            world,
            x * 16 + 8,
            64,
            z * 16 + 8,
            DrawEmpire.getMarkerAPI().getMarkerIcon(type == ChunkType.TRADING ? "chest" : "temple"),
            false
        );
    }

    public void setEmpire(Empire empire) {
        this.empire = empire;
        updateDescription();
        updateColor();
    }

}
