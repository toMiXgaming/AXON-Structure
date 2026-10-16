package pl.axon.structure.core;

/** Struktura postawiona w świecie, której skrzynie są odświeżane. */
public final class StructureInstance {

    private final String id;
    private final String structureId;
    private final Placement placement;
    private final int centerX;
    private final int centerZ;
    private final boolean automatic;
    private long lastRefresh;

    public StructureInstance(String id, String structureId, Placement placement, int centerX, int centerZ,
                             boolean automatic, long lastRefresh) {
        this.id = id;
        this.structureId = structureId;
        this.placement = placement;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.automatic = automatic;
        this.lastRefresh = lastRefresh;
    }

    public String id() {
        return id;
    }

    public String structureId() {
        return structureId;
    }

    public Placement placement() {
        return placement;
    }

    public String world() {
        return placement.world();
    }

    public int centerX() {
        return centerX;
    }

    public int centerZ() {
        return centerZ;
    }

    public boolean automatic() {
        return automatic;
    }

    public long lastRefresh() {
        return lastRefresh;
    }

    public void lastRefresh(long time) {
        this.lastRefresh = time;
    }

    public double distanceSquared(double x, double z) {
        double dx = centerX - x;
        double dz = centerZ - z;
        return dx * dx + dz * dz;
    }
}
