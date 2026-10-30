package pl.axon.structure.loot;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Klasy skrzyń i pule wczytane z sekcji {@code klasy} i {@code pule} w config.yml. */
public final class LootConfig {

    private final Map<String, CrateClass> classes = new LinkedHashMap<>();
    private final Map<String, CratePool> pools = new LinkedHashMap<>();

    /** Sekcja z pojemnikami-modelami; jej klasy i pule zastępują te o tym samym id z {@code klasy} i {@code pule}. */
    public static final String CONTAINERS = "pojemniki";

    public static LootConfig load(ConfigurationSection root, Logger logger) {
        LootConfig config = new LootConfig();
        config.loadClasses(root.getConfigurationSection("klasy"), logger);
        config.loadClasses(root.getConfigurationSection(CONTAINERS + ".klasy"), logger);
        config.loadPools(root.getConfigurationSection("pule"), logger);
        config.loadPools(root.getConfigurationSection(CONTAINERS + ".pule"), logger);
        return config;
    }

    private void loadClasses(ConfigurationSection classesSection, Logger logger) {
        if (classesSection == null) {
            return;
        }
        for (String id : classesSection.getKeys(false)) {
            ConfigurationSection section = classesSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                classes.put(id.toLowerCase(Locale.ROOT), parseClass(id, section, logger));
            } catch (IllegalArgumentException e) {
                logger.severe("Klasa skrzyni '" + id + "': " + e.getMessage());
            }
        }
    }

    private void loadPools(ConfigurationSection poolsSection, Logger logger) {
        if (poolsSection == null) {
            return;
        }
        for (String id : poolsSection.getKeys(false)) {
            ConfigurationSection section = poolsSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            LinkedHashMap<String, Integer> weights = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                String classId = key.toLowerCase(Locale.ROOT);
                if (!classId.equals(CratePool.NONE) && !classes.containsKey(classId)) {
                    logger.warning("Pula '" + id + "' odwołuje się do nieznanej klasy '" + key + "'");
                    continue;
                }
                weights.put(classId, section.getInt(key));
            }
            pools.put(id.toLowerCase(Locale.ROOT), new CratePool(id, weights));
        }
    }

    private static CrateClass parseClass(String id, ConfigurationSection section, Logger logger) {
        CrateClass.Kind kind = CrateClass.Kind.parse(section.getString("blok", "skrzynia"));
        String name = color(section.getString("nazwa", id));
        String decoration = section.getString("dekoracja", "");
        String model = section.getString("model", "");
        int minItems = section.getInt("min-przedmiotow", 1);
        List<CrateClass.LootEntry> loot = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("loot")) {
            Map<String, Object> entry = new HashMap<>();
            raw.forEach((k, v) -> entry.put(String.valueOf(k), v));
            Object itemName = entry.get("przedmiot");
            String itemId = itemName == null ? "" : String.valueOf(itemName).trim();
            String axonId = null;
            Material material = null;
            if (itemId.toLowerCase(Locale.ROOT).startsWith(CrateClass.LootEntry.CARD_PREFIX)) {
                String color = itemId.substring(CrateClass.LootEntry.CARD_PREFIX.length()).trim()
                        .toLowerCase(Locale.ROOT);
                if (!AccessCardMath.known(color)) {
                    logger.warning("Klasa '" + id + "': nieznana karta '" + itemName
                            + "' (niebieska, czerwona, zielona), pomijam");
                    continue;
                }
                double chance = Double.parseDouble(String.valueOf(entry.getOrDefault("szansa", 100)));
                loot.add(CrateClass.LootEntry.card(color, chance));
                continue;
            }
            if (itemId.toLowerCase(Locale.ROOT).startsWith(AxonItemsBridge.TABLE_PREFIX)) {
                String table = itemId.substring(AxonItemsBridge.TABLE_PREFIX.length()).toLowerCase(Locale.ROOT);
                if (table.isBlank()) {
                    logger.warning("Klasa '" + id + "': pusta nazwa tabeli w '" + itemName + "', pomijam");
                    continue;
                }
                double chance = Double.parseDouble(String.valueOf(entry.getOrDefault("szansa", 100)));
                loot.add(CrateClass.LootEntry.table(table, chance));
                continue;
            }
            if (itemId.toLowerCase(Locale.ROOT).startsWith(AxonItemsBridge.PREFIX)) {
                // sprawdzane dopiero przy losowaniu: AxonItems włącza się po nas (load: STARTUP)
                axonId = itemId.substring(AxonItemsBridge.PREFIX.length()).toLowerCase(Locale.ROOT);
            } else {
                material = Material.matchMaterial(itemId);
            }
            if (material == null && (axonId == null || axonId.isBlank())) {
                logger.warning("Klasa '" + id + "': nieznany przedmiot '" + itemName + "', pomijam");
                continue;
            }
            int[] amount = parseRange(String.valueOf(entry.getOrDefault("ilosc", "1")));
            double chance = Double.parseDouble(String.valueOf(entry.getOrDefault("szansa", 100)));
            String display = entry.containsKey("nazwa") ? color(String.valueOf(entry.get("nazwa"))) : null;
            List<String> lore = new ArrayList<>();
            if (entry.get("opis") instanceof List<?> lines) {
                for (Object line : lines) {
                    lore.add(color(String.valueOf(line)));
                }
            }
            int fromDay = Math.max(0, Integer.parseInt(String.valueOf(entry.getOrDefault("od-dnia", 0)).trim()));
            loot.add(new CrateClass.LootEntry(material, axonId, amount[0], amount[1], chance, display, lore, null,
                    fromDay));
        }
        CrateClass.ContainerSpec container = null;
        if (kind == CrateClass.Kind.MODEL) {
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("blok: model wymaga pola model (np. axon:beczka)");
            }
            long respawn = section.contains("odnowienie-minut")
                    ? Math.max(1, section.getLong("odnowienie-minut")) * 60_000L : -1;
            container = new CrateClass.ContainerSpec(section.getString("model-rozbity", ""),
                    Math.max(1, section.getInt("uderzenia", 3)),
                    Math.max(1, section.getInt("uderzenia-narzedziem", 2)),
                    respawn,
                    color(section.getString("ogloszenie", "")),
                    Math.max(1, Math.min(3, section.getInt("dlugosc", 1))),
                    (float) Math.max(0.2, section.getDouble("szerokosc", 0.9)),
                    (float) Math.max(0.2, section.getDouble("wysokosc", 1.0)),
                    Math.max(0, section.getInt("blokada-minut", 0)),
                    Math.max(0, Math.min(3, section.getInt("karta-poziom", 0))));
        }
        return new CrateClass(id, name, kind, decoration == null ? "" : decoration,
                model == null ? "" : model, minItems, loot, container);
    }

    private static int[] parseRange(String value) {
        String v = value.trim();
        int dash = v.indexOf('-', 1);
        if (dash < 0) {
            int n = Integer.parseInt(v);
            return new int[]{n, n};
        }
        int a = Integer.parseInt(v.substring(0, dash).trim());
        int b = Integer.parseInt(v.substring(dash + 1).trim());
        return new int[]{Math.min(a, b), Math.max(a, b)};
    }

    @SuppressWarnings("deprecation")
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public CrateClass crateClass(String id) {
        return classes.get(id.toLowerCase(Locale.ROOT));
    }

    public CratePool pool(String id) {
        return pools.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, CrateClass> classes() {
        return classes;
    }

    public Map<String, CratePool> pools() {
        return pools;
    }
}
