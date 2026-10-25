package pl.axon.structure.generator;

import pl.axon.structure.api.Entrance;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;
import pl.axon.structure.core.Traits;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Osiedle Słoneczne: trzy bloki z wielkiej płyty wokół podwórka, parking z wrakami aut, rząd blaszanych garaży
 * i altana śmietnikowa. Średnia struktura z gęstym, ale słabszym lootem w mieszkaniach i lepszym na dachach
 * oraz w odciętej klatce zawalonego bloku.
 * <p>
 * Plan w lokalnych współrzędnych (y względem gruntu, piwnica pod blokiem 3 y -5..-1):
 * <pre>
 *  z 0-3     chodnik od ulicy, wjazd na parking x 16-23 (wjazd z drogi: 19 0 north)
 *  x 2-40    parking z 4-31: wraki aut, budka parkingowa, wózek sklepowy
 *  x 46-82   blok 3 (z 4-15): 4 piętra, dwie klatki, sklep na parterze, piwnica z komórkami
 *  x 44-84   podwórko z 18-55: trzepak, piaskownica, huśtawka, zjeżdżalnia, ławki, drzewa
 *  x 30-37   altana śmietnikowa z 33-37
 *  x 6-24    blok 5 (z 40-51): 3 piętra, jedna klatka
 *  x 0-44    uliczka do garaży z 60-64 (wjazd z drogi: 0 62 west)
 *  x 46-82   blok 4 (z 58-69): 4 piętra, wschodni koniec zawalony, druga klatka odcięta od 2 piętra
 *  x 4-39    rząd sześciu garaży z 66-73
 * </pre>
 * Klatka schodowa: dwa pasy schodów (x c i c + 1) w ścianach x c - 1 i c + 2, podest północny z 1-4
 * i południowy z 9-10 (licząc od ściany północnej bloku). Parzyste biegi idą na północ pasem c,
 * nieparzyste na południe pasem c + 1, więc na piętra nieparzyste wchodzi się od północy, a na parzyste od południa.
 */
public final class HousingEstateStructure implements StructureDefinition {

    private static final int WIDTH = 86;
    private static final int DEPTH = 76;
    private static final int G = 5;
    private static final int HEIGHT = G + 22;
    /** Wysokość kondygnacji: strop w y = 4 * piętro, chodzi się po y + 1. */
    private static final int STOREY = 4;
    /** Szerokość segmentu z jedną klatką (bez wspólnej ściany). */
    private static final int SEGMENT = 18;
    private static final int BLOCK_DEPTH = 12;

    @Override
    public String id() {
        return "osiedle";
    }

    @Override
    public String displayName() {
        return "Osiedle Słoneczne";
    }

    @Override
    public String description() {
        return "Trzy bloki z wielkiej płyty wokół podwórka, parking z wrakami, garaże, piwnice z komórkami i zawalona klatka";
    }

    @Override
    public Blueprint build(Random random) {
        Traits defaults = Traits.defaults();
        Traits traits = new Traits(List.of(new Entrance(19, 0, "north"), new Entrance(0, 62, "west")), true, null,
                new LinkedHashSet<>(List.of("rownina", "srodek")), defaults.climates(), false, Map.of());
        return new Builder(random).build().traits(traits);
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);
        private final Furniture furniture;
        private final Palette soil;
        private final Palette lawn;
        private final Palette asphalt;
        private final Palette panel;
        private final Palette basementWall;
        private final Palette rubble;
        private final Palette burnt;
        private final Palette car;
        private final Palette tin;

        Builder(Random rnd) {
            this.rnd = rnd;
            this.furniture = new Furniture(bp, G, rnd);
            soil = new Palette(rnd)
                    .add("dirt", 8).add("stone", 3).add("gravel", 1);
            lawn = new Palette(rnd)
                    .add("grass_block", 12).add("coarse_dirt", 3).add("dirt", 1).add("podzol", 1);
            asphalt = new Palette(rnd)
                    .add("black_concrete", 10).add("gray_concrete", 3).add("gravel", 1).add("cobblestone", 1);
            panel = new Palette(rnd)
                    .add("light_gray_concrete", 14).add("smooth_stone", 1);
            basementWall = new Palette(rnd)
                    .add("stone_bricks", 5).add("cobblestone", 3).add("andesite", 2);
            rubble = new Palette(rnd)
                    .add("cobblestone", 5).add("light_gray_concrete", 3).add("gravel", 2).add("andesite", 2)
                    .add("smooth_stone_slab[type=bottom,waterlogged=false]", 2);
            burnt = new Palette(rnd)
                    .add("black_concrete", 4).add("gray_concrete", 3).add("coal_block", 1).add("blackstone", 2);
            car = new Palette(rnd)
                    .add("red_terracotta", 3).add("white_terracotta", 2).add("light_blue_terracotta", 2)
                    .add("yellow_terracotta", 1).add("green_terracotta", 2).add("brown_terracotta", 2);
            tin = new Palette(rnd)
                    .add("light_gray_terracotta", 5).add("gray_terracotta", 3).add("brown_terracotta", 1);
        }

        Blueprint build() {
            bp.ground(G);
            ground();
            parking();
            garbageShelter();
            garages();
            int[] a = block(46, 4, 2, 4, false, true);
            basement(46, 4, a[0]);
            shop(46, 4);
            block(6, 40, 1, 3, false, false);
            block(46, 58, 2, 4, true, false);
            collapse(46, 58);
            courtyard();
            signs();
            // skrzynie, pod którymi zawalił się strop (wyrwa w bloku 4), znikają
            bp.spawnPoints().removeIf(p -> !p.pool().startsWith("punkt-") && bp.isAir(p.pos().x(), p.pos().y() - 1, p.pos().z()));
            return bp;
        }

        // ------------------------------------------------------------------ pomocnicze (y względem gruntu)

        private void set(int x, int y, int z, String state) {
            bp.set(x, y + G, z, state);
        }

        private String get(int x, int y, int z) {
            return bp.get(x, y + G, z);
        }

