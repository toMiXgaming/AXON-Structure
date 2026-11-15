package pl.axon.structure.life;

import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.BoundingBox;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.util.Map;
import java.util.Random;

/**
 * Ograniczenie naturalnego spawnu potworów w obrysie struktur (np. ciemna galeria, w której bez tego roi się od
 * zombie). Struktury bez własnego wpisu dostają ograniczenie domyślne. Działa tylko na spawn naturalny: potwory
 * z pluginów, komend, spawnerów i jajek przechodzą.
 */
public final class MonsterLimitService implements Listener {

    /**
     * @param chance szansa, że naturalny spawn potwora w obrysie się uda (0 = wcale, 1 = jak wszędzie)
     * @param max    najwięcej potworów naraz w obrysie struktury; 0 = bez limitu
     */
    public record Limit(double chance, int max) {
    }

    private final InstanceStore store;
    private final Random random = new Random();
    private Map<String, Limit> limits = Map.of();
    private Limit defaultLimit;

    public MonsterLimitService(InstanceStore store) {
        this.store = store;
    }

    /**
     * @param limits       id struktury (małymi literami) → ograniczenie
     * @param defaultLimit ograniczenie dla struktur bez własnego wpisu; null = bez ograniczenia
     */
    public void configure(Map<String, Limit> limits, Limit defaultLimit) {
        this.limits = Map.copyOf(limits);
        this.defaultLimit = defaultLimit;
    }

    /** Ograniczenie dla struktury: jej własny wpis, a gdy go nie ma, domyślne (może być null). */
    public static Limit limitFor(Map<String, Limit> limits, Limit defaultLimit, String structureId) {
        Limit own = limits.get(structureId.toLowerCase(java.util.Locale.ROOT));
        return own != null ? own : defaultLimit;
    }

    /** Czy przepuścić spawn: najpierw limit liczby potworów, potem losowanie szansy. */
    public static boolean allow(Limit limit, double roll, int monstersInside) {
        if (limit.max() > 0 && monstersInside >= limit.max()) {
            return false;
        }
        return roll < limit.chance();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if ((limits.isEmpty() && defaultLimit == null) || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
                || !(event.getEntity() instanceof Enemy)) {
            return;
        }
        World world = event.getLocation().getWorld();
        if (world == null) {
            return;
        }
        int x = event.getLocation().getBlockX();
        int z = event.getLocation().getBlockZ();
        for (StructureInstance instance : store.all()) {
            Limit limit = limitFor(limits, defaultLimit, instance.structureId());
            Box box = instance.footprint();
            if (limit == null || box == null || !instance.world().equals(world.getName()) || !box.contains(x, z)) {
                continue;
            }
            int inside = 0;
            if (limit.max() > 0) {
                BoundingBox area = new BoundingBox(box.minX(), world.getMinHeight(), box.minZ(),
                        box.maxX() + 1, world.getMaxHeight(), box.maxZ() + 1);
                inside = world.getNearbyEntities(area, e -> e instanceof Enemy).size();
            }
            if (!allow(limit, random.nextDouble(), inside)) {
                event.setCancelled(true);
            }
            return;
        }
    }
}
