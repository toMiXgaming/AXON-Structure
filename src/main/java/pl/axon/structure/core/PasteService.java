package pl.axon.structure.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

/** Wkleja struktury do świata partiami (żeby nie zamrozić serwera) i pamięta, jak je cofnąć. */
public final class PasteService {

    private static final int BLOCKS_PER_TICK = 4000;
    private static final int MAX_FOUNDATION_DEPTH = 12;
    private static final int HISTORY_SIZE = 5;
    /** Odległość (w blokach) między graczem a frontem struktury. */
    private static final int FRONT_DISTANCE = 3;

    private record Op(int x, int y, int z, BlockData data) {
    }

    private record Snapshot(int x, int y, int z, BlockData data) {
    }

    private static final class PasteRecord {
        final String structureName;
        final World world;
        final List<Snapshot> snapshots = new ArrayList<>();
        final List<UUID> entities = new ArrayList<>();

        PasteRecord(String structureName, World world) {
            this.structureName = structureName;
            this.world = world;
        }
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Deque<PasteRecord>> history = new HashMap<>();
    private final Map<String, BlockData> dataCache = new HashMap<>();
    private boolean busy;

    public PasteService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isBusy() {
        return busy;
    }

    public void paste(Player player, StructureDefinition definition, long seed) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Random random = new Random(seed);
        Blueprint blueprint = definition.build(random);
        int steps = Rotation.stepsFromYaw(location.getYaw());
        int baseX = location.getBlockX();
        int baseY = location.getBlockY() - 1;
        int baseZ = location.getBlockZ();

        if (baseY - MAX_FOUNDATION_DEPTH < world.getMinHeight() || baseY + blueprint.height() > world.getMaxHeight()) {
            player.sendMessage("§cZa mało miejsca w pionie – zejdź niżej albo wejdź wyżej.");
            return;
        }

        int offsetX = -blueprint.width() / 2;
        Transform transform = (lx, ly, lz) -> {
            int[] r = Rotation.rotate(lx + offsetX, lz + FRONT_DISTANCE, steps);
            return new int[]{baseX + r[0], baseY + ly, baseZ + r[1]};
        };

        List<Op> ops = new ArrayList<>();
        addFoundation(world, blueprint, transform, ops);
        for (int y = 0; y < blueprint.height(); y++) {
            for (int x = 0; x < blueprint.width(); x++) {
                for (int z = 0; z < blueprint.depth(); z++) {
                    int[] w = transform.apply(x, y, z);
                    ops.add(new Op(w[0], w[1], w[2], resolve(Rotation.rotateState(blueprint.get(x, y, z), steps))));
                }
            }
        }

        busy = true;
        PasteRecord record = new PasteRecord(definition.displayName(), world);
        player.sendMessage("§7Wklejam §e" + definition.displayName() + "§7 (" + ops.size() + " bloków)...");

