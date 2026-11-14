package pl.axon.structure.vehicle;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.axon.structure.api.RoadsideSpot;
import pl.axon.structure.loot.AxonItemsBridge;
import pl.axon.structure.world.Terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Wraki pojazdów PRL przy drogach.
 *
 * <p>Wrak to {@link ItemDisplay} z modelem {@code axon:wrak_*} (patrz {@code vehicles.py}) plus
 * {@link Interaction} jako hitbox bagażnika.
 *
 * <p>Pod wrakiem stawiamy bariery ({@link VehicleMath#collisionBlocks}) — gracz nie przechodzi
 * przez wrak, ale może na niego wskoczyć. Bariery są zapisywane w PDC świata, więc po restarcie
 * serwera wiemy, które bloki posprzątać przy odświeżeniu.
 *
 * <p>Klik w hitbox otwiera bagażnik (wirtualny ekwipunek 9 slotów) z lootem z tabeli
 * {@code wrak_auto} / {@code wrak_wojskowy} z AxonItems. Loot losuje się raz na wrak.
 *
 * <p>Wszystko działa tylko na wątek serwera i tylko w wczytanych chunkach — wraki stoją przy
 * drogach, więc nie ma sensu ładować chunków na siłę.
 */
public final class VehicleService implements Listener {

    /** Rozmiar bagażnika wraku. */
    public static final int TRUNK_SLOTS = 9;

    private static final String MODEL_TAG = "axon_wrak";
    private static final String LOOT_TAG = "axon_wrak_loot";
    private static final String KEY_TAG = "axon_wrak_id";
    private static final String ROAD_PREFIX = "droga:";

    private final JavaPlugin plugin;
    private final NamespacedKey modelKey;
    private final NamespacedKey lootKey;
    private final NamespacedKey wreckKey;

    /** Otwarte bagażniki: gracz → stan (żeby zapisać zawartość po zamknięciu). */
    private final Map<UUID, OpenTrunk> open = new HashMap<>();

    /** Zawartość bagażników wraków z komendy: klucz wraku → ekwipunek (loot losowany raz na wrak). */
    private final Map<String, ItemStack[]> contents = new HashMap<>();

    /** Postawione wraki: klucz „świat:x:y:z” → opis (do sprzątania i diagnostyki). */
    private final Map<String, Placed> placed = new HashMap<>();

    /** Wraki przy drogach: klucz → wrak (z punktów AxonMapGenerator). */
    private final Map<String, Roadside> roadside = new HashMap<>();

    /** Wraki przy drogach: świat → klucz chunka → lista. */
    private final Map<String, Map<Long, List<Roadside>>> roadsideByChunk = new HashMap<>();

    private BukkitTask ticker;

    private VehicleConfig config = VehicleConfig.defaults();

    public VehicleService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modelKey = new NamespacedKey(plugin, MODEL_TAG);
        this.lootKey = new NamespacedKey(plugin, LOOT_TAG);
        this.wreckKey = new NamespacedKey(plugin, KEY_TAG);
    }

    public void configure(VehicleConfig config) {
        this.config = config == null ? VehicleConfig.defaults() : config;
        for (String error : this.config.errors()) {
            plugin.getLogger().warning("wraki: " + error);
        }
    }

    public VehicleConfig config() {
        return config;
    }

    // ------------------------------------------------------------------
    //  Stawianie wraków z listy poboczy
    // ------------------------------------------------------------------

    /**
     * Wraki przy drogach z punktów pobocza zgłoszonych przez AxonMapGenerator. Działają jak beczki przy drogach:
     *
     * <ul>
     *     <li>przy wywołaniu tylko losujemy, który punkt dostaje który wrak (z seeda świata, więc po restarcie
     *     tak samo),</li>
     *     <li>model i hitbox stają, gdy chunk się wczyta, i znikają, gdy chunk się wyładuje (encje nie są
     *     zapisywane w świecie, więc nie ma dubli ani sierot),</li>
     *     <li>opróżniony bagażnik napełnia się po {@code wraki.odnowienie-minut}, o ile żaden gracz nie stoi
     *     bliżej niż {@code wraki.blokada-gdy-gracz-blizej-niz}.</li>
     * </ul>
     *
     * <p>Powtórne wywołanie dla świata podmienia plan.
     *
     * @return liczba zaplanowanych wraków
     */
    public int placeAlongRoads(World world, List<RoadsideSpot> spots) {
        if (world == null || spots == null) {
            return 0;
        }
        forgetRoadside(world);
        if (spots.isEmpty() || !config.enabled() || !config.inWorld(world.getName())) {
            return 0;
        }
        Random roll = new Random(world.getSeed() ^ 0x57524B5FL);
        Map<Long, List<Roadside>> chunks = new HashMap<>();
        int count = 0;
        int lastX = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        for (RoadsideSpot spot : spots) {
            if (spot == null) {
                continue;
            }
            // Odstęp liczony po osi drogi — punkty z RoadsideSpots są już rzadkie, ale
            // konfiguracja może je zagęścić.
            if (lastX != Integer.MIN_VALUE
                    && Math.abs(spot.x() - lastX) + Math.abs(spot.z() - lastZ) < config.minSpacing()) {
                continue;
            }
            if (roll.nextDouble() * 100.0 >= config.chancePercent()) {
                continue;
            }
            VehicleModel model = config.roll(roll);
            if (model == null) {
                continue;
            }
            Roadside wreck = new Roadside(ROAD_PREFIX + world.getName() + ":" + spot.x() + ":" + spot.z(),
                    world.getName(), spot, model);
            if (roadside.putIfAbsent(wreck.key, wreck) != null) {
                continue;
            }
            chunks.computeIfAbsent(chunkKey(spot.x() >> 4, spot.z() >> 4), k -> new ArrayList<>()).add(wreck);
            count++;
            lastX = spot.x();
            lastZ = spot.z();
        }
        roadsideByChunk.put(world.getName(), chunks);
        for (Roadside wreck : roadside.values()) {
            if (wreck.world.equals(world.getName())) {
                show(world, wreck);
            }
        }
        startTicker();
        plugin.getLogger().info("Wraki przy drogach w świecie " + world.getName() + ": " + count);
        return count;
    }

    /** Liczba wraków przy drogach w świecie i ile z nich stoi teraz (wczytane chunki). */
    public int[] roadsideCount(World world) {
        int all = 0;
        int shown = 0;
        for (Roadside wreck : roadside.values()) {
            if (world != null && wreck.world.equals(world.getName())) {
                all++;
                if (!wreck.entities.isEmpty()) {
                    shown++;
                }
            }
        }
        return new int[]{all, shown};
    }

    private void forgetRoadside(World world) {
        for (Roadside wreck : List.copyOf(roadside.values())) {
            if (wreck.world.equals(world.getName())) {
                hide(world, wreck);
                roadside.remove(wreck.key);
            }
        }
        roadsideByChunk.remove(world.getName());
    }

    /** Stawia model i hitbox wraku, jeśli chunk jest wczytany i wrak jeszcze nie stoi. */
    private void show(World world, Roadside wreck) {
        RoadsideSpot spot = wreck.spot;
        if (!wreck.entities.isEmpty() || !world.isChunkLoaded(spot.x() >> 4, spot.z() >> 4)) {
            return;
        }
        int groundY = groundY(world, spot.x(), spot.z(), wreck.model, spot.steps());
        if (groundY == Integer.MIN_VALUE) {
            return;
        }
        // Wrak obrócony „wzdłuż drogi”: RoadsideSpot.steps() obraca yaw (przód do drogi) o 90°.
        Spawned spawned = spawn(world, spot.x(), groundY, spot.z(), spot.steps(), wreck.model, false, wreck.key);
        if (spawned != null) {
            wreck.entities.addAll(spawned.entities());
        }
    }

    /** Zdejmuje encje wraku (bariery zostają w świecie, przy ponownym postawieniu nie są dublowane). */
    private void hide(World world, Roadside wreck) {
        for (UUID id : wreck.entities) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        wreck.entities.clear();
        for (Map.Entry<UUID, OpenTrunk> entry : List.copyOf(open.entrySet())) {
            if (wreck.key.equals(entry.getValue().wreckKey())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        List<Roadside> list = roadsideIn(event.getChunk());
        if (list.isEmpty()) {
            return;
        }
        World world = event.getWorld();
        List<Roadside> copy = List.copyOf(list);
        // w następnym ticku: stawianie encji w trakcie wczytywania chunka bywa gubione
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Roadside wreck : copy) {
                if (roadside.get(wreck.key) == wreck) {
                    show(world, wreck);
                }
            }
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Roadside wreck : roadsideIn(event.getChunk())) {
            hide(event.getWorld(), wreck);
        }
    }

    private List<Roadside> roadsideIn(Chunk chunk) {
        Map<Long, List<Roadside>> chunks = roadsideByChunk.get(chunk.getWorld().getName());
        if (chunks == null) {
            return List.of();
        }
        List<Roadside> list = chunks.get(chunkKey(chunk.getX(), chunk.getZ()));
        return list == null ? List.of() : list;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** Co pół minuty: opróżnione bagażniki napełniają się po czasie odnowienia, gdy w pobliżu nie ma gracza. */
    private void startTicker() {
        if (ticker != null) {
            return;
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::refill, 600L, 600L);
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long respawn = config.respawnMinutes() * 60_000L;
        for (Roadside wreck : roadside.values()) {
            if (wreck.emptiedAt <= 0 || now - wreck.emptiedAt < respawn || isOpen(wreck.key)) {
                continue;
            }
            World world = Bukkit.getWorld(wreck.world);
            if (world == null || playerNearby(world, wreck.spot.x(), wreck.spot.z())) {
                continue;
            }
            wreck.contents = null; // nowy loot losuje się przy następnym otwarciu
            wreck.emptiedAt = 0;
        }
    }

    private boolean isOpen(String wreckKey) {
        for (OpenTrunk trunk : open.values()) {
            if (wreckKey.equals(trunk.wreckKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean playerNearby(World world, int x, int z) {
        int radius = config.playerBlockRadius();
        if (radius <= 0) {
            return false;
        }
        double r2 = (double) radius * radius;
        for (Player player : world.getPlayers()) {
            Location at = player.getLocation();
            double dx = at.getX() - (x + 0.5);
            double dz = at.getZ() - (z + 0.5);
            if (dx * dx + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stawia wrak w konkretnych współrzędnych (komenda {@code /struktury wrak}). Taki wrak jest zapisywany
     * w świecie i nie odnawia się.
     *
     * @param groundY blok gruntu — model i bariery startują od {@code groundY + 1}
     */
    public boolean place(World world, int x, int groundY, int z, int steps, VehicleModel model) {
        if (world == null || model == null) {
            return false;
        }
        String key = key(world, x, groundY, z);
        if (placed.containsKey(key)) {
            return false;
        }
        Spawned spawned = spawn(world, x, groundY, z, steps, model, true, null);
        if (spawned == null) {
            return false;
        }
        placed.put(key, new Placed(world.getName(), x, groundY, z, steps, model.id(),
                spawned.entities(), spawned.barriers()));
        return true;
    }

    private record Spawned(List<UUID> entities, List<int[]> barriers) {
    }

    private Spawned spawn(World world, int x, int groundY, int z, int steps, VehicleModel model,
                          boolean persistent, String wreckKey) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        List<Entity> spawned = new ArrayList<>();
        try {
            // Paczka ma jeden model na cały pojazd (axon:wrak_*), więc jeden wyświetlacz w środku obrysu.
            // Spód kół leży na powierzchni gruntu (groundY + 1), środek modelu scale/2 wyżej. Wcześniej
            // środek stał w bloku gruntu: model był w ziemi, a wyświetlacz brał światło z wnętrza
            // pełnego bloku (0), więc z ziemi wystawała tylko czarna płyta.
            Location base = new Location(world, x + 0.5, groundY + 1.0 + model.centerHeight(), z + 0.5);
            // Wyświetlacz przedmiotu sam obraca model o 180° (jak meble w PropService).
            base.setYaw(steps * 90f + 180f);
            base.setPitch(0f);
            float scale = (float) model.scale();
            float size = (float) (Math.max(model.length(), model.height()) + 1.0);
            ItemDisplay display = world.spawn(base, ItemDisplay.class, entity -> {
                entity.setItemStack(modelItem(model.id()));
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                entity.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 1f, 0f),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0f, 0f, 1f, 0f)));
                // Obrys do odcinania poza ekranem: bez tego długi wrak (Autosan, 10 m) znika, gdy jego
                // środek wyjdzie poza kadr.
                entity.setDisplayWidth(size);
                entity.setDisplayHeight(size);
                entity.setViewRange(Math.max(1f, size / 8f));
                entity.setPersistent(persistent);
                entity.setInvulnerable(true);
                entity.setGravity(false);
                tag(entity.getPersistentDataContainer(), model.id(), "", null);
            });
            spawned.add(display);

            // Hitbox bagażnika na całym wraku (kilka kwadratów wzdłuż długości), trochę większy od barier,
            // żeby klik w dowolne miejsce wraku otwierał bagażnik zamiast trafiać w barierę.
            String trunkKey = wreckKey != null ? wreckKey : key(world, x, groundY, z);
            for (double[] box : VehicleMath.hitboxes(model)) {
                double[] offset = VehicleMath.rotateOffset(box[0], 0.0, steps);
                Location at = new Location(world, x + 0.5 + offset[0], groundY + 1.0, z + 0.5 + offset[1]);
                Interaction hitbox = world.spawn(at, Interaction.class, entity -> {
                    entity.setInteractionWidth((float) box[1]);
                    entity.setInteractionHeight((float) box[2]);
                    entity.setResponsive(true);
                    entity.setPersistent(persistent);
                    entity.setInvulnerable(true);
                    tag(entity.getPersistentDataContainer(), model.id(), model.lootTable(), trunkKey);
                });
                spawned.add(hitbox);
            }

            List<int[]> barriers = new ArrayList<>();
            if (config.barriers()) {
                List<int[]> wanted = VehicleMath.collisionBlocks(model, x, groundY, z, steps);
                // Bariery ze starej wersji były większe niż model (niewidzialne ściany obok wraku): sprzątamy
                // te, które wypadają poza nowy obrys.
                Set<Long> keep = new HashSet<>();
                for (int[] pos : wanted) {
                    keep.add(blockKey(pos));
                }
                for (int[] pos : VehicleMath.legacyCollisionBlocks(model, x, groundY, z, steps)) {
                    if (!keep.contains(blockKey(pos))) {
                        Block block = world.getBlockAt(pos[0], pos[1], pos[2]);
                        if (block.getType() == Material.BARRIER) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
                BlockData barrier = Material.BARRIER.createBlockData();
                for (int[] pos : wanted) {
                    Block block = world.getBlockAt(pos[0], pos[1], pos[2]);
                    // powietrze albo roślina (trawa, kwiaty, warstwa śniegu) — nie pełny blok
                    if (block.getType().isAir() || (!block.getType().isSolid() && !block.isLiquid())) {
                        block.setBlockData(barrier, false);
                        barriers.add(pos);
                    }
                }
            }
            return new Spawned(spawned.stream().map(Entity::getUniqueId).toList(), List.copyOf(barriers));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Nie udało się postawić wraku " + model.id() + " na "
                    + x + " " + groundY + " " + z + ": " + e.getMessage());
            for (Entity entity : spawned) {
                entity.remove();
            }
            return null;
        }
    }

    // ------------------------------------------------------------------
    //  Sprzątanie
    // ------------------------------------------------------------------

    /** Usuwa wszystkie wraki (i ich bariery) w świecie. */
    public int clear(World world) {
        if (world == null) {
            return 0;
        }
        int removed = 0;
        String prefix = world.getName() + ":";
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Placed> entry : placed.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                keys.add(entry.getKey());
            }
        }
        for (String key : keys) {
            if (remove(world, key)) {
                removed++;
            }
        }
        // Sieroty po restarcie: encje z PDC i bariery z PDC.
        for (Entity entity : world.getEntities()) {
            if (isWreckEntity(entity)) {
                entity.remove();
            }
        }
        // Wraki przy drogach wrócą przy następnym wczytaniu chunka.
        for (Roadside wreck : roadside.values()) {
            if (wreck.world.equals(world.getName())) {
                wreck.entities.clear();
            }
        }
        return removed;
    }

    /** Usuwa wrak w podanych współrzędnych (razem z barierami i zapamiętanym lootem). */
    public boolean remove(World world, int x, int groundY, int z) {
        return remove(world, key(world, x, groundY, z));
    }

    private boolean remove(World world, String key) {
        Placed record = placed.remove(key);
        if (record == null) {
            return false;
        }
        contents.remove(key);
        for (UUID id : record.entities()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        // Bariery: te, które faktycznie postawiliśmy (lista w rekordzie).
        for (int[] pos : record.barriers()) {
            Block block = world.getBlockAt(pos[0], pos[1], pos[2]);
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR, false);
            }
        }
        return true;
    }

    /** Liczba postawionych wraków w świecie. */
    public int count(World world) {
        if (world == null) {
            return 0;
        }
        String prefix = world.getName() + ":";
        int count = 0;
        for (String key : placed.keySet()) {
            if (key.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    /** Opisy postawionych wraków w świecie (do diagnostyki w komendzie). */
    public List<Placed> list(World world) {
        List<Placed> result = new ArrayList<>();
        if (world == null) {
            return result;
        }
        String prefix = world.getName() + ":";
        for (Map.Entry<String, Placed> entry : placed.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    //  Bagażnik (klik w hitbox)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction hitbox)) {
            return;
        }
        PersistentDataContainer pdc = hitbox.getPersistentDataContainer();
        String table = pdc.get(lootKey, PersistentDataType.STRING);
        if (table == null || table.isBlank()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            return; // sneak = nie otwieraj (pozwala obejrzeć wrak)
        }
        openTrunk(player, hitbox, table);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        OpenTrunk trunk = open.remove(player.getUniqueId());
        if (trunk == null) {
            return;
        }
        ItemStack[] snapshot = event.getInventory().getContents();
        trunk.holder.contents = snapshot;
        Roadside wreck = trunk.wreckKey() == null ? null : roadside.get(trunk.wreckKey());
        if (wreck == null) {
            contents.put(trunk.wreckKey(), snapshot);
            return;
        }
        wreck.contents = snapshot;
        // jak skrzynia przy drodze: czas odnowienia liczymy od opróżnienia
        if (isEmpty(snapshot)) {
            if (wreck.emptiedAt <= 0) {
                wreck.emptiedAt = System.currentTimeMillis();
            }
        } else {
            wreck.emptiedAt = 0;
        }
    }

    private void openTrunk(Player player, Interaction hitbox, String table) {
        TrunkHolder holder = new TrunkHolder(hitbox.getUniqueId(), "&8Bagażnik wraku");
        Inventory inventory = Bukkit.createInventory(holder, TRUNK_SLOTS, "Bagażnik wraku");
        holder.inventory = inventory;
        String key = hitbox.getPersistentDataContainer().get(wreckKey, PersistentDataType.STRING);
        if (key == null) {
            key = hitbox.getUniqueId().toString(); // hitbox ze starej wersji (bez klucza wraku)
        }
        Roadside wreck = roadside.get(key);
        ItemStack[] stored = wreck != null ? wreck.contents : contents.get(key);
        if (stored == null) {
            stored = rollLoot(table);
            if (wreck != null) {
                wreck.contents = stored;
            } else {
                contents.put(key, stored);
            }
        }
        inventory.setContents(stored);
        holder.contents = stored;
        open.put(player.getUniqueId(), new OpenTrunk(holder, hitbox, key));
        player.openInventory(inventory);
    }

    private static boolean isEmpty(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Losuje loot z tabeli AxonItems; gdy most nieaktywny — pusto (wrak nadal stoi). */
    private ItemStack[] rollLoot(String table) {
        ItemStack[] slots = new ItemStack[TRUNK_SLOTS];
        if (!config.loot()) {
            return slots;
        }
        List<ItemStack> items = AxonItemsBridge.rollTable(table);
        int slot = 0;
        for (ItemStack item : items) {
            if (item == null || slot >= TRUNK_SLOTS) {
                break;
            }
            slots[slot++] = item;
        }
        return slots;
    }

    // ------------------------------------------------------------------
    //  Pomocnicze
    // ------------------------------------------------------------------

    /**
     * Grunt pod wrakiem: najniższy z gruntów pod wszystkimi kolumnami obrysu (koła stoją na najniższym
     * miejscu, a nie wiszą nad zboczem). W każdej kolumnie liczy się tylko prawdziwy grunt
     * ({@link Terrain#isGround}): trawa, kwiaty, warstwy śniegu, liście, pnie i bariery są pomijane.
     * Wcześniej brany był najwyższy nie-powietrzny blok w środku, więc wrak stał na trawie albo na
     * koronie drzewa i wisiał w powietrzu.
     *
     * @return Y bloku gruntu albo {@code Integer.MIN_VALUE}, gdy w obrysie nie ma gruntu
     */
    public static int groundY(World world, int x, int z, VehicleModel model, int steps) {
        int[] half = model == null ? new int[]{0, 0} : VehicleMath.halfBlocks(model, steps);
        int best = Integer.MAX_VALUE;
        int center = columnGround(world, x, z);
        for (int dx = -half[0]; dx <= half[0]; dx++) {
            for (int dz = -half[1]; dz <= half[1]; dz++) {
                int cx = x + dx;
                int cz = z + dz;
                if (!world.isChunkLoaded(cx >> 4, cz >> 4)) {
                    continue;
                }
                int y = columnGround(world, cx, cz);
                // dziura, woda albo urwisko obok drogi: nie zakopujemy wraku głębiej niż 2 bloki pod środkiem
                if (y != Integer.MIN_VALUE && (center == Integer.MIN_VALUE || y >= center - 2)) {
                    best = Math.min(best, y);
                }
            }
        }
        return best == Integer.MAX_VALUE ? center : best;
    }

    /** Najwyższy blok gruntu w kolumnie albo {@code Integer.MIN_VALUE} (woda, pustka, niewczytany chunk). */
    private static int columnGround(World world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return Integer.MIN_VALUE;
        }
        int min = world.getMinHeight();
        for (int y = world.getMaxHeight() - 1; y >= min; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (type.isAir() || type == Material.BARRIER) {
                continue;
            }
            if (Terrain.isLiquid(type)) {
                return Integer.MIN_VALUE;
            }
            if (Terrain.isGround(type)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void tag(PersistentDataContainer pdc, String modelId, String lootTable, String wreck) {
        pdc.set(modelKey, PersistentDataType.STRING, modelId == null ? "" : modelId);
        if (lootTable != null && !lootTable.isBlank()) {
            pdc.set(lootKey, PersistentDataType.STRING, lootTable);
        }
        if (wreck != null) {
            pdc.set(wreckKey, PersistentDataType.STRING, wreck);
        }
    }

    private boolean isWreckEntity(Entity entity) {
        if (!(entity instanceof ItemDisplay) && !(entity instanceof Interaction)) {
            return false;
        }
        return entity.getPersistentDataContainer().has(modelKey, PersistentDataType.STRING);
    }

    /** Przedmiot-model z paczki: {@code axon:<nazwa>} jako item_model (bez tego widać zwykły papier). */
    private ItemStack modelItem(String modelId) {
        ItemStack item = new ItemStack(Material.PAPER);
        var meta = item.getItemMeta();
        if (meta != null) {
            String id = modelId == null ? "" : modelId;
            meta.getPersistentDataContainer().set(modelKey, PersistentDataType.STRING, id);
            NamespacedKey model = NamespacedKey.fromString(id.contains(":") ? id : "axon:" + id);
            if (model != null) {
                meta.setItemModel(model);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static long blockKey(int[] pos) {
        return ((long) (pos[0] & 0x3FFFFFF) << 38) | ((long) (pos[2] & 0x3FFFFFF) << 12) | (pos[1] & 0xFFF);
    }

    private static String key(World world, int x, int y, int z) {
        return (world == null ? "?" : world.getName()) + ":" + x + ":" + y + ":" + z;
    }

    /** Opis postawionego wraku. */
    public record Placed(String world, int x, int groundY, int z, int steps, String modelId,
                         List<UUID> entities, List<int[]> barriers) {
    }

    /** Wrak przy drodze: stały punkt i model, encje tylko gdy chunk jest wczytany, loot odnawia się po czasie. */
    private static final class Roadside {
        final String key;
        final String world;
        final RoadsideSpot spot;
        final VehicleModel model;
        final List<UUID> entities = new ArrayList<>();
        ItemStack[] contents;
        long emptiedAt;

        Roadside(String key, String world, RoadsideSpot spot, VehicleModel model) {
            this.key = key;
            this.world = world;
            this.spot = spot;
            this.model = model;
        }
    }

    private record OpenTrunk(TrunkHolder holder, Interaction hitbox, String wreckKey) {
    }

    /** Holder wirtualnego bagażnika — trzyma zawartość między otwarciami. */
    public static final class TrunkHolder implements InventoryHolder {

        private final UUID sourceId;
        private final String title;
        private ItemStack[] contents;
        private Inventory inventory;

        TrunkHolder(UUID sourceId, String title) {
            this.sourceId = sourceId;
            this.title = title;
        }

        public UUID sourceId() {
            return sourceId;
        }

        public String title() {
            return title;
        }

        public ItemStack[] contents() {
            return contents;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
