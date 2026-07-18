package io.github.twmeai.openbedwars.config;

public record ArenaProtection(
        int spawnRadius,
        int itemShopRadius,
        int upgradeShopRadius,
        int generatorRadius
) {
    public ArenaProtection {
        if (spawnRadius < 0 || itemShopRadius < 0 || upgradeShopRadius < 0 || generatorRadius < 0) {
            throw new IllegalArgumentException("Arena protection radii cannot be negative");
        }
    }

    public boolean protectsSpawn(Position center, int x, int y, int z) {
        return contains(center, x, y, z, spawnRadius, spawnRadius, spawnRadius);
    }

    public boolean protectsItemShop(Position center, int x, int y, int z) {
        return contains(center, x, y, z, itemShopRadius, 1, 4);
    }

    public boolean protectsUpgradeShop(Position center, int x, int y, int z) {
        return contains(center, x, y, z, upgradeShopRadius, 1, 4);
    }

    public boolean protectsGenerator(Position center, int x, int y, int z) {
        return contains(center, x, y, z, generatorRadius, 2, 5);
    }

    private boolean contains(Position center, int x, int y, int z, int radius, int below, int above) {
        if (radius == 0) return false;
        Position.BlockPosition block = center.block();
        return Math.abs(x - block.x()) <= radius
                && Math.abs(z - block.z()) <= radius
                && y >= block.y() - below
                && y <= block.y() + above;
    }
}
