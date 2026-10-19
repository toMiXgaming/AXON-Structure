package pl.axon.structure.oasis;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Przedmioty z configu ({@code bread}, {@code qa:ak47}) i liczenie ich w ekwipunku gracza. */
final class Items {

    private Items() {
    }

    /** Przedmiot z opisu: nazwa z Minecrafta albo {@code qa:<nazwa>} z QualityArmory; null, gdy nie istnieje. */
    static ItemStack create(String spec, int amount) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        ItemStack item = null;
        if (spec.toLowerCase(Locale.ROOT).startsWith("qa:")) {
            ItemStack stack = pl.axon.structure.util.QaItems.find(spec.substring(3));
            if (stack == null) {
                return null;
            }
            item = stack;
        } else {
            Material material = Material.matchMaterial(spec);
            if (material != null && material.isItem() && material != Material.AIR) {
                item = new ItemStack(material);
            }
        }
        if (item != null) {
            item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        }
        return item;
    }

    /**
     * Czy przedmiot z ekwipunku to ten sam towar. Zwykłe przedmioty muszą być identyczne (bez zaklęć, uszkodzeń,
     * nazw i zawartości), żeby nie dało się sprzedać zaklętego miecza albo pełnej skrzynki jako zwykłego towaru.
     * Bronie QualityArmory różnią się nazwą, a ich amunicja i zużycie siedzą w danych, więc dla nich liczy się typ,
     * nazwa, model i zaklęcia.
     */
    @SuppressWarnings("deprecation")
    static boolean same(ItemStack have, ItemStack wanted) {
        if (have == null || wanted == null || have.getType() != wanted.getType()) {
            return false;
        }
        ItemMeta b = wanted.getItemMeta();
        if (b == null || !b.hasDisplayName()) {
            return have.isSimilar(wanted);
        }
        ItemMeta a = have.getItemMeta();
        if (a == null || !a.hasDisplayName() || !a.getDisplayName().equals(b.getDisplayName())) {
            return false;
        }
        if (a.hasCustomModelData() != b.hasCustomModelData()
                || (a.hasCustomModelData() && a.getCustomModelData() != b.getCustomModelData())) {
            return false;
        }
        return a.getEnchants().equals(b.getEnchants());
    }

    static int count(Player player, ItemStack wanted) {
        int n = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (same(item, wanted)) {
                n += item.getAmount();
            }
        }
        return n;
    }

    /** Zabiera {@code amount} sztuk towaru (wywołuj po sprawdzeniu {@link #count}). */
    static void take(Player player, ItemStack wanted, int amount) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack item = contents[i];
            if (!same(item, wanted)) {
                continue;
            }
            int used = Math.min(amount, item.getAmount());
            amount -= used;
            if (used == item.getAmount()) {
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - used);
            }
        }
        inv.setStorageContents(contents);
    }

    /** Towar w ilości {@code total} rozłożony na pełne stosy (np. 128 strzał to dwa stosy po 64). */
    static ItemStack[] stacks(ItemStack proto, int total) {
        int max = Math.max(1, proto.getMaxStackSize());
        List<ItemStack> out = new ArrayList<>();
        for (int left = Math.max(1, total); left > 0; left -= max) {
            ItemStack item = proto.clone();
            item.setAmount(Math.min(max, left));
            out.add(item);
        }
        return out.toArray(new ItemStack[0]);
    }

    /** Daje przedmioty, a to, co się nie zmieści, wyrzuca pod nogi. */
    static void give(Player player, ItemStack... items) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<Integer, ItemStack> left = player.getInventory().addItem(item);
            for (ItemStack rest : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
        }
    }

    /** Ładna nazwa towaru do opisów. */
    @SuppressWarnings("deprecation")
    static String name(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        String raw = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
