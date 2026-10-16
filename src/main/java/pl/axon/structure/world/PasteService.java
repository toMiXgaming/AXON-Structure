package pl.axon.structure.world;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Placement;
import pl.axon.structure.core.Rotation;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.loot.CrateService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Stawia struktury w świecie partiami (żeby nie zamrozić serwera), dopasowując teren wokół,
 * i pamięta, jak cofnąć ręczne wklejenia.
 */
public final class PasteService {

    /**
     * @param blocksPerTick ile bloków stawiać na tick
     * @param smoothing     szerokość pasa terenu wokół struktury, który łagodnie przechodzi w jej poziom
     * @param maxFoundation maks. głębokość dosypywania gruntu pod strukturą
     * @param clearAbove    ile bloków ponad strukturą wycinać (drzewa, pagórki)
     */
    public record Settings(int blocksPerTick, int smoothing, int maxFoundation, int clearAbove) {
    }

    private record Op(int x, int y, int z, BlockData data) {
    }

    private record Snapshot(int x, int y, int z, BlockData data) {
    }

    private static final class PasteRecord {
        final String structureName;
        final World world;
        final List<Snapshot> snapshots = new ArrayList<>();
        String instanceId;
        int cratesPlaced;

        PasteRecord(String structureName, World world) {
            this.structureName = structureName;
            this.world = world;
        }
    }

    private static final int HISTORY_SIZE = 5;

    private final JavaPlugin plugin;
    private final InstanceStore store;
    private final CrateService crates;
    private final Map<UUID, Deque<PasteRecord>> history = new HashMap<>();
    private final Map<String, BlockData> dataCache = new HashMap<>();
    private Settings settings;
    private boolean busy;

