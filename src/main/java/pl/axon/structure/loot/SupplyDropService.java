package pl.axon.structure.loot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.axon.structure.world.Terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Zrzuty zaopatrzenia: skrzynia-model spada w losowym punkcie w promieniu od środka mapy
 * (albo od gracza, gdy zrzut wzywa komenda), a na czacie leci ogłoszenie z namiarami.
 *
 * <p>Loot pochodzi z tabeli {@code zrzut} w AxonItems/loot.yml – najbogatszej tabeli w grze,
 * z progami {@code od-dnia}, więc pierwszy zrzut nie łamie progresji wipe'u.
 *
 * <p>Dwa tryby:
 * <ul>
 *   <li><b>automatyczny</b> – timer co {@code zrzuty.co-ile-minut}, punkt losowany wokół spawnu
 *       świata (promień {@code zrzuty.promien}),</li>
 *   <li><b>ręczny</b> – {@code /struktury zrzut [promień]}, punkt losowany wokół gracza.</li>
 * </ul>
 *
 * <p>Wszystkie operacje na blokach i encjach idą na głównym wątku (scheduler), a wybór
 * współrzędnych jest czystą matematyką z {@link DropMath}. Chunki są sprawdzane przed
 * dotknięciem terenu – brak synchronicznego ładowania chunków.
 */
public final class SupplyDropService {

    /** Identyfikator puli z config.yml, z której losujemy klasę zrzutu. */
    public static final String POOL = "zrzut";

    /** Nazwa tabeli lootu AxonItems, z której pochodzi zawartość zrzutu. */
    public static final String TABLE = "zrzut";

    public record Settings(boolean enabled, List<String> worlds, int intervalMinutes,
                           int radius, int minRadius, int announceRadius) {
    }

    private final JavaPlugin plugin;
    private final ModelCrateService models;
    private final Random random = new Random();
    private Settings settings;
    private BukkitTask task;
    private long lastDrop;

    public SupplyDropService(JavaPlugin plugin, ModelCrateService models) {
        this.plugin = plugin;
        this.models = models;
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    public void start() {
        stop();
        if (settings == null || !settings.enabled() || settings.intervalMinutes() <= 0) {
            return;
        }
        long periodTicks = 20L * 60L; // sprawdzamy raz na minutę, nie raz na interwał
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, periodTicks, periodTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (settings == null || !settings.enabled()) {
            return;
        }
        long interval = settings.intervalMinutes() * 60_000L;
        long now = System.currentTimeMillis();
        if (lastDrop == 0) {
            lastDrop = now; // pierwszy zrzut dopiero po pełnym interwale, nie od razu po starcie
            return;
        }
        if (now - lastDrop < interval) {
            return;
        }
        lastDrop = now;
        for (String worldName : settings.worlds()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                dropAtSpawn(world);
            }
        }
    }

    /** Zrzut automatyczny: losowy punkt w promieniu od spawnu świata. */
    public boolean dropAtSpawn(World world) {
        if (world == null) {
            return false;
        }
        Location spawn = world.getSpawnLocation();
        return drop(world, spawn.getBlockX(), spawn.getBlockZ(), settings == null ? 500 : settings.radius(),
                settings == null ? 120 : settings.minRadius(), "&e&l[Zrzut] &fZaopatrzenie spadło w okolicy!");
    }

    /**
     * Zrzut ręczny wokół gracza.
     *
     * @param player gracz wzywający zrzut (może być {@code null} – wtedy wokół spawnu świata)
     * @param radius promień losowania (0 = wartość z configu)
     */
    public boolean dropNear(Player player, int radius) {
        World world = player != null ? player.getWorld() : Bukkit.getWorlds().get(0);
        int cx;
        int cz;
        if (player != null) {
            cx = player.getLocation().getBlockX();
            cz = player.getLocation().getBlockZ();
        } else {
            cx = world.getSpawnLocation().getBlockX();
            cz = world.getSpawnLocation().getBlockZ();
        }
        int max = radius > 0 ? radius : (settings == null ? 200 : settings.radius());
        int min = settings == null ? 40 : Math.min(settings.minRadius(), max);
        return drop(world, cx, cz, max, min, "&e&l[Zrzut] &fZaopatrzenie spadło w okolicy!");
    }

