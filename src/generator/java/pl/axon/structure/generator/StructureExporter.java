package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.StructureFile;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * Zapisuje struktury zdefiniowane w kodzie do plików {@code .axs}.
 * Uruchomienie: {@code ./gradlew generujStruktury} (pliki trafią do build/struktury).
 */
public final class StructureExporter {

    private static final List<StructureDefinition> STRUCTURES = List.of(new GasStationStructure());

    private StructureExporter() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "build/struktury");
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 1L;
        Files.createDirectories(out);
        for (StructureDefinition definition : STRUCTURES) {
            Blueprint bp = definition.build(new Random(seed))
                    .meta(definition.id(), definition.displayName(), definition.description());
            Path file = out.resolve(definition.id() + StructureFile.EXTENSION);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                StructureFile.write(bp, writer);
            }
            System.out.println("Zapisano " + file + " (" + Files.size(file) / 1024 + " KB)");
        }
    }
}
