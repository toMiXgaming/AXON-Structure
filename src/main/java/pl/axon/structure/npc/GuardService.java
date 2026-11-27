package pl.axon.structure.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.loot.CrateClass;
import pl.axon.structure.loot.CrateService;
import pl.axon.structure.loot.LootConfig;
import pl.axon.structure.loot.LootStats;
import pl.axon.structure.loot.TierConfig;
import pl.axon.structure.world.InstanceStore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Strażnicy w strukturach. Gdy na serwerze jest AxonNPC, to on stawia NPC (wygląd gracza, walka bronią AxonGuns,
 * rozmowy przez AI, nastawienie gangu do graczy); grupą NPC jest instancja struktury. Bez AxonNPC zostaje stara
 * droga: NPC-gracze z Citizens z walką z Sentinela (który obsługuje też broń QualityArmory).
 * Miejsca strażników są w plikach struktur jako punkty z pulą {@code npc-<zestaw>}, a zestawy (skin, broń,
 * zbroja, życie, loot) w sekcji {@code straznicy} configu. Strażnicy pojawiają się, gdy gracz podejdzie do
 * struktury, znikają, gdy w okolicy nikogo nie ma, a zabici wracają po {@code odradzanie-minut}.
 * <p>
 * Citizens i Sentinel są opcjonalne i wołane przez refleksję: bez nich plugin działa normalnie, tylko bez strażników.
 */
public final class GuardService implements Listener {

    public static final String POOL_PREFIX = "npc-";
    /** Handlarz w oazie: stoi bez broni i nie walczy, prawym kliknięciem otwiera sklep. */
    public static final String TRADER_POOL = "punkt-handlarz";
    public static final String TRADER_KIT = "handlarz";
    public static final String FENCE_POOL = "punkt-czarny-rynek";
    public static final String FENCE_KIT = "paser";

    public record Kit(String id, String name, String skinTexture, String skinSignature, String skinName,
                      String weapon, List<String> armor, double health, double damage, double range,
                      double accuracy, String lootClass, List<String> targets, boolean walks, int walkRadius,
                      boolean farms, boolean sleeps) {

        /** Bez celów NPC tylko stoi (handlarz); z "players" walczy z graczami, z "monsters" broni przed potworami. */
        public boolean fights() {
            return !targets.isEmpty();
        }

        public boolean attacksPlayers() {
            return targets.stream().anyMatch(t -> t.equalsIgnoreCase("players"));
        }
    }

    public record Settings(boolean enabled, int activationRadius, int respawnMinutes, Map<String, Kit> kits) {
    }

    /** Jeden strażnik: miejsce w strukturze i (gdy stoi) jego NPC. */
    private static final class Guard {
        final String instanceId;
        final Kit kit;
        final Location location;
        pl.axon.structure.core.Box area;
        String personalName;
        String displayName() { return personalName == null ? kit.name() : personalName; }
        String slot;
        boolean trader() {
            return kit.id().equals("handlarz") || kit.id().equals("paser");
        }
        Object npc;
        /** SentinelTrait tego NPC (do śledzenia celu głową co tick). */
        Object sentinel;
        UUID entityId;
        long deadUntil;

        Guard(String instanceId, Kit kit, Location location) {
            this.instanceId = instanceId;
            this.kit = kit;
            this.location = location;
        }
    }

    private final BundledSkin scrapperSkin = new BundledSkin();

    public void loadSkin() {
        Kit kit = settings.kits().get("zlomiarz");
        if (kit != null && kit.skinTexture().isBlank() && kit.skinName().isBlank()) scrapperSkin.start(plugin);
    }

    private boolean bundled(Kit kit) {
        return kit.id().equals("zlomiarz") && kit.skinTexture().isBlank() && kit.skinName().isBlank();
    }

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final CrateService crates;
    private LootStats stats;
    private final Random random = new Random();
    private final Map<String, Guard> guards = new HashMap<>();
    private final Map<UUID, Guard> byEntity = new HashMap<>();
    private Settings settings;
    private BukkitTask task;
    /** Co tick: płynne obracanie głów strażników w stronę celu. */
    private BukkitTask aimTask;
    private Field chasingField;
    private Method lookMethod;
    private boolean lookResolved;
    private boolean warnedAim;
    private Object registry;
    private boolean warned;
    private boolean warnedWander;
    private final java.util.Set<String> warnedWeapons = new java.util.HashSet<>();
    /** Zestawy, których brakuje w configu, a struktury ich wymagają (do komunikatu i komendy). */
    private final java.util.Set<String> missingKits = new java.util.TreeSet<>();

