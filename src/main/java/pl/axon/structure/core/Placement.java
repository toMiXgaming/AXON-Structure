package pl.axon.structure.core;

/**
 * Położenie struktury w świecie: punkt (0, 0, 0) układu lokalnego i obrót o {@code steps} * 90°.
 * Warstwa y = 0 struktury (grunt) leży na wysokości {@code originY}.
 */
public record Placement(String world, int originX, int originY, int originZ, int steps) {

    public int[] toWorld(int lx, int ly, int lz) {
        int[] r = Rotation.rotate(lx, lz, steps);
        return new int[]{originX + r[0], originY + ly, originZ + r[1]};
    }

    public String rotateFacing(String facing) {
        return Rotation.rotateFacing(facing, steps);
    }

    /** Placement, w którym lokalny punkt (lx, lz) wypada na światowym (worldX, worldZ). */
    public static Placement anchored(String world, int worldX, int worldY, int worldZ, int lx, int lz, int steps) {
        int[] r = Rotation.rotate(lx, lz, steps);
        return new Placement(world, worldX - r[0], worldY, worldZ - r[1], steps);
    }

    public int[] center(Blueprint bp) {
        return toWorld(bp.width() / 2, 0, bp.depth() / 2);
    }
}
