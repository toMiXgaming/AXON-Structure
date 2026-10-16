package pl.axon.structure.core;

import org.bukkit.inventory.ItemStack;

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
 */
public final class Blueprint {

    public record Pos(int x, int y, int z) {
    }

    public record ContainerSpec(Pos pos, LootType loot) {
    }

    public record SignSpec(Pos pos, String[] lines) {
    }

    public record ItemFrameSpec(Pos pos, String facing, ItemStack item, ContainerSpec fallback) {
    }

    private final int width;
    private final int height;
    private final int depth;
    private final Map<Pos, String> blocks = new HashMap<>();
    private final List<ContainerSpec> containers = new ArrayList<>();
    private final List<SignSpec> signs = new ArrayList<>();
    private final List<ItemFrameSpec> itemFrames = new ArrayList<>();

    public Blueprint(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
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
        blocks.put(new Pos(x, y, z), normalize(state));
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
        return blocks.getOrDefault(new Pos(x, y, z), "minecraft:air");
    }

    public boolean isAir(int x, int y, int z) {
        return get(x, y, z).equals("minecraft:air");
    }

    public void container(int x, int y, int z, String state, LootType loot) {
        set(x, y, z, state);
        containers.add(new ContainerSpec(new Pos(x, y, z), loot));
    }

    public void sign(int x, int y, int z, String state, String... lines) {
        set(x, y, z, state);
        String[] padded = new String[4];
        for (int i = 0; i < 4; i++) {
            padded[i] = i < lines.length ? lines[i] : "";
        }
        signs.add(new SignSpec(new Pos(x, y, z), padded));
    }

    /**
     * Ramka z przedmiotem. Jeśli serwer nie pozwoli jej postawić, przedmiot trafi do kontenera {@code fallback}.
     */
    public void itemFrame(int x, int y, int z, String facing, ItemStack item, ContainerSpec fallback) {
        itemFrames.add(new ItemFrameSpec(new Pos(x, y, z), facing, item, fallback));
    }

    public List<ContainerSpec> containers() {
        return containers;
    }

    public List<SignSpec> signs() {
        return signs;
    }

    public List<ItemFrameSpec> itemFrames() {
        return itemFrames;
    }

    static String normalize(String state) {
        return state.contains(":") ? state : "minecraft:" + state;
    }
}
