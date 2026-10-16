package pl.axon.structure.loot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Placement;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** Losuje i odświeża skrzynie w postawionych strukturach. */
public final class CrateService {

    public record Settings(long intervalMillis, int playerBlockRadius, boolean replaceFull) {
    }

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final Random random = new Random();
    private final Set<String> warnedPools = new HashSet<>();
    private final Set<Material> decorationTypes = new HashSet<>();
    private LootConfig loot;
    private Settings settings;
    private BukkitTask task;
    private String dropPool = "";

    public CrateService(JavaPlugin plugin, StructureLibrary library, InstanceStore store) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
    }

    public void configure(LootConfig loot, Settings settings) {
        this.loot = loot;
        this.settings = settings;
        warnedPools.clear();
        decorationTypes.clear();
        for (CrateClass crate : loot.classes().values()) {
            if (!crate.decoration().isBlank()) {
                try {
                    decorationTypes.add(Bukkit.createBlockData(crate.decoration()).getMaterial());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Zła dekoracja w klasie '" + crate.id() + "': " + crate.decoration());
                }
            }
        }
    }

    /** Pula oznaczająca miejsca zrzutów: tych punktów nie losujemy przy odświeżaniu, obsługuje je {@link AirdropService}. */
    public void dropPool(String pool) {
        this.dropPool = pool == null ? "" : pool;
    }

    public LootConfig loot() {
        return loot;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 30, 20L * 30);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (StructureInstance instance : store.all()) {
            if (now - instance.lastRefresh() < settings.intervalMillis()) {
                continue;
            }
            World world = Bukkit.getWorld(instance.world());
            if (world == null || !world.isChunkLoaded(instance.centerX() >> 4, instance.centerZ() >> 4)) {
                continue;
            }
            if (playerNearby(world, instance)) {
                continue;
            }
            if (refresh(instance, false) >= 0) {
                changed = true;
            }
        }
        if (changed) {
            store.save();
        }
    }

    private boolean playerNearby(World world, StructureInstance instance) {
        int radius = settings.playerBlockRadius();
        if (radius <= 0) {
            return false;
        }
        double limit = (double) radius * radius;
        for (Player player : world.getPlayers()) {
            Location l = player.getLocation();
            if (instance.distanceSquared(l.getX(), l.getZ()) < limit) {
                return true;
            }
        }
        return false;
    }

    /**
     * Losuje skrzynie od nowa.
     *
     * @param force też pełne skrzynie (np. zaraz po postawieniu albo z komendy)
     * @return liczba postawionych skrzyń albo -1, gdy struktura nie jest wczytana
     */
    public int refresh(StructureInstance instance, boolean force) {
        Blueprint bp = library.get(instance.structureId());
        World world = Bukkit.getWorld(instance.world());
        if (bp == null || world == null) {
            return -1;
        }
        Placement placement = instance.placement();
        int placed = 0;
        for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
            if (point.pool().equalsIgnoreCase(dropPool)) {
                continue;
            }
            int[] w = placement.toWorld(point.pos().x(), point.pos().y(), point.pos().z());
            if (!world.isChunkLoaded(w[0] >> 4, w[2] >> 4)) {
                continue;
            }
            String facing = placement.rotateFacing(point.facing());
            boolean ok = point.kind() == Blueprint.SpawnKind.FRAME
                    ? refreshFrame(world, w, facing, point.pool(), force)
                    : refreshCrate(world, w, facing, point.pool(), force);
            if (ok) {
                placed++;
            }
        }
        instance.lastRefresh(System.currentTimeMillis());
        return placed;
    }

    /** Usuwa skrzynie i ramki struktury (przy cofaniu). */
    public void clear(StructureInstance instance) {
        Blueprint bp = library.get(instance.structureId());
        World world = Bukkit.getWorld(instance.world());
        if (bp == null || world == null) {
            return;
        }
        for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
            if (point.pool().equalsIgnoreCase(dropPool)) {
                continue;
            }
            int[] w = instance.placement().toWorld(point.pos().x(), point.pos().y(), point.pos().z());
            if (point.kind() == Blueprint.SpawnKind.FRAME) {
                removeFrames(world, w);
            } else {
                Block block = world.getBlockAt(w[0], w[1], w[2]);
                if (isCrateBlock(block.getType())) {
                    removeCrate(block);
                }
            }
        }
    }

    private CrateClass pick(String poolId) {
        CratePool pool = loot.pool(poolId);
        if (pool == null) {
            if (warnedPools.add(poolId)) {
                plugin.getLogger().warning("Brak puli '" + poolId + "' w config.yml – to miejsce zostaje puste.");
            }
            return null;
        }
        String classId = pool.roll(random);
        return classId == null ? null : loot.crateClass(classId);
    }

    private boolean refreshCrate(World world, int[] w, String facing, String poolId, boolean force) {
        Block block = world.getBlockAt(w[0], w[1], w[2]);
        Material type = block.getType();
        if (isCrateBlock(type)) {
            if (!force && !settings.replaceFull() && block.getState() instanceof Container c && !isEmpty(c)) {
                return false;
            }
            removeCrate(block);
        } else if (!type.isAir()) {
            return false; // gracz coś tu postawił – nie ruszamy
        }
        CrateClass crate = pick(poolId);
        if (crate == null || crate.kind() == CrateClass.Kind.FRAME) {
            return false;
        }
        String horizontal = facing.equals("up") || facing.equals("down") ? "north" : facing;
        String state = crate.kind() == CrateClass.Kind.CHEST
                ? "minecraft:chest[facing=" + horizontal + ",type=single]"
                : "minecraft:barrel[facing=up,open=false]";
        block.setBlockData(Bukkit.createBlockData(state), false);
        BlockState blockState = block.getState();
        if (blockState instanceof Container container) {
            setName(container, crate.displayName());
            container.update(true, false);
        }
        if (block.getState() instanceof Container container) {
            container.getInventory().clear();
            crate.fill(container.getInventory(), random);
        }
        if (!crate.decoration().isBlank()) {
            Block above = block.getRelative(BlockFace.UP);
            if (above.getType().isAir()) {
                try {
                    BlockData decoration = Bukkit.createBlockData(crate.decoration());
                    above.setBlockData(decoration, false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Zła dekoracja w klasie '" + crate.id() + "': " + crate.decoration());
                }
            }
        }
        return true;
    }

    private boolean refreshFrame(World world, int[] w, String facing, String poolId, boolean force) {
        List<ItemFrame> frames = framesAt(world, w);
        if (!force && !settings.replaceFull()) {
            for (ItemFrame frame : frames) {
                if (frame.getItem() != null && !frame.getItem().getType().isAir()) {
                    return false; // przedmiot wciąż leży
                }
            }
        }
        removeFrames(world, w);
        CrateClass crate = pick(poolId);
        if (crate == null || crate.kind() != CrateClass.Kind.FRAME) {
            return false;
        }
        List<ItemStack> items = crate.roll(random);
        if (items.isEmpty()) {
            return false;
        }
        BlockFace face = BlockFace.valueOf(facing.toUpperCase(Locale.ROOT));
        Location location = new Location(world, w[0] + 0.5, w[1] + 0.5, w[2] + 0.5);
        try {
            world.spawn(location, ItemFrame.class, frame -> {
                frame.setFacingDirection(face, true);
                frame.setItem(items.get(0));
            });
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Nie udało się postawić ramki na " + w[0] + " " + w[1] + " " + w[2]
                    + ": " + e.getMessage());
            return false;
        }
    }

    private List<ItemFrame> framesAt(World world, int[] w) {
        Location center = new Location(world, w[0] + 0.5, w[1] + 0.5, w[2] + 0.5);
        List<ItemFrame> frames = new java.util.ArrayList<>();
        for (Entity entity : world.getNearbyEntities(center, 0.6, 0.6, 0.6)) {
            if (entity instanceof ItemFrame frame) {
                frames.add(frame);
            }
        }
        return frames;
    }

    private void removeFrames(World world, int[] w) {
        for (ItemFrame frame : framesAt(world, w)) {
            frame.remove();
        }
    }

    private void removeCrate(Block block) {
        if (block.getState() instanceof Container container) {
            container.getInventory().clear();
        }
        block.setBlockData(Bukkit.createBlockData(Material.AIR), false);
        Block above = block.getRelative(BlockFace.UP);
        if (decorationTypes.contains(above.getType())) {
            above.setBlockData(Bukkit.createBlockData(Material.AIR), false);
        }
    }

    private static boolean isCrateBlock(Material type) {
        return type == Material.CHEST || type == Material.BARREL;
    }

    private static boolean isEmpty(Container container) {
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private static void setName(Container container, String name) {
        container.setCustomName(name);
    }
}
