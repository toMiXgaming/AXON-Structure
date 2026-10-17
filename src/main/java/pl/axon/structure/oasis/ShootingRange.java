package pl.axon.structure.oasis;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Strzelnica w oazie: trafienia w tarcze (punkty {@code punkt-tarcza}) z linii ognia ({@code punkt-strzelnica})
 * liczą się w serii trwającej {@code czas-serii-sekund} od pierwszego trafienia; dalsza tarcza daje więcej punktów.
 * Najlepsza seria tygodnia trafia do rankingu, a na koniec tygodnia zwycięzca dostaje nagrodę z Vaulta.
 * <p>
 * Liczą się strzały z łuku, kuszy i inne pociski, a z QualityArmory – przez jego zdarzenie trafienia w blok
 * (wołane przez refleksję, bo QualityArmory jest opcjonalny).
 */
final class ShootingRange {

    private static final double LINE_RADIUS = 7;
    private static final long SAME_TARGET_MS = 250;

    private record Series(long until, int points, int hits) {
    }

    private final OasisService oasis;
    private final Map<UUID, Series> series = new HashMap<>();
    private final Map<String, Long> lastHit = new HashMap<>();
    private int seconds = 60;
    private double weeklyReward = 1000;
    private ZoneId zone = ZoneId.systemDefault();
    private boolean qaHooked;

    ShootingRange(OasisService oasis) {
        this.oasis = oasis;
    }

    void configure(ConfigurationSection section, ZoneId zone) {
        seconds = section == null ? 60 : Math.max(10, section.getInt("czas-serii-sekund", 60));
        weeklyReward = section == null ? 1000 : Math.max(0, section.getDouble("nagroda-tygodnia", 1000));
        this.zone = zone;
    }

    /** Podpina się pod zdarzenie trafienia w blok z QualityArmory, jeśli jest na serwerze. */
    @SuppressWarnings("unchecked")
    void hookQualityArmory() {
        if (qaHooked) {
            return;
        }
        String[] names = {"me.zombie_striker.qg.api.QAWeaponDamageBlockEvent", "me.zombie_striker.qg.api.QAProjectileHitBlockEvent"};
        for (String name : names) {
            try {
                Class<?> type = Class.forName(name);
                if (!Event.class.isAssignableFrom(type)) {
                    continue;
                }
                Method playerGetter = getter(type, Player.class);
                Method blockGetter = getter(type, Block.class);
                if (playerGetter == null || blockGetter == null) {
                    continue;
                }
                EventExecutor executor = (listener, event) -> {
                    if (!type.isInstance(event)) {
                        return;
                    }
                    try {
                        Object p = playerGetter.invoke(event);
                        Object b = blockGetter.invoke(event);
                        if (p instanceof Player player && b instanceof Block block) {
                            hit(player, block);
                        }
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // inna wersja QualityArmory
                    }
                };
                Bukkit.getPluginManager().registerEvent((Class<? extends Event>) type, new Listener() {
                }, EventPriority.MONITOR, executor, oasis.plugin(), true);
                qaHooked = true;
                oasis.plugin().getLogger().info("Strzelnica liczy trafienia z QualityArmory (" + type.getSimpleName() + ").");
                return;
            } catch (ClassNotFoundException ignored) {
                // brak tej klasy w tej wersji
            }
        }
    }

    private static Method getter(Class<?> type, Class<?> returns) {
        for (Method m : type.getMethods()) {
            if (m.getParameterCount() == 0 && returns.isAssignableFrom(m.getReturnType())) {
                return m;
            }
        }
        return null;
    }