        new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                try {
                    int end = Math.min(index + BLOCKS_PER_TICK, ops.size());
                    for (; index < end; index++) {
                        Op op = ops.get(index);
                        Block block = world.getBlockAt(op.x(), op.y(), op.z());
                        BlockData current = block.getBlockData();
                        if (current.equals(op.data())) {
                            continue;
                        }
                        record.snapshots.add(new Snapshot(op.x(), op.y(), op.z(), current));
                        block.setBlockData(op.data(), false);
                    }
                    if (index >= ops.size()) {
                        cancel();
                        finish(player, blueprint, transform, steps, record, random);
                    }
                } catch (RuntimeException e) {
                    cancel();
                    busy = false;
                    pushHistory(player, record);
                    plugin.getLogger().log(Level.SEVERE, "Błąd podczas wklejania struktury", e);
                    player.sendMessage("§cWklejanie przerwane przez błąd. Użyj §e/struktury cofnij§c, żeby posprzątać.");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public boolean undo(Player player) {
        Deque<PasteRecord> records = history.get(player.getUniqueId());
        if (records == null || records.isEmpty()) {
            return false;
        }
        PasteRecord record = records.pop();
        busy = true;
        for (UUID id : record.entities) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        List<Snapshot> snapshots = record.snapshots;
        player.sendMessage("§7Cofam §e" + record.structureName + "§7 (" + snapshots.size() + " bloków)...");
        new BukkitRunnable() {
            private int index = snapshots.size() - 1;

            @Override
            public void run() {
                int end = Math.max(index - BLOCKS_PER_TICK, -1);
                for (; index > end; index--) {
                    Snapshot s = snapshots.get(index);
                    record.world.getBlockAt(s.x(), s.y(), s.z()).setBlockData(s.data(), false);
                }
                if (index < 0) {
                    cancel();
                    busy = false;
                    player.sendMessage("§aCofnięto §e" + record.structureName + "§a.");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    private void finish(Player player, Blueprint blueprint, Transform transform, int steps,
                        PasteRecord record, Random random) {
        try {
            World world = record.world;
            connectNeighbours(world, record);
            for (Blueprint.ContainerSpec spec : blueprint.containers()) {
                fillContainer(world, transform, spec, random);
            }
            for (Blueprint.SignSpec spec : blueprint.signs()) {
                writeSign(world, transform, spec);
            }
            for (Blueprint.ItemFrameSpec spec : blueprint.itemFrames()) {
                spawnItemFrame(world, transform, steps, spec, record);
            }
            int[] front = transform.apply(blueprint.width() / 2, 1, 0);
            player.sendMessage("§aGotowe! §e" + blueprint.width() + "×" + blueprint.depth() + "×" + blueprint.height()
                    + "§a, front przy §f" + front[0] + " " + front[1] + " " + front[2]
                    + "§a. Cofnięcie: §e/struktury cofnij");
        } finally {
            busy = false;
            pushHistory(player, record);
        }
    }

    private void pushHistory(Player player, PasteRecord record) {
        Deque<PasteRecord> records = history.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        records.push(record);
        while (records.size() > HISTORY_SIZE) {
            records.removeLast();
        }
    }

    /** Wypełnia pustą przestrzeń pod podłogą struktury, żeby nie wisiała w powietrzu na zboczu. */
    private void addFoundation(World world, Blueprint blueprint, Transform transform, List<Op> ops) {
        BlockData dirt = resolve("minecraft:dirt");
        BlockData stone = resolve("minecraft:stone");
        for (int x = 0; x < blueprint.width(); x++) {
            for (int z = 0; z < blueprint.depth(); z++) {
                int[] w = transform.apply(x, 0, z);
                for (int depth = 1; depth <= MAX_FOUNDATION_DEPTH; depth++) {
                    Block block = world.getBlockAt(w[0], w[1] - depth, w[2]);
                    if (block.getType().isSolid()) {
                        break;
                    }
                    ops.add(new Op(w[0], w[1] - depth, w[2], depth <= 3 ? dirt : stone));
                }
            }
        }
    }

    private BlockData resolve(String state) {
        return dataCache.computeIfAbsent(state, s -> {
            try {
                return Bukkit.createBlockData(s);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Nieprawidłowy stan bloku w strukturze: " + s);
                return Bukkit.createBlockData(Material.STONE);
            }
        });
    }

    // ------------------------------------------------------------------ łączenie szyb, płotów i murków

    private enum Family {PANE, FENCE, WALL}

    private static Family familyOf(Material material) {
        String name = material.name();
        if (name.endsWith("_PANE") || name.equals("IRON_BARS")) {
            return Family.PANE;
        }
        if (name.endsWith("_FENCE")) {
            return Family.FENCE;
        }
        if (name.endsWith("_WALL")) {
            return Family.WALL;
        }
        return null;
    }

    /**
     * Bloki stawiane są bez fizyki, więc szyby, kraty, płoty i murki trzeba połączyć z sąsiadami ręcznie.
     */
    private void connectNeighbours(World world, PasteRecord record) {
        BlockFace[] sides = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (Snapshot snapshot : record.snapshots) {
            Block block = world.getBlockAt(snapshot.x(), snapshot.y(), snapshot.z());
            Family family = familyOf(block.getType());
            if (family == null) {
                continue;
            }
            String base = block.getBlockData().getAsString();
            int bracket = base.indexOf('[');
            if (bracket >= 0) {
                base = base.substring(0, bracket);
            }
            StringBuilder state = new StringBuilder(base).append('[');
            for (BlockFace face : sides) {
                Block neighbour = block.getRelative(face);
                Material type = neighbour.getType();
                boolean connect = type.isOccluding() || familyOf(type) == family;
                String key = face.name().toLowerCase(Locale.ROOT);
                if (family == Family.WALL) {
                    state.append(key).append('=').append(connect ? "low" : "none").append(',');
                } else {
                    state.append(key).append('=').append(connect).append(',');
                }
            }
            if (family == Family.WALL) {
                state.append("up=true,");
            }
            state.setCharAt(state.length() - 1, ']');
            block.setBlockData(resolve(state.toString()), false);
        }
    }

    // ------------------------------------------------------------------ zawartość: loot, napisy, ramki

    @SuppressWarnings("deprecation")
    private void fillContainer(World world, Transform transform, Blueprint.ContainerSpec spec, Random random) {
        Blueprint.Pos p = spec.pos();
        int[] w = transform.apply(p.x(), p.y(), p.z());
        Block block = world.getBlockAt(w[0], w[1], w[2]);
        BlockState state = block.getState();
        if (!(state instanceof Container container)) {
            return;
        }
        container.setCustomName("§6" + spec.loot().displayName());
        container.update(true, false);
        BlockState refreshed = block.getState();
        if (refreshed instanceof Container filled) {
            filled.getInventory().clear();
            spec.loot().fill(filled.getInventory(), random);
        }
    }

    @SuppressWarnings("deprecation")
    private void writeSign(World world, Transform transform, Blueprint.SignSpec spec) {
        Blueprint.Pos p = spec.pos();
        int[] w = transform.apply(p.x(), p.y(), p.z());
        BlockState state = world.getBlockAt(w[0], w[1], w[2]).getState();
        if (!(state instanceof Sign sign)) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            sign.getSide(Side.FRONT).setLine(i, spec.lines()[i]);
        }
        sign.setWaxed(true);
        sign.update(true, false);
    }

    private void spawnItemFrame(World world, Transform transform, int steps, Blueprint.ItemFrameSpec spec,
                                PasteRecord record) {
        Blueprint.Pos p = spec.pos();
        int[] w = transform.apply(p.x(), p.y(), p.z());
        BlockFace facing = BlockFace.valueOf(Rotation.rotateFacing(spec.facing(), steps).toUpperCase(Locale.ROOT));
        Location location = new Location(world, w[0] + 0.5, w[1], w[2] + 0.5);
        try {
            ItemFrame frame = world.spawn(location, ItemFrame.class, f -> {
                f.setFacingDirection(facing, true);
                f.setItem(spec.item().clone());
            });
            record.entities.add(frame.getUniqueId());
        } catch (RuntimeException e) {
            plugin.getLogger().fine("Nie udało się postawić ramki: " + e.getMessage());
            if (spec.fallback() != null) {
                Blueprint.Pos f = spec.fallback().pos();
                int[] fw = transform.apply(f.x(), f.y(), f.z());
                if (world.getBlockAt(fw[0], fw[1], fw[2]).getState() instanceof Container container) {
                    container.getInventory().addItem(spec.item().clone());
                }
            }
        }
    }

    @FunctionalInterface
    private interface Transform {
        int[] apply(int x, int y, int z);
    }
}
