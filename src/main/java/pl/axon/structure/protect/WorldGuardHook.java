package pl.axon.structure.protect;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.StructureInstance;

import java.util.Map;
import java.util.logging.Logger;

/** Jedyna klasa, która dotyka API WorldGuarda; ładowana tylko wtedy, gdy WorldGuard jest na serwerze. */
final class WorldGuardHook {

    static final String STRUCTURE_PREFIX = "struktura_";
    static final String PLOT_PREFIX = "dzialka_";
    private static final int PRIORITY = 10;

    private final Logger logger;
    private final java.util.Set<RegionManager> dirty = new java.util.HashSet<>();
    private boolean batch;

    WorldGuardHook(Logger logger) {
        this.logger = logger;
    }

    private RegionManager regions(World world) {
        return world == null ? null
                : WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    static String regionId(StructureInstance instance) {
        return STRUCTURE_PREFIX + instance.id().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Szuka działki (regionu {@code dzialka_*}), której największy możliwy obszar leży bliżej niż
     * {@code distance} kratek od chronionego obszaru struktury.
     */
    String findPlotNear(World world, Box area, int plotRadius, int distance) {
        RegionManager manager = regions(world);
        if (manager == null) {
            return null;
        }
        for (ProtectedRegion region : manager.getRegions().values()) {
            if (!region.getId().startsWith(PLOT_PREFIX)) {
                continue;
            }
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            Box current = new Box(min.x(), min.z(), max.x(), max.z());
            // Działka rośnie z poziomem, więc liczy się jej największy obszar, a nie obecny.
            Box plot = current.union(Box.around(current.centerX(), current.centerZ(), plotRadius));
            if (plot.gap(area) < distance) {
                return region.getId();
            }
        }
        return null;
    }

    void ensureRegion(StructureInstance instance, Box area, Map<String, String> flags) {
        if (area == null) {
            return;
        }
        World world = Bukkit.getWorld(instance.world());
        RegionManager manager = regions(world);
        if (manager == null) {
            return;
        }
        String id = regionId(instance);
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(id,
                BlockVector3.at(area.minX(), world.getMinHeight(), area.minZ()),
                BlockVector3.at(area.maxX(), world.getMaxHeight() - 1, area.maxZ()));
        ProtectedRegion old = manager.getRegion(id);
        if (old != null) {
            region.copyFrom(old);
        }
        region.setPriority(PRIORITY);
        for (Map.Entry<String, String> entry : flags.entrySet()) {
            Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(entry.getKey());
            if (flag == null) {
                logger.warning("Nieznana flaga WorldGuard w ochrona.flagi-worldguard: " + entry.getKey());
                continue;
            }
            setFlag(region, flag, entry.getValue());
        }
        manager.addRegion(region);
        if (batch) {
            dirty.add(manager);
        } else {
            save(manager);
        }
    }

    /** Przy synchronizacji wszystkich struktur: regiony zapisujemy raz na końcu, nie po każdej. */
    void beginBatch() {
        batch = true;
    }

    void endBatch() {
        batch = false;
        for (RegionManager manager : dirty) {
            save(manager);
        }
        dirty.clear();
    }

    private <T> void setFlag(ProtectedRegion region, Flag<T> flag, String input) {
        try {
            region.setFlag(flag, flag.parseInput(FlagContext.create().setInput(input).setObject("region", region).build()));
        } catch (Exception e) {
            logger.warning("Zła wartość flagi " + flag.getName() + ": '" + input + "' (" + e.getMessage() + ")");
        }
    }

    void removeRegion(StructureInstance instance) {
        RegionManager manager = regions(Bukkit.getWorld(instance.world()));
        if (manager != null && !manager.removeRegion(regionId(instance)).isEmpty()) {
            save(manager);
        }
    }

    private void save(RegionManager manager) {
        try {
            manager.saveChanges();
        } catch (Exception e) {
            logger.warning("Nie udało się zapisać regionów WorldGuard: " + e.getMessage());
        }
    }
}
