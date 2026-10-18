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

    /** Oferta z configu; {@code item} to null, dopóki przedmiotu nie da się zbudować (bronie QualityArmory). */
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
            int amount = Math.max(1, number(map.get("ilosc"), 1).intValue());
            // bronie QualityArmory wczytują się po naszym pluginie: brakujące przedmioty szukamy znów przy otwarciu
            ItemStack item = Items.create(spec, amount);
            if (item == null && !spec.toLowerCase(java.util.Locale.ROOT).startsWith("qa:")) {
                logger.warning("Sklep w oazie: nie ma przedmiotu '" + spec + "'.");
                continue;
            }
            offers.add(new Offer(spec, item, amount, number(map.get("kupno"), 0).doubleValue(),
                    number(map.get("skup"), 0).doubleValue()));
            if (offers.size() >= 45) {
                break;
            }
        }
    }

    /** Uzupełnia oferty, których przedmiot nie istniał przy wczytaniu configu; zwraca te, które da się pokazać. */
    private List<Offer> available() {
        List<Offer> out = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            if (o.item() == null) {
                ItemStack item = Items.create(o.spec(), o.amount());
                if (item == null) {
                    continue;
                }
                o = new Offer(o.spec(), item, o.amount(), o.buy(), o.sell());
                offers.set(i, o);
            }
            out.add(o);
        }
        return out;
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
        List<Offer> offers = available();
        if (offers.isEmpty()) {
            player.sendMessage("§cHandlarz nie ma dziś nic na sprzedaż.");
            return;
        }
        int rows = Math.min(6, (offers.size() + 8) / 9 + 1);
        Menu menu = new Menu(rows, title);
        draw(menu, player, offers);
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
            draw(menu, player, offers);
        });
        player.openInventory(menu.getInventory());
    }

    private void draw(Menu menu, Player player, List<Offer> offers) {
        String balance = oasis.vault().format(oasis.vault().balance(player));
        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (o.amount() > o.item().getAmount()) {
                lore.add("&7Ilość: &f" + o.amount() + " szt.");
            }
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
        Items.give(player, Items.stacks(offer.item(), offer.amount() * times));
        player.sendMessage("§aKupiono §f" + offer.amount() * times + "× " + Items.name(offer.item())
                + " §aza §f" + oasis.vault().format(price) + "§a.");
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
        player.sendMessage("§eSprzedano §f" + n * offer.amount() + "× " + Items.name(offer.item())
                + " §eza §f" + oasis.vault().format(offer.sell() * n) + "§e.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }
}
