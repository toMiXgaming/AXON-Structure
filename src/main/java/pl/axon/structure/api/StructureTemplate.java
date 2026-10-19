package pl.axon.structure.api;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Niemutowalna kopia szablonu struktury. Można ją czytać z dowolnego wątku, także z generatora chunków.
 * <p>
 * Bloki są w tablicy: indeks komórki = {@code (y * depth + z) * width + x}, a wartość 0 oznacza powietrze.
 * Współrzędne są lokalne (układ szablonu, przed obrotem).
 */
public final class StructureTemplate {

    private final String id;
    private final String name;
    private final int width;
    private final int height;
    private final int depth;
    private final int groundLevel;
    private final String[] palette;
    private final short[] cells;
    private final List<Entrance> entrances;
    private final Map<String, String> tags;
    private final SizeClass sizeClass;
    private final boolean mapAllowed;
    private final Set<String> zones;
    private final Set<String> climates;
    private final boolean safe;

    private StructureTemplate(Blueprint bp) {
        Traits traits = bp.traits();
        this.id = bp.id();
        this.name = bp.name();
        this.width = bp.width();
        this.height = bp.height();
        this.depth = bp.depth();
        this.groundLevel = traits.groundLevel();

        List<String> paletteList = new ArrayList<>();
        paletteList.add(null); // 0 = powietrze
        Map<String, Integer> index = new HashMap<>();
        this.cells = new short[width * height * depth];
        for (Map.Entry<Blueprint.Pos, String> entry : bp.blocks().entrySet()) {
            Blueprint.Pos pos = entry.getKey();
            Integer code = index.get(entry.getValue());
            if (code == null) {
                if (paletteList.size() > Short.MAX_VALUE) {
                    throw new IllegalStateException("Za dużo różnych bloków w szablonie '" + id + "'");
                }
                code = paletteList.size();
                index.put(entry.getValue(), code);
                paletteList.add(entry.getValue());
            }
            cells[(pos.y() * depth + pos.z()) * width + pos.x()] = code.shortValue();
        }
        this.palette = paletteList.toArray(new String[0]);

        this.entrances = List.copyOf(traits.entrances());
        this.tags = traits.tags();
        this.sizeClass = traits.sizeClass() != null ? traits.sizeClass() : SizeClass.fromSize(width, depth);
        this.mapAllowed = traits.mapAllowed();
        this.zones = traits.zones();
        this.climates = traits.climates();
        this.safe = traits.safe();
    }

    /**
     * Tworzy kopię szablonu. Wywołuje to AXON-Structure przy wczytywaniu plików; inne pluginy tego nie robią.
     */
    public static StructureTemplate snapshot(Blueprint bp) {
        return new StructureTemplate(bp);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    /** Wysokość poziomu terenu wewnątrz szablonu ({@code grunt} w nagłówku, domyślnie 0). */
    public int groundLevel() {
        return groundLevel;
    }

    /** Stan bloku (np. {@code minecraft:oak_stairs[facing=east]}) albo null dla powietrza lub poza szablonem. */
    public String blockAt(int lx, int ly, int lz) {
        if (lx < 0 || lx >= width || ly < 0 || ly >= height || lz < 0 || lz >= depth) {
            return null;
        }
        return palette[cells[(ly * depth + lz) * width + lx]];
    }

    public List<Entrance> entrances() {
        return entrances;
    }

    /** Pozostałe pola nagłówka, których AS nie zna. */
    public Map<String, String> tags() {
        return tags;
    }

    public SizeClass sizeClass() {
        return sizeClass;
    }

    /** Czy generator może stawiać tę strukturę ({@code mapa: tak|nie}). */
    public boolean mapAllowed() {
        return mapAllowed;
    }

    public Set<String> zones() {
        return zones;
    }

    public Set<String> climates() {
        return climates;
    }

    /** Strefa bez PvP ({@code bezpieczna: tak}). */
    public boolean safe() {
        return safe;
    }
}
