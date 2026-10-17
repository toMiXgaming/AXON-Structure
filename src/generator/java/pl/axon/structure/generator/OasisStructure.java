package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

/**
 * Oaza „Świt” – stara, ogromna jednostka wojskowa przejęta przez przetrwańców. Mury z prefabrykatów są
 * połatane blachą i workami z piaskiem, budynki z wielkiej płyty mają nowe dachy, zabite deskami okna
 * i rusztowania. To jedyne miejsce na mapie bez przemocy: PvP jest tu wyłączone ({@code strefa: bezpieczna}),
 * strażnicy NPC strzelają tylko do potworów, a gracze handlują przy stanowiskach wymiany.
 * <p>
 * Struktura ma {@code grunt: 2}. W kodzie wysokości liczone są od gruntu (y = 0 to powierzchnia).
 * <pre>
 *  z 0-9     dojazd: droga x 70-79, tablica „OAZA ŚWIT – STREFA BEZ PRZEMOCY” i tablica z zasadami
 *  z 10      mur z płyt z chodnikiem (x 3-5 / 144-146 / z 11-13 / 134-136), brama główna x 70-79
 *            między wieżami bramnymi, brama boczna zachodnia z 64-66 i wschodnia z 82-84, cztery wieże
 *            wartownicze w rogach
 *  z 19-41   warsztat z gazikiem x 12-32, generator i wiatrak x 36-44, depozyt x 54-66, budka kontrolna,
 *            ogródek przy studni x 84-90, radiostacja z masztem x 92-102, sklep x 106-126, lazaret x 130-137
 *  z 42-45   aleja północna
 *  z 46-90   noclegownia (dawne koszary) x 12-40 z praniem, targowisko x 50-98 (8 stanowisk wymiany,
 *            kramy, tablica zleceń, fontanna), stołówka i Bar Pod Kanistrem z automatami x 106-137,
 *            kuchnia polowa, plac z ogniskiem i plac zabaw x 106-137 / z 72-91
 *  z 64-91   ogrody warzywne x 12-30, szklarnia x 33-44, zagrody x 33-45
 *  z 92-95   aleja południowa
 *  z 96-128  kaplica-świetlica w remoncie x 12-28, wieża ciśnień, lądowisko x 33-53, sad z letnią sceną
 *            x 56-84, strzelnica x 89-137 (5 torów, tarcze na 10, 20 i 35 m), parking z naprawionymi autami
 * </pre>
 */
public final class OasisStructure implements StructureDefinition {

    private static final int WIDTH = 150;
    private static final int DEPTH = 140;
    /** Ile warstw leży pod ziemią (tylko ziemia pod fundamentami i wodą). */
    private static final int G = 2;
    private static final int HEIGHT = G + 32;

    @Override
    public String id() {
        return "oaza";
    }

    @Override
    public String displayName() {
        return "Oaza Świt";
    }

    @Override
    public String description() {
        return "Bezpieczna baza przetrwańców w dawnej jednostce: targowisko ze stanowiskami wymiany, sklep, bar z automatami, strzelnica, noclegownia i ogrody – bez PvP";
    }

    @Override
    public Blueprint build(Random random) {
        return new Builder(random).build();
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);

        private final Palette grass;
        private final Palette asphalt;
        private final Palette path;
        private final Palette paving;
        private final Palette olive;
        private final Palette plaster;
        private final Palette concrete;
        private final Palette brick;
        private final Palette sheet;
        private final Palette sandbag;
        private final Palette white;
        private final Palette army;
        private final Palette fresh;
        private final Furniture f;

        Builder(Random rnd) {
            this.rnd = rnd;
            f = new Furniture(bp, G, rnd);
            grass = new Palette(rnd)
                    .add("grass_block", 14).add("coarse_dirt", 2).add("dirt", 1);
            asphalt = new Palette(rnd)
                    .add("gray_concrete", 7).add("andesite", 2).add("black_concrete", 1).add("cobblestone", 1);
            path = new Palette(rnd)
                    .add("dirt_path", 6).add("coarse_dirt", 2).add("gravel", 2);
            paving = new Palette(rnd)
                    .add("stone_bricks", 5).add("polished_andesite", 3).add("smooth_stone", 2);
            olive = new Palette(rnd)
                    .add("green_terracotta", 10).add("terracotta", 1).add("mossy_stone_bricks", 1);
            plaster = new Palette(rnd)
                    .add("light_gray_concrete", 6).add("white_terracotta", 3).add("cracked_stone_bricks", 1);
            concrete = new Palette(rnd)
                    .add("gray_concrete", 6).add("light_gray_concrete", 3).add("cracked_stone_bricks", 1);
            brick = new Palette(rnd)
                    .add("bricks", 12).add("terracotta", 2).add("mud_bricks", 1);
            sheet = new Palette(rnd)
                    .add("light_gray_concrete", 4).add("iron_block", 3).add("waxed_cut_copper", 1);
            sandbag = new Palette(rnd)
                    .add("mud_bricks", 5).add("packed_mud", 4);
            white = new Palette(rnd)
                    .add("white_concrete", 6).add("white_terracotta", 2).add("light_gray_concrete", 1);
            army = new Palette(rnd)
                    .add("green_concrete", 5).add("green_terracotta", 2);
            fresh = new Palette(rnd)
                    .add("green_concrete", 4).add("lime_concrete", 1).add("light_gray_concrete", 1);
        }

        Blueprint build() {
            bp.ground(G);
            bp.safeZone(true);
            ground();
            roads();
            perimeter();
            catwalks();
            tower(2, 10);
            tower(142, 10);
            tower(2, 132);
            tower(142, 132);
            mainGate();
            sideGates();
            approach();
            depot();
            checkpoint();
            wellGarden();
            radioStation();
            shop();
            infirmary();
            workshop();
            generator();
            plaza();
            dorms();
            laundry();
            gardens();
            greenhouse();
            pens();
            chapel();
            waterTower();
            helipad();
            orchard();
            range();
            canteenAndBar();
            fieldKitchen();
            bonfireSquare();
            parking();
            lampPosts();
            weeds();
            connect();
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

        /** Punkt dla pluginu (strażnik, stanowisko itp.) – to nie jest skrzynia. */
        private void marker(int x, int y, int z, String facing, String pool) {
            bp.crate(x, y + G, z, facing, pool);
        }

        /** Punkt w bloku lady / maszyny / tarczy: znacznik, a w nim widoczny blok. */
        private void counterMarker(int x, int y, int z, String facing, String pool, String block) {
            bp.crate(x, y + G, z, facing, pool);
            set(x, y, z, block);
        }

        private void sign(int x, int y, int z, String facing, String... lines) {
            bp.sign(x, y + G, z, "spruce_wall_sign[facing=" + facing + ",waterlogged=false]", lines);
        }

        private void post(int x, int y, int z, int rotation, String... lines) {
            bp.sign(x, y + G, z, "oak_sign[rotation=" + rotation + ",waterlogged=false]", lines);
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        private String pick(String... states) {
            return states[rnd.nextInt(states.length)];
        }

        private void door(int x, int y, int z, String type, String facing, String hinge, boolean open) {
            String props = "facing=" + facing + ",hinge=" + hinge + ",open=" + open + ",powered=false";
            set(x, y, z, type + "[half=lower," + props + "]");
            set(x, y + 1, z, type + "[half=upper," + props + "]");
        }

        private static String stairs(String material, String facing) {
            return material + "_stairs[facing=" + facing + ",half=bottom,shape=straight,waterlogged=false]";
        }

        private static String slab(String material, String type) {
            return material + "_slab[type=" + type + ",waterlogged=false]";
        }

        private static String lantern() {
            return "lantern[hanging=false,waterlogged=false]";
        }

        private static String hanging() {
            return "lantern[hanging=true,waterlogged=false]";
        }

        private static String campfire(String facing) {
            return "campfire[facing=" + facing + ",lit=true,signal_fire=false,waterlogged=false]";
        }

        private static String candle(int n) {
            return "candle[candles=" + n + ",lit=true,waterlogged=false]";
        }

        private static String ladder(String facing) {
            return "ladder[facing=" + facing + ",waterlogged=false]";
        }

        private static String trapdoor(String material, String facing, String half, boolean open) {
            return material + "_trapdoor[facing=" + facing + ",half=" + half + ",open=" + open
                    + ",powered=false,waterlogged=false]";
        }

        private String flowerPot() {
            return pick("potted_red_tulip", "potted_cornflower", "potted_fern", "potted_dandelion", "potted_poppy",
                    "potted_oxeye_daisy", "potted_azure_bluet", "potted_oak_sapling", "potted_allium");
        }

        private String flower() {
            return pick("poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "red_tulip",
                    "white_tulip", "lily_of_the_valley");
        }

        private String woolColor() {
            return pick("red", "yellow", "blue", "white", "orange", "lime", "light_blue", "pink", "cyan");
        }

        /** Ściany prostopadłościanu (bez podłogi i dachu). */
        private void walls(int x1, int z1, int x2, int z2, int y1, int y2, Palette palette) {
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    set(x, y, z1, palette.next());
                    set(x, y, z2, palette.next());
                }
                for (int z = z1; z <= z2; z++) {
                    set(x1, y, z, palette.next());
                    set(x2, y, z, palette.next());
                }
            }
        }

        private void floor(int x1, int z1, int x2, int z2, int y, Palette palette) {
            fill(x1, y, z1, x2, y, z2, palette);
        }

        /** Okno po remoncie: nowa szyba, zabite deskami albo jeszcze stara, szara szyba. */
        private String window() {
            double r = rnd.nextDouble();
            if (r < 0.6) {
                return "glass_pane";
            }
            if (r < 0.82) {
                return "spruce_planks";
            }
            if (r < 0.9) {
                return "stripped_spruce_log[axis=y]";
            }
            return "light_gray_stained_glass_pane";
        }

        private void windowsX(int z, int x1, int x2, int step, int... ys) {
            for (int x = x1; x <= x2; x += step) {
                String w = window();
                for (int y : ys) {
                    set(x, y, z, w);
                }
            }
        }

        private void windowsZ(int x, int z1, int z2, int step, int... ys) {
            for (int z = z1; z <= z2; z += step) {
                String w = window();
                for (int y : ys) {
                    set(x, y, z, w);
                }
            }
        }

        private static boolean oldWall(String state) {
            return switch (state) {
                case "minecraft:green_terracotta", "minecraft:terracotta", "minecraft:light_gray_concrete",
                     "minecraft:white_terracotta", "minecraft:gray_concrete", "minecraft:bricks",
                     "minecraft:cracked_stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:white_concrete",
                     "minecraft:smooth_stone" -> true;
                default -> false;
            };
        }

