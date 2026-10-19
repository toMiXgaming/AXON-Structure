package pl.axon.structure.oasis;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Paser w kryjówkach czarnego rynku (struktura {@code czarny-rynek}): kilka drogich towarów losowanych z listy
 * {@code czarny-rynek.lista} co {@code odswiezanie-godzin}, opcjonalnie z limitem sztuk na okres (wspólnym dla
 * wszystkich kryjówek). Tylko kupno, za pieniądze z Vaulta. Losowanie jest zapisane w oaza.yml, więc restart
 * serwera go nie zmienia. Ekran GUI ({@link BlackMarketScreen}) kupuje tym samym {@link #buy}.
 */
final class BlackMarketService {

    record Offer(String spec, int amount, double price, int stock) {
    }

    private final OasisService oasis;
    private final Random random = new Random();
    private final List<Offer> pool = new ArrayList<>();
    private boolean enabled = true;
    private String title = "&8Paser";
    private int shown = 4;
    private long periodMillis = 3 * 3_600_000L;

    BlackMarketService(OasisService oasis) {
        this.oasis = oasis;
    }

    void configure(ConfigurationSection section, Logger logger) {
        pool.clear();
        if (section == null) {
            return;
        }
        enabled = section.getBoolean("wlaczony", true);
        title = section.getString("nazwa", "&8Paser");
        shown = Math.max(1, Math.min(9, section.getInt("ile", 4)));
        periodMillis = Math.max(1, section.getLong("odswiezanie-godzin", 3)) * 3_600_000L;
        for (Map<?, ?> map : section.getMapList("lista")) {
            String spec = String.valueOf(map.get("przedmiot"));
            int amount = map.get("ilosc") instanceof Number n ? n.intValue() : 1;
            double price = map.get("cena") instanceof Number n ? n.doubleValue() : 0;
            int stock = map.get("sztuk") instanceof Number n ? n.intValue() : 0;
            if (spec.isBlank() || amount <= 0 || price <= 0) {
                logger.warning("Czarny rynek: pomijam ofertę '" + spec + "' (zła ilość albo cena).");
                continue;
            }
            // przedmiot sprawdzamy dopiero przy otwarciu: bronie QualityArmory wczytują się po naszym pluginie
            pool.add(new Offer(spec, amount, price, Math.max(0, stock)));
        }
    }

    int size() {
        return pool.size();
    }

    String title() {
        return title;
    }

    Offer offer(int index) {
        return pool.get(index);
    }

    /** Ile milisekund do nowego towaru. */
    long untilRefresh() {
        return periodMillis - System.currentTimeMillis() % periodMillis;
    }

    /** Oferty w bieżącym okresie (indeksy listy), losowane na nowo po jego końcu. */
    private List<Integer> current() {
        long period = System.currentTimeMillis() / periodMillis;
        List<Integer> chosen = oasis.data().getIntegerList("czarny-rynek.aktywne");
        boolean valid = oasis.data().getLong("czarny-rynek.okres", -1) == period && !chosen.isEmpty();
        for (int index : chosen) {
            valid &= index >= 0 && index < pool.size();
        }
        if (!valid) {
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < pool.size(); i++) {
                all.add(i);
            }
            Collections.shuffle(all, random);
            chosen = new ArrayList<>(all.subList(0, Math.min(shown, all.size())));
            oasis.data().set("czarny-rynek.okres", period);
            oasis.data().set("czarny-rynek.aktywne", chosen);
            oasis.data().set("czarny-rynek.sprzedane", null);
            oasis.markDirty();
        }
        return chosen;
    }

    int sold(int index) {
        return oasis.data().getInt("czarny-rynek.sprzedane." + index, 0);
    }

    void open(Player player) {
        if (!enabled || pool.isEmpty()) {
            player.sendMessage("§8Paser: §7Nic dziś nie mam. Przyjdź później.");
            return;
        }
        if (!oasis.vault().available()) {
            player.sendMessage("§cPaser nie handluje: na serwerze nie ma Vaulta z pluginem ekonomii.");
            return;
        }
        List<Integer> offers = current();
        if (oasis.openScreen(player, new BlackMarketScreen(oasis, this, player, offers))) {
            player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.6f, 0.8f);
            return;
        }
        openChest(player, offers);
    }

    /** Towar pasera jako okno skrzyni (tryb awaryjny GUI). */
    void openChest(Player player, List<Integer> offers) {
        Menu menu = new Menu(1, title);
        draw(menu, offers);
        menu.onClick(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= offers.size()) {
                return;
            }
            buy(player, offers.get(slot));
            draw(menu, offers);
        });
        player.openInventory(menu.getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.6f, 0.8f);
    }

    private void draw(Menu menu, List<Integer> offers) {
        long left = periodMillis - System.currentTimeMillis() % periodMillis;
        String refresh = "&7Nowy towar za &f" + (left / 3_600_000L) + " h " + (left / 60_000L % 60) + " min";
        menu.getInventory().clear();
        for (int i = 0; i < offers.size(); i++) {
            Offer offer = pool.get(offers.get(i));
            ItemStack item = Items.create(offer.spec(), offer.amount());
            String stock = offer.stock() <= 0 ? "&7Na stanie: &fbez limitu"
                    : "&7Na stanie: &f" + Math.max(0, offer.stock() - sold(offers.get(i))) + " szt.";
            menu.getInventory().setItem(i, item == null
                    ? Menu.icon(Material.BARRIER, "&cTowar nie dojechał", "&7(" + offer.spec() + ")")
                    : Menu.withLore(item, "", "&eCena: &f" + oasis.vault().format(offer.price()), stock,
                    "&aKliknij, żeby kupić.", refresh));
        }
        for (int i = offers.size(); i < 9; i++) {
            menu.getInventory().setItem(i, Menu.filler());
        }
    }

    void buy(Player player, int index) {
        Offer offer = pool.get(index);
        if (offer.stock() > 0 && sold(index) >= offer.stock()) {
            player.sendMessage("§8Paser: §7Tego już nie mam. Wróć, jak przyjdzie nowy towar.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 0.8f);
            return;
        }
        ItemStack item = Items.create(offer.spec(), offer.amount());
        if (item == null) {
            player.sendMessage("§cTego towaru nie da się dziś kupić.");
            return;
        }
        if (!oasis.vault().withdraw(player, offer.price())) {
            player.sendMessage("§8Paser: §7Za mało kasy. To kosztuje §f" + oasis.vault().format(offer.price()) + "§7.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 0.8f);
            return;
        }
        Items.give(player, Items.stacks(item, offer.amount()));
        if (offer.stock() > 0) {
            oasis.data().set("czarny-rynek.sprzedane." + index, sold(index) + 1);
        }
        oasis.markDirty();
        player.sendMessage("§8Paser: §7Interes ubity. §f" + Items.name(item) + " §7jest twój.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.7f, 0.8f);
    }
}
