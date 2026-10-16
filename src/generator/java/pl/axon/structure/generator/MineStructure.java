package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.Random;

/**
 * Zamknięta Kopalnia Węgla Kamiennego „Jedność” – śląski przemysł po upadku, z małym podziemiem pod nadszybiem.
 * <p>
 * Struktura ma {@code grunt: 12}: dolne 12 warstw to skała z wykutym podszybiem, wklejana pod powierzchnię.
 * W kodzie wysokości liczone są od gruntu (y = 0 to powierzchnia, y &lt; 0 to podziemie).
 * <pre>
 *  z 0-3    droga
 *  z 5      płot z bramą x 36-43 (ceglane słupy, napis KWK JEDNOŚĆ), portiernia x 46-50
 *  z 6-29   plac z torami: cechownia x 3-18 / z 12-28, sortownia x 58-66 / z 9-19 z rampą i taśmociągiem
 *  z 30-44  nadszybie x 28-44 z wieżą szybową, schody wieży x 46-49, maszynownia x 53-69
 *  z 45-69  hałda ze sztolnią (środek x 16 / z 57)
 *  z 70     tylny płot
 *  pod ziemią: szyb x 34-37 / z 35-38 do podszybia (y -10), chodnik na wschód, przekop do składu
 *              materiałów wybuchowych i zalana komora pomp
 * </pre>
 * Parkour: wieża szybowa (schody z urwanym biegiem → belka kratownicy → wyższy podest), pulpit maszynisty
 * (kratownica → bęben → belka suwnicy → pomost), sortownia (rampa → taśmociąg → najwyższe piętro).
 */
public final class MineStructure implements StructureDefinition {

    private static final int WIDTH = 80;
    private static final int DEPTH = 72;
    /** Ile warstw leży pod ziemią. */
    private static final int G = 12;
    private static final int HEIGHT = G + 46;

    // nadszybie i szyb
    private static final int NX1 = 28;
    private static final int NX2 = 44;
    private static final int NZ1 = 30;
    private static final int NZ2 = 44;
    private static final int NROOF = 12;
    private static final int SX1 = 34;
    private static final int SX2 = 37;
    private static final int SZ1 = 35;
    private static final int SZ2 = 38;
    /** Poziom stóp w podszybiu. */
    private static final int UY = -10;

    // maszynownia
    private static final int MX1 = 53;
    private static final int MX2 = 69;
    private static final int MZ1 = 30;
    private static final int MZ2 = 44;

    // sortownia
    private static final int RX1 = 58;
    private static final int RX2 = 66;
    private static final int RZ1 = 9;
    private static final int RZ2 = 19;

    // cechownia
    private static final int CX1 = 3;
    private static final int CX2 = 18;
    private static final int CZ1 = 12;
    private static final int CZ2 = 28;

    // hałda
    private static final int HCX = 16;
    private static final int HCZ = 57;
    private static final double HR = 12.5;
    private static final int HTOP = 13;

    @Override
    public String id() {
        return "kopalnia";
    }

    @Override
    public String displayName() {
        return "Kopalnia Jedność";
    }

    @Override
    public String description() {
        return "Zamknięta kopalnia węgla: wieża szybowa, cechownia, maszynownia, sortownia, hałda ze sztolnią i podszybie pod ziemią";
    }

    @Override
    public Blueprint build(Random random) {
        return new Builder(random).build();
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);
        private final int[][] heap = new int[WIDTH][DEPTH];

        private final Palette grass;
        private final Palette dust;
        private final Palette brick;
        private final Palette rock;
        private final Palette coal;
        private final Palette steel;
        private final Palette sheet;
        private final Palette rubble;
        private final Furniture f;

        Builder(Random rnd) {
            this.rnd = rnd;
            f = new Furniture(bp, G, rnd);
            grass = new Palette(rnd)
                    .add("grass_block", 12).add("coarse_dirt", 3).add("dirt", 2).add("podzol", 1);
            dust = new Palette(rnd)
                    .add("coarse_dirt", 5).add("black_concrete", 3).add("dirt", 2).add("gravel", 2)
                    .add("tuff", 1).add("blackstone", 1);
            brick = new Palette(rnd)
                    .add("bricks", 14).add("terracotta", 2).add("polished_granite", 1).add("mud_bricks", 1);
            rock = new Palette(rnd)
                    .add("stone", 20).add("andesite", 1).add("tuff", 1).add("coal_ore", 1);
            coal = new Palette(rnd)
                    .add("black_concrete", 4).add("tuff", 3).add("basalt[axis=y]", 2).add("blackstone", 3)
                    .add("coal_ore", 1).add("cobbled_deepslate", 1);
            steel = new Palette(rnd)
                    .add("iron_block", 6).add("waxed_weathered_cut_copper", 1).add("orange_terracotta", 1);
            sheet = new Palette(rnd)
                    .add("light_gray_concrete", 5).add("iron_block", 3).add("orange_terracotta", 1).add("andesite", 1);
            rubble = new Palette(rnd)
                    .add("cobblestone", 4).add("tuff", 2).add("gravel", 1).add("cobbled_deepslate", 2);
        }

