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
    // A farm is reset on the next 90-minute boundary after player activity.
    public java.util.List<String> farmNames = java.util.List.of();
    public java.util.List<String> previousFarmNames = java.util.List.of();
    public long nameRequest;
    public Boolean residentsPresent;
    public long renewalDue;
    public final java.util.Set<String> missingGuards = new java.util.HashSet<>();
    public final java.util.Map<String, org.bukkit.configuration.ConfigurationSection> farmContainers =
            new java.util.HashMap<>();

    public boolean farmCycle() {
        return scrapSite() || structureId.equals("gospodarstwo") || structureId.equals("kombinat");
    }

    public boolean scrapSite() {
        return structureId.equals("oilen") || structureId.equals("osiedle") || structureId.equals("zajezdnia");
    }

    public int groupSize() {
        return structureId.equals("zajezdnia") ? 3 : 2;
    }

    public void disturbed(long now) {
        if (farmCycle() && renewalDue == 0) {
            long interval = 90L * 60_000L;
            renewalDue = (Math.floorDiv(now, interval) + 1) * interval;
        }
    }

    private Box footprint;
    private String externalKey;

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

    /** Obrys struktury bez marginesu ochrony; null dla starych wpisów, dopóki nie zostanie wyliczony. */
    public Box footprint() {
        return footprint;
    }

    public void footprint(Box footprint) {
        this.footprint = footprint;
    }

    /** Klucz nadany przez pluginy zewnętrzne przy rejestracji (np. AxonMapGenerator); null dla ręcznych. */
    public String externalKey() {
        return externalKey;
    }

    public void externalKey(String externalKey) {
        this.externalKey = externalKey;
    }

    public double distanceSquared(double x, double z) {
        double dx = centerX - x;
        double dz = centerZ - z;
        return dx * dx + dz * dz;
    }

    /**
     * Kwadrat odległości od krawędzi obrysu (0 w środku). Dla dużych struktur to lepsza miara „gracz jest blisko”
     * niż odległość od środka; bez znanego obrysu – od środka.
     */
    public double edgeDistanceSquared(double x, double z) {
        if (footprint == null) {
            return distanceSquared(x, z);
        }
        double dx = Math.max(0, Math.max(footprint.minX() - x, x - (footprint.maxX() + 1)));
        double dz = Math.max(0, Math.max(footprint.minZ() - z, z - (footprint.maxZ() + 1)));
        return dx * dx + dz * dz;
    }
}
