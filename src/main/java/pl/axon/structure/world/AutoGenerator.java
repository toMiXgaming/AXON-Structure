package pl.axon.structure.world;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.Placement;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.protect.ProtectionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Losowo stawia struktury w nowo wygenerowanych chunkach, tylko tam, gdzie teren jest w miarę płaski i suchy.
 */
public final class AutoGenerator implements Listener {

    /**
     * @param enabled          czy auto-generowanie działa
     * @param worlds           nazwy światów
     * @param chancePercent    szansa na próbę przy każdym nowym chunku
     * @param minDistance      minimalna odległość między strukturami
     * @param minSpawnDistance minimalna odległość od spawnu świata
     * @param maxHeightDiff    maks. różnica wysokości terenu pod strukturą (10. i 90. percentyl)
     * @param maxLiquidPercent maks. udział wody/lawy pod strukturą
     * @param weights          struktura → waga (pusta mapa = wszystkie po równo)
     */
    public record Settings(boolean enabled, Set<String> worlds, double chancePercent, int minDistance,
                           int minSpawnDistance, int maxHeightDiff, double maxLiquidPercent,
                           Map<String, Integer> weights) {
    }

    /** Wynik oceny terenu. */
    public record Site(boolean ok, int baseY, String reason) {
    }

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final PasteService paster;
    private final ProtectionService protection;
    private final Random random = new Random();
    private Settings settings;
    private boolean attemptInProgress;

