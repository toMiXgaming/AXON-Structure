package pl.axon.structure.loot;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Klasa skrzyni z configu: jaki blok, jaka nazwa i jaki loot. */
public record CrateClass(String id, String displayName, Kind kind, String decoration, int minItems,
                         List<LootEntry> loot) {

    public enum Kind {
        CHEST, BARREL, FRAME;

        public static Kind parse(String value) {
            return switch (value.toLowerCase(java.util.Locale.ROOT)) {
                case "chest", "skrzynia" -> CHEST;
                case "barrel", "beczka" -> BARREL;
                case "frame", "item_frame", "ramka" -> FRAME;
                default -> throw new IllegalArgumentException("nieznany blok '" + value
                        + "' (dozwolone: skrzynia, beczka, ramka)");
            };
        }
    }

    public record LootEntry(Material material, int min, int max, double chance, String name, List<String> lore) {

        @SuppressWarnings("deprecation")
        public ItemStack create(Random random) {
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
    }

    /** Losuje przedmioty: każdy wpis ma własną szansę, a gdy nic nie wypadnie, dobiera do {@code minItems}. */
    public List<ItemStack> roll(Random random) {
        List<ItemStack> items = new ArrayList<>();
        for (LootEntry entry : loot) {
            if (random.nextDouble() * 100.0 < entry.chance()) {
                items.add(entry.create(random));
            }
        }
        int guard = 0;
        while (items.size() < minItems && !loot.isEmpty() && guard++ < 32) {
            items.add(loot.get(random.nextInt(loot.size())).create(random));
        }
        return items;
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
