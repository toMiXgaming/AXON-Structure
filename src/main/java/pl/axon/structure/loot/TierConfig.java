package pl.axon.structure.loot;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Tiery struktur z sekcji {@code tiery} w config.yml. Tier mówi, jak bogaty jest loot w strukturze: im wyższy, tym
 * więcej sztuk w stosach ({@code ilosc}), tym częściej zwykła skrzynia z puli zamienia się w lepszą klasę
 * ({@code ulepszenia}) i tym więcej dodatkowych pozycji może wypaść w każdym pojemniku ({@code dodatki}).
 *
 * <p>Tier dostaje cała struktura (id pliku .axs, np. {@code galeria}), a osobno pojemniki przy drogach z
 * AxonMapGenerator. Poziom 0 albo {@code wlaczone: false} = loot dokładnie taki, jak w klasach i pulach.
 */
public final class TierConfig {

    /**
     * Jeden poziom.
     *
     * @param level    numer poziomu (1, 2, 3...), 0 = bez tieru
     * @param name     nazwa do komunikatów (z kolorami)
     * @param amount   mnożnik liczby sztuk w stosach; ułamek dolosowuje sztukę, pojedyncze przedmioty bez zmian
     * @param upgrades klasa z puli → (lepsza klasa → szansa w procentach); losowane raz, najwyżej jedno ulepszenie
     * @param extras   dodatkowe pozycje lootu losowane w każdym pojemniku tej struktury (każda z własną szansą)
     */
    public record Tier(int level, String name, double amount, Map<String, LinkedHashMap<String, Double>> upgrades,
                       List<CrateClass.LootEntry> extras) {

        /** Bez tieru: loot jak w klasie. */
        public static final Tier NONE = new Tier(0, "", 1.0, Map.of(), List.of());

        public Tier {
            amount = Math.max(0.0, amount);
            upgrades = Collections.unmodifiableMap(new LinkedHashMap<>(upgrades));
            extras = List.copyOf(extras);
        }

        /** Klasa po ulepszeniu albo ta sama, gdy nic nie wypadło (wynik może być null tylko, gdy wejście jest null). */
        public String upgrade(String classId, Random random) {
            if (classId == null) {
                return null;
            }
            LinkedHashMap<String, Double> targets = upgrades.get(classId.toLowerCase(Locale.ROOT));
            if (targets == null || targets.isEmpty()) {
                return classId;
            }
            double roll = random.nextDouble() * 100.0;
            for (Map.Entry<String, Double> target : targets.entrySet()) {
                roll -= target.getValue();
                if (roll < 0) {
                    return target.getKey();
                }
            }
            return classId;
        }

        /**
         * Liczba sztuk po mnożniku {@code ilosc}. Część ułamkowa dolosowuje jedną sztukę (×1,3 z 5 daje 6 w 50% i 7
         * w 50%), więc średnio wychodzi dokładnie {@code amount × mnożnik}. Pojedyncze przedmioty (korpusy, karty,
         * 1 HQM) i rzeczy niestackowalne się nie mnożą – tier ma dawać więcej materiałów, a nie podwajać rzadkości.
         */
        public int scale(int amount, int maxStack, Random random) {
            if (amount <= 1 || maxStack <= 1 || this.amount == 1.0) {
                return amount;
            }
            double exact = amount * this.amount;
            int whole = (int) Math.floor(exact);
            if (random.nextDouble() < exact - whole) {
                whole++;
            }
            return Math.max(1, Math.min(maxStack, whole));
        }

        /** Ten sam tier bez dodatków (loot zabitych strażników: tylko więcej sztuk). */
        public Tier withoutExtras() {
            return extras.isEmpty() ? this : new Tier(level, name, amount, upgrades, List.of());
        }

        /** Nazwa do komunikatów: z configu albo „Tier N”. */
        public String label() {
            return level <= 0 ? "bez tieru" : name.isBlank() ? "Tier " + level : name;
        }
    }

    /** Tiery wyłączone: wszędzie {@link Tier#NONE}. */
    public static final TierConfig DISABLED = new TierConfig(false, 0, 0, Map.of(), Map.of());

    private final boolean enabled;
    private final int defaultLevel;
    private final int roadsideLevel;
    private final Map<String, Integer> structures;
    private final Map<Integer, Tier> levels;

    TierConfig(boolean enabled, int defaultLevel, int roadsideLevel, Map<String, Integer> structures,
               Map<Integer, Tier> levels) {
        this.enabled = enabled;
        this.defaultLevel = defaultLevel;
        this.roadsideLevel = roadsideLevel;
        this.structures = Collections.unmodifiableMap(new LinkedHashMap<>(structures));
        this.levels = Collections.unmodifiableMap(new TreeMap<>(levels));
    }

    public boolean enabled() {
        return enabled;
    }

    /** Tier struktury o tym id (nazwa pliku .axs bez rozszerzenia); bez wpisu – {@code domyslny}. */
    public Tier forStructure(String structureId) {
        if (structureId == null) {
            return Tier.NONE;
        }
        return level(structures.getOrDefault(structureId.toLowerCase(Locale.ROOT), defaultLevel));
    }

    /** Tier pojemników przy drogach z AxonMapGenerator. */
    public Tier roadside() {
        return level(roadsideLevel);
    }

    /** Poziom o numerze; nieznany albo 0 = {@link Tier#NONE}. */
    public Tier level(int level) {
        if (!enabled || level <= 0) {
            return Tier.NONE;
        }
        return levels.getOrDefault(level, Tier.NONE);
    }

    /** Numer tieru struktury (do list i komend). */
    public int levelOf(String structureId) {
        return forStructure(structureId).level();
    }

