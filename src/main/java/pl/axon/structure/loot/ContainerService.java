package pl.axon.structure.loot;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.api.RoadsideSpot;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Pojemniki z lootem bez prawdziwego bloku: model z paczki zasobów (ItemDisplay) i niewidzialna encja Interaction,
 * która przyjmuje uderzenia i kliknięcia. Beczki się rozbija (model zmienia się na rozbity), a PPM otwiera
 * wirtualny ekwipunek; skrzynie otwiera się od razu. Po czasie pojemnik wraca w tym samym miejscu, ale nie wtedy,
 * gdy obok stoi gracz.
 *
 * <p>Encje nie zapisują się w świecie ({@code persistent: false}): stają przy wczytaniu chunka i znikają przy
 * jego zwolnieniu, więc nigdy nie zostają po usuniętym pojemniku ani po wyłączeniu pluginu.
 */
public final class ContainerService implements Listener {

    /**
     * @param respawnMillis    po jakim czasie od rozbicia (beczka) albo opróżnienia (skrzynia) pojemnik wraca
     * @param brokenMillis     po jakim czasie rozbita beczka znika, nawet z lootem w środku
     * @param playerBlockRadius nie odnawiaj, gdy gracz jest bliżej niż tyle bloków (0 = zawsze odnawiaj)
     * @param roadPool         pula pojemników przy drogach
     */
    public record Settings(long respawnMillis, long brokenMillis, int playerBlockRadius, String roadPool) {
    }

    private static final String TAG = "axs_pojemnik";
    private static final String ROAD_PREFIX = "droga:";
    private static final String COMMAND_PREFIX = "komenda:";
    private static final long HIT_COOLDOWN_MS = 200;
    private static final long OPEN_COOLDOWN_MS = 250;

    private final JavaPlugin plugin;
    private final CrateService crates;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final Random random = new Random();
    private final Map<String, ContainerSpot> spots = new LinkedHashMap<>();
    private final Map<String, Map<Long, List<ContainerSpot>>> byChunk = new HashMap<>();
    private final Map<UUID, ContainerSpot> byEntity = new HashMap<>();
    private final Map<UUID, Long> lastOpen = new HashMap<>();
    private Settings settings = new Settings(10 * 60_000L, 5 * 60_000L, 24, "droga");
    private BukkitTask task;
    private int commandCounter;

