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
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.axon.structure.api.RoadsideSpot;
import pl.axon.structure.loot.AxonItemsBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    private final JavaPlugin plugin;
    private final NamespacedKey modelKey;
    private final NamespacedKey lootKey;

    /** Otwarte bagażniki: gracz → stan (żeby zapisać zawartość po zamknięciu). */
    private final Map<UUID, OpenTrunk> open = new HashMap<>();

    /** Zawartość bagażników: id hitboxa → ekwipunek (loot losowany raz na wrak). */
    private final Map<UUID, ItemStack[]> contents = new HashMap<>();

    /** Postawione wraki: klucz „świat:x:y:z” → opis (do sprzątania i diagnostyki). */
    private final Map<String, Placed> placed = new HashMap<>();

    /** Wraki do postawienia: świat → klucz chunka → lista (z punktów AxonMapGenerator). */
    private final Map<String, Map<Long, List<Planned>>> planned = new HashMap<>();

    private VehicleConfig config = VehicleConfig.defaults();

    public VehicleService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modelKey = new NamespacedKey(plugin, MODEL_TAG);
        this.lootKey = new NamespacedKey(plugin, LOOT_TAG);
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
     * Stawia wraki na liście punktów pobocza zgłoszonych przez AxonMapGenerator.
     *
     * <p>AxonMapGenerator woła to przy starcie serwera, kiedy prawie żaden chunk przy drogach nie jest
     * wczytany. Dlatego wraki są tu tylko losowane (z seeda świata, więc tak samo po każdym restarcie),
     * a stawiane w chunkach już wczytanych i potem przy wczytaniu kolejnych ({@link EntitiesLoadEvent}).
     * Wrak, który już stoi w świecie, zostaje; powtórne wywołanie podmienia tylko plan.
     *
     * @return liczba zaplanowanych wraków
     */
    public int placeAlongRoads(World world, List<RoadsideSpot> spots) {
        if (world == null || spots == null || spots.isEmpty()) {
            return 0;
        }
        if (!config.inWorld(world.getName())) {
            return 0;
        }
        // Losowanie z seeda świata: po restarcie te same punkty dostają te same wraki, więc wrak już
        // stojący w świecie (encje są trwałe) zostaje, a brakujący dostawiamy przy wczytaniu chunka.
        Random roll = new Random(world.getSeed() ^ 0x57524B5FL);
        Map<Long, List<Planned>> chunks = new HashMap<>();
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
            chunks.computeIfAbsent(chunkKey(spot.x() >> 4, spot.z() >> 4), k -> new ArrayList<>())
                    .add(new Planned(spot, model));
            count++;
            lastX = spot.x();
            lastZ = spot.z();
        }
        planned.put(world.getName(), chunks);

        // Chunki już wczytane: od razu (o ile ich encje też są już wczytane, inaczej zrobi to EntitiesLoadEvent).
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.isEntitiesLoaded()) {
                ensure(chunk, List.of(chunk.getEntities()));
            }
        }
        plugin.getLogger().info("Wraki przy drogach w świecie " + world.getName() + ": " + count
                + " miejsc, stawiane przy wczytaniu chunków.");
        return count;
    }

    /** Wraki zaplanowane w świecie (do diagnostyki). */
    public int plannedCount(World world) {
        Map<Long, List<Planned>> chunks = world == null ? null : planned.get(world.getName());
        if (chunks == null) {
            return 0;
        }
        int n = 0;
        for (List<Planned> list : chunks.values()) {
            n += list.size();
        }
        return n;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        Chunk chunk = event.getChunk();
        if (plannedIn(chunk).isEmpty()) {
            return;
        }
        List<Entity> entities = List.copyOf(event.getEntities());
        // w następnym ticku: stawianie encji w trakcie wczytywania chunka bywa gubione
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (chunk.isLoaded()) {
                ensure(chunk, entities);
            }
        });
    }

    /** Dostawia w chunku zaplanowane wraki, których jeszcze nie ma (wrak sprzed restartu zostaje). */
    private void ensure(Chunk chunk, List<Entity> entities) {
        World world = chunk.getWorld();
        for (Planned plan : plannedIn(chunk)) {
            RoadsideSpot spot = plan.spot();
            if (hasWreckNear(entities, spot.x() + 0.5, spot.z() + 0.5)) {
                continue;
            }
            place(world, spot, plan.model());
        }
    }

    private boolean hasWreckNear(List<Entity> entities, double x, double z) {
        for (Entity entity : entities) {
            if (!entity.isValid() || !isWreckEntity(entity)) {
                continue;
            }
            Location at = entity.getLocation();
            if (Math.abs(at.getX() - x) <= 1.5 && Math.abs(at.getZ() - z) <= 1.5) {
                return true;
            }
        }
        return false;
    }

    private List<Planned> plannedIn(Chunk chunk) {
        Map<Long, List<Planned>> chunks = planned.get(chunk.getWorld().getName());
        if (chunks == null) {
            return List.of();
        }
        List<Planned> list = chunks.get(chunkKey(chunk.getX(), chunk.getZ()));
        return list == null ? List.of() : list;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /**
     * Stawia jeden wrak w punkcie pobocza. Grunt bierzemy z najwyższego nie-powietrznego bloku
     * w kolumnie (bez ładowania chunków — jeśli chunk nie jest wczytany, pomijamy).
     */
    public boolean place(World world, RoadsideSpot spot, VehicleModel model) {
        if (world == null || spot == null || model == null) {
            return false;
        }
        int chunkX = spot.x() >> 4;
        int chunkZ = spot.z() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }
        int groundY = groundY(world, spot.x(), spot.z());
        if (groundY == Integer.MIN_VALUE) {
            return false;
        }
        int steps = spot.steps();
        // Wrak obrócony „wzdłuż drogi”: RoadsideSpot.steps() obraca yaw (przód do drogi) o 90°.
        return place(world, spot.x(), groundY, spot.z(), steps, model);
    }

    /**
     * Stawia wrak w konkretnych współrzędnych (używane też przez komendę {@code /struktury wrak}).
     *
     * @param groundY blok gruntu — model i bariery startują od {@code groundY + 1}
     */
    public boolean place(World world, int x, int groundY, int z, int steps, VehicleModel model) {
        if (world == null || model == null) {
            return false;
        }
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        String key = key(world, x, groundY, z);
        if (placed.containsKey(key)) {
            return false;
        }
        List<Entity> spawned = new ArrayList<>();
        try {
            // Paczka ma jeden model na cały pojazd (axon:wrak_*), więc jeden wyświetlacz w środku obrysu.
            // Kilka części z tym samym modelem dawało nałożone na siebie kopie auta.
            {
                double px = x + 0.5;
                double pz = z + 0.5;
                Location base = new Location(world, px, groundY + model.centerHeight(), pz);
                float yaw = steps * 90f;
                ItemDisplay display = world.spawn(base, ItemDisplay.class, entity -> {
                    entity.setItemStack(modelItem(model.id()));
                    entity.setBillboard(Display.Billboard.FIXED);
                    entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    entity.setTransformation(new Transformation(
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f(0f, 0f, 1f, 0f),
                            new Vector3f(1f, 1f, 1f),
                            new AxisAngle4f(0f, 0f, 1f, 0f)));
                    entity.setRotation(yaw, 0f);
                    entity.setPersistent(true);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    tag(entity.getPersistentDataContainer(), model.id(), "");
                });
                spawned.add(display);
            }

            // Hitbox bagażnika — jeden na wrak, w środku obrysu.
            Location trunk = new Location(world, x + 0.5, groundY + 1.0, z + 0.5);
            Interaction hitbox = world.spawn(trunk, Interaction.class, entity -> {
                entity.setInteractionWidth(1.0f);
                entity.setInteractionHeight(1.0f);
                entity.setResponsive(true);
                entity.setPersistent(true);
                entity.setInvulnerable(true);
                tag(entity.getPersistentDataContainer(), model.id(), model.lootTable());
            });
            spawned.add(hitbox);

            List<int[]> barriers = new ArrayList<>();
            if (config.barriers()) {
                BlockData barrier = Material.BARRIER.createBlockData();
                for (int[] pos : VehicleMath.collisionBlocks(model, x, groundY, z, steps)) {
                    Block block = world.getBlockAt(pos[0], pos[1], pos[2]);
                    if (block.getType().isAir()) {
                        block.setBlockData(barrier, false);
                        barriers.add(pos);
                    }
                }
            }

            placed.put(key, new Placed(world.getName(), x, groundY, z, steps, model.id(),
                    spawned.stream().map(Entity::getUniqueId).toList(), List.copyOf(barriers)));
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Nie udało się postawić wraku " + model.id() + " na "
                    + x + " " + groundY + " " + z + ": " + e.getMessage());
            for (Entity entity : spawned) {
                entity.remove();
            }
            return false;
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
        for (UUID id : record.entities()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                if (entity instanceof Interaction) {
                    contents.remove(id);
                }
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
        contents.put(trunk.hitbox.getUniqueId(), snapshot);
    }

    private void openTrunk(Player player, Interaction hitbox, String table) {
        TrunkHolder holder = new TrunkHolder(hitbox.getUniqueId(), "&8Bagażnik wraku");
        Inventory inventory = Bukkit.createInventory(holder, TRUNK_SLOTS, "Bagażnik wraku");
        holder.inventory = inventory;
        ItemStack[] stored = contents.get(hitbox.getUniqueId());
        if (stored == null) {
            stored = rollLoot(table);
            inventory.setContents(stored);
            contents.put(hitbox.getUniqueId(), stored);
        } else {
            inventory.setContents(stored);
        }
        holder.contents = stored;
        open.put(player.getUniqueId(), new OpenTrunk(holder, hitbox));
        player.openInventory(inventory);
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

    /** Najwyższy nie-powietrzny blok w kolumnie albo {@code Integer.MIN_VALUE}, gdy brak. */
    private static int groundY(World world, int x, int z) {
        int max = world.getMaxHeight() - 1;
        int min = world.getMinHeight();
        for (int y = max; y >= min; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (!type.isAir() && type != Material.WATER && type != Material.LAVA) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void tag(PersistentDataContainer pdc, String modelId, String lootTable) {
        pdc.set(modelKey, PersistentDataType.STRING, modelId == null ? "" : modelId);
        if (lootTable != null && !lootTable.isBlank()) {
            pdc.set(lootKey, PersistentDataType.STRING, lootTable);
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

    private static String key(World world, int x, int y, int z) {
        return (world == null ? "?" : world.getName()) + ":" + x + ":" + y + ":" + z;
    }

    /** Opis postawionego wraku. */
    public record Placed(String world, int x, int groundY, int z, int steps, String modelId,
                         List<UUID> entities, List<int[]> barriers) {
    }

    /** Wrak wylosowany dla punktu pobocza, stawiany gdy chunk się wczyta. */
    private record Planned(RoadsideSpot spot, VehicleModel model) {
    }

    private record OpenTrunk(TrunkHolder holder, Interaction hitbox) {
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
