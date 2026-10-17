package pl.axon.structure.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
 * Strażnicy w strukturach: NPC-gracze z Citizens z walką z Sentinela (który obsługuje też broń QualityArmory).
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

    public record Kit(String id, String name, String skinTexture, String skinSignature, String skinName,
                      String weapon, List<String> armor, double health, double damage, double range,
                      double accuracy, String lootClass, List<String> targets) {

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
        Object npc;
        UUID entityId;
        long deadUntil;

        Guard(String instanceId, Kit kit, Location location) {
            this.instanceId = instanceId;
            this.kit = kit;
            this.location = location;
        }
    }

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final CrateService crates;
    private final Random random = new Random();
    private final Map<String, Guard> guards = new HashMap<>();
    private final Map<UUID, Guard> byEntity = new HashMap<>();
    private Settings settings;
    private BukkitTask task;
    private Object registry;
    private boolean warned;

    public GuardService(JavaPlugin plugin, StructureLibrary library, InstanceStore store, CrateService crates) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.crates = crates;
    }

    public void configure(Settings settings) {
        removeAll();
        this.settings = settings;
    }

    public void start() {
        stopTimer();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 15, 20L * 5);
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
    }

    /** Czy Citizens i Sentinel są na serwerze. */
    public boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("Citizens") && Bukkit.getPluginManager().isPluginEnabled("Sentinel");
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
                nearest = Math.min(nearest, instance.distanceSquared(l.getX(), l.getZ()));
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
        for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
            String kitId = kitId(point.pool());
            if (kitId == null) {
                continue;
            }
            String key = instance.id() + "#" + index++;
            Kit kit = settings.kits().get(kitId);
            if (kit == null) {
                if (!warned) {
                    warned = true;
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
            if (guard.npc != null || now < guard.deadUntil) {
                continue;
            }
            spawn(guard);
        }
    }

    /** Zestaw dla punktu ze struktury albo null, gdy to nie jest miejsce na NPC. */
    private static String kitId(String pool) {
        String p = pool.toLowerCase(Locale.ROOT);
        if (p.startsWith(POOL_PREFIX)) {
            return p.substring(POOL_PREFIX.length());
        }
        return p.equals(TRADER_POOL) ? TRADER_KIT : null;
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
        CrateClass loot = guard.kit.lootClass().isBlank() ? null : crates.loot().crateClass(guard.kit.lootClass());
        if (loot != null) {
            event.getDrops().addAll(loot.roll(random));
        }
        guard.deadUntil = System.currentTimeMillis() + settings.respawnMinutes() * 60_000L;
        // NPC sprzątamy tickiem później, gdy Citizens skończy obsługę śmierci
        Bukkit.getScheduler().runTask(plugin, () -> destroy(guard));
    }

    // ------------------------------------------------------------------ Citizens i Sentinel przez refleksję

    private void spawn(Guard guard) {
        try {
            Object npc = call(registry(), "createNPC", new Class<?>[]{EntityType.class, String.class},
                    EntityType.PLAYER, LootConfig.color(guard.kit.name()));
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
        if (guard.npc != null) {
            try {
                call(guard.npc, "destroy", new Class<?>[0]);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // NPC już zniknął
            }
            guard.npc = null;
        }
    }

    private boolean isSpawned(Object npc) {
        try {
            return Boolean.TRUE.equals(call(npc, "isSpawned", new Class<?>[0]));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
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
        if (!kit.skinTexture().isBlank() && !kit.skinSignature().isBlank()) {
            call(skin, "setSkinPersistent", new Class<?>[]{String.class, String.class, String.class},
                    kit.id(), kit.skinSignature(), kit.skinTexture());
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
            try {
                Class<?> qa = Class.forName("me.zombie_striker.qg.api.QualityArmory");
                Object item = qa.getMethod("getCustomItemAsItemStack", String.class).invoke(null, name);
                if (item instanceof ItemStack stack) {
                    return stack;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                // brak QualityArmory albo innej wersji API – niżej zapas
            }
            plugin.getLogger().warning("Nie ma broni QualityArmory '" + name + "', strażnik dostaje kuszę.");
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
        setField(sentinel, "accuracy", kit.accuracy());
        setField(sentinel, "chaseRange", Math.max(12.0, kit.range()));
        setField(sentinel, "respawnTime", -1L); // odradzaniem zajmuje się ten plugin
        setField(sentinel, "spawnPoint", guard.location.clone());
        setField(sentinel, "fightback", kit.attacksPlayers());
        setField(sentinel, "needsAmmo", false);
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
