package pl.axon.structure.vehicle;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Konfiguracja wraków przy drogach ({@code wraki:} w config.yml).
 *
 * <p>Wraki stawiamy wzdłuż poboczy dróg wygenerowanych przez AxonMapGenerator. AxonMapGenerator
 * nie zna modeli z paczki, więc to AXON-Structure dostaje listę punktów pobocza przez API
 * ({@code setRoadsideContainers}) i sam decyduje, gdzie postawić wrak, jak go obrócić i czy
 * w ogóle go stawiać (szansa procentowa).
 *
 * <p>Klasa jest czysta (bez Bukkita poza {@link ConfigurationSection}) — testy jednostkowe
 * sprawdzają parsowanie i walidację bez serwera.
 */
public final class VehicleConfig {

    /** Waga „brak wraku” w puli — pozwala zostawić puste pobocze. */
    public static final String NONE = "brak";

    private final boolean enabled;
    private final List<String> worlds;
    private final double chancePercent;
    private final int minSpacing;
    private final boolean barriers;
    private final boolean loot;
    private final String lootPool;
    private final Map<String, Integer> weights;
    private final List<String> errors;

    private VehicleConfig(boolean enabled, List<String> worlds, double chancePercent, int minSpacing,
                          boolean barriers, boolean loot, String lootPool,
                          Map<String, Integer> weights, List<String> errors) {
        this.enabled = enabled;
        this.worlds = List.copyOf(worlds);
        this.chancePercent = chancePercent;
        this.minSpacing = minSpacing;
        this.barriers = barriers;
        this.loot = loot;
        this.lootPool = lootPool;
        this.weights = Map.copyOf(weights);
        this.errors = List.copyOf(errors);
    }

    /** Domyślne ustawienia: wraki włączone, 35% szans na pobocze, bariery i loot włączone. */
    public static VehicleConfig defaults() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("wrak_fiat_126p", 22);
        weights.put("wrak_polonez", 18);
        weights.put("wrak_zuk", 14);
        weights.put("wrak_nysa", 12);
        weights.put("wrak_ursus", 12);
        weights.put("wrak_wsk", 10);
        weights.put("wrak_star_266", 7);
        weights.put("wrak_autosan", 5);
        weights.put(NONE, 25);
        return new VehicleConfig(true, List.of("world"), 35.0, 24, true, true, "wrak", weights, List.of());
    }

    /**
     * Wczytuje sekcję {@code wraki:}. Brak sekcji = {@link #defaults()}. Każdy błąd trafia do
     * {@link #errors()} i jest logowany przez plugin, ale nie blokuje startu — złe wpisy są
     * po prostu pomijane.
     */
    public static VehicleConfig load(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        List<String> errors = new ArrayList<>();

        boolean enabled = section.getBoolean("wlaczone", true);
        List<String> worlds = new ArrayList<>(section.getStringList("swiaty"));
        if (worlds.isEmpty()) {
            worlds.add("world");
        }

        double chance = clampDouble(section.getDouble("szansa-procent", 35.0), 0.0, 100.0,
                "szansa-procent", errors);
        int spacing = clampInt(section.getInt("min-odstep", 24), 4, 512, "min-odstep", errors);
        boolean barriers = section.getBoolean("bariery", true);
        boolean loot = section.getBoolean("loot", true);
        String lootPool = section.getString("pula-lootu", "wrak");
        if (lootPool == null || lootPool.isBlank()) {
            lootPool = "wrak";
        }

        Map<String, Integer> weights = new LinkedHashMap<>();
        ConfigurationSection weightSection = section.getConfigurationSection("modele");
        if (weightSection == null) {
            // Brak listy modeli = wszystkie wraki po równo + „brak”.
            for (VehicleModel model : VehicleModel.ALL) {
                weights.put(model.id(), 10);
            }
            weights.put(NONE, 20);
        } else {
            for (String rawKey : weightSection.getKeys(false)) {
                String key = rawKey.trim().toLowerCase(Locale.ROOT);
                int weight = weightSection.getInt(rawKey);
                if (weight <= 0) {
                    errors.add("modele." + rawKey + ": waga musi być dodatnia (jest " + weight + ")");
                    continue;
                }
                if (!NONE.equals(key) && VehicleModel.byId(key) == null) {
                    errors.add("modele." + rawKey + ": nieznany wrak (dostępne: "
                            + String.join(", ", VehicleModel.ids()) + ")");
                    continue;
                }
                weights.put(key, weight);
            }
            if (weights.isEmpty()) {
                errors.add("modele: pusta lista – używam domyślnych wag");
                for (VehicleModel model : VehicleModel.ALL) {
                    weights.put(model.id(), 10);
                }
                weights.put(NONE, 20);
            }
        }

        return new VehicleConfig(enabled, worlds, chance, spacing, barriers, loot, lootPool, weights, errors);
    }

    public boolean enabled() {
        return enabled;
    }

    public List<String> worlds() {
        return worlds;
    }

    public double chancePercent() {
        return chancePercent;
    }

    public int minSpacing() {
        return minSpacing;
    }

    public boolean barriers() {
        return barriers;
    }

    public boolean loot() {
        return loot;
    }

    public String lootPool() {
        return lootPool;
    }

    /** Wagi modeli: id wraku (albo {@link #NONE}) → waga. Zawsze zawiera co najmniej jeden wpis. */
    public Map<String, Integer> weights() {
        return weights;
    }

    public List<String> errors() {
        return errors;
    }

    /** Czy w tym świecie wraki są włączone. */
    public boolean inWorld(String world) {
        return enabled && world != null && worlds.contains(world);
    }

    /**
     * Losuje model wraku (albo {@code null} = brak wraku) według wag. Zwraca {@code null}, gdy
     * wylosowano {@link #NONE} lub gdy wagi są puste.
     */
    public VehicleModel roll(java.util.Random random) {
        if (random == null || weights.isEmpty()) {
            return null;
        }
        int total = 0;
        for (int weight : weights.values()) {
            total += weight;
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return NONE.equals(entry.getKey()) ? null : VehicleModel.byId(entry.getKey());
            }
        }
        return null;
    }

    private static int clampInt(int value, int min, int max, String key, List<String> errors) {
        if (value < min || value > max) {
            errors.add(key + ": " + value + " poza zakresem " + min + "–" + max + " – przycinam");
            return Math.max(min, Math.min(max, value));
        }
        return value;
    }

    private static double clampDouble(double value, double min, double max, String key, List<String> errors) {
        if (Double.isNaN(value) || value < min || value > max) {
            errors.add(key + ": " + value + " poza zakresem " + min + "–" + max + " – przycinam");
            return Math.max(min, Math.min(max, Double.isNaN(value) ? min : value));
        }
        return value;
    }
}
