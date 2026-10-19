package pl.axon.structure.oasis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.axon.structure.loot.LootConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Okno (GUI) oazy: sklep, wymiana, depozyt, zlecenia, automat. Domyślnie każde kliknięcie jest blokowane;
 * okna, w których gracz przekłada przedmioty, ustawiają własną obsługę.
 */
public final class Menu implements InventoryHolder {

    private final Inventory inventory;
    private Consumer<InventoryClickEvent> onClick = e -> e.setCancelled(true);
    private Consumer<InventoryDragEvent> onDrag = e -> e.setCancelled(true);
    private Consumer<InventoryCloseEvent> onClose = e -> {
    };

    @SuppressWarnings("deprecation")
    public Menu(int rows, String title) {
        inventory = Bukkit.createInventory(this, rows * 9, LootConfig.color(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Menu onClick(Consumer<InventoryClickEvent> handler) {
        this.onClick = handler;
        return this;
    }

    public Menu onDrag(Consumer<InventoryDragEvent> handler) {
        this.onDrag = handler;
        return this;
    }

    public Menu onClose(Consumer<InventoryCloseEvent> handler) {
        this.onClose = handler;
        return this;
    }

    void click(InventoryClickEvent event) {
        onClick.accept(event);
    }

    void drag(InventoryDragEvent event) {
        onDrag.accept(event);
    }

    void close(InventoryCloseEvent event) {
        onClose.accept(event);
    }

    /** Przedmiot z nazwą i opisem (kolory {@code &}). */
    @SuppressWarnings("deprecation")
    public static ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LootConfig.color(name));
            List<String> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(LootConfig.color(line));
            }
            meta.setLore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Dopisuje linie opisu do kopii przedmiotu (np. cena pod towarem w sklepie). */
    @SuppressWarnings("deprecation")
    public static ItemStack withLore(ItemStack base, String... lore) {
        ItemStack item = base.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lines = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            for (String line : lore) {
                lines.add(LootConfig.color(line));
            }
            meta.setLore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filler() {
        return icon(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
}
