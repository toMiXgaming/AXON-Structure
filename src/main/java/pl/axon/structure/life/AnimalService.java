package pl.axon.structure.life;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import pl.axon.structure.StructureLibrary;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.oasis.OasisService;
import pl.axon.structure.world.InstanceStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Zwierzęta w strukturach: punkty {@code punkt-zwierze-<typ>} z pliku struktury (np. {@code punkt-zwierze-chicken}).
 * Gdy gracz jest w pobliżu struktury, na każdym punkcie stoi jedno zwierzę; zabite wraca po {@code odradzanie-minut}.
 * Zwierzęta nie są zapisywane ze światem ({@code persistent = false}) i mają znacznik PDC, więc nie mnożą się po
 * restartach i przeładowaniach chunków. Oswojone albo wzięte na smycz przestają być dekoracją i zostają u gracza.
 * W oazie (strefa bez PvP) gracze nie mogą ich ranić.
 */
public final class AnimalService implements Listener {

    public static final String POOL_PREFIX = "punkt-zwierze-";

    /**
     * @param activationRadius zwierzęta pojawiają się, gdy gracz jest bliżej niż tyle bloków od środka struktury
     * @param maxStray         zwierzę, które odejdzie dalej od swojego punktu, wraca na miejsce
     */
    public record Settings(boolean enabled, int activationRadius, int respawnMinutes, int maxStray) {
    }

    private static final class Spot {
        final String instanceId;
        final EntityType type;
        final Location location;
        UUID entity;
        long deadUntil;