    public GuardService(JavaPlugin plugin, StructureLibrary library, InstanceStore store, CrateService crates) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.crates = crates;
    }

    /** Statystyki lootu ({@code statystyki.yml}); null = nie liczymy. */
    public void stats(LootStats stats) {
        this.stats = stats;
    }

    public void configure(Settings settings) {
        removeAll();
        missingKits.clear();
        warned = false;
        this.settings = settings;
    }

    public void start() {
        stopTimer();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 15, 20L * 5);
        aimTask = Bukkit.getScheduler().runTaskTimer(plugin, this::aimTick, 20L, 1L);
    }

    public void stop() {
        stopTimer();
        removeAll();
    }

    private void stopTimer() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (aimTask != null) {
            aimTask.cancel();
            aimTask = null;
        }
    }

    /** Czy jest czym stawiać strażników: AxonNPC albo Citizens z Sentinelem. */
    public boolean available() {
        return axonNpc() || (Bukkit.getPluginManager().isPluginEnabled("Citizens")
                && Bukkit.getPluginManager().isPluginEnabled("Sentinel"));
    }

    /** Czy strażników stawia AxonNPC (ma pierwszeństwo przed Citizens). */
    public boolean axonNpc() {
        return Bukkit.getPluginManager().isPluginEnabled("AxonNPC");
    }

    /** Nazwa tego, co stawia strażników (do komendy). */
    public String backend() {
        return axonNpc() ? "AxonNPC" : "Citizens + Sentinel";
    }

    public int activeCount() {
        int n = 0;
        for (Guard guard : guards.values()) {
            if (guard.npc != null) {
                n++;
            }
        }
        return n;
    }

    private void tick() {
        if (!settings.enabled() || !available()) {
            return;
        }
        long now = System.currentTimeMillis();
        double activate = (double) settings.activationRadius() * settings.activationRadius();
        double keep = activate * 2.25; // 1,5 × promień: mniej mrugania na granicy
        for (StructureInstance instance : store.all()) {
            World world = Bukkit.getWorld(instance.world());
            if (world == null) {
                continue;
            }
            double nearest = Double.MAX_VALUE;
            for (Player player : world.getPlayers()) {
                Location l = player.getLocation();
                nearest = Math.min(nearest, instance.edgeDistanceSquared(l.getX(), l.getZ()));
            }
            if (nearest <= activate) {
                spawnGuards(instance, now);
            } else if (nearest > keep) {
                despawnGuards(instance.id());
            }
        }
        Iterator<Map.Entry<String, Guard>> it = guards.entrySet().iterator();
        while (it.hasNext()) {
            Guard guard = it.next().getValue();
            if (store.get(guard.instanceId) == null) {
                // struktura cofnięta albo zapomniana
                destroy(guard);
                it.remove();
            } else if (guard.npc != null && !isSpawned(guard.npc)) {
                // zabity bez zdarzenia śmierci albo zdjęty razem z chunkiem
                boolean loaded = guard.location.getWorld() != null && guard.location.getWorld()
                        .isChunkLoaded(guard.location.getBlockX() >> 4, guard.location.getBlockZ() >> 4);
                destroy(guard);
                if (loaded) {
                    guard.deadUntil = now + settings.respawnMinutes() * 60_000L;
                }
            }
        }
        for (Guard guard : guards.values()) {
            // rolników z AxonNPC prowadzi sam AxonNPC
            if (guard.npc != null && guard.kit.farms() && !(guard.npc instanceof Entity)) {
                farm(guard);
            }
        }
    }

    /**
     * Stawia strażników we wszystkich wczytanych strukturach, bez czekania na graczy (komenda, test).
     *
     * @return liczba postawionych albo -1, gdy brak Citizens lub Sentinela
     */
    public int spawnAllLoaded() {
        if (!available()) {
            return -1;
        }
        int before = activeCount();
        long now = System.currentTimeMillis();
        for (StructureInstance instance : store.all()) {
            World world = Bukkit.getWorld(instance.world());
            if (world != null && world.isChunkLoaded(instance.centerX() >> 4, instance.centerZ() >> 4)) {
                spawnGuards(instance, now);
            }
        }
        return activeCount() - before;
    }

    private void spawnGuards(StructureInstance instance, long now) {
        Blueprint bp = library.get(instance.structureId());
        World world = Bukkit.getWorld(instance.world());
        if (bp == null || world == null) {
            return;
        }
        int index = 0;
        if (instance.farmCycle() && !instance.scrapSite()) {
            if (instance.residentsPresent == null) {
                instance.residentsPresent = java.util.concurrent.ThreadLocalRandom.current().nextBoolean();
                store.save();
            }
            if (!instance.residentsPresent) {
                despawnGuards(instance.id());
                return;
            }
        }
        int farmerIndex = 0;
        for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
            String kitId = kitId(point.pool());
            if (kitId == null) {
                continue;
            }
            String key = instance.id() + "#" + index++;
            int farmer = instance.farmCycle() && (kitId.equals("powsinoga") || kitId.equals("zlomiarz")) ? farmerIndex++ : -1;
            if (instance.missingGuards.contains(key)) continue;
            Kit kit = settings.kits().get(kitId);
            if (kit == null) {
                if (missingKits.add(kitId)) {
                    plugin.getLogger().warning("Brak zestawu strażnika '" + kitId
                            + "' w sekcji straznicy.zestawy – te miejsca zostają puste.");
                }
                continue;
            }
            int[] w = instance.placement().toWorld(point.pos().x(), point.pos().y(), point.pos().z());
            if (!world.isChunkLoaded(w[0] >> 4, w[2] >> 4)) {
                continue;
            }
            Guard guard = guards.computeIfAbsent(key, k -> new Guard(instance.id(), kit,
                    new Location(world, w[0] + 0.5, w[1], w[2] + 0.5)));
            if (farmer >= 0 && farmer < instance.groupSize()) {
                if (!ensureFarmNames(instance, now)) continue;
                String name = instance.farmNames.get(farmer);
                if (!name.equals(guard.personalName)) {
                    if (guard.npc != null && !renameGuard(guard, name)) continue;
                    guard.personalName = name;
                }
            }
            guard.slot = key;
            if (guard.area == null) guard.area = pl.axon.structure.core.Box.of(bp, instance.placement());
            if (guard.npc != null || now < guard.deadUntil) {
                continue;
            }
            spawn(guard);
        }
    }

    static boolean validPair(List<String> names, List<String> previous, int count) {
        if (names.size() != count || names.stream().distinct().count() != count) return false;
        for (String name : names) {
            if (!name.matches("[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]{1,15}") || name.equals("Powsinoga")
                    || previous.stream().anyMatch(name::equalsIgnoreCase)) return false;
        }
        return true;
    }

    private boolean ensureFarmNames(StructureInstance owner, long now) {
        if (owner.farmNames.size() == owner.groupSize()) return true;
        if (owner.nameRequest != 0 && now - owner.nameRequest < 60_000L) return false;
        if (!axonNpc()) return false;
        owner.nameRequest = now;
        java.util.function.Consumer<List<String>> done = names -> {
            if (!plugin.isEnabled() || store.get(owner.id()) != owner || owner.nameRequest != now) return;
            // Leave the request timestamp in place on failure to throttle retries.
            if (names == null || !validPair(names, owner.previousFarmNames, owner.groupSize())) return;
            if (!owner.previousFarmNames.isEmpty()) {
                try {
                    if (!Boolean.TRUE.equals(axonNpcApi().getMethod("resetResidents", String.class)
                            .invoke(null, owner.id()))) return;
                } catch (ReflectiveOperationException e) {
                    plugin.getLogger().warning("Nie udało się odnowić pamięci obsady " + owner.id() + ": " + e);
                    return;
                }
                forget(owner.id());
            }
            owner.farmNames = List.copyOf(names);
            owner.nameRequest = 0;
            store.save();
        };
        try {
            axonNpcApi().getMethod("groupNames", int.class, String.class, List.class, java.util.function.Consumer.class)
                    .invoke(null, owner.groupSize(), owner.scrapSite() ? "złomiarzy" : "robotników rolnych", owner.previousFarmNames, done);
        } catch (ReflectiveOperationException ignored) {
            // Retry after the naming API becomes available.
        }
        return owner.farmNames.size() == owner.groupSize();
    }

    private boolean renameGuard(Guard guard, String name) {
        try {
            if (guard.npc instanceof Entity entity) {
                axonNpcApi().getMethod("renameLocal", Entity.class, String.class).invoke(null, entity, name);
            } else {
                call(guard.npc, "setName", new Class<?>[]{String.class}, name);
            }
            return true;
        } catch (ReflectiveOperationException error) {
            // Keep a living NPC in place when running with a mismatched backend.
            guard.personalName = name;
            return false;
        }
    }

    /** Zestaw dla punktu ze struktury albo null, gdy to nie jest miejsce na NPC. */
    private static String kitId(String pool) {
        String p = pool.toLowerCase(Locale.ROOT);
        if (p.startsWith(POOL_PREFIX)) {
            return p.substring(POOL_PREFIX.length());
        }
        return p.equals(TRADER_POOL) ? TRADER_KIT : p.equals(FENCE_POOL) ? FENCE_KIT : null;
    }

    /** Zestaw NPC postawionego przez ten plugin albo null, gdy to nie nasz NPC. */
    public Kit kitOf(Entity entity) {
        Guard guard = byEntity.get(entity.getUniqueId());
        return guard == null ? null : guard.kit;
    }

    private void despawnGuards(String instanceId) {
        for (Guard guard : guards.values()) {
            if (guard.instanceId.equals(instanceId) && guard.npc != null) {
                destroy(guard);
            }
        }
    }

    private void removeAll() {
        for (Guard guard : guards.values()) {
            destroy(guard);
        }
        guards.clear();
        byEntity.clear();
    }

    // ------------------------------------------------------------------ zabici strażnicy: loot zamiast ekwipunku

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        Guard guard = byEntity.remove(event.getEntity().getUniqueId());
        if (guard == null) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        CrateClass loot = guard.kit.lootClass().isBlank() || crates.emptyLoot() ? null
                : crates.loot().crateClass(guard.kit.lootClass());
        if (loot != null) {
            // tier struktury: więcej sztuk w stosach; dodatki tieru są tylko w pojemnikach
            TierConfig.Tier tier = crates.tierOf(guard.instanceId);
            List<ItemStack> drops = loot.roll(random, tier.withoutExtras());
            event.getDrops().addAll(drops);
            if (stats != null) {
                stats.record(event.getEntity().getKiller(), LootStats.source("npc", tier, false), drops);
            }
        }
        StructureInstance owner = store.get(guard.instanceId);
        if (owner != null && owner.farmCycle() && event.getEntity().getKiller() != null) {
            owner.missingGuards.add(guard.slot);
            owner.disturbed(System.currentTimeMillis());
            store.save();
        } else {
            guard.deadUntil = System.currentTimeMillis() + settings.respawnMinutes() * 60_000L;
        }
        // NPC sprzątamy tickiem później, gdy Citizens skończy obsługę śmierci
        Bukkit.getScheduler().runTask(plugin, () -> destroy(guard));
    }

    // ------------------------------------------------------------------ Citizens i Sentinel przez refleksję

    private boolean allowedMove(Guard guard, Location from, Location to) {
        if (guard == null || guard.area == null || to == null) return true;
        if (guard.trader()) {
            return to.getBlockX() == guard.location.getBlockX() && to.getBlockZ() == guard.location.getBlockZ();
        }
        return guard.area.contains(to.getBlockX(), to.getBlockZ())
                || !guard.area.contains(from.getBlockX(), from.getBlockZ());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobMove(io.papermc.paper.event.entity.EntityMoveEvent event) {
        if (!allowedMove(byEntity.get(event.getEntity().getUniqueId()), event.getFrom(), event.getTo()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!allowedMove(byEntity.get(event.getPlayer().getUniqueId()), event.getFrom(), event.getTo()))
            event.setCancelled(true);
    }

    private void spawn(Guard guard) {
        if (bundled(guard.kit) && scrapperSkin.loading) return;
        if (axonNpc()) {
            spawnAxonNpc(guard);
            return;
        }
        try {
            Object npc = call(registry(), "createNPC", new Class<?>[]{EntityType.class, String.class},
                    EntityType.PLAYER, LootConfig.color(guard.displayName()));
            skin(npc, guard.kit);
            equip(npc, guard.kit);
            if (guard.kit.fights()) {
                sentinel(npc, guard);
            } else {
                trySet(npc, "setProtected", boolean.class, true);
            }
            Boolean ok = (Boolean) call(npc, "spawn", new Class<?>[]{Location.class}, guard.location);
            if (ok == null || !ok) {
                call(npc, "destroy", new Class<?>[0]);
                return;
            }
            guard.npc = npc;
            Entity entity = (Entity) call(npc, "getEntity", new Class<?>[0]);
            if (entity != null) {
                guard.entityId = entity.getUniqueId();
                byEntity.put(entity.getUniqueId(), guard);
            }
            if (!guard.trader()) {
                wander(npc, guard);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("Nie udało się postawić strażnika (Citizens/Sentinel): " + e);
            }
        }
    }

    private void destroy(Guard guard) {
        if (guard.entityId != null) {
            byEntity.remove(guard.entityId);
            guard.entityId = null;
        }
        if (guard.npc instanceof Entity entity) {
            try {
                axonNpcApi().getMethod("remove", Entity.class).invoke(null, entity);
            } catch (ReflectiveOperationException | RuntimeException e) {
                entity.remove();
            }
            guard.npc = null;
        } else if (guard.npc != null) {
            try {
                call(guard.npc, "destroy", new Class<?>[0]);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // NPC już zniknął
            }
            guard.npc = null;
        }
        guard.sentinel = null;
    }

    private boolean isSpawned(Object npc) {
        if (npc instanceof Entity entity) {
            return entity.isValid() && !entity.isDead();
        }
        try {
            return Boolean.TRUE.equals(call(npc, "isSpawned", new Class<?>[0]));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Chodzenie po okolicy (Citizens: punkty trasy „wander”), tylko w kwadracie {@code promien-chodzenia} wokół
     * miejsca ze struktury, żeby NPC nie wyszedł poza nią. Gdy coś się nie uda (inna wersja Citizens), NPC po prostu stoi.
     */
    private void wander(Object npc, Guard guard) {
        try {
            Object waypoints = trait(npc, "net.citizensnpcs.trait.waypoint.Waypoints");
            Object ok = call(waypoints, "setWaypointProvider", new Class<?>[]{String.class}, "wander");
            if (!Boolean.TRUE.equals(ok)) {
                return;
            }
            Object provider = call(waypoints, "getCurrentProvider", new Class<?>[0]);
            int radius = (Math.max(guard.area.maxX() - guard.area.minX(), guard.area.maxZ() - guard.area.minZ()) + 1) / 2;
            Location center = guard.location.clone();
            center.setX(guard.area.centerX() + 0.5);
            center.setZ(guard.area.centerZ() + 0.5);
            // najpierw zasięg, potem środek obszaru: obszar liczy się z bieżącego zasięgu
            call(provider, "setXYRange", new Class<?>[]{int.class, int.class}, radius, 2);
            call(provider, "addRegionCentre", new Class<?>[]{Location.class}, center);
            trySet(provider, "setDelay", int.class, 60 + random.nextInt(80));
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warnedWander) {
                warnedWander = true;
                plugin.getLogger().warning("NPC nie mogą chodzić (inna wersja Citizens?): " + e);
            }
        }
    }

    /**
     * Rolnicy (zestawy z {@code uprawia: true}): co kilka sekund podchodzą do dojrzałej rośliny w pobliżu, machają ręką
     * i zbierają ją, sadząc od nowa (wiek 0), więc pola w oazie widocznie żyją. Tylko wygląd: nic nie wypada.
     */
    private void farm(Guard guard) {
        Entity entity = guard.entityId == null ? null : Bukkit.getEntity(guard.entityId);
        if (!(entity instanceof Player body) || random.nextInt(3) != 0) {
            return;
        }
        Location here = body.getLocation();
        List<org.bukkit.block.Block> ripe = new ArrayList<>();
        List<org.bukkit.block.Block> any = new ArrayList<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    org.bukkit.block.Block b = here.getBlock().getRelative(dx, dy, dz);
                    if (b.getBlockData() instanceof org.bukkit.block.data.Ageable crop
                            && b.getRelative(0, -1, 0).getType() == Material.FARMLAND) {
                        any.add(b);
                        if (crop.getAge() >= crop.getMaximumAge()) {
                            ripe.add(b);
                        }
                    }
                }
            }
        }
        List<org.bukkit.block.Block> pool = ripe.isEmpty() ? any : ripe;
        if (pool.isEmpty()) {
            return;
        }
        org.bukkit.block.Block crop = pool.get(random.nextInt(pool.size()));
        Location target = crop.getLocation().add(0.5, 0, 0.5);
        if (target.distanceSquared(here) > 9) {
            // za daleko: podejdź (Citizens), zbierze przy następnym razie
            try {
                Object navigator = call(guard.npc, "getNavigator", new Class<?>[0]);
                call(navigator, "setTarget", new Class<?>[]{Location.class}, target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // starsza wersja Citizens: zostaje w miejscu
            }
            return;
        }
        try {
            call(guard.npc, "faceLocation", new Class<?>[]{Location.class}, target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // bez obracania
        }
        body.swingMainHand();
        if (crop.getBlockData() instanceof org.bukkit.block.data.Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
            ageable.setAge(0);
            crop.setBlockData(ageable, false);
            crop.getWorld().playSound(target, org.bukkit.Sound.BLOCK_CROP_BREAK, 0.6f, 1f);
            crop.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, target.clone().add(0, 0.5, 0), 6, 0.3, 0.3, 0.3);
        }
    }

    // ------------------------------------------------------------------ AxonNPC przez refleksję

    public static UUID scrapChest(String group, org.bukkit.entity.Player player, UUID witness) {
        return scrapChest(group, player, witness, witness == null);
    }

    public static UUID scrapChest(String group, org.bukkit.entity.Player player, UUID witness, boolean opening) {
        if (!Bukkit.getPluginManager().isPluginEnabled("AxonNPC")) return null;
        try {
            return (UUID) axonNpcApi().getMethod("scrapChest", String.class, org.bukkit.entity.Player.class, UUID.class, boolean.class)
                    .invoke(null, group, player, witness, opening);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Class<?> axonNpcApi() throws ClassNotFoundException {
        org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin("AxonNPC");
        ClassLoader loader = p == null ? GuardService.class.getClassLoader() : p.getClass().getClassLoader();
        return Class.forName("pl.axon.npc.api.AxonNpcApi", true, loader);
    }

    /**
     * Strażnik przez AxonNPC. Zestaw idzie jako mapa z kluczami jak w configu; grupą (wspólne nastawienie gangu
     * do graczy i alarm) jest instancja struktury.
     */
    private void spawnAxonNpc(Guard guard) {
        Kit kit = guard.kit;
        Map<String, Object> map = new HashMap<>();
        map.put("id", kit.id());
        StructureInstance home = store.get(guard.instanceId);
        if (home != null) {
            Blueprint blueprint = library.get(home.structureId());
            map.put("struktura", blueprint == null ? home.structureId() : blueprint.name());
        }
        map.put("nazwa", guard.displayName());
        map.put("skin-tekstura", bundled(kit) ? scrapperSkin.value : kit.skinTexture());
        map.put("skin-podpis", bundled(kit) ? scrapperSkin.signature : kit.skinSignature());
        map.put("skin-gracza", kit.skinName());
        if (guard.area != null) {
            map.put("teren", List.of(guard.area.minX(), guard.area.minZ(), guard.area.maxX(), guard.area.maxZ()));
        }
        map.put("bron", kit.weapon());
        if (!Bukkit.getPluginManager().isPluginEnabled("AxonGuns") && kit.weapon().toLowerCase(Locale.ROOT).startsWith("qa:")) {
            // bez AxonGuns w ręce zostaje broń QualityArmory (strzela wtedy z kuszy)
            ItemStack item = weapon(kit.weapon());
            if (item != null) {
                map.put("przedmiot-w-rece", item);
            }
        }
        map.put("zbroja", kit.armor());
        map.put("zycie", kit.health());
        map.put("obrazenia", kit.damage());
        map.put("zasieg", kit.range());
        map.put("celnosc", kit.accuracy());
        map.put("cele", kit.targets());
        map.put("chodzi", !guard.trader());
        map.put("promien-chodzenia", kit.walkRadius());
        map.put("uprawia", kit.farms());
        map.put("spi", kit.sleeps());
        try {
            Object body = axonNpcApi().getMethod("spawn", Location.class, Map.class, String.class)
                    .invoke(null, guard.location, map, guard.instanceId);
            if (body instanceof Entity entity) {
                guard.npc = entity;
                guard.entityId = entity.getUniqueId();
                byEntity.put(entity.getUniqueId(), guard);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("Nie udało się postawić strażnika przez AxonNPC: " + e);
            }
        }
    }

    /** Osobny rejestr w pamięci: strażnicy nie trafiają do zapisu Citizens i nie wracają po restarcie. */
    private Object registry() throws ReflectiveOperationException {
        if (registry == null) {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Class<?> storeType = Class.forName("net.citizensnpcs.api.npc.NPCDataStore");
            Object memory = Class.forName("net.citizensnpcs.api.npc.MemoryNPCDataStore").getConstructor().newInstance();
            registry = api.getMethod("createAnonymousNPCRegistry", storeType).invoke(null, memory);
        }
        return registry;
    }

    private Object trait(Object npc, String className) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        return call(npc, "getOrAddTrait", new Class<?>[]{Class.class}, type);
    }

    private void skin(Object npc, Kit kit) throws ReflectiveOperationException {
        Object skin = trait(npc, "net.citizensnpcs.trait.SkinTrait");
        trySet(skin, "setFetchDefaultSkin", boolean.class, false);
        String texture = bundled(kit) ? scrapperSkin.value : kit.skinTexture();
        String signature = bundled(kit) ? scrapperSkin.signature : kit.skinSignature();
        if (!texture.isBlank() && !signature.isBlank()) {
            call(skin, "setSkinPersistent", new Class<?>[]{String.class, String.class, String.class},
                    kit.id(), signature, texture);
        } else if (!kit.skinName().isBlank()) {
            call(skin, "setSkinName", new Class<?>[]{String.class}, kit.skinName());
        }
    }

    private void equip(Object npc, Kit kit) throws ReflectiveOperationException {
        Object equipment = trait(npc, "net.citizensnpcs.api.trait.trait.Equipment");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<? extends Enum> slotType = (Class<? extends Enum>) Class.forName(
                "net.citizensnpcs.api.trait.trait.Equipment$EquipmentSlot");
        Method set = equipment.getClass().getMethod("set", slotType, ItemStack.class);
        ItemStack weapon = weapon(kit.weapon());
        if (weapon != null) {
            set.invoke(equipment, slot(slotType, "HAND"), weapon);
        }
        String[] slots = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};
        for (int i = 0; i < slots.length && i < kit.armor().size(); i++) {
            Material material = Material.matchMaterial(kit.armor().get(i));
            if (material != null && material != Material.AIR) {
                set.invoke(equipment, slot(slotType, slots[i]), new ItemStack(material));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object slot(Class<? extends Enum> type, String name) {
        return Enum.valueOf(type, name);
    }

    /** Broń: {@code qa:<nazwa>} z QualityArmory albo zwykły przedmiot z Minecrafta (np. crossbow). */
    private ItemStack weapon(String spec) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        if (spec.toLowerCase(Locale.ROOT).startsWith("qa:")) {
            String name = spec.substring(3);
            ItemStack gun = pl.axon.structure.util.QaItems.find(name);
            if (gun != null) {
                return gun;
            }
            if (warnedWeapons.add(name.toLowerCase(Locale.ROOT))) {
                java.util.List<String> names = pl.axon.structure.util.QaItems.names();
                plugin.getLogger().warning("Nie ma broni QualityArmory '" + name + "', strażnik dostaje kuszę."
                        + (names.isEmpty() ? " QualityArmory nie odpowiada." : " Dostępne: " + String.join(", ", names)));
            }
            return new ItemStack(Material.CROSSBOW);
        }
        Material material = Material.matchMaterial(spec);
        return material == null ? null : new ItemStack(material);
    }

    private void sentinel(Object npc, Guard guard) throws ReflectiveOperationException {
        Object sentinel = trait(npc, "org.mcmonkey.sentinel.SentinelTrait");
        Kit kit = guard.kit;
        for (String target : kit.targets()) {
            call(sentinel, "addTarget", new Class<?>[]{String.class}, target);
        }
        if (!kit.attacksPlayers()) {
            // strażnik oazy: gracze go nie obchodzą, nawet gdy ktoś go uderzy
            try {
                call(sentinel, "addIgnore", new Class<?>[]{String.class}, "players");
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // starsza wersja Sentinela
            }
        }
        if (!trySet(sentinel, "setHealth", double.class, kit.health())) {
            setField(sentinel, "health", kit.health());
        }
        setField(sentinel, "damage", kit.damage());
        setField(sentinel, "range", kit.range());
        // surowe „celnosc” byłoby dla Sentinela + QualityArmory ogromnym rozrzutem, patrz AimMath.ACCURACY_SCALE
        setField(sentinel, "accuracy", AimMath.sentinelAccuracy(kit.accuracy()));
        setField(sentinel, "chaseRange", Math.max(12.0, kit.range()));
        setField(sentinel, "respawnTime", -1L); // odradzaniem zajmuje się ten plugin
        setField(sentinel, "spawnPoint", guard.location.clone());
        setField(sentinel, "fightback", kit.attacksPlayers());
        setField(sentinel, "needsAmmo", false);
        guard.sentinel = sentinel;
    }

    // ------------------------------------------------------------------ płynne celowanie

    /**
     * Sentinel obraca NPC w stronę celu tylko co swój „update rate” (domyślnie 10 ticków), więc głowa
     * przeskakuje. Co tick obracamy ją o kawałek w stronę oczu celu (AimMath), więc ruch jest płynny,
     * a w chwili strzału lufa już patrzy na cel.
     */
    private void aimTick() {
        if (byEntity.isEmpty()) {
            return;
        }
        for (Guard guard : byEntity.values()) {
            if (guard.sentinel == null || guard.entityId == null) {
                continue;
            }
            Entity entity = Bukkit.getEntity(guard.entityId);
            if (!(entity instanceof LivingEntity self) || !self.isValid() || self.isDead()) {
                continue;
            }
            LivingEntity target = chasing(guard.sentinel);
            if (target == null || !target.isValid() || target.isDead() || target.getWorld() != self.getWorld()) {
                continue;
            }
            Location eye = self.getEyeLocation();
            Location aim = target.getEyeLocation();
            double reach = guard.kit.range() + 8;
            double dist2 = eye.distanceSquared(aim);
            if (dist2 < 0.25 || dist2 > reach * reach) {
                continue;
            }
            float[] want = AimMath.yawPitch(eye.getX(), eye.getY(), eye.getZ(), aim.getX(), aim.getY(), aim.getZ());
            Location now = self.getLocation();
            float[] next = AimMath.step(now.getYaw(), now.getPitch(), want[0], want[1]);
            if (next[0] != now.getYaw() || next[1] != now.getPitch()) {
                look(self, next[0], next[1]);
            }
        }
    }

    /** Aktualny cel Sentinela (pole {@code chasing}) albo null. */
    private LivingEntity chasing(Object sentinel) {
        try {
            if (chasingField == null) {
                chasingField = sentinel.getClass().getField("chasing");
            }
            Object value = chasingField.get(sentinel);
            return value instanceof LivingEntity living ? living : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warnedAim) {
                warnedAim = true;
                plugin.getLogger().warning("Płynne celowanie strażników wyłączone (inna wersja Sentinela?): " + e);
            }
            return null;
        }
    }

    /** Obrót ciała i głowy NPC: przez Citizens (NMS.look), a gdy go nie ma – zwykłe setRotation. */
    private void look(LivingEntity entity, float yaw, float pitch) {
        if (!lookResolved) {
            lookResolved = true;
            try {
                lookMethod = Class.forName("net.citizensnpcs.util.NMS")
                        .getMethod("look", Entity.class, float.class, float.class);
            } catch (ReflectiveOperationException | RuntimeException e) {
                lookMethod = null;
            }
        }
        if (lookMethod != null) {
            try {
                lookMethod.invoke(null, entity, yaw, pitch);
                return;
            } catch (ReflectiveOperationException | RuntimeException e) {
                lookMethod = null;
            }
        }
        entity.setRotation(yaw, pitch);
    }

    private static Object call(Object target, String method, Class<?>[] types, Object... args)
            throws ReflectiveOperationException {
        Method m = findMethod(target.getClass(), method, types);
        return m.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>[] types) throws NoSuchMethodException {
        try {
            return type.getMethod(name, types);
        } catch (NoSuchMethodException e) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == types.length) {
                        m.setAccessible(true);
                        return m;
                    }
                }
            }
            throw e;
        }
    }

    private static boolean trySet(Object target, String method, Class<?> type, Object value) {
        try {
            target.getClass().getMethod(method, type).invoke(target, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static void setField(Object target, String name, Object value) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                // szukamy w nadklasie
            } catch (ReflectiveOperationException | RuntimeException e) {
                return;
            }
        }
    }

    /** Ile miejsc na strażników mają postawione struktury (do komendy). */
    /** Zestawy wymagane przez postawione struktury, których nie ma w sekcji straznicy.zestawy. */
    public java.util.Set<String> missingKits() {
        for (StructureInstance instance : store.all()) {
            Blueprint bp = library.get(instance.structureId());
            if (bp != null) {
                for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
                    String kitId = kitId(point.pool());
                    if (kitId != null && !settings.kits().containsKey(kitId)) {
                        missingKits.add(kitId);
                    }
                }
            }
        }
        return java.util.Collections.unmodifiableSet(missingKits);
    }

    public int guardPoints() {
        int n = 0;
        for (StructureInstance instance : store.all()) {
            Blueprint bp = library.get(instance.structureId());
            if (bp != null) {
                for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
                    if (kitId(point.pool()) != null) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** Usuwa strażników jednej struktury (przy usuwaniu albo cofaniu struktury). */
    public void forget(String instanceId) {
        Iterator<Map.Entry<String, Guard>> it = guards.entrySet().iterator();
        List<Guard> removed = new ArrayList<>();
        while (it.hasNext()) {
            Guard guard = it.next().getValue();
            if (guard.instanceId.equals(instanceId)) {
                removed.add(guard);
                it.remove();
            }
        }
        removed.forEach(this::destroy);
    }
}
