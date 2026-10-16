package pl.axon.structure.core;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Rodzaje pojemników z Rusta przełożone na skrzynie i beczki z Minecrafta. */
public enum LootType {

    SCRAP_BARREL("Beczka",
            entry(Material.IRON_NUGGET, 4, 14, 100, "Złom"),
            entry(Material.COPPER_INGOT, 1, 4, 50, null),
            entry(Material.IRON_INGOT, 1, 2, 30, null),
            entry(Material.CHAIN, 1, 2, 20, "Rura metalowa"),
            entry(Material.REDSTONE, 1, 4, 25, null),
            entry(Material.GUNPOWDER, 1, 2, 15, null),
            entry(Material.STRING, 1, 3, 20, "Lina")),

    FUEL_BARREL("Beczka paliwa",
            entry(Material.COAL, 4, 12, 100, "Paliwo niskiej jakości"),
            entry(Material.CHARCOAL, 2, 6, 50, null),
            entry(Material.IRON_NUGGET, 2, 6, 40, "Złom"),
            entry(Material.LAVA_BUCKET, 1, 1, 8, "Ropa naftowa"),
            entry(Material.BLAZE_POWDER, 1, 1, 5, null)),

    BROWN_CRATE("Brązowa skrzynia",
            entry(Material.IRON_NUGGET, 6, 20, 100, "Złom"),
            entry(Material.IRON_INGOT, 1, 4, 50, null),
            entry(Material.GOLD_NUGGET, 2, 8, 30, null),
            entry(Material.REDSTONE, 2, 8, 30, null),
            entry(Material.TORCH, 2, 8, 30, null),
            entry(Material.ARROW, 4, 12, 30, null),
            entry(Material.CHAIN, 1, 3, 20, "Rura metalowa"),
            entry(Material.BOW, 1, 1, 10, null),
            entry(Material.STONE_AXE, 1, 1, 12, null),
            entry(Material.IRON_PICKAXE, 1, 1, 8, null),
            entry(Material.LEATHER_CHESTPLATE, 1, 1, 10, null),
            entry(Material.IRON_SWORD, 1, 1, 6, null),
            entry(Material.CROSSBOW, 1, 1, 5, null)),

    FOOD_CRATE("Skrzynka z jedzeniem",
            entry(Material.BREAD, 1, 3, 60, null),
            entry(Material.APPLE, 1, 4, 50, null),
            entry(Material.BAKED_POTATO, 1, 3, 40, null),
            entry(Material.CARROT, 1, 4, 40, null),
            entry(Material.DRIED_KELP, 3, 8, 30, "Suszone wodorosty"),
            entry(Material.COOKIE, 2, 6, 30, null),
            entry(Material.MUSHROOM_STEW, 1, 1, 15, "Puszka gulaszu"),
            entry(Material.HONEY_BOTTLE, 1, 1, 10, null),
            entry(Material.GOLDEN_CARROT, 1, 2, 8, null)),

    MEDICAL_CRATE("Skrzynka medyczna",
            entry(Material.PAPER, 1, 4, 60, "Bandaż"),
            entry(Material.HONEY_BOTTLE, 1, 2, 40, "Syrop"),
            entry(Material.STRING, 1, 3, 30, "Szmata"),
            entry(Material.MILK_BUCKET, 1, 1, 25, null),
            entry(Material.GLISTERING_MELON_SLICE, 1, 2, 25, "Strzykawka medyczna"),
            entry(Material.SUSPICIOUS_STEW, 1, 1, 15, null),
            entry(Material.GOLDEN_APPLE, 1, 1, 12, "Apteczka")),

    TOOL_BOX("Skrzynka narzędziowa",
            entry(Material.IRON_NUGGET, 8, 24, 100, "Złom"),
            entry(Material.IRON_INGOT, 2, 5, 70, null),
            entry(Material.REDSTONE, 2, 6, 40, null),
            entry(Material.CHAIN, 2, 4, 30, "Rura metalowa"),
            entry(Material.IRON_PICKAXE, 1, 1, 25, null),
            entry(Material.IRON_AXE, 1, 1, 25, null),
            entry(Material.IRON_SHOVEL, 1, 1, 20, null),
            entry(Material.SHEARS, 1, 1, 20, null),
            entry(Material.FLINT_AND_STEEL, 1, 1, 20, null),
            entry(Material.COMPASS, 1, 1, 10, null),
            entry(Material.SPYGLASS, 1, 1, 10, null),
            entry(Material.CROSSBOW, 1, 1, 8, null));

    private record Entry(Material material, int min, int max, int chance, String name) {
    }

    private final String displayName;
    private final List<Entry> entries;

    LootType(String displayName, Entry... entries) {
        this.displayName = displayName;
        this.entries = List.of(entries);
    }

    private static Entry entry(Material material, int min, int max, int chance, String name) {
        return new Entry(material, min, max, chance, name);
    }

    public String displayName() {
        return displayName;
    }

    /** Wypełnia ekwipunek losowym lootem; pierwszy wpis tabeli ma zawsze 100% szans. */
    public void fill(Inventory inventory, Random random) {
        List<Integer> freeSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            freeSlots.add(i);
        }
        Collections.shuffle(freeSlots, random);
        int slot = 0;
        for (Entry entry : entries) {
            if (slot >= freeSlots.size()) {
                break;
            }
            if (random.nextInt(100) >= entry.chance()) {
                continue;
            }
            int amount = entry.min() + random.nextInt(entry.max() - entry.min() + 1);
            inventory.setItem(freeSlots.get(slot++), createItem(entry.material(), amount, entry.name()));
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack createItem(Material material, int amount, String name) {
        ItemStack item = new ItemStack(material, amount);
        if (name != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + name);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
