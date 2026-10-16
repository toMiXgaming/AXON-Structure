package pl.axon.structure.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tekstowy format pliku struktury ({@code .axs}).
 *
 * <pre>
 * # AXON-Structure v1
 * id: stacja
 * nazwa: Opuszczona stacja benzynowa
 * opis: ...
 * rozmiar: 45 16 52          (szerokość wysokość głębokość)
 * paleta:
 * 0 minecraft:air
 * 1 minecraft:stone
 * warstwy:
 * 0 0: 1*45                  (y z: kolejne bloki wzdłuż x, "indeks*ile")
 * skrzynie:
 * 16 1 8 north paliwo        (x y z kierunek pula)
 * ramki:
 * 11 2 24 up karta
 * napisy:
 * 1 4 4|OXUM'S|GAS||
 * </pre>
 */
public final class StructureFile {

    public static final String EXTENSION = ".axs";
    private static final String HEADER = "# AXON-Structure v1";

    private StructureFile() {
    }

    public static void write(Blueprint bp, Writer out) throws IOException {
        List<String> palette = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        palette.add(Blueprint.AIR);
        index.put(Blueprint.AIR, 0);
        for (int y = 0; y < bp.height(); y++) {
            for (int z = 0; z < bp.depth(); z++) {
                for (int x = 0; x < bp.width(); x++) {
                    String state = bp.get(x, y, z);
                    if (!index.containsKey(state)) {
                        index.put(state, palette.size());
                        palette.add(state);
                    }
                }
            }
        }

        out.write(HEADER + "\n");
        out.write("id: " + bp.id() + "\n");
        out.write("nazwa: " + bp.name() + "\n");
        out.write("opis: " + bp.description() + "\n");
        out.write("rozmiar: " + bp.width() + " " + bp.height() + " " + bp.depth() + "\n");
        out.write("paleta:\n");
        for (int i = 0; i < palette.size(); i++) {
            out.write(i + " " + palette.get(i) + "\n");
        }
        out.write("warstwy:\n");
        for (int y = 0; y < bp.height(); y++) {
            for (int z = 0; z < bp.depth(); z++) {
                StringBuilder row = new StringBuilder();
                int x = 0;
                boolean allAir = true;
                while (x < bp.width()) {
                    int current = index.get(bp.get(x, y, z));
                    int run = 1;
                    while (x + run < bp.width() && index.get(bp.get(x + run, y, z)) == current) {
                        run++;
                    }
                    if (current != 0) {
                        allAir = false;
                    }
                    if (!row.isEmpty()) {
                        row.append(' ');
                    }
                    row.append(current);
                    if (run > 1) {
                        row.append('*').append(run);
                    }
                    x += run;
                }
                if (!allAir) {
                    out.write(y + " " + z + ": " + row + "\n");
                }
            }
        }
        out.write("skrzynie:\n");
        for (Blueprint.SpawnPoint p : bp.spawnPoints()) {
            if (p.kind() == Blueprint.SpawnKind.CRATE) {
                out.write(p.pos().x() + " " + p.pos().y() + " " + p.pos().z() + " " + p.facing() + " " + p.pool() + "\n");
            }
        }
        out.write("ramki:\n");
        for (Blueprint.SpawnPoint p : bp.spawnPoints()) {
            if (p.kind() == Blueprint.SpawnKind.FRAME) {
                out.write(p.pos().x() + " " + p.pos().y() + " " + p.pos().z() + " " + p.facing() + " " + p.pool() + "\n");
            }
        }
        out.write("napisy:\n");
        for (Blueprint.SignSpec sign : bp.signs()) {
            out.write(sign.pos().x() + " " + sign.pos().y() + " " + sign.pos().z()
                    + "|" + String.join("|", sign.lines()) + "\n");
        }
    }

