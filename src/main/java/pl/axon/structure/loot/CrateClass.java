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
 * Klasa skrzyni z configu: jaki blok, jaka nazwa i jaki loot. {@code model} to klucz modelu przedmiotu
 * z paczki zasobów (np. {@code axon:zrzut}), rysowanego w miejscu bloku; pusty = zwykły blok.
 * Dla {@link Kind#MODEL} nie ma bloku wcale: model to cały pojemnik, a {@code container} mówi, jak się zachowuje.
 */
public record CrateClass(String id, String displayName, Kind kind, String decoration, String model, int minItems,
                         List<LootEntry> loot, ContainerSpec container) {

    public enum Kind {
        CHEST, BARREL, FRAME, MODEL;

        public static Kind parse(String value) {
            return switch (value.toLowerCase(java.util.Locale.ROOT)) {
                case "chest", "skrzynia" -> CHEST;
                case "barrel", "beczka" -> BARREL;
                case "frame", "item_frame", "ramka" -> FRAME;
                case "model", "pojemnik" -> MODEL;
                default -> throw new IllegalArgumentException("nieznany blok '" + value
                        + "' (dozwolone: skrzynia, beczka, ramka, model)");
            };
        }
    }

    /**
     * Pojemnik-model (ItemDisplay + Interaction, bez bloku).
     *
     * @param brokenModel   model po rozbiciu; pusty = pojemnik się nie rozbija, PPM od razu go otwiera
     * @param hits          uderzenia ręką potrzebne do rozbicia
     * @param toolHits      uderzenia czymkolwiek w ręce (narzędzie, broń)
     * @param respawnMillis po jakim czasie pojemnik wraca; -1 = wartość z {@code pojemniki.odnowienie-minut}
     * @param announce      komunikat na czacie, gdy pojemnik się odnowi; pusty = bez komunikatu
     * @param length        ile bloków zajmuje wzdłuż swojej osi x (skrzynia wojskowa: 2)
     * @param width         szerokość encji Interaction (klikalny obszar)
     * @param height        wysokość encji Interaction
     */
    public record ContainerSpec(String brokenModel, int hits, int toolHits, long respawnMillis, String announce,
                                int length, float width, float height) {

        public boolean breakable() {
            return !brokenModel.isBlank();
        }
    }

    /**
     * Pozycja lootu: przedmiot z Minecrafta ({@code material}) albo przedmiot AxonCore ({@code axonId},
     * np. {@code scrap} z wpisu {@code axon:scrap}) tworzony przez plugin AxonItems.
     */
    public record LootEntry(Material material, String axonId, int min, int max, double chance, String name,
                            List<String> lore, String table) {

        public LootEntry(Material material, String axonId, int min, int max, double chance, String name,
                         List<String> lore) {
            this(material, axonId, min, max, chance, name, lore, null);
        }

        /** Wpis {@code tabela:<id>}: cała tabela z loot.yml w AxonItems. */
        public static LootEntry table(String table, double chance) {
            return new LootEntry(null, null, 1, 1, chance, null, List.of(), table);
        }

        /** Nowy przedmiot albo null, gdy to przedmiot AxonCore, a AxonItems nie działa albo nie zna tego id. */
        @SuppressWarnings("deprecation")
        public ItemStack create(Random random) {
            int amount = min + (max > min ? random.nextInt(max - min + 1) : 0);
            if (axonId != null) {
                // AxonItems sam nadaje nazwę, opis, model i PDC; nazwa z configu tylko by to zasłoniła
                return AxonItemsBridge.create(axonId, Math.max(1, amount));
            }
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
    }

    /** Losuje przedmioty: każdy wpis ma własną szansę, a gdy nic nie wypadnie, dobiera do {@code minItems}. */
    public List<ItemStack> roll(Random random) {
        List<ItemStack> items = new ArrayList<>();
        for (LootEntry entry : loot) {
            if (random.nextDouble() * 100.0 < entry.chance()) {
                add(items, entry, random);
            }
        }
        int guard = 0;
        while (items.size() < minItems && !loot.isEmpty() && guard++ < 32) {
            add(items, loot.get(random.nextInt(loot.size())), random);
        }
        return items;
    }

    private static void add(List<ItemStack> items, ItemStack item) {
        if (item != null) {
            items.add(item);
        }
    }

    private static void add(List<ItemStack> items, LootEntry entry, Random random) {
        if (entry.table() != null) {
            items.addAll(AxonItemsBridge.rollTable(entry.table()));
        } else {
            add(items, entry.create(random));
        }
    }

    public void fill(Inventory inventory, Random random) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);
        int slot = 0;
        for (ItemStack item : roll(random)) {
            if (slot >= slots.size()) {
                break;
            }
            inventory.setItem(slots.get(slot++), item);
        }
    }
}
