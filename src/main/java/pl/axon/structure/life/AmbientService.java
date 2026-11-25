package pl.axon.structure.life;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.StructureInstance;
import pl.axon.structure.world.InstanceStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Odgłosy w strukturach: gdy gracz jest w obrysie struktury, co jakiś czas słyszy (tylko on) pojedynczy dźwięk
 * z listy dla tej struktury – wiatr i skrzypienie w ruinach, trzaski radia w jednostce, szum agregatu i gwar
 * w oazie. Jeden zegar na cały serwer i tylko gracze, którzy są w strukturach, więc prawie nic nie kosztuje.
 */
public final class AmbientService {

    /** Dźwięk z configu: nazwa z Minecrafta (albo z paczki zasobów), wysokość i mnożnik głośności. */
    public record SoundSpec(String key, float pitch, float volume) {

        /** "minecraft:ambient.cave 0.8 1.2" – nazwa, opcjonalnie wysokość i głośność. */
        public static SoundSpec parse(String line) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isBlank()) {
                return null;
            }
            try {
                float pitch = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
                float volume = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
                String key = parts[0].toLowerCase(Locale.ROOT);
                return new SoundSpec(key.contains(":") ? key : "minecraft:" + key, pitch, volume);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public static final String DEFAULT_KEY = "domyslne";

    /**
     * @param intervalSeconds co ile sekund losować odgłos dla każdego gracza w strukturze
     * @param chancePercent   szansa, że w danym losowaniu coś zabrzmi
     * @param sounds          id struktury (albo {@link #DEFAULT_KEY}) → odgłosy; pusta lista = cisza
     */
    public record Settings(boolean enabled, int intervalSeconds, double chancePercent, float volume,
                           Map<String, List<SoundSpec>> sounds) {
    }

    private final JavaPlugin plugin;
    private final InstanceStore store;
    private final Random random = new Random();
    private Settings settings;
    private BukkitTask task;
    private boolean started;

    public AmbientService(JavaPlugin plugin, InstanceStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void configure(Settings settings) {
        boolean intervalChanged = this.settings == null || this.settings.intervalSeconds() != settings.intervalSeconds();
        this.settings = settings;
        if (started && intervalChanged) {
            start();
        }
    }

    public Settings settings() {
        return settings;
    }

    public void start() {
        stopTimer();
        long period = 20L * Math.max(2, settings.intervalSeconds());
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
        started = true;
    }

    public void stop() {
        stopTimer();
        started = false;
    }

    private void stopTimer() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!settings.enabled() || settings.chancePercent() <= 0) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            List<Player> players = world.getPlayers();
            if (players.isEmpty()) {
                continue;
            }
            List<StructureInstance> instances = store.inWorld(world.getName());
            if (instances.isEmpty()) {
                continue;
            }
            for (Player player : players) {
                if (random.nextDouble() * 100.0 >= settings.chancePercent()) {
                    continue;
                }
                StructureInstance inside = structureAt(instances, player.getLocation());
                if (inside != null) {
                    play(player, inside);
                }
            }
        }
    }

    private static StructureInstance structureAt(List<StructureInstance> instances, Location location) {
        for (StructureInstance instance : instances) {
            Box box = instance.footprint();
            if (box != null && box.contains(location.getBlockX(), location.getBlockZ())) {
                return instance;
            }
        }
        return null;
    }

    private void play(Player player, StructureInstance instance) {
        List<SoundSpec> list = settings.sounds().get(instance.structureId().toLowerCase(Locale.ROOT));
        if (list == null) {
            list = settings.sounds().get(DEFAULT_KEY);
        }
        if (list == null || list.isEmpty()) {
            return;
        }
        SoundSpec sound = list.get(random.nextInt(list.size()));
        // gdzieś obok gracza (4-10 bloków), żeby dźwięk miał kierunek, a nie grał „w głowie”
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 4 + random.nextDouble() * 6;
        Location at = player.getLocation().add(Math.cos(angle) * distance, random.nextDouble() * 4 - 1,
                Math.sin(angle) * distance);
        float pitch = sound.pitch() * (0.9f + random.nextFloat() * 0.2f);
        player.playSound(at, sound.key(), SoundCategory.AMBIENT, settings.volume() * sound.volume(), pitch);
    }

    /** Ile struktur ma własne odgłosy (do komendy). */
    public List<String> configuredStructures() {
        List<String> out = new ArrayList<>(settings.sounds().keySet());
        out.remove(DEFAULT_KEY);
        return out;
    }
}