    public PasteService(JavaPlugin plugin, InstanceStore store, CrateService crates) {
        this.plugin = plugin;
        this.store = store;
        this.crates = crates;
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    public boolean isBusy() {
        return busy;
    }

    /**
     * Stawia strukturę.
     *
     * @param feedback   komu wysyłać komunikaty (może być null)
     * @param undoOwner  gracz, który może to cofnąć (null = bez historii, np. auto-generowanie)
     * @param automatic  czy struktura powstała z auto-generowania
     * @param onDone     wywoływane po postawieniu i wylosowaniu skrzyń (z null, gdy stawianie przerwał błąd)
     */
    public boolean paste(Blueprint bp, Placement placement, CommandSender feedback, UUID undoOwner,
                         boolean automatic, Consumer<StructureInstance> onDone) {
        World world = Bukkit.getWorld(placement.world());
        if (world == null || busy) {
            return false;
        }
        int baseY = placement.originY();
        if (baseY - settings.maxFoundation() < world.getMinHeight()
                || baseY + bp.height() + settings.clearAbove() > world.getMaxHeight()) {
            send(feedback, "§cZa mało miejsca w pionie na tej wysokości.");
            return false;
        }

        Map<Long, Op> ops = new LinkedHashMap<>();
        planTerrain(world, bp, placement, ops);
        int steps = placement.steps();
        for (int y = 0; y < bp.height(); y++) {
            for (int x = 0; x < bp.width(); x++) {
                for (int z = 0; z < bp.depth(); z++) {
                    int[] w = placement.toWorld(x, y, z);
                    put(ops, w[0], w[1], w[2], resolve(Rotation.rotateState(bp.get(x, y, z), steps)));
                }
            }
        }
        List<Op> list = new ArrayList<>(ops.values());

        busy = true;
        PasteRecord record = new PasteRecord(bp.name(), world);
        send(feedback, "§7Stawiam §e" + bp.name() + "§7 (" + list.size() + " bloków)...");

        new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                try {
                    int end = Math.min(index + settings.blocksPerTick(), list.size());
                    for (; index < end; index++) {
                        Op op = list.get(index);
                        Block block = world.getBlockAt(op.x(), op.y(), op.z());
                        BlockData current = block.getBlockData();
                        if (current.equals(op.data())) {
                            continue;
                        }
                        record.snapshots.add(new Snapshot(op.x(), op.y(), op.z(), current));
                        block.setBlockData(op.data(), false);
                    }
                    if (index >= list.size()) {
                        cancel();
                        StructureInstance instance = finish(bp, placement, record, automatic);
                        busy = false;
                        if (undoOwner != null) {
                            pushHistory(undoOwner, record);
                        }
                        int[] c = placement.center(bp);
                        send(feedback, "§aGotowe! §e" + bp.name() + "§a stoi na §f" + c[0] + " " + c[1] + " " + c[2]
                                + "§a, skrzynie: §f" + record.cratesPlaced + "/" + bp.spawnPoints().size()
                                + (undoOwner != null ? "§a. Cofnięcie: §e/struktury cofnij" : "§a."));
                        if (onDone != null) {
                            onDone.accept(instance);
                        }
                    }
                } catch (RuntimeException e) {
                    cancel();
                    busy = false;
                    if (undoOwner != null) {
                        pushHistory(undoOwner, record);
                    }
                    plugin.getLogger().log(Level.SEVERE, "Błąd podczas stawiania struktury", e);
                    send(feedback, "§cStawianie przerwane przez błąd (szczegóły w konsoli).");
                    if (onDone != null) {
                        onDone.accept(null);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    private StructureInstance finish(Blueprint bp, Placement placement, PasteRecord record, boolean automatic) {
        World world = record.world;
        connectNeighbours(world, record);
        for (Blueprint.SignSpec spec : bp.signs()) {
            writeSign(world, placement, spec);
        }
        int[] center = placement.center(bp);
        String id = UUID.randomUUID().toString().substring(0, 8);
        StructureInstance instance = new StructureInstance(id, bp.id(), placement, center[0], center[2],
                automatic, System.currentTimeMillis());
        record.instanceId = id;
        store.add(instance);
        record.cratesPlaced = crates.refresh(instance, true);
        store.save();
        return instance;
    }

    public boolean undo(UUID owner, CommandSender feedback) {
        Deque<PasteRecord> records = history.get(owner);
        if (records == null || records.isEmpty() || busy) {
            return false;
        }
        PasteRecord record = records.pop();
        busy = true;
        if (record.instanceId != null) {
            StructureInstance instance = store.get(record.instanceId);
            if (instance != null) {
                crates.clear(instance);
                store.remove(instance.id());
            }
        }
        List<Snapshot> snapshots = record.snapshots;
        send(feedback, "§7Cofam §e" + record.structureName + "§7 (" + snapshots.size() + " bloków)...");
        new BukkitRunnable() {
            private int index = snapshots.size() - 1;

            @Override
            public void run() {
                int end = Math.max(index - settings.blocksPerTick(), -1);
                for (; index > end; index--) {
                    Snapshot s = snapshots.get(index);
                    record.world.getBlockAt(s.x(), s.y(), s.z()).setBlockData(s.data(), false);
                }
                if (index < 0) {
                    cancel();
                    busy = false;
                    send(feedback, "§aCofnięto §e" + record.structureName + "§a.");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    private void pushHistory(UUID owner, PasteRecord record) {
        Deque<PasteRecord> records = history.computeIfAbsent(owner, k -> new ArrayDeque<>());
        records.push(record);
        while (records.size() > HISTORY_SIZE) {
            records.removeLast();
        }
    }

    // ------------------------------------------------------------------ dopasowanie terenu

    /**
     * Teren pod, nad i wokół struktury:
     * <ul>
     *     <li>pod strukturą dosypuje grunt aż do litej ziemi (żadnych dziur i jaskiń pod podłogą),</li>
     *     <li>nad strukturą wycina drzewa i pagórki,</li>
     *     <li>w pasie wokół struktury podnosi albo obniża teren liniowo od poziomu struktury do naturalnego,
     *     żeby nie było klifów ani rowów na krawędzi.</li>
     * </ul>
     */
    private void planTerrain(World world, Blueprint bp, Placement placement, Map<Long, Op> ops) {
        int baseY = placement.originY();
        int topY = baseY + bp.height();
        int margin = Math.max(0, settings.smoothing());
        BlockData air = resolve("minecraft:air");
        BlockData stone = resolve("minecraft:stone");

        for (int lx = -margin; lx < bp.width() + margin; lx++) {
            for (int lz = -margin; lz < bp.depth() + margin; lz++) {
                int[] w = placement.toWorld(lx, 0, lz);
                int x = w[0];
                int z = w[2];
                Terrain.Column column = Terrain.scan(world, x, z);
                BlockData top = resolve(column.top());
                BlockData under = resolve(column.under());
                int dx = lx < 0 ? -lx : Math.max(0, lx - (bp.width() - 1));
                int dz = lz < 0 ? -lz : Math.max(0, lz - (bp.depth() - 1));
                int distance = Math.max(dx, dz);

                if (distance == 0) {
                    // nad strukturą: wycinamy wszystko do clearAbove
                    for (int y = topY; y < topY + settings.clearAbove(); y++) {
                        if (!world.getBlockAt(x, y, z).getType().isAir()) {
                            put(ops, x, y, z, air);
                        }
                    }
                    // pod strukturą: fundament do litego gruntu
                    for (int depth = 1; depth <= settings.maxFoundation(); depth++) {
                        int y = baseY - depth;
                        Material type = world.getBlockAt(x, y, z).getType();
                        if (Terrain.isGround(type)) {
                            break;
                        }
                        put(ops, x, y, z, depth <= 3 ? under : stone);
                    }
                    continue;
                }

                // pas wygładzania: cel = poziom struktury przechodzący liniowo w naturalny teren
                int surface = column.surfaceY();
                int target = baseY + (int) Math.round((surface - baseY) * (distance / (double) (margin + 1)));
                if (surface < target) {
                    int from = Math.max(surface + 1, target - settings.maxFoundation());
                    for (int y = from; y <= target; y++) {
                        put(ops, x, y, z, y == target ? top : under);
                    }
                    if (!column.liquid() && surface >= from - 1) {
                        put(ops, x, surface, z, under);
                    }
                } else if (surface > target) {
                    for (int y = target + 1; y <= surface; y++) {
                        put(ops, x, y, z, air);
                    }
                    put(ops, x, target, z, top);
                }
                // drzewa i rośliny nad wyrównanym gruntem
                int ground = Math.min(surface, target);
                for (int y = ground + 1; y <= ground + settings.clearAbove(); y++) {
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (type.isAir()) {
                        continue;
                    }
                    if (Terrain.isVegetation(type) || !type.isSolid()) {
                        if (!ops.containsKey(key(x, y, z)) && !Terrain.isLiquid(type)) {
                            put(ops, x, y, z, air);
                        }
                    }
                }
            }
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFF);
    }

    private static void put(Map<Long, Op> ops, int x, int y, int z, BlockData data) {
        long k = key(x, y, z);
        ops.remove(k); // nowsza operacja trafia na koniec kolejki
        ops.put(k, new Op(x, y, z, data));
    }

    private BlockData resolve(Material material) {
        return resolve(material.getKey().toString());
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

    /** Bloki stawiane są bez fizyki, więc szyby, kraty, płoty i murki łączymy z sąsiadami ręcznie. */
    private void connectNeighbours(World world, PasteRecord record) {
        BlockFace[] sides = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (Snapshot snapshot : record.snapshots) {
            Block block = world.getBlockAt(snapshot.x(), snapshot.y(), snapshot.z());
            Family family = familyOf(block.getType());
            if (family == null) {
                continue;
            }
            StringBuilder state = new StringBuilder(block.getType().getKey().toString()).append('[');
            for (BlockFace face : sides) {
                Material type = block.getRelative(face).getType();
                boolean connect = type.isOccluding() || familyOf(type) == family;
                String side = face.name().toLowerCase(Locale.ROOT);
                if (family == Family.WALL) {
                    state.append(side).append('=').append(connect ? "low" : "none").append(',');
                } else {
                    state.append(side).append('=').append(connect).append(',');
                }
            }
            if (family == Family.WALL) {
                state.append("up=true,");
            }
            state.setCharAt(state.length() - 1, ']');
            block.setBlockData(resolve(state.toString()), false);
        }
    }

    @SuppressWarnings("deprecation")
    private void writeSign(World world, Placement placement, Blueprint.SignSpec spec) {
        Blueprint.Pos p = spec.pos();
        int[] w = placement.toWorld(p.x(), p.y(), p.z());
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

    private static void send(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(message);
        }
    }
}
