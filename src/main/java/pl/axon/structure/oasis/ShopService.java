package pl.axon.structure.oasis;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Sklep w oazie: towary z configu ({@code oaza.sklep.oferty}) za pieniądze z Vaulta. Lewy klik kupuje,
 * prawy sprzedaje (skup), shift mnoży razy 4.
 */
final class ShopService {

    record Offer(String spec, ItemStack item, int amount, double buy, double sell) {
    }

    private final OasisService oasis;
    private String title = "&6Sklep";
    private final List<Offer> offers = new ArrayList<>();

    ShopService(OasisService oasis) {
        this.oasis = oasis;
    }

    void configure(ConfigurationSection section, Logger logger) {
        offers.clear();
        if (section == null) {
            return;
        }
        title = section.getString("nazwa", "&6Sklep");
        for (Map<?, ?> map : section.getMapList("oferty")) {
            String spec = String.valueOf(map.get("przedmiot"));
            int amount = number(map.get("ilosc"), 1).intValue();
            ItemStack item = Items.create(spec, amount);
            if (item == null) {
                logger.warning("Sklep w oazie: nie ma przedmiotu '" + spec + "' (QualityArmory wczytuje bronie później – "
                        + "po starcie wpisz /struktury przeladuj, jeśli to broń).");
                continue;
            }
            offers.add(new Offer(spec, item, item.getAmount(), number(map.get("kupno"), 0).doubleValue(),
                    number(map.get("skup"), 0).doubleValue()));
            if (offers.size() >= 45) {
                break;
            }
        }
    }

    private static Number number(Object value, Number fallback) {
        if (value instanceof Number n) {
            return n;
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    int size() {
        return offers.size();
    }

    void open(Player player) {
        if (!oasis.vault().available()) {
            player.sendMessage("§cSklep jest zamknięty: na serwerze nie ma Vaulta z pluginem ekonomii.");
            return;
        }
        if (offers.isEmpty()) {
            player.sendMessage("§cHandlarz nie ma dziś nic na sprzedaż.");
            return;
        }
        int rows = Math.min(6, (offers.size() + 8) / 9 + 1);
        Menu menu = new Menu(rows, title);
        draw(menu, player);
        menu.onClick(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= offers.size()) {
                return;
            }
            Offer offer = offers.get(slot);
            int times = event.isShiftClick() ? 4 : 1;
            if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
                buy(player, offer, times);
            } else if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                sell(player, offer, times);
            }
            draw(menu, player);
        });
        player.openInventory(menu.getInventory());
    }

    private void draw(Menu menu, Player player) {
        String balance = oasis.vault().format(oasis.vault().balance(player));
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(o.buy() > 0 ? "&aLPM: kup za &f" + oasis.vault().format(o.buy()) : "&8Nie na sprzedaż");
            lore.add(o.sell() > 0 ? "&ePPM: sprzedaj za &f" + oasis.vault().format(o.sell()) : "&8Handlarz tego nie skupuje");
            lore.add("&7Shift: razy 4");
            lore.add("&7Twoje saldo: &f" + balance);
            menu.getInventory().setItem(i, Menu.withLore(o.item(), lore.toArray(new String[0])));
        }
    }

    private void buy(Player player, Offer offer, int times) {
        if (offer.buy() <= 0) {
            return;
        }
        double price = offer.buy() * times;
        if (!oasis.vault().withdraw(player, price)) {
            player.sendMessage("§cMasz za mało pieniędzy (" + oasis.vault().format(price) + ").");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            return;
        }
        for (int i = 0; i < times; i++) {
            Items.give(player, offer.item().clone());
        }
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.7f, 1f);
    }

    private void sell(Player player, Offer offer, int times) {
        if (offer.sell() <= 0) {
            return;
        }
        int have = Items.count(player, offer.item()) / offer.amount();
        int n = Math.min(times, have);
        if (n <= 0) {
            player.sendMessage("§cNie masz " + offer.amount() + "× " + Items.name(offer.item()) + "§c.");
            return;
        }
        Items.take(player, offer.item(), n * offer.amount());
        oasis.vault().deposit(player, offer.sell() * n);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }
}
