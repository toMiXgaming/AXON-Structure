package pl.axon.structure.oasis;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

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
     * Czy przedmiot z ekwipunku to ten sam towar: ten sam typ i ta sama nazwa (bronie QualityArmory różnią się nazwą,
     * a ich amunicja i zużycie siedzą w danych, więc nie porównujemy całych danych).
     */
    @SuppressWarnings("deprecation")
    static boolean same(ItemStack have, ItemStack wanted) {
        if (have == null || wanted == null || have.getType() != wanted.getType()) {
            return false;
        }
        ItemMeta a = have.getItemMeta();
        ItemMeta b = wanted.getItemMeta();
        String nameA = a != null && a.hasDisplayName() ? a.getDisplayName() : "";
        String nameB = b != null && b.hasDisplayName() ? b.getDisplayName() : "";
        return nameA.equals(nameB);
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
