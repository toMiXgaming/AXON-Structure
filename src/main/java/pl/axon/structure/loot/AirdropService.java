package pl.axon.structure.loot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Zrzuty zaopatrzenia: co losowy czas nad jedną z postawionych struktur (miejsca z pulą zrzutów w pliku .axs,
 * np. lądowisko jednostki i dach szpitala) spada na spadochronie skrzynia z rzadkim lootem. Serwer dostaje
 * ogłoszenie ze współrzędnymi, a nad skrzynią unosi się dym, dopóki ktoś jej nie opróżni albo nie minie jej czas.
 */
public final class AirdropService {

    public record Settings(boolean enabled, int minMinutes, int maxMinutes, int minPlayers, String pool,
                           int lifetimeMinutes, int fallHeight, int fallSeconds, String crateName,
                           String announce, String landed, String expired) {
    }

    private record Target(StructureInstance instance, Blueprint blueprint, World world, int x, int y, int z) {
    }

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final CrateService crates;
    private final Random random = new Random();
    private Settings settings;
    private BukkitTask task;
    private long nextDrop;

    // aktywny zrzut (jeden naraz)
    private Target active;
    private BlockDisplay crateDisplay;
    private BlockDisplay chuteDisplay;
    private BukkitTask fallTask;
    private long expiresAt;
    private boolean landed;

