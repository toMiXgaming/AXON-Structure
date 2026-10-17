package pl.axon.structure.oasis;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Depozyt w oazie: prywatny schowek każdego gracza, zapisywany w {@code oaza.yml}. Otwierany przy ladzie depozytu,
 * więc łup z wypraw można bezpiecznie zostawić, zanim wyjdzie się znów między strzelających.
 */
final class StashService {

    private final OasisService oasis;
    private int rows = 3;
    private double fee;
    private final Map<UUID, Menu> open = new HashMap<>();

    StashService(OasisService oasis) {
        this.oasis = oasis;
    }

    void configure(ConfigurationSection section) {
        rows = section == null ? 3 : Math.max(1, Math.min(6, section.getInt("rzedy", 3)));
        fee = section == null ? 0 : Math.max(0, section.getDouble("oplata", 0));
    }

    void open(Player player) {
        UUID id = player.getUniqueId();
        Menu menu = open.get(id);
        if (menu == null) {
            if (fee > 0) {
                if (!oasis.vault().available() || !oasis.vault().withdraw(player, fee)) {
                    player.sendMessage("§cDepozyt kosztuje " + oasis.vault().format(fee) + " za otwarcie.");
                    return;
                }
                player.sendMessage("§7Opłata za depozyt: §f" + oasis.vault().format(fee));
            }
            menu = new Menu(rows, "&8Depozyt: " + player.getName());
            ConfigurationSection saved = oasis.data().getConfigurationSection("depozyt." + id);
            if (saved != null) {
                for (String key : saved.getKeys(false)) {
                    ItemStack item = saved.getItemStack(key);
                    int slot = parse(key);
                    if (item != null && slot >= 0 && slot < rows * 9) {
                        menu.getInventory().setItem(slot, item);
                    } else if (item != null) {
                        Items.give(player, item); // depozyt zmniejszony w configu: nadmiar wraca do gracza
                    }
                }
            }
            Menu m = menu;
            // zwykłe przekładanie przedmiotów, jak w skrzyni
            menu.onClick(e -> {
            }).onDrag(e -> {
            }).onClose(e -> {
                if (m.getInventory().getViewers().size() <= 1) {
                    save(id, m);
                    open.remove(id);
                }
            });
            open.put(id, menu);
        }
        player.openInventory(menu.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 0.5f, 1.3f);
    }

    private void save(UUID id, Menu menu) {
        ItemStack[] contents = menu.getInventory().getContents();
        oasis.data().set("depozyt." + id, null);
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null && !contents[slot].getType().isAir()) {
                oasis.data().set("depozyt." + id + "." + slot, contents[slot]);
            }
        }
        oasis.markDirty();
    }

    private static int parse(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Zapisuje otwarte schowki (przy wyłączaniu). */
    void saveAll() {
        for (Map.Entry<UUID, Menu> e : open.entrySet()) {
            save(e.getKey(), e.getValue());
        }
    }
}
