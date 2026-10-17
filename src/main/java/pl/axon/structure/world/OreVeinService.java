package pl.axon.structure.world;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Żyły rudy w kopalni: punkty {@code punkt-ruda} z pliku struktury leżą na blokach rudy. Te bloki (i tylko one)
 * wolno kopać mimo ochrony struktury. Z rudy miedzi wypada siarka taka sama jak z głazów AxonOre (żółty barwnik
 * z custom-model-data 10001, po którym AxonPlot rozpoznaje siarkę), z reszty zwykły drop. Wykopane miejsce zamienia
 * się w kamień, a po {@code odnawianie-minut} odrasta w nim losowa ruda (według wag). Kolejka odnawiania jest
 * zapisywana w {@code kopalnia.yml}, więc przeżywa restart.
 */
public final class OreVeinService implements Listener {

    public static final String POOL = "punkt-ruda";

    /** Rudy, które mogą się pojawić w żyle; klucze jak w AxonOre (copper = siarka). */
    public enum Ore {
        COAL("coal", Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, 0, 2),
        IRON("iron", Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, 0, 0),
        COPPER("copper", Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, 0, 0),
        GOLD("gold", Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, 0, 0),
        REDSTONE("redstone", Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, 1, 5),
        LAPIS("lapis", Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, 2, 5),
        DIAMOND("diamond", Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, 3, 7),
        EMERALD("emerald", Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, 3, 7);

        private final String key;
        private final Material normal;
        private final Material deepslate;
        private final int minExp;
        private final int maxExp;

        Ore(String key, Material normal, Material deepslate, int minExp, int maxExp) {
            this.key = key;
            this.normal = normal;
            this.deepslate = deepslate;
            this.minExp = minExp;
            this.maxExp = maxExp;
        }

        public String key() {
            return key;
        }

        Material block(boolean deep) {
            return deep ? deepslate : normal;
        }

        int rollExp(Random random) {
            return minExp + (maxExp > minExp ? random.nextInt(maxExp - minExp + 1) : 0);
        }

        public static Ore of(Material material) {
            for (Ore ore : values()) {
                if (ore.normal == material || ore.deepslate == material) {
                    return ore;
                }
            }
            return null;
        }

        /** Ruda po nazwie z configu: angielskiej (jak w AxonOre) albo polskiej. */
        public static Ore parse(String name) {
            String n = name.toLowerCase(Locale.ROOT).trim();
            return switch (n) {
                case "coal", "wegiel", "węgiel" -> COAL;
                case "iron", "zelazo", "żelazo" -> IRON;
                case "copper", "miedz", "miedź", "siarka" -> COPPER;
                case "gold", "zloto", "złoto" -> GOLD;
                case "redstone" -> REDSTONE;
                case "lapis", "lazuryt" -> LAPIS;
                case "diamond", "diament" -> DIAMOND;
                case "emerald", "szmaragd" -> EMERALD;
                default -> null;
            };
        }
    }

    /** Co zostaje po wykopaniu rudy, zanim odrośnie. */
    public enum Leftover {
        STONE, COBBLESTONE, AIR;

        public static Leftover parse(String value) {
            return switch (value == null ? "" : value.toLowerCase(Locale.ROOT).trim()) {
                case "bruk", "cobblestone" -> COBBLESTONE;
                case "powietrze", "air" -> AIR;
                default -> STONE;
            };
        }

        Material block(boolean deep) {
            return switch (this) {
                case STONE -> deep ? Material.DEEPSLATE : Material.STONE;
                case COBBLESTONE -> deep ? Material.COBBLED_DEEPSLATE : Material.COBBLESTONE;
                case AIR -> Material.AIR;
            };
        }
    }

    /**
     * @param regenMillis       po jakim czasie wykopana ruda odrasta
     * @param playerBlockRadius nie odnawiaj, gdy gracz stoi bliżej niż tyle bloków od miejsca
     * @param weights           wagi losowania rud przy odnawianiu
     */
    public record Settings(boolean enabled, long regenMillis, Leftover leftover, int playerBlockRadius, boolean exp,
                           Map<Ore, Integer> weights, boolean sulfur, String sulfurName, List<String> sulfurLore,
                           int sulfurModelData, int sulfurMin, int sulfurMax) {
    }

    /** Miejsce na rudę w postawionej strukturze. */
    private record Cell(String instanceId, String world, int x, int y, int z, boolean deepslate) {

        String key() {
            return OreVeinService.key(world, x, y, z);
        }
    }