        /** Łaty na ścianach budynku: blacha, świeży beton, deski i miedź – widać, że ktoś tu naprawia. */
        private void patches(int x1, int z1, int x2, int z2, int y1, int y2, int count) {
            String[] mats = {"iron_block", "light_gray_concrete", "spruce_planks", "waxed_cut_copper", "iron_block",
                    "mud_bricks"};
            for (int i = 0; i < count; i++) {
                String mat = mats[rnd.nextInt(mats.length)];
                int side = rnd.nextInt(4);
                int y = y1 + rnd.nextInt(Math.max(1, y2 - y1));
                int t = rnd.nextInt(side < 2 ? x2 - x1 - 1 : z2 - z1 - 1) + 1;
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 2; b++) {
                        int x = side == 0 || side == 1 ? x1 + t + a : side == 2 ? x1 : x2;
                        int z = side == 2 || side == 3 ? z1 + t + a : side == 0 ? z1 : z2;
                        if (x > x2 || z > z2 || y + b > y2) {
                            continue;
                        }
                        if (oldWall(get(x, y + b, z))) {
                            set(x, y + b, z, mat);
                        }
                    }
                }
            }
        }

        /** Dach dwuspadowy z kalenicą wzdłuż x; szczyty na x1 i x2 zamurowane. */
        private void gableX(int x1, int z1, int x2, int z2, int y0, String mat, Palette gable) {
            for (int k = 0; z1 + k <= z2 - k; k++) {
                int y = y0 + k;
                for (int x = x1 - 1; x <= x2 + 1; x++) {
                    if (z1 + k == z2 - k) {
                        set(x, y, z1 + k, slab(mat, "bottom"));
                    } else {
                        set(x, y, z1 + k, stairs(mat, "south"));
                        set(x, y, z2 - k, stairs(mat, "north"));
                    }
                }
                for (int z = z1 + k + 1; z <= z2 - k - 1; z++) {
                    set(x1, y, z, gable.next());
                    set(x2, y, z, gable.next());
                }
            }
        }

        /** Dach dwuspadowy z kalenicą wzdłuż z; szczyty na z1 i z2 zamurowane. */
        private void gableZ(int x1, int z1, int x2, int z2, int y0, String mat, Palette gable) {
            for (int k = 0; x1 + k <= x2 - k; k++) {
                int y = y0 + k;
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    if (x1 + k == x2 - k) {
                        set(x1 + k, y, z, slab(mat, "bottom"));
                    } else {
                        set(x1 + k, y, z, stairs(mat, "east"));
                        set(x2 - k, y, z, stairs(mat, "west"));
                    }
                }
                for (int x = x1 + k + 1; x <= x2 - k - 1; x++) {
                    set(x, y, z1, gable.next());
                    set(x, y, z2, gable.next());
                }
            }
        }

        /** Płaski dach z attyką z worków z piaskiem i panelami słonecznymi. */
        private void flatRoof(int x1, int z1, int x2, int z2, int y, boolean solar) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, y, z, chance(0.88) ? "smooth_stone" : "light_gray_concrete");
                    boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                    if (edge) {
                        set(x, y + 1, z, chance(0.7) ? slab("mud_brick", "bottom") : "mud_bricks");
                    }
                }
            }
            if (solar) {
                for (int x = x1 + 2; x <= (x1 + x2) / 2; x++) {
                    for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                        set(x, y + 1, z, "daylight_detector[inverted=false,power=0]");
                    }
                }
            }
        }

        private void sandbags(int x1, int z1, int x2, int z2, int h) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    for (int y = 1; y < h; y++) {
                        set(x, y, z, sandbag.next());
                    }
                    set(x, h, z, slab("mud_brick", "bottom"));
                }
            }
        }

        private void lampPost(int x, int z) {
            fill(x, 1, z, x, 3, z, "spruce_fence");
            set(x, 4, z, lantern());
        }

        /** Mała brzoza / dąb z liśćmi, które nie opadają. */
        private void tree(int x, int z, String wood) {
            int trunk = 4 + rnd.nextInt(2);
            for (int y = 1; y <= trunk; y++) {
                set(x, y, z, wood + "_log[axis=y]");
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = trunk - 1; y <= trunk + 1; y++) {
                        int r = y > trunk ? 1 : 2;
                        if (Math.abs(dx) <= r && Math.abs(dz) <= r && !(Math.abs(dx) == 2 && Math.abs(dz) == 2)
                                && isAir(x + dx, y, z + dz)) {
                            set(x + dx, y, z + dz, wood + "_leaves[distance=1,persistent=true,waterlogged=false]");
                        }
                    }
                }
            }
        }

        /** Gazik (UAZ) z nowymi częściami: 4 × 2, z maską otwartą, jeśli {@code repair}. */
        private void uaz(int x, int z, boolean alongZ, boolean repair) {
            int dx = alongZ ? 0 : 1;
            int dz = alongZ ? 1 : 0;
            for (int i = 0; i < 4; i++) {
                for (int w = 0; w < 2; w++) {
                    int px = x + dx * i + dz * w;
                    int pz = z + dz * i + dx * w;
                    set(px, 1, pz, i == 0 && repair ? "lime_concrete" : fresh.next());
                    if (i >= 1 && i <= 2) {
                        set(px, 2, pz, i == 1 ? "glass_pane" : army.next());
                    }
                }
            }
            if (repair) {
                String face = alongZ ? "north" : "west";
                set(x, 2, z, trapdoor("iron", face, "bottom", true));
            }
        }

        // ------------------------------------------------------------------ teren, drogi

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    fill(x, -2, z, x, -1, z, "dirt");
                    set(x, 0, z, grass.next());
                }
            }
        }

        private void roads() {
            // droga główna od dojazdu do targowiska
            for (int x = 70; x <= 79; x++) {
                for (int z = 0; z <= 46; z++) {
                    set(x, 0, z, asphalt.next());
                }
            }
            for (int z = 2; z <= 44; z += 4) {
                set(74, 0, z, "white_concrete");
                set(75, 0, z, "white_concrete");
            }
            // obwodnica przy murze
            for (int x = 8; x <= 141; x++) {
                for (int z : new int[]{16, 17, 18, 129, 130, 131}) {
                    set(x, 0, z, path.next());
                }
            }
            for (int z = 16; z <= 131; z++) {
                for (int x : new int[]{8, 9, 10, 139, 140, 141}) {
                    set(x, 0, z, path.next());
                }
            }
            // aleje
            for (int x = 8; x <= 141; x++) {
                for (int z = 42; z <= 45; z++) {
                    set(x, 0, z, path.next());
                }
                for (int z = 92; z <= 95; z++) {
                    set(x, 0, z, path.next());
                }
            }
            for (int z = 42; z <= 131; z++) {
                for (int x = 46; x <= 48; x++) {
                    set(x, 0, z, path.next());
                }
                for (int x = 100; x <= 103; x++) {
                    set(x, 0, z, path.next());
                }
            }
            for (int z = 46; z <= 95; z++) {
                for (int x = 84; x <= 88; x++) {
                    if (z > 90) {
                        set(x, 0, z, path.next());
                    }
                }
            }
            for (int z = 96; z <= 131; z++) {
                for (int x = 54; x <= 56; x++) {
                    set(x, 0, z, path.next());
                }
                for (int x = 85; x <= 88; x++) {
                    set(x, 0, z, path.next());
                }
            }
            // ścieżka od zachodniej bramy
            for (int x = 3; x <= 45; x++) {
                for (int z = 63; z <= 66; z++) {
                    set(x, 0, z, path.next());
                }
            }
            // ścieżka od wschodniej bramy
            for (int x = 138; x <= 146; x++) {
                for (int z = 81; z <= 85; z++) {
                    set(x, 0, z, path.next());
                }
            }
        }

        // ------------------------------------------------------------------ mur, chodnik, wieże

        private void perimeter() {
            for (int x = 2; x <= 147; x++) {
                wallCell(x, 10, x);
                wallCell(x, 137, x);
            }
            for (int z = 11; z <= 136; z++) {
                wallCell(2, z, z);
                wallCell(147, z, z);
            }
            fill(70, 1, 10, 79, 6, 10, "air");
            fill(2, 1, 64, 2, 2, 66, "air");
            fill(147, 1, 82, 147, 2, 84, "air");
            // worki z piaskiem pod murem od zewnątrz przy bramie
            for (int x = 48; x <= 101; x++) {
                if ((x < 64 || x > 85) && chance(0.8)) {
                    set(x, 1, 9, chance(0.6) ? slab("mud_brick", "bottom") : "packed_mud");
                }
            }
        }

        /** Płyta muru (y 1-4) ze słupkiem co 5 bloków; stare płyty, łaty z blachy, worki i świeży beton. */
        private void wallCell(int x, int z, int pos) {
            if (pos % 5 == 0) {
                fill(x, 1, z, x, 5, z, "polished_andesite");
                return;
            }
            double r = rnd.nextDouble();
            String base = r < 0.55 ? "smooth_stone" : r < 0.68 ? "light_gray_concrete" : r < 0.76 ? "iron_block"
                    : r < 0.84 ? "mud_bricks" : r < 0.9 ? "cracked_stone_bricks" : r < 0.95 ? "spruce_planks"
                    : "waxed_exposed_cut_copper";
            for (int y = 1; y <= 4; y++) {
                set(x, y, z, chance(0.12) ? pick("iron_block", "packed_mud", "light_gray_concrete") : base);
            }
        }

        private int wx(int side, int t, int d) {
            return side <= 1 ? t : side == 2 ? 2 + d : 147 - d;
        }

        private int wz(int side, int t, int d) {
            return side == 0 ? 10 + d : side == 1 ? 137 - d : t;
        }

        /** Chodnik (deski na y 3, chodzi się na y 4) z poręczą od środka i latarniami. */
        private void catwalk(int side, int t1, int t2, int gate1, int gate2) {
            for (int t = t1; t <= t2; t++) {
                for (int d = 1; d <= 3; d++) {
                    set(wx(side, t, d), 3, wz(side, t, d), slab("spruce", "top"));
                }
                set(wx(side, t, 3), 4, wz(side, t, 3), "spruce_fence");
                if (t % 12 == 0) {
                    set(wx(side, t, 3), 5, wz(side, t, 3), lantern());
                }
                if (t % 6 == 0 && (t < gate1 || t > gate2)) {
                    fill(wx(side, t, 3), 1, wz(side, t, 3), wx(side, t, 3), 2, wz(side, t, 3), "spruce_log[axis=y]");
                }
            }
        }

        /** Schody na chodnik w rzędzie d = 4, w górę w stronę rosnącego t. */
        private void wallStairs(int side, int t0) {
            String facing = side <= 1 ? "east" : "south";
            for (int i = 0; i < 3; i++) {
                int x = wx(side, t0 + i, 4);
                int z = wz(side, t0 + i, 4);
                for (int y = 1; y <= i; y++) {
                    set(x, y, z, "stone_bricks");
                }
                set(x, i + 1, z, stairs("stone_brick", facing));
            }
            for (int t = t0 + 1; t <= t0 + 3; t++) {
                set(wx(side, t, 3), 4, wz(side, t, 3), "air");
                set(wx(side, t, 3), 5, wz(side, t, 3), "air");
            }
        }

        private void catwalks() {
            catwalk(0, 8, 63, 0, -1);
            catwalk(0, 86, 141, 0, -1);
            catwalk(1, 8, 141, 0, -1);
            catwalk(2, 16, 131, 63, 67);
            catwalk(3, 16, 131, 81, 85);
            wallStairs(0, 58);
            wallStairs(0, 88);
            wallStairs(1, 40);
            wallStairs(1, 108);
            wallStairs(2, 56);
            wallStairs(2, 110);
            wallStairs(3, 50);
            wallStairs(3, 110);
            // strażnicy na chodniku
            marker(40, 4, 12, "north", "npc-ochroniarz");
            marker(100, 4, 135, "south", "npc-ochroniarz");
            sign(60, 2, 15, "south", "NA MUR", "TYLKO Z", "LATARKĄ", "");
        }

        /**
         * Wieża wartownicza w rogu: cokół 6 × 6 (y 1-3, góra na poziomie chodnika), na nim słupy, podest y 8
         * z drabiną, barierką i dachem namiotowym. Strażnik stoi na podeście.
         */
        private void tower(int x1, int z1) {
            fill(x1, 1, z1, x1 + 5, 3, z1 + 5, concrete);
            for (int x = x1; x <= x1 + 5; x++) {
                for (int z = z1; z <= z1 + 5; z++) {
                    set(x, 3, z, "smooth_stone");
                }
            }
            int[][] legs = {{x1 + 1, z1 + 1}, {x1 + 4, z1 + 1}, {x1 + 1, z1 + 4}, {x1 + 4, z1 + 4}};
            for (int[] leg : legs) {
                fill(leg[0], 4, leg[1], leg[0], 11, leg[1], "spruce_log[axis=y]");
            }
            for (int x = x1 + 1; x <= x1 + 4; x++) {
                for (int z = z1 + 1; z <= z1 + 4; z++) {
                    if (isAir(x, 8, z)) {
                        set(x, 8, z, "spruce_planks");
                    }
                    boolean edge = x == x1 + 1 || x == x1 + 4 || z == z1 + 1 || z == z1 + 4;
                    if (edge && isAir(x, 9, z)) {
                        set(x, 9, z, "spruce_fence");
                    }
                }
            }
            // drabina przy słupie
            for (int y = 4; y <= 9; y++) {
                set(x1 + 2, y, z1 + 1, ladder("east"));
            }
            // dach namiotowy
            for (int ring = 0; ring <= 2; ring++) {
                int a1 = x1 + ring;
                int a2 = x1 + 5 - ring;
                int b1 = z1 + ring;
                int b2 = z1 + 5 - ring;
                int y = 12 + ring;
                for (int x = a1; x <= a2; x++) {
                    for (int z = b1; z <= b2; z++) {
                        boolean edge = x == a1 || x == a2 || z == b1 || z == b2;
                        if (!edge) {
                            continue;
                        }
                        if (ring == 2) {
                            set(x, y, z, slab("dark_oak", "bottom"));
                        } else if (z == b1) {
                            set(x, y, z, stairs("dark_oak", "south"));
                        } else if (z == b2) {
                            set(x, y, z, stairs("dark_oak", "north"));
                        } else if (x == a1) {
                            set(x, y, z, stairs("dark_oak", "east"));
                        } else {
                            set(x, y, z, stairs("dark_oak", "west"));
                        }
                    }
                }
            }
            set(x1 + 3, 11, z1 + 2, hanging());
            set(x1 + 2, 4, z1 + 3, lantern());
            set(x1 + 4, 10, z1 + 4, lantern());
            // worki z piaskiem na cokole
            set(x1 + 3, 4, z1 + 3, slab("mud_brick", "bottom"));
            marker(x1 + 3, 9, z1 + 3, z1 < 70 ? "north" : "south", "npc-ochroniarz");
        }

        // ------------------------------------------------------------------ bramy i dojazd

        private void mainGate() {
            // wieże bramne: pełny cokół, na nim wartownia z wejściem z chodnika
            for (int gx : new int[]{64, 80}) {
                fill(gx, 1, 10, gx + 5, 3, 15, concrete);
                for (int x = gx; x <= gx + 5; x++) {
                    for (int z = 10; z <= 15; z++) {
                        set(x, 3, z, "smooth_stone");
                    }
                }
                walls(gx, 10, gx + 5, 15, 4, 7, concrete);
                flatRoof(gx, 10, gx + 5, 15, 8, false);
                patches(gx, 10, gx + 5, 15, 4, 7, 3);
                int walk = gx == 64 ? gx : gx + 5;
                int road = gx == 64 ? gx + 5 : gx;
                fill(walk, 4, 11, walk, 5, 13, "air");
                set(road, 5, 12, "glass_pane");
                set(road, 5, 13, "glass_pane");
                set(gx + 2, 5, 10, "iron_bars");
                set(gx + 3, 5, 10, "iron_bars");
                set(gx + 2, 5, 15, "glass_pane");
                set(gx + 3, 5, 15, "glass_pane");
                fill(road, 1, 11, road, 3, 14, "polished_andesite");
                set(gx + 2, 7, 13, hanging());
                int inner = gx == 64 ? gx + 4 : gx + 1;
                f.table(inner, 4, 14, "spruce");
                f.chair(inner, 4, 13, "south", "spruce");
                set(inner, 4, 11, slab("mud_brick", "bottom"));
                set(gx, 9, 10, lantern());
                set(gx + 5, 9, 10, lantern());
                set(road, 4, 16, lantern());
            }
            // belka z napisem nad bramą
            for (int x = 70; x <= 79; x++) {
                set(x, 7, 10, "spruce_log[axis=x]");
                set(x, 8, 10, "spruce_planks");
            }
            sign(73, 7, 9, "north", "", "WITAJCIE", "", "");
            sign(74, 7, 9, "north", "", "OAZA", "", "");
            sign(75, 7, 9, "north", "", "ŚWIT", "", "");
            sign(76, 7, 9, "north", "", "BEZ BRONI", "W RĘKU", "");
            sign(74, 7, 11, "south", "UWAŻAJ", "NA SIEBIE", "TAM", "NA ZEWNĄTRZ");
            sign(75, 7, 11, "south", "WRACAJ", "DO NAS", "CAŁY", "");
            // skrzydła bramy otwarte na zewnątrz
            for (int z = 5; z <= 9; z++) {
                fill(69, 1, z, 69, 3, z, z % 2 == 0 ? "green_concrete" : "iron_block");
                fill(80, 1, z, 80, 3, z, z % 2 == 0 ? "green_concrete" : "iron_block");
            }
            // strażnicy w bramie
            marker(70, 1, 13, "north", "npc-ochroniarz");
            marker(79, 1, 13, "north", "npc-ochroniarz");
        }

        private void checkpoint() {
            int x1 = 82;
            int x2 = 86;
            int z1 = 20;
            int z2 = 24;
            walls(x1, z1, x2, z2, 1, 3, plaster);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            fill(x1 - 1, 4, z1 - 1, x2 + 1, 4, z2 + 1, slab("smooth_stone", "bottom"));
            fill(x1, 2, z1 + 1, x1, 2, z2 - 1, "glass_pane");
            set(84, 2, z1, "glass_pane");
            door(84, 1, z2, "spruce_door", "south", "left", false);
            fill(x1 + 1, 1, z1 + 1, x1 + 2, 1, z1 + 1, slab("spruce", "top"));
            f.lamp(x1 + 1, 2, z1 + 1);
            f.chair(x1 + 1, 1, z1 + 2, "east", "spruce");
            set(x2 - 1, 3, z2 - 1, hanging());
            sign(x1 - 1, 3, 22, "west", "KONTROLA", "BROŃ NA", "PLECY,", "UŚMIECH NA TWARZ");
            // podniesiony szlaban
            set(80, 1, 19, "polished_andesite");
            for (int y = 2; y <= 6; y++) {
                set(80, y, 19, y % 2 == 0 ? "red_concrete" : "white_concrete");
            }
            sandbags(81, 26, 83, 26, 1);
        }

        private void sideGates() {
            // zachodnia: przejście pod chodnikiem, budka i strażnik
            sign(1, 3, 63, "west", "BRAMA", "ZACHODNIA", "", "");
            sign(1, 3, 67, "west", "OAZA ŚWIT", "WEJŚCIE", "BOCZNE", "");
            set(3, 1, 62, lantern());
            set(3, 1, 67, lantern());
            sandbags(0, 61, 1, 62, 1);
            sandbags(0, 68, 1, 69, 1);
            marker(8, 1, 66, "west", "npc-ochroniarz");
            walls(5, 68, 8, 71, 1, 3, sandbag);
            fill(5, 4, 68, 8, 4, 71, slab("spruce", "bottom"));
            fill(6, 1, 68, 6, 2, 68, "air");
            set(7, 1, 70, "spruce_planks");
            set(7, 2, 70, lantern());
            // wschodnia
            set(146, 1, 81, lantern());
            set(146, 1, 85, lantern());
            sandbags(148, 80, 149, 80, 1);
            sandbags(148, 86, 149, 86, 1);
            marker(141, 1, 82, "east", "npc-ochroniarz");
            walls(141, 87, 144, 90, 1, 3, sandbag);
            fill(141, 4, 87, 144, 4, 90, slab("spruce", "bottom"));
            fill(142, 1, 87, 142, 2, 87, "air");
            set(143, 1, 89, "spruce_planks");
            set(143, 2, 89, lantern());
        }

        private void approach() {
            // tablica powitalna
            board(55, 5, 62);
            sign(57, 4, 4, "north", "", "OAZA ŚWIT", "", "");
            sign(58, 4, 4, "north", "STREFA", "BEZ", "PRZEMOCY", "");
            sign(59, 4, 4, "north", "", "TU NIKT", "DO NIKOGO", "NIE STRZELA");
            sign(60, 4, 4, "north", "STRAŻ", "PILNUJE", "DNIEM", "I NOCĄ");
            sign(57, 3, 4, "north", "HANDEL", "UCZCIWY", "NA", "STANOWISKACH");
            sign(58, 3, 4, "north", "SKLEP", "BAR", "STRZELNICA", "NOCLEG");
            sign(59, 3, 4, "north", "KTO PRZYCHODZI", "W POKOJU", "TEN JEST", "U SIEBIE");
            sign(60, 3, 4, "north", "", "WITAJ", "PRZYBYSZU", "");
            // tablica z zasadami
            board(87, 5, 94);
            sign(89, 4, 4, "north", "", "ZASADY", "OAZY", "");
            sign(90, 4, 4, "north", "1.", "NIE STRZELASZ", "DO LUDZI", "");
            sign(91, 4, 4, "north", "2.", "WYMIANA TYLKO", "PRZY", "STANOWISKACH");
            sign(92, 4, 4, "north", "3.", "STRAŻ STRZELA", "DO POTWORÓW,", "NIE DO WAS");
            sign(89, 3, 4, "north", "4.", "SZANUJ", "OGRODY", "I PRANIE");
            sign(90, 3, 4, "north", "5.", "PIJANYCH", "WYPROWADZAMY", "ZA BRAMĘ");
            sign(91, 3, 4, "north", "6.", "POMÓŻ", "PRZY REMONCIE", "TO I TWÓJ DOM");
            sign(92, 3, 4, "north", "", "– RADA", "OAZY", "");
            // latarnie przy dojeździe, odsunięte zapory
            for (int z = 1; z <= 8; z += 7) {
                lampPost(68, z);
                lampPost(81, z);
            }
            for (int[] b : new int[][]{{64, 1}, {66, 3}, {83, 2}, {85, 1}}) {
                fill(b[0], 1, b[1], b[0] + 1, 1, b[1], "smooth_stone");
                set(b[0], 2, b[1], "smooth_stone_slab[type=bottom,waterlogged=false]");
            }
            set(65, 1, 6, campfire("north"));
            f.bench(63, 1, 7, 3, true, "south", "spruce");
            post(66, 1, 7, 8, "OGNISKO", "DLA TYCH", "CO CZEKAJĄ", "");
        }

        /** Tablica na dwóch słupach: deski x1..x2 (y 2-4) na z, napisy po północnej stronie. */
        private void board(int x1, int z, int x2) {
            fill(x1, 1, z, x1, 5, z, "spruce_log[axis=y]");
            fill(x2, 1, z, x2, 5, z, "spruce_log[axis=y]");
            fill(x1 + 1, 2, z, x2 - 1, 4, z, "dark_oak_planks");
            fill(x1, 6, z, x2, 6, z, slab("spruce", "bottom"));
            set(x1, 6, z, lantern());
            set(x2, 6, z, lantern());
        }

        // ------------------------------------------------------------------ pas północny

        /** Depozyt: betonowy budynek z okienkiem kasowym za kratą (punkt-depozyt w ladzie). */
        private void depot() {
            int x1 = 54;
            int x2 = 66;
            int z1 = 22;
            int z2 = 32;
            walls(x1, z1, x2, z2, 1, 4, concrete);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            flatRoof(x1, z1, x2, z2, 5, true);
            patches(x1, z1, x2, z2, 1, 4, 6);
            for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                set(x1, 2, z, "iron_bars");
                set(x1, 3, z, "iron_bars");
            }
            set(x2, 3, 24, "iron_bars");
            set(x2, 3, 30, "iron_bars");
            door(x2, 1, 27, "spruce_door", "east", "left", false);
            sign(x2 + 1, 3, 27, "east", "DEPOZYT", "ODDAJESZ –", "ODBIERASZ", "NIC NIE GINIE");
            sign(x2 + 1, 3, 25, "east", "GODZINY", "OD ŚWITU", "DO ZMROKU", "");
            // lada kasowa z kratą
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                set(60, 1, z, slab("smooth_stone", "top"));
                set(60, 2, z, "iron_bars");
                set(60, 3, z, "iron_bars");
                set(60, 4, z, concrete.next());
            }
            counterMarker(60, 1, 27, "east", "punkt-depozyt", slab("smooth_stone", "top"));
            set(60, 2, 27, "air");
            set(61, 4, 27, "air");
            f.hangingLamp(63, 4, 25);
            f.hangingLamp(63, 4, 29);
            f.bench(65, 1, 23, 3, false, "east", "spruce");
            set(62, 1, 31, flowerPot());
            set(65, 1, 31, "spruce_planks");
            set(65, 2, 31, flowerPot());
            // skarbiec: beczki i sejf
            for (int x = x1 + 1; x <= 58; x++) {
                set(x, 1, z1 + 1, "barrel[facing=up,open=false]");
                set(x, 2, z1 + 1, "barrel[facing=up,open=false]");
                set(x, 1, z2 - 1, "barrel[facing=up,open=false]");
            }
            fill(x1 + 1, 1, 26, x1 + 2, 2, 28, "iron_block");
            set(x1 + 2, 2, 27, "iron_trapdoor[facing=east,half=top,open=false,powered=false,waterlogged=false]");
            f.desk(58, 1, 25, 58, 26, "dark_oak");
            f.lamp(58, 2, 25);
            f.hangingLamp(57, 4, 28);
            sandbags(x2 + 1, 22, x2 + 1, 23, 2);
            sandbags(x2 + 1, 31, x2 + 1, 32, 2);
        }

        /** Ogródek ze studnią w miejscu po budce (między drogą a radiostacją). */
        private void wellGarden() {
            int cx = 87;
            int cz = 33;
            for (int x = cx - 3; x <= cx + 3; x++) {
                for (int z = cz - 3; z <= cz + 3; z++) {
                    set(x, 0, z, Math.abs(x - cx) + Math.abs(z - cz) <= 3 ? "dirt_path" : "grass_block");
                }
            }
            // studnia
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    set(x, 1, z, x == cx && z == cz ? "water" : "cobblestone");
                    if (x == cx && z == cz) {
                        set(x, 0, z, "water");
                        set(x, -1, z, "water");
                    }
                }
            }
            set(cx - 1, 2, cz - 1, "spruce_fence");
            set(cx + 1, 2, cz - 1, "spruce_fence");
            set(cx - 1, 3, cz - 1, "spruce_fence");
            set(cx + 1, 3, cz - 1, "spruce_fence");
            fill(cx - 1, 4, cz - 1, cx + 1, 4, cz - 1, slab("spruce", "bottom"));
            set(cx, 3, cz - 1, "chain[axis=y,waterlogged=false]");
            set(cx, 2, cz - 1, "lantern[hanging=true,waterlogged=false]");
            for (int[] p : new int[][]{{cx - 3, cz - 3}, {cx + 3, cz - 3}, {cx - 3, cz + 3}, {cx + 3, cz + 3}}) {
                set(p[0], 1, p[1], flower());
            }
            for (int x = cx - 2; x <= cx + 2; x += 4) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    if (get(x, 0, z).contains("grass")) {
                        set(x, 1, z, flower());
                    }
                }
            }
            f.bench(cx - 2, 1, cz + 3, 2, true, "south", "spruce");
            post(cx + 2, 1, cz + 3, 0, "WODA", "PITNA", "PRZEGOTUJ", "MIMO WSZYSTKO");
        }

        /** Radiostacja z masztem na dachu (punkt-radio na biurku radiooperatora). */
        private void radioStation() {
            int x1 = 92;
            int x2 = 102;
            int z1 = 20;
            int z2 = 30;
            walls(x1, z1, x2, z2, 1, 4, brick);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            flatRoof(x1, z1, x2, z2, 5, false);
            patches(x1, z1, x2, z2, 1, 4, 5);
            windowsX(z2, 94, 100, 3, 2, 3);
            windowsZ(x1, 23, 27, 2, 2);
            windowsZ(x2, 23, 27, 2, 2);
            set(97, 2, z2, "air");
            set(97, 3, z2, "air");
            door(97, 1, z2, "spruce_door", "south", "left", false);
            sign(97, 4, z2 + 1, "south", "RADIOSTACJA", "„ŚWIT FM”", "NADAJEMY", "CO WIECZÓR");
            // biurko z radiem
            for (int x = 94; x <= 100; x++) {
                set(x, 1, 21, slab("dark_oak", "top"));
            }
            counterMarker(97, 1, 21, "south", "punkt-radio", slab("dark_oak", "top"));
            set(97, 2, 21, "jukebox[has_record=false]");
            set(96, 2, 21, "note_block[instrument=harp,note=0,powered=false]");
            set(98, 2, 21, "lever[face=floor,facing=south,powered=false]");
            set(99, 2, 21, candle(2));
            set(95, 2, 21, "daylight_detector[inverted=false,power=0]");
            set(94, 2, 21, "redstone_lamp[lit=false]");
            f.chair(99, 1, 22, "south", "dark_oak");
            // mapa zasięgu na ścianie, regał z płytami, prycza radiooperatora
            for (int x = 94; x <= 100; x++) {
                set(x, 3, z1, pick("white_terracotta", "white_terracotta", "lime_terracotta", "light_blue_terracotta"));
            }
            f.bookshelf(x2 - 1, 1, 24, x2 - 1, 26, 2, "west");
            f.bed(x1 + 1, 1, 28, "north", "blue");
            f.hangingLamp(97, 4, 25);
            set(x1 + 1, 1, 24, flowerPot());
            sign(x1 + 1, 2, 22, "east", "CZĘSTOTLIWOŚĆ", "ZAPASOWA", "W SZUFLADZIE", "");
            // maszt kratowy
            fill(100, 6, 23, 100, 26, 23, "iron_bars");
            set(100, 6, 23, "iron_block");
            for (int y : new int[]{14, 21}) {
                fill(99, y, 23, 101, y, 23, "iron_bars");
                fill(100, y, 22, 100, y, 24, "iron_bars");
            }
            set(100, 27, 23, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            set(99, 22, 23, "lantern[hanging=true,waterlogged=false]");
            set(101, 15, 23, "end_rod[facing=east]");
            set(99, 15, 23, "end_rod[facing=west]");
            set(94, 7, 22, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            set(94, 7, 28, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            for (int x = 94; x <= 97; x++) {
                set(x, 6, 26, "daylight_detector[inverted=false,power=0]");
            }
        }

        /** Sklep w odremontowanym budynku sztabu: lada, regały, zaplecze. */
        private void shop() {
            int x1 = 106;
            int x2 = 126;
            int z1 = 20;
            int z2 = 36;
            walls(x1, z1, x2, z2, 1, 5, plaster);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            for (int x = x1 + 1; x < x2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 6, z, "spruce_planks");
                }
            }
            gableX(x1, z1, x2, z2, 6, "spruce", plaster);
            patches(x1, z1, x2, z2, 1, 5, 8);
            // witryny i wejście
            windowsX(z2, 108, 114, 1, 2, 3);
            windowsX(z2, 119, 124, 1, 2, 3);
            windowsX(z1, 109, 123, 3, 3, 4);
            windowsZ(x1, 24, 32, 4, 2, 3);
            windowsZ(x2, 24, 32, 4, 2, 3);
            door(116, 1, z2, "spruce_door", "south", "left", false);
            door(117, 1, z2, "spruce_door", "south", "right", false);
            for (int x = 114; x <= 119; x++) {
                set(x, 4, z2 + 1, x % 2 == 0 ? "green_wool" : "white_wool");
            }
            sign(116, 5, z2 + 1, "south", "SKLEP", "KUPNO", "SPRZEDAŻ", "");
            sign(117, 5, z2 + 1, "south", "CENY", "UCZCIWE", "JAK NA", "KONIEC ŚWIATA");
            set(115, 1, z2 + 1, flowerPot());
            set(118, 1, z2 + 1, flowerPot());
            // lada; punkt-sklep w ladzie, handlarz za nią
            for (int x = 108; x <= 123; x++) {
                set(x, 1, 28, slab("spruce", "top"));
            }
            counterMarker(116, 1, 28, "south", "punkt-sklep", slab("spruce", "top"));
            marker(116, 1, 27, "south", "punkt-handlarz");
            f.lamp(110, 2, 28);
            set(121, 2, 28, "decorated_pot[cracked=false,facing=south,waterlogged=false]");
            set(113, 2, 28, "lectern[facing=south,has_book=false,powered=false]");
            // regały za ladą
            for (int x = 115; x <= 124; x++) {
                set(x, 1, 21, x % 3 == 0 ? "bookshelf" : "barrel[facing=south,open=false]");
                set(x, 2, 21, x % 2 == 0 ? "barrel[facing=south,open=false]" : "bookshelf");
                set(x, 3, 21, slab("spruce", "bottom"));
            }
            for (int x = 115; x <= 122; x++) {
                set(x, 1, 23, x % 4 == 0 ? "spruce_planks" : "barrel[facing=up,open=false]");
                set(x, 2, 23, x % 4 == 0 ? "spruce_planks" : (chance(0.5) ? flowerPot() : "air"));
                set(x, 3, 23, slab("spruce", "bottom"));
            }
            // zaplecze za ścianką z drzwiami
            fill(113, 1, z1 + 1, 113, 5, 26, plaster);
            door(113, 1, 25, "spruce_door", "east", "left", false);
            for (int x = x1 + 1; x <= 112; x++) {
                set(x, 1, z1 + 1, "barrel[facing=up,open=false]");
                set(x, 2, z1 + 1, x % 2 == 0 ? "barrel[facing=up,open=false]" : "hay_block[axis=x]");
            }
            fill(x1 + 1, 1, 24, x1 + 2, 1, 26, "stripped_oak_log[axis=z]");
            set(x1 + 1, 2, 24, "stripped_oak_log[axis=z]");
            set(111, 1, 25, "composter[level=3]");
            f.hangingLamp(110, 5, 23);
            // sala dla klientów
            f.bench(108, 1, 34, 3, true, "south", "spruce");
            f.table(123, 1, 32, "spruce");
            f.chair(122, 1, 32, "west", "spruce");
            f.chair(124, 1, 32, "east", "spruce");
            for (int x = 110; x <= 122; x += 6) {
                f.hangingLamp(x, 5, 31);
                f.hangingLamp(x, 5, 25);
            }
            sign(118, 3, 27, "south", "NIE MA", "NA KRESKĘ", "NAWET", "DLA SZWAGRA");
        }

        /** Mały lazaret: kilka łóżek, leki, kotły. */
        private void infirmary() {
            int x1 = 130;
            int x2 = 137;
            int z1 = 20;
            int z2 = 32;
            walls(x1, z1, x2, z2, 1, 4, white);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, white);
            flatRoof(x1, z1, x2, z2, 5, false);
            for (int x = 132; x <= 135; x++) {
                set(x, 6, 26, "red_concrete");
            }
            for (int z = 24; z <= 28; z++) {
                set(133, 6, z, "red_concrete");
                set(134, 6, z, "red_concrete");
            }
            patches(x1, z1, x2, z2, 1, 4, 3);
            door(133, 1, z2, "birch_door", "south", "left", false);
            fill(131, 3, z2, 131, 3, z2, "red_concrete");
            set(135, 3, z2, "red_concrete");
            sign(134, 3, z2 + 1, "south", "LAZARET", "OPATRUNKI", "ZA DARMO", "");
            windowsZ(x2, 22, 30, 4, 2, 3);
            windowsZ(x1, 22, 30, 4, 2);
            for (int z = 21; z <= 27; z += 3) {
                f.bed(136, 1, z, "east", "white");
                f.bed(136, 1, z + 1, "east", "white");
            }
            set(131, 1, 21, "brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]");
            set(132, 1, 21, "brewing_stand[has_bottle_0=false,has_bottle_1=true,has_bottle_2=false]");
            set(131, 1, 23, "water_cauldron[level=3]");
            set(131, 1, 24, "cauldron");
            f.desk(131, 1, 28, 131, 29, "birch");
            f.lamp(131, 2, 28);
            set(131, 2, 29, flowerPot());
            f.hangingLamp(133, 4, 24);
            f.hangingLamp(133, 4, 29);
            sign(x1 + 1, 2, 26, "east", "MYJ RĘCE", "NAWET", "PO KONCU", "ŚWIATA");
        }

        /** Warsztat: hala z blachy ze świetlikami, stanowiska ślusarskie, gazik w naprawie i Star na podwórzu. */
        private void workshop() {
            int x1 = 12;
            int x2 = 32;
            int z1 = 20;
            int z2 = 34;
            walls(x1, z1, x2, z2, 1, 6, sheet);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean sky = (x - x1) % 5 == 2 && z > z1 + 1 && z < z2 - 1;
                    set(x, 7, z, sky ? "glass" : chance(0.85) ? "light_gray_concrete" : "iron_block");
                }
            }
            // ząbkowany dach z blachy
            for (int x = x1; x <= x2; x++) {
                if ((x - x1) % 5 == 0) {
                    for (int z = z1; z <= z2; z++) {
                        set(x, 8, z, "iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
                    }
                }
            }
            for (int x = x1 + 2; x <= x2 - 2; x += 3) {
                set(x, 8, z1 + 1, "daylight_detector[inverted=false,power=0]");
            }
            patches(x1, z1, x2, z2, 1, 6, 6);
            fill(18, 1, z2, 24, 4, z2, "air");
            fill(17, 5, z2, 25, 5, z2, "yellow_concrete");
            door(x2, 1, 27, "spruce_door", "east", "left", false);
            windowsX(z1, 14, 30, 4, 4, 5);
            windowsZ(x1, 23, 31, 4, 4, 5);
            sign(16, 4, z2 + 1, "south", "WARSZTAT", "NAPRAWIAMY", "WSZYSTKO", "POZA SERCEM");
            // stanowiska wzdłuż północnej ściany
            set(14, 1, 21, "crafting_table");
            set(15, 1, 21, "crafting_table");
            set(17, 1, 21, "anvil[facing=east]");
            set(19, 1, 21, "smithing_table");
            set(20, 1, 21, "grindstone[face=floor,facing=south]");
            set(22, 1, 21, "stonecutter[facing=south]");
            set(23, 1, 21, "fletching_table");
            set(25, 1, 21, "blast_furnace[facing=south,lit=true]");
            set(26, 1, 21, "furnace[facing=south,lit=true]");
            fill(27, 1, 21, 31, 1, 21, slab("spruce", "top"));
            set(28, 2, 21, "lantern[hanging=false,waterlogged=false]");
            set(30, 2, 21, "lever[face=floor,facing=south,powered=false]");
            set(14, 1, 23, "anvil[facing=north]");
            set(x1 + 1, 1, 26, "smithing_table");
            set(x1 + 1, 1, 27, "grindstone[face=floor,facing=east]");
            set(x1 + 1, 1, 28, "crafting_table");
            // narzędzia na ścianie (klapy) i stosy opon
            for (int x = 14; x <= 26; x += 3) {
                set(x, 3, z1 + 1, trapdoor("spruce", "south", "top", true));
            }
            for (int z = 30; z <= 32; z++) {
                set(x2 - 1, 1, z, "blackstone_wall");
                set(x2 - 1, 2, z, "blackstone_wall");
            }
            // gazik na podnośniku z otwartą maską
            uaz(21, 26, false, true);
            for (int x = 21; x <= 24; x++) {
                set(x, 4, 25, "iron_block");
            }
            fill(22, 5, 26, 22, 6, 26, "chain[axis=y,waterlogged=false]");
            set(26, 1, 26, "cauldron");
            set(26, 1, 27, "water_cauldron[level=1]");
            for (int x = 15; x <= 29; x += 7) {
                set(x, 6, 24, hanging());
                set(x, 6, 30, hanging());
            }
            // podwórze: Star z nowymi blachami, palety, opony
            for (int x = x1; x <= 38; x++) {
                for (int z = 35; z <= 40; z++) {
                    set(x, 0, z, chance(0.85) ? "smooth_stone" : "light_gray_concrete");
                }
            }
            int sx = 26;
            for (int z = 36; z <= 38; z++) {
                set(sx, 1, z, "polished_blackstone");
                fill(sx + 1, 1, z, sx + 2, 2, z, fresh);
                fill(sx + 3, 1, z, sx + 4, 3, z, fresh);
                for (int x = sx + 5; x <= sx + 10; x++) {
                    set(x, 1, z, "spruce_planks");
                }
            }
            set(sx + 3, 2, 37, "glass_pane");
            fill(sx + 6, 2, 36, sx + 9, 2, 38, "stripped_oak_log[axis=x]");
            set(sx + 5, 2, 36, "spruce_fence");
            set(sx + 10, 2, 38, "spruce_fence");
            // materiały: kamienne cegły na paletach, belki
            fill(14, 1, 37, 16, 2, 38, "stone_bricks");
            set(14, 3, 37, slab("stone_brick", "bottom"));
            fill(18, 1, 37, 21, 1, 38, "stripped_spruce_log[axis=x]");
            fill(18, 2, 37, 20, 2, 37, "stripped_spruce_log[axis=x]");
            set(23, 1, 39, "blackstone_wall");
            set(23, 2, 39, "blackstone_wall");
            set(24, 1, 39, "blackstone_wall");
            lampPost(13, 40);
        }

        /** Szopa z agregatem i wiatrak na maszcie. */
        private void generator() {
            int x1 = 36;
            int x2 = 44;
            int z1 = 20;
            int z2 = 28;
            walls(x1, z1, x2, z2, 1, 4, brick);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    set(x, 5, z, chance(0.8) ? trapdoor("iron", "north", "bottom", false)
                            : slab("waxed_cut_copper", "bottom"));
                }
            }
            for (int x = x1 + 1; x < x2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 5, z, "smooth_stone");
                }
            }
            door(40, 1, z2, "spruce_door", "south", "left", false);
            set(38, 3, z2, "iron_bars");
            set(42, 3, z2, "iron_bars");
            sign(40, 3, z2 + 1, "south", "AGREGAT", "NIE DOTYKAĆ", "BO PRĄDU", "NIE BĘDZIE");
            fill(38, 1, 22, 42, 2, 24, "iron_block");
            set(40, 3, 23, "observer[facing=up,powered=false]");
            set(37, 1, 23, "furnace[facing=west,lit=true]");
            set(43, 1, 23, "furnace[facing=east,lit=true]");
            set(37, 1, 26, "barrel[facing=up,open=false]");
            set(43, 1, 26, "barrel[facing=up,open=false]");
            set(37, 2, 26, "barrel[facing=up,open=false]");
            set(40, 4, 26, hanging());
            set(x1 + 1, 2, 21, "lever[face=wall,facing=east,powered=false]");
            // beczki z paliwem przed szopą
            for (int x = 45; x <= 45; x++) {
                set(x, 1, 21, "barrel[facing=up,open=false]");
                set(x, 1, 22, "barrel[facing=up,open=false]");
                set(x, 2, 21, "barrel[facing=up,open=false]");
            }
            // wiatrak
            int mx = 40;
            int mz = 33;
            fill(mx, 1, mz, mx, 15, mz, "andesite_wall");
            fill(mx - 1, 0, mz - 1, mx + 1, 0, mz + 1, "polished_andesite");
            set(mx, 16, mz, "iron_block");
            set(mx, 16, mz - 1, "iron_block");
            set(mx, 16, mz + 1, "light_gray_concrete");
            for (int i = 1; i <= 4; i++) {
                set(mx, 16 + i, mz - 1, "white_concrete");
                set(mx - i, 16 - i, mz - 1, "white_concrete");
                set(mx + i, 16 - i, mz - 1, "white_concrete");
            }
            set(mx, 17, mz + 1, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            post(mx + 2, 1, mz + 1, 0, "WIATRAK", "ZBUDOWAŁ", "INŻYNIER", "MIETEK");
        }

        // ------------------------------------------------------------------ targowisko

        private void plaza() {
            int x1 = 50;
            int x2 = 98;
            int z1 = 46;
            int z2 = 90;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 0, z, paving.next());
                }
            }
            // róża wiatrów pośrodku
            int cx = 74;
            int cz = 74;
            for (int x = cx - 8; x <= cx + 9; x++) {
                for (int z = cz - 8; z <= cz + 9; z++) {
                    double d = Math.hypot(x - cx - 0.5, z - cz - 0.5);
                    if (Math.abs(d - 8) < 0.6) {
                        set(x, 0, z, "polished_andesite");
                    } else if ((x == cx || x == cx + 1 || z == cz || z == cz + 1) && d < 8) {
                        set(x, 0, z, "smooth_stone");
                    }
                }
            }
            fountain(72, 72);
            // 8 stanowisk wymiany: dwa rzędy po cztery
            int n = 1;
            for (int z0 : new int[]{49, 85}) {
                for (int x0 : new int[]{52, 59, 86, 93}) {
                    tradeBooth(x0, z0, n++);
                }
            }
            // kramy z towarem
            stall(52, 57, "red", "white");
            stall(60, 57, "yellow", "green");
            stall(52, 76, "blue", "white");
            stall(60, 76, "orange", "brown");
            stall(86, 57, "lime", "white");
            stall(93, 57, "red", "yellow");
            stall(86, 76, "cyan", "white");
            stall(93, 76, "purple", "white");
            jobBoard();
            // drzewka w donicach w rogach
            for (int[] t : new int[][]{{51, 47}, {97, 47}, {51, 89}, {97, 89}}) {
                for (int x = t[0] - 1; x <= t[0] + 1; x++) {
                    for (int z = t[1] - 1; z <= t[1] + 1; z++) {
                        set(x, 0, z, x == t[0] && z == t[1] ? "grass_block" : "stone_bricks");
                        if (!(x == t[0] && z == t[1])) {
                            set(x, 1, z, slab("stone_brick", "bottom"));
                        }
                    }
                }
                tree(t[0], t[1], "oak");
            }
            // ławki i donice wokół fontanny
            f.bench(68, 1, 69, 3, true, "north", "spruce");
            f.bench(79, 1, 80, 3, true, "south", "spruce");
            f.bench(68, 1, 80, 3, true, "south", "spruce");
            f.bench(79, 1, 69, 3, true, "north", "spruce");
            for (int[] p : new int[][]{{67, 69}, {71, 69}, {78, 69}, {82, 69}, {67, 80}, {71, 80}, {78, 80}, {82, 80}}) {
                set(p[0], 1, p[1], flowerPot());
            }
            // dzwon na ogłoszenia
            set(66, 1, 64, "stone_bricks");
            set(66, 2, 64, "bell[attachment=floor,facing=north,powered=false]");
            // latarnie na placu
            for (int[] p : new int[][]{{50, 55}, {50, 66}, {50, 81}, {98, 55}, {98, 66}, {98, 81}, {69, 47}, {80, 47},
                    {69, 89}, {80, 89}, {66, 74}, {83, 74}}) {
                lampPost(p[0], p[1]);
            }
            // owoce i warzywa w stosach, wózek, stoliki pod drzewami
            set(58, 1, 66, "hay_block[axis=y]");
            set(59, 1, 66, "pumpkin");
            set(58, 1, 67, "melon");
            set(58, 2, 66, "pumpkin");
            set(91, 1, 66, "barrel[facing=up,open=false]");
            set(90, 1, 66, "hay_block[axis=x]");
            set(90, 1, 67, "composter[level=8]");
            for (int[] t : new int[][]{{55, 68}, {93, 68}}) {
                for (int x = t[0] - 1; x <= t[0] + 1; x++) {
                    for (int z = t[1] - 1; z <= t[1] + 1; z++) {
                        set(x, 0, z, x == t[0] && z == t[1] ? "grass_block" : "stone_bricks");
                        if (!(x == t[0] && z == t[1])) {
                            set(x, 1, z, slab("stone_brick", "bottom"));
                        }
                    }
                }
                tree(t[0], t[1], "birch");
            }
            for (int[] t : new int[][]{{61, 70}, {87, 70}}) {
                f.table(t[0], 1, t[1], "spruce");
                f.chair(t[0] - 1, 1, t[1], "west", "spruce");
                f.chair(t[0] + 1, 1, t[1], "east", "spruce");
                f.chair(t[0], 1, t[1] + 1, "south", "spruce");
            }
            // wózek z lemoniadą
            fill(66, 1, 52, 68, 1, 53, "spruce_planks");
            set(65, 1, 52, "polished_blackstone");
            set(65, 1, 53, "polished_blackstone");
            set(69, 1, 52, "polished_blackstone");
            set(69, 1, 53, "polished_blackstone");
            set(66, 2, 52, "water_cauldron[level=3]");
            set(68, 2, 53, flowerPot());
            fill(66, 3, 52, 68, 3, 53, "yellow_wool");
            set(66, 2, 53, "spruce_fence");
            set(68, 2, 52, "spruce_fence");
            post(67, 1, 54, 0, "LEMONIADA", "Z MIĘTĄ", "ZA UŚMIECH", "");
            marker(74, 1, 57, "north", "npc-ochroniarz");
            marker(66, 1, 84, "south", "npc-ochroniarz");
            post(84, 1, 47, 8, "TARGOWISKO", "HANDLUJ", "UCZCIWIE", "");
        }

        private void fountain(int x0, int z0) {
            for (int x = x0; x <= x0 + 5; x++) {
                for (int z = z0; z <= z0 + 5; z++) {
                    boolean rim = x == x0 || x == x0 + 5 || z == z0 || z == z0 + 5;
                    if (rim) {
                        set(x, 1, z, "polished_andesite");
                        set(x, 0, z, "stone_bricks");
                    } else {
                        set(x, 0, z, "water");
                        set(x, -1, z, "stone_bricks");
                    }
                }
            }
            fill(x0 + 2, 0, z0 + 2, x0 + 3, 2, z0 + 3, "mossy_stone_bricks");
            set(x0 + 2, 3, z0 + 2, lantern());
            set(x0 + 3, 3, z0 + 3, lantern());
            set(x0 + 3, 3, z0 + 2, "potted_red_tulip");
            set(x0 + 2, 3, z0 + 3, "potted_cornflower");
            for (int x = x0 + 1; x <= x0 + 4; x++) {
                set(x, 0, z0 + 1, "water");
            }
            set(x0, 2, z0, candle(3));
            set(x0 + 5, 2, z0 + 5, candle(2));
        }

        /**
         * Stanowisko wymiany 5 × 3: lada w środkowym rzędzie (punkt-handel w środku lady), po obu stronach
         * miejsce dla gracza, nad wszystkim kolorowy daszek na dwóch słupach.
         */
        private void tradeBooth(int x0, int z0, int number) {
            String a = woolColor();
            String b = "white";
            for (int x = x0; x <= x0 + 4; x++) {
                for (int z = z0; z <= z0 + 2; z++) {
                    set(x, 0, z, (x + z) % 2 == 0 ? "polished_andesite" : "smooth_stone");
                    set(x, 4, z, ((x - x0) % 2 == 0 ? a : b) + "_wool");
                }
            }
            for (int x = x0; x <= x0 + 4; x++) {
                set(x, 5, z0 + 1, ((x - x0) % 2 == 0 ? a : b) + "_carpet");
            }
            fill(x0, 1, z0 + 1, x0, 3, z0 + 1, "stripped_spruce_log[axis=y]");
            fill(x0 + 4, 1, z0 + 1, x0 + 4, 3, z0 + 1, "stripped_spruce_log[axis=y]");
            set(x0 + 1, 1, z0 + 1, slab("smooth_stone", "top"));
            set(x0 + 3, 1, z0 + 1, slab("smooth_stone", "top"));
            set(x0 + 1, 2, z0 + 1, lantern());
            set(x0 + 3, 2, z0 + 1, flowerPot());
            counterMarker(x0 + 2, 1, z0 + 1, "south", "punkt-handel", slab("smooth_stone", "top"));
            sign(x0 - 1, 2, z0 + 1, "west", "STANOWISKO", "WYMIANY", "NR " + number, "");
            sign(x0 + 5, 2, z0 + 1, "east", "WYMIANA", "BEZ OSZUSTW", "STRAŻ", "PATRZY");
        }

        /** Kram z towarem (bez skrzyń): 5 × 4, słupy w rogach, pasiasty daszek, lada z beczek i siana. */
        private void stall(int x0, int z0, String a, String b) {
            for (int[] p : new int[][]{{x0, z0}, {x0 + 4, z0}, {x0, z0 + 3}, {x0 + 4, z0 + 3}}) {
                fill(p[0], 1, p[1], p[0], 3, p[1], "spruce_fence");
            }
            for (int x = x0; x <= x0 + 4; x++) {
                for (int z = z0; z <= z0 + 3; z++) {
                    set(x, 4, z, (z - z0) % 2 == 0 ? a + "_wool" : b + "_wool");
                }
            }
            String[] goods = {"barrel[facing=up,open=false]", "hay_block[axis=y]", "composter[level=8]", "melon",
                    "pumpkin", "barrel[facing=up,open=false]", "decorated_pot[cracked=false,facing=north,waterlogged=false]"};
            for (int x = x0 + 1; x <= x0 + 3; x++) {
                set(x, 1, z0 + 1, goods[rnd.nextInt(goods.length)]);
                if (chance(0.5)) {
                    set(x, 2, z0 + 1, chance(0.5) ? flowerPot() : candle(1 + rnd.nextInt(3)));
                }
                set(x, 1, z0 + 3, x == x0 + 2 ? "air" : goods[rnd.nextInt(goods.length)]);
            }
            set(x0 + 2, 3, z0 + 2, hanging());
        }

        /** Tablica zleceń na środku placu, z daszkiem; punkt-zlecenia przed tablicą. */
        private void jobBoard() {
            int x1 = 70;
            int x2 = 78;
            int z = 64;
            fill(x1, 1, z, x1, 4, z, "spruce_log[axis=y]");
            fill(x2, 1, z, x2, 4, z, "spruce_log[axis=y]");
            fill(x1 + 1, 1, z, x2 - 1, 3, z, "dark_oak_planks");
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                set(x, 4, z - 1, stairs("spruce", "south"));
                set(x, 4, z + 1, stairs("spruce", "north"));
                set(x, 5, z, slab("spruce", "bottom"));
            }
            fill(x1 + 1, 4, z, x2 - 1, 4, z, "spruce_planks");
            String[][] jobs = {
                    {"ZLECENIA", "– PISZ –", "PODPISUJ", "ZOSTAW"},
                    {"POTRZEBNE", "ŻELAZO", "DO MURU", "PŁACI RADA"},
                    {"KTO PRZYNIESIE", "NASIONA", "MARCHWI", "DOSTANIE OBIAD"},
                    {"SZUKAM", "EKIPY", "NA SZPITAL", "– ROMEK"},
                    {"ZGINĄŁ PIES", "BURY", "WABI SIĘ", "KAPSEL"},
                    {"POMOC", "PRZY DACHU", "KAPLICY", "W SOBOTĘ"},
                    {"KUPIĘ", "AMUNICJĘ", "9 MM", "STANOWISKO 3"},
                    {"NOCNE WARTY", "CHĘTNI", "DO KAPRALA", "ZDZISKA"},
                    {"OGNISKO", "W PIĄTEK", "GITARA", "MILE WIDZIANA"},
                    {"ZNALEZIONO", "KLUCZE", "ODBIÓR", "W DEPOZYCIE"},
                    {"NIE PŁACZ", "ŻE SIĘ", "SKOŃCZYŁO", "CIESZ SIĘ ŻE ŻYJESZ"},
                    {"RADIO", "ŚWIT FM", "CO WIECZÓR", "O ZMROKU"},
                    {"PRANIE", "PANI HALINY", "NIE RUSZAĆ", ""},
                    {"WYMIENIĘ", "SKÓRY", "NA CHLEB", "STANOWISKO 6"}};
            int i = 0;
            for (int y = 3; y >= 1 && i < jobs.length; y--) {
                for (int x = x1 + 1; x <= x2 - 1 && i < jobs.length; x++) {
                    sign(x, y, z - 1, "north", jobs[i++]);
                }
            }
            for (int x = x1 + 1; x <= x2 - 1; x++) {
                set(x, 3, z + 1, x % 2 == 0 ? "white_terracotta" : "air");
            }
            marker(74, 1, 61, "south", "punkt-zlecenia");
            set(x1 - 1, 1, z - 2, lantern());
            set(x2 + 1, 1, z - 2, lantern());
        }

        // ------------------------------------------------------------------ pas zachodni

        /** Noclegownia w dawnych koszarach: dwie kondygnacje pokoi z łóżkami, nowy dach. */
        private void dorms() {
            int x1 = 12;
            int x2 = 40;
            int z1 = 46;
            int z2 = 62;
            walls(x1, z1, x2, z2, 1, 9, olive);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean inside = x > x1 && x < x2 && z > z1 && z < z2;
                    set(x, 5, z, inside ? (chance(0.9) ? "spruce_planks" : "oak_planks") : "stone_bricks");
                    if (inside) {
                        set(x, 10, z, "spruce_planks");
                    }
                }
            }
            gableX(x1, z1, x2, z2, 10, "spruce", olive);
            // kilka łat z dawnego dachu z ciemnego drewna
            for (int i = 0; i < 10; i++) {
                int x = x1 + rnd.nextInt(x2 - x1);
                int k = rnd.nextInt(6);
                set(x, 10 + k, z1 + k, stairs("dark_oak", "south"));
            }
            fill(32, 15, 54, 32, 19, 54, "bricks");
            set(32, 20, 54, campfire("north"));
            patches(x1, z1, x2, z2, 1, 9, 14);
            int wallN = 52;
            int wallS = 56;
            for (int h : new int[]{0, 5}) {
                fill(x1 + 1, h + 1, wallN, x2 - 1, h + 4, wallN, olive);
                fill(x1 + 1, h + 1, wallS, x2 - 1, h + 4, wallS, olive);
                for (int px : new int[]{19, 26, 33}) {
                    fill(px, h + 1, z1 + 1, px, h + 4, wallN - 1, olive);
                    fill(px, h + 1, wallS + 1, px, h + 4, z2 - 1, olive);
                }
                int[][] rooms = {{13, 18}, {20, 25}, {27, 32}, {34, 39}};
                for (int[] r : rooms) {
                    int dx = r[0] + 3;
                    door(dx, h + 1, wallN, "spruce_door", "south", "left", false);
                    door(dx, h + 1, wallS, "spruce_door", "north", "left", false);
                    for (boolean north : new boolean[]{true, false}) {
                        int head = north ? z1 + 1 : z2 - 1;
                        int foot = north ? z1 + 2 : z2 - 2;
                        String facing = north ? "north" : "south";
                        String[] colors = {"red", "blue", "yellow", "green", "light_blue", "orange", "lime", "pink", "brown"};
                        f.bed(r[0], h + 1, foot, facing, colors[rnd.nextInt(colors.length)]);
                        f.bed(r[1], h + 1, foot, facing, colors[rnd.nextInt(colors.length)]);
                        f.wardrobe(r[0] + 2, h + 1, head, north ? "south" : "north", "spruce");
                        set(r[0] + 4, h + 1, head, "spruce_planks");
                        f.lamp(r[0] + 4, h + 2, head);
                        set(r[0] + 1, h + 1, north ? wallN - 1 : wallS + 1, chance(0.5) ? flowerPot() : woolColor() + "_carpet");
                        f.hangingLamp(r[0] + 2, h + 4, north ? 50 : 58);
                    }
                }
                windowsX(z1, 14, 38, 2, h + 2, h + 3);
                windowsX(z2, 14, 38, 2, h + 2, h + 3);
                for (int x = 14; x <= 38; x += 6) {
                    f.hangingLamp(x, h + 4, 54);
                }
            }
            // drzwi wejściowe na końcach korytarza i od południa
            door(x1, 1, 54, "spruce_door", "west", "left", false);
            door(x2, 1, 54, "spruce_door", "east", "left", false);
            // schody na piętro w korytarzu
            for (int i = 0; i < 4; i++) {
                int x = 14 + i;
                for (int y = 1; y <= i; y++) {
                    set(x, y, 55, "spruce_planks");
                }
                set(x, i + 1, 55, stairs("spruce", "east"));
            }
            fill(13, 5, 55, 17, 5, 55, "air");
            fill(13, 6, 55, 17, 6, 55, "air");
            for (int x = 13; x <= 17; x++) {
                set(x, 6, 54, "spruce_fence");
            }
            set(13, 1, 53, "potted_fern");
            sign(x1 - 1, 3, 54, "west", "NOCLEGOWNIA", "ŁÓŻKO ZA", "UŚMIECH", "I CISZĘ");
            sign(x2 + 1, 3, 54, "east", "NOCLEGOWNIA", "CISZA NOCNA", "OD 22:00", "");
            sign(25, 2, 53, "south", "BUTY", "ZOSTAW", "PRZY", "DRZWIACH");
            // donice pod oknami
            for (int x = 14; x <= 38; x += 2) {
                set(x, 1, z2 + 1, chance(0.7) ? flowerPot() : "air");
            }
        }

        /** Sznury z praniem przy noclegowni: płotek na słupkach z kolorowymi dywanami. */
        private void laundry() {
            for (int line : new int[]{42, 44}) {
                fill(line, 1, 48, line, 3, 48, "spruce_fence");
                fill(line, 1, 60, line, 3, 60, "spruce_fence");
                for (int z = 49; z <= 59; z++) {
                    set(line, 3, z, "spruce_fence");
                    if (chance(0.75)) {
                        set(line, 4, z, woolColor() + "_carpet");
                    }
                }
            }
            set(43, 1, 49, "composter[level=0]");
            set(43, 1, 59, "water_cauldron[level=2]");
            post(43, 1, 47, 8, "PRANIE", "PANI HALINY", "NIE", "RUSZAĆ!");
        }

        /** Ogrody warzywne: pola z kanałami nawadniającymi, kompostowniki, ule, strach na wróble. */
        private void gardens() {
            int x1 = 12;
            int x2 = 30;
            int z1 = 68;
            int z2 = 90;
            String[] crops = {"wheat[age=7]", "carrots[age=7]", "potatoes[age=7]", "beetroots[age=3]"};
            int band = 0;
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                boolean water = (z - z1) % 5 == 0;
                if (water) {
                    band++;
                }
                for (int x = x1 + 1; x <= x2 - 1; x++) {
                    if (x == 21) {
                        set(x, 0, z, water ? "spruce_planks" : "dirt_path");
                        continue;
                    }
                    if (water) {
                        set(x, 0, z, "water");
                        set(x, -1, z, "dirt");
                    } else {
                        set(x, 0, z, "farmland[moisture=7]");
                        set(x, 1, z, crops[(band + (x > 21 ? 1 : 0)) % crops.length]);
                    }
                }
            }
            // płotek z przejściami
            for (int x = x1; x <= x2; x++) {
                if (x < 20 || x > 22) {
                    set(x, 1, z1, "oak_fence");
                    set(x, 1, z2, "oak_fence");
                }
            }
            for (int z = z1; z <= z2; z++) {
                if (z < 78 || z > 80) {
                    set(x1, 1, z, "oak_fence");
                }
                set(x2, 1, z, "oak_fence");
            }
            for (int z = 78; z <= 80; z++) {
                set(x1, 0, z, "dirt_path");
                for (int x = x1 + 1; x <= 20; x++) {
                    if (z == 79) {
                        set(x, 0, z, "dirt_path");
                        set(x, 1, z, "air");
                    }
                }
            }
            // strach na wróble
            set(25, 1, 77, "oak_fence");
            set(25, 2, 77, "hay_block[axis=y]");
            set(25, 3, 77, "carved_pumpkin[facing=north]");
            set(24, 2, 77, "oak_fence");
            set(26, 2, 77, "oak_fence");
            // kompostowniki i ule przy wejściu
            for (int x = 13; x <= 18; x++) {
                set(x, 1, 67, x % 2 == 0 ? "composter[level=" + (rnd.nextInt(8)) + "]" : "air");
            }
            for (int x = 24; x <= 28; x += 2) {
                set(x, 1, 67, "spruce_fence");
                set(x, 2, 67, "beehive[facing=north,honey_level=" + rnd.nextInt(6) + "]");
            }
            set(21, 2, z1, "air");
            post(22, 1, 67, 8, "OGRÓD", "WSPÓLNY", "ZRYWASZ –", "SADZISZ");
            set(19, 1, 67, lantern());
            set(23, 1, 67, lantern());
            set(19, 1, 91, lantern());
            set(23, 1, 91, lantern());
            set(11, 1, 77, lantern());
            set(11, 1, 81, lantern());
        }

        /** Szklarnia z drewnianą ramą i szklanym dachem. */
        private void greenhouse() {
            int x1 = 33;
            int x2 = 44;
            int z1 = 68;
            int z2 = 80;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                    if (edge) {
                        boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                        boolean frame = corner || (z == z1 || z == z2) && (x - x1) % 4 == 0 || (x == x1 || x == x2) && (z - z1) % 4 == 0;
                        for (int y = 1; y <= 4; y++) {
                            set(x, y, z, frame ? "stripped_spruce_log[axis=y]" : y == 1 ? "spruce_planks" : "glass");
                        }
                    } else {
                        set(x, 0, z, "coarse_dirt");
                    }
                }
            }
            for (int k = 0; x1 + k <= x2 - k; k++) {
                int y = 5 + k;
                for (int z = z1; z <= z2; z++) {
                    boolean rib = (z - z1) % 4 == 0;
                    set(x1 + k, y, z, rib ? "stripped_spruce_log[axis=z]" : "glass");
                    set(x2 - k, y, z, rib ? "stripped_spruce_log[axis=z]" : "glass");
                }
                for (int x = x1 + k + 1; x <= x2 - k - 1; x++) {
                    set(x, y, z1, "glass");
                    set(x, y, z2, "glass");
                }
            }
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                for (int x : new int[]{34, 35, 36, 41, 42, 43}) {
                    set(x, 0, z, "farmland[moisture=7]");
                    set(x, 1, z, z % 3 == 0 ? "carrots[age=7]" : z % 3 == 1 ? "potatoes[age=7]" : "beetroots[age=3]");
                }
                set(37, 0, z, "water");
                set(40, 0, z, "water");
                set(38, 0, z, "spruce_planks");
                set(39, 0, z, "spruce_planks");
            }
            for (int x : new int[]{34, 43}) {
                set(x, 1, z1 + 1, "sweet_berry_bush[age=3]");
                set(x, 1, z1 + 2, "sweet_berry_bush[age=2]");
            }
            set(35, 1, z2 - 1, "melon");
            set(42, 1, z2 - 1, "pumpkin");
            set(38, 1, z1 + 1, "spruce_planks");
            set(39, 1, z1 + 1, "spruce_planks");
            set(38, 2, z1 + 1, flowerPot());
            set(39, 2, z1 + 1, flowerPot());
            set(38, 4, 74, hanging());
            set(39, 4, 74, hanging());
            fill(38, 5, 74, 39, 5, 74, "glass");
            door(38, 1, z2, "spruce_door", "south", "left", false);
            door(39, 1, z2, "spruce_door", "south", "right", false);
            set(38, 3, z2, "stripped_spruce_log[axis=x]");
            set(39, 3, z2, "stripped_spruce_log[axis=x]");
            sign(37, 3, z2 + 1, "south", "SZKLARNIA", "ZAMYKAJ", "DRZWI", "– CIEPŁO UCIEKA");
        }

        /** Zagrody: płoty, siano, koryta z wodą i wiata. */
        private void pens() {
            int x1 = 33;
            int x2 = 45;
            int z1 = 83;
            int z2 = 90;
            for (int x = x1; x <= x2; x++) {
                set(x, 1, z1, "spruce_fence");
                set(x, 1, z2, "spruce_fence");
            }
            for (int z = z1; z <= z2; z++) {
                set(x1, 1, z, "spruce_fence");
                set(x2, 1, z, "spruce_fence");
                set(39, 1, z, "spruce_fence");
            }
            set(x2, 1, 86, "spruce_fence_gate[facing=east,in_wall=false,open=true,powered=false]");
            set(x2, 1, 87, "spruce_fence_gate[facing=east,in_wall=false,open=true,powered=false]");
            for (int x = x1 + 1; x < x2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 0, z, chance(0.5) ? "coarse_dirt" : chance(0.5) ? "podzol" : "grass_block");
                }
            }
            set(34, 1, 84, "hay_block[axis=x]");
            set(35, 1, 84, "hay_block[axis=x]");
            set(34, 2, 84, "hay_block[axis=x]");
            set(37, 1, 89, "water_cauldron[level=3]");
            set(41, 1, 89, "water_cauldron[level=2]");
            set(43, 1, 84, "hay_block[axis=z]");
            set(43, 1, 85, "composter[level=6]");
            // wiata
            fill(40, 1, 84, 40, 3, 84, "spruce_fence");
            fill(44, 1, 84, 44, 3, 84, "spruce_fence");
            fill(40, 4, 84, 44, 4, 85, slab("spruce", "bottom"));
            set(42, 3, 84, hanging());
            post(46, 1, 82, 4, "ZAGRODA", "KURY I KOZY", "WRÓCĄ", "NA WIOSNĘ");
        }

        // ------------------------------------------------------------------ pas południowy

        /** Kaplica-świetlica w remoncie: rusztowanie, wiadra z farbą, betoniarka i materiały. */
        private void chapel() {
            int x1 = 12;
            int x2 = 28;
            int z1 = 98;
            int z2 = 116;
            walls(x1, z1, x2, z2, 1, 6, brick);
            floor(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 0, paving);
            for (int x = x1 + 1; x < x2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 7, z, "spruce_planks");
                }
            }
            gableZ(x1, z1, x2, z2, 7, "brick", brick);
            // połowa dachu jeszcze stara (ciemna), druga nowa
            for (int k = 0; k < 8; k++) {
                for (int z = 108; z <= z2 + 1; z++) {
                    if (chance(0.6)) {
                        set(x2 - k, 7 + k, z, stairs("dark_oak", "west"));
                    }
                }
            }
            patches(x1, z1, x2, z2, 1, 6, 8);
            // wieżyczka z dzwonem nad wejściem
            walls(18, 95, 22, 99, 1, 13, brick);
            fill(19, 1, 95, 21, 6, 95, "air");
            fill(19, 0, 96, 21, 0, 98, "stone_bricks");
            fill(19, 1, 99, 21, 6, 99, "air");
            for (int x = 18; x <= 22; x++) {
                for (int z = 95; z <= 99; z++) {
                    set(x, 14, z, slab("stone_brick", "bottom"));
                }
            }
            fill(19, 10, 95, 21, 12, 95, "air");
            fill(19, 10, 99, 21, 12, 99, "air");
            fill(18, 10, 96, 18, 12, 98, "air");
            fill(22, 10, 96, 22, 12, 98, "air");
            set(20, 9, 97, "stone_bricks");
            set(20, 13, 97, "bell[attachment=ceiling,facing=north,powered=false]");
            set(20, 15, 97, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            door(20, 1, z1, "spruce_door", "north", "left", false);
            sign(20, 3, 94, "north", "KAPLICA", "I ŚWIETLICA", "ZEBRANIA RADY", "W NIEDZIELĘ");
            set(20, 3, 95, "bricks");
            // wysokie okna
            for (int z = 101; z <= 113; z += 3) {
                for (int y = 2; y <= 4; y++) {
                    String g = y == 4 ? "yellow_stained_glass_pane" : "glass_pane";
                    set(x1, y, z, g);
                    set(x2, y, z, chance(0.3) ? "spruce_planks" : g);
                }
            }
            // ławki, ołtarz, świece
            for (int z = 101; z <= 110; z += 2) {
                f.bench(14, 1, z, 4, true, "north", "spruce");
                f.bench(23, 1, z, 4, true, "north", "spruce");
            }
            fill(17, 1, 114, 23, 1, 114, "smooth_quartz");
            set(20, 1, 113, "lectern[facing=north,has_book=false,powered=false]");
            for (int x = 17; x <= 23; x += 2) {
                set(x, 2, 114, candle(1 + rnd.nextInt(4)));
            }
            fill(20, 3, 115, 20, 5, 115, "spruce_planks");
            fill(19, 4, 115, 21, 4, 115, "spruce_planks");
            f.hangingLamp(20, 6, 104);
            f.hangingLamp(20, 6, 110);
            set(13, 1, 115, flowerPot());
            set(27, 1, 115, flowerPot());
            // rusztowanie na wschodniej ścianie
            for (int z = 100; z <= 114; z++) {
                for (int y = 1; y <= 7; y++) {
                    if (z % 3 == 1 || y == 4 || y == 7) {
                        set(29, y, z, "scaffolding[bottom=false,distance=0,waterlogged=false]");
                    }
                }
            }
            set(30, 1, 101, "water_cauldron[level=3]");
            set(30, 1, 103, "cauldron");
            set(29, 8, 106, "cauldron");
            set(30, 1, 113, "water_cauldron[level=1]");
            for (int y = 1; y <= 7; y++) {
                set(30, y, 108, ladder("east"));
            }
            set(30, 0, 108, "stone_bricks");
            // betoniarka i materiały na zapleczu
            set(25, 1, 120, "light_gray_concrete");
            set(26, 1, 120, "polished_blackstone");
            set(25, 2, 120, "barrel[facing=east,open=true]");
            set(26, 2, 120, "orange_terracotta");
            set(24, 2, 120, trapdoor("iron", "west", "top", true));
            fill(28, 1, 119, 30, 2, 120, "stone_bricks");
            fill(28, 1, 122, 31, 1, 123, "stripped_oak_log[axis=x]");
            fill(28, 2, 122, 30, 2, 123, "stripped_oak_log[axis=x]");
            set(24, 1, 123, "white_concrete");
            set(24, 2, 123, "light_gray_concrete");
            set(23, 1, 123, "white_concrete");
            sign(12 - 1, 3, 107, "west", "REMONT", "DACHU", "POMOC MILE", "WIDZIANA");
        }

        /** Odnowiona wieża ciśnień na czterech nogach. */
        private void waterTower() {
            int cx = 17;
            int cz = 122;
            for (int[] leg : new int[][]{{cx - 2, cz - 2}, {cx + 2, cz - 2}, {cx - 2, cz + 2}, {cx + 2, cz + 2}}) {
                fill(leg[0], 1, leg[1], leg[0], 10, leg[1], "andesite_wall");
            }
            for (int y = 11; y <= 16; y++) {
                for (int x = cx - 3; x <= cx + 3; x++) {
                    for (int z = cz - 3; z <= cz + 3; z++) {
                        double d = Math.hypot(x - cx, z - cz);
                        if (d <= 3.3 && (d > 2.3 || y == 11 || y == 16)) {
                            set(x, y, z, y == 11 || y == 16 ? "light_gray_concrete"
                                    : chance(0.25) ? "waxed_cut_copper" : chance(0.5) ? "iron_block" : "light_gray_concrete");
                        }
                    }
                }
            }
            set(cx, 17, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            for (int y = 1; y <= 10; y++) {
                set(cx - 1, y, cz - 2, ladder("east"));
            }
            fill(cx, 3, cz, cx, 10, cz, "chain[axis=y,waterlogged=false]");
            set(cx, 2, cz, hanging());
            set(cx, 0, cz, "stone_bricks");
            set(cx + 1, 1, cz, "water_cauldron[level=3]");
            post(cx + 3, 1, cz - 3, 8, "WIEŻA", "CIŚNIEŃ", "ZNÓW DZIAŁA", "");
        }

        /** Lądowisko po naprawie: nowa płyta, żółte koło, białe H, światła i rękaw wiatrowy. */
        private void helipad() {
            int cx = 43;
            int cz = 110;
            for (int x = 33; x <= 53; x++) {
                for (int z = 99; z <= 121; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    String b = chance(0.8) ? "smooth_stone" : "light_gray_concrete";
                    if (Math.abs(d - 8) < 0.6) {
                        b = "yellow_concrete";
                    }
                    boolean hBar = (x == 40 || x == 46) && z >= 106 && z <= 114;
                    boolean hCross = z == 110 && x >= 40 && x <= 46;
                    if (hBar || hCross) {
                        b = "white_concrete";
                    }
                    set(x, 0, z, b);
                }
            }
            for (int[] p : new int[][]{{33, 99}, {53, 99}, {33, 121}, {53, 121}, {43, 99}, {43, 121}, {33, 110}, {53, 110}}) {
                set(p[0], 0, p[1], "sea_lantern");
            }
            // rękaw wiatrowy
            fill(52, 1, 101, 52, 5, 101, "iron_bars");
            set(51, 5, 101, "orange_wool");
            set(50, 5, 101, "white_wool");
            set(49, 5, 101, "orange_wool");
            post(34, 1, 99, 8, "LĄDOWISKO", "ZRZUTY", "I GOŚCIE", "Z NIEBA");
            sandbags(33, 123, 37, 123, 1);
        }

        /** Sad z ławkami i letnią sceną (miejsce na zebrania i koncerty). */
        private void orchard() {
            int x1 = 57;
            int x2 = 83;
            int z1 = 97;
            int z2 = 127;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 0, z, chance(0.9) ? "grass_block" : "podzol");
                }
            }
            // drzewa owocowe w rzędach
            for (int x = 60; x <= 80; x += 5) {
                for (int z = 100; z <= 108; z += 8) {
                    tree(x, z, x % 2 == 0 ? "oak" : "birch");
                }
            }
            // ścieżka przez sad
            for (int z = z1; z <= z2; z++) {
                for (int x = 69; x <= 71; x++) {
                    set(x, 0, z, "dirt_path");
                    for (int y = 1; y <= 6; y++) {
                        if (get(x, y, z).contains("leaves")) {
                            set(x, y, z, "air");
                        }
                    }
                }
            }
            for (int x = x1; x <= x2; x++) {
                for (int z = 112; z <= 114; z++) {
                    set(x, 0, z, "dirt_path");
                }
            }
            // scena z desek i daszkiem
            for (int x = 63; x <= 77; x++) {
                for (int z = 122; z <= 127; z++) {
                    set(x, 1, z, x == 63 || x == 77 || z == 127 ? "spruce_planks" : slab("spruce", "top"));
                }
            }
            for (int x : new int[]{63, 77}) {
                fill(x, 2, 127, x, 6, 127, "stripped_spruce_log[axis=y]");
                fill(x, 2, 122, x, 6, 122, "spruce_fence");
            }
            for (int x = 62; x <= 78; x++) {
                for (int z = 121; z <= 128; z++) {
                    set(x, 7, z, (x % 2 == 0) ? "red_wool" : "white_wool");
                }
            }
            fill(64, 2, 127, 76, 6, 127, "dark_oak_planks");
            set(70, 2, 125, "jukebox[has_record=false]");
            set(68, 2, 124, "note_block[instrument=harp,note=0,powered=false]");
            set(72, 2, 124, "note_block[instrument=bass,note=5,powered=false]");
            set(66, 6, 123, hanging());
            set(74, 6, 123, hanging());
            set(70, 6, 125, hanging());
            set(70, 1, 121, stairs("spruce", "south"));
            set(69, 1, 121, stairs("spruce", "south"));
            set(71, 1, 121, stairs("spruce", "south"));
            sign(70, 4, 126, "north", "LETNIA SCENA", "KONCERT", "W SOBOTĘ", "PO ZMROKU");
            // ławki z pni przed sceną
            for (int z = 116; z <= 119; z += 3) {
                for (int x = 60; x <= 80; x++) {
                    if (x < 68 || x > 72) {
                        set(x, 1, z, "stripped_oak_log[axis=x]");
                    }
                }
            }
            for (int[] p : new int[][]{{58, 112}, {82, 112}, {58, 120}, {82, 120}, {68, 98}, {72, 98}}) {
                lampPost(p[0], p[1]);
            }
            set(65, 1, 104, "beehive[facing=south,honey_level=3]");
            set(75, 1, 104, "beehive[facing=south,honey_level=5]");
            post(72, 1, 113, 4, "SAD", "JABŁKA", "DLA DZIECI", "NAJPIERW");
            for (int i = 0; i < 40; i++) {
                int x = x1 + rnd.nextInt(x2 - x1 + 1);
                int z = z1 + rnd.nextInt(110 - z1);
                if (get(x, 0, z).contains("grass") && isAir(x, 1, z)) {
                    set(x, 1, z, flower());
                }
            }
        }

        /**
         * Strzelnica: zadaszona linia strzału (x 89-97), barierka z furtkami do każdego toru,
         * 5 torów oddzielonych murkami, tarcze na 10, 20 i 35 bloków, kulochwyt z ziemi i worków.
         */
        private void range() {
            int zN = 99;
            int zS = 119;
            int xe = 137;
            for (int x = 89; x <= xe; x++) {
                for (int z = zN; z <= zS; z++) {
                    set(x, 0, z, x <= 97 ? paving.next() : chance(0.6) ? "coarse_dirt" : "packed_mud");
                }
            }
            // boczne mury i belki nad torami
            for (int x = 98; x <= xe; x++) {
                for (int y = 1; y <= 4; y++) {
                    set(x, y, zN, sandbag.next());
                    set(x, y, zS, sandbag.next());
                }
            }
            for (int bx : new int[]{104, 114, 124}) {
                for (int z = zN; z <= zS; z++) {
                    set(bx, 5, z, "spruce_log[axis=z]");
                }
                fill(bx, 1, zN, bx, 4, zN, "polished_andesite");
                fill(bx, 1, zS, bx, 4, zS, "polished_andesite");
            }
            // murki między torami
            int[] dividers = {103, 107, 111, 115};
            for (int dz : dividers) {
                for (int x = 97; x <= 133; x++) {
                    set(x, 1, dz, sandbag.next());
                    set(x, 2, dz, chance(0.6) ? "mud_bricks" : "packed_mud");
                }
            }
            // kulochwyt
            for (int x = 134; x <= xe; x++) {
                int h = 4 + (x - 134);
                for (int z = zN; z <= zS; z++) {
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, y == h ? "grass_block" : x == 134 && y <= 2 ? "packed_mud" : "dirt");
                    }
                }
            }
            // tory: furtka, barierka, tarcze
            int[] lanes = {100, 104, 108, 112, 116};
            int lane = 1;
            for (int a : lanes) {
                door(97, 1, a, "spruce_door", "east", "left", false);
                set(97, 1, a + 1, "spruce_fence");
                set(97, 1, a + 2, "spruce_fence");
                set(97, 3, a, "spruce_planks");
                int[][] targets = {{106, a}, {116, a + 1}, {131, a + 2}};
                for (int[] t : targets) {
                    set(t[0], 1, t[1], "spruce_fence");
                    counterMarker(t[0], 2, t[1], "west", "punkt-tarcza", "target[power=0]");
                }
                sign(96, 3, a, "west", "TOR " + lane, "10 · 20 · 35", "", "");
                lane++;
            }
            // zadaszenie linii strzału i poczekalnia
            for (int x = 89; x <= 98; x++) {
                for (int z = zN; z <= zS; z++) {
                    set(x, 5, z, x == 89 || x == 98 || z == zN || z == zS ? slab("spruce", "bottom") : "spruce_planks");
                }
            }
            for (int x = 89; x <= 98; x++) {
                for (int z = zN; z <= zS; z++) {
                    if (x == 89 && (z - zN) % 5 == 0 || x == 98 && (z - zN) % 5 == 0) {
                        fill(x, 1, z, x, 4, z, "spruce_log[axis=y]");
                    }
                }
            }
            for (int x = 89; x <= 97; x++) {
                fill(x, 1, zN, x, 4, zN, sandbag);
                fill(x, 1, zS, x, 4, zS, sandbag);
            }
            for (int z = zN + 1; z < zS; z += 4) {
                f.hangingLamp(93, 4, z);
                f.hangingLamp(96, 4, z + 2);
            }
            f.bench(90, 1, 101, 5, false, "west", "spruce");
            f.bench(90, 1, 112, 5, false, "west", "spruce");
            // stojak na broń: kamienie szlifierskie, stoły kowalskie, stół łuczarski
            set(93, 1, zN + 1, "grindstone[face=floor,facing=south]");
            set(94, 1, zN + 1, "smithing_table");
            set(95, 1, zN + 1, "fletching_table");
            set(93, 1, zS - 1, "smithing_table");
            set(94, 1, zS - 1, "grindstone[face=floor,facing=north]");
            set(95, 1, zS - 1, "barrel[facing=up,open=false]");
            post(88, 1, 106, 4, "STRZELNICA", "STRZELASZ", "TYLKO", "W STRONĘ WAŁU");
            post(88, 1, 111, 4, "NA TORZE", "TYLKO", "PROWADZĄCY", "– KAPRAL ZDZISEK");
            marker(96, 1, 109, "east", "punkt-strzelnica");
        }

        // ------------------------------------------------------------------ pas wschodni

        /** Stołówka (x 106-122) i Bar Pod Kanistrem z automatami (x 122-137). */
        private void canteenAndBar() {
            int z1 = 48;
            int z2 = 70;
            // stołówka
            int cx1 = 106;
            int cx2 = 122;
            walls(cx1, z1, cx2, z2, 1, 5, olive);
            floor(cx1 + 1, z1 + 1, cx2 - 1, z2 - 1, 0, white);
            for (int x = cx1 + 1; x < cx2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 6, z, "spruce_planks");
                }
            }
            gableZ(cx1, z1, cx2, z2, 6, "brick", olive);
            patches(cx1, z1, cx2, z2, 1, 5, 10);
            windowsZ(cx1, 51, 67, 3, 2, 3);
            windowsX(z2, 108, 120, 3, 2, 3);
            door(113, 1, z2, "spruce_door", "south", "left", false);
            door(114, 1, z2, "spruce_door", "south", "right", false);
            sign(113, 4, z2 + 1, "south", "STOŁÓWKA", "ZUPA CODZIENNIE", "W POŁUDNIE", "");
            sign(114, 4, z2 + 1, "south", "MENU:", "ZUPA", "ALBO", "ZUPA");
            // kuchnia: wędzarnie z okapami, ognisko, zlew
            for (int x = 108; x <= 120; x++) {
                if (x % 3 == 0) {
                    set(x, 1, 49, "smoker[facing=south,lit=true]");
                    set(x, 3, 49, trapdoor("iron", "south", "top", false));
                } else if (x % 3 == 1) {
                    set(x, 1, 49, slab("smooth_stone", "top"));
                    if (chance(0.5)) {
                        set(x, 2, 49, "cauldron");
                    }
                } else {
                    set(x, 1, 49, "water_cauldron[level=2]");
                }
            }
            set(107, 1, 50, campfire("east"));
            set(107, 3, 50, trapdoor("iron", "east", "top", false));
            set(121, 1, 50, "barrel[facing=up,open=false]");
            for (int x = 108; x <= 120; x++) {
                set(x, 1, 53, x == 120 ? "air" : slab("smooth_stone", "top"));
            }
            set(110, 2, 53, "decorated_pot[cracked=false,facing=south,waterlogged=false]");
            set(116, 2, 53, candle(3));
            // stoły z ławkami
            for (int z = 57; z <= 67; z += 5) {
                f.desk(108, 1, z, 119, z, "dark_oak");
                f.bench(108, 1, z - 1, 12, true, "north", "spruce");
                f.bench(108, 1, z + 1, 12, true, "south", "spruce");
                f.lamp(110 + rnd.nextInt(8), 2, z);
                set(109 + rnd.nextInt(9), 2, z, flowerPot());
            }
            for (int z = 52; z <= 66; z += 7) {
                f.hangingLamp(110, 5, z);
                f.hangingLamp(118, 5, z);
            }
            fill(cx2, 1, 60, cx2, 2, 61, "air");
            // bar
            int bx1 = 122;
            int bx2 = 137;
            walls(bx1, z1, bx2, z2, 1, 5, brick);
            floor(bx1 + 1, z1 + 1, bx2 - 1, z2 - 1, 0, paving);
            for (int x = bx1 + 1; x < bx2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 0, z, (x + z) % 2 == 0 ? "spruce_planks" : "dark_oak_planks");
                }
            }
            flatRoof(bx1, z1, bx2, z2, 6, true);
            fill(cx2, 1, 60, cx2, 2, 61, "air");
            patches(bx1, z1, bx2, z2, 1, 5, 6);
            windowsX(z2, 124, 135, 3, 2, 3);
            windowsZ(bx2, 51, 55, 2, 2, 3);
            door(129, 1, z2, "dark_oak_door", "south", "left", false);
            door(130, 1, z2, "dark_oak_door", "south", "right", false);
            sign(129, 4, z2 + 1, "south", "BAR", "POD", "KANISTREM", "");
            sign(130, 4, z2 + 1, "south", "PIWO Z", "WŁASNEJ", "WARKI", "");
            for (int x = 127; x <= 132; x++) {
                set(x, 5, z2 + 1, (x % 2 == 0) ? "red_wool" : "white_wool");
            }
            // wielki kanister na dachu
            for (int x = 128; x <= 131; x++) {
                for (int y = 7; y <= 11; y++) {
                    set(x, y, 67, "red_concrete");
                    set(x, y, 68, "red_concrete");
                }
            }
            fill(129, 12, 67, 130, 12, 67, "iron_bars");
            set(131, 12, 68, "iron_block");
            fill(128, 9, 66, 131, 9, 66, "black_concrete");
            set(127, 7, 66, lantern());
            set(132, 7, 66, lantern());
            // lada baru
            for (int x = 124; x <= 133; x++) {
                set(x, 1, 54, slab("dark_oak", "top"));
            }
            for (int x = 123; x <= 136; x++) {
                set(x, 1, 49, x % 2 == 0 ? "barrel[facing=south,open=false]" : "barrel[facing=up,open=false]");
                set(x, 2, 49, x % 3 == 0 ? "barrel[facing=south,open=false]" : slab("spruce", "top"));
                set(x, 3, 49, x % 2 == 0 ? candle(2) : "air");
            }
            set(125, 1, 51, "brewing_stand[has_bottle_0=true,has_bottle_1=true,has_bottle_2=false]");
            set(127, 1, 51, "water_cauldron[level=3]");
            set(128, 1, 51, "cauldron");
            for (int x = 124; x <= 133; x += 2) {
                f.chair(x, 1, 55, "south", "dark_oak");
            }
            set(126, 2, 54, candle(3));
            set(131, 2, 54, "decorated_pot[cracked=false,facing=south,waterlogged=false]");
            sign(130, 3, 50, "south", "NA KRESKĘ", "NIE", "NALEWAMY", "");
            // stoliki
            for (int[] t : new int[][]{{125, 60}, {125, 65}, {130, 62}, {130, 67}}) {
                f.table(t[0], 1, t[1], "dark_oak");
                f.chair(t[0] - 1, 1, t[1], "west", "dark_oak");
                f.chair(t[0] + 1, 1, t[1], "east", "dark_oak");
            }
            // kącik muzyczny
            set(123, 1, 69, "jukebox[has_record=false]");
            set(124, 1, 69, "note_block[instrument=bass,note=3,powered=false]");
            // automaty do gry (punkt-automat w każdym automacie)
            for (int z = 58; z <= 64; z += 2) {
                counterMarker(136, 1, z, "west", "punkt-automat", "dispenser[facing=west,triggered=false]");
                set(136, 2, z, "observer[facing=west,powered=false]");
                set(136, 3, z, z % 4 == 0 ? "redstone_lamp[lit=false]" : "sea_lantern");
                set(136, 2, z + 1, "lever[face=wall,facing=west,powered=false]");
            }
            set(136, 2, 65, "air");
            sign(136, 4, 61, "west", "AUTOMATY", "GRASZ NA", "WŁASNE", "RYZYKO");
            for (int z = 52; z <= 66; z += 7) {
                f.hangingLamp(126, 5, z);
                f.hangingLamp(132, 5, z);
            }
        }

        /** Kuchnia polowa pod namiotem: ogniska z kotłami, stoły i ławy. */
        private void fieldKitchen() {
            int x1 = 106;
            int x2 = 117;
            int z1 = 73;
            int z2 = 80;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 0, z, chance(0.7) ? "coarse_dirt" : "packed_mud");
                }
            }
            for (int[] p : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}, {x1, 76}, {x2, 76}}) {
                fill(p[0], 1, p[1], p[0], 3, p[1], "spruce_fence");
            }
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    int y = z == z1 || z == z2 ? 4 : z == z1 + 1 || z == z2 - 1 ? 5 : 6;
                    set(x, y, z, "green_wool");
                }
            }
            for (int x = x1 + 1; x <= x2 - 1; x += 3) {
                set(x, 1, 74, campfire("south"));
                set(x + 1, 1, 74, "cauldron");
            }
            set(116, 1, 74, "smoker[facing=south,lit=true]");
            f.desk(108, 1, 78, 115, 78, "spruce");
            f.bench(108, 1, 79, 8, true, "south", "spruce");
            f.bench(108, 1, 77, 8, true, "north", "spruce");
            set(110, 2, 78, candle(2));
            set(114, 2, 78, flowerPot());
            set(111, 5, 76, hanging());
            set(115, 5, 76, hanging());
            post(118, 1, 72, 4, "KUCHNIA", "POLOWA", "GROCHÓWKA", "PO 18:00");
        }

        /** Plac z dużym ogniskiem, ławy z pni, huśtawki, zjeżdżalnia i piaskownica. */
        private void bonfireSquare() {
            int cx = 127;
            int cz = 82;
            for (int x = 119; x <= 137; x++) {
                for (int z = 73; z <= 91; z++) {
                    double d = Math.hypot(x - cx - 0.5, z - cz - 0.5);
                    if (d < 7.5) {
                        set(x, 0, z, chance(0.6) ? "coarse_dirt" : "gravel");
                    }
                }
            }
            for (int x = cx - 1; x <= cx + 2; x++) {
                for (int z = cz - 1; z <= cz + 2; z++) {
                    boolean inner = x >= cx && x <= cx + 1 && z >= cz && z <= cz + 1;
                    set(x, 1, z, inner ? campfire("north") : slab("cobblestone", "bottom"));
                }
            }
            for (int x = cx - 6; x <= cx + 7; x++) {
                for (int z = cz - 6; z <= cz + 7; z++) {
                    double d = Math.hypot(x - cx - 0.5, z - cz - 0.5);
                    boolean gap = Math.abs(x - cx - 0.5) < 1.5 || Math.abs(z - cz - 0.5) < 1.5;
                    if (Math.abs(d - 5) < 0.5 && !gap) {
                        set(x, 1, z, Math.abs(x - cx - 0.5) > Math.abs(z - cz - 0.5) ? "stripped_oak_log[axis=z]"
                                : "stripped_oak_log[axis=x]");
                    }
                }
            }
            for (int[] p : new int[][]{{cx - 6, cz - 6}, {cx + 7, cz - 6}, {cx - 6, cz + 7}, {cx + 7, cz + 7}}) {
                lampPost(p[0], p[1]);
            }
            post(cx - 1, 1, cz - 7, 8, "OGNISKO", "WIECZOREM", "OPOWIEŚCI", "I GITARA");
            // huśtawki
            fill(131, 1, 74, 131, 4, 74, "spruce_log[axis=y]");
            fill(136, 1, 74, 136, 4, 74, "spruce_log[axis=y]");
            fill(132, 4, 74, 135, 4, 74, "spruce_log[axis=x]");
            for (int x : new int[]{132, 135}) {
                fill(x, 2, 74, x, 3, 74, "chain[axis=y,waterlogged=false]");
                set(x, 1, 74, slab("oak", "bottom"));
            }
            // zjeżdżalnia
            fill(135, 1, 89, 136, 2, 90, "spruce_planks");
            fill(135, 3, 89, 136, 3, 90, slab("spruce", "bottom"));
            for (int y = 1; y <= 3; y++) {
                set(137, y, 89, ladder("east"));
            }
            set(137, 0, 89, "stone_bricks");
            for (int i = 0; i < 3; i++) {
                set(134 - i, 2 - i, 89, i == 2 ? slab("quartz", "bottom") : stairs("quartz", "east"));
                if (i < 2) {
                    fill(134 - i, 1, 89, 134 - i, 1 - i, 89, i == 0 ? "quartz_block" : "air");
                }
            }
            // piaskownica
            for (int x = 120; x <= 124; x++) {
                for (int z = 87; z <= 91; z++) {
                    boolean edge = x == 120 || x == 124 || z == 87 || z == 91;
                    set(x, edge ? 1 : 0, z, edge ? "stripped_spruce_log[axis=" + (z == 87 || z == 91 ? "x" : "z") + "]" : "sand");
                }
            }
            set(122, 1, 89, "red_carpet");
            // huśtawka-ważka
            set(127, 1, 90, "spruce_fence");
            fill(125, 2, 90, 129, 2, 90, "stripped_oak_log[axis=x]");
            set(125, 2, 90, slab("oak", "bottom"));
            set(129, 2, 90, slab("oak", "bottom"));
            set(126, 2, 90, "stripped_oak_log[axis=x]");
            set(128, 2, 90, "stripped_oak_log[axis=x]");
            post(133, 1, 81, 12, "PLAC ZABAW", "DZIECI MAJĄ", "PIERWSZEŃSTWO", "");
            // namioty przyjezdnych
            tent(119, 74, "green");
            tent(119, 79, "brown");
            tent(134, 77, "light_gray");
            set(122, 1, 78, campfire("north").replace("lit=true", "lit=true"));
            post(121, 1, 84, 4, "NAMIOTY", "DLA GOŚCI", "PIERWSZA NOC", "ZA DARMO");
        }

        /** Mały namiot (3 × 4 wzdłuż z) ze śpiworem w środku, wejście od północy. */
        private void tent(int x0, int z0, String color) {
            for (int z = z0; z <= z0 + 3; z++) {
                set(x0, 1, z, color + "_wool");
                set(x0 + 2, 1, z, color + "_wool");
                set(x0 + 1, 2, z, color + "_wool");
            }
            set(x0 + 1, 2, z0, "air");
            set(x0 + 1, 1, z0, "air");
            f.bed(x0 + 1, 1, z0 + 2, "south", "brown");
            set(x0 + 1, 2, z0 - 1, "air");
            set(x0 - 1, 1, z0, lantern());
        }

        /** Parking z naprawionymi autami za strzelnicą. */
        private void parking() {
            for (int x = 90; x <= 136; x++) {
                for (int z = 122; z <= 128; z++) {
                    set(x, 0, z, asphalt.next());
                }
            }
            for (int x = 92; x <= 134; x += 6) {
                for (int z = 122; z <= 125; z++) {
                    set(x, 0, z, "white_concrete");
                }
            }
            uaz(94, 123, true, false);
            uaz(106, 123, true, true);
            uaz(118, 123, true, false);
            // ciągnik
            fill(124, 1, 123, 126, 1, 125, "red_concrete");
            fill(125, 2, 124, 126, 3, 124, "red_concrete");
            set(124, 2, 124, "glass_pane");
            set(123, 1, 123, "polished_blackstone");
            set(123, 1, 125, "polished_blackstone");
            set(127, 1, 123, "polished_blackstone");
            set(127, 1, 125, "polished_blackstone");
            set(130, 1, 123, "iron_block");
            set(130, 1, 124, "iron_block");
            set(130, 2, 123, "barrel[facing=up,open=false]");
            post(91, 1, 121, 0, "PARKING", "AUTA RADY", "KLUCZYKI", "W DEPOZYCIE");
            lampPost(100, 121);
            lampPost(112, 121);
            lampPost(124, 121);
        }

        // ------------------------------------------------------------------ oświetlenie, zieleń, łączenia

        private void lampPosts() {
            for (int x = 12; x <= 138; x += 12) {
                lampPost(x, 41);
                lampPost(x, 96);
            }
            for (int z = 22; z <= 128; z += 12) {
                lampPost(11, z);
                lampPost(138, z);
            }
            for (int z = 48; z <= 128; z += 10) {
                lampPost(49, z);
                lampPost(104, z);
            }
            for (int z = 20; z <= 40; z += 10) {
                lampPost(69, z);
                lampPost(80, z + 5);
            }
        }

        private boolean nearMarker(int x, int y, int z) {
            for (Blueprint.SpawnPoint p : bp.spawnPoints()) {
                if (Math.abs(p.pos().x() - x) <= 1 && Math.abs(p.pos().z() - z) <= 1 && Math.abs(p.pos().y() - (y + G)) <= 2) {
                    return true;
                }
            }
            return false;
        }

        private void weeds() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    if (!get(x, 0, z).equals("minecraft:grass_block") || !isAir(x, 1, z) || !isAir(x, 2, z)) {
                        continue;
                    }
                    if (!chance(0.12) || nearMarker(x, 1, z)) {
                        continue;
                    }
                    double r = rnd.nextDouble();
                    if (r < 0.35) {
                        set(x, 1, z, flower());
                    } else if (r < 0.85) {
                        set(x, 1, z, "short_grass");
                    } else {
                        set(x, 1, z, "tall_grass[half=lower]");
                        set(x, 2, z, "tall_grass[half=upper]");
                    }
                }
            }
        }

        private static String bare(String state) {
            String s = state.replace("minecraft:", "");
            int i = s.indexOf('[');
            return i < 0 ? s : s.substring(0, i);
        }

        private static boolean fullSolid(String state) {
            String n = bare(state);
            if (n.equals("air")) {
                return false;
            }
            String[] not = {"slab", "stairs", "fence", "_wall", "pane", "bars", "door", "carpet", "sign", "lantern", "torch",
                    "ladder", "chain", "button", "lever", "pot", "flower", "grass", "fern", "bed", "cauldron", "campfire",
                    "candle", "water", "rod", "sapling", "wheat", "carrots", "potatoes", "beetroots", "farmland",
                    "dirt_path", "scaffolding", "hopper", "anvil", "grindstone", "brewing", "bell", "lectern", "composter",
                    "cobweb", "tripwire", "plate", "daylight", "bush", "tulip", "poppy", "dandelion", "allium", "bluet",
                    "daisy", "cornflower", "lily", "skull", "glass", "stonecutter", "end_rod", "leaves", "trapdoor"};
            for (String s : not) {
                if (n.contains(s)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean connects(String self, String other) {
            String o = bare(other);
            if (o.equals("air")) {
                return false;
            }
            if (self.endsWith("_fence")) {
                return o.endsWith("_fence") && !o.contains("nether") || o.contains("fence_gate") || fullSolid(other);
            }
            if (self.endsWith("_wall")) {
                return o.endsWith("_wall") || o.contains("fence_gate") || o.equals("iron_bars") || o.endsWith("pane")
                        || fullSolid(other);
            }
            return o.endsWith("glass_pane") || o.equals("iron_bars") || o.endsWith("_wall") || o.equals("glass")
                    || o.endsWith("stained_glass") || fullSolid(other);
        }

        /** Łączy płoty, szyby, kraty i murki z sąsiadami (plugin wkleja bloki bez aktualizacji kształtu). */
        private void connect() {
            int[][] d = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
            for (Map.Entry<Blueprint.Pos, String> e : new ArrayList<>(bp.blocks().entrySet())) {
                String s = e.getValue();
                if (s.contains("[")) {
                    continue;
                }
                String n = bare(s);
                boolean fence = n.endsWith("_fence");
                boolean wall = n.endsWith("_wall");
                boolean pane = n.endsWith("glass_pane") || n.equals("iron_bars");
                if (!fence && !wall && !pane) {
                    continue;
                }
                Blueprint.Pos p = e.getKey();
                boolean[] c = new boolean[4];
                for (int i = 0; i < 4; i++) {
                    c[i] = connects(n, bp.get(p.x() + d[i][0], p.y(), p.z() + d[i][1]));
                }
                StringBuilder sb = new StringBuilder("minecraft:").append(n).append('[');
                if (wall) {
                    // kolejność alfabetyczna: east, north, south, up, waterlogged, west
                    boolean straight = c[0] && c[1] && !c[2] && !c[3] || c[2] && c[3] && !c[0] && !c[1];
                    boolean above = !bp.isAir(p.x(), p.y() + 1, p.z());
                    sb.append("east=").append(c[2] ? "low" : "none")
                            .append(",north=").append(c[0] ? "low" : "none")
                            .append(",south=").append(c[1] ? "low" : "none")
                            .append(",up=").append(!straight || above)
                            .append(",waterlogged=false")
                            .append(",west=").append(c[3] ? "low" : "none");
                } else {
                    sb.append("east=").append(c[2])
                            .append(",north=").append(c[0])
                            .append(",south=").append(c[1])
                            .append(",waterlogged=false")
                            .append(",west=").append(c[3]);
                }
                sb.append(']');
                bp.set(p.x(), p.y(), p.z(), sb.toString());
            }
        }
    }
}
