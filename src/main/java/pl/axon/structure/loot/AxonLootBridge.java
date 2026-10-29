package pl.axon.structure.loot;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Refleksyjny most do AxonItems – bez zależności Maven, tak samo jak most do QualityArmory.
 *
 * <p>AXON-Structure nie może mieć AxonItems na classpath (AxonItems zależy od Papera i ma własny
 * cykl wydawniczy), więc wołamy publiczne API przez refleksję. Gdy AxonItems nie jest zainstalowany,
 * most jest nieaktywny i skrzynie po prostu nie dostają przedmiotów AxonCore – serwer działa dalej.
 *
 * <p>Rozwiązywane leniwie, raz na start i po każdym {@code /struktury przeladuj}.
 */
public final class AxonLootBridge {

    private static final String PLUGIN_NAME = "AxonItems";
    private static final String API_CLASS = "pl.axonitems.api.AxonItems";

    private final Logger logger;
    private final Set<String> warnedTables = new HashSet<>();
    private final Set<String> warnedItems = new HashSet<>();

    private Object api;
    private Method createMethod;
    private Method rollLootMethod;
    private Method hasLootTableMethod;
    private Method wipeDayMethod;
    private boolean resolved;
    private boolean available;

    public AxonLootBridge(Logger logger) {
        this.logger = logger;
    }

    /** Wołane przy starcie i przy przeładowaniu – pozwala wykryć AxonItems dołączony później. */
    public void resolve() {
        resolved = true;
        available = false;
        api = null;
        createMethod = null;
        rollLootMethod = null;
        hasLootTableMethod = null;
        wipeDayMethod = null;

        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        try {
            Class<?> holder = Class.forName(API_CLASS, true, plugin.getClass().getClassLoader());
            Method apiMethod = holder.getMethod("api");
            Object instance = apiMethod.invoke(null);
            if (instance == null) {
                logger.warning("AxonItems jest włączony, ale AxonItems.api() zwróciło null – loot AxonCore wyłączony.");
                return;
            }
            Class<?> apiType = instance.getClass();
            createMethod = findMethod(apiType, "create", String.class, int.class);
            rollLootMethod = findMethod(apiType, "rollLoot", String.class);
            hasLootTableMethod = findMethod(apiType, "hasLootTable", String.class);
            wipeDayMethod = findMethod(apiType, "wipeDay");
            if (createMethod == null || rollLootMethod == null) {
                logger.warning("AxonItems nie ma oczekiwanego API (create/rollLoot) – loot AxonCore wyłączony.");
                return;
            }
            api = instance;
            available = true;
            logger.info("Podłączono loot AxonCore z AxonItems (dzień wipe'u: " + wipeDay() + ").");
        } catch (ClassNotFoundException e) {
            logger.info("AxonItems nie znaleziony – skrzynie będą miały tylko przedmioty z Minecrafta.");
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.warning("Nie udało się podłączyć API AxonItems: " + e.getMessage());
        }
    }

    public boolean available() {
        if (!resolved) {
            resolve();
        }
        return available;
    }

    /** Tworzy przedmiot AxonItems albo {@code null}, gdy id nie istnieje / most nieaktywny. */
    public ItemStack createItem(String axonId, int amount) {
        if (!available() || axonId == null) {
            return null;
        }
        try {
            Object result = createMethod.invoke(api, axonId, Math.max(1, amount));
            if (result instanceof ItemStack stack) {
                return stack;
            }
            if (warnedItems.add(axonId)) {
                logger.warning("AxonItems nie zna przedmiotu 'axon:" + axonId + "' – pomijam w skrzyniach.");
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (warnedItems.add(axonId)) {
                logger.warning("Błąd tworzenia 'axon:" + axonId + "': " + e.getMessage());
            }
        }
        return null;
    }

    /** Losuje całą tabelę lootu AxonItems (z szansami i progiem dnia wipe'u). */
    public List<ItemStack> rollTable(String tableId) {
        if (!available() || tableId == null) {
            return List.of();
        }
        if (hasLootTableMethod != null) {
            try {
                Object has = hasLootTableMethod.invoke(api, tableId);
                if (has instanceof Boolean present && !present) {
                    if (warnedTables.add(tableId)) {
                        logger.warning("AxonItems nie ma tabeli lootu '" + tableId + "' – pomijam.");
                    }
                    return List.of();
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // brak metody hasLootTable to nie błąd – po prostu spróbujemy wylosować
            }
        }
        try {
            Object result = rollLootMethod.invoke(api, tableId);
            if (result instanceof List<?> list) {
                List<ItemStack> items = new ArrayList<>(list.size());
                for (Object element : list) {
                    if (element instanceof ItemStack stack) {
                        items.add(stack);
                    }
                }
                return items;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (warnedTables.add(tableId)) {
                logger.warning("Błąd losowania tabeli '" + tableId + "': " + e.getMessage());
            }
        }
        return List.of();
    }

    /** Dzień wipe'u z AxonItems (0, gdy zegar wyłączony albo most nieaktywny). */
    public int wipeDay() {
        if (!available() || wipeDayMethod == null) {
            return 0;
        }
        try {
            Object result = wipeDayMethod.invoke(api);
            if (result instanceof Integer day) {
                return day;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // zostaje 0
        }
        return 0;
    }

    public String describe() {
        return available() ? "AxonItems podłączony (dzień " + wipeDay() + ")" : "AxonItems niepodłączony";
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            Method method = type.getMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