    public ContainerService(JavaPlugin plugin, CrateService crates, StructureLibrary library, InstanceStore store) {
        this.plugin = plugin;
        this.crates = crates;
        this.library = library;
        this.store = store;
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    public void start() {
        stop();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(TAG)) {
                    entity.remove(); // po /reload albo awarii
                }
            }
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
        for (ContainerSpot spot : List.copyOf(spots.values())) {
            showIfLoaded(spot);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ContainerSpot spot : spots.values()) {
            closeViewers(spot);
            despawnEntities(spot);
        }
    }

    private boolean running() {
        return task != null;
    }

    // ------------------------------------------------------------------ rejestracja miejsc

    public boolean has(String key) {
        return spots.containsKey(key);
    }

    /**
     * Pojemnik z już wylosowaną klasą w punkcie struktury (stawianie i odświeżanie przez {@link CrateService}).
     * Zastępuje to, co stało pod tym kluczem.
     */
    public void place(String key, String world, int x, int y, int z, float yaw, String pool, String owner,
                      CrateClass crate) {
        remove(key);
        ContainerSpot spot = new ContainerSpot(key, world, x, y, z, yaw, pool, null, owner, false);
        add(spot);
        activate(spot, crate);
        showIfLoaded(spot);
    }

    /** Punkt struktury po restarcie: klasa losuje się z puli, gdy chunk się wczyta. */
    public void register(String key, String world, int x, int y, int z, float yaw, String pool, String owner) {
        if (spots.containsKey(key)) {
            return;
        }
        add(new ContainerSpot(key, world, x, y, z, yaw, pool, null, owner, false));
    }

    /** Losuje pojemnik od nowa (komenda {@code /struktury odswiez}). Zwraca, czy coś stoi. */
    public boolean reroll(String key) {
        ContainerSpot spot = spots.get(key);
        if (spot == null) {
            return false;
        }
        closeViewers(spot);
        despawnEntities(spot);
        spot.state = ContainerSpot.State.WAITING;
        spot.respawnAt = 0;
        respawn(spot, System.currentTimeMillis(), true, false);
        return spot.visible();
    }

    /** Pojemniki przy drogach w świecie (z AxonMapGenerator). Zastępuje poprzednią listę dla tego świata. */
    public int setRoadside(String world, Collection<RoadsideSpot> list) {
        Set<String> keep = new HashSet<>();
        for (RoadsideSpot r : list) {
            String key = ROAD_PREFIX + world + ":" + r.x() + ":" + r.z();
            if (!keep.add(key) || spots.containsKey(key)) {
                continue;
            }
            ContainerSpot spot = new ContainerSpot(key, world, r.x(), ContainerSpot.ON_TERRAIN, r.z(), r.yaw(),
                    settings.roadPool(), null, null, true);
            add(spot);
            showIfLoaded(spot);
        }
        String prefix = ROAD_PREFIX + world + ":";
        for (String key : List.copyOf(spots.keySet())) {
            if (key.startsWith(prefix) && !keep.contains(key)) {
                remove(key);
            }
        }
        return keep.size();
    }

    /** Pojemnik o stałej klasie postawiony komendą (do testów modeli). Znika po restarcie. */
    public void placeCommand(CrateClass crate, Location at, float yaw) {
        ContainerSpot spot = new ContainerSpot(COMMAND_PREFIX + (++commandCounter), at.getWorld().getName(),
                at.getBlockX(), at.getBlockY(), at.getBlockZ(), yaw, null, crate.id(), null, false);
        add(spot);
        activate(spot, crate);
        showIfLoaded(spot);
    }

    public void removeOwner(String owner) {
        for (ContainerSpot spot : List.copyOf(spots.values())) {
            if (owner.equals(spot.owner)) {
                remove(spot.key);
            }
        }
    }

    public void removeWorld(String world) {
        for (ContainerSpot spot : List.copyOf(spots.values())) {
            if (spot.world.equals(world)) {
                remove(spot.key);
            }
        }
    }

    public void remove(String key) {
        ContainerSpot spot = spots.remove(key);
        if (spot == null) {
            return;
        }
        closeViewers(spot);
        despawnEntities(spot);
        Map<Long, List<ContainerSpot>> chunks = byChunk.get(spot.world);
        if (chunks != null) {
            List<ContainerSpot> list = chunks.get(chunkKey(spot.chunkX(), spot.chunkZ()));
            if (list != null) {
                list.remove(spot);
            }
        }
    }

    private void add(ContainerSpot spot) {
        spots.put(spot.key, spot);
        byChunk.computeIfAbsent(spot.world, w -> new HashMap<>())
                .computeIfAbsent(chunkKey(spot.chunkX(), spot.chunkZ()), k -> new ArrayList<>()).add(spot);
    }

    /** Linie do {@code /struktury pojemniki}. */
    public List<String> status() {
        int road = 0, structure = 0, intact = 0, closed = 0, broken = 0, waiting = 0, dead = 0, shown = 0;
        for (ContainerSpot spot : spots.values()) {
            if (spot.roadside) {
                road++;
            } else if (spot.owner != null) {
                structure++;
            }
            if (spot.dead) {
                dead++;
                continue;
            }
            switch (spot.state) {
                case INTACT -> intact++;
                case CLOSED -> closed++;
                case BROKEN -> broken++;
                case WAITING -> waiting++;
            }
            if (!spot.entities.isEmpty()) {
                shown++;
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("§6Pojemniki-modele: §f" + spots.size() + " §7(przy drogach " + road + ", w strukturach " + structure
                + ", z komendy " + (spots.size() - road - structure) + ")");
        lines.add("§7Całe beczki: §f" + intact + "§7, skrzynie: §f" + closed + "§7, rozbite: §f" + broken
                + "§7, czekają na odnowienie: §f" + waiting + (dead > 0 ? "§7, nie da się postawić: §f" + dead : ""));
        lines.add("§7Stoi teraz we wczytanych chunkach: §f" + shown + "§7. Odnowienie co §f"
                + settings.respawnMillis() / 60_000L + " min§7, rozbita znika po §f" + settings.brokenMillis() / 60_000L
                + " min§7, pula przy drogach: §f" + settings.roadPool());
        if (!AxonItemsBridge.available()) {
            lines.add("§cAxonItems nie działa: przedmioty axon:... w lootcie są pomijane.");
        }
        return lines;
    }

    // ------------------------------------------------------------------ losowanie i odnawianie

    private CrateClass roll(ContainerSpot spot) {
        CrateClass crate = spot.fixedClass != null ? crates.loot().crateClass(spot.fixedClass)
                : crates.pickClass(spot.pool);
        // klasy z prawdziwym blokiem stawia tylko CrateService; tutaj liczą się jak „brak”
        return crate != null && crate.kind() == CrateClass.Kind.MODEL ? crate : null;
    }

    private void activate(ContainerSpot spot, CrateClass crate) {
        spot.crate = crate;
        spot.inventory = null;
        spot.damage = 0;
        spot.emptiedAt = 0;
        spot.state = crate.container().breakable() ? ContainerSpot.State.INTACT : ContainerSpot.State.CLOSED;
    }

    private long respawnOf(CrateClass crate) {
        long own = crate == null ? -1 : crate.container().respawnMillis();
        return own > 0 ? own : settings.respawnMillis();
    }

    /** Losuje pojemnik w pustym miejscu. {@code force} pomija sprawdzanie graczy obok. */
    private void respawn(ContainerSpot spot, long now, boolean force, boolean announce) {
        if (!force && playerNearby(spot)) {
            return;
        }
        CrateClass crate = roll(spot);
        if (crate == null) {
            spot.state = ContainerSpot.State.WAITING;
            spot.crate = null;
            spot.respawnAt = now + settings.respawnMillis(); // wypadło „brak”: spróbujemy przy następnym odnowieniu
            return;
        }
        activate(spot, crate);
        showIfLoaded(spot);
        if (announce && !crate.container().announce().isBlank() && !spot.entities.isEmpty()) {
            announce(spot, crate);
        }
    }

    private void announce(ContainerSpot spot, CrateClass crate) {
        World world = Bukkit.getWorld(spot.world);
        if (world == null) {
            return;
        }
        String place = "przy drodze";
        if (spot.owner != null) {
            StructureInstance instance = store.get(spot.owner);
            Blueprint bp = instance == null ? null : library.get(instance.structureId());
            place = bp == null ? "w strukturze" : bp.name();
        }
        String text = crate.container().announce().replace("{nazwa}", crate.displayName()).replace("{miejsce}", place);
        for (Player player : world.getPlayers()) {
            player.sendMessage(text);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (ContainerSpot spot : List.copyOf(spots.values())) {
            if (spot.dead || !loaded(spot)) {
                continue;
            }
            switch (spot.state) {
                case WAITING -> {
                    if (now >= spot.respawnAt) {
                        respawn(spot, now, spot.respawnAt == 0, spot.respawnAt != 0);
                    }
                }
                case BROKEN -> {
                    if (now - spot.brokenAt >= settings.brokenMillis()) {
                        vanish(spot);
                    }
                }
                case CLOSED -> {
                    if (spot.emptiedAt > 0 && now - spot.emptiedAt >= respawnOf(spot.crate) && !playerNearby(spot)) {
                        despawnEntities(spot);
                        spot.state = ContainerSpot.State.WAITING;
                        respawn(spot, now, true, true);
                    }
                }
                default -> {
                }
            }
            if (spot.visible()) {
                if (spot.display != null && Bukkit.getEntity(spot.display) == null) {
                    despawnEntities(spot); // ktoś usunął encje (np. /kill)
                }
                if (spot.entities.isEmpty()) {
                    showIfLoaded(spot); // np. blok zasłaniał miejsce przy wczytaniu chunka
                }
            }
        }
    }

    /** Rozbita beczka znika (pusta albo po czasie); wraca po czasie odnowienia liczonym od rozbicia. */
    private void vanish(ContainerSpot spot) {
        closeViewers(spot);
        despawnEntities(spot);
        spot.respawnAt = spot.brokenAt + respawnOf(spot.crate);
        spot.state = ContainerSpot.State.WAITING;
        spot.inventory = null;
        spot.crate = null;
    }

    private boolean playerNearby(ContainerSpot spot) {
        int radius = settings.playerBlockRadius();
        World world = Bukkit.getWorld(spot.world);
        if (radius <= 0 || world == null) {
            return false;
        }
        double limit = (double) radius * radius;
        for (Player player : world.getPlayers()) {
            Location l = player.getLocation();
            double dx = l.getX() - (spot.x + 0.5);
            double dz = l.getZ() - (spot.z + 0.5);
            if (dx * dx + dz * dz < limit) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ encje

    private boolean loaded(ContainerSpot spot) {
        World world = Bukkit.getWorld(spot.world);
        return world != null && world.isChunkLoaded(spot.chunkX(), spot.chunkZ());
    }

    private void showIfLoaded(ContainerSpot spot) {
        if (!running() || !loaded(spot)) {
            return;
        }
        if (spot.state == ContainerSpot.State.WAITING) {
            if (spot.respawnAt == 0) {
                respawn(spot, System.currentTimeMillis(), true, false); // pierwsze postawienie po starcie
            }
            return;
        }
        if (spot.entities.isEmpty()) {
            spawnEntities(spot);
        }
    }

    private void spawnEntities(ContainerSpot spot) {
        World world = Bukkit.getWorld(spot.world);
        if (world == null || spot.crate == null || spot.dead) {
            return;
        }
        if (spot.y == ContainerSpot.ON_TERRAIN) {
            int top = world.getHighestBlockYAt(spot.x, spot.z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block ground = world.getBlockAt(spot.x, top, spot.z);
            if (ground.isLiquid() || !ground.getType().isSolid() || top + 1 >= world.getMaxHeight()) {
                spot.dead = true; // woda, lód na rzece, dziura – tu beczka nie stanie
                return;
            }
            spot.y = top + 1;
        }
        CrateClass.ContainerSpec spec = spot.crate.container();
        int[][] cells = cells(spot, spec.length());
        for (int[] cell : cells) {
            Block block = world.getBlockAt(cell[0], spot.y, cell[1]);
            if (!block.isPassable() || block.isLiquid()) {
                return; // coś tu stoi (blok gracza, stara skrzynia) – spróbujemy przy następnym wczytaniu
            }
        }
        despawnEntities(spot);
        double cx = 0;
        double cz = 0;
        for (int[] cell : cells) {
            cx += cell[0] + 0.5;
            cz += cell[1] + 0.5;
            Location base = new Location(world, cell[0] + 0.5, spot.y, cell[1] + 0.5);
            Interaction interaction = world.spawn(base, Interaction.class, i -> {
                i.setInteractionWidth(spec.width());
                i.setInteractionHeight(spec.height());
                i.setResponsive(true);
                i.addScoreboardTag(TAG);
                i.setPersistent(false);
            });
            track(spot, interaction);
        }
        Location center = new Location(world, cx / cells.length, spot.y + 0.5, cz / cells.length);
        // jak meble: przód modelu patrzy na południe, a wyświetlacz sam obraca model o 180°
        center.setYaw(spot.yaw + 180f);
        center.setPitch(0f);
        ItemStack item = modelItem(spot.state == ContainerSpot.State.BROKEN ? spec.brokenModel() : spot.crate.model());
        float size = spec.length() + 1f;
        ItemDisplay display = world.spawn(center, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1f, 1f, 1f),
                    new Quaternionf()));
            d.setDisplayWidth(size);
            d.setDisplayHeight(size);
            d.setViewRange(Math.max(1f, size / 8f));
            d.addScoreboardTag(TAG);
            d.setPersistent(false);
        });
        track(spot, display);
        spot.display = display.getUniqueId();
    }

    /**
     * Bloki zajmowane przez pojemnik: pierwszy to punkt, kolejne idą wzdłuż osi -x modelu (skrzynia wojskowa po
     * obróceniu w paczce leży od -16 do 16 pikseli), obróconej razem z modelem.
     */
    private static int[][] cells(ContainerSpot spot, int length) {
        int[][] cells = new int[length][];
        int quarter = Math.floorMod(Math.round(spot.yaw / 90f), 4);
        for (int i = 0; i < length; i++) {
            int lx = -i;
            int lz = 0;
            for (int q = 0; q < quarter; q++) { // obrót o 90° jak yaw: (x, z) -> (-z, x)
                int t = lx;
                lx = -lz;
                lz = t;
            }
            cells[i] = new int[]{spot.x + lx, spot.z + lz};
        }
        return cells;
    }

    private ItemStack modelItem(String model) {
        ItemStack item = new ItemStack(Material.PAPER);
        NamespacedKey key = NamespacedKey.fromString(model);
        if (key == null) {
            plugin.getLogger().warning("Zły model pojemnika: " + model);
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(key);
        item.setItemMeta(meta);
        return item;
    }

    private void track(ContainerSpot spot, Entity entity) {
        spot.entities.add(entity.getUniqueId());
        byEntity.put(entity.getUniqueId(), spot);
    }

    private void despawnEntities(ContainerSpot spot) {
        for (UUID id : spot.entities) {
            byEntity.remove(id);
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        spot.entities.clear();
        spot.display = null;
    }

    private void closeViewers(ContainerSpot spot) {
        if (spot.inventory != null) {
            for (HumanEntity viewer : List.copyOf(spot.inventory.getViewers())) {
                viewer.closeInventory();
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        List<ContainerSpot> list = spotsIn(event.getChunk());
        if (list.isEmpty()) {
            return;
        }
        // w następnym ticku: stawianie encji w trakcie wczytywania chunka bywa gubione
        List<ContainerSpot> copy = List.copyOf(list);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (ContainerSpot spot : copy) {
                if (spots.get(spot.key) == spot) {
                    showIfLoaded(spot);
                }
            }
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (ContainerSpot spot : spotsIn(event.getChunk())) {
            closeViewers(spot);
            despawnEntities(spot);
        }
    }

    private List<ContainerSpot> spotsIn(Chunk chunk) {
        Map<Long, List<ContainerSpot>> chunks = byChunk.get(chunk.getWorld().getName());
        if (chunks == null) {
            return List.of();
        }
        List<ContainerSpot> list = chunks.get(chunkKey(chunk.getX(), chunk.getZ()));
        return list == null ? List.of() : list;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    // ------------------------------------------------------------------ gracze

    /** Uderzenie w encję Interaction. Paper woła oba zdarzenia, powtórkę odsiewa {@link #HIT_COOLDOWN_MS}. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreAttack(PrePlayerAttackEntityEvent event) {
        ContainerSpot spot = byEntity.get(event.getAttacked().getUniqueId());
        if (spot != null) {
            event.setCancelled(true);
            hit(event.getPlayer(), spot);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        ContainerSpot spot = byEntity.get(event.getEntity().getUniqueId());
        if (spot != null) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                hit(player, spot);
            }
        }
    }

    private void hit(Player player, ContainerSpot spot) {
        if (spot.state != ContainerSpot.State.INTACT || spot.crate == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - spot.lastHit < HIT_COOLDOWN_MS) {
            return;
        }
        spot.lastHit = now;
        CrateClass.ContainerSpec spec = spot.crate.container();
        boolean tool = !player.getInventory().getItemInMainHand().getType().isAir();
        spot.damage += 1.0 / (tool ? spec.toolHits() : spec.hits());
        Location at = new Location(player.getWorld(), spot.x + 0.5, spot.y + 0.6, spot.z + 0.5);
        if (spot.damage < 0.999) {
            at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.4f);
            at.getWorld().spawnParticle(Particle.BLOCK, at, 8, 0.25, 0.3, 0.25, Material.IRON_BLOCK.createBlockData());
            int left = (int) Math.ceil((1.0 - spot.damage) * (tool ? spec.toolHits() : spec.hits()) - 1e-6);
            actionBar(player, "§7" + spot.crate.displayName() + "§7: jeszcze §f" + left + "§7 "
                    + (left == 1 ? "uderzenie" : "uderzenia"));
            return;
        }
        spot.state = ContainerSpot.State.BROKEN;
        spot.brokenAt = now;
        spot.inventory = newInventory(spot);
        if (!crates.emptyLoot()) {
            spot.crate.fill(spot.inventory, random); // loot losuje się w chwili rozbicia
        }
        Entity display = spot.display == null ? null : Bukkit.getEntity(spot.display);
        if (display instanceof ItemDisplay itemDisplay) {
            itemDisplay.setItemStack(modelItem(spec.brokenModel()));
        }
        at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.7f, 1.3f);
        at.getWorld().spawnParticle(Particle.BLOCK, at, 30, 0.35, 0.35, 0.35, Material.IRON_BLOCK.createBlockData());
        actionBar(player, "§a" + spot.crate.displayName() + "§a rozbita. §7PPM, żeby przeszukać.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        ContainerSpot spot = byEntity.get(event.getRightClicked().getUniqueId());
        if (spot == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            open(event.getPlayer(), spot);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (byEntity.containsKey(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void open(Player player, ContainerSpot spot) {
        long now = System.currentTimeMillis();
        Long last = lastOpen.get(player.getUniqueId());
        if (last != null && now - last < OPEN_COOLDOWN_MS) {
            return;
        }
        lastOpen.put(player.getUniqueId(), now);
        switch (spot.state) {
            case INTACT -> actionBar(player, "§7Najpierw rozbij: §fLPM§7.");
            case CLOSED -> {
                if (spot.inventory == null) {
                    spot.inventory = newInventory(spot);
                    if (!crates.emptyLoot()) {
                        spot.crate.fill(spot.inventory, random); // przy pierwszym otwarciu, nie da się podejrzeć z daleka
                    }
                }
                player.openInventory(spot.inventory);
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.6f, 1.1f);
            }
            case BROKEN -> {
                if (spot.inventory != null) {
                    player.openInventory(spot.inventory);
                }
            }
            default -> {
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static Inventory newInventory(ContainerSpot spot) {
        return Bukkit.createInventory(spot, 27, spot.crate.displayName());
    }

    /** Z pojemnika można tylko brać: nie da się w nim nic schować. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ContainerSpot)) {
            return;
        }
        boolean top = event.getClickedInventory() == event.getView().getTopInventory();
        boolean blocked = switch (event.getAction()) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> top;
            case MOVE_TO_OTHER_INVENTORY -> !top;
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> top && event.getHotbarButton() >= 0
                    && event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) != null;
            case UNKNOWN -> true;
            default -> false;
        };
        if (blocked) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ContainerSpot)) {
            return;
        }
        int size = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < size) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ContainerSpot spot) || spots.get(spot.key) != spot
                || spot.inventory != event.getInventory() || !spot.inventory.isEmpty()) {
            return;
        }
        if (spot.inventory.getViewers().size() > 1) {
            return; // ktoś jeszcze w nim grzebie
        }
        if (spot.state == ContainerSpot.State.BROKEN) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (spots.get(spot.key) == spot && spot.state == ContainerSpot.State.BROKEN) {
                    vanish(spot);
                }
            });
        } else if (spot.state == ContainerSpot.State.CLOSED && spot.emptiedAt == 0) {
            spot.emptiedAt = System.currentTimeMillis();
        }
    }

    private static void actionBar(Player player, String text) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }
}
