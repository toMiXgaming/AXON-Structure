package pl.axon.structure.gui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * Wejście do pełnoekranowego GUI (sekcja {@code gui:} w configu). Decyduje, czy okno oazy otworzy się jako ekran
 * z shaderem, czy jako zwykła skrzynia (tryb awaryjny): skrzynia, gdy nie ma PacketEvents, ekran jest wyłączony
 * w configu, gracz nie wczytał paczki zasobów, sam wybrał skrzynię ({@code /oaza gui}) albo nie da się go teraz
 * posadzić (pojazd, skok, kucanie). Klasy z PacketEvents wczytują się dopiero, gdy ten plugin jest włączony.
 */
public final class ShaderGui implements Listener {

    private static final String PACKETEVENTS = "packetevents";

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Set<UUID> packLoaded = new HashSet<>();
    private final Set<UUID> chestMode = new HashSet<>();
    private boolean enabled = true;
    private boolean requirePack = true;
    private GuiBackend backend;
    private boolean started;

    public ShaderGui(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "gui.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        for (String id : data.getStringList("tryb-skrzyni")) {
            try {
                chestMode.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // zły wpis w gui.yml: pomijamy
            }
        }
    }

    public void configure(ConfigurationSection section) {
        enabled = section == null || section.getBoolean("shader", true);
        requirePack = section == null || section.getBoolean("wymagaj-paczki", true);
    }

    /** Rejestruje zdarzenia i, jeśli jest PacketEvents, włącza silnik ekranów. */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (!Bukkit.getPluginManager().isPluginEnabled(PACKETEVENTS)) {
            plugin.getLogger().info("GUI: brak PacketEvents, okna oazy otwierają się jako skrzynie.");
            return;
        }
        try {
            backend = PacketGui.create(plugin, this);
            plugin.getLogger().info("GUI: ekrany z shaderem gotowe (PacketEvents), skrzynia jako tryb awaryjny.");
        } catch (RuntimeException | LinkageError e) {
            backend = null;
            plugin.getLogger().log(Level.WARNING, "GUI: nie udało się włączyć ekranów, zostają skrzynie", e);
        }
    }

    public void stop() {
        if (backend != null) {
            try {
                backend.disable();
            } catch (RuntimeException | LinkageError e) {
                plugin.getLogger().log(Level.WARNING, "GUI: błąd przy wyłączaniu ekranów", e);
            }
            backend = null;
        }
    }

    /** Powód, dla którego gracz dostanie skrzynię, albo null, gdy dostanie ekran (o ile da się go posadzić). */
    public String fallbackReason(Player player) {
        if (backend == null) {
            return "na serwerze nie ma PacketEvents";
        }
        if (!enabled) {
            return "ekran jest wyłączony w configu (gui.shader)";
        }
        if (chestMode.contains(player.getUniqueId())) {
            return "wybrałeś tryb skrzyni";
        }
        if (requirePack && !packLoaded.contains(player.getUniqueId())) {
            return "nie masz wczytanej paczki zasobów serwera";
        }
        return null;
    }

    /**
     * Otwiera ekran (albo podmienia otwarty). False = otwórz zwykłą skrzynię: ekranu nie ma, gracz go nie chce
     * albo nie da się go teraz posadzić.
     */
    public boolean open(Player player, GuiScreen screen) {
        if (backend == null) {
            return false;
        }
        if (!backend.isOpen(player) && fallbackReason(player) != null) {
            return false;
        }
        try {
            return backend.open(player, screen);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "GUI: nie udało się otworzyć ekranu, będzie skrzynia", e);
            return false;
        }
    }

    public boolean isOpen(Player player) {
        return backend != null && backend.isOpen(player);
    }

    /** Otwarty ekran gracza albo null. */
    public GuiScreen screen(Player player) {
        return backend == null ? null : backend.screen(player);
    }

    public void close(Player player) {
        closeIf(player, screen -> true);
    }

    /** Zamyka ekran gracza, jeśli to ten (np. ekran wymiany, która się skończyła). */
    public void closeIf(Player player, Predicate<GuiScreen> which) {
        if (backend != null) {
            backend.closeIf(player, which);
        }
    }

    public boolean chestMode(Player player) {
        return chestMode.contains(player.getUniqueId());
    }

    /** Przełącza wybór gracza (ekran albo skrzynia); zwraca true, gdy od teraz woli skrzynię. */
    public boolean toggleChestMode(Player player) {
        boolean chest = !chestMode.remove(player.getUniqueId());
        if (chest) {
            chestMode.add(player.getUniqueId());
        }
        List<String> ids = new ArrayList<>();
        for (UUID id : chestMode) {
            ids.add(id.toString());
        }
        data.set("tryb-skrzyni", ids);
        save();
        return chest;
    }

    // ------------------------------------------------------------------ dla silnika

    /** Numery map ekranu z gui.yml (stałe, żeby po restarcie nie tworzyć nowych). */
    List<Integer> mapIds() {
        return data.getIntegerList("mapy");
    }

    void saveMapIds(List<Integer> ids) {
        data.set("mapy", ids);
        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się zapisać " + file.getName() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ paczka zasobów

    @EventHandler
    public void onPack(PlayerResourcePackStatusEvent event) {
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            packLoaded.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        packLoaded.remove(event.getPlayer().getUniqueId());
    }
}