    public AutoGenerator(JavaPlugin plugin, StructureLibrary library, InstanceStore store, PasteService paster,
                         ProtectionService protection) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.paster = paster;
        this.protection = protection;
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!settings.enabled() || !event.isNewChunk() || attemptInProgress || paster.isBusy()) {
            return;
        }
        World world = event.getWorld();
        if (!settings.worlds().contains(world.getName())) {
            return;
        }
        if (random.nextDouble() * 100.0 >= settings.chancePercent()) {
            return;
        }
        Blueprint bp = pickStructure();
        if (bp == null) {
            return;
        }
        Chunk chunk = event.getChunk();
        int centerX = (chunk.getX() << 4) + 8;
        int centerZ = (chunk.getZ() << 4) + 8;
        if (!farEnough(world, centerX, centerZ)) {
            return;
        }
        attemptInProgress = true;
        int steps = random.nextInt(4);
        tryPlace(world, bp, centerX, centerZ, steps, true, null, null, placed -> attemptInProgress = false);
    }

    /**
     * Próbuje postawić strukturę ze środkiem w (centerX, centerZ), ładując (i generując) potrzebne chunki
     * w tle, a potem oceniając teren.
     *
     * @param checkTerrain false = postaw nawet na nierównym terenie (komenda admina)
     * @param done         wywoływane z true/false na wątku serwera
     */
    public void tryPlace(World world, Blueprint bp, int centerX, int centerZ, int steps, boolean checkTerrain,
                         CommandSender feedback, java.util.UUID undoOwner, Consumer<Boolean> done) {
        Placement draft = Placement.anchored(world.getName(), centerX, 0, centerZ, bp.width() / 2, bp.depth() / 2, steps);
        // Sprawdzane przed ładowaniem chunków, żeby nie generować terenu na próżno.
        String plot = protection.plotConflict(world, Box.of(bp, draft));
        if (plot != null) {
            if (feedback != null) {
                feedback.sendMessage("§cZa blisko działki §e" + plot + "§c (wymagany odstęp: §e"
                        + protection.settings().plotDistance() + "§c kratek).");
            }
            done.accept(false);
            return;
        }
        int margin = paster.settings().smoothing();
        int[] a = draft.toWorld(-margin, 0, -margin);
        int[] b = draft.toWorld(bp.width() - 1 + margin, 0, bp.depth() - 1 + margin);
        int minCx = Math.min(a[0], b[0]) >> 4;
        int maxCx = Math.max(a[0], b[0]) >> 4;
        int minCz = Math.min(a[2], b[2]) >> 4;
        int maxCz = Math.max(a[2], b[2]) >> 4;
        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                futures.add(world.getChunkAtAsync(cx, cz));
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).whenComplete((ignored, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        plugin.getLogger().warning("Nie udało się załadować terenu pod strukturę: " + error);
                        done.accept(false);
                        return;
                    }
                    Site site = evaluate(world, bp, draft);
                    if (checkTerrain && !site.ok()) {
                        if (feedback != null) {
                            feedback.sendMessage("§cTeren się nie nadaje: " + site.reason());
                        }
                        done.accept(false);
                        return;
                    }
                    Placement placement = new Placement(draft.world(), draft.originX(), site.baseY() - bp.ground(),
                            draft.originZ(), steps);
                    boolean started = paster.paste(bp, placement, feedback, undoOwner, feedback == null, instance -> {
                        if (instance == null) {
                            done.accept(false);
                            return;
                        }
                        if (feedback == null) {
                            plugin.getLogger().info("Auto-generowanie: postawiono '" + bp.id() + "' na "
                                    + instance.centerX() + " " + site.baseY() + " " + instance.centerZ()
                                    + " (" + world.getName() + ")");
                        }
                        done.accept(true);
                    });
                    if (!started) {
                        done.accept(false);
                    }
                }));
    }

    /** Ocenia teren pod strukturą: woda, nierówności i wysokość, na której ją postawić (mediana). */
    public Site evaluate(World world, Blueprint bp, Placement draft) {
        List<Integer> heights = new ArrayList<>();
        int liquid = 0;
        int samples = 0;
        for (int lx = 0; lx < bp.width(); lx += 2) {
            for (int lz = 0; lz < bp.depth(); lz += 2) {
                int[] w = draft.toWorld(lx, 0, lz);
                Terrain.Column column = Terrain.scan(world, w[0], w[2]);
                samples++;
                if (column.liquid()) {
                    liquid++;
                }
                heights.add(column.surfaceY());
            }
        }
        int[] sorted = heights.stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(sorted);
        int median = sorted[sorted.length / 2];
        int low = sorted[(int) (sorted.length * 0.1)];
        int high = sorted[Math.min(sorted.length - 1, (int) (sorted.length * 0.9))];
        double liquidPercent = samples == 0 ? 0 : liquid * 100.0 / samples;
        if (liquidPercent > settings.maxLiquidPercent()) {
            return new Site(false, median, String.format(Locale.ROOT, "za dużo wody (%.0f%%)", liquidPercent));
        }
        if (high - low > settings.maxHeightDiff()) {
            return new Site(false, median, "za duża różnica wysokości (" + (high - low) + " bloków)");
        }
        return new Site(true, median, "");
    }

    private boolean farEnough(World world, int x, int z) {
        Location spawn = world.getSpawnLocation();
        double dxs = spawn.getX() - x;
        double dzs = spawn.getZ() - z;
        double minSpawn = settings.minSpawnDistance();
        if (dxs * dxs + dzs * dzs < minSpawn * minSpawn) {
            return false;
        }
        double min = (double) settings.minDistance() * settings.minDistance();
        for (StructureInstance instance : store.inWorld(world.getName())) {
            if (instance.distanceSquared(x, z) < min) {
                return false;
            }
        }
        return true;
    }

    private Blueprint pickStructure() {
        List<Blueprint> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (Blueprint bp : library.all()) {
            if (bp.safeZone()) {
                continue; // oaza stoi tylko tam, gdzie postawi ją admin
            }
            int weight = settings.weights().isEmpty()
                    ? 1
                    : settings.weights().getOrDefault(bp.id().toLowerCase(Locale.ROOT), 0);
            if (weight > 0) {
                candidates.add(bp);
                weights.add(weight);
                total += weight;
            }
        }
        if (total == 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return candidates.get(i);
            }
        }
        return null;
    }
}
