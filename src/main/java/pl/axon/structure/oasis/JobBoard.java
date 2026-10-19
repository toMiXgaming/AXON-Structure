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
 * Tablica zleceń w oazie: kilka zleceń „przynieś N sztuk X” losowanych z configu co {@code odswiezanie-godzin}.
 * Każdy gracz może wykonać każde zlecenie raz w danym okresie i dostaje nagrodę z Vaulta.
 */
final class JobBoard {

    record Job(String spec, ItemStack item, int amount, double reward) {
    }

    private final OasisService oasis;
    private final Random random = new Random();
    private final List<Job> pool = new ArrayList<>();
    private int active = 5;
    private long periodMillis = 24 * 3_600_000L;

    JobBoard(OasisService oasis) {
        this.oasis = oasis;
    }

    void configure(ConfigurationSection section, Logger logger) {
        pool.clear();
        if (section == null) {
            return;
        }
        active = Math.max(1, Math.min(27, section.getInt("ile", 5)));
        periodMillis = Math.max(1, section.getLong("odswiezanie-godzin", 24)) * 3_600_000L;
        for (Map<?, ?> map : section.getMapList("lista")) {
            String spec = String.valueOf(map.get("przedmiot"));
            int amount = map.get("ilosc") instanceof Number n ? n.intValue() : 1;
            double reward = map.get("nagroda") instanceof Number n ? n.doubleValue() : 0;
            ItemStack item = Items.create(spec, 1);
            if (item == null || amount <= 0 || reward <= 0) {
                logger.warning("Tablica zleceń: pomijam zlecenie '" + spec + "' (zły przedmiot, ilość albo nagroda).");
                continue;
            }
            pool.add(new Job(spec, item, amount, reward));
        }
    }

    /** Aktualne zlecenia: losowane na nowo, gdy minie okres (zapis w oaza.yml, żeby restart ich nie zmieniał). */
    private List<Integer> current() {
        long period = System.currentTimeMillis() / periodMillis;
        List<Integer> chosen = oasis.data().getIntegerList("zlecenia.aktywne");
        boolean valid = oasis.data().getLong("zlecenia.okres", -1) == period && !chosen.isEmpty();
        for (int index : chosen) {
            valid &= index >= 0 && index < pool.size();
        }
        if (!valid) {
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < pool.size(); i++) {
                all.add(i);
            }
            Collections.shuffle(all, random);
            chosen = new ArrayList<>(all.subList(0, Math.min(active, all.size())));
            oasis.data().set("zlecenia.okres", period);
            oasis.data().set("zlecenia.aktywne", chosen);
            oasis.data().set("zlecenia.wykonane", null);
            oasis.markDirty();
        }
        return chosen;
    }

    private String doneKey(Player player) {
        return "zlecenia.wykonane." + player.getUniqueId();
    }

    void open(Player player) {
        if (pool.isEmpty()) {
            player.sendMessage("§cNa tablicy nie ma dziś żadnych zleceń.");
            return;
        }
        if (!oasis.vault().available()) {
            player.sendMessage("§cZlecenia są wstrzymane: na serwerze nie ma Vaulta z pluginem ekonomii.");
            return;
        }
        List<Integer> jobs = current();
        Menu menu = new Menu((jobs.size() + 8) / 9, "&8Tablica zleceń");
        draw(menu, player, jobs);
        menu.onClick(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= jobs.size()) {
                return;
            }
            complete(player, jobs.get(slot));
            draw(menu, player, jobs);
        });
        player.openInventory(menu.getInventory());
    }

    private void draw(Menu menu, Player player, List<Integer> jobs) {
        List<Integer> done = oasis.data().getIntegerList(doneKey(player));
        long left = periodMillis - System.currentTimeMillis() % periodMillis;
        String refresh = "&7Nowe zlecenia za &f" + (left / 3_600_000L) + " h " + (left / 60_000L % 60) + " min";
        for (int i = 0; i < jobs.size(); i++) {
            Job job = pool.get(jobs.get(i));
            boolean finished = done.contains(jobs.get(i));
            ItemStack icon = finished
                    ? Menu.icon(Material.LIME_STAINED_GLASS_PANE, "&aWykonane", "&7" + job.amount() + "× "
                    + Items.name(job.item()), refresh)
                    : Menu.withLore(job.item(), "", "&ePrzynieś: &f" + job.amount() + " szt.",
                    "&eNagroda: &f" + oasis.vault().format(job.reward()),
                    "&7Masz: &f" + Items.count(player, job.item()), "&aKliknij, żeby oddać.", refresh);
            menu.getInventory().setItem(i, icon);
        }
    }

    private void complete(Player player, int index) {
        List<Integer> done = new ArrayList<>(oasis.data().getIntegerList(doneKey(player)));
        if (done.contains(index)) {
            player.sendMessage("§7To zlecenie już wykonałeś. Nowe pojawią się po odświeżeniu tablicy.");
            return;
        }
        Job job = pool.get(index);
        if (Items.count(player, job.item()) < job.amount()) {
            player.sendMessage("§cPotrzebujesz " + job.amount() + "× " + Items.name(job.item()) + "§c.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            return;
        }
        Items.take(player, job.item(), job.amount());
        oasis.vault().deposit(player, job.reward());
        done.add(index);
        oasis.data().set(doneKey(player), done);
        oasis.markDirty();
        player.sendMessage("§aZlecenie wykonane! §7Nagroda: §f" + oasis.vault().format(job.reward()));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }
}
