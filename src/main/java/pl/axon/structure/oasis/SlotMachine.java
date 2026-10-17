package pl.axon.structure.oasis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Automat w barze: płacisz stawkę, trzy bębny kręcą się w okienku. Trzy takie same symbole wypłacają stawkę razy
 * mnożnik symbolu, dwa takie same – razy {@code para-mnoznik}. Wynik jest losowany od razu, więc zamknięcie okna
 * niczego nie zmienia.
 */
final class SlotMachine {

    record Symbol(Material material, int weight, double multiplier) {
    }

    private static final int[] REELS = {11, 13, 15};

    private final OasisService oasis;
    private final Random random = new Random();
    private final List<Symbol> symbols = new ArrayList<>();
    private final Set<UUID> spinning = new HashSet<>();
    private double cost = 50;
    private double pairMultiplier = 1;
    private int totalWeight;

    SlotMachine(OasisService oasis) {
        this.oasis = oasis;
    }

    void configure(ConfigurationSection section) {
        symbols.clear();
        totalWeight = 0;
        if (section == null) {
            return;
        }
        cost = Math.max(1, section.getDouble("stawka", 50));
        pairMultiplier = Math.max(0, section.getDouble("para-mnoznik", 1));
        for (Map<?, ?> map : section.getMapList("symbole")) {
            Material material = Material.matchMaterial(String.valueOf(map.get("przedmiot")));
            int weight = map.get("waga") instanceof Number n ? n.intValue() : 1;
            double multiplier = map.get("mnoznik") instanceof Number n ? n.doubleValue() : 2;
            if (material != null && material.isItem() && weight > 0) {
                symbols.add(new Symbol(material, weight, multiplier));
                totalWeight += weight;
            }
        }
    }

    private Symbol roll() {
        int r = random.nextInt(totalWeight);
        for (Symbol s : symbols) {
            r -= s.weight();
            if (r < 0) {
                return s;
            }
        }
        return symbols.get(0);
    }

    void play(Player player) {
        if (symbols.isEmpty()) {
            player.sendMessage("§cAutomat jest zepsuty.");
            return;
        }
        if (!spinning.add(player.getUniqueId())) {
            return;
        }
        if (!oasis.vault().available() || !oasis.vault().withdraw(player, cost)) {
            spinning.remove(player.getUniqueId());
            player.sendMessage("§cGra kosztuje " + oasis.vault().format(cost) + ".");
            return;
        }
        Symbol[] result = {roll(), roll(), roll()};
        double win = payout(result);
        Menu menu = new Menu(3, "&5Automat &8– stawka " + oasis.vault().format(cost));
        Inventory inv = menu.getInventory();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, Menu.icon(Material.PURPLE_STAINED_GLASS_PANE, " "));
        }
        player.openInventory(inv);
        final int[] tick = {0};
        Bukkit.getScheduler().runTaskTimer(oasis.plugin(), task -> {
            tick[0]++;
            for (int r = 0; r < REELS.length; r++) {
                boolean stopped = tick[0] >= 8 + r * 5;
                Material shown = stopped ? result[r].material() : roll().material();
                inv.setItem(REELS[r], Menu.icon(shown, stopped ? "&f" + Items.name(new org.bukkit.inventory.ItemStack(shown)) : "&7…"));
            }
            if (tick[0] < 8 + 2 * 5) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.8f);
                return;
            }
            task.cancel();
            spinning.remove(player.getUniqueId());
            if (win > 0) {
                oasis.vault().deposit(player, win);
                player.sendMessage("§a§lWygrana! §f" + oasis.vault().format(win));
                player.playSound(player.getLocation(), win >= cost * 5 ? Sound.UI_TOAST_CHALLENGE_COMPLETE
                        : Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                inv.setItem(22, Menu.icon(Material.GOLD_INGOT, "&a&lWygrana: " + oasis.vault().format(win)));
            } else {
                player.sendMessage("§7Nic tym razem.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
                inv.setItem(22, Menu.icon(Material.BARRIER, "&7Nic tym razem", "&7Kliknij automat jeszcze raz."));
            }
        }, 2L, 2L);
    }

    private double payout(Symbol[] r) {
        if (r[0] == r[1] && r[1] == r[2]) {
            return cost * r[0].multiplier();
        }
        if (r[0] == r[1] || r[1] == r[2] || r[0] == r[2]) {
            return cost * pairMultiplier;
        }
        return 0;
    }
}
