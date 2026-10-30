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

    public static LootConfig load(ConfigurationSection root, Logger logger) {
        LootConfig config = new LootConfig();
        ConfigurationSection classesSection = root.getConfigurationSection("klasy");
        if (classesSection != null) {
            for (String id : classesSection.getKeys(false)) {
                ConfigurationSection section = classesSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                try {
                    config.classes.put(id.toLowerCase(Locale.ROOT), parseClass(id, section, logger));
                } catch (IllegalArgumentException e) {
                    logger.severe("Klasa skrzyni '" + id + "': " + e.getMessage());
                }
            }
        }
        ConfigurationSection poolsSection = root.getConfigurationSection("pule");
        if (poolsSection != null) {
            for (String id : poolsSection.getKeys(false)) {
                ConfigurationSection section = poolsSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                LinkedHashMap<String, Integer> weights = new LinkedHashMap<>();
                for (String key : section.getKeys(false)) {
                    String classId = key.toLowerCase(Locale.ROOT);
                    if (!classId.equals(CratePool.NONE) && !config.classes.containsKey(classId)) {
                        logger.warning("Pula '" + id + "' odwołuje się do nieznanej klasy '" + key + "'");
                        continue;
                    }
                    weights.put(classId, section.getInt(key));
                }
                config.pools.put(id.toLowerCase(Locale.ROOT), new CratePool(id, weights));
            }
        }
        return config;
    }

    private static CrateClass parseClass(String id, ConfigurationSection section, Logger logger) {
        CrateClass.Kind kind = CrateClass.Kind.parse(section.getString("blok", "skrzynia"));
        String name = color(section.getString("nazwa", id));
        String decoration = section.getString("dekoracja", "");
        String modelName = section.getString("model", id);
        String zone = color(section.getString("strefa", ""));
        int minItems = section.getInt("min-przedmiotow", 1);
        // 0 = globalny interwał z odswiezanie.co-ile-minut; skrzynia wojskowa ustawia 30.
        int refreshMinutes = Math.max(0, section.getInt("odswiezanie-minut", 0));
        // 0 = brak blokady; elitarna skrzynia ustawia 10, żeby nie dało się jej otworzyć od razu.
        int lockMinutes = Math.max(0, section.getInt("blokada-minut", 0));
        // 0 = bez karty; elitarna skrzynia ustawia 3 (tylko zielona karta ją otwiera).
        int accessLevel = Math.max(0, section.getInt("karta-poziom", 0));
        // Opcjonalny drop karty dostępu: {karta-drop: niebieska, karta-szansa: 8}
        // Karta jest prawdziwym przedmiotem z PDC (tworzona przez AccessCardService),
        // więc działa w stole i przy elitarnych skrzyniach.
        String cardDrop = section.getString("karta-drop", null);
        if (cardDrop != null) {
            cardDrop = cardDrop.trim().toLowerCase(Locale.ROOT);
            if (!AccessCardMath.known(cardDrop)) {
                logger.warning("Klasa '" + id + "': nieznana karta-drop '" + cardDrop + "', pomijam");
                cardDrop = null;
            }
        }
        double cardChance = cardDrop == null ? 0 : section.getDouble("karta-szansa", 0);
        List<CrateClass.LootEntry> loot = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("loot")) {
            Map<String, Object> entry = new HashMap<>();
            raw.forEach((k, v) -> entry.put(String.valueOf(k), v));

            // 1) Odwołanie do całej tabeli lootu AxonItems: {tabela: beczka_przydrozna}
            Object tableName = entry.get("tabela");
            if (tableName != null && !String.valueOf(tableName).isBlank()) {
                loot.add(CrateClass.LootEntry.ofTable(String.valueOf(tableName).trim()));
                continue;
            }

            Object itemName = entry.get("przedmiot");
            if (itemName == null) {
                logger.warning("Klasa '" + id + "': wpis bez 'przedmiot' ani 'tabela', pomijam");
                continue;
            }
            String rawItem = String.valueOf(itemName).trim();
            int[] amount = parseRange(raw(entry, "ilosc", "1"));
            double chance = Double.parseDouble(raw(entry, "szansa", "100"));

            // 2) Przedmiot AxonItems: {przedmiot: axon:scrap, ilosc: "2-6"}
            if (rawItem.toLowerCase(Locale.ROOT).startsWith("axon:")) {
                String axonId = rawItem.substring("axon:".length()).trim().toLowerCase(Locale.ROOT);
                if (axonId.isEmpty()) {
                    logger.warning("Klasa '" + id + "': pusty identyfikator 'axon:', pomijam");
                    continue;
                }
                loot.add(CrateClass.LootEntry.ofAxon(axonId, amount[0], amount[1], chance));
                continue;
            }

            // 3) Zwykły przedmiot z Minecrafta.
            Material material = Material.matchMaterial(rawItem);
            if (material == null) {
                logger.warning("Klasa '" + id + "': nieznany przedmiot '" + rawItem + "', pomijam");
                continue;
            }
            String display = entry.containsKey("nazwa") ? color(String.valueOf(entry.get("nazwa"))) : null;
            List<String> lore = new ArrayList<>();
            if (entry.get("opis") instanceof List<?> lines) {
                for (Object line : lines) {
                    lore.add(color(String.valueOf(line)));
                }
            }
            loot.add(CrateClass.LootEntry.ofMaterial(material, amount[0], amount[1], chance, display, lore));
        }
        return new CrateClass(id, name, kind, decoration == null ? "" : decoration, minItems, loot,
                modelName, zone, refreshMinutes, lockMinutes, accessLevel, cardDrop, cardChance);
    }

    private static String raw(Map<String, Object> entry, String key, String fallback) {
        Object value = entry.get(key);
        return value == null ? fallback : String.valueOf(value);
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
