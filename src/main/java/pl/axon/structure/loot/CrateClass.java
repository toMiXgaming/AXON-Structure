package pl.axon.structure.loot;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Klasa skrzyni z configu: jaki blok, jaka nazwa i jaki loot.
 *
 * <p>Wpis lootu może być trojaki:
 * <ul>
 *   <li>{@code przedmiot: iron_ingot} – zwykły przedmiot z Minecrafta,</li>
 *   <li>{@code przedmiot: axon:scrap} – przedmiot z AxonItems (tworzony przez API, z modelem i PDC),</li>
 *   <li>{@code tabela: beczka_przydrozna} – cała tabela lootu z AxonItems (z szansami i progiem dnia wipe'u).</li>
 * </ul>
 * Dzięki temu beczki przy drogach dają prawdziwy złom AxonCore, a nie przemianowane żelazne bryłki.
 */
public record CrateClass(String id, String displayName, Kind kind, String decoration, int minItems,
                         List<LootEntry> loot, String modelName, String zone, int refreshMinutes,
                         int lockMinutes) {

    /** Stary konstruktor – bez modelu i strefy (skrzynie blokowe). */
    public CrateClass(String id, String displayName, Kind kind, String decoration, int minItems,
                      List<LootEntry> loot) {
        this(id, displayName, kind, decoration, minItems, loot, null, null, 0, 0);
    }

    /** Konstruktor z modelem i strefą, bez własnego interwału odświeżania i bez blokady. */
    public CrateClass(String id, String displayName, Kind kind, String decoration, int minItems,
                      List<LootEntry> loot, String modelName, String zone) {
        this(id, displayName, kind, decoration, minItems, loot, modelName, zone, 0, 0);
    }

    /** Konstruktor z modelem, strefą i interwałem odświeżania, bez blokady. */
    public CrateClass(String id, String displayName, Kind kind, String decoration, int minItems,
                      List<LootEntry> loot, String modelName, String zone, int refreshMinutes) {
        this(id, displayName, kind, decoration, minItems, loot, modelName, zone, refreshMinutes, 0);
    }

    public CrateClass {
        if (modelName == null || modelName.isBlank()) {
            modelName = id;
        }
        if (zone == null) {
            zone = "";
        }
        if (refreshMinutes < 0) {
            refreshMinutes = 0;
        }
        if (lockMinutes < 0) {
            lockMinutes = 0;
        }
    }

    /**
     * Własny interwał odświeżania w minutach (0 = użyj globalnego z {@code odswiezanie.co-ile-minut}).
     * Skrzynia wojskowa ustawia tu 30, żeby odradzała się częściej niż reszta struktur.
     */
    public boolean hasOwnRefresh() {
        return refreshMinutes > 0;
    }

    /**
     * Czy skrzynia startuje zablokowana (np. elitarna otwiera się po 10 minutach od postawienia).
     * Blokada liczy się od momentu postawienia i jest pokazywana jako odliczanie nad modelem.
     */
    public boolean locked() {
        return lockMinutes > 0;
    }

    public enum Kind {
        CHEST, BARREL, FRAME, MODEL;

        public static Kind parse(String value) {
            return switch (value.toLowerCase(java.util.Locale.ROOT)) {
                case "chest", "skrzynia" -> CHEST;
                case "barrel", "beczka" -> BARREL;
                case "frame", "item_frame", "ramka" -> FRAME;
                case "model", "pojemnik", "pojemnik-model" -> MODEL;
                default -> throw new IllegalArgumentException("nieznany blok '" + value
                        + "' (dozwolone: skrzynia, beczka, ramka, model)");
            };
        }

        /** Czy klasa stawia prawdziwy blok (skrzynia/beczka) zamiast pojemnika-modelu. */
        public boolean isBlock() {
            return this == CHEST || this == BARREL;
        }
    }

    /**
     * Jeden wpis lootu.
     *
     * @param material  blok/przedmiot Minecrafta albo {@code null}, gdy wpis wskazuje przedmiot AxonItems
     * @param axonId    id przedmiotu AxonItems (bez prefiksu) albo {@code null}
     * @param tableId   id tabeli lootu AxonItems albo {@code null}
     * @param min       dolna granica ilości
     * @param max       górna granica ilości
     * @param chance    szansa w procentach
     * @param name      opcjonalna nazwa (tylko dla przedmiotów Minecrafta)
     * @param lore      opcjonalny opis (tylko dla przedmiotów Minecrafta)
     */
    public record LootEntry(Material material, String axonId, String tableId, int min, int max, double chance,
                            String name, List<String> lore) {

        public static LootEntry ofMaterial(Material material, int min, int max, double chance,
                                           String name, List<String> lore) {
            return new LootEntry(material, null, null, min, max, chance, name, lore);
        }

        public static LootEntry ofAxon(String axonId, int min, int max, double chance) {
            return new LootEntry(null, axonId, null, min, max, chance, null, List.of());
        }

        public static LootEntry ofTable(String tableId) {
            return new LootEntry(null, null, tableId, 1, 1, 100.0, null, List.of());
        }

        public boolean isTable() {
            return tableId != null;
        }

        public boolean isAxon() {
            return axonId != null;
        }

        /** Przedmiot z Minecrafta – jedyny wariant, który sam potrafi się zbudować bez AxonItems. */
        @SuppressWarnings("deprecation")
        public ItemStack createVanilla(Random random) {
            if (material == null) {
                return null;
            }
            int amount = min + (max > min ? random.nextInt(max - min + 1) : 0);
            ItemStack item = new ItemStack(material, Math.max(1, amount));
            if (name != null || !lore.isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (name != null) {
                        meta.setDisplayName(name);
                    }
                    if (!lore.isEmpty()) {
                        meta.setLore(lore);
                    }
                    item.setItemMeta(meta);
                }
            }
            return item;
        }

        /** Ilość do wylosowania (dla wpisów AxonItems). */
        public int rollAmount(Random random) {
            return min + (max > min ? random.nextInt(max - min + 1) : 0);
        }
    }

    /**
     * Losuje przedmioty: każdy wpis ma własną szansę, a gdy nic nie wypadnie, dobiera do {@code minItems}.
     *
     * <p>Wpisy AxonItems i tabele wymagają mostu do AxonItems – bez niego są pomijane (skrzynia
     * dostanie wtedy tylko przedmioty z Minecrafta, więc serwer bez AxonItems nadal działa).
     */
    public List<ItemStack> roll(Random random, AxonLootBridge bridge) {
        List<ItemStack> items = new ArrayList<>();
        for (LootEntry entry : loot) {
            if (random.nextDouble() * 100.0 >= entry.chance()) {
                continue;
            }
            if (entry.isTable()) {
                if (bridge != null) {
                    items.addAll(bridge.rollTable(entry.tableId()));
                }
                continue;
            }
            if (entry.isAxon()) {
                if (bridge != null) {
                    ItemStack item = bridge.createItem(entry.axonId(), entry.rollAmount(random));
                    if (item != null) {
                        items.add(item);
                    }
                }
                continue;
            }
            ItemStack vanilla = entry.createVanilla(random);
            if (vanilla != null) {
                items.add(vanilla);
            }
        }
        int guard = 0;
        while (items.size() < minItems && !loot.isEmpty() && guard++ < 32) {
            LootEntry entry = loot.get(random.nextInt(loot.size()));
            if (entry.isTable()) {
                if (bridge != null) {
                    items.addAll(bridge.rollTable(entry.tableId()));
                }
            } else if (entry.isAxon()) {
                if (bridge != null) {
                    ItemStack item = bridge.createItem(entry.axonId(), entry.rollAmount(random));
                    if (item != null) {
                        items.add(item);
                    }
                }
            } else {
                ItemStack vanilla = entry.createVanilla(random);
                if (vanilla != null) {
                    items.add(vanilla);
                }
            }
        }
        return items;
    }

    public void fill(Inventory inventory, Random random, AxonLootBridge bridge) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);
        int slot = 0;
        for (ItemStack item : roll(random, bridge)) {
            if (slot >= slots.size()) {
                break;
            }
            inventory.setItem(slots.get(slot++), item);
        }
    }
}