    /** Bloki, które odnawianie może zastąpić rudą (zostawione po wykopaniu albo zwykła skała). */
    private static final Set<Material> REPLACEABLE = EnumSet.of(Material.AIR, Material.CAVE_AIR, Material.STONE,
            Material.DEEPSLATE, Material.COBBLESTONE, Material.COBBLED_DEEPSLATE, Material.TUFF, Material.GRAVEL,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE);
    private static final Pattern HEX = Pattern.compile("(?:<#|&#)([0-9a-fA-F]{6})>?");
    private static final long TICK = 20L * 10;
    /** Co ile przebiegów zegara (po 10 s) sprawdzać, czy w jakiejś żyle nie brakuje rudy. */
    private static final int SWEEP_EVERY = 6;

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final File file;
    private final Random random = new Random();
    /** świat → klucz bloku → miejsce na rudę. */
    private final Map<String, Map<Long, Cell>> index = new HashMap<>();
    private long indexSignature = Long.MIN_VALUE;
    private int indexSize;
    /** Wykopane miejsca: "świat;x;y;z" → kiedy (ms) ruda ma odrosnąć. */
    private final Map<String, Long> pending = new LinkedHashMap<>();
    private boolean dirty;
    private boolean warnedWeights;
    private int sweepCounter;
    private Settings settings;
    private BukkitTask task;

