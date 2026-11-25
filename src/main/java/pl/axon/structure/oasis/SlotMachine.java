package pl.axon.structure.oasis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Automat w barze: płacisz stawkę, trzy bębny kręcą się w okienku. Trzy takie same symbole wypłacają stawkę razy
 * mnożnik symbolu, dwa takie same – razy {@code para-mnoznik}. Symbol z mnożnikiem 0 to pudło: nie płaci ani
 * w trójce, ani w parze. Wynik jest losowany od razu, więc zamknięcie okna
 * niczego nie zmienia. Grę pokazuje okno skrzyni albo ekran GUI ({@link SlotScreen}); losuje i płaci zawsze
 * {@link #spin}.
 */
final class SlotMachine {

    record Symbol(Material material, int weight, double multiplier) {
    }

    private static final int[] REELS = {11, 13, 15};

    private final OasisService oasis;
    private final Random random = new Random();
    private final List<Symbol> symbols = new ArrayList<>();
    /** Gry w toku (bębny się kręcą). */
    private final Map<UUID, Spin> spinning = new HashMap<>();
    /** Wygrane z gier, których bębny jeszcze się kręcą (wypłacane przy wyłączaniu pluginu, żeby stawka nie przepadła). */
    private final Map<UUID, Double> pending = new HashMap<>();
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
            if (material != null && material.isItem() && weight > 0 && multiplier >= 0) {
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

    /** Co widać w trakcie gry: okno skrzyni albo ekran GUI. Wynik i wypłata nie zależą od widoku. */
    interface SpinView {
        /** Co 2 ticki: symbole na bębnach i które już stanęły. */
        void frame(Symbol[] shown, boolean[] stopped);

        /** Bębny stanęły, wygrana (0 = nic) jest już wypłacona. */
        void done(Symbol[] result, double win);
    }

    /** Gra w toku: wynik wylosowany od razu, widok można podmienić (np. ekran na skrzynię). */
    static final class Spin {
        final Symbol[] result;
        final double win;
        SpinView view;

        Spin(Symbol[] result, double win, SpinView view) {
            this.result = result;
            this.win = win;
            this.view = view;
        }
    }

    double cost() {
        return cost;
    }

    double pairMultiplier() {
        return pairMultiplier;
    }

    List<Symbol> symbols() {
        return symbols;
    }

    /** Gra gracza, której bębny jeszcze się kręcą, albo null. */
    Spin active(Player player) {
        return spinning.get(player.getUniqueId());
    }

    /** Klik w automat: ekran GUI (gra rusza od razu), a gdy się nie da, okno skrzyni jak dotąd. */
    void play(Player player) {
        if (symbols.isEmpty()) {
            player.sendMessage("§cAutomat jest zepsuty.");
            return;
        }
        if (spinning.containsKey(player.getUniqueId())) {
            return;
        }
        SlotScreen screen = new SlotScreen(oasis, this, player);
        if (oasis.openScreen(player, screen)) {
            screen.spin();
            return;
        }
        Spin spin = spin(player, null);
        if (spin != null) {
            spin.view = chestView(player);
        }
    }

    /**
     * Pobiera stawkę, losuje wynik i kręci bębnami (wypłata na końcu, niezależnie od tego, czy okno jest otwarte).
     * Null, gdy gra nie ruszyła (gracz dostał komunikat albo już gra).
     */
    Spin spin(Player player, SpinView view) {
        UUID id = player.getUniqueId();
        if (symbols.isEmpty() || spinning.containsKey(id)) {
            return null;
        }
        if (!oasis.vault().available() || !oasis.vault().withdraw(player, cost)) {
            player.sendMessage("§cGra kosztuje " + oasis.vault().format(cost) + ".");
            return null;
        }
        Symbol[] result = {roll(), roll(), roll()};
        double win = payout(result);
        Spin spin = new Spin(result, win, view);
        spinning.put(id, spin);
        pending.put(id, win);
        final int[] tick = {0};
        Bukkit.getScheduler().runTaskTimer(oasis.plugin(), task -> {
            tick[0]++;
            Symbol[] shown = new Symbol[REELS.length];
            boolean[] stopped = new boolean[REELS.length];
            for (int r = 0; r < REELS.length; r++) {
                stopped[r] = tick[0] >= 8 + r * 5;
                shown[r] = stopped[r] ? result[r] : roll();
            }
            show(spin, v -> v.frame(shown, stopped));
            if (tick[0] < 8 + 2 * 5) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.8f);
                return;
            }
            task.cancel();
            spinning.remove(id);
            pending.remove(id);
            if (win > 0) {
                oasis.vault().deposit(player, win);
                player.sendMessage("§a§lWygrana! §f" + oasis.vault().format(win));
                player.playSound(player.getLocation(), win >= cost * 5 ? Sound.UI_TOAST_CHALLENGE_COMPLETE
                        : Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            } else {
                player.sendMessage("§7Nic tym razem.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
            }
            show(spin, v -> v.done(result, win));
        }, 2L, 2L);
        return spin;
    }

    /** Błąd w widoku nie może zatrzymać gry ani wypłaty. */
    private void show(Spin spin, java.util.function.Consumer<SpinView> action) {
        if (spin.view == null) {
            return;
        }
        try {
            action.accept(spin.view);
        } catch (RuntimeException e) {
            oasis.plugin().getLogger().warning("Automat: błąd widoku gry: " + e);
            spin.view = null;
        }
    }

    /**
     * Okno skrzyni automatu (tryb awaryjny GUI): z grą w toku przejmuje jej widok, bez gry pokazuje ostatni wynik
     * ({@code last} może być null).
     */
    void openChest(Player player, Symbol[] last, double lastWin) {
        SpinView view = chestView(player);
        Spin spin = active(player);
        if (spin != null) {
            spin.view = view;
        } else if (last != null) {
            view.frame(last, new boolean[]{true, true, true});
            view.done(last, lastWin);
        }
    }

    private SpinView chestView(Player player) {
        Menu menu = new Menu(3, "&5Automat &8– stawka " + oasis.vault().format(cost));
        Inventory inv = menu.getInventory();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, Menu.icon(Material.PURPLE_STAINED_GLASS_PANE, " "));
        }
        player.openInventory(inv);
        return new SpinView() {
            @Override
            public void frame(Symbol[] shown, boolean[] stopped) {
                for (int r = 0; r < REELS.length; r++) {
                    Material material = shown[r].material();
                    inv.setItem(REELS[r], Menu.icon(material, stopped[r]
                            ? "&f" + Items.name(new org.bukkit.inventory.ItemStack(material)) : "&7…"));
                }
            }

            @Override
            public void done(Symbol[] result, double win) {
                if (win > 0) {
                    inv.setItem(22, Menu.icon(Material.GOLD_INGOT, "&a&lWygrana: " + oasis.vault().format(win)));
                } else {
                    inv.setItem(22, Menu.icon(Material.BARRIER, "&7Nic tym razem", "&7Kliknij automat jeszcze raz."));
                }
            }
        };
    }

    /** Przy wyłączaniu: gry w trakcie kończą się od razu wylosowanym wynikiem. */
    void settleAll() {
        for (Map.Entry<UUID, Double> e : pending.entrySet()) {
            if (e.getValue() > 0) {
                oasis.vault().deposit(Bukkit.getOfflinePlayer(e.getKey()), e.getValue());
                Player player = Bukkit.getPlayer(e.getKey());
                if (player != null) {
                    player.sendMessage("§a§lWygrana! §f" + oasis.vault().format(e.getValue()));
                }
            }
        }
        pending.clear();
        spinning.clear();
    }

    private double payout(Symbol[] r) {
        if (r[0] == r[1] && r[1] == r[2]) {
            return cost * r[0].multiplier();
        }
        // para pustych symboli (mnożnik 0) nic nie płaci
        Symbol pair = r[0] == r[1] || r[0] == r[2] ? r[0] : r[1] == r[2] ? r[1] : null;
        if (pair != null && pair.multiplier() > 0) {
            return cost * pairMultiplier;
        }
        return 0;
    }
}
