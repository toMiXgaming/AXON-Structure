package pl.axon.structure.protect;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.util.Map;

/**
 * Ochrona postawionych struktur: obszar, którego gracze nie mogą niszczyć ani rozbudowywać, oraz odstęp
 * od działek AxonPlot. Jeśli na serwerze jest WorldGuard, każda struktura dostaje też region
 * {@code struktura_<id>} (po nim AxonPlot rozpoznaje struktury, a flaga pvp włącza walkę).
 */
public final class ProtectionService {

    /**
     * @param enabled       czy blokować niszczenie i budowanie
     * @param margin        ile kratek wokół obrysu struktury też jest chronione
     * @param plotDistance  minimalny odstęp między chronionym obszarem a największym możliwym obszarem działki
     * @param plotRadius    promień działki na najwyższym poziomie (AxonPlot: levels.*.radius)
     * @param message       komunikat dla gracza
     * @param regionFlags   flagi WorldGuard ustawiane na regionie struktury
     */
    public record Settings(boolean enabled, int margin, int plotDistance, int plotRadius, String message,
                           Map<String, String> regionFlags) {
    }

    public static final String BYPASS_PERMISSION = "axonstructure.bypass";

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private WorldGuardHook worldGuard;
    private Settings settings;

    public ProtectionService(JavaPlugin plugin, StructureLibrary library, InstanceStore store) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    /** Podpina WorldGuarda, jeśli jest włączony. Wywoływane raz przy starcie. */
    public void hookWorldGuard() {
        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            worldGuard = new WorldGuardHook(plugin.getLogger());
            plugin.getLogger().info("WorldGuard wykryty: struktury dostają regiony struktura_<id>.");
        } else {
            plugin.getLogger().warning("Brak WorldGuarda: nie sprawdzam odległości od działek AxonPlot.");
        }
    }

    public boolean hasWorldGuard() {
        return worldGuard != null;
    }

    /** Chroniony obszar struktury (obrys + margines) albo null, gdy obrys nie jest znany. */
    public Box area(StructureInstance instance) {
        Box footprint = instance.footprint();
        return footprint == null ? null : footprint.expand(settings.margin());
    }

    /** Struktura, której chroniony obszar obejmuje ten blok, albo null. */
    public StructureInstance protectedAt(World world, int x, int z) {
        if (!settings.enabled()) {
            return null;
        }
        for (StructureInstance instance : store.inWorld(world.getName())) {
            Box area = area(instance);
            if (area != null && area.contains(x, z)) {
                return instance;
            }
        }
        return null;
    }

    public StructureInstance protectedAt(Block block) {
        return protectedAt(block.getWorld(), block.getX(), block.getZ());
    }

    public StructureInstance protectedAt(Location location) {
        World world = location.getWorld();
        return world == null ? null : protectedAt(world, location.getBlockX(), location.getBlockZ());
    }

    /**
     * Sprawdza, czy struktura o takim obrysie nie wypada za blisko działki.
     *
     * @return nazwa regionu działki, która przeszkadza, albo null
     */
    public String plotConflict(World world, Box footprint) {
        if (worldGuard == null) {
            return null;
        }
        return worldGuard.findPlotNear(world, footprint.expand(settings.margin()), settings.plotRadius(),
                settings.plotDistance());
    }

    public void onPlaced(StructureInstance instance) {
        if (worldGuard != null) {
            worldGuard.ensureRegion(instance, area(instance), settings.regionFlags());
        }
    }

    public void onRemoved(StructureInstance instance) {
        if (worldGuard != null) {
            worldGuard.removeRegion(instance);
        }
    }

    /**
     * Uzupełnia obrys starszych wpisów z dane.yml i odtwarza regiony (np. po zmianie marginesu albo flag).
     */
    public void syncAll() {
        boolean changed = false;
        for (StructureInstance instance : store.all()) {
            if (instance.footprint() == null) {
                Blueprint bp = library.get(instance.structureId());
                if (bp == null) {
                    plugin.getLogger().warning("Nie znam obrysu struktury " + instance.id() + " ('"
                            + instance.structureId() + "' nie ma w folderze struktury), więc nie jest chroniona.");
                    continue;
                }
                instance.footprint(Box.of(bp, instance.placement()));
                changed = true;
            }
            onPlaced(instance);
        }
        if (changed) {
            store.save();
        }
    }
}