        Spot(String instanceId, EntityType type, Location location) {
            this.instanceId = instanceId;
            this.type = type;
            this.location = location;
        }
    }

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("kura", "chicken"), Map.entry("kurczak", "chicken"), Map.entry("krowa", "cow"),
            Map.entry("owca", "sheep"), Map.entry("swinia", "pig"), Map.entry("świnia", "pig"),
            Map.entry("kot", "cat"), Map.entry("wilk", "wolf"), Map.entry("pies", "wolf"), Map.entry("lis", "fox"),
            Map.entry("kon", "horse"), Map.entry("koń", "horse"), Map.entry("osiol", "donkey"),
            Map.entry("osioł", "donkey"), Map.entry("krolik", "rabbit"), Map.entry("królik", "rabbit"),
            Map.entry("koza", "goat"), Map.entry("nietoperz", "bat"));

    private final JavaPlugin plugin;
    private final StructureLibrary library;
    private final InstanceStore store;
    private final OasisService oasis;
    private final NamespacedKey tag;
    private final Map<String, Spot> spots = new HashMap<>();
    private final Map<UUID, Spot> byEntity = new HashMap<>();
    private final Set<String> warnedTypes = new HashSet<>();
    private Settings settings;
    private BukkitTask task;

    public AnimalService(JavaPlugin plugin, StructureLibrary library, InstanceStore store, OasisService oasis) {
        this.plugin = plugin;
        this.library = library;
        this.store = store;
        this.oasis = oasis;
        this.tag = new NamespacedKey(plugin, "zwierze");
    }

    public void configure(Settings settings) {
        // punkty mogły się przesunąć (nowe pliki struktur): stawiamy od nowa
        removeAll();
        warnedTypes.clear();
        this.settings = settings;
    }

    public void start() {
        stopTimer();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 20, 20L * 5);
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

    private void tick() {
        if (!settings.enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        double activate = (double) settings.activationRadius() * settings.activationRadius();
        for (StructureInstance instance : store.all()) {
            World world = Bukkit.getWorld(instance.world());
            if (world == null) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                Location l = player.getLocation();
                if (instance.edgeDistanceSquared(l.getX(), l.getZ()) <= activate) {
                    spawnAnimals(instance, world, now);
                    break;
                }
            }
        }
        double stray = (double) settings.maxStray() * settings.maxStray();
        Iterator<Map.Entry<String, Spot>> it = spots.entrySet().iterator();
        while (it.hasNext()) {
            Spot spot = it.next().getValue();
            if (store.get(spot.instanceId) == null) {
                // struktura cofnięta albo zapomniana
                remove(spot);
                it.remove();
                continue;
            }
            if (spot.entity == null) {
                continue;
            }
            Entity entity = Bukkit.getEntity(spot.entity);
            if (entity == null || !entity.isValid()) {
                // zdjęte razem z chunkiem (nie jest zapisywane) – postawi się znowu, gdy ktoś podejdzie
                byEntity.remove(spot.entity);
                spot.entity = null;
            } else if (settings.maxStray() > 0 && entity.getPassengers().isEmpty()
                    && (entity.getWorld() != spot.location.getWorld()
                    || entity.getLocation().distanceSquared(spot.location) > stray)) {
                entity.teleport(spot.location);
            }
        }
    }

    private void spawnAnimals(StructureInstance instance, World world, long now) {
        Blueprint bp = library.get(instance.structureId());
        if (bp == null) {
            return;
        }
        int index = 0;
        for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
            EntityType type = animalType(point.pool());
            if (type == null) {
                continue;
            }
            String key = instance.id() + "#" + index++;
            int[] w = instance.placement().toWorld(point.pos().x(), point.pos().y(), point.pos().z());
            if (!world.isChunkLoaded(w[0] >> 4, w[2] >> 4)) {
                continue;
            }
            Spot spot = spots.computeIfAbsent(key, k -> new Spot(instance.id(), type, standOn(world, w)));
            if (spot.entity != null || now < spot.deadUntil) {
                continue;
            }
            spawn(spot);
        }
    }

    /** Miejsce dla zwierzęcia: sam punkt albo blok wyżej, gdy punkt leży na pełnym bloku. */
    private static Location standOn(World world, int[] w) {
        Block block = world.getBlockAt(w[0], w[1], w[2]);
        int y = block.isPassable() ? w[1] : w[1] + 1;
        return new Location(world, w[0] + 0.5, y, w[2] + 0.5);
    }

    /** Typ zwierzęcia z nazwy punktu albo null, gdy to nie punkt zwierzęcia (albo nieznany typ). */
    private EntityType animalType(String pool) {
        String p = pool.toLowerCase(Locale.ROOT);
        if (!p.startsWith(POOL_PREFIX)) {
            return null;
        }
        String name = p.substring(POOL_PREFIX.length());
        name = ALIASES.getOrDefault(name, name);
        EntityType type = null;
        NamespacedKey key = NamespacedKey.fromString(name);
        if (key != null) {
            type = Registry.ENTITY_TYPE.get(key);
        }
        Class<? extends Entity> entityClass = type == null ? null : type.getEntityClass();
        boolean ok = entityClass != null && type.isSpawnable() && (Animals.class.isAssignableFrom(entityClass)
                || WaterMob.class.isAssignableFrom(entityClass) || Ambient.class.isAssignableFrom(entityClass));
        if (!ok) {
            if (warnedTypes.add(name)) {
                plugin.getLogger().warning("Punkt '" + pool + "': '" + name + "' to nie zwierzę z Minecrafta "
                        + "(np. chicken, cow, sheep, pig, cat, wolf, fox, horse) – to miejsce zostaje puste.");
            }
            return null;
        }
        return type;
    }

    private void spawn(Spot spot) {
        try {
            Entity entity = spot.location.getWorld().spawn(spot.location, spot.type.getEntityClass(), e -> {
                e.setPersistent(false);
                if (e instanceof LivingEntity living) {
                    living.setRemoveWhenFarAway(false);
                }
                e.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, (byte) 1);
            });
            if (entity == null || !entity.isValid()) {
                return; // inny plugin zablokował pojawienie się
            }
            spot.entity = entity.getUniqueId();
            byEntity.put(spot.entity, spot);
        } catch (RuntimeException e) {
            if (warnedTypes.add("spawn:" + spot.type)) {
                plugin.getLogger().warning("Nie udało się postawić zwierzęcia " + spot.type + ": " + e.getMessage());
            }
        }
    }

    private void remove(Spot spot) {
        if (spot.entity == null) {
            return;
        }
        byEntity.remove(spot.entity);
        Entity entity = Bukkit.getEntity(spot.entity);
        if (entity != null) {
            entity.remove();
        }
        spot.entity = null;
    }

    private void removeAll() {
        for (Spot spot : spots.values()) {
            remove(spot);
        }
        spots.clear();
        byEntity.clear();
    }

    /** Zwierzę przestaje być dekoracją (oswojone, na smyczy): zapisuje się ze światem jak każde inne. */
    private void release(Entity entity) {
        Spot spot = byEntity.remove(entity.getUniqueId());
        if (spot == null) {
            return;
        }
        entity.setPersistent(true);
        entity.getPersistentDataContainer().remove(tag);
        spot.entity = null;
        spot.deadUntil = System.currentTimeMillis() + settings.respawnMinutes() * 60_000L;
    }

    // ------------------------------------------------------------------ zdarzenia

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        Spot spot = byEntity.remove(event.getEntity().getUniqueId());
        if (spot != null) {
            spot.entity = null;
            spot.deadUntil = System.currentTimeMillis() + settings.respawnMinutes() * 60_000L;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        release(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        release(event.getEntity());
    }

    /** Resztki po awarii albo starej sesji: nasze zwierzę, którego nie pilnujemy, znika. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity.getPersistentDataContainer().has(tag, PersistentDataType.BYTE)
                    && !byEntity.containsKey(entity.getUniqueId())) {
                entity.remove();
            }
        }
    }

    /** W oazie zwierzęta to część wystroju: gracze ich nie ranią (tak jak siebie nawzajem). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (!byEntity.containsKey(victim.getUniqueId()) || attacker(event.getDamager()) == null) {
            return;
        }
        if (oasis.safeAt(victim.getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        ProjectileSource source = null;
        if (damager instanceof Projectile projectile) {
            source = projectile.getShooter();
        } else if (damager instanceof AreaEffectCloud cloud) {
            source = cloud.getSource();
        } else if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            return p;
        }
        return source instanceof Player p ? p : null;
    }

    // ------------------------------------------------------------------ do komend

    public int activeCount() {
        return byEntity.size();
    }

    /** Ile punktów zwierząt mają postawione struktury. */
    public int spotCount() {
        int n = 0;
        for (StructureInstance instance : store.all()) {
            Blueprint bp = library.get(instance.structureId());
            if (bp != null) {
                for (Blueprint.SpawnPoint point : bp.spawnPoints()) {
                    if (point.pool().toLowerCase(Locale.ROOT).startsWith(POOL_PREFIX)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }
}