        Blueprint build() {
            bp.ground(G);
            ground();
            fence();
            gate();
            gatehouse();
            yard();
            washhouse();
            headframeBuilding();
            shaft();
            underground();
            headframe();
            towerStairs();
            winderHouse();
            sorting();
            heap();
            adit();
            weeds();
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

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        private String brokenGlass() {
            double roll = rnd.nextDouble();
            if (roll < 0.45) {
                return "air";
            }
            if (roll < 0.52) {
                return "cobweb";
            }
            return chance(0.2) ? "gray_stained_glass_pane" : "glass_pane";
        }

        private void door(int x, int y, int z, String type, String facing, String hinge, boolean open) {
            String props = "facing=" + facing + ",hinge=" + hinge + ",open=" + open + ",powered=false";
            set(x, y, z, type + "[half=lower," + props + "]");
            set(x, y + 1, z, type + "[half=upper," + props + "]");
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

        /** Wagonik: dwa kociołki na torze, czasem z węglem obok. */
        private void cart(int x, int z, boolean alongX) {
            set(x, 1, z, "cauldron");
            if (alongX) {
                set(x + 1, 1, z, "cauldron");
            } else {
                set(x, 1, z + 1, "cauldron");
            }
        }

        // ------------------------------------------------------------------ teren, płot, brama

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    fill(x, -G, z, x, -4, z, rock);
                    for (int y = -3; y <= -1; y++) {
                        set(x, y, z, chance(0.85) ? "dirt" : "coarse_dirt");
                    }
                    String top;
                    if (z <= 3) {
                        top = chance(0.8) ? "gray_concrete" : chance(0.6) ? "andesite" : "cobblestone";
                    } else if (x >= 22 && x <= 56 && z >= 6 && z <= 29) {
                        top = dust.next();
                    } else {
                        top = grass.next();
                    }
                    set(x, 0, z, top);
                }
            }
            for (int x = 34; x <= 45; x++) {
                set(x, 0, 4, chance(0.7) ? "gravel" : "coarse_dirt");
            }
            // ścieżka do cechowni i pod maszynownię
            for (int x = CX2 + 1; x <= 21; x++) {
                for (int z = 18; z <= 21; z++) {
                    set(x, 0, z, chance(0.6) ? "coarse_dirt" : "gravel");
                }
            }
            for (int x = 45; x <= MX1 - 1; x++) {
                for (int z = 30; z <= 44; z++) {
                    set(x, 0, z, chance(0.5) ? "coarse_dirt" : dust.next());
                }
            }
        }

        private void fence() {
            for (int x = 1; x <= WIDTH - 2; x++) {
                if (x < 35 || x > 44) {
                    fencePiece(x, 5, x);
                }
                fencePiece(x, DEPTH - 2, x);
            }
            for (int z = 6; z <= DEPTH - 3; z++) {
                fencePiece(1, z, z);
                fencePiece(WIDTH - 2, z, z);
            }
            for (int[] gap : new int[][]{{12, 5}, {66, 5}, {44, DEPTH - 2}}) {
                fill(gap[0], 1, gap[1], gap[0] + 2, 2, gap[1], "air");
            }
            fill(WIDTH - 2, 1, 50, WIDTH - 2, 2, 52, "air");
        }

        private void fencePiece(int x, int z, int pos) {
            if (pos % 4 == 0) {
                fill(x, 1, z, x, 2, z, "polished_andesite");
            } else {
                set(x, 1, z, "iron_bars");
                set(x, 2, z, chance(0.85) ? "iron_bars" : "air");
            }
        }

        /** Ceglane słupy z belką i żeliwnymi literami (część odpadła), skrzydła bramy otwarte do środka. */
        private void gate() {
            fill(35, 1, 5, 35, 7, 5, brick);
            fill(44, 1, 5, 44, 7, 5, brick);
            set(35, 8, 5, "stone_brick_slab[type=bottom]");
            set(44, 8, 5, "stone_brick_slab[type=bottom]");
            for (int x = 36; x <= 43; x++) {
                set(x, 7, 5, chance(0.85) ? "polished_blackstone" : "orange_terracotta");
            }
            sign(38, 7, 4, "dark_oak_wall_sign[facing=north]", "KWK", "", "", "");
            sign(41, 7, 4, "dark_oak_wall_sign[facing=north]", "JEDN Ś Ć", "", "", "");
            for (int z = 6; z <= 9; z++) {
                set(34, 1, z, "iron_bars");
                set(34, 2, z, "iron_bars");
                set(45, 1, z, "iron_bars");
                if (z < 9) {
                    set(45, 2, z, "iron_bars");
                }
            }
        }

        private void gatehouse() {
            int x1 = 46;
            int x2 = 50;
            int z1 = 7;
            int z2 = 11;
            walls(x1, z1, x2, z2, 1, 3, brick);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 0, z, "smooth_stone");
                    set(x, 4, z, "smooth_stone_slab[type=bottom]");
                }
            }
            set(48, 2, z1, brokenGlass());
            set(x1, 2, 9, brokenGlass());
            door(x1, 1, 10, "spruce_door", "west", "left", true);
            set(49, 1, 8, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            set(49, 1, 10, "spruce_planks");
            set(48, 1, 10, "spruce_slab[type=top]");
            set(x2 - 1, 3, 10, "lever[face=wall,facing=west,powered=false]");
            crate(47, 1, 8, "east", "parter");
            f.lamp(49, 2, 10);
            sign(47, 3, 6, "spruce_wall_sign[facing=north]", "SZCZĘŚĆ", "BOŻE", "", "");
            // szlaban w pozycji pionowej
            fill(52, 1, 6, 52, 4, 6, "stripped_birch_log[axis=y]");
        }

        // ------------------------------------------------------------------ plac z torami

        private void yard() {
            // główny tor od bramy do nadszybia
            for (int z = 6; z <= NZ1 + 3; z++) {
                set(39, 1, z, "rail[shape=north_south]");
            }
            // bocznica do sortowni
            for (int x = 41; x < RX1; x++) {
                set(x, 1, 17, "rail[shape=east_west]");
            }
            set(41, 1, 17, "polished_blackstone_wall");
            cart(39, 11, false);
            cart(39, 24, false);
            cart(47, 17, true);
            // przewrócone wagoniki z węglem
            set(43, 1, 23, "cauldron");
            set(44, 1, 23, "coal_block");
            set(44, 1, 24, "coal_block");
            set(43, 1, 25, "black_concrete");
            set(30, 1, 12, "cauldron");
            set(31, 1, 12, "coal_block");
            // stos starych podkładów
            fill(25, 1, 22, 30, 1, 23, "dark_oak_log[axis=x]");
            fill(26, 2, 22, 29, 2, 22, "dark_oak_log[axis=x]");
            crate(24, 1, 22, "east", "plac");
            crate(31, 1, 24, "west", "plac");
            crate(41, 1, 11, "west", "plac");
            crate(37, 1, 25, "east", "plac");
            crate(45, 1, 22, "north", "plac");
            crate(51, 1, 46, "north", "paliwo");
            crate(52, 1, 46, "north", "paliwo");
            set(50, 1, 46, "blackstone_wall");
        }

        // ------------------------------------------------------------------ cechownia z łaźnią

        private void washhouse() {
            int floor = 5;
            int roof = 10;
            walls(CX1, CZ1, CX2, CZ2, 1, roof - 1, brick);
            for (int x = CX1; x <= CX2; x++) {
                for (int z = CZ1; z <= CZ2; z++) {
                    set(x, roof, z, chance(0.9) ? "smooth_stone" : "andesite");
                    boolean inside = x > CX1 && x < CX2 && z > CZ1 && z < CZ2;
                    if (inside) {
                        set(x, floor, z, chance(0.9) ? "spruce_planks" : "stripped_spruce_wood[axis=y]");
                    } else {
                        set(x, floor, z, "stone_bricks");
                    }
                }
            }
            // gzyms
            for (int x = CX1 - 1; x <= CX2 + 1; x++) {
                set(x, roof, CZ1 - 1, "stone_brick_slab[type=top]");
                set(x, roof, CZ2 + 1, "stone_brick_slab[type=top]");
            }
            for (int z = CZ1; z <= CZ2; z++) {
                set(CX1 - 1, roof, z, "stone_brick_slab[type=top]");
                set(CX2 + 1, roof, z, "stone_brick_slab[type=top]");
            }
            // wysokie okna (większość wybita)
            for (int z = CZ1 + 2; z <= CZ2 - 2; z += 3) {
                for (int y : new int[]{2, 3, 7, 8}) {
                    set(CX1, y, z, brokenGlass());
                    if (z < 18 || z > 21) {
                        set(CX2, y, z, brokenGlass());
                    }
                }
            }
            for (int x = CX1 + 3; x <= CX2 - 3; x += 3) {
                for (int y : new int[]{2, 3, 7, 8}) {
                    set(x, y, CZ1, brokenGlass());
                    set(x, y, CZ2, brokenGlass());
                }
            }
            // wejście od placu
            door(CX2, 1, 19, "spruce_door", "east", "left", true);
            fill(CX2, 1, 20, CX2, 2, 20, "air");
            set(CX2 + 1, 0, 19, "stone_brick_slab[type=top]");
            set(CX2 + 1, 0, 20, "stone_brick_slab[type=top]");

            // parter: lampownia (zachód) i cechownia z mozaiką (wschód)
            fill(9, 1, CZ1 + 1, 9, floor - 1, CZ2 - 1, brick);
            fill(9, 1, 20, 9, 2, 20, "air");
            String[] mosaic = {"white_terracotta", "red_terracotta", "yellow_terracotta", "black_terracotta"};
            for (int x = 10; x < CX2; x++) {
                for (int z = CZ1 + 1; z < CZ2; z++) {
                    int ring = Math.max(Math.abs(x - 14), Math.abs(z - 20));
                    set(x, 0, z, chance(0.07) ? "cracked_stone_bricks" : mosaic[ring % mosaic.length]);
                }
            }
            for (int x = CX1 + 1; x < 9; x++) {
                for (int z = CZ1 + 1; z < CZ2; z++) {
                    set(x, 0, z, chance(0.8) ? "smooth_stone" : "cracked_stone_bricks");
                }
            }
            // ściana z setkami haczyków na lampy
            for (int z = CZ1 + 1; z < CZ2; z++) {
                if (z == 20) {
                    continue;
                }
                for (int y = 2; y <= 4; y++) {
                    if (chance(0.85)) {
                        set(10, y, z, "tripwire_hook[facing=east,attached=false,powered=false]");
                    }
                }
            }
            // regały z lampami w lampowni
            for (int x : new int[]{4, 6}) {
                for (int z = CZ1 + 2; z <= CZ2 - 2; z++) {
                    set(x, 1, z, "spruce_planks");
                    set(x, 2, z, chance(0.55) ? "lantern[hanging=false]" : "air");
                    set(x, 3, z, "spruce_slab[type=bottom]");
                }
            }
            crate(8, 1, CZ1 + 1, "west", "parter");
            crate(5, 1, CZ2 - 1, "north", "parter");
            crate(8, 1, 24, "west", "zaplecze");
            // ławki w cechowni przed pulpitem, lampa pod stropem
            for (int x = 12; x <= 16; x += 2) {
                f.bench(x, 1, 15, 3, false, "west", "spruce");
            }
            set(14, 1, CZ1 + 1, "lectern[facing=south,has_book=false,powered=false]");
            f.hangingLamp(14, 4, 20);
            crate(17, 1, CZ1 + 1, "west", "parter");
            crate(11, 1, CZ2 - 1, "north", "parter");

            // całe schody na piętro wzdłuż południowej ściany
            for (int i = 0; i < 4; i++) {
                set(16 - i, 1 + i, CZ2 - 1, "brick_stairs[facing=west,half=bottom,shape=straight]");
                for (int y = 1; y < 1 + i; y++) {
                    set(16 - i, y, CZ2 - 1, "bricks");
                }
            }
            fill(13, floor, CZ2 - 2, 16, floor, CZ2 - 1, "air");

            // piętro: łaźnia łańcuszkowa
            for (int x = 6; x <= 16; x += 2) {
                for (int z = CZ1 + 3; z <= CZ2 - 4; z += 3) {
                    set(x, 9, z, "chain[axis=y]");
                    set(x, 8, z, "chain[axis=y]");
                    String[] wool = {"blue_wool", "brown_wool", "gray_wool", "light_blue_wool", "green_wool"};
                    set(x, 7, z, chance(0.75) ? wool[rnd.nextInt(wool.length)] : "chain[axis=y]");
                }
            }
            for (int x = CX1 + 2; x < CX2; x += 2) {
                set(x, 8, CZ1 + 1, "lightning_rod[facing=down]");
                set(x, floor, CZ1 + 1, "cyan_terracotta");
                set(x, floor, CZ1 + 2, "cyan_terracotta");
            }
            for (int z = 17; z <= 23; z += 3) {
                fill(8, floor + 1, z, 15, floor + 1, z, "spruce_slab[type=bottom]");
            }
            // szafki pod zachodnią ścianą
            for (int z = CZ1 + 3; z <= CZ2 - 3; z++) {
                set(CX1 + 1, floor + 1, z, "spruce_planks");
                set(CX1 + 1, floor + 2, z, chance(0.7)
                        ? "spruce_trapdoor[facing=east,half=top,open=true,powered=false,waterlogged=false]" : "air");
            }
            crate(CX1 + 1, floor + 1, 17, "east", "parter");
            crate(CX1 + 1, floor + 1, 24, "east", "parter");
            set(CX1 + 1, floor + 2, 17, "air");
            set(CX1 + 1, floor + 2, 24, "air");
            // umywalki i ubikacja pod wschodnią ścianą łaźni
            f.sink(CX2 - 1, floor + 1, 15, "east");
            f.sink(CX2 - 1, floor + 1, 16, "east");
            f.toilet(CX2 - 1, floor + 1, CZ2 - 1, "south");
            crate(CX2 - 1, floor + 1, CZ2 - 3, "west", "toalety");
            for (int i = 0; i < 25; i++) {
                int x = CX1 + 1 + rnd.nextInt(CX2 - CX1 - 1);
                int z = CZ1 + 1 + rnd.nextInt(CZ2 - CZ1 - 1);
                if (isAir(x, roof - 1, z)) {
                    set(x, roof - 1, z, "cobweb");
                }
            }
        }

        // ------------------------------------------------------------------ nadszybie, szyb i podszybie

        private void headframeBuilding() {
            walls(NX1, NZ1, NX2, NZ2, 1, NROOF - 1, brick);
            for (int x = NX1; x <= NX2; x++) {
                for (int z = NZ1; z <= NZ2; z++) {
                    set(x, NROOF, z, chance(0.9) ? "smooth_stone" : "andesite");
                    if (x > NX1 && x < NX2 && z > NZ1 && z < NZ2) {
                        set(x, 0, z, chance(0.85) ? "stone_bricks" : "cracked_stone_bricks");
                    }
                }
            }
            // duża brama na wagoniki
            fill(37, 1, NZ1, 41, 4, NZ1, "air");
            set(37, 5, NZ1, "brick_stairs[facing=east,half=top,shape=straight]");
            set(41, 5, NZ1, "brick_stairs[facing=west,half=top,shape=straight]");
            // okna
            for (int x = NX1 + 2; x <= NX2 - 2; x += 3) {
                for (int y : new int[]{2, 3, 7, 8, 9}) {
                    if (x < 36 || x > 42 || y > 5) {
                        set(x, y, NZ1, brokenGlass());
                    }
                    set(x, y, NZ2, brokenGlass());
                }
            }
            for (int z = NZ1 + 2; z <= NZ2 - 2; z += 3) {
                for (int y : new int[]{7, 8, 9}) {
                    set(NX1, y, z, brokenGlass());
                    set(NX2, y, z, brokenGlass());
                }
            }
            // nogi wieży szybowej przechodzą przez budynek
            for (int[] leg : legs()) {
                fill(leg[0], 1, leg[1], leg[0], NROOF - 1, leg[1], "iron_block");
            }
            // tablica sygnałowa i ławka
            set(42, 2, NZ2 - 1, "lever[face=wall,facing=north,powered=false]");
            set(41, 2, NZ2 - 1, "stone_button[face=wall,facing=north,powered=false]");
            f.bench(30, 1, 42, 3, true, "south", "spruce");
            crate(30, 1, 32, "east", "plac");
            crate(43, 1, 32, "west", "parter");
            crate(29, 1, 43, "east", "plac");
            // obozowisko dzikiego lokatora w kącie przy tablicy sygnałowej
            f.camp(41, 1, 41, "east");
            sign(43, 2, 43, "spruce_wall_sign[facing=north]", "MÓJ KĄT", "WYPAD", "", "");
            // ktoś nie trafił w przejście przy barierce
            f.skeleton(36, 1, 41, "east");
            for (int i = 0; i < 20; i++) {
                int x = NX1 + 1 + rnd.nextInt(NX2 - NX1 - 1);
                int z = NZ1 + 1 + rnd.nextInt(NZ2 - NZ1 - 1);
                if (isAir(x, NROOF - 1, z)) {
                    set(x, NROOF - 1, z, "cobweb");
                }
            }
        }

        private int[][] legs() {
            return new int[][]{{32, 33}, {39, 33}, {32, 40}, {39, 40}};
        }

        /** Otwór szybu z barierką i kratą z dziurami, drabina ewakuacyjna z półkami, klatka na dnie. */
        private void shaft() {
            // obudowa szybu z cegły
            for (int y = UY; y <= -1; y++) {
                for (int x = SX1 - 1; x <= SX2 + 1; x++) {
                    for (int z = SZ1 - 1; z <= SZ2 + 1; z++) {
                        boolean wall = x == SX1 - 1 || x == SX2 + 1 || z == SZ1 - 1 || z == SZ2 + 1;
                        set(x, y, z, wall ? (chance(0.85) ? "stone_bricks" : "mossy_stone_bricks") : "air");
                    }
                }
            }
            for (int x = SX1; x <= SX2; x++) {
                for (int z = SZ1; z <= SZ2; z++) {
                    set(x, UY - 1, z, "stone_bricks");
                    // krata nad szybem
                    set(x, 0, z, "iron_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]");
                }
            }
            // dziury w kracie
            set(36, 0, 37, "air");
            set(37, 0, 35, "air");
            // barierka dookoła, przejście przy drabinie
            for (int x = SX1 - 1; x <= SX2 + 1; x++) {
                set(x, 1, SZ1 - 1, "iron_bars");
                set(x, 1, SZ2 + 1, "iron_bars");
            }
            for (int z = SZ1 - 1; z <= SZ2 + 1; z++) {
                set(SX1 - 1, 1, z, "iron_bars");
                set(SX2 + 1, 1, z, "iron_bars");
            }
            set(SX1 - 1, 1, 36, "air");
            // drabina ewakuacyjna na zachodniej ścianie szybu
            for (int y = UY; y <= 0; y++) {
                set(SX1, y, 36, "ladder[facing=east,waterlogged=false]");
            }
            // półki odpoczynkowe
            set(SX1, -6, 37, "stone_brick_slab[type=top]");
            set(SX1, -3, 35, "stone_brick_slab[type=top]");
            // klatka szybowa utknęła na dnie: kraty z dwóch stron, wybite drzwi, dach
            for (int x = SX1 + 1; x <= SX2; x++) {
                for (int y = UY; y <= UY + 3; y++) {
                    set(x, y, SZ1, "iron_bars");
                    set(x, y, SZ2, "iron_bars");
                }
                for (int z = SZ1; z <= SZ2; z++) {
                    set(x, UY + 4, z, chance(0.8) ? "iron_block" : "waxed_weathered_cut_copper");
                    set(x, UY - 1, z, "spruce_planks");
                }
            }
            set(SX2, UY, SZ1 + 1, "iron_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]");
            // liny od kół wieży
            for (int y = UY + 5; y <= -1; y++) {
                set(36, y, 36, "chain[axis=y]");
            }
            set(36, 0, 36, "air");
        }

        private void underground() {
            // podszybie: hala x 38-48 / z 33-40, stopy y -10..-7, strop y -6
            int hx1 = SX2 + 1;
            int hx2 = 48;
            int hz1 = 33;
            int hz2 = 40;
            carve(hx1, UY, hz1, hx2, UY + 3, hz2);
            lining(hx1, hz1, hx2, hz2, UY + 3);
            for (int x = hx1 + 1; x <= hx2; x += 3) {
                for (int z = hz1; z <= hz2; z++) {
                    set(x, UY + 4, z, "iron_block");
                }
            }
            for (int x = hx1; x <= hx2; x++) {
                set(x, UY, 36, "rail[shape=east_west]");
            }
            set(41, UY, 36, "cauldron");
            set(42, UY, 36, "cauldron");
            set(45, UY, 38, "cauldron");
            set(46, UY, 38, "coal_block");
            set(44, UY + 3, 36, "lantern[hanging=true,waterlogged=false]");
            sign(39, UY + 2, hz1, "oak_wall_sign[facing=south]", "DO SZYBU", "→", "", "");
            crate(47, UY, hz1, "south", "podszybie");
            crate(40, UY, hz2, "north", "podszybie");
            // ławka pod tablicą, więcej lamp pod stropem hali i w klatce szybowej
            f.bench(41, UY, hz1, 4, true, "north", "spruce");
            f.hangingLamp(40, UY + 3, 34);
            f.hangingLamp(47, UY + 3, 39);
            f.hangingLamp(36, UY + 3, 37);
            breakRoom();

            // chodnik na wschód: x 49-72, z 35-37, stopy y -10..-8
            carve(hx2 + 1, UY, 35, 72, UY + 2, 37);
            for (int x = hx2 + 1; x <= 72; x++) {
                set(x, UY, 36, "rail[shape=east_west]");
                if (x % 4 == 0) {
                    fill(x, UY, 35, x, UY + 1, 35, "oak_fence");
                    fill(x, UY, 37, x, UY + 1, 37, "oak_fence");
                    fill(x, UY + 2, 35, x, UY + 2, 37, "oak_log[axis=z]");
                }
            }
            set(57, UY + 1, 36, "lantern[hanging=true,waterlogged=false]");
            set(57, UY + 2, 36, "oak_log[axis=z]");
            set(54, UY, 36, "cauldron");
            set(66, UY, 36, "cauldron");
            // lampy pod stropem chodnika (nad głową, między odrzwiami)
            for (int x : new int[]{51, 55, 63, 69}) {
                f.hangingLamp(x, UY + 2, 36);
            }
            for (int x : new int[]{50, 59, 67}) {
                set(x, UY + 2, chance(0.5) ? 35 : 37, "cobweb");
            }
            // zabarykadowany boczny chodnik z zawałem i szkielet przed barykadą
            fill(70, UY, 38, 71, UY + 1, 41, "air");
            fill(70, UY, 40, 71, UY + 1, 41, rubble);
            set(70, UY, 38, "crafting_table");
            set(70, UY + 1, 38, "oak_fence");
            set(71, UY, 38, "oak_fence");
            set(71, UY + 1, 38, "spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]");
            set(70, UY + 1, 39, "cobweb");
            sign(70, UY, 37, "oak_wall_sign[facing=north]", "ZAWAŁ", "NIE WCHODZIĆ", "TAM ZOSTAŁ", "HENIEK");
            f.skeleton(71, UY, 37, "east");

            // przekop na północ do składu materiałów wybuchowych
            carve(59, UY, 29, 61, UY + 2, 34);
            for (int z = 30; z <= 32; z += 2) {
                fill(59, UY + 2, z, 61, UY + 2, z, "oak_log[axis=x]");
            }
            carve(57, UY, 23, 63, UY + 2, 27);
            lining(57, 23, 63, 27, UY + 2);
            fill(59, UY, 28, 61, UY + 2, 28, "iron_bars");
            fill(61, UY, 28, 61, UY + 1, 28, "air");
            // regały z laskami dynamitu (czerwone świece) i skrzynka zapalników
            for (int x = 57; x <= 63; x += 2) {
                set(x, UY, 23, "spruce_planks");
                set(x, UY + 1, 23, chance(0.6) ? "red_candle[candles=" + (3 + rnd.nextInt(2)) + ",lit=false,waterlogged=false]"
                        : "spruce_slab[type=bottom]");
            }
            set(63, UY, 25, "red_candle[candles=4,lit=false,waterlogged=false]");
            f.hangingLamp(60, UY + 2, 25);
            f.hangingLamp(60, UY + 2, 33);
            crate(63, UY, 27, "west", "magazyn");
            set(57, UY, 26, "spruce_planks");
            set(60, UY + 2, 28, "iron_block");
            sign(60, UY + 2, 29, "oak_wall_sign[facing=south]", "MATERIAŁY", "WYBUCHOWE", "WSTĘP", "WZBRONIONY");
            crate(60, UY, 24, "south", "podszybie-sklad");

            // zalana komora pomp na końcu chodnika
            carve(73, UY, 31, 78, UY + 4, 41);
            lining(73, 31, 78, 41, UY + 4);
            for (int x = 74; x <= 78; x++) {
                for (int z = 37; z <= 41; z++) {
                    if (x > 74 || z > 38) {
                        set(x, UY - 1, z, "water");
                    }
                }
            }
            fill(76, UY, 33, 77, UY + 1, 34, "iron_block");
            set(76, UY + 2, 33, "waxed_weathered_cut_copper");
            fill(77, UY + 2, 34, 77, UY + 4, 34, "chain[axis=y]");
            for (int z = 35; z <= 40; z++) {
                set(77, UY, z, "lightning_rod[facing=south]");
            }
            set(75, UY + 4, 36, "lantern[hanging=true,waterlogged=false]");
            f.hangingLamp(74, UY + 4, 40);
            f.hangingLamp(77, UY + 4, 32);
            crate(74, UY, 32, "east", "podszybie");
            crate(78, UY, 31, "west", "zaplecze");
            set(73, UY, 40, "crafting_table");
            set(73, UY, 41, "grindstone[face=floor,facing=east]");
            sign(73, UY + 2, 38, "oak_wall_sign[facing=east]", "POMPY STOJĄ", "WODA IDZIE", "SPADAJ", "GÓRĄ");
        }

        /**
         * Kanciapa sztygara za halą podszybia (x 41-48 / z 42-46): szafki, stół z krzesłami, ławka, zlew,
         * zapalona lampa. Ślepa, jedno wejście z hali.
         */
        private void breakRoom() {
            carve(41, UY, 42, 48, UY + 2, 46);
            lining(41, 42, 48, 46, UY + 2);
            fill(44, UY, 41, 44, UY + 1, 41, "air");
            for (int z = 42; z <= 44; z++) {
                f.wardrobe(41, UY, z, "east", "spruce");
            }
            f.desk(46, UY, 44, 47, 44, "spruce");
            f.chair(46, UY, 43, "north", "spruce");
            f.chair(47, UY, 45, "south", "spruce");
            set(46, UY + 1, 44, "lantern[hanging=false,waterlogged=false]");
            set(47, UY + 1, 44, "flower_pot");
            f.bench(43, UY, 46, 3, true, "south", "spruce");
            f.sink(48, UY, 46, "east");
            f.hangingLamp(44, UY + 2, 44);
            crate(48, UY, 42, "west", "lada");
            crate(41, UY, 46, "east", "zaplecze");
            sign(45, UY + 1, 42, "spruce_wall_sign[facing=south]", "KTO ZŻARŁ", "MI KANAPKI", "Z SZAFKI?!", "- ZBYSZEK");
        }

        private void carve(int x1, int y1, int z1, int x2, int y2, int z2) {
            fill(x1, y1, z1, x2, y2, z2, "air");
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, y1 - 1, z, chance(0.8) ? "stone_bricks" : "gravel");
                }
            }
        }

        /** Obudowa z cegły wokół wykutej komory (ściany i strop). */
        private void lining(int x1, int z1, int x2, int z2, int top) {
            for (int y = UY; y <= top + 1; y++) {
                for (int x = x1 - 1; x <= x2 + 1; x++) {
                    for (int z = z1 - 1; z <= z2 + 1; z++) {
                        boolean edge = x == x1 - 1 || x == x2 + 1 || z == z1 - 1 || z == z2 + 1 || y == top + 1;
                        if (edge && isRock(get(x, y, z))) {
                            set(x, y, z, chance(0.85) ? "bricks" : "mud_bricks");
                        }
                    }
                }
            }
        }

        private boolean isCrate(int x, int y, int z) {
            Blueprint.Pos pos = new Blueprint.Pos(x, y + G, z);
            return bp.spawnPoints().stream().anyMatch(p -> p.pos().equals(pos));
        }

        private static boolean isRock(String state) {
            return switch (state) {
                case "minecraft:stone", "minecraft:andesite", "minecraft:tuff", "minecraft:coal_ore",
                     "minecraft:dirt", "minecraft:coarse_dirt" -> true;
                default -> false;
            };
        }

        // ------------------------------------------------------------------ wieża szybowa

        /** Stalowa kratownica nad nadszybiem, platforma y35 z budką i dwoma kołami linowymi. */
        private void headframe() {
            int top = 35;
            for (int[] leg : legs()) {
                fill(leg[0], NROOF + 1, leg[1], leg[0], top - 1, leg[1], "iron_block");
            }
            for (int y = 18; y < top; y += 6) {
                for (int i = 33; i <= 38; i++) {
                    set(i, y, 33, "iron_bars");
                    set(i, y, 40, "iron_bars");
                }
                for (int i = 34; i <= 39; i++) {
                    set(32, y, i, "iron_bars");
                    set(39, y, i, "iron_bars");
                }
            }
            for (int x = 31; x <= 40; x++) {
                for (int z = 32; z <= 41; z++) {
                    set(x, top, z, chance(0.85) ? "iron_block" : "polished_andesite");
                }
            }
            // koła linowe (oś wzdłuż x), szprychy z płotów
            for (int wx : new int[]{33, 35}) {
                for (int z = 32; z <= 41; z++) {
                    for (int y = top + 1; y <= top + 10; y++) {
                        double d = Math.hypot(z - 36.5, y - (top + 5.0));
                        if (d > 3.6 && d <= 4.6) {
                            set(wx, y, z, steel.next());
                        } else if (d <= 3.6 && (Math.abs(z - 36.5) < 0.6 || Math.abs(y - (top + 5.0)) < 0.6)) {
                            set(wx, y, z, "spruce_fence");
                        }
                    }
                }
                set(wx, top + 5, 36, "iron_block");
                set(wx, top + 5, 37, "iron_block");
            }
            fill(34, top + 5, 36, 34, top + 5, 37, "iron_block");
            // budka przy kołach z rzadką skrzynią
            int bx1 = 37;
            int bx2 = 40;
            int bz1 = 37;
            int bz2 = 41;
            for (int y = top + 1; y <= top + 3; y++) {
                for (int x = bx1; x <= bx2; x++) {
                    set(x, y, bz1, sheet.next());
                    set(x, y, bz2, sheet.next());
                }
                for (int z = bz1; z <= bz2; z++) {
                    set(bx1, y, z, sheet.next());
                    set(bx2, y, z, sheet.next());
                }
            }
            for (int x = bx1; x <= bx2; x++) {
                for (int z = bz1; z <= bz2; z++) {
                    set(x, top + 4, z, "iron_block");
                }
            }
            door(39, top + 1, bz1, "iron_door", "north", "left", true);
            set(bx2, top + 2, 39, brokenGlass());
            set(bx1, top + 2, 39, brokenGlass());
            set(38, top + 2, bz2, brokenGlass());
            crate(38, top + 1, 40, "east", "wieza-szybowa");
            set(39, top + 3, 40, "lantern[hanging=true,waterlogged=false]");
            set(38, top + 5, 39, "lightning_rod[facing=up]");
        }

        /**
         * Zygzakowate schody stalowe po wschodniej stronie wieży. Biegi po x 47 (na północ) i x 49 (na południe),
         * podesty na z 33-34 i z 41-42. Trzeci bieg urwany: z ostatniego stopnia skacze się na belkę
         * kratownicy x 46 i z niej na wyższy podest.
         */
        private void towerStairs() {
            int[] landingY = {0, 7, 14, 21, 28, 35};
            for (int i = 1; i < landingY.length; i++) {
                int z1 = i % 2 == 1 ? 33 : 41;
                for (int x = 47; x <= 49; x++) {
                    for (int z = z1; z <= z1 + 1; z++) {
                        set(x, landingY[i], z, chance(0.85) ? "waxed_weathered_cut_copper" : "iron_block");
                    }
                }
            }
            for (int f = 0; f < 5; f++) {
                int base = landingY[f];
                boolean north = f % 2 == 0;
                int x = north ? 47 : 49;
                for (int s = 0; s < 6; s++) {
                    int z = north ? 40 - s : 35 + s;
                    int y = base + 1 + s;
                    if (f == 2 && s >= 5) {
                        continue; // urwany ostatni stopień trzeciego biegu
                    }
                    set(x, y, z, "waxed_weathered_cut_copper_stairs[facing=" + (north ? "north" : "south")
                            + ",half=bottom,shape=straight,waterlogged=false]");
                }
            }
            // wiszący krzywo fragment urwanego stopnia
            set(48, 18, 35, "waxed_weathered_cut_copper_slab[type=top,waterlogged=false]");
            // belka kratownicy nad przepaścią
            for (int z = 33; z <= 38; z++) {
                set(46, 20, z, "iron_block");
            }
            set(46, 19, 38, "iron_bars");
            set(46, 19, 33, "iron_bars");
            // słupy (kraty, nie da się po nich wspiąć)
            for (int z : new int[]{33, 42}) {
                for (int y = 1; y <= 34; y++) {
                    if (isAir(50, y, z)) {
                        set(50, y, z, "iron_bars");
                    }
                }
            }
            // kładka z górnego podestu do platformy wieży
            for (int x = 41; x <= 46; x++) {
                set(x, 35, 33, "iron_block");
                set(x, 35, 34, chance(0.8) ? "iron_block" : "waxed_weathered_cut_copper");
            }
        }

        // ------------------------------------------------------------------ maszynownia wyciągowa

        private void winderHouse() {
            int roof = 12;
            walls(MX1, MZ1, MX2, MZ2, 1, roof - 1, brick);
            for (int x = MX1; x <= MX2; x++) {
                for (int z = MZ1; z <= MZ2; z++) {
                    set(x, roof, z, chance(0.9) ? "smooth_stone" : "andesite");
                    if (x > MX1 && x < MX2 && z > MZ1 && z < MZ2) {
                        set(x, 0, z, chance(0.8) ? "polished_andesite" : "stone_bricks");
                    }
                }
            }
            door(MX1, 1, 37, "iron_door", "west", "left", true);
            fill(MX1, 1, 38, MX1, 3, 38, "air");
            for (int x = MX1 + 2; x <= MX2 - 2; x += 3) {
                for (int y = 7; y <= 9; y++) {
                    set(x, y, MZ1, brokenGlass());
                    set(x, y, MZ2, brokenGlass());
                }
            }
            for (int z = MZ1 + 3; z <= MZ2 - 3; z += 3) {
                for (int y = 7; y <= 9; y++) {
                    set(MX2, y, z, brokenGlass());
                }
            }
            // bęben maszyny wyciągowej (oś wzdłuż x), łożyska po bokach
            for (int x = 57; x <= 64; x++) {
                for (int z = 33; z <= 41; z++) {
                    for (int y = 1; y <= 7; y++) {
                        if (Math.hypot(z - 37, y - 4) <= 3.2) {
                            boolean band = x == 57 || x == 64 || x == 60;
                            set(x, y, z, band ? "waxed_weathered_cut_copper" : chance(0.85) ? "iron_block" : "waxed_copper_block");
                        }
                    }
                }
            }
            fill(65, 1, 36, 65, 4, 38, "polished_blackstone");
            // kratownica przy bębnie: stopnie z przerwami
            int[][] truss = {{54, 40, 1}, {54, 38, 2}, {54, 36, 3}, {54, 34, 4}, {56, 34, 5}, {56, 36, 6}};
            for (int[] t : truss) {
                set(t[0], t[2], t[1], "iron_block");
                for (int y = 1; y < t[2]; y++) {
                    set(t[0], y, t[1], "iron_bars");
                }
            }
            // belka suwnicy w poprzek hali i hak na łańcuchu
            for (int z = MZ1 + 1; z < MZ2; z++) {
                set(66, 8, z, "iron_block");
            }
            fill(66, 4, 39, 66, 7, 39, "chain[axis=y]");
            // pomost pulpitu maszynisty pod północną ścianą
            for (int x = 59; x <= 64; x++) {
                for (int z = MZ1 + 1; z <= MZ1 + 2; z++) {
                    set(x, 9, z, chance(0.85) ? "iron_block" : "waxed_weathered_cut_copper");
                }
            }
            for (int y = 1; y <= 8; y++) {
                set(59, y, MZ1 + 1, "iron_bars");
            }
            set(62, 10, MZ1 + 1, "lever[face=floor,facing=south,powered=false]");
            set(63, 10, MZ1 + 1, "lever[face=floor,facing=south,powered=true]");
            set(64, 10, MZ1 + 1, "observer[facing=south,powered=false]");
            crate(60, 10, MZ1 + 1, "south", "maszynownia");
            // warsztat konserwatorów pod wschodnią ścianą, pulpit z dziennikiem zmian
            set(MX2 - 1, 1, 40, "crafting_table");
            set(MX2 - 1, 1, 41, "anvil[facing=north]");
            set(MX2 - 1, 1, 42, "grindstone[face=floor,facing=west]");
            crate(MX2 - 1, 1, MZ2 - 1, "north", "zaplecze");
            set(66, 1, MZ1 + 1, "lectern[facing=south,has_book=false,powered=false]");
            crate(MX2 - 1, 1, MZ1 + 1, "west", "parter");
            for (int i = 0; i < 15; i++) {
                int x = MX1 + 1 + rnd.nextInt(MX2 - MX1 - 1);
                int z = MZ1 + 1 + rnd.nextInt(MZ2 - MZ1 - 1);
                if (isAir(x, roof - 1, z)) {
                    set(x, roof - 1, z, "cobweb");
                }
            }
        }

        // ------------------------------------------------------------------ sortownia

        private void sorting() {
            int roof = 18;
            for (int y = 1; y < roof; y++) {
                Palette p = y <= 5 ? brick : sheet;
                for (int x = RX1; x <= RX2; x++) {
                    set(x, y, RZ1, p.next());
                    set(x, y, RZ2, p.next());
                }
                for (int z = RZ1; z <= RZ2; z++) {
                    set(RX1, y, z, p.next());
                    set(RX2, y, z, p.next());
                }
            }
            for (int x = RX1; x <= RX2; x++) {
                for (int z = RZ1; z <= RZ2; z++) {
                    set(x, roof, z, chance(0.85) ? "light_gray_concrete" : "andesite");
                    boolean inside = x > RX1 && x < RX2 && z > RZ1 && z < RZ2;
                    if (inside) {
                        set(x, 0, z, "smooth_stone");
                        set(x, 6, z, chance(0.85) ? "iron_block" : "polished_andesite");
                        set(x, 12, z, chance(0.85) ? "iron_block" : "polished_andesite");
                    }
                }
            }
            // wjazd dla bocznicy
            fill(RX1, 1, 16, RX1, 3, 18, "air");
            // okna: najwyższe piętro na cztery strony, parter od zachodu
            for (int x = RX1 + 2; x <= RX2 - 2; x += 2) {
                set(x, 14, RZ1, brokenGlass());
                set(x, 15, RZ1, brokenGlass());
                set(x, 14, RZ2, brokenGlass());
                set(x, 15, RZ2, brokenGlass());
            }
            for (int z = RZ1 + 2; z <= RZ2 - 2; z += 3) {
                set(RX1, 14, z, brokenGlass());
                set(RX1, 15, z, brokenGlass());
                set(RX2, 15, z, brokenGlass());
                set(RX1, 2, z, brokenGlass());
            }
            // zawalone schody: gruz na dole, resztka stopni pod stropem
            fill(64, 6, 11, 65, 6, 13, "air");
            set(65, 1, 13, rubble.next());
            set(65, 1, 14, rubble.next());
            set(64, 1, 13, rubble.next());
            set(65, 2, 13, rubble.next());
            set(65, 5, 12, "stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
            set(65, 4, 13, "stone_brick_stairs[facing=north,half=top,shape=straight,waterlogged=false]");
            // środkowe piętro: sita i leje, pochyły taśmociąg na górę
            for (int x = 61; x <= 63; x += 2) {
                for (int z = 11; z <= 17; z += 3) {
                    set(x, 7, z, "cauldron");
                    set(x, 8, z, "iron_bars");
                }
            }
            for (int s = 0; s < 6; s++) {
                set(60, 7 + s, 17 - s, "polished_blackstone_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
            }
            fill(60, 12, 13, 60, 12, 17, "air");
            set(60, 12, 12, "polished_blackstone");
            // najwyższe piętro: lepsze skrzynie
            crate(RX1 + 1, 13, RZ1 + 1, "east", "strych");
            crate(RX2 - 1, 13, RZ2 - 1, "west", "strych");
            set(62, 13, 10, "grindstone[face=floor,facing=north]");
            fill(63, 13, 18, 64, 13, 18, "cauldron");
            crate(RX2 - 1, 7, RZ2 - 1, "west", "magazyn");
            crate(RX1 + 1, 1, RZ1 + 1, "east", "magazyn");
            set(RX1 + 1, 1, RZ1 + 3, "crafting_table");
            sign(RX2 - 1, 2, 16, "oak_wall_sign[facing=west]", "WĘGIEL SIĘ", "SKOŃCZYŁ.", "NADZIEJA", "TEŻ");

            // rampa załadunkowa i zewnętrzny taśmociąg (1 blok, bez barierek)
            for (int x = 66; x <= 71; x++) {
                for (int z = 24; z <= 28; z++) {
                    set(x, 1, z, chance(0.8) ? "smooth_stone" : "cracked_stone_bricks");
                }
            }
            for (int s = 0; s <= 10; s++) {
                int z = 23 - s;
                int y = 2 + s;
                set(68, y, z, "polished_blackstone_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
                if (s % 4 == 2) {
                    fill(68, 1, z, 68, y - 1, z, "andesite_wall");
                }
            }
            set(67, 12, 13, "polished_blackstone");
            fill(RX2, 13, 13, RX2, 14, 13, "air");
            // zasobnik nad bocznicą i zamknięta galeria do sortowni
            for (int[] leg : new int[][]{{50, 15}, {53, 15}, {50, 19}, {53, 19}}) {
                fill(leg[0], 1, leg[1], leg[0], 4, leg[1], "andesite_wall");
            }
            fill(50, 5, 15, 53, 8, 19, sheet);
            fill(54, 7, 17, RX1 - 1, 9, 17, sheet);
        }

        // ------------------------------------------------------------------ hałda i sztolnia

        private void heap() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    double d = Math.hypot(x - HCX, z - HCZ);
                    int h = d >= HR ? 0 : (int) Math.round(HTOP * (1 - d / HR) + (chance(0.3) ? 0.6 : 0));
                    heap[x][z] = h;
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, coal.next());
                    }
                    if (h > 0) {
                        double r = rnd.nextDouble();
                        String top = r < 0.4 ? "grass_block" : r < 0.55 ? "coarse_dirt" : r < 0.6 ? "podzol" : coal.next();
                        set(x, h, z, top);
                    }
                }
            }
            // brzozy na zboczach
            for (int[] t : new int[][]{{8, 58}, {24, 60}, {19, 66}, {10, 64}, {22, 51}, {12, 49}}) {
                int h = heap[t[0]][t[1]];
                if (h <= 0) {
                    continue;
                }
                int trunk = 4 + rnd.nextInt(2);
                for (int y = h + 1; y <= h + trunk; y++) {
                    set(t[0], y, t[1], "birch_log[axis=y]");
                }
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        for (int y = h + trunk - 1; y <= h + trunk + 1; y++) {
                            int r = y > h + trunk ? 1 : 2;
                            if (Math.abs(dx) <= r && Math.abs(dz) <= r && !(Math.abs(dx) == 2 && Math.abs(dz) == 2)
                                    && isAir(t[0] + dx, y, t[1] + dz)) {
                                set(t[0] + dx, y, t[1] + dz, "birch_leaves[distance=1,persistent=true,waterlogged=false]");
                            }
                        }
                    }
                }
            }
            // przewrócony wózek na szczycie
            int h = heap[HCX][HCZ];
            set(HCX, h + 1, HCZ, "cauldron");
            set(HCX + 1, h + 1, HCZ, "coal_block");
        }

        /** Sztolnia u podnóża hałdy: wcięcie, chodnik z dwoma rozgałęzieniami i zawałem, komora na końcu. */
        private void adit() {
            // wcięcie w zbocze
            fill(15, 1, 45, 17, HTOP, 48, "air");
            // chodnik główny
            fill(15, 1, 49, 17, 3, 59, "air");
            for (int z = 49; z <= 59; z += 3) {
                fill(15, 1, z, 15, 2, z, "oak_fence");
                fill(17, 1, z, 17, 2, z, "oak_fence");
                fill(15, 3, z, 17, 3, z, "oak_log[axis=x]");
            }
            for (int z = 45; z <= 63; z++) {
                set(16, 1, z, "rail[shape=north_south]");
            }
            // gałąź wschodnia z zawałem
            fill(18, 1, 52, 22, 2, 53, "air");
            fill(21, 1, 52, 22, 1, 53, rubble);
            set(21, 2, 52, "cobblestone");
            set(22, 2, 53, "tuff");
            // gałąź zachodnia ze starym wózkiem
            fill(10, 1, 55, 14, 2, 56, "air");
            set(10, 1, 55, "cauldron");
            set(11, 1, 55, "cauldron");
            set(13, 2, 56, "cobweb");
            // komora na końcu z porzuconym sprzętem
            fill(13, 1, 60, 19, 3, 64, "air");
            for (int x : new int[]{13, 19}) {
                fill(x, 1, 60, x, 2, 60, "oak_fence");
            }
            fill(13, 3, 60, 19, 3, 60, "oak_log[axis=x]");
            set(18, 1, 64, "smithing_table");
            set(17, 1, 64, "grindstone[face=floor,facing=north]");
            set(14, 1, 61, "anvil[facing=north]");
            set(16, 3, 62, "cobweb");
            crate(13, 1, 63, "east", "sztolnia");
            crate(19, 1, 62, "west", "sztolnia");
            crate(20, 1, 52, "west", "plac");
            // lampy pod stropem chodnika i komory
            f.hangingLamp(16, 3, 53);
            f.hangingLamp(15, 3, 63);
            // obudowa wejścia
            fill(14, 1, 49, 14, 3, 49, "oak_log[axis=y]");
            fill(18, 1, 49, 18, 3, 49, "oak_log[axis=y]");
            fill(14, 4, 49, 18, 4, 49, "oak_log[axis=x]");
            sign(18, 2, 48, "oak_wall_sign[facing=north]", "SZTOLNIA", "„JEDNOŚĆ”", "", "");
        }

        // ------------------------------------------------------------------ zarośla

        private void weeds() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 6; z < DEPTH; z++) {
                    String below = get(x, 0, z);
                    if (heap[x][z] > 0 || (!below.equals("minecraft:grass_block") && !below.equals("minecraft:podzol")
                            && !below.equals("minecraft:coarse_dirt"))) {
                        continue;
                    }
                    if (!isAir(x, 1, z) || !isAir(x, 2, z) || isCrate(x, 1, z) || !chance(0.18)) {
                        continue;
                    }
                    double r = rnd.nextDouble();
                    if (r < 0.3) {
                        set(x, 1, z, "tall_grass[half=lower]");
                        set(x, 2, z, "tall_grass[half=upper]");
                    } else if (r < 0.75) {
                        set(x, 1, z, "short_grass");
                    } else {
                        set(x, 1, z, "fern");
                    }
                }
            }
        }
    }
}