    public static Blueprint read(Reader in) throws IOException {
        BufferedReader reader = in instanceof BufferedReader b ? b : new BufferedReader(in);
        Map<String, String> meta = new HashMap<>();
        List<String> palette = new ArrayList<>();
        Blueprint bp = null;
        String section = "";
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            try {
                if (line.endsWith(":") && !line.contains(" ")) {
                    section = line.substring(0, line.length() - 1);
                    if (section.equals("paleta") || section.equals("warstwy")) {
                        bp = ensureBlueprint(bp, meta);
                    }
                    continue;
                }
                switch (section) {
                    case "" -> {
                        int colon = line.indexOf(':');
                        if (colon < 0) {
                            throw new IOException("oczekiwano 'klucz: wartość'");
                        }
                        meta.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                    }
                    case "paleta" -> {
                        String[] parts = line.trim().split(" ", 2);
                        int idx = Integer.parseInt(parts[0]);
                        if (idx != palette.size()) {
                            throw new IOException("indeksy palety muszą iść po kolei od 0");
                        }
                        palette.add(Blueprint.normalize(parts[1].trim()));
                    }
                    case "warstwy" -> readRow(bp, palette, line);
                    case "skrzynie", "ramki" -> {
                        String[] p = line.trim().split("\\s+");
                        if (p.length != 5) {
                            throw new IOException("oczekiwano 'x y z kierunek pula'");
                        }
                        int x = Integer.parseInt(p[0]);
                        int y = Integer.parseInt(p[1]);
                        int z = Integer.parseInt(p[2]);
                        if (section.equals("skrzynie")) {
                            bp.crate(x, y, z, p[3], p[4]);
                        } else {
                            bp.frame(x, y, z, p[3], p[4]);
                        }
                    }
                    case "napisy" -> {
                        String[] p = line.split("\\|", -1);
                        String[] xyz = p[0].trim().split("\\s+");
                        String[] lines = new String[Math.min(4, p.length - 1)];
                        System.arraycopy(p, 1, lines, 0, lines.length);
                        int x = Integer.parseInt(xyz[0]);
                        int y = Integer.parseInt(xyz[1]);
                        int z = Integer.parseInt(xyz[2]);
                        bp.sign(x, y, z, bp.get(x, y, z), lines);
                    }
                    default -> throw new IOException("nieznana sekcja '" + section + "'");
                }
            } catch (IOException | RuntimeException e) {
                throw new IOException("linia " + lineNo + ": " + e.getMessage(), e);
            }
        }
        return ensureBlueprint(bp, meta);
    }

    private static Blueprint ensureBlueprint(Blueprint bp, Map<String, String> meta) throws IOException {
        if (bp != null) {
            return bp;
        }
        String size = meta.get("rozmiar");
        if (size == null || !meta.containsKey("id")) {
            throw new IOException("brak 'id' albo 'rozmiar' w nagłówku");
        }
        String[] s = size.trim().split("\\s+");
        Blueprint created = new Blueprint(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        created.meta(meta.get("id"), meta.getOrDefault("nazwa", meta.get("id")), meta.getOrDefault("opis", ""));
        return created;
    }

    private static void readRow(Blueprint bp, List<String> palette, String line) throws IOException {
        int colon = line.indexOf(':');
        String[] yz = line.substring(0, colon).trim().split("\\s+");
        int y = Integer.parseInt(yz[0]);
        int z = Integer.parseInt(yz[1]);
        int x = 0;
        for (String token : line.substring(colon + 1).trim().split("\\s+")) {
            int star = token.indexOf('*');
            int idx = Integer.parseInt(star < 0 ? token : token.substring(0, star));
            int count = star < 0 ? 1 : Integer.parseInt(token.substring(star + 1));
            if (idx < 0 || idx >= palette.size()) {
                throw new IOException("indeks " + idx + " spoza palety");
            }
            for (int i = 0; i < count; i++) {
                bp.set(x++, y, z, palette.get(idx));
            }
        }
        if (x != bp.width()) {
            throw new IOException("wiersz ma " + x + " bloków zamiast " + bp.width());
        }
    }
}
