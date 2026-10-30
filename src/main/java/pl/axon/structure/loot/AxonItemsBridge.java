package pl.axon.structure.loot;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Przedmioty AxonCore z pluginu AxonItems ({@code axon:scrap} w lootcie). Przez refleksję, bo AxonItems nie jest
 * zależnością przy budowaniu i może go nie być na serwerze. Wtedy pozycje {@code axon:} są po prostu pomijane.
 */
public final class AxonItemsBridge {

    public static final String PREFIX = "axon:";
    /** Wpis lootu, który losuje całą tabelę z loot.yml w AxonItems, np. {@code tabela:skrzynia_wojskowa}. */
    public static final String TABLE_PREFIX = "tabela:";
    private static final String PLUGIN = "AxonItems";
    private static final String API_CLASS = "pl.axonitems.api.AxonItemsApi";

    private static Object api;
    private static Method create;
    private static Method exists;
    private static Method rollLoot;
    private static boolean warnedMissing;
    private static final Set<String> warnedIds = new HashSet<>();

    private AxonItemsBridge() {
    }

    /** Przedmiot AxonCore albo null (brak AxonItems albo nieznane id; ostrzeżenie w logu raz). */
    public static ItemStack create(String id, int amount) {
        if (!resolve()) {
            return null;
        }
        try {
            Object result = create.invoke(api, id, amount);
            if (result instanceof Optional<?> optional && optional.isPresent()
                    && optional.get() instanceof ItemStack item) {
                return item;
            }
            if (warnedIds.add(id)) {
                log("AxonItems nie zna id '" + id + "' – pomijam je w lootcie (sprawdź items.yml w AxonItems).");
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            api = null; // AxonItems mógł zostać przeładowany; następnym razem pobierzemy API od nowa
            log("Błąd przy tworzeniu przedmiotu " + PREFIX + id + " w AxonItems: " + e);
            return null;
        }
    }

    /**
     * Losuje tabelę lootu z {@code loot.yml} w AxonItems (wpis {@code tabela:<id>}). AxonItems pilnuje przy tym
     * dni wipe'u ({@code od-dnia}), więc np. korpusy T3 nie wypadną przed 10. dniem. Pusta lista, gdy AxonItems
     * nie działa albo nie zna tabeli.
     */
    public static List<ItemStack> rollTable(String table) {
        if (!resolve() || rollLoot == null) {
            return List.of();
        }
        try {
            Object result = rollLoot.invoke(api, table);
            if (!(result instanceof List<?> list)) {
                return List.of();
            }
            List<ItemStack> items = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ItemStack stack) {
                    items.add(stack);
                }
            }
            if (items.isEmpty() && warnedIds.add("tabela:" + table)) {
                log("Tabela lootu '" + table + "' w AxonItems nic nie dała (nieznana tabela albo wszystko zablokowane"
                        + " dniem wipe'u).");
            }
            return items;
        } catch (ReflectiveOperationException | RuntimeException e) {
            api = null;
            log("Błąd przy losowaniu tabeli " + TABLE_PREFIX + table + " w AxonItems: " + e);
            return List.of();
        }
    }

    /** Czy AxonItems działa i zna to id (do komend i podglądu klas). */
    public static boolean known(String id) {
        if (!resolve()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(exists.invoke(api, id));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public static boolean available() {
        return resolve();
    }

    private static boolean resolve() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN);
        if (plugin == null || !plugin.isEnabled()) {
            api = null;
            if (!warnedMissing) {
                warnedMissing = true;
                log("Brak pluginu " + PLUGIN + ": przedmioty " + PREFIX + "... w lootcie są pomijane.");
            }
            return false;
        }
        if (api != null && create != null) {
            return true;
        }
        try {
            Class<?> type = Class.forName(API_CLASS, true, plugin.getClass().getClassLoader());
            Object service = Bukkit.getServicesManager().load(type);
            if (service == null) {
                return false;
            }
            create = type.getMethod("create", String.class, int.class);
            exists = type.getMethod("exists", String.class);
            try {
                rollLoot = type.getMethod("rollLoot", String.class);
            } catch (NoSuchMethodException old) {
                rollLoot = null; // starszy AxonItems bez tabel lootu
            }
            api = service;
            warnedMissing = false;
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (!warnedMissing) {
                warnedMissing = true;
                log("Nie udało się połączyć z API " + PLUGIN + " (" + e + "): przedmioty " + PREFIX
                        + "... w lootcie są pomijane.");
            }
            return false;
        }
    }

    private static void log(String message) {
        JavaPlugin.getProvidingPlugin(AxonItemsBridge.class).getLogger().warning(message);
    }
}