    /**
     * Stawia zrzut w losowym punkcie wokół {@code (centerX, centerZ)}.
     *
     * @return {@code true}, gdy udało się postawić pojemnik
     */
    public boolean drop(World world, int centerX, int centerZ, int maxRadius, int minRadius, String message) {
        if (world == null) {
            return false;
        }
        CrateClass crate = pickCrate();
        if (crate == null) {
            plugin.getLogger().warning("Zrzut: brak klasy w puli '" + POOL + "' – pomijam.");
            return false;
        }
        String modelName = crate.modelName();
        if (modelName == null || modelName.isBlank()) {
            modelName = "zrzut";
        }
        // Szukamy miejsca, w którym chunk jest już wczytany – bez synchronicznego ładowania.
        int[] spot = findSpot(world, centerX, centerZ, maxRadius, minRadius);
        if (spot == null) {
            plugin.getLogger().info("Zrzut: brak wczytanego chunka w promieniu " + maxRadius
                    + " od " + centerX + " " + centerZ + " – pomijam.");
            return false;
        }
        int[] w = {spot[0], spot[1], spot[2]};
        models.forgetAt(world, w);
        boolean placed = models.place(world, w, modelName, "north", POOL, crate.zone());
        if (!placed) {
            return false;
        }
        announce(world, w, crate, message);
        return true;
    }

    /**
     * Szuka punktu z wczytanym chunkiem i gruntem nadający się na zrzut.
     *
     * @return {@code {x, y, z}} albo {@code null}
     */
    private int[] findSpot(World world, int centerX, int centerZ, int maxRadius, int minRadius) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int[] xz = DropMath.pick(random, centerX, centerZ, minRadius, maxRadius);
            int x = xz[0];
            int z = xz[1];
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            Terrain.Column column = Terrain.scan(world, x, z);
            if (column.liquid()) {
                continue; // nie zrzucamy do wody ani lawy
            }
            int y = column.surfaceY() + 1;
            if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 2) {
                continue;
            }
            if (!world.getBlockAt(x, y, z).getType().isAir()) {
                continue;
            }
            return new int[]{x, y, z};
        }
        return null;
    }

    private CrateClass pickCrate() {
        LootConfig loot = models.loot();
        if (loot == null) {
            return null;
        }
        CratePool pool = loot.pool(POOL);
        if (pool == null) {
            return null;
        }
        String classId = pool.roll(random);
        return classId == null ? null : loot.crateClass(classId);
    }

    private void announce(World world, int[] w, CrateClass crate, String message) {
        String base = message == null || message.isBlank()
                ? "&e&l[Zrzut] &fZaopatrzenie spadło w okolicy!"
                : message;
        String colored = LootConfig.color(base);
        String coords = LootConfig.color("&7Współrzędne: &f" + w[0] + " " + w[1] + " " + w[2]
                + " &7(" + crate.displayName() + "&7)");
        int radius = settings == null ? 0 : settings.announceRadius();
        for (Player player : world.getPlayers()) {
            if (radius > 0) {
                Location l = player.getLocation();
                long dx = (long) l.getBlockX() - w[0];
                long dz = (long) l.getBlockZ() - w[2];
                if (dx * dx + dz * dz > (long) radius * radius) {
                    continue;
                }
            }
            player.sendMessage(colored);
            player.sendMessage(coords);
        }
        plugin.getLogger().info("Zrzut postawiony: " + world.getName() + " " + w[0] + " " + w[1] + " " + w[2]
                + " (" + crate.id() + ")");
    }

    /** Lista postawionych zrzutów w świecie (diagnostyka). */
    public List<Location> list(World world) {
        List<Location> result = new ArrayList<>();
        if (world == null) {
            return result;
        }
        for (Location location : models.list(world)) {
            if (location.getBlock().getType() == Material.AIR) {
                result.add(location);
            }
        }
        return result;
    }
}
