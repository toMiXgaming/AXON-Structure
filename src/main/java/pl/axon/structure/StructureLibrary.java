package pl.axon.structure;

import pl.axon.structure.api.StructureTemplate;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureFile;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Struktury wczytane z plików {@code .axs} w folderze {@code struktury/}. */
public final class StructureLibrary {

    private final File folder;
    private final Logger logger;
    private final Map<String, Blueprint> structures = new LinkedHashMap<>();
    /** Niemutowalne kopie do odczytu z innych wątków (generator chunków). Podmieniane w całości przy reload. */
    private volatile Map<String, StructureTemplate> templates = Map.of();

    public StructureLibrary(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public File folder() {
        return folder;
    }

    public void reload() {
        structures.clear();
        loadFiles();
        Map<String, StructureTemplate> snapshot = new LinkedHashMap<>();
        for (Blueprint bp : structures.values()) {
            snapshot.put(bp.id().toLowerCase(Locale.ROOT), StructureTemplate.snapshot(bp));
        }
        templates = Collections.unmodifiableMap(snapshot);
    }

    private void loadFiles() {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            logger.warning("Nie udało się utworzyć folderu " + folder);
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(StructureFile.EXTENSION));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                Blueprint bp = StructureFile.read(reader);
                structures.put(bp.id().toLowerCase(Locale.ROOT), bp);
                logger.info("Wczytano strukturę '" + bp.id() + "' z " + file.getName() + " ("
                        + bp.width() + "×" + bp.depth() + "×" + bp.height() + ", "
                        + bp.spawnPoints().size() + " miejsc na skrzynie)");
            } catch (IOException e) {
                logger.severe("Błąd w pliku " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public Blueprint get(String id) {
        return structures.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Blueprint> all() {
        return structures.values();
    }

    /** Niemutowalne szablony; bezpieczne do odczytu z dowolnego wątku. */
    public Collection<StructureTemplate> templates() {
        return templates.values();
    }

    public StructureTemplate template(String id) {
        return templates.get(id.toLowerCase(Locale.ROOT));
    }
}
