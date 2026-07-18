package io.github.twmeai.openbedwars.config;

import org.bukkit.Location;
import org.bukkit.World;

public record Position(double x, double y, double z, float yaw, float pitch) {
    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public BlockPosition block() {
        return new BlockPosition(floor(x), floor(y), floor(z));
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    public record BlockPosition(int x, int y, int z) {
    }
}
