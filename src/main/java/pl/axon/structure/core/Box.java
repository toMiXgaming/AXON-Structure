package pl.axon.structure.core;

/** Prostokąt w poziomie (x/z, włącznie z krawędziami), np. obrys struktury albo działki. */
public record Box(int minX, int minZ, int maxX, int maxZ) {

    public Box {
        if (minX > maxX) {
            int t = minX;
            minX = maxX;
            maxX = t;
        }
        if (minZ > maxZ) {
            int t = minZ;
            minZ = maxZ;
            maxZ = t;
        }
    }

    /** Obrys struktury postawionej w danym miejscu (po obrocie). */
    public static Box of(Blueprint bp, Placement placement) {
        int[] a = placement.toWorld(0, 0, 0);
        int[] b = placement.toWorld(bp.width() - 1, 0, bp.depth() - 1);
        return new Box(a[0], a[2], b[0], b[2]);
    }

    public static Box around(int x, int z, int radius) {
        return new Box(x - radius, z - radius, x + radius, z + radius);
    }

    public Box expand(int amount) {
        return new Box(minX - amount, minZ - amount, maxX + amount, maxZ + amount);
    }

    public Box union(Box other) {
        return new Box(Math.min(minX, other.minX), Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX), Math.max(maxZ, other.maxZ));
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public int centerX() {
        return Math.floorDiv(minX + maxX, 2);
    }

    public int centerZ() {
        return Math.floorDiv(minZ + maxZ, 2);
    }

    /**
     * Ile wolnych bloków dzieli dwa prostokąty (po osiach, metryka Czebyszewa). 0 = stykają się
     * albo nachodzą na siebie.
     */
    public int gap(Box other) {
        int dx = Math.max(0, Math.max(other.minX - maxX, minX - other.maxX) - 1);
        int dz = Math.max(0, Math.max(other.minZ - maxZ, minZ - other.maxZ) - 1);
        return Math.max(dx, dz);
    }

    @Override
    public String toString() {
        return minX + " " + minZ + " .. " + maxX + " " + maxZ;
    }
}
