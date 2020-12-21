package dev.sucrose.tinyempires.models;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

public class Warp {

	private boolean isPublic;
	private double cost;
	private String world;
	private double x;
	private double y;
	private double z;

	public Warp(String worldName, double x, double y, double z, double cost, boolean isPublic) {
		this.world = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		this.isPublic = isPublic;
	}

	public Document toDocument() {
		return new Document("world", world)
			.append("cost", cost)
			.append("public", isPublic)
			.append("x", x)
			.append("y", y)
			.append("z", z);
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Location getCoordinatesAsSpigotLocation() {
		final World worldObject = Bukkit.getWorld(world);
		if (worldObject == null)
			throw new NullPointerException(ChatColor.RED +
				String.format(
					"Could not fetch world '%s' when getting Warp location as Spigot object",
					world
				)
			);

		return new Location(worldObject, x, y, z);
	}

	public void setLocation(String world, double x, double y, double z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}
}
