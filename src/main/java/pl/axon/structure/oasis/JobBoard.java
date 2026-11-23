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
 * Każdy gracz może wykonać każde zlecenie raz w danym okresie i dostaje nagrodę z Vaulta, przedmiot
 * ({@code nagroda-przedmiot}, np. HQM) albo jedno i drugie. Zlecenie z {@code od-dnia} pojawia się na tablicy dopiero
 * od tego dnia wipe'u (zegar z AxonItems). Ekran GUI
 * ({@link JobScreen}) oddaje towar tym samym {@link #complete}.
 */
final class JobBoard {

    record Job(String spec, ItemStack item, int amount, double reward, String rewardSpec, ItemStack rewardItem,
               int rewardAmount, int fromDay) {

        boolean hasItemReward() {
            return rewardItem != null && rewardAmount > 0;
        }
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
            Object rawRewardSpec = map.get("nagroda-przedmiot");
            String rewardSpec = rawRewardSpec == null ? null : String.valueOf(rawRewardSpec);
            int rewardAmount = map.get("nagroda-ilosc") instanceof Number n ? n.intValue() : 1;
            int fromDay = map.get("od-dnia") instanceof Number n ? n.intValue() : 0;
            // przedmioty AxonItems i QualityArmory wczytują się po naszym pluginie: brakujące szukamy znów przy otwarciu
            ItemStack item = Items.create(spec, 1);
            ItemStack rewardItem = rewardSpec == null ? null : Items.create(rewardSpec, 1);
            if (rewardSpec != null && ((rewardItem == null && !late(rewardSpec)) || rewardAmount <= 0)) {
                logger.warning("Tablica zleceń: pomijam zlecenie '" + spec + "' (zła nagroda-przedmiot '" + rewardSpec
                        + "' albo nagroda-ilosc).");
                continue;
            }
            if ((item == null && !late(spec)) || amount <= 0 || (reward <= 0 && rewardSpec == null)) {
                logger.warning("Tablica zleceń: pomijam zlecenie '" + spec + "' (zły przedmiot, ilość albo nagroda).");
                continue;
            }
            pool.add(new Job(spec, item, amount, Math.max(0, reward), rewardSpec, rewardItem, rewardAmount,
                    Math.max(0, fromDay)));
        }
    }

    Job job(int index) {
        Job job = resolve(index);
        return job == null ? pool.get(index) : job;
    }

    /** Przedmiot z pluginu, który wczytuje się po nas (AxonItems, QualityArmory). */
    private static boolean late(String spec) {
        String lower = spec.toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("axon:") || lower.startsWith("qa:");
    }

    /** Uzupełnia brakujące przedmioty zlecenia; null, gdy dalej ich nie ma. */
    private Job resolve(int index) {
        Job job = pool.get(index);
        if (job.item() != null && (job.rewardSpec() == null || job.rewardItem() != null)) {
            return job;
        }
        ItemStack item = job.item() != null ? job.item() : Items.create(job.spec(), 1);
        ItemStack prize = job.rewardSpec() == null || job.rewardItem() != null ? job.rewardItem()
                : Items.create(job.rewardSpec(), 1);
        if (item == null || (job.rewardSpec() != null && prize == null)) {
            return null;
        }
        job = new Job(job.spec(), item, job.amount(), job.reward(), job.rewardSpec(), prize, job.rewardAmount(),
                job.fromDay());
        pool.set(index, job);
        return job;
    }

    /** Nagroda do opisu: kwota, przedmiot albo oba („300 zł + 1x HQM”). */
    String rewardText(Job job) {
        String money = job.reward() > 0 ? oasis.vault().format(job.reward()) : null;
        String item = job.hasItemReward() ? job.rewardAmount() + "x " + Items.name(job.rewardItem()) : null;
        if (money != null && item != null) {
            return money + " + " + item;
        }
        return money != null ? money : item == null ? "-" : item;
    }

    /** Czy zlecenie może już być na tablicy (dzień wipe'u z AxonItems). */
    static boolean unlocked(int fromDay, int wipeDay) {
        return fromDay <= 0 || wipeDay >= fromDay;
    }

    /** Ile milisekund do nowych zleceń. */
    long untilRefresh() {
        return periodMillis - System.currentTimeMillis() % periodMillis;
    }

    /** Czy gracz wykonał już to zlecenie w tym okresie. */
    boolean done(Player player, int index) {
        return oasis.data().getIntegerList(doneKey(player)).contains(index);
    }

    /** Aktualne zlecenia: losowane na nowo, gdy minie okres (zapis w oaza.yml, żeby restart ich nie zmieniał). */
    private List<Integer> current() {
        long period = System.currentTimeMillis() / periodMillis;
        int day = pl.axon.structure.loot.AxonItemsBridge.wipeDay();
        List<Integer> chosen = oasis.data().getIntegerList("zlecenia.aktywne");
        boolean valid = oasis.data().getLong("zlecenia.okres", -1) == period && !chosen.isEmpty();
        for (int index : chosen) {
            valid &= index >= 0 && index < pool.size() && unlocked(pool.get(index).fromDay(), day)
                    && resolve(index) != null;
        }
        if (!valid) {
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < pool.size(); i++) {
                if (unlocked(pool.get(i).fromDay(), day) && resolve(i) != null) {
                    all.add(i);
                }
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
        if (oasis.openScreen(player, new JobScreen(oasis, this, player, jobs))) {
            return;
        }
        openChest(player, jobs);
    }

    /** Tablica jako okno skrzyni (tryb awaryjny GUI). */
    void openChest(Player player, List<Integer> jobs) {
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
            Job job = job(jobs.get(i));
            boolean finished = done.contains(jobs.get(i));
            ItemStack icon = finished
                    ? Menu.icon(Material.LIME_STAINED_GLASS_PANE, "&aWykonane", "&7" + job.amount() + "× "
                    + Items.name(job.item()), refresh)
                    : Menu.withLore(job.item(), "", "&ePrzynieś: &f" + job.amount() + " szt.",
                    "&eNagroda: &f" + rewardText(job),
                    "&7Masz: &f" + Items.count(player, job.item()), "&aKliknij, żeby oddać.", refresh);
            menu.getInventory().setItem(i, icon);
        }
    }

    void complete(Player player, int index) {
        List<Integer> done = new ArrayList<>(oasis.data().getIntegerList(doneKey(player)));
        if (done.contains(index)) {
            player.sendMessage("§7To zlecenie już wykonałeś. Nowe pojawią się po odświeżeniu tablicy.");
            return;
        }
        Job job = job(index);
        if (Items.count(player, job.item()) < job.amount()) {
            player.sendMessage("§cPotrzebujesz " + job.amount() + "× " + Items.name(job.item()) + "§c.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
            return;
        }
        ItemStack prize = null;
        if (job.hasItemReward()) {
            // budujemy na nowo: przedmioty AxonItems mogą zmienić wygląd po przeładowaniu
            prize = Items.create(job.rewardSpec(), job.rewardAmount());
            if (prize == null) {
                player.sendMessage("§cNagroda tego zlecenia jest chwilowo niedostępna. Spróbuj później.");
                return;
            }
        }
        Items.take(player, job.item(), job.amount());
        if (job.reward() > 0) {
            oasis.vault().deposit(player, job.reward());
        }
        if (prize != null) {
            Items.give(player, Items.stacks(prize, job.rewardAmount()));
        }
        done.add(index);
        oasis.data().set(doneKey(player), done);
        oasis.markDirty();
        player.sendMessage("§aZlecenie wykonane! §7Nagroda: §f" + rewardText(job));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }
}
