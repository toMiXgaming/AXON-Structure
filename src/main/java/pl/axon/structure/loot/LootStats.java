package pl.axon.structure.loot;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Statystyki lootu w {@code statystyki.yml}: ile pojemników w strukturach każdego tieru i przy drogach otworzył albo
 * rozbił gracz, ilu strażników zabił i jakie przedmioty z tego dostał. Na tej podstawie da się po wipe'ie policzyć
 * prawdziwy loot na godzinę gry (czas gry jest w statystykach Minecrafta, {@code play_time}) i skalibrować tabele.
 *
 * <p>Liczą się tylko pojemniki-modele i strażnicy, bo tam wiadomo, kto wylosował loot. Skrzynie-bloki losują się
 * przy odświeżeniu struktury, bez gracza. Zapis co kilka minut i przy wyłączeniu, tylko gdy coś się zmieniło.
 */
public final class LootStats {

    /** Klucz id przedmiotu AxonItems w PDC. */
    private static final NamespacedKey AXON_ID = new NamespacedKey("axon", "item_id");

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> players = new HashMap<>();
    private boolean enabled = true;
    private boolean dirty;

    private static final class Entry {
        String name;
        final Map<String, Long> sources = new TreeMap<>();
        final Map<String, Long> items = new TreeMap<>();
    }

    public LootStats(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "statystyki.yml");
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Loot z jednego źródła trafił do gracza.
     *
     * @param source np. {@code pojemnik-t3}, {@code pojemnik-droga}, {@code npc-t2}
     */
    public void record(Player player, String source, Collection<ItemStack> items) {
        if (!enabled || player == null) {
            return;
        }
        Entry entry = players.computeIfAbsent(player.getUniqueId(), k -> new Entry());
        entry.name = player.getName();
        entry.sources.merge(source, 1L, Long::sum);
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                entry.items.merge(idOf(item), (long) item.getAmount(), Long::sum);
            }
        }
        dirty = true;
    }

    /** {@code axon:<id>} dla przedmiotów AxonItems, inaczej {@code minecraft:<materiał>}. */
    static String idOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String id = meta.getPersistentDataContainer().get(AXON_ID, PersistentDataType.STRING);
            if (id != null && !id.isBlank()) {
                return "axon:" + id.toLowerCase(Locale.ROOT);
            }
        }
        return item.getType().getKey().toString();
    }

    /** Źródło lootu z tieru: {@code <rodzaj>-t<poziom>}, przy drodze {@code <rodzaj>-droga}. */
    public static String source(String kind, TierConfig.Tier tier, boolean roadside) {
        return kind + "-" + (roadside ? "droga" : "t" + tier.level());
    }

    public void load() {
        players.clear();
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("gracze");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection p = section.getConfigurationSection(key);
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (p == null) {
                continue;
            }
            Entry entry = new Entry();
            entry.name = p.getString("nazwa", "?");
            readCounts(p.getConfigurationSection("zrodla"), entry.sources);
            readCounts(p.getConfigurationSection("przedmioty"), entry.items);
            players.put(id, entry);
        }
    }

    private static void readCounts(ConfigurationSection section, Map<String, Long> into) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            into.put(key.replace('/', ':'), section.getLong(key));
        }
    }

    /** Zapisuje, jeśli coś się zmieniło od ostatniego zapisu. */
    public void save() {
        if (!dirty) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(java.util.List.of(
                "Statystyki lootu AXON-Structure (statystyki.wlaczone w config.yml).",
                "zrodla: ile razy gracz dostał loot z pojemnika-modelu (pojemnik-t<tier>, pojemnik-droga)",
                "albo zabitego strażnika (npc-t<tier>); przedmioty: suma sztuk z tych źródeł (axon/<id> = axon:<id>).",
                "Czas gry gracza jest w world/stats/<uuid>.json (minecraft:play_time, w tickach)."));
        for (Map.Entry<UUID, Entry> e : players.entrySet()) {
            String base = "gracze." + e.getKey() + ".";
            yaml.set(base + "nazwa", e.getValue().name);
            // ':' w kluczu YAML jest w porządku, ale '.' nie – id przedmiotów zapisujemy z '/' (axon/hqm)
            e.getValue().sources.forEach((k, v) -> yaml.set(base + "zrodla." + k, v));
            e.getValue().items.forEach((k, v) -> yaml.set(base + "przedmioty." + k.replace(':', '/'), v));
        }
        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nie udało się zapisać " + file.getName(), e);
        }
    }
}
