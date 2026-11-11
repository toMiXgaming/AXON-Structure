package pl.axon.structure.loot;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Pancerze AXON z pluginu AxonArmor ({@code axon:cien_helm} w lootcie i sklepie). Przez refleksję, bo AxonArmor
 * nie jest zależnością przy budowaniu i może go nie być na serwerze. Wtedy zwraca null i wpis jest pomijany.
 */
public final class AxonArmorBridge {

    private static final String PLUGIN = "AxonArmor";
    private static final String API_CLASS = "pl.axon.armor.AxonArmorAPI";

    private static Method createItem;
    private static Method itemIds;

    private AxonArmorBridge() {
    }

    /** Element pancerza po id (np. {@code cien_helm}) albo null, gdy AxonArmor nie działa albo nie zna id. */
    public static ItemStack create(String id, int amount) {
        if (!resolve()) {
            return null;
        }
        try {
            Object result = createItem.invoke(null, id, Math.max(1, amount));
            return result instanceof ItemStack item ? item : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            createItem = null; // AxonArmor mógł zostać przeładowany; następnym razem połączymy się od nowa
            return null;
        }
    }

    /** Czy AxonArmor działa i zna to id. */
    public static boolean known(String id) {
        if (!resolve()) {
            return false;
        }
        try {
            return ((java.util.Collection<?>) itemIds.invoke(null)).contains(id);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean resolve() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN);
        if (plugin == null || !plugin.isEnabled()) {
            createItem = null;
            return false;
        }
        if (createItem != null && itemIds != null) {
            return true;
        }
        try {
            Class<?> type = Class.forName(API_CLASS, true, plugin.getClass().getClassLoader());
            createItem = type.getMethod("createItem", String.class, int.class);
            itemIds = type.getMethod("itemIds");
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }
}