        private boolean isAir(int x, int y, int z) {
            return bp.isAir(x, y + G, z);
        }

        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
            bp.fill(x1, y1 + G, z1, x2, y2 + G, z2, state);
        }

        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Palette palette) {
            bp.fill(x1, y1 + G, z1, x2, y2 + G, z2, palette);
        }

        private void crate(int x, int y, int z, String facing, String pool) {
            bp.crate(x, y + G, z, facing, pool);
        }

        private void sign(int x, int y, int z, String state, String... lines) {
            bp.sign(x, y + G, z, state, lines);
        }

        private boolean isCrate(int x, int y, int z) {
            Blueprint.Pos pos = new Blueprint.Pos(x, y + G, z);
            return bp.spawnPoints().stream().anyMatch(p -> p.pos().equals(pos));
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        private <T> T pick(T[] options) {
            return options[rnd.nextInt(options.length)];
        }

        private void door(int x, int y, int z, String type, String facing, String hinge, boolean open) {
            String props = "facing=" + facing + ",hinge=" + hinge + ",open=" + open + ",powered=false";
            set(x, y, z, type + "[half=lower," + props + "]");
            set(x, y + 1, z, type + "[half=upper," + props + "]");
        }

        private static String stairs(String material, String facing) {
            return material + "_stairs[facing=" + facing + ",half=bottom,shape=straight,waterlogged=false]";
        }

        private String window() {
            double roll = rnd.nextDouble();
            if (roll < 0.3) {
                return "air";
            }
            if (roll < 0.36) {
                return "cobweb";
            }
            if (roll < 0.46) {
                return pick(new String[]{"spruce_planks", "oak_planks", "spruce_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]"});
            }
            return "glass_pane";
        }

        // ------------------------------------------------------------------ teren

        private void ground() {
            fill(0, -G, 0, WIDTH - 1, -1, DEPTH - 1, soil);
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    String block = lawn.next();
                    set(x, 0, z, block);
                    if (block.equals("grass_block") && chance(0.25)) {
                        set(x, 1, z, chance(0.85) ? "short_grass" : chance(0.5) ? "dandelion" : "fern");
                    }
                }
            }
            // chodnik od ulicy z płyt chodnikowych, krawężnik
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z <= 2; z++) {
                    set(x, 0, z, chance(0.12) ? "cracked_stone_bricks" : chance(0.1) ? "grass_block" : "stone_bricks");
                    set(x, 1, z, "air");
                }
                set(x, 0, 3, "smooth_stone");
                set(x, 1, 3, "air");
            }
            // uliczka do garaży i dojście do bloków
            for (int x = 0; x <= 44; x++) {
                for (int z = 60; z <= 64; z++) {
                    set(x, 0, z, asphalt.next());
                    set(x, 1, z, "air");
                }
            }
            path(40, 17, 85, 17);
            path(40, 18, 41, 59);
            path(42, 56, 85, 56);
            path(15, 32, 16, 39);
            path(25, 45, 39, 46);
            path(82, 70, 85, 75);
            for (int x = 46; x <= 82; x++) {
                for (int z = 70; z <= 71; z++) {
                    set(x, 0, z, chance(0.15) ? "cracked_stone_bricks" : "stone_bricks");
                    set(x, 1, z, "air");
                }
            }
        }

        private void path(int x1, int z1, int x2, int z2) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, 0, z, chance(0.15) ? "cracked_stone_bricks" : chance(0.08) ? "moss_block" : "stone_bricks");
                    set(x, 1, z, "air");
                }
            }
        }

        // ------------------------------------------------------------------ parking

        private void parking() {
            for (int x = 2; x <= 40; x++) {
                for (int z = 4; z <= 31; z++) {
                    set(x, 0, z, chance(0.04) ? "grass_block" : asphalt.next());
                    set(x, 1, z, chance(0.02) ? "short_grass" : "air");
                }
            }
            // wjazd z ulicy: obniżony krawężnik i podniesiony szlaban
            for (int x = 16; x <= 23; x++) {
                for (int z = 0; z <= 3; z++) {
                    set(x, 0, z, asphalt.next());
                }
            }
            fill(15, 1, 3, 15, 1, 3, "red_concrete");
            fill(15, 2, 3, 15, 4, 3, "white_concrete");
            set(15, 5, 3, "red_concrete");
            // budka parkingowa (wybita szyba)
            fill(11, 1, 4, 13, 3, 6, "white_terracotta");
            fill(12, 1, 5, 12, 2, 5, "air");
            set(13, 2, 5, "air");
            set(12, 2, 4, "glass_pane");
            set(11, 2, 5, "air");
            fill(10, 4, 3, 14, 4, 7, "smooth_stone_slab[type=bottom,waterlogged=false]");
            set(12, 1, 6, "air");
            set(12, 2, 6, "air");
            crate(12, 1, 5, "east", "plac");
            // krawężnik od strony bloków i żywopłot od ulicy
            for (int x = 2; x <= 40; x++) {
                if (x < 16 || x > 23) {
                    set(x, 1, 3, chance(0.75) ? "oak_leaves[distance=1,persistent=true,waterlogged=false]" : "air");
                }
            }
            for (int z = 4; z <= 31; z++) {
                set(1, 1, z, chance(0.8) ? "oak_leaves[distance=1,persistent=true,waterlogged=false]" : "air");
            }
            // linie miejsc parkingowych (dwa rzędy po prostej)
            for (int x = 4; x <= 38; x += 3) {
                for (int z = 7; z <= 10; z++) {
                    set(x, 0, z, "white_concrete");
                }
                for (int z = 24; z <= 27; z++) {
                    set(x, 0, z, "white_concrete");
                }
            }
            // wraki: rząd północny, rząd południowy i kilka porzuconych w alejce
            int[][] cars = {{5, 7, 1}, {11, 7, 1}, {20, 8, 1}, {26, 7, 1}, {35, 7, 1},
                    {8, 24, 1}, {14, 24, 1}, {29, 23, 1}, {32, 24, 1}, {18, 16, 0}, {30, 15, 0}};
            for (int[] c : cars) {
                double roll = rnd.nextDouble();
                if (roll < 0.15) {
                    burntCar(c[0], c[1], c[2] == 1);
                } else {
                    sedan(c[0], c[1], c[2] == 1, roll < 0.4);
                }
            }
            // wózek sklepowy, opony i kanister
            set(23, 1, 13, "lodestone");
            set(7, 1, 14, "black_concrete");
            set(7, 2, 14, "black_carpet");
            set(37, 1, 19, "respawn_anchor[charges=0]");
            crate(36, 1, 19, "west", "paliwo");
            crate(24, 1, 28, "north", "plac");
            crate(3, 1, 12, "east", "plac");
            crate(19, 1, 29, "north", "npc-szabrownik");
        }

        /** Osobówka 2 × 4 na parkingu (osłona), wzdłuż z albo x; czasem z otwartym bagażnikiem i beczką w środku. */
        private void sedan(int x, int z, boolean alongZ, boolean openTrunk) {
            String body = car.next();
            for (int i = 0; i < 4; i++) {
                for (int w = 0; w < 2; w++) {
                    int px = alongZ ? x + w : x + i;
                    int pz = alongZ ? z + i : z + w;
                    set(px, 1, pz, body);
                    if (i == 1) {
                        set(px, 2, pz, chance(0.4) ? "air" : "glass_pane");
                    } else if (i == 2) {
                        set(px, 2, pz, body);
                    }
                }
            }
            // zderzak od przodu
            set(x, 1, z, "polished_blackstone");
            set(alongZ ? x + 1 : x, 1, alongZ ? z : z + 1, "polished_blackstone");
            if (openTrunk) {
                int tx = alongZ ? x : x + 3;
                int tz = alongZ ? z + 3 : z;
                crate(tx, 1, tz, alongZ ? "south" : "east", "plac");
            }
        }

        /** Spalony wrak bez szyb, z sadzą dookoła. */
        private void burntCar(int x, int z, boolean alongZ) {
            for (int i = 0; i < 4; i++) {
                for (int w = 0; w < 2; w++) {
                    int px = alongZ ? x + w : x + i;
                    int pz = alongZ ? z + i : z + w;
                    set(px, 1, pz, burnt.next());
                    if (i == 2) {
                        set(px, 2, pz, "blackstone_slab[type=bottom,waterlogged=false]");
                    }
                }
            }
            for (int i = -1; i <= 4; i++) {
                int px = alongZ ? x - 1 : x + i;
                int pz = alongZ ? z + i : z - 1;
                if (chance(0.5)) {
                    set(px, 0, pz, "coal_block");
                }
            }
        }

        // ------------------------------------------------------------------ altana śmietnikowa

        private void garbageShelter() {
            for (int x = 30; x <= 37; x++) {
                for (int z = 33; z <= 37; z++) {
                    set(x, 0, z, "smooth_stone");
                }
            }
            for (int y = 1; y <= 3; y++) {
                fill(30, y, 33, 37, y, 33, "bricks");
                fill(30, y, 33, 30, y, 37, "bricks");
                fill(37, y, 33, 37, y, 37, "bricks");
            }
            fill(30, 4, 33, 37, 4, 37, "smooth_stone_slab[type=bottom,waterlogged=false]");
            fill(33, 4, 35, 34, 4, 36, "air");
            // kontenery: zielone skrzynie 2 × 1 z klapą i blaszane kosze
            for (int x : new int[]{31, 34}) {
                set(x, 1, 34, "green_terracotta");
                set(x + 1, 1, 34, "green_terracotta");
                set(x, 2, 34, "dark_oak_trapdoor[facing=south,half=bottom,open=false,powered=false,waterlogged=false]");
                set(x + 1, 2, 34, "dark_oak_trapdoor[facing=south,half=bottom,open=true,powered=false,waterlogged=false]");
            }
            set(36, 1, 34, "composter[level=0]");
            set(36, 1, 35, "composter[level=0]");
            crate(33, 1, 36, "south", "smietnik");
            crate(31, 1, 36, "south", "smietnik");
            // worki ze śmieciami dookoła
            for (int i = 0; i < 9; i++) {
                int x = 29 + rnd.nextInt(10);
                int z = 38 + rnd.nextInt(3);
                if (isAir(x, 1, z)) {
                    set(x, 1, z, pick(new String[]{"black_wool", "gray_wool", "black_carpet", "brown_carpet"}));
                }
            }
        }

        // ------------------------------------------------------------------ garaże

        private void garages() {
            int z1 = 66;
            int z2 = 73;
            for (int i = 0; i < 6; i++) {
                int x1 = 4 + i * 6;
                int x2 = x1 + 6;
                for (int x = x1; x <= x2; x++) {
                    for (int z = z1; z <= z2; z++) {
                        set(x, 0, z, "smooth_stone");
                        boolean wall = x == x1 || x == x2 || z == z2;
                        for (int y = 1; y <= 3; y++) {
                            set(x, y, z, wall ? tin.next() : "air");
                        }
                        set(x, 4, z, chance(0.08) ? "air" : "smooth_stone_slab[type=bottom,waterlogged=false]");
                    }
                }
                // brama: część otwarta, część zamknięta blachą
                boolean open = i != 2 && (i == 3 || chance(0.6));
                for (int x = x1 + 1; x <= x2 - 1; x++) {
                    for (int y = 1; y <= 3; y++) {
                        set(x, y, z1, open && y <= 2 ? "air" : "iron_block");
                    }
                }
                if (!open) {
                    // dziura w bramie wygięta łomem
                    set(x1 + 3, 1, z1, "air");
                }
                set(x1 + 3, 3, z1, "white_concrete");
                garageInside(i, x1, z1, open);
            }
        }

        private void garageInside(int i, int x1, int z1, boolean open) {
            int xi = x1 + 1;
            // regał na tylnej ścianie
            fill(xi, 1, z1 + 6, xi + 4, 1, z1 + 6, "spruce_slab[type=top,waterlogged=false]");
            switch (i) {
                case 0 -> {
                    sedan(xi + 1, z1 + 1, true, false);
                    crate(xi + 4, 1, z1 + 5, "west", "garaz");
                }
                case 1 -> {
                    set(xi, 1, z1 + 2, "crafting_table");
                    set(xi, 1, z1 + 3, "grindstone[face=floor,facing=east]");
                    crate(xi + 3, 1, z1 + 5, "north", "garaz");
                    set(xi + 4, 1, z1 + 5, "respawn_anchor[charges=0]");
                }
                case 2 -> {
                    // zamknięty na kłódkę: najlepszy garaż, wchodzi się przez dziurę w bramie
                    crate(xi + 1, 1, z1 + 5, "north", "garaz");
                    crate(xi + 3, 1, z1 + 5, "north", "garaz");
                    set(xi + 4, 1, z1 + 2, "respawn_anchor[charges=0]");
                    set(xi + 4, 1, z1 + 3, "respawn_anchor[charges=0]");
                }
                case 3 -> {
                    // garaż okradziony z kanistrów: puste miejsca po nich i kartka na bramie
                    fill(xi, 1, z1 + 5, xi + 4, 1, z1 + 5, "air");
                    set(xi + 2, 1, z1 + 3, "barrel[facing=up,open=true]");
                    furniture.chair(xi + 1, 1, z1 + 2, "west", "spruce");
                }
                case 4 -> {
                    sedan(xi + 1, z1 + 1, true, true);
                }
                default -> {
                    furniture.camp(xi + 2, 1, z1 + 3, "east");
                    crate(xi, 1, z1 + 5, "east", "garaz");
                }
            }
        }

        // ------------------------------------------------------------------ bloki

        /**
         * Blok z wielkiej płyty: {@code segments} klatek po {@link #SEGMENT} kratek, wejścia od południa
         * (ściana z0 + 11), balkony od północy (z0 - 1). Zwraca x środkowych pasów klatek.
         */
        private int[] block(int x0, int z0, int segments, int floors, boolean ruined, boolean shop) {
            int x1 = x0 + SEGMENT * segments;
            int z1 = z0 + BLOCK_DEPTH - 1;
            int roof = floors * STOREY;
            // fundament z cokołem
            fill(x0, -1, z0, x1, 0, z1, "stone");
            fill(x0 - 1, 1, z0 - 1, x1 + 1, roof + 3, z1 + 1, "air");
            for (int f = 0; f < floors; f++) {
                int y = f * STOREY;
                fill(x0, y, z0, x1, y, z1, "smooth_stone");
                for (int dy = 1; dy < STOREY; dy++) {
                    for (int x = x0; x <= x1; x++) {
                        set(x, y + dy, z0, panel.next());
                        set(x, y + dy, z1, panel.next());
                    }
                    for (int z = z0; z <= z1; z++) {
                        set(x0, y + dy, z, panel.next());
                        set(x1, y + dy, z, panel.next());
                    }
                }
                // spoina między płytami na zewnątrz
                for (int x = x0; x <= x1; x++) {
                    set(x, y, z0, "gray_concrete");
                    set(x, y, z1, "gray_concrete");
                }
                for (int z = z0; z <= z1; z++) {
                    set(x0, y, z, "gray_concrete");
                    set(x1, y, z, "gray_concrete");
                }
            }
            roof(x0, z0, x1, z1, roof);
            int[] centres = new int[segments];
            for (int s = 0; s < segments; s++) {
                int sx = x0 + s * SEGMENT;
                int c = sx + 9;
                centres[s] = c;
                if (s > 0) {
                    for (int f = 0; f < floors; f++) {
                        fill(sx, f * STOREY + 1, z0 + 1, sx, f * STOREY + 3, z1 - 1, "light_gray_concrete");
                    }
                }
                stairwell(c, z0, floors);
                for (int f = 0; f < floors; f++) {
                    boolean top = f == floors - 1;
                    if (!(shop && s == 0 && f == 0)) {
                        apartment(sx + 1, c - 2, z0, f, c, top, ruined && s == 1);
                    }
                    apartment(sx + SEGMENT - 1, c + 3, z0, f, c, top, ruined && s == 1);
                }
                if (!ruined || s == 0) {
                    roofHatch(c, z0, floors);
                }
            }
            return centres;
        }

        private void roof(int x0, int z0, int x1, int z1, int y) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    set(x, y, z, chance(0.15) ? "gravel" : chance(0.08) ? "moss_block" : "black_concrete");
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    if (edge) {
                        set(x, y + 1, z, "gray_concrete");
                    }
                }
            }
            // wywietrzniki i anteny
            for (int x = x0 + 4; x < x1 - 2; x += 9) {
                set(x, y + 1, z0 + 5, "bricks");
                set(x, y + 2, z0 + 5, "brick_slab[type=bottom,waterlogged=false]");
                if (chance(0.7)) {
                    int ax = x + 3;
                    fill(ax, y + 1, z0 + 3, ax, y + 4, z0 + 3, "lightning_rod[facing=up,powered=false,waterlogged=false]");
                    set(ax, y + 3, z0 + 2, "iron_bars");
                    set(ax, y + 3, z0 + 4, "iron_bars");
                }
            }
        }

        /** Wyłaz na dach z najwyższego piętra: drabina przy ścianie podestu, na który wchodzi się na ostatnie piętro. */
        private void roofHatch(int c, int z0, int floors) {
            int top = (floors - 1) * STOREY;
            boolean north = (floors - 1) % 2 == 1;
            int z = north ? z0 + 1 : z0 + 10;
            String facing = north ? "south" : "north";
            for (int y = top + 1; y <= top + STOREY; y++) {
                set(c, y, z, "ladder[facing=" + facing + ",waterlogged=false]");
            }
            set(c, top + STOREY + 1, z, "air");
        }

        private void stairwell(int c, int z0, int floors) {
            int z1 = z0 + BLOCK_DEPTH - 1;
            for (int f = 0; f < floors; f++) {
                int y = f * STOREY;
                // ściany klatki
                for (int z = z0 + 1; z < z1; z++) {
                    for (int dy = 1; dy < STOREY; dy++) {
                        set(c - 1, y + dy, z, "light_gray_concrete");
                        set(c + 2, y + dy, z, "light_gray_concrete");
                        set(c, y + dy, z, "air");
                        set(c + 1, y + dy, z, "air");
                    }
                    // lastryko na podeście
                    set(c, y, z, "polished_andesite");
                    set(c + 1, y, z, "polished_andesite");
                }
                // luksfery w ścianie północnej
                for (int dy = 1; dy < STOREY; dy++) {
                    set(c, y + dy, z0, "white_stained_glass");
                    set(c + 1, y + dy, z0, chance(0.2) ? "air" : "white_stained_glass");
                }
                // szafa elektryczna w ścianie podestu, na który wchodzi się na to piętro
                set(c + 2, y + 2, f % 2 == 1 ? z0 + 2 : z0 + 10, "sticky_piston[extended=false,facing=west]");
                furniture.hangingLamp(c, y + STOREY - 1, z0 + 3);
            }
            // biegi schodów
            for (int f = 0; f < floors - 1; f++) {
                int y = f * STOREY;
                boolean even = f % 2 == 0;
                int lane = even ? c : c + 1;
                for (int i = 0; i < STOREY; i++) {
                    int z = even ? z0 + 8 - i : z0 + 5 + i;
                    set(lane, y + 1 + i, z, stairs("stone_brick", even ? "north" : "south"));
                    // miejsce na głowę: dziura w stropie nad biegiem
                    for (int h = y + 2 + i; h <= y + 4 + i; h++) {
                        set(lane, h, z, "air");
                    }
                }
            }
            // wejście do klatki z podwórka
            fill(c, 0, z1 + 1, c + 1, 0, z1 + 2, "stone_bricks");
            fill(c - 1, 4, z1 + 1, c + 2, 4, z1 + 2, "smooth_stone_slab[type=bottom,waterlogged=false]");
            boolean open = chance(0.6);
            door(c, 1, z1, "dark_oak_door", "south", "left", open);
            if (chance(0.5)) {
                set(c + 1, 1, z1, "air");
                set(c + 1, 2, z1, "air");
            } else {
                door(c + 1, 1, z1, "dark_oak_door", "south", "right", open);
            }
            set(c, 3, z1, "white_stained_glass");
            set(c + 1, 3, z1, "white_stained_glass");
            // luksfery nad wejściem na kolejnych piętrach
            for (int f = 1; f < floors; f++) {
                for (int dy = 1; dy < STOREY; dy++) {
                    set(c, f * STOREY + dy, z1, "white_stained_glass");
                    set(c + 1, f * STOREY + dy, z1, chance(0.15) ? "air" : "white_stained_glass");
                }
            }
        }

        /**
         * Mieszkanie po jednej stronie klatki. {@code outer} to x przy ścianie szczytowej lub między segmentami,
         * {@code inner} to x przy ścianie klatki. Drzwi z podestu, na który wchodzi się na to piętro.
         */
        private void apartment(int outer, int inner, int z0, int f, int c, boolean top, boolean cutOff) {
            int y = f * STOREY;
            int dir = outer < inner ? -1 : 1;
            int xa = Math.min(outer, inner);
            int xb = Math.max(outer, inner);
            boolean northEntry = f % 2 == 1;
            int entryZ = northEntry ? z0 + 3 : z0 + 9;
            int wellWall = dir < 0 ? c - 1 : c + 2;
            int width = xb - xa + 1;

            // podłogi: parkiet w pokoju, lastryko i płytki w kuchni
            String parquet = pick(new String[]{"oak_planks", "spruce_planks", "birch_planks"});
            for (int x = xa; x <= xb; x++) {
                for (int z = z0 + 1; z <= z0 + 10; z++) {
                    boolean room = northEntry ? z >= z0 + 7 : z <= z0 + 5;
                    set(x, y, z, room ? parquet : chance(0.5) ? "white_terracotta" : "light_gray_terracotta");
                }
            }
            // ściana działowa z przejściem
            int gap = xa + 1 + rnd.nextInt(Math.max(1, width - 2));
            for (int x = xa; x <= xb; x++) {
                for (int dy = 1; dy < STOREY; dy++) {
                    set(x, y + dy, z0 + 6, x == gap && dy <= 2 ? "air" : "white_concrete");
                }
            }
            // drzwi do mieszkania
            if (!cutOff || f < 2) {
                String type = chance(0.85) ? "spruce_door" : "birch_door";
                if (chance(0.25)) {
                    set(wellWall, y + 1, entryZ, "air");
                    set(wellWall, y + 2, entryZ, "air");
                } else {
                    door(wellWall, y + 1, entryZ, type, dir < 0 ? "west" : "east", "left", chance(0.5));
                }
            }

            // okna: od północy pokój lub kuchnia z balkonem, od południa okna kuchni/pokoju
            int winA = dir < 0 ? xa + 1 : xb - 2;
            int winB = dir < 0 ? xa + 4 : xb - 4;
            for (int x : new int[]{winA, winA + 1, winB}) {
                if (x >= xa && x <= xb && x != wellWall) {
                    set(x, y + 2, z0, window());
                    set(x, y + 2, z0 + 11, window());
                    if (y > 0 || chance(0.5)) {
                        set(x, y + 3, z0 + 11, chance(0.3) ? "air" : "glass_pane");
                    }
                }
            }
            balcony(winB, z0, y, f);

            boolean entryHalfNorth = northEntry;
            int hz1 = entryHalfNorth ? z0 + 1 : z0 + 7;
            int hz2 = entryHalfNorth ? z0 + 5 : z0 + 10;
            int rz1 = entryHalfNorth ? z0 + 7 : z0 + 1;
            int rz2 = entryHalfNorth ? z0 + 10 : z0 + 5;
            bathroom(outer, dir, entryHalfNorth, z0, y);
            kitchen(xa, xb, hz1, hz2, y, outer, entryHalfNorth);
            room(xa, xb, rz1, rz2, y, entryHalfNorth, gap);

            // stan mieszkania i łup
            double state = rnd.nextDouble();
            if (state < 0.18) {
                furniture.traces(xa, rz1, xb, rz2, y + 1, chance(0.3) ? 1 : 0, 1);
            } else if (state < 0.28) {
                int cx = (xa + xb) / 2;
                int cz = (rz1 + rz2) / 2;
                if (isAir(cx, y + 1, cz)) {
                    set(cx, y + 1, cz, "candle[candles=2,lit=false,waterlogged=false]");
                }
            }
            String pool = cutOff && f >= 2 ? "pietro" : top && chance(0.35) ? "pietro" : "mieszkanie";
            if (chance(cutOff ? 1.0 : 0.6)) {
                placeCrate(xa, rz1, xb, rz2, y + 1, pool);
            }
            if (chance(0.25)) {
                placeCrate(xa, hz1, xb, hz2, y + 1, "lodowki");
            }
            for (int i = 0; i < 2; i++) {
                int x = xa + rnd.nextInt(width);
                int z = z0 + 1 + rnd.nextInt(10);
                if (isAir(x, y + 3, z) && chance(0.6)) {
                    set(x, y + 3, z, "cobweb");
                }
            }
        }

        private void placeCrate(int x1, int z1, int x2, int z2, int y, String pool) {
            for (int k = 0; k < 40; k++) {
                int x = x1 + rnd.nextInt(x2 - x1 + 1);
                int z = z1 + rnd.nextInt(z2 - z1 + 1);
                if (isAir(x, y, z) && isAir(x, y + 1, z) && !isCrate(x, y, z) && !isCrate(x, y - 1, z)
                        && !get(x, y - 1, z).contains("air")) {
                    crate(x, y, z, pick(new String[]{"north", "south", "east", "west"}), pool);
                    return;
                }
            }
        }

        /** Łazienka 2 × 2 w rogu przy ścianie zewnętrznej, w połowie z wejściem: kibel i umywalka. */
        private void bathroom(int outer, int dir, boolean north, int z0, int y) {
            int bx1 = outer;
            int bx2 = outer - dir;
            int wallX = outer - 2 * dir;
            int za = north ? z0 + 1 : z0 + 9;
            int zb = north ? z0 + 2 : z0 + 10;
            int wallZ = north ? z0 + 3 : z0 + 8;
            for (int dy = 1; dy < STOREY; dy++) {
                for (int z = Math.min(za, wallZ); z <= Math.max(zb, wallZ); z++) {
                    set(wallX, y + dy, z, "white_concrete");
                }
                set(bx1, y + dy, wallZ, "white_concrete");
                set(bx2, y + dy, wallZ, "white_concrete");
            }
            // drzwi łazienki w ścianie bocznej, od strony środka mieszkania
            int dz = north ? z0 + 2 : z0 + 9;
            set(wallX, y + 1, dz, "air");
            set(wallX, y + 2, dz, "air");
            for (int x : new int[]{bx1, bx2}) {
                for (int z = za; z <= zb; z++) {
                    set(x, y, z, chance(0.5) ? "white_concrete" : "cyan_terracotta");
                }
            }
            String back = north ? "north" : "south";
            set(bx1, y + 1, north ? za : zb, "end_portal_frame[eye=false,facing=" + (north ? "south" : "north") + "]");
            furniture.sink(bx2, y + 1, north ? za : zb, back);
            if (chance(0.5)) {
                set(bx1, y + 1, north ? zb : za, "cauldron");
            }
        }

        private void kitchen(int xa, int xb, int z1, int z2, int y, int outer, boolean north) {
            int wallZ = north ? z1 : z2;
            String face = north ? "south" : "north";
            int dirIn = outer < xb ? 1 : -1;
            int start = outer + 3 * dirIn;
            // ciąg kuchenny przy ścianie zewnętrznej: kuchenka, zlew, lodówka
            if (chance(0.85)) {
                furniture.stove(start, y + 1, wallZ, face);
            }
            int sx = start + dirIn;
            if (sx >= xa && sx <= xb) {
                set(sx, y + 1, wallZ, "spruce_planks");
                set(sx, y + 2, wallZ, "air");
            }
            int fx = start + 2 * dirIn;
            if (fx >= xa && fx <= xb && chance(0.7)) {
                set(fx, y + 1, wallZ, "calibrated_sculk_sensor[facing=" + face + ",power=0,sculk_sensor_phase=inactive,waterlogged=false]");
                set(fx, y + 2, wallZ, "barrier");
            }
            // stół z krzesłami
            int tz = north ? z1 + 2 : z2 - 2;
            int tx = (xa + xb) / 2 + (chance(0.5) ? 0 : 1);
            if (isAir(tx, y + 1, tz) && chance(0.8)) {
                furniture.table(tx, y + 1, tz, "spruce");
                if (isAir(tx + 1, y + 1, tz)) {
                    furniture.chair(tx + 1, y + 1, tz, "east", "spruce");
                }
                if (chance(0.5) && isAir(tx - 1, y + 1, tz)) {
                    furniture.chair(tx - 1, y + 1, tz, "west", "spruce");
                }
            }
        }

        private void room(int xa, int xb, int z1, int z2, int y, boolean entryNorth, int gap) {
            // dywan we wzór
            String rug = pick(new String[]{"red", "brown", "orange", "green"});
            String rug2 = rug.equals("red") ? "orange" : "yellow";
            int cx = (xa + xb) / 2;
            int cz = (z1 + z2) / 2;
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    if (chance(0.85)) {
                        set(x, y + 1, z, ((x + z) % 2 == 0 ? rug : rug2) + "_carpet");
                    }
                }
            }
            // meblościanka wzdłuż ściany działowej
            int wallZ = entryNorth ? z1 : z2;
            String face = entryNorth ? "south" : "north";
            int len = 2 + rnd.nextInt(3);
            int bx = xa + rnd.nextInt(Math.max(1, xb - xa - len + 2));
            for (int x = bx; x < bx + len && x <= xb; x++) {
                if (x != gap && (isAir(x, y + 1, wallZ) || get(x, y + 1, wallZ).contains("carpet"))) {
                    set(x, y + 1, wallZ, chance(0.3) ? "chiseled_bookshelf[facing=" + face
                            + ",slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=false,slot_5_occupied=true]"
                            : "bookshelf");
                    if (chance(0.7)) {
                        set(x, y + 2, wallZ, "bookshelf");
                    }
                }
            }
            // łóżko lub wersalka przy ścianie zewnętrznej
            int bedZ = entryNorth ? z2 : z1;
            String bedFacing = rnd.nextBoolean() ? "east" : "west";
            int bedX = bedFacing.equals("east") ? xa : xb;
            if (chance(0.8) && isAir(bedX, y + 1, bedZ) && isAir(bedX + Furniture.dx(bedFacing), y + 1, bedZ)) {
                furniture.bed(bedX, y + 1, bedZ, bedFacing, pick(new String[]{"brown", "red", "light_gray", "orange"}));
            }
            // fotel i telewizor na szafce
            int ax = bedFacing.equals("east") ? xb : xa;
            if (chance(0.6) && isAir(ax, y + 1, cz)) {
                furniture.chair(ax, y + 1, cz, bedFacing.equals("east") ? "east" : "west", "dark_oak");
            }
            if (chance(0.3)) {
                int lx = cx;
                int lz = entryNorth ? z1 + 1 : z2 - 1;
                if (isAir(lx, y + 1, lz)) {
                    set(lx, y + 1, lz, "lectern[facing=" + face + ",has_book=false,powered=false]");
                }
            }
        }

        /** Balkon 3 × 2 z kolorowym murkiem przed oknem pokoju; część zabudowana, czasem odpadł cały. */
        private void balcony(int x, int z0, int y, int f) {
            if (y == 0 || chance(0.12)) {
                return;
            }
            String front = pick(new String[]{"blue_terracotta", "cyan_terracotta", "yellow_terracotta", "green_terracotta", "orange_terracotta"});
            fill(x - 1, y, z0 - 2, x + 1, y, z0 - 1, "smooth_stone_slab[type=top,waterlogged=false]");
            fill(x - 1, y + 1, z0 - 2, x + 1, y + 3, z0 - 1, "air");
            fill(x - 1, y + 1, z0 - 2, x + 1, y + 1, z0 - 2, front);
            // drzwi balkonowe
            set(x, y + 1, z0, "air");
            set(x, y + 2, z0, "air");
            if (chance(0.3)) {
                fill(x - 1, y + 2, z0 - 2, x + 1, y + 2, z0 - 2, chance(0.5) ? "glass_pane" : "spruce_planks");
            } else if (chance(0.4)) {
                set(x + 1, y + 2, z0 - 2, "white_banner[rotation=0]");
            } else if (chance(0.3)) {
                set(x - 1, y + 1, z0 - 1, "barrel[facing=up,open=false]");
            }
        }

        // ------------------------------------------------------------------ piwnica pod blokiem 3

        private void basement(int x0, int z0, int c) {
            int x1 = x0 + SEGMENT * 2;
            int z1 = z0 + BLOCK_DEPTH - 1;
            fill(x0, -5, z0, x1, -1, z1, basementWall);
            fill(x0 + 1, -5, z0 + 1, x1 - 1, -5, z1 - 1, "cobblestone");
            fill(x0 + 1, -4, z0 + 1, x1 - 1, -1, z1 - 1, "air");
            // zejście w pierwszej klatce pasem c + 1 z parteru
            int lane = c + 1;
            for (int i = 0; i < 4; i++) {
                int z = z0 + 8 - i;
                set(lane, -i, z, stairs("stone_brick", "south"));
                for (int h = -i + 1; h <= 0; h++) {
                    set(lane, h, z, "air");
                }
            }
            set(lane, 0, z0 + 9, "polished_andesite");
            // komórki lokatorskie wzdłuż korytarza z 3-5: od południa z 7-10, od północy pas z 1
            fill(x0 + 1, -4, z0 + 1, x1 - 1, -1, z0 + 1, "air");
            fill(x0 + 1, -1, z0 + 3, x1 - 1, -1, z0 + 5, "air");
            for (int x = x0 + 1; x <= x1 - 1; x++) {
                // rury pod sufitem korytarza
                set(x, -1, z0 + 3, "light_gray_terracotta");
            }
            int cell = 0;
            for (int x = x0 + 1; x + 3 <= x1 - 1; x += 4) {
                if (x + 3 >= lane - 1 && x <= lane + 1) {
                    continue;
                }
                cell++;
                for (int z = z0 + 6; z <= z0 + 10; z++) {
                    for (int dy = -4; dy <= -2; dy++) {
                        set(x, dy, z, "spruce_fence");
                    }
                }
                for (int xx = x; xx <= x + 3; xx++) {
                    for (int dy = -4; dy <= -2; dy++) {
                        set(xx, dy, z0 + 6, "spruce_fence");
                    }
                }
                set(x + 2, -4, z0 + 6, "spruce_fence_gate[facing=north,in_wall=false,open=" + chance(0.5) + ",powered=false]");
                set(x + 2, -3, z0 + 6, "air");
                cellContents(cell, x + 1, z0 + 7);
            }
            furniture.hangingLamp(x0 + 6, -2, z0 + 4);
            furniture.hangingLamp(x1 - 6, -2, z0 + 4);
            // szafa elektryczna i bojler przy schodach
            set(lane + 1, -4, z0 + 4, "sticky_piston[extended=false,facing=west]");
        }

        private void cellContents(int cell, int x, int z) {
            switch (cell % 5) {
                case 1 -> {
                    // ktoś tu mieszka
                    furniture.bed(x, -4, z + 2, "east", "brown");
                    set(x + 2, -4, z, "candle[candles=3,lit=false,waterlogged=false]");
                    sign(x + 1, -4, z, "oak_sign[rotation=0,waterlogged=false]", "TU ŚPI", "ZBYCHU", "NIE RUSZAĆ", "");
                    crate(x + 2, -4, z + 3, "west", "piwnica");
                }
                case 2 -> {
                    // słoiki na półkach
                    fill(x, -4, z + 3, x + 2, -4, z + 3, "spruce_slab[type=top,waterlogged=false]");
                    for (int i = 0; i < 3; i++) {
                        set(x + i, -3, z + 3, chance(0.7) ? "decorated_pot[cracked=false,facing=north,waterlogged=false]" : "air");
                    }
                    crate(x + 1, -4, z + 1, "north", "piwnica");
                    sign(x, -4, z, "oak_sign[rotation=8,waterlogged=false]", "KOMÓRKA 7", "SŁOIKI MAMY", "RĘCE PRECZ", "");
                }
                case 3 -> {
                    set(x, -4, z + 2, "barrel[facing=up,open=false]");
                    set(x, -3, z + 2, "barrel[facing=up,open=false]");
                    set(x + 2, -4, z + 3, "composter[level=0]");
                    crate(x + 1, -4, z + 3, "north", "piwnica");
                }
                case 4 -> {
                    set(x + 1, -4, z + 2, "crafting_table");
                    set(x + 2, -4, z + 2, "respawn_anchor[charges=0]");
                }
                default -> {
                    set(x, -4, z + 1, "cobweb");
                    crate(x + 2, -4, z + 2, "west", "piwnica");
                }
            }
        }

        // ------------------------------------------------------------------ sklep na parterze bloku 3

        /** Lewe mieszkanie parteru pierwszej klatki przerobione na sklepik z wejściem od ulicy. */
        private void shop(int x0, int z0) {
            int xa = x0 + 1;
            int xb = x0 + 7;
            fill(xa, 1, z0 + 1, xb, 3, z0 + 10, "air");
            fill(xa, 0, z0 + 1, xb, 0, z0 + 10, "white_terracotta");
            for (int x = xa; x <= xb; x++) {
                for (int z = z0 + 1; z <= z0 + 10; z++) {
                    if ((x + z) % 2 == 0) {
                        set(x, 0, z, "gray_terracotta");
                    }
                }
            }
            // zaplecze odcięte ścianą z 8
            fill(xa, 1, z0 + 8, xb, 3, z0 + 8, "white_concrete");
            set(xa + 3, 1, z0 + 8, "air");
            set(xa + 3, 2, z0 + 8, "air");
            // witryna i wejście od ulicy
            for (int x = xa; x <= xb; x++) {
                set(x, 1, z0, x == xa + 3 ? "air" : "light_gray_concrete");
                set(x, 2, z0, x == xa + 3 ? "air" : chance(0.5) ? "air" : "glass_pane");
                set(x, 3, z0, "red_concrete");
            }
            // lada z kasą, regały sklepowe
            for (int x = xa + 1; x <= xb - 1; x++) {
                set(x, 1, z0 + 5, x == xa + 3 ? "air" : "jack_o_lantern[facing=north]");
            }
            set(xa + 1, 2, z0 + 5, "ender_chest[facing=north,waterlogged=false]");
            crate(xb - 1, 1, z0 + 6, "north", "lada");
            set(xa, 1, z0 + 2, "bee_nest[facing=east,honey_level=0]");
            set(xa, 1, z0 + 3, "bee_nest[facing=east,honey_level=0]");
            set(xb, 1, z0 + 2, "bee_nest[facing=west,honey_level=0]");
            set(xb, 1, z0 + 3, "bee_nest[facing=west,honey_level=0]");
            crate(xa + 4, 1, z0 + 2, "north", "sklep");
            set(xa + 2, 1, z0 + 2, "lodestone");
            // zaplecze: chłodnia i skrzynki
            set(xa, 1, z0 + 9, "calibrated_sculk_sensor[facing=east,power=0,sculk_sensor_phase=inactive,waterlogged=false]");
            set(xa, 2, z0 + 9, "barrier");
            crate(xb, 1, z0 + 10, "west", "zaplecze");
            crate(xa + 1, 1, z0 + 10, "north", "lodowki");
            sign(xa + 3, 3, z0 - 1, "spruce_wall_sign[facing=north,waterlogged=false]", "SKLEP", "U HALINKI", "ZESZYT", "ZAMKNIĘTY");
        }

        // ------------------------------------------------------------------ zawalony koniec bloku 4

        /**
         * Wschodni koniec bloku 4 zawalił się od 2 piętra w górę, a bieg schodów 1→2 w drugiej klatce zniknął.
         * Na górne mieszkania drugiej klatki wchodzi się po gruzowisku od podwórka (północ) przez wyrwę.
         */
        private void collapse(int x0, int z0) {
            int x1 = x0 + SEGMENT * 2;
            int z1 = z0 + BLOCK_DEPTH - 1;
            int c = x0 + SEGMENT + 9;
            // wyrwa: najwyższe piętro i dach od x1 - 10, 2 piętro od x1 - 4
            fill(x1 - 10, 12, z0 - 2, x1, 18, z1, "air");
            fill(x1 - 4, 9, z0 - 2, x1, 11, z1, "air");
            for (int x = x1 - 11; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = 8; y <= 13; y++) {
                        String s = get(x, y, z);
                        if ((s.contains("concrete") || s.contains("smooth_stone") || s.contains("andesite")) && chance(0.35)) {
                            set(x, y, z, burnt.next());
                        }
                    }
                }
            }
            // zerwany bieg schodów 1→2 w drugiej klatce i gruz na podeście
            for (int i = 0; i < STOREY; i++) {
                set(c + 1, STOREY + 1 + i, z0 + 5 + i, "air");
            }
            fill(c, STOREY + 1, z0 + 2, c + 1, STOREY + 1, z0 + 3, rubble);
            // gruzowisko od północy: schodkowy nasyp do stropu 2 piętra (y 8)
            for (int x = x1 - 8; x <= x1 + 2; x++) {
                for (int z = z0 - 12; z <= z0 - 1; z++) {
                    int d = z0 - z;
                    int h = Math.max(0, 9 - d) - (x > x1 ? 2 : 0) - (x < x1 - 6 ? 3 : 0);
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, rubble.next());
                    }
                }
            }
            // przejście z nasypu do mieszkania przez wybitą ścianę
            fill(x1 - 5, 9, z0, x1 - 4, 10, z0, "air");
            sign(x1 - 7, 1, z0 - 13, "oak_sign[rotation=8,waterlogged=false]", "NIE WCHODZIĆ", "STROP", "SIĘ SYPIE", "– ADM.");
            crate(x1 - 4, 9, z0 + 3, "north", "pietro");
            crate(x1 - 2, 7, z0 - 3, "north", "npc-szabrownik");
        }

        // ------------------------------------------------------------------ podwórko

        private void courtyard() {
            // trzepak: dwa słupki i drążek
            fill(50, 1, 26, 50, 3, 26, "andesite_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
            fill(55, 1, 26, 55, 3, 26, "andesite_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
            fill(51, 3, 26, 54, 3, 26, "iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]");
            set(52, 2, 26, "red_wool");
            set(53, 2, 26, "red_wool");
            // piaskownica
            for (int x = 60; x <= 64; x++) {
                for (int z = 24; z <= 28; z++) {
                    boolean edge = x == 60 || x == 64 || z == 24 || z == 28;
                    set(x, 0, z, "sand");
                    set(x, 1, z, edge ? "spruce_slab[type=bottom,waterlogged=false]" : "air");
                }
            }
            set(62, 1, 26, "flower_pot");
            // huśtawka: rama z płotów i łańcuchy z jednym siedziskiem
            fill(68, 1, 30, 68, 4, 30, "dark_oak_fence");
            fill(72, 1, 30, 72, 4, 30, "dark_oak_fence");
            fill(69, 4, 30, 71, 4, 30, "dark_oak_fence");
            set(70, 3, 30, "chain[axis=y,waterlogged=false]");
            set(70, 2, 30, "chain[axis=y,waterlogged=false]");
            set(70, 1, 30, "dark_oak_slab[type=top,waterlogged=false]");
            set(71, 3, 30, "chain[axis=y,waterlogged=false]");
            // zjeżdżalnia
            for (int i = 0; i < 3; i++) {
                set(76, 1 + i, 24 + i, stairs("weathered_cut_copper", "south"));
            }
            set(76, 3, 27, "weathered_cut_copper_slab[type=bottom,waterlogged=false]");
            for (int i = 0; i < 3; i++) {
                set(76, 3 - i, 28 + i, i == 2 ? "weathered_cut_copper_slab[type=bottom,waterlogged=false]" : stairs("weathered_cut_copper", "north"));
            }
            // ławki i kosze
            furniture.bench(48, 1, 36, 4, true, "south", "spruce");
            furniture.bench(64, 1, 36, 4, true, "south", "spruce");
            set(47, 1, 36, "composter[level=0]");
            furniture.bench(78, 1, 42, 4, false, "east", "spruce");
            // suszarka na pranie
            fill(48, 1, 46, 48, 3, 46, "iron_bars");
            fill(56, 1, 46, 56, 3, 46, "iron_bars");
            fill(49, 3, 46, 55, 3, 46, "tripwire[attached=false,disarmed=false,east=true,north=false,powered=false,south=false,west=true]");
            set(51, 2, 46, "white_banner[rotation=0]");
            // drzewa: lipy i brzozy przerośnięte przez lata
            int[][] trees = {{45, 21}, {58, 41}, {72, 49}, {83, 22}, {66, 52}, {44, 50}, {28, 54}, {4, 36}, {34, 44}};
            for (int[] t : trees) {
                tree(t[0], t[1], chance(0.5));
            }
            // krzaki i dzikie trawy
            for (int i = 0; i < 40; i++) {
                int x = 43 + rnd.nextInt(42);
                int z = 18 + rnd.nextInt(38);
                if (get(x, 0, z).contains("grass_block") && isAir(x, 1, z)) {
                    set(x, 1, z, chance(0.4) ? "azalea_leaves[distance=1,persistent=true,waterlogged=false]" : "tall_grass[half=lower]");
                    if (get(x, 1, z).contains("tall_grass")) {
                        set(x, 2, z, "tall_grass[half=upper]");
                    }
                }
            }
            // łup i szabrownicy na podwórku
            crate(57, 1, 37, "south", "plac");
            crate(74, 1, 40, "west", "plac");
            crate(66, 1, 44, "north", "npc-szabrownik");
            furniture.camp(80, 1, 33, "north");
            crate(81, 1, 35, "west", "mieszkanie");
            furniture.animalSomewhere(44, 18, 84, 55, 1, "cat");
            // dachy: wyłaz drabiną z ostatniego piętra
            crate(52, 17, 9, "south", "dach");
            crate(72, 17, 6, "west", "dach");
            crate(64, 17, 11, "north", "npc-szabrownik");
            crate(12, 13, 45, "south", "dach");
            crate(52, 17, 63, "south", "dach");
            // uliczka przy garażach
            crate(30, 1, 62, "east", "npc-szabrownik");
        }

        private void tree(int x, int z, boolean birch) {
            String log = birch ? "birch_log[axis=y]" : "oak_log[axis=y]";
            String leaves = (birch ? "birch" : "oak") + "_leaves[distance=1,persistent=true,waterlogged=false]";
            int h = 5 + rnd.nextInt(3);
            set(x, 0, z, "dirt");
            for (int y = 1; y <= h; y++) {
                set(x, y, z, log);
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = h - 2; y <= h + 1; y++) {
                        int r = Math.abs(dx) + Math.abs(dz) + (y > h ? 1 : 0);
                        if (r <= 3 && !(dx == 0 && dz == 0 && y <= h) && isAir(x + dx, y, z + dz) && chance(0.85)) {
                            set(x + dx, y, z + dz, leaves);
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ tabliczki

        private void signs() {
            sign(14, 1, 2, "oak_sign[rotation=0,waterlogged=false]", "PARKING", "TYLKO DLA", "MIESZKAŃCÓW", "– SPÓŁDZIELNIA");
            sign(10, 1, 11, "oak_sign[rotation=4,waterlogged=false]", "SPRZEDAM", "STAN IGŁA", "NIE BITY", "TEL. 12-34");
            sign(49, 1, 27, "oak_sign[rotation=8,waterlogged=false]", "TRZEPANIE", "WT. I CZW.", "DO 10:00", "– ADMINISTRACJA");
            sign(38, 2, 36, "spruce_wall_sign[facing=east,waterlogged=false]", "WYWÓZ", "ŚMIECI", "ZAWIESZONY", "DO ODWOŁANIA");
            sign(23, 3, 65, "spruce_wall_sign[facing=north,waterlogged=false]", "KTO UKRADŁ", "MOJE", "KANISTRY", "– GARAŻ 4");
            sign(83, 6, 10, "spruce_wall_sign[facing=east,waterlogged=false]", "OSIEDLE", "SŁONECZNE", "BLOK 3", "");
            sign(83, 6, 64, "spruce_wall_sign[facing=east,waterlogged=false]", "OSIEDLE", "SŁONECZNE", "BLOK 4", "");
            sign(25, 6, 45, "spruce_wall_sign[facing=east,waterlogged=false]", "OSIEDLE", "SŁONECZNE", "BLOK 5", "");
            sign(57, 2, 16, "spruce_wall_sign[facing=south,waterlogged=false]", "DOMOFON", "NIE DZIAŁA", "PUKAĆ", "GŁOŚNO");
            sign(17, 2, 52, "spruce_wall_sign[facing=south,waterlogged=false]", "ZEBRANIE", "LOKATORÓW", "ODWOŁANE", "NA ZAWSZE");
            sign(75, 2, 70, "spruce_wall_sign[facing=south,waterlogged=false]", "KLUCZE", "PANI HALINY", "SĄ POD", "WYCIERACZKĄ");
            furniture.graffiti(44, 18, 84, 55, 1, "MAREK", "+ KAŚKA", "NA ZAWSZE", "");
            // słońce na szczycie bloku 3
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int r = dy * dy + dz * dz;
                    if (r <= 5) {
                        set(82, 11 + dy, 9 + dz, r <= 2 ? "yellow_concrete" : "orange_concrete");
                    }
                }
            }
        }
    }
}