    public OreVeinService(JavaPlugin plugin, StructureLibrary library, InstanceStore store) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.file = new File(plugin.getDataFolder(), "kopalnia.yml");
        load();
    }

    public void configure(Settings settings) {
        this.settings = settings;
        warnedWeights = false;
        indexSignature = Long.MIN_VALUE; // pliki struktur mogły się zmienić
    }

    public Settings settings() {
        return settings;
    }

    public boolean enabled() {
        return settings != null && settings.enabled();
    }

    public void start() {
        stopTimer();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 20, TICK);
    }

    public void stop() {
        stopTimer();
        save();
    }

    private void stopTimer() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Czy struktura ma żyły rudy (wtedy jej region WorldGuard musi pozwalać kopać). */
    public static boolean hasVeins(Blueprint bp) {
        for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
            if (point.pool().equalsIgnoreCase(POOL)) {
                return true;
            }
        }
        return false;
    }

    /** Czy gracz może wykopać ten blok mimo ochrony struktury: to ruda w miejscu {@code punkt-ruda}. */
    public boolean isMineable(Block block) {
        return enabled() && Ore.of(block.getType()) != null && cellAt(block) != null;
    }

    // ------------------------------------------------------------------ indeks miejsc

    private static String key(String world, int x, int y, int z) {
        return world + ";" + x + ";" + y + ";" + z;
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
    }

    /** Podpis listy struktur: zmienia się, gdy struktura dojdzie, zniknie albo jej plik zostanie wczytany od nowa. */
    private long signature() {
        long h = 17;
        for (StructureInstance instance : store.all()) {
            h = h * 31 + instance.id().hashCode();
            h = h * 31 + System.identityHashCode(library.get(instance.structureId()));
        }
        return h;
    }

    private Map<String, Map<Long, Cell>> index() {
        long signature = signature();
        if (signature != indexSignature) {
            index.clear();
            indexSize = 0;
            for (StructureInstance instance : store.all()) {
                Blueprint bp = library.get(instance.structureId());
                if (bp == null) {
                    continue;
                }
                for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
                    if (!point.pool().equalsIgnoreCase(POOL)) {
                        continue;
                    }
                    Blueprint.Pos p = point.pos();
                    int[] w = instance.placement().toWorld(p.x(), p.y(), p.z());
                    boolean deep = bp.get(p.x(), p.y(), p.z()).contains("deepslate");
                    Cell cell = new Cell(instance.id(), instance.world(), w[0], w[1], w[2], deep);
                    if (index.computeIfAbsent(instance.world(), k -> new HashMap<>())
                            .put(blockKey(w[0], w[1], w[2]), cell) == null) {
                        indexSize++;
                    }
                }
            }
            indexSignature = signature;
        }
        return index;
    }

    private static Cell lookup(Map<String, Map<Long, Cell>> cells, String world, int x, int y, int z) {
        Map<Long, Cell> inWorld = cells.get(world);
        return inWorld == null ? null : inWorld.get(blockKey(x, y, z));
    }

    private Cell cellAt(Block block) {
        return lookup(index(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    /** Miejsce po kluczu "świat;x;y;z" z kolejki odnawiania. */
    private static Cell lookup(Map<String, Map<Long, Cell>> cells, String key) {
        int c3 = key.lastIndexOf(';');
        int c2 = c3 <= 0 ? -1 : key.lastIndexOf(';', c3 - 1);
        int c1 = c2 <= 0 ? -1 : key.lastIndexOf(';', c2 - 1);
        if (c1 <= 0) {
            return null;
        }
        try {
            return lookup(cells, key.substring(0, c1), Integer.parseInt(key.substring(c1 + 1, c2)),
                    Integer.parseInt(key.substring(c2 + 1, c3)), Integer.parseInt(key.substring(c3 + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ kopanie

    /**
     * Drop ustawiamy na HIGHEST, czyli po AxonOre (HIGH), który w swoim świecie z {@code mining.block-natural-ores}
     * wyłącza drop rud spoza głazów. Miedź: bez zwykłego dropu (siarkę wyrzucamy na MONITOR, gdy wiadomo, że blok
     * naprawdę zniknie). Reszta: zwykły drop z fortuną i jedwabnym dotykiem.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakDrops(BlockBreakEvent event) {
        if (!enabled()) {
            return;
        }
        Block block = event.getBlock();
        Ore ore = Ore.of(block.getType());
        if (ore == null || cellAt(block) == null) {
            return;
        }
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        boolean harvest = block.isPreferredTool(tool);
        event.setDropItems(!(ore == Ore.COPPER && settings.sulfur()));
        boolean silk = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        event.setExpToDrop(settings.exp() && harvest && !silk ? ore.rollExp(random) : 0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroken(BlockBreakEvent event) {
        if (!enabled()) {
            return;
        }
        Block block = event.getBlock();
        Ore ore = Ore.of(block.getType());
        Cell cell = ore == null ? null : cellAt(block);
        if (cell == null) {
            return;
        }
        Player player = event.getPlayer();
        if (ore == Ore.COPPER && settings.sulfur() && player.getGameMode() != GameMode.CREATIVE
                && block.isPreferredTool(player.getInventory().getItemInMainHand())) {
            int min = Math.max(1, settings.sulfurMin());
            int max = Math.max(min, settings.sulfurMax());
            int amount = min + random.nextInt(max - min + 1);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), sulfur(amount));
        }
        pending.put(cell.key(), System.currentTimeMillis() + settings.regenMillis());
        dirty = true;
        // blok znika dopiero po obsłudze zdarzenia: w jego miejsce kamień tick później
        Bukkit.getScheduler().runTask(plugin, () -> afterMined(cell));
    }

    private void afterMined(Cell cell) {
        World world = Bukkit.getWorld(cell.world());
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
        if (Ore.of(block.getType()) != null) {
            // ruda jednak została (inny plugin cofnął wykopanie)
            pending.remove(cell.key());
            return;
        }
        if (block.getType().isAir() && settings.leftover() != Leftover.AIR) {
            block.setType(settings.leftover().block(cell.deepslate()), false);
        }
    }

    /** Siarka taka jak z głazów AxonOre: żółty barwnik, nazwa bez kursywy, custom-model-data, bez opisu i PDC. */
    @SuppressWarnings("deprecation")
    public ItemStack sulfur(int amount) {
        ItemStack item = new ItemStack(Material.YELLOW_DYE, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(itemText(settings.sulfurName()));
            if (!settings.sulfurLore().isEmpty()) {
                meta.lore(settings.sulfurLore().stream().map(OreVeinService::itemText).toList());
            }
            meta.setCustomModelData(settings.sulfurModelData());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Ta sama zamiana kolorów co w AxonOre (Text.itemComponent): hex {@code <#RRGGBB>} / {@code &#RRGGBB} i kody {@code &}. */
    @SuppressWarnings("deprecation")
    private static Component itemText(String text) {
        Matcher m = HEX.matcher(text == null ? "" : text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            StringBuilder hex = new StringBuilder("§x");
            for (char c : m.group(1).toCharArray()) {
                hex.append('§').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(hex.toString()));
        }
        m.appendTail(sb);
        String legacy = ChatColor.translateAlternateColorCodes('&', sb.toString());
        return LegacyComponentSerializer.legacySection().deserialize(legacy)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    // ------------------------------------------------------------------ odnawianie

    private void tick() {
        if (!enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Map<Long, Cell>> cells = index();
        Iterator<Map.Entry<String, Long>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            Cell cell = lookup(cells, entry.getKey());
            if (cell == null) {
                // struktura zapomniana albo plik bez tego punktu
                it.remove();
                dirty = true;
                continue;
            }
            if (now < entry.getValue()) {
                continue;
            }
            World world = Bukkit.getWorld(cell.world());
            if (world == null || !world.isChunkLoaded(cell.x() >> 4, cell.z() >> 4) || playerNear(world, cell)) {
                continue;
            }
            regenerate(world, cell);
            it.remove();
            dirty = true;
        }
        if (++sweepCounter >= SWEEP_EVERY) {
            sweepCounter = 0;
            sweep(now);
        }
        if (dirty) {
            save();
        }
    }

    /** Miejsca bez rudy, których nie ma w kolejce (np. kolejka zginęła, admin z axonstructure.bypass wykopał rudę przy wyłączonej kopalni). */
    private void sweep(long now) {
        for (Map<Long, Cell> cells : index().values()) {
            for (Cell cell : cells.values()) {
                if (pending.containsKey(cell.key())) {
                    continue;
                }
                World world = Bukkit.getWorld(cell.world());
                if (world == null || !world.isChunkLoaded(cell.x() >> 4, cell.z() >> 4)) {
                    continue;
                }
                Material type = world.getBlockAt(cell.x(), cell.y(), cell.z()).getType();
                // Powietrze tylko wtedy, gdy po wykopaniu zostaje powietrze: w strukturze postawionej ze starszego
                // pliku (bez żył) punkt może wypaść w pustym korytarzu i ruda by go zatkała.
                boolean air = type.isAir();
                if (Ore.of(type) == null && REPLACEABLE.contains(type) && (!air || settings.leftover() == Leftover.AIR)) {
                    pending.put(cell.key(), now + settings.regenMillis());
                    dirty = true;
                }
            }
        }
    }

    private boolean playerNear(World world, Cell cell) {
        double limit = (double) settings.playerBlockRadius() * settings.playerBlockRadius();
        if (limit <= 0) {
            return false;
        }
        for (Player player : world.getPlayers()) {
            Location l = player.getLocation();
            double dx = l.getX() - (cell.x() + 0.5);
            double dy = l.getY() - (cell.y() + 0.5);
            double dz = l.getZ() - (cell.z() + 0.5);
            if (dx * dx + dy * dy + dz * dz < limit) {
                return true;
            }
        }
        return false;
    }

    private void regenerate(World world, Cell cell) {
        Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
        Material type = block.getType();
        if (Ore.of(type) != null || !REPLACEABLE.contains(type)) {
            return; // już jest ruda albo ktoś (admin) postawił tu coś innego
        }
        block.setType(roll().block(cell.deepslate()), false);
        world.playSound(block.getLocation().add(0.5, 0.5, 0.5),
                cell.deepslate() ? Sound.BLOCK_DEEPSLATE_PLACE : Sound.BLOCK_STONE_PLACE, 0.5f, 0.8f);
    }

    private Ore roll() {
        int total = 0;
        for (int weight : settings.weights().values()) {
            total += Math.max(0, weight);
        }
        if (total <= 0) {
            if (!warnedWeights) {
                warnedWeights = true;
                plugin.getLogger().warning("kopalnia.rudy: wszystkie wagi są zerowe, odrasta węgiel.");
            }
            return Ore.COAL;
        }
        int r = random.nextInt(total);
        for (Map.Entry<Ore, Integer> entry : settings.weights().entrySet()) {
            r -= Math.max(0, entry.getValue());
            if (r < 0) {
                return entry.getKey();
            }
        }
        return Ore.COAL;
    }

    // ------------------------------------------------------------------ zapis

    private void load() {
        pending.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String line : yaml.getStringList("odnawianie")) {
            int split = line.lastIndexOf(';');
            if (split <= 0) {
                continue;
            }
            try {
                pending.put(line.substring(0, split), Long.parseLong(line.substring(split + 1)));
            } catch (NumberFormatException ignored) {
                // uszkodzony wpis
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Long> entry : pending.entrySet()) {
            lines.add(entry.getKey() + ";" + entry.getValue());
        }
        yaml.set("odnawianie", lines);
        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać " + file.getName() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ do komend

    /** Ile miejsc na rudę mają postawione struktury. */
    public int veinCount() {
        index();
        return indexSize;
    }

    /** Ile wykopanych miejsc czeka na odrośnięcie rudy. */
    public int pendingCount() {
        return pending.size();
    }
}
