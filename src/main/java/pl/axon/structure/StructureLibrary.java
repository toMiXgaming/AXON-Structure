package pl.axon.structure;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureFile;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Struktury wczytane z plików {@code .axs} w folderze {@code struktury/}. */
public final class StructureLibrary {

    private final File folder;
    private final Logger logger;
    private final Map<String, Blueprint> structures = new LinkedHashMap<>();

    public StructureLibrary(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public File folder() {
        return folder;
    }

    public void reload() {
        structures.clear();
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
                        + bp.spawnPoints().size() + " punktów)");
            } catch (IOException | RuntimeException e) {
                // zły plik nie może zablokować wczytania reszty struktur
                logger.severe("Błąd w pliku " + file.getName() + ": " + e);
            }
        }
    }

    public Blueprint get(String id) {
        return structures.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Blueprint> all() {
        return structures.values();
    }
}