    /** Trafienie w blok: liczy się tylko tarcza oazy i tylko strzelec stojący przy linii ognia. */
    void hit(Player player, Block block) {
        OasisService.Point target = oasis.pointAt(block, "punkt-tarcza", 0.1);
        if (target == null) {
            return;
        }
        OasisService.Point line = oasis.nearestPoint(player.getLocation(), "punkt-strzelnica");
        if (line == null || line.center().distance(player.getLocation()) > LINE_RADIUS) {
            return;
        }
        long now = System.currentTimeMillis();
        String key = player.getUniqueId() + "@" + target.key();
        Long previous = lastHit.put(key, now);
        if (previous != null && now - previous < SAME_TARGET_MS) {
            return; // kilka zdarzeń z jednego strzału
        }
        rollWeek();
        double distance = player.getEyeLocation().distance(target.center());
        int points = 1 + (int) (distance / 10);
        Series s = series.get(player.getUniqueId());
        if (s == null || now > s.until()) {
            s = new Series(now + seconds * 1000L, 0, 0);
            UUID id = player.getUniqueId();
            Bukkit.getScheduler().runTaskLater(oasis.plugin(), () -> endSeries(id), seconds * 20L + 2);
            player.sendMessage("§eSeria na strzelnicy: §f" + seconds + " s§e. Strzelaj w tarcze!");
        }
        s = new Series(s.until(), s.points() + points, s.hits() + 1);
        series.put(player.getUniqueId(), s);
        player.playSound(target.center(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.8f + Math.min(1.2f, points * 0.2f));
        OasisService.actionBar(player, "§a+" + points + " §7(" + (int) distance + " m)  §fRazem: §e"
                + s.points() + " §7pkt, " + s.hits() + " trafień");
    }

    private void endSeries(UUID id) {
        Series s = series.get(id);
        if (s == null || System.currentTimeMillis() < s.until()) {
            return;
        }
        series.remove(id);
        Player player = Bukkit.getPlayer(id);
        String best = "strzelnica.wyniki." + id;
        int previous = oasis.data().getInt(best, 0);
        if (s.points() > previous) {
            oasis.data().set(best, s.points());
            if (player != null) {
                oasis.data().set("strzelnica.nazwy." + id, player.getName());
            }
            oasis.markDirty();
        }
        if (player != null) {
            player.sendMessage("§eKoniec serii: §f" + s.points() + " pkt §7(" + s.hits() + " trafień)"
                    + (s.points() > previous ? " §a– twój najlepszy wynik w tym tygodniu!" : "§7, rekord tygodnia: " + previous));
        }
    }

    private String week() {
        LocalDate today = LocalDate.now(zone);
        return today.get(IsoFields.WEEK_BASED_YEAR) + "-" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    /** Nowy tydzień: nagroda dla zwycięzcy poprzedniego i czysty ranking. */
    void rollWeek() {
        String now = week();
        String saved = oasis.data().getString("strzelnica.tydzien");
        if (now.equals(saved)) {
            return;
        }
        if (saved != null) {
            List<Map.Entry<String, Integer>> top = ranking();
            if (!top.isEmpty() && weeklyReward > 0) {
                Map.Entry<String, Integer> winner = top.get(0);
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(winner.getKey()));
                String name = oasis.data().getString("strzelnica.nazwy." + winner.getKey(), "?");
                if (oasis.vault().deposit(player, weeklyReward)) {
                    Bukkit.broadcastMessage("§6[Oaza] §fStrzelec tygodnia: §e" + name + " §f(" + winner.getValue()
                            + " pkt) dostaje §e" + oasis.vault().format(weeklyReward) + "§f!");
                }
            }
        }
        oasis.data().set("strzelnica.tydzien", now);
        oasis.data().set("strzelnica.wyniki", null);
        oasis.data().set("strzelnica.nazwy", null);
        oasis.markDirty();
    }

    List<Map.Entry<String, Integer>> ranking() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        ConfigurationSection section = oasis.data().getConfigurationSection("strzelnica.wyniki");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                list.add(Map.entry(key, section.getInt(key)));
            }
        }
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list;
    }

    void showRanking(org.bukkit.command.CommandSender sender) {
        rollWeek();
        List<Map.Entry<String, Integer>> top = ranking();
        sender.sendMessage("§6§lStrzelnica – ranking tygodnia §7(nagroda: " + oasis.vault().format(weeklyReward) + ")");
        if (top.isEmpty()) {
            sender.sendMessage("§7Nikt jeszcze nie strzelał w tym tygodniu.");
        }
        for (int i = 0; i < top.size() && i < 10; i++) {
            Map.Entry<String, Integer> e = top.get(i);
            sender.sendMessage("§e" + (i + 1) + ". §f" + oasis.data().getString("strzelnica.nazwy." + e.getKey(), "?")
                    + " §7– " + e.getValue() + " pkt");
        }
    }
}
