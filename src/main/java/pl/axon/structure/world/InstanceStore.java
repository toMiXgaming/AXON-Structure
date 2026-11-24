package pl.axon.structure.world;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.axon.structure.core.Box;
import pl.axon.structure.core.Placement;
import pl.axon.structure.core.StructureInstance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Lista postawionych struktur zapisywana w {@code dane.yml}. */
public final class InstanceStore {

    private final File file;
    private final Logger logger;
    private final Map<String, StructureInstance> instances = new LinkedHashMap<>();
    private final java.util.concurrent.ExecutorService writer = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AxonStructure-IO");
        t.setDaemon(true);
        return t;
    });

    public InstanceStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public void load() {
        instances.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("instancje");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            Placement placement = new Placement(s.getString("swiat", "world"),
                    s.getInt("x"), s.getInt("y"), s.getInt("z"), s.getInt("obrot"));
            StructureInstance instance = new StructureInstance(id, s.getString("struktura", "?"), placement,
                    s.getInt("srodek-x"), s.getInt("srodek-z"), s.getBoolean("auto"), s.getLong("odswiezono"));
            List<Integer> area = s.getIntegerList("obrys");
            if (area.size() == 4) {
                instance.footprint(new Box(area.get(0), area.get(1), area.get(2), area.get(3)));
            }
            instance.externalKey(s.getString("klucz"));
            instances.put(id, instance);
        }
    }

    /** Zapis w tle: tekst powstaje na wątku wołającego (stan nie zmienia się w trakcie), na dysk idzie osobny wątek. */
    public void save() {
        String data = serialize();
        try {
            writer.execute(() -> write(data));
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            write(data);
        }
    }

    /** Kończy zapisy w tle i zapisuje stan ostatni raz (wyłączenie pluginu). */
    public void close() {
        writer.shutdown();
        try {
            writer.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        write(serialize());
    }

    private String serialize() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (StructureInstance instance : instances.values()) {
            String path = "instancje." + instance.id() + ".";
            Placement p = instance.placement();
            yaml.set(path + "struktura", instance.structureId());
            yaml.set(path + "swiat", p.world());
            yaml.set(path + "x", p.originX());
            yaml.set(path + "y", p.originY());
            yaml.set(path + "z", p.originZ());
            yaml.set(path + "obrot", p.steps());
            yaml.set(path + "srodek-x", instance.centerX());
            yaml.set(path + "srodek-z", instance.centerZ());
            yaml.set(path + "auto", instance.automatic());
            yaml.set(path + "odswiezono", instance.lastRefresh());
            Box box = instance.footprint();
            if (box != null) {
                yaml.set(path + "obrys", List.of(box.minX(), box.minZ(), box.maxX(), box.maxZ()));
            }
            if (instance.externalKey() != null) {
                yaml.set(path + "klucz", instance.externalKey());
            }
        }
        return yaml.saveToString();
    }

    private synchronized void write(String data) {
        try {
            java.nio.file.Path target = file.toPath();
            java.nio.file.Path tmp = target.resolveSibling(file.getName() + ".tmp");
            java.nio.file.Files.writeString(tmp, data, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.severe("Nie udało się zapisać " + file.getName() + ": " + e.getMessage());
        }
    }

    public void add(StructureInstance instance) {
        instances.put(instance.id(), instance);
        save();
    }

    public void remove(String id) {
        if (instances.remove(id) != null) {
            save();
        }
    }

    public StructureInstance get(String id) {
        return instances.get(id);
    }

    /** Instancja zarejestrowana z podanym kluczem zewnętrznym albo null. */
    public StructureInstance findByKey(String key) {
        for (StructureInstance instance : instances.values()) {
            if (key.equals(instance.externalKey())) {
                return instance;
            }
        }
        return null;
    }

    public Collection<StructureInstance> all() {
        return instances.values();
    }

    public List<StructureInstance> inWorld(String world) {
        List<StructureInstance> result = new ArrayList<>();
        for (StructureInstance instance : instances.values()) {
            if (instance.world().equals(world)) {
                result.add(instance);
            }
        }
        return result;
    }

    public StructureInstance nearest(String world, double x, double z) {
        StructureInstance best = null;
        double bestDistance = Double.MAX_VALUE;
        for (StructureInstance instance : inWorld(world)) {
            double d = instance.distanceSquared(x, z);
            if (d < bestDistance) {
                bestDistance = d;
                best = instance;
            }
        }
        return best;
    }
}
