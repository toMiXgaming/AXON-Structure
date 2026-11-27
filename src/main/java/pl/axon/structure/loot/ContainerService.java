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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.api.RoadsideSpot;
import pl.axon.structure.api.StructureContainerEvent;
import pl.axon.structure.api.StructureContainerEvent.Phase;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.util.ArrayDeque;
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
 * która przyjmuje uderzenia i kliknięcia. Beczki mają HP i się je rozbija (model zmienia się na rozbity), a PPM otwiera
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
     * @param damage           obrażenia przedmiotów przy rozbijaniu beczek ({@code pojemniki.niszczenie})
     * @param hpBarMillis      jak długo pasek HP wisi nad beczką po ostatnim uderzeniu (0 = bez paska nad beczką)
     */
    public record Settings(long respawnMillis, long brokenMillis, int playerBlockRadius, String roadPool,
                           BarrelMath.Damage damage, long hpBarMillis) {
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
    private LootStats stats;
    private final Map<String, Map<Long, List<ContainerSpot>>> byChunk = new HashMap<>();
    private final Map<UUID, ContainerSpot> byEntity = new HashMap<>();
    private record WarnedContainer(Object key, UUID speaker) {}
    private final Map<UUID, WarnedContainer> warnedContainers = new HashMap<>();

    // Finish a player's transaction before another viewer can change this same chest.
    private final Map<Object, Runnable> pendingLoot = new HashMap<>();
    private final Map<UUID, CursorLoot> cursorLoot = new HashMap<>();

    private record CursorLoot(Object key, ItemStack type, ArrayDeque<Take> takes) {}

    private record Take(StructureInstance owner, Player player, Location at, UUID transaction, int amount) {
        void report(Phase phase, int quantity) {
            publish(owner, player, at, phase, transaction, quantity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void warnAtContainer(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        warnedContainers.remove(player.getUniqueId());
        Inventory top = event.getInventory();
        if (!(top.getHolder() instanceof ContainerSpot || top.getHolder() instanceof org.bukkit.block.Container
                || top.getHolder() instanceof org.bukkit.block.DoubleChest)) return;
        ContainerSpot spot = top.getHolder() instanceof ContainerSpot model ? model : null;
        StructureInstance owner = ownerOf(top, spot);
        if (owner == null) return;
        if (crates.usesNpcRotation(owner.id())) {
            publish(owner, player, locationOf(top, spot, player), Phase.OPEN, null, 0);
            return;
        }
        if (!owner.farmCycle() || !owner.scrapSite()) return;
        UUID speaker = pl.axon.structure.npc.GuardService.scrapChest(owner.id(), player, null, true);
        Object key = top.getHolder() instanceof ContainerSpot model ? model : top.getLocation();
        if (speaker != null && key != null) warnedContainers.put(player.getUniqueId(), new WarnedContainer(key, speaker));
    }

    private final Map<UUID, Long> lastOpen = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();
    private Settings settings = new Settings(10 * 60_000L, 5 * 60_000L, 24, "droga", BarrelMath.Damage.DEFAULT,
            4000L);
    private BukkitTask task;
    private int commandCounter;

    public ContainerService(JavaPlugin plugin, CrateService crates, StructureLibrary library, InstanceStore store) {
        this.plugin = plugin;
        this.crates = crates;
        this.library = library;
        this.store = store;
    }

    /** Statystyki lootu ({@code statystyki.yml}); null = nie liczymy. */
    public void stats(LootStats stats) {
        this.stats = stats;
    }

    private void record(Player player, ContainerSpot spot) {
        if (stats != null && spot.fixedClass == null) {
            stats.record(player, LootStats.source("pojemnik", tierOf(spot), spot.roadside),
                    java.util.Arrays.asList(spot.inventory.getContents()));
        }
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
        warnedContainers.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ContainerSpot spot : spots.values()) {
            closeViewers(spot);
            despawnEntities(spot);
        }
    }

    private boolean saveQueued;
    private void saveSoon() {
        if (saveQueued || !plugin.isEnabled()) return;
        saveQueued = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            saveQueued = false;
            store.save();
        });
    }

    private StructureInstance farm(ContainerSpot spot) {
        StructureInstance owner = spot.owner == null ? null : store.get(spot.owner);
        return owner != null && owner.farmCycle() ? owner : null;
    }

    private boolean usesStructureRotation(ContainerSpot spot) {
        return farm(spot) != null || (spot.owner != null && crates.usesNpcRotation(spot.owner));
    }

    private void snapshot(ContainerSpot spot) {
        StructureInstance owner = spot.owner == null ? null : store.get(spot.owner);
        if (!usesStructureRotation(spot)) return;
        if (owner == null) return;
        var data = new org.bukkit.configuration.MemoryConfiguration();
        data.set("stan", spot.state.name());
        data.set("klasa", spot.crate == null ? null : spot.crate.id());
        data.set("hp", spot.hp);
        data.set("rozbito", spot.brokenAt);
        data.set("blokada", spot.lockedUntil);
        if (spot.inventory != null) {
            data.set("sloty", java.util.Arrays.asList(spot.inventory.getContents()));
        }
        owner.farmContainers.put(spot.key, data);
        saveSoon();
    }

    private void restore(ContainerSpot spot) {
        StructureInstance owner = spot.owner == null ? null : store.get(spot.owner);
        if (owner == null) return;
        var data = owner.farmContainers.get(spot.key);
        if (data == null) return;
        spot.state = ContainerSpot.State.valueOf(data.getString("stan", "WAITING"));
        spot.respawnAt = Long.MAX_VALUE;
        if (spot.state == ContainerSpot.State.WAITING) return;
        spot.crate = crates.loot().crateClass(data.getString("klasa", ""));
        if (spot.crate == null || spot.crate.container() == null) {
            spot.state = ContainerSpot.State.WAITING;
            return;
        }
        spot.hp = data.getDouble("hp");
        spot.brokenAt = data.getLong("rozbito");
        spot.lockedUntil = data.getLong("blokada");
        if (data.isList("sloty")) {
            spot.inventory = newInventory(spot);
            var items = data.getList("sloty");
            for (int i = 0; i < Math.min(items.size(), spot.inventory.getSize()); i++) {
                if (items.get(i) instanceof ItemStack item) spot.inventory.setItem(i, item);
            }
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
        ContainerSpot spot = new ContainerSpot(key, world, x, y, z, yaw, pool, null, owner, false);
        restore(spot);
        add(spot);
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
        Map<Integer, Integer> byTier = new java.util.TreeMap<>();
        for (ContainerSpot spot : spots.values()) {
            byTier.merge(tierOf(spot).level(), 1, Integer::sum);
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
        if (crates.loot().tiers().enabled() && !byTier.isEmpty()) {
            StringBuilder tiers = new StringBuilder("§7Tiery miejsc: ");
            byTier.forEach((level, n) -> tiers.append(level == 0 ? "bez" : crates.loot().tiers().level(level).label())
                    .append(" §f").append(n).append("§7, "));
            lines.add(tiers.substring(0, tiers.length() - 2));
        }
        if (!AxonItemsBridge.available()) {
            lines.add("§cAxonItems nie działa: przedmioty axon:... w lootcie są pomijane.");
        }
        return lines;
    }

    // ------------------------------------------------------------------ losowanie i odnawianie

    private CrateClass roll(ContainerSpot spot) {
        CrateClass crate = spot.fixedClass != null ? crates.loot().crateClass(spot.fixedClass)
                : crates.pickClass(spot.pool, tierOf(spot));
        // klasy z prawdziwym blokiem stawia tylko CrateService; tutaj liczą się jak „brak”
        return crate != null && crate.kind() == CrateClass.Kind.MODEL ? crate : null;
    }

    /**
     * Tier miejsca: przy drodze – {@code tiery.przy-drogach}, w strukturze – tier tej struktury, z komendy – bez.
     * Liczony przy każdym losowaniu, więc {@code /struktury przeladuj} od razu zmienia loot.
     */
    TierConfig.Tier tierOf(ContainerSpot spot) {
        if (spot.fixedClass != null) {
            return TierConfig.Tier.NONE; // pojemnik testowy z komendy: loot dokładnie jak w klasie
        }
        if (spot.roadside) {
            return crates.loot().tiers().roadside();
        }
        return crates.tierOf(spot.owner);
    }

    private void activate(ContainerSpot spot, CrateClass crate) {
        spot.crate = crate;
        spot.inventory = null;
        spot.hp = crate.container().hp();
        spot.hpShownUntil = 0;
        spot.emptiedAt = 0;
        spot.lockedUntil = CrateLockMath.unlockAt(System.currentTimeMillis(), crate.container().lockMinutes());
        spot.state = crate.container().breakable() ? ContainerSpot.State.INTACT : ContainerSpot.State.CLOSED;
        snapshot(spot);
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
            spot.respawnAt = usesStructureRotation(spot) ? Long.MAX_VALUE : now + settings.respawnMillis();
            snapshot(spot); // wypadło „brak”: spróbujemy przy następnym odnowieniu
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
        String text = crate.container().announce().replace("{nazwa}", crate.displayName()).replace("{miejsce}", place)
                .replace("{tier}", tierOf(spot).label());
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
                    boolean sharedRotation = usesStructureRotation(spot);
                    if (!sharedRotation && spot.respawnAt == Long.MAX_VALUE) {
                        spot.respawnAt = now + settings.respawnMillis();
                    }
                    if (now >= spot.respawnAt && (!sharedRotation || spot.respawnAt == 0)) {
                        respawn(spot, now, spot.respawnAt == 0, spot.respawnAt != 0);
                    }
                }
                case BROKEN -> {
                    if (now - spot.brokenAt >= settings.brokenMillis()) {
                        vanish(spot);
                    }
                }
                case CLOSED -> {
                    if (!usesStructureRotation(spot) && spot.emptiedAt > 0 && now - spot.emptiedAt >= respawnOf(spot.crate) && !playerNearby(spot)) {
                        despawnEntities(spot);
                        spot.state = ContainerSpot.State.WAITING;
                        respawn(spot, now, true, true);
                    }
                }
                default -> {
                }
            }
            if (spot.visible() && spot.lockedUntil > CrateLockMath.UNLOCKED) {
                tickLock(spot, now);
            }
            if (spot.visible()) {
                if (anyMissing(spot)) {
                    // ktoś usunął część encji (np. /kill albo plugin czyszczący encje): sam model bez hitboxa
                    // wyglądał jak beczka, ale nie reagował na klik, więc stawiamy cały pojemnik od nowa
                    despawnEntities(spot);
                }
                if (spot.entities.isEmpty()) {
                    showIfLoaded(spot); // np. blok zasłaniał miejsce przy wczytaniu chunka
                }
            }
        }
    }

    /** Odliczanie nad zablokowanym pojemnikiem; po czasie zdejmuje blokadę i ogłasza to na czacie. */
    private void tickLock(ContainerSpot spot, long now) {
        Entity text = spot.lockText == null ? null : Bukkit.getEntity(spot.lockText);
        if (!CrateLockMath.locked(spot.lockedUntil, now)) {
            spot.lockedUntil = CrateLockMath.UNLOCKED;
            if (text != null) {
                text.remove();
                spot.entities.remove(spot.lockText);
                byEntity.remove(spot.lockText);
            }
            spot.lockText = null;
            World world = Bukkit.getWorld(spot.world);
            if (world != null && spot.crate != null) {
                String message = "§a§l[Zaopatrzenie] §f" + spot.crate.displayName() + " §fzostała odblokowana!";
                world.getPlayers().forEach(player -> player.sendMessage(message));
            }
            return;
        }
        String line = CrateLockMath.displayLine(spot.lockedUntil, now);
        if (line == null || spot.entities.isEmpty()) {
            return;
        }
        var component = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
        if (text instanceof TextDisplay display) {
            display.text(component);
            return;
        }
        World world = Bukkit.getWorld(spot.world);
        if (world == null) {
            return;
        }
        Location at = new Location(world, spot.x + 0.5, spot.y + spot.crate.container().height() + 0.4, spot.z + 0.5);
        TextDisplay display = world.spawn(at, TextDisplay.class, entity -> {
            entity.text(component);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(true);
            entity.setPersistent(false);
            entity.addScoreboardTag(TAG);
        });
        spot.lockText = display.getUniqueId();
        track(spot, display);
    }

    private static boolean anyMissing(ContainerSpot spot) {
        for (UUID id : spot.entities) {
            Entity entity = Bukkit.getEntity(id);
            if (entity == null || !entity.isValid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encja pojemnika, której nie zna żaden punkt (sierota po wcześniejszym postawieniu): usuwamy ją razem z innymi
     * sierotami obok i od razu stawiamy brakujące pojemniki w tym chunku. Zwraca, czy to była sierota.
     */
    private boolean removeOrphan(Entity clicked) {
        if (!clicked.getScoreboardTags().contains(TAG) || byEntity.containsKey(clicked.getUniqueId())) {
            return false;
        }
        for (Entity near : clicked.getNearbyEntities(3, 3, 3)) {
            if (near.getScoreboardTags().contains(TAG) && !byEntity.containsKey(near.getUniqueId())) {
                near.remove();
            }
        }
        clicked.remove();
        for (ContainerSpot spot : List.copyOf(spotsIn(clicked.getLocation().getChunk()))) {
            if (anyMissing(spot)) {
                despawnEntities(spot);
            }
            showIfLoaded(spot);
        }
        return true;
    }

    /** Rozbita beczka znika (pusta albo po czasie); wraca po czasie odnowienia liczonym od rozbicia. */
    private void vanish(ContainerSpot spot) {
        closeViewers(spot);
        despawnEntities(spot);
        spot.respawnAt = usesStructureRotation(spot) ? Long.MAX_VALUE : spot.brokenAt + respawnOf(spot.crate);
        spot.state = ContainerSpot.State.WAITING;
        spot.inventory = null;
        spot.crate = null;
        snapshot(spot);
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

    /**
     * Stawia od nowa modele pojemników we wczytanych chunkach, którym ktoś usunął encje (np. {@code /killall all}).
     * Bez tego wróciłyby dopiero po ponownym wczytaniu chunka.
     *
     * @return ile pojemników postawiono od nowa
     */
    public int respawnMissing() {
        if (!running()) {
            return 0;
        }
        int count = 0;
        for (ContainerSpot spot : List.copyOf(spots.values())) {
            if (!loaded(spot) || spot.entities.isEmpty()) {
                continue;
            }
            boolean missing = false;
            for (UUID id : spot.entities) {
                Entity entity = Bukkit.getEntity(id);
                if (entity == null || !entity.isValid()) {
                    missing = true;
                    break;
                }
            }
            if (missing) {
                despawnEntities(spot);
                spawnEntities(spot);
                count++;
            }
        }
        return count;
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
        spot.lockText = null;
        spot.hpText = null;
    }

    private void closeViewers(ContainerSpot spot) {
        discardPending(spot);
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
        if (spot == null && removeOrphan(event.getAttacked())) {
            event.setCancelled(true);
            return;
        }
        if (spot != null) {
            event.setCancelled(true);
            // naładowanie zamachu czytamy tutaj, zanim gra je wyzeruje po ciosie
            hit(event.getPlayer(), spot, event.getPlayer().getAttackCooldown());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        ContainerSpot spot = byEntity.get(event.getEntity().getUniqueId());
        if (spot != null) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                hit(player, spot, player.getAttackCooldown());
            }
        }
    }

    /**
     * Uderzenie w beczkę: zabiera HP zależnie od przedmiotu w ręce i naładowania zamachu (jak drzewa w
     * AxonMapGenerator). Spamowanie LPM daje ułamek obrażeń, bo zamach nie zdąży się naładować.
     */
    private void hit(Player player, ContainerSpot spot, float charge) {
        if (spot.state != ContainerSpot.State.INTACT || spot.crate == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lockedMessage(player, spot, now)) {
            return;
        }
        Long last = lastHit.get(player.getUniqueId());
        if (last != null && now - last < HIT_COOLDOWN_MS) {
            return; // Paper woła dla jednego ciosu dwa zdarzenia
        }
        lastHit.put(player.getUniqueId(), now);
        // anulowany atak nie zeruje naładowania po stronie serwera, więc robimy to sami (inaczej spam LPM = pełne ciosy)
        player.resetCooldown();
        CrateClass.ContainerSpec spec = spot.crate.container();
        ItemStack held = player.getInventory().getItemInMainHand();
        BarrelMath.Damage table = settings.damage();
        double base = table.base(held.getType().isAir() ? null : held.getType().name());
        int enchant = held.getType().isAir() ? 0 : Math.max(held.getEnchantmentLevel(Enchantment.SHARPNESS),
                held.getEnchantmentLevel(Enchantment.EFFICIENCY));
        double damage = BarrelMath.damage(base, enchant, table.enchant(), charge);
        Location at = new Location(player.getWorld(), spot.x + 0.5, spot.y + 0.6, spot.z + 0.5);
        if (damage <= 0) {
            actionBar(player, "§7Tym nie rozbijesz: " + spot.crate.displayName());
            return;
        }
        StructureInstance owner = farm(spot);
        if (owner != null) owner.disturbed(now);
        spot.hp -= damage;
        if (spot.hp > 1e-6) {
            snapshot(spot);
            at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, charge < 0.9f ? 1.7f : 1.4f);
            at.getWorld().spawnParticle(Particle.BLOCK, at, charge < 0.9f ? 4 : 8, 0.25, 0.3, 0.25,
                    Material.IRON_BLOCK.createBlockData());
            int percent = BarrelMath.percent(spot.hp, spec.hp());
            String color = BarrelMath.color(percent);
            String bar = BarrelMath.bar(spot.hp, spec.hp(), 10, "|", ".");
            int left = BarrelMath.hitsLeft(spot.hp, BarrelMath.damage(base, enchant, table.enchant(), 1f));
            actionBar(player, spot.crate.displayName() + " " + color + percent + "% §8[" + color + bar + "§8]"
                    + " §7jeszcze ~" + left + " " + (left == 1 ? "uderzenie" : left < 5 ? "uderzenia" : "uderzeń")
                    + (charge < 0.9f ? " §c(słaby cios)" : ""));
            showHpBar(spot, now);
            wobble(spot);
            return;
        }
        spot.hp = 0;
        spot.state = ContainerSpot.State.BROKEN;
        spot.brokenAt = now;
        removeHpBar(spot);
        spot.inventory = newInventory(spot);
        if (!crates.emptyLoot()) {
            spot.crate.fill(spot.inventory, random, tierOf(spot)); // loot losuje się w chwili rozbicia
            record(player, spot);
        }
        snapshot(spot);
        Entity display = spot.display == null ? null : Bukkit.getEntity(spot.display);
        if (display instanceof ItemDisplay itemDisplay) {
            itemDisplay.setItemStack(modelItem(spec.brokenModel()));
        }
        at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.7f, 1.3f);
        at.getWorld().spawnParticle(Particle.BLOCK, at, 30, 0.35, 0.35, 0.35, Material.IRON_BLOCK.createBlockData());
        actionBar(player, "§a" + spot.crate.displayName() + "§a rozbita. §7PPM, żeby przeszukać.");
    }

    /** Pasek HP nad beczką, widoczny dla wszystkich; znika po {@code hpBarMillis} od ostatniego uderzenia. */
    private void showHpBar(ContainerSpot spot, long now) {
        long keep = settings.hpBarMillis();
        World world = Bukkit.getWorld(spot.world);
        if (keep <= 0 || world == null || spot.crate == null || spot.entities.isEmpty()) {
            return;
        }
        double max = spot.crate.container().hp();
        int percent = BarrelMath.percent(spot.hp, max);
        String color = BarrelMath.color(percent);
        var text = LegacyComponentSerializer.legacySection().deserialize(
                color + BarrelMath.bar(spot.hp, max, 12, "▌", "§8▌") + " §f" + percent + "%");
        Entity existing = spot.hpText == null ? null : Bukkit.getEntity(spot.hpText);
        if (existing instanceof TextDisplay display) {
            display.text(text);
        } else {
            Location above = new Location(world, spot.x + 0.5, spot.y + spot.crate.container().height() + 0.35,
                    spot.z + 0.5);
            TextDisplay display = world.spawn(above, TextDisplay.class, entity -> {
                entity.text(text);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(false);
                entity.setShadowed(true);
                entity.setBackgroundColor(org.bukkit.Color.fromARGB(96, 0, 0, 0));
                entity.setViewRange(0.25f); // ok. 16 bloków: widać z bliska, nie z drugiego końca drogi
                entity.setPersistent(false);
                entity.addScoreboardTag(TAG);
            });
            spot.hpText = display.getUniqueId();
            track(spot, display);
        }
        spot.hpShownUntil = now + keep;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (spots.get(spot.key) == spot && System.currentTimeMillis() >= spot.hpShownUntil) {
                removeHpBar(spot);
            }
        }, keep / 50L + 2L);
    }

    private void removeHpBar(ContainerSpot spot) {
        if (spot.hpText == null) {
            return;
        }
        Entity text = Bukkit.getEntity(spot.hpText);
        if (text != null) {
            text.remove();
        }
        spot.entities.remove(spot.hpText);
        byEntity.remove(spot.hpText);
        spot.hpText = null;
    }

    /** Beczka lekko podskakuje i kurczy się przy uderzeniu, a po chwili wraca (interpolacja po stronie klienta). */
    private void wobble(ContainerSpot spot) {
        if (spot.display == null || !(Bukkit.getEntity(spot.display) instanceof ItemDisplay display)) {
            return;
        }
        float tilt = (random.nextBoolean() ? 1 : -1) * 0.07f;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(2);
        display.setTransformation(new Transformation(new Vector3f(0f, 0.03f, 0f),
                new Quaternionf().rotateZ(tilt), new Vector3f(0.94f, 0.94f, 0.94f), new Quaternionf()));
        UUID id = spot.display;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getEntity(id) instanceof ItemDisplay d) {
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(3);
                d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1f, 1f, 1f),
                        new Quaternionf()));
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        ContainerSpot spot = byEntity.get(event.getRightClicked().getUniqueId());
        if (spot == null) {
            if (removeOrphan(event.getRightClicked())) {
                event.setCancelled(true);
            }
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
        if (spot.state == ContainerSpot.State.CLOSED && lockedMessage(player, spot, now)) {
            return;
        }
        switch (spot.state) {
            case INTACT -> actionBar(player, "§7Najpierw rozbij: §fLPM§7.");
            case CLOSED -> {
                if (spot.inventory == null) {
                    spot.inventory = newInventory(spot);
                    if (!crates.emptyLoot()) {
                        spot.crate.fill(spot.inventory, random, tierOf(spot)); // przy pierwszym otwarciu, nie da się podejrzeć z daleka
                        record(player, spot);
                    }
                }
                snapshot(spot);
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

    private static boolean lockedMessage(Player player, ContainerSpot spot, long now) {
        if (!CrateLockMath.locked(spot.lockedUntil, now)) {
            return false;
        }
        actionBar(player, "§c§lZABLOKOWANA §7jeszcze §f"
                + CrateLockMath.countdown(CrateLockMath.remainingMillis(spot.lockedUntil, now)));
        return true;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackLoot(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ContainerSpot spot = inventory.getHolder() instanceof ContainerSpot model ? model : null;
        if (spot == null && !(inventory.getHolder() instanceof org.bukkit.block.Container)
                && !(inventory.getHolder() instanceof org.bukkit.block.DoubleChest)) return;
        StructureInstance owner = ownerOf(inventory, spot);
        if (owner == null || (!owner.farmCycle() && !crates.usesNpcRotation(owner.id()))) return;
        Object containerKey = spot == null ? inventory.getLocation() : spot;
        if (containerKey == null) return;
        finishPending(containerKey);
        var action = event.getAction();
        boolean collect = action == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR;
        boolean canTake = switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, MOVE_TO_OTHER_INVENTORY,
                 DROP_ALL_SLOT, DROP_ONE_SLOT, SWAP_WITH_CURSOR, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD,
                 COLLECT_TO_CURSOR -> collect || event.getRawSlot() >= 0 && event.getRawSlot() < inventory.getSize();
            default -> false;
        };
        ItemStack clicked = collect ? event.getCursor() : event.getCurrentItem();
        ItemStack type = canTake && clicked != null && !clicked.getType().isAir() ? clicked.asOne() : null;
        var view = event.getView();
        var carried = cursorSettlement(view, player, containerKey, event.getCursor());
        if (type == null && carried == null) return;
        WarnedContainer warning = warnedContainers.get(player.getUniqueId());
        UUID witness = warning != null && containerKey.equals(warning.key()) ? warning.speaker() : null;
        int before = type == null ? 0 : countSimilar(inventory, type);
        boolean npcOwned = crates.usesNpcRotation(owner.id());
        Location at = npcOwned ? locationOf(inventory, spot, player) : null;
        UUID transaction = type != null && at != null && StructureContainerEvent.getHandlerList().getRegisteredListeners().length > 0
                ? UUID.randomUUID() : null;
        if (transaction != null) publish(owner, player, at, Phase.BEFORE_TAKE, transaction, 0);

        boolean takesToCursor = switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, SWAP_WITH_CURSOR, COLLECT_TO_CURSOR -> true;
            default -> false;
        };
        scheduleLoot(containerKey, () -> {
            if (carried != null) carried.run();
            if (type == null) return;
            boolean same = store.get(owner.id()) == owner && (spot == null || spot.inventory == inventory);
            int removed = same ? Math.max(0, before - countSimilar(inventory, type)) : 0;
            if (transaction != null) {
                Take take = new Take(owner, player, at, transaction, removed);
                if (removed > 0 && takesToCursor && type.isSimilar(view.getCursor())) {
                    var cursor = cursorLoot.computeIfAbsent(player.getUniqueId(),
                        ignored -> new CursorLoot(containerKey, type, new ArrayDeque<>()));
                    cursor.takes().add(take);
                    take.report(Phase.HELD, removed);
                } else {
                    take.report(Phase.AFTER_TAKE, removed);
                }
            }
            if (removed == 0) return;
            if (!npcOwned && witness != null) pl.axon.structure.npc.GuardService.scrapChest(owner.id(), player, witness, false);
            owner.disturbed(System.currentTimeMillis());
            if (spot != null && spot.inventory == inventory) snapshot(spot);
            saveSoon();
        });
    }

    private Runnable cursorSettlement(InventoryView view, Player player, Object key, ItemStack cursorBefore) {
        UUID playerId = player.getUniqueId();
        var carried = cursorLoot.get(playerId);
        if (carried == null || !carried.key().equals(key) || !carried.type().isSimilar(cursorBefore)) return null;
        int cursorAmount = cursorBefore.getAmount();
        int inCrate = countSimilar(view.getTopInventory(), carried.type());
        return () -> {
            ItemStack after = view.getCursor();
            int stillHeld = carried.type().isSimilar(after) ? after.getAmount() : 0;
            int spent = cursorAmount - stillHeld;
            if (spent <= 0) return;
            int returned = Math.clamp(countSimilar(view.getTopInventory(), carried.type()) - inCrate, 0, spent);
            settleTakes(carried, returned, spent - returned);
            if (carried.takes().isEmpty()) cursorLoot.remove(playerId, carried);
        };
    }

    private static void settleTakes(CursorLoot cursor, int returned, int withdrawn) {
        while ((returned > 0 || withdrawn > 0) && !cursor.takes().isEmpty()) {
            var take = cursor.takes().removeFirst();
            int putBack = Math.min(returned, take.amount());
            int taken = Math.min(withdrawn, take.amount() - putBack);
            returned -= putBack;
            withdrawn -= taken;
            int remains = take.amount() - putBack - taken;
            if (taken > 0 || remains == 0) {
                take.report(Phase.AFTER_TAKE, taken);
            } else {
                cursor.takes().addFirst(new Take(take.owner(), take.player(), take.at(), take.transaction(), remains));
            }
        }
    }

    private void scheduleLoot(Object key, Runnable finish) {
        pendingLoot.put(key, finish);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (pendingLoot.remove(key, finish)) finish.run();
        });
    }

    private StructureInstance ownerOf(Inventory inventory, ContainerSpot spot) {
        if (spot != null) return spot.owner == null ? null : store.get(spot.owner);
        return crates.trackedContainerAt(inventory.getLocation());
    }

    private static Location locationOf(Inventory inventory, ContainerSpot spot, Player player) {
        return spot != null ? new Location(player.getWorld(), spot.x + 0.5, spot.y + 0.5, spot.z + 0.5)
            : inventory.getLocation();
    }

    private static void publish(StructureInstance owner, Player player, Location at, Phase phase,
                                UUID transaction, int removed) {
        if (at != null && StructureContainerEvent.getHandlerList().getRegisteredListeners().length > 0) {
            Bukkit.getPluginManager().callEvent(new StructureContainerEvent(owner.id(), player, at, phase, transaction, removed));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAutomatedTransfer(InventoryMoveItemEvent event) {
        discardPending(event.getSource().getLocation());
        discardPending(event.getDestination().getLocation());
    }

    void discardPending(Object key) {
        pendingLoot.remove(key);
        cursorLoot.values().removeIf(cursor -> {
            if (!cursor.key().equals(key)) return false;
            cursor.takes().forEach(take -> take.report(Phase.AFTER_TAKE, 0));
            return true;
        });
    }

    private void finishPending(Object key) {
        Runnable previous = pendingLoot.remove(key);
        if (previous != null) previous.run();
    }

    private static int countSimilar(Inventory inventory, ItemStack type) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (type.isSimilar(item)) count += item.getAmount();
        }
        return count;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        finishPending(inventory.getHolder() instanceof ContainerSpot spot ? spot : inventory.getLocation());
        if (!(inventory.getHolder() instanceof ContainerSpot)) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackCursorDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        var view = event.getView();
        var inventory = view.getTopInventory();
        Object key = inventory.getHolder() instanceof ContainerSpot spot ? spot : inventory.getLocation();
        if (key == null) return;
        var settlement = cursorSettlement(view, player, key, event.getOldCursor());
        if (settlement != null) scheduleLoot(key, settlement);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        finishPending(inventory.getHolder() instanceof ContainerSpot spot ? spot : inventory.getLocation());
        UUID player = event.getPlayer().getUniqueId();
        warnedContainers.remove(player);
        var remaining = cursorLoot.remove(player);
        if (remaining != null) remaining.takes().forEach(take -> take.report(Phase.AFTER_TAKE, take.amount()));
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
