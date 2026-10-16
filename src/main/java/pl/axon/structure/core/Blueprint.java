package pl.axon.structure.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Struktura zapisana w lokalnych współrzędnych, zanim trafi do świata.
 * <p>
 * Układ lokalny: x rośnie w prawo (wschód), z rośnie w głąb struktury (południe),
 * y = 0 to warstwa gruntu. Front struktury jest po stronie z = 0 (północ).
 * Każda pozycja w obrębie prostopadłościanu, która nie została ustawiona, zostanie wyczyszczona do powietrza.
 * <p>
 * Klasa nie zależy od API Bukkita, więc korzysta z niej też generator plików struktur.
 */
public final class Blueprint {

    public static final String AIR = "minecraft:air";

    public record Pos(int x, int y, int z) {
    }

    public enum SpawnKind {
        /** Skrzynia albo beczka – blok zależy od wylosowanej klasy. */
        CRATE,
        /** Ramka z przedmiotem, np. karta dostępu. */
        FRAME
    }

    /** Miejsce, w którym przy odświeżaniu losowana jest skrzynia z podanej puli. */
    public record SpawnPoint(Pos pos, SpawnKind kind, String facing, String pool) {
    }

    public record SignSpec(Pos pos, String[] lines) {
    }

    private String id = "struktura";
    private String name = "Struktura";
    private String description = "";
    private final int width;
    private final int height;
    private final int depth;
    private final Map<Pos, String> blocks = new HashMap<>();
    private final List<SpawnPoint> spawnPoints = new ArrayList<>();
    private final List<SignSpec> signs = new ArrayList<>();

    public Blueprint(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Blueprint meta(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        return this;
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

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
    }

    public void set(int x, int y, int z, String state) {
        if (!inBounds(x, y, z)) {
            return;
        }
        String normalized = normalize(state);
        if (normalized.equals(AIR)) {
            blocks.remove(new Pos(x, y, z));
        } else {
            blocks.put(new Pos(x, y, z), normalized);
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Palette palette) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, palette.next());
                }
            }
        }
    }

    /** Zwraca stan bloku albo {@code minecraft:air}, jeśli pozycja nie została ustawiona. */
    public String get(int x, int y, int z) {
        return blocks.getOrDefault(new Pos(x, y, z), AIR);
    }

    public boolean isAir(int x, int y, int z) {
        return get(x, y, z).equals(AIR);
    }

    /** Miejsce na skrzynię. Sam blok zostaje pusty – stawia go odświeżanie skrzyń. */
    public void crate(int x, int y, int z, String facing, String pool) {
        set(x, y, z, AIR);
        spawnPoints.removeIf(p -> p.pos().equals(new Pos(x, y, z)));
        spawnPoints.add(new SpawnPoint(new Pos(x, y, z), SpawnKind.CRATE, facing, pool));
    }

    /** Miejsce na ramkę z przedmiotem przyczepioną do bloku po stronie przeciwnej do {@code facing}. */
    public void frame(int x, int y, int z, String facing, String pool) {
        spawnPoints.add(new SpawnPoint(new Pos(x, y, z), SpawnKind.FRAME, facing, pool));
    }

    public void sign(int x, int y, int z, String state, String... lines) {
        set(x, y, z, state);
        String[] padded = new String[4];
        for (int i = 0; i < 4; i++) {
            padded[i] = i < lines.length ? lines[i] : "";
        }
        signs.add(new SignSpec(new Pos(x, y, z), padded));
    }

    public Map<Pos, String> blocks() {
        return blocks;
    }

    public List<SpawnPoint> spawnPoints() {
        return spawnPoints;
    }

    public List<SignSpec> signs() {
        return signs;
    }

    static String normalize(String state) {
        return state.contains(":") ? state : "minecraft:" + state;
    }
}