    public int defaultLevel() {
        return defaultLevel;
    }

    public int roadsideLevel() {
        return roadsideLevel;
    }

    public Map<String, Integer> structures() {
        return structures;
    }

    public Map<Integer, Tier> levels() {
        return levels;
    }

    /**
     * Czyta sekcję {@code tiery}. Ulepszenia sprawdza z klasami: cel musi istnieć i dać się postawić w tym samym
     * miejscu (ramka tylko w ramkę, skrzynia albo beczka w skrzynię, beczkę albo pojemnik-model, model w model).
     */
    public static TierConfig load(ConfigurationSection section, Map<String, CrateClass> classes, Logger logger) {
        if (section == null || !section.getBoolean("wlaczone", true)) {
            return DISABLED;
        }
        Map<String, Integer> structures = new LinkedHashMap<>();
        ConfigurationSection structuresSection = section.getConfigurationSection("struktury");
        if (structuresSection != null) {
            for (String id : structuresSection.getKeys(false)) {
                structures.put(id.toLowerCase(Locale.ROOT), Math.max(0, structuresSection.getInt(id)));
            }
        }
        Map<Integer, Tier> levels = new TreeMap<>();
        ConfigurationSection levelsSection = section.getConfigurationSection("poziomy");
        if (levelsSection != null) {
            for (String key : levelsSection.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(key.trim());
                } catch (NumberFormatException e) {
                    logger.warning("tiery.poziomy: '" + key + "' to nie numer poziomu, pomijam");
                    continue;
                }
                ConfigurationSection tier = levelsSection.getConfigurationSection(key);
                if (level <= 0 || tier == null) {
                    logger.warning("tiery.poziomy." + key + ": poziom musi być liczbą od 1 w górę, pomijam");
                    continue;
                }
                levels.put(level, parseTier(level, tier, classes, logger));
            }
        }
        int defaultLevel = Math.max(0, section.getInt("domyslny", 1));
        int roadsideLevel = Math.max(0, section.getInt("przy-drogach", 1));
        for (Map.Entry<String, Integer> e : structures.entrySet()) {
            if (e.getValue() > 0 && !levels.containsKey(e.getValue())) {
                logger.warning("tiery.struktury." + e.getKey() + ": nie ma poziomu " + e.getValue()
                        + " w tiery.poziomy – struktura będzie bez tieru");
            }
        }
        return new TierConfig(true, defaultLevel, roadsideLevel, structures, levels);
    }

    private static Tier parseTier(int level, ConfigurationSection section, Map<String, CrateClass> classes,
                                  Logger logger) {
        String where = "tiery.poziomy." + level;
        String name = LootConfig.color(section.getString("nazwa", ""));
        double amount = section.getDouble("ilosc", 1.0);
        if (amount <= 0) {
            logger.warning(where + ".ilosc musi być większe od 0, biorę 1.0");
            amount = 1.0;
        }
        Map<String, LinkedHashMap<String, Double>> upgrades = new LinkedHashMap<>();
        ConfigurationSection upgradesSection = section.getConfigurationSection("ulepszenia");
        if (upgradesSection != null) {
            for (String from : upgradesSection.getKeys(false)) {
                String fromId = from.toLowerCase(Locale.ROOT);
                CrateClass source = classes.get(fromId);
                ConfigurationSection targets = upgradesSection.getConfigurationSection(from);
                if (source == null || targets == null) {
                    logger.warning(where + ".ulepszenia: nieznana klasa '" + from + "', pomijam");
                    continue;
                }
                LinkedHashMap<String, Double> chances = new LinkedHashMap<>();
                double total = 0;
                for (String to : targets.getKeys(false)) {
                    String toId = to.toLowerCase(Locale.ROOT);
                    CrateClass target = classes.get(toId);
                    if (target == null) {
                        logger.warning(where + ".ulepszenia." + from + ": nieznana klasa '" + to + "', pomijam");
                        continue;
                    }
                    if (!compatible(source.kind(), target.kind())) {
                        logger.warning(where + ".ulepszenia." + from + " -> " + to + ": klasy "
                                + source.kind().name().toLowerCase(Locale.ROOT) + " nie da się zamienić w "
                                + target.kind().name().toLowerCase(Locale.ROOT) + ", pomijam");
                        continue;
                    }
                    double chance = Math.max(0, targets.getDouble(to));
                    if (chance > 0) {
                        chances.put(toId, chance);
                        total += chance;
                    }
                }
                if (total > 100) {
                    logger.warning(where + ".ulepszenia." + from + ": szanse sumują się do " + total
                            + "% – skaluję do 100%");
                    double factor = 100.0 / total;
                    chances.replaceAll((k, v) -> v * factor);
                }
                if (!chances.isEmpty()) {
                    upgrades.put(fromId, chances);
                }
            }
        }
        List<CrateClass.LootEntry> extras = new ArrayList<>(
                LootConfig.parseEntries(where + ".dodatki", section.getMapList("dodatki"), logger));
        return new Tier(level, name, amount, upgrades, extras);
    }

    /** Czy klasę {@code from} da się zamienić w {@code to} w tym samym miejscu. */
    static boolean compatible(CrateClass.Kind from, CrateClass.Kind to) {
        if (from == CrateClass.Kind.FRAME || to == CrateClass.Kind.FRAME) {
            return from == to;
        }
        if (from == CrateClass.Kind.MODEL) {
            return to == CrateClass.Kind.MODEL; // pojemniki-modele losuje ContainerService: tylko modele
        }
        return true; // skrzynia/beczka: CrateService postawi blok albo pojemnik-model
    }
}