    public AirdropService(JavaPlugin plugin, StructureLibrary library, InstanceStore store, CrateService crates) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.crates = crates;
    }

    public void configure(Settings settings) {
        this.settings = settings;
        crates.dropPool(settings.pool());
        scheduleNext();
    }

    public Settings settings() {
        return settings;
    }

    public void start() {
        stopTimer();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 10, 20L * 2);
    }

    public void stop() {
        stopTimer();
        finish(false);
    }

    private void stopTimer() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void scheduleNext() {
        int min = Math.max(1, settings.minMinutes());
        int max = Math.max(min, settings.maxMinutes());
        nextDrop = System.currentTimeMillis() + (min + random.nextInt(max - min + 1)) * 60_000L;
    }

    /** Minuty do następnego zrzutu (do komendy). */
    public long minutesToNext() {
        return Math.max(0, (nextDrop - System.currentTimeMillis()) / 60_000L);
    }

    public boolean isActive() {
        return active != null;
    }

    private void tick() {
        if (active != null) {
            if (landed) {
                smoke();
                if (System.currentTimeMillis() >= expiresAt || crateEmptied()) {
                    finish(true);
                }
            }
            return;
        }
        if (!settings.enabled() || System.currentTimeMillis() < nextDrop) {
            return;
        }
        if (Bukkit.getOnlinePlayers().size() < settings.minPlayers()) {
            scheduleNext();
            return;
        }
        trigger(null);
    }

    /**
     * Zrzuca skrzynię teraz.
     *
     * @return opis wyniku do komendy
     */
    public String trigger(CommandSender sender) {
        if (active != null) {
            return "§cJeden zrzut już trwa (" + active.x() + " " + active.y() + " " + active.z() + ").";
        }
        scheduleNext();
        List<Target> targets = targets();
        if (targets.isEmpty()) {
            return "§cŻadna postawiona struktura nie ma miejsca na zrzut (pula §e" + settings.pool() + "§c).";
        }
        Target target = targets.get(random.nextInt(targets.size()));
        World world = target.world();
        world.getChunkAt(target.x() >> 4, target.z() >> 4); // wczytuje chunk, jeśli trzeba
        active = target;
        landed = false;
        broadcast(format(settings.announce(), target));
        plugin.getLogger().info("Zrzut: spada nad " + target.blueprint().id() + " na " + target.x() + " "
                + target.y() + " " + target.z());
        startFall(target);
        return "§aZrzut spada nad §e" + target.blueprint().name() + " §a(" + target.x() + " " + target.y() + " "
                + target.z() + ").";
    }

    private List<Target> targets() {
        List<Target> out = new ArrayList<>();
        for (StructureInstance instance : store.all()) {
            Blueprint bp = library.get(instance.structureId());
            World world = Bukkit.getWorld(instance.world());
            if (bp == null || world == null) {
                continue;
            }
            for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
                if (point.pool().equalsIgnoreCase(settings.pool())) {
                    int[] w = instance.placement().toWorld(point.pos().x(), point.pos().y(), point.pos().z());
                    out.add(new Target(instance, bp, world, w[0], w[1], w[2]));
                }
            }
        }
        return out;
    }

    private void startFall(Target target) {
        World world = target.world();
        int ticks = Math.max(20, settings.fallSeconds() * 20);
        double height = Math.max(8, settings.fallHeight());
        Location start = new Location(world, target.x(), target.y() + height, target.z());
        crateDisplay = world.spawn(start, BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData(Material.BARREL));
            d.setTeleportDuration(2);
            d.setPersistent(false);
        });
        chuteDisplay = world.spawn(start, BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData(Material.WHITE_WOOL));
            d.setTransformation(new Transformation(new Vector3f(-1.25f, 2.2f, -1.25f), new AxisAngle4f(),
                    new Vector3f(3.5f, 0.35f, 3.5f), new AxisAngle4f()));
            d.setTeleportDuration(2);
            d.setPersistent(false);
        });
        final int[] step = {0};
        fallTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            step[0] += 2;
            double t = Math.min(1.0, step[0] / (double) ticks);
            Location at = start.clone().subtract(0, height * t, 0);
            if (crateDisplay != null && crateDisplay.isValid()) {
                crateDisplay.teleport(at);
            }
            if (chuteDisplay != null && chuteDisplay.isValid()) {
                chuteDisplay.teleport(at);
            }
            world.spawnParticle(Particle.CLOUD, at.clone().add(0.5, 0, 0.5), 2, 0.2, 0.1, 0.2, 0.01);
            if (t >= 1.0) {
                land(target);
            }
        }, 2L, 2L);
    }

    private void land(Target target) {
        cancelFall();
        removeDisplays();
        World world = target.world();
        Block block = world.getBlockAt(target.x(), target.y(), target.z());
        if (!block.getType().isAir() && block.getType() != Material.CHEST && block.getType() != Material.BARREL) {
            plugin.getLogger().warning("Zrzut: miejsce " + target.x() + " " + target.y() + " " + target.z()
                    + " jest zajęte (" + block.getType() + "), skrzynia ląduje piętro wyżej.");
            block = block.getRelative(0, 1, 0);
        }
        block.setBlockData(Bukkit.createBlockData("minecraft:chest[facing=north,type=single]"), false);
        CrateClass crate = pick();
        if (block.getState() instanceof Container container) {
            setName(container, LootConfig.color(settings.crateName()));
            container.update(true, false);
        }
        if (block.getState() instanceof Container container) {
            container.getInventory().clear();
            if (crate != null) {
                crate.fill(container.getInventory(), random);
            }
        }
        active = new Target(target.instance(), target.blueprint(), world, block.getX(), block.getY(), block.getZ());
        landed = true;
        expiresAt = System.currentTimeMillis() + Math.max(1, settings.lifetimeMinutes()) * 60_000L;
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.EXPLOSION, center, 1);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.6f);
        broadcast(format(settings.landed(), active));
        plugin.getLogger().info("Zrzut wylądował na " + block.getX() + " " + block.getY() + " " + block.getZ());
    }

    private CrateClass pick() {
        CratePool pool = crates.loot().pool(settings.pool());
        if (pool == null) {
            plugin.getLogger().warning("Brak puli '" + settings.pool() + "' w config.yml – zrzut będzie pusty.");
            return null;
        }
        String classId = pool.roll(random);
        return classId == null ? null : crates.loot().crateClass(classId);
    }

    private void smoke() {
        Location top = new Location(active.world(), active.x() + 0.5, active.y() + 1.0, active.z() + 0.5);
        if (active.world().isChunkLoaded(active.x() >> 4, active.z() >> 4)) {
            active.world().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, top, 3, 0.15, 0.1, 0.15, 0.02);
        }
    }

    private boolean crateEmptied() {
        if (!active.world().isChunkLoaded(active.x() >> 4, active.z() >> 4)) {
            return false;
        }
        Block block = active.world().getBlockAt(active.x(), active.y(), active.z());
        if (!(block.getState() instanceof Container container)) {
            return true;
        }
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Kończy zrzut: zabiera skrzynię (jeśli jeszcze stoi) i sprząta animację. */
    private void finish(boolean announce) {
        cancelFall();
        removeDisplays();
        if (active != null && landed) {
            Block block = active.world().getBlockAt(active.x(), active.y(), active.z());
            boolean emptied = crateEmptied();
            if (block.getType() == Material.CHEST) {
                if (block.getState() instanceof Container container) {
                    container.getInventory().clear();
                }
                block.setBlockData(Bukkit.createBlockData(Material.AIR), false);
            }
            if (announce && !emptied) {
                broadcast(format(settings.expired(), active));
            }
        }
        active = null;
        landed = false;
    }

    private void cancelFall() {
        if (fallTask != null) {
            fallTask.cancel();
            fallTask = null;
        }
    }

    private void removeDisplays() {
        if (crateDisplay != null) {
            crateDisplay.remove();
            crateDisplay = null;
        }
        if (chuteDisplay != null) {
            chuteDisplay.remove();
            chuteDisplay = null;
        }
    }

    private String format(String text, Target target) {
        return LootConfig.color(text
                .replace("{nazwa}", target.blueprint().name())
                .replace("{x}", String.valueOf(target.x()))
                .replace("{y}", String.valueOf(target.y()))
                .replace("{z}", String.valueOf(target.z()))
                .replace("{minuty}", String.valueOf(settings.lifetimeMinutes())));
    }

    @SuppressWarnings("deprecation")
    private static void broadcast(String message) {
        if (!message.isBlank()) {
            Bukkit.broadcastMessage(message);
        }
    }

    @SuppressWarnings("deprecation")
    private static void setName(Container container, String name) {
        container.setCustomName(name);
    }
}
