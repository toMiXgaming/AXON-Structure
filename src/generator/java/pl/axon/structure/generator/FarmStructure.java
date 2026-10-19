package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.Random;

/**
 * Opuszczone gospodarstwo rolne „Agro-Pol” (dawny PGR) – podwórze w kształcie podkowy.
 * <p>
 * Plan w lokalnych współrzędnych (front na północy, z = 0):
 * <pre>
 *  z 0-3    dziurawa droga
 *  z 5      betonowy płot z bramą (x 24-31) i tablicą ZAKŁAD ROLNY AGRO-POL
 *  z 6-42   podwórze: Ursus na cegłach, przyczepa, studnia, jomby; przed domem przewrócony rozrzutnik
 *           stodoła x 2-14 / z 8-40 (warsztat, kombajn, strych od z 27)
 *           dom „kostka” x 38-47 / z 22-31, balkon z 20-21, szopa na węgiel x 34-37 / z 18-24
 *  z 43-53  za stodołą: beka paliwowa i beczki, dwa silosy (drabina w klatce na zachodnim)
 *  z 55     tylny płot
 * </pre>
 * Parkour: strych stodoły – heder kombajnu, maska, kabina, sterta siana, krawędź strychu.
 * Piętro domu – stos drewna, niski i wysoki dach szopy, balkon.
 */
public final class FarmStructure implements StructureDefinition {

    private static final int WIDTH = 51;
    private static final int HEIGHT = 24;
    private static final int DEPTH = 57;

    // stodoła
    private static final int SX1 = 2;
    private static final int SX2 = 14;
    private static final int SZ1 = 8;
    private static final int SZ2 = 40;
    private static final int BARN_WALL_TOP = 7;
    private static final int LOFT_Y = 5;
    private static final int LOFT_Z = 27;

    // dom
    private static final int HX1 = 38;
    private static final int HX2 = 47;
    private static final int HZ1 = 22;
    private static final int HZ2 = 31;
    private static final int UPPER_Y = 5;
    private static final int HOUSE_ROOF_Y = 10;

    // silosy
    private static final int SILO_Z = 48;
    private static final int SILO_A_X = 24;
    private static final int SILO_B_X = 32;
    private static final int SILO_TOP = 17;

    @Override
    public String id() {
        return "gospodarstwo";
    }

    @Override
    public String displayName() {
        return "Opuszczone gospodarstwo Agro-Pol";
    }

    @Override
    public String description() {
        return "Dawny PGR: stodoła ze strychem, dom-kostka z balkonem, silosy i beka paliwowa";
    }

    @Override
    public Blueprint build(Random random) {
        return new Builder(random).build();
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);

        private final Palette yard;
        private final Palette grass;
        private final Palette barnLower;
        private final Palette barnUpper;
        private final Palette eternit;
        private final Palette plaster;
        private final Palette tarPaper;
        private final Palette rust;
        private final Palette siloSheet;
        private final Palette rubble;
        private final Palette hay;
        private final Furniture f;

        Builder(Random rnd) {
            this.rnd = rnd;
            this.f = new Furniture(bp, 0, rnd);
            yard = new Palette(rnd)
                    .add("coarse_dirt", 5).add("mud", 3).add("dirt", 3).add("grass_block", 4)
                    .add("packed_mud", 2).add("rooted_dirt", 1);
            grass = new Palette(rnd)
                    .add("grass_block", 14).add("coarse_dirt", 2).add("dirt", 1).add("podzol", 1);
            // pustak (szary) na dole, biała cegła sylikatowa wyżej
            barnLower = new Palette(rnd)
                    .add("light_gray_concrete", 6).add("stone_bricks", 3).add("cracked_stone_bricks", 2)
                    .add("andesite", 1).add("mossy_stone_bricks", 1);
            barnUpper = new Palette(rnd)
                    .add("calcite", 6).add("white_terracotta", 3).add("polished_diorite", 2).add("diorite", 1);
            eternit = new Palette(rnd)
                    .add("andesite", 7).add("stone", 2).add("cobblestone", 1);
            // tynk „baranek”
            plaster = new Palette(rnd)
                    .add("white_terracotta", 6).add("calcite", 3).add("diorite", 2).add("light_gray_terracotta", 1);
            tarPaper = new Palette(rnd)
                    .add("black_concrete", 6).add("black_terracotta", 2).add("gray_concrete", 1);
            rust = new Palette(rnd)
                    .add("orange_terracotta", 5).add("brown_terracotta", 2).add("terracotta", 2).add("red_terracotta", 1);
            siloSheet = new Palette(rnd)
                    .add("orange_terracotta", 3).add("brown_terracotta", 1).add("waxed_exposed_cut_copper", 1);
            rubble = new Palette(rnd)
                    .add("cobblestone", 4).add("andesite", 2).add("cracked_stone_bricks", 2).add("white_terracotta", 1);
            hay = new Palette(rnd)
                    .add("hay_block[axis=y]", 6).add("hay_block[axis=x]", 2).add("brown_wool", 2).add("podzol", 1);
        }

        Blueprint build() {
            ground();
            road();
            fence();
            gateSign();
            yardPaving();
            well();
            tractor();
            trailer();
            spreader();
            yardBarrels();
            barnShell();
            barnRoof();
            barnDoor();
            workshop();
            combineParkour();
            loft();
            house();
            houseGroundFloor();
            houseUpperFloor();
            balconyAndShed();
            fuelTank();
            silo(SILO_A_X, true);
            silo(SILO_B_X, false);
            outhouse();
            squatterCamp();
            graffiti();
            extraYardCrates();
            lifeSigns();
            weeds();
            weathering();
            return bp;
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        // ------------------------------------------------------------------ teren

        private boolean inYard(int x, int z) {
            return x >= 15 && x <= 37 && z >= 6 && z <= 42;
        }

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    bp.set(x, 0, z, inYard(x, z) ? yard.next() : grass.next());
                }
            }
        }

        private void road() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z <= 3; z++) {
                    String block = chance(0.8) ? "gray_concrete" : chance(0.6) ? "andesite" : "cobblestone";
                    bp.set(x, 0, z, block);
                }
            }
            // dziury w asfalcie
            for (int i = 0; i < 9; i++) {
                int x = rnd.nextInt(WIDTH - 2);
                int z = rnd.nextInt(3);
                bp.set(x, 0, z, "mud");
                bp.set(x + 1, 0, z, chance(0.5) ? "coarse_dirt" : "gravel");
                if (chance(0.4)) {
                    bp.set(x, 0, z + 1, "coarse_dirt");
                }
            }
            // zjazd do bramy
            for (int x = 24; x <= 31; x++) {
                bp.set(x, 0, 4, chance(0.7) ? "gravel" : "coarse_dirt");
            }
        }

        /** Niski płot z betonowych przęseł z ażurem, miejscami przewrócony. */
        private void fence() {
            for (int x = 1; x <= WIDTH - 2; x++) {
                if (x < 24 || x > 31) {
                    fencePanel(x, 5, x);
                }
                fencePanel(x, DEPTH - 2, x);
            }
            for (int z = 6; z <= DEPTH - 3; z++) {
                fencePanel(1, z, z);
                fencePanel(WIDTH - 2, z, z);
            }
            // słupki bramy i otwarte, zardzewiałe skrzydło
            bp.fill(23, 1, 5, 23, 3, 5, "polished_andesite");
            bp.fill(32, 1, 5, 32, 3, 5, "polished_andesite");
            for (int z = 6; z <= 8; z++) {
                bp.set(33, 1, z, "iron_bars");
            }
            bp.set(33, 2, 6, "iron_bars");
            // przewrócone przęsła
            toppledPanel(true, 8, 5, -1);
            toppledPanel(true, 40, 5, -1);
            toppledPanel(true, 12, DEPTH - 2, 1);
            toppledPanel(true, 40, DEPTH - 2, 1);
            toppledPanel(false, 1, 20, -1);
            toppledPanel(false, WIDTH - 2, 12, 1);
            toppledPanel(false, WIDTH - 2, 40, 1);
        }

        private void fencePanel(int x, int z, int pos) {
            if (pos % 4 == 1) {
                bp.fill(x, 1, z, x, 2, z, "polished_andesite");
                return;
            }
            // ażur w dolnym rzędzie, górny pełny – płot ma 2 bloki i nie da się na niego wskoczyć
            if (pos % 2 == 0) {
                bp.set(x, 1, z, chance(0.9) ? "light_gray_concrete" : "cracked_stone_bricks");
            }
            bp.set(x, 2, z, chance(0.9) ? "light_gray_concrete" : "cracked_stone_bricks");
        }

        /** Przęsło (3 kratki między słupkami) leży na ziemi po zewnętrznej stronie płotu. */
        private void toppledPanel(boolean alongX, int a, int b, int out) {
            for (int i = 1; i <= 3; i++) {
                int x = alongX ? a + i : a;
                int z = alongX ? b : b + i;
                bp.fill(x, 1, z, x, 2, z, "air");
                int lx = alongX ? x : x + out;
                int lz = alongX ? z + out : z;
                bp.set(lx, 1, lz, chance(0.7) ? "smooth_stone_slab[type=bottom]" : "andesite_slab[type=bottom]");
            }
        }

        private void gateSign() {
            // przekrzywiona tablica na dwóch słupkach
            bp.set(17, 3, 5, "iron_bars");
            bp.set(19, 3, 5, "iron_bars");
            for (int x = 17; x <= 21; x++) {
                int low = x <= 19 ? 4 : 3;
                bp.set(x, low, 5, chance(0.8) ? "orange_terracotta" : "brown_terracotta");
                bp.set(x, low + 1, 5, chance(0.8) ? "white_terracotta" : "orange_terracotta");
            }
            bp.sign(18, 5, 4, "oak_wall_sign[facing=north]", "ZAK AD", "ROLNY", "", "");
            bp.sign(20, 4, 4, "oak_wall_sign[facing=north]", "AGRO-P L", "", "", "");
        }

        /** Popękane betonowe płyty („jomby”) od bramy na podwórze, do stodoły i do domu. */
        private void yardPaving() {
            paveStrip(25, 6, 30, 26);
            paveStrip(15, 24, 24, 27);
            paveStrip(29, 25, 34, 27);
        }

        private void paveStrip(int x1, int z1, int x2, int z2) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean joint = x % 3 == 0 || z % 3 == 0;
                    String block;
                    if (chance(0.12)) {
                        block = "mud";
                    } else if (joint && chance(0.4)) {
                        block = "grass_block";
                    } else {
                        block = chance(0.75) ? "smooth_stone" : "cracked_stone_bricks";
                    }
                    bp.set(x, 0, z, block);
                }
            }
        }

        private void well() {
            int cx = 27;
            int cz = 33;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        bp.set(cx, 0, cz, "water");
                        continue;
                    }
                    bp.set(cx + dx, 1, cz + dz, chance(0.7) ? "cobblestone" : "mossy_cobblestone");
                }
            }
            // spróchniały daszek na dwóch słupkach, zerwany łańcuch
            bp.fill(cx - 1, 2, cz, cx - 1, 3, cz, "spruce_fence");
            bp.fill(cx + 1, 2, cz, cx + 1, 3, cz, "spruce_fence");
            bp.set(cx, 3, cz, "stripped_spruce_log[axis=x]");
            bp.set(cx, 2, cz, "chain[axis=y]");
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (chance(0.75)) {
                        bp.set(cx + dx, 4, cz + dz, chance(0.7) ? "spruce_slab[type=bottom]" : "dark_oak_slab[type=bottom]");
                    }
                }
            }
            bp.set(cx + 2, 1, cz + 1, "spruce_slab[type=bottom]");
        }

        // ------------------------------------------------------------------ maszyny na podwórzu

        /** Czerwony Ursus bez prawego tylnego koła, oparty na cegłach. */
        private void tractor() {
            // przednie koła
            bp.set(18, 1, 12, "black_concrete");
            bp.set(22, 1, 12, "black_concrete");
            // maska z silnikiem
            bp.fill(19, 1, 11, 21, 1, 13, "gray_concrete");
            bp.fill(19, 2, 11, 21, 2, 13, "red_concrete");
            bp.set(20, 2, 11, "iron_bars");
            bp.set(21, 2, 12, chance(0.5) ? "red_terracotta" : "red_concrete");
            bp.set(21, 3, 12, "lightning_rod[facing=up]");
            // rama, siedzenie i kierownica
            bp.fill(19, 1, 14, 21, 1, 16, "gray_concrete");
            bp.set(20, 2, 15, "spruce_stairs[facing=south,half=bottom]");
            bp.set(20, 2, 14, "lever[face=floor,facing=north,powered=false]");
            // kabina: słupki i dach z dziurą
            for (int[] p : new int[][]{{19, 14}, {21, 14}, {19, 16}, {21, 16}}) {
                bp.fill(p[0], 2, p[1], p[0], 3, p[1], "iron_bars");
            }
            bp.fill(19, 4, 14, 21, 4, 16, "red_concrete");
            bp.set(20, 4, 14, "air");
            // lewe tylne koło
            bp.fill(18, 1, 15, 18, 2, 16, "black_concrete");
            // prawe tylne koło zdjęte – oś na cegłach
            bp.set(22, 1, 15, "bricks");
            bp.set(22, 1, 16, "brick_slab[type=bottom]");
            bp.set(22, 2, 15, "chain[axis=x]");
            // zaczep do przyczepy
            bp.set(20, 1, 17, "chain[axis=z]");
        }

        private void trailer() {
            bp.fill(19, 1, 18, 21, 1, 24, "dark_oak_planks");
            for (int z = 18; z <= 24; z++) {
                bp.set(19, 2, z, chance(0.85) ? "dark_oak_slab[type=bottom]" : "air");
                bp.set(21, 2, z, chance(0.85) ? "dark_oak_slab[type=bottom]" : "air");
            }
            bp.set(20, 2, 24, "dark_oak_slab[type=bottom]");
            // resztki zgniłej słomy
            for (int z = 18; z <= 23; z++) {
                if (chance(0.7)) {
                    bp.set(20, 2, z, chance(0.5) ? "hay_block[axis=z]" : "brown_carpet");
                }
            }
            for (int z : new int[]{20, 21}) {
                bp.set(18, 1, z, "black_concrete");
                bp.set(22, 1, z, "black_concrete");
            }
        }

        /** Rozrzutnik obornika leży do góry kołami. */
        private void spreader() {
            for (int x = 40; x <= 44; x++) {
                for (int z = 11; z <= 13; z++) {
                    boolean rim = x == 40 || x == 44 || z == 11 || z == 13;
                    bp.set(x, 1, z, rim ? "brown_terracotta" : "mud");
                    bp.set(x, 2, z, rust.next());
                }
            }
            bp.set(42, 3, 11, "black_concrete");
            bp.set(42, 3, 13, "black_concrete");
            bp.set(39, 1, 12, "grindstone[face=floor,facing=west]");
            bp.set(39, 2, 12, "chain[axis=y]");
            for (int x = 37; x <= 39; x++) {
                for (int z = 10; z <= 14; z++) {
                    if (chance(0.5)) {
                        bp.set(x, 0, z, chance(0.5) ? "mud" : "podzol");
                    }
                }
            }
        }

        private void yardBarrels() {
            bp.crate(17, 1, 13, "east", "plac");
            bp.crate(23, 1, 14, "west", "plac");
            bp.crate(17, 1, 18, "east", "plac");
            bp.crate(23, 1, 21, "west", "plac");
            bp.crate(24, 1, 11, "north", "plac");
            // niebieskie i rdzawe beczki po ropie, opony
            bp.set(24, 1, 16, "blue_terracotta");
            bp.set(24, 1, 17, "light_blue_terracotta");
            bp.set(24, 2, 17, "blue_terracotta");
            bp.set(16, 1, 15, "orange_terracotta");
            bp.set(16, 1, 16, "blue_terracotta");
            bp.set(25, 1, 19, "orange_terracotta");
            tires(16, 21);
            tires(25, 13);
            tires(29, 38);
        }

        private void tires(int x, int z) {
            bp.set(x, 1, z, "blackstone_wall");
            if (chance(0.6)) {
                bp.set(x, 2, z, "blackstone_wall");
            }
        }

        // ------------------------------------------------------------------ stodoła

        private void barnShell() {
            for (int y = 1; y <= BARN_WALL_TOP; y++) {
                Palette wall = y <= 2 ? barnLower : barnUpper;
                for (int x = SX1; x <= SX2; x++) {
                    bp.set(x, y, SZ1, wall.next());
                    bp.set(x, y, SZ2, wall.next());
                }
                for (int z = SZ1; z <= SZ2; z++) {
                    bp.set(SX1, y, z, wall.next());
                    bp.set(SX2, y, z, wall.next());
                }
            }
            // szczyty
            for (int x = SX1 + 1; x < SX2; x++) {
                for (int y = BARN_WALL_TOP + 1; y < roofY(x); y++) {
                    bp.set(x, y, SZ1, barnUpper.next());
                    bp.set(x, y, SZ2, barnUpper.next());
                }
            }
            // klepisko
            for (int x = SX1 + 1; x < SX2; x++) {
                for (int z = SZ1 + 1; z < SZ2; z++) {
                    double r = rnd.nextDouble();
                    bp.set(x, 0, z, r < 0.4 ? "coarse_dirt" : r < 0.7 ? "packed_mud" : r < 0.9 ? "dirt" : "rooted_dirt");
                }
            }
            // małe okienka w zachodniej ścianie
            for (int z = 12; z <= 36; z += 6) {
                bp.set(SX1, 3, z, chance(0.5) ? "air" : "iron_bars");
                bp.set(SX1, 4, z, "air");
            }
            // drzwi do warsztatu od frontu
            door(5, 1, SZ1, "spruce_door", "north", "left", true);
            // wybite okienko szczytowe i okno strychu na podwórze
            bp.fill(7, 7, SZ2, 9, 8, SZ2, "air");
            bp.fill(SX2, 6, 31, SX2, 7, 33, "air");
            bp.set(SX2, 7, 32, "cobweb");
            // belki pod dachem (wysoko, żeby nie przeszkadzały w skokach na strych)
            for (int z : new int[]{11, 17, 33, 39}) {
                for (int x = SX1 + 1; x < SX2; x++) {
                    bp.set(x, 8, z, "stripped_spruce_log[axis=x]");
                }
            }
            bp.set(8, 7, 17, "lantern[hanging=true]");
            bp.set(8, 7, 11, "chain[axis=y]");
        }

        /** Wysokość połaci dachu nad kolumną x (dach dwuspadowy z kalenicą na x = 8). */
        private static int roofY(int x) {
            return 7 + Math.min(x - (SX1 - 1), (SX2 + 1) - x);
        }

        private void barnRoof() {
            int ridge = (SX1 + SX2) / 2;
            for (int x = SX1 - 1; x <= SX2 + 1; x++) {
                for (int z = SZ1 - 1; z <= SZ2 + 1; z++) {
                    String block;
                    if (x == ridge) {
                        block = eternit.next();
                    } else {
                        String stair = chance(0.75) ? "andesite_stairs" : chance(0.7) ? "stone_stairs" : "cobblestone_stairs";
                        block = stair + "[facing=" + (x < ridge ? "east" : "west") + ",half=bottom]";
                    }
                    bp.set(x, roofY(x), z, block);
                }
            }
            // spadłe płyty eternitu – widać krokwie
            roofHole(10, 12, 13, 16);
            roofHole(4, 6, 29, 32);
            roofHole(11, 13, 35, 37);
            // płyty leżące przy ścianie
            bp.set(SX2 + 1, 1, 14, "andesite_slab[type=bottom]");
            bp.set(SX2 + 2, 1, 15, "andesite_slab[type=bottom]");
            bp.set(SX1 - 1, 1, 30, "andesite_slab[type=bottom]");
        }

        private void roofHole(int x1, int x2, int z1, int z2) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    bp.set(x, roofY(x), z, z % 2 == 0 ? "stripped_spruce_wood[axis=y]" : "air");
                }
            }
        }

        /** Wrota przesuwne zablokowane w połowie, drugie skrzydło wisi na jednym zawiasie. */
        private void barnDoor() {
            bp.fill(SX2, 1, 20, SX2, 5, 27, "air");
            for (int z = 20; z <= 23; z++) {
                for (int y = 1; y <= 5; y++) {
                    bp.set(SX2 + 1, y, z, chance(0.8) ? "spruce_planks" : "stripped_spruce_wood[axis=y]");
                }
            }
            bp.fill(SX2 + 1, 4, 26, SX2 + 1, 5, 27, "spruce_planks");
            bp.set(SX2 + 1, 3, 27, "spruce_planks");
            for (int z = 19; z <= 28; z++) {
                bp.set(SX2 + 1, 6, z, "chain[axis=z]");
            }
        }

        private void workshop() {
            bp.set(3, 1, 10, "crafting_table");
            bp.set(3, 1, 11, "chipped_anvil[facing=north]");
            bp.set(3, 1, 12, "smithing_table");
            bp.set(3, 1, 13, "spruce_slab[type=top]");
            bp.set(3, 1, 14, "spruce_slab[type=top]");
            bp.set(3, 2, 13, "lantern[hanging=false]");
            bp.set(6, 1, 9, "grindstone[face=floor,facing=north]");
            bp.set(3, 3, 11, "tripwire_hook[facing=east,attached=false,powered=false]");
            // plamy smaru
            for (int[] p : new int[][]{{5, 12}, {6, 13}, {4, 14}, {5, 13}, {7, 11}}) {
                bp.set(p[0], 0, p[1], "black_concrete");
            }
            bp.set(6, 1, 11, "black_carpet");
            // taboret przy warsztacie, beczka w kącie, kompostownik i skrzynka przy zachodniej ścianie
            f.chair(4, 1, 13, "east", "spruce");
            bp.set(3, 1, 9, "barrel[facing=up,open=false]");
            bp.set(3, 1, 22, "composter[level=4]");
            bp.crate(3, 1, 16, "east", "klepisko");
            bp.crate(13, 1, 10, "west", "klepisko");
            bp.crate(7, 1, 9, "south", "klepisko");
            bp.crate(3, 1, 20, "east", "klepisko");
        }

        /**
         * Zardzewiały kombajn i sterta siana – jedyna droga na strych. Każdy krok to +1 blok:
         * heder (y1) → maska (y2) → kabina (y3) → przez kratkę na siano (y4) → przez kratkę na strych (y5).
         */
        private void combineParkour() {
            // opuszczony heder
            for (int x = 8; x <= 13; x++) {
                bp.set(x, 1, 16, x % 2 == 0 ? "yellow_terracotta" : "orange_terracotta");
            }
            // korpus i zbiornik
            for (int x = 9; x <= 12; x++) {
                for (int z = 17; z <= 21; z++) {
                    bp.set(x, 1, z, chance(0.7) ? "yellow_terracotta" : "orange_terracotta");
                    bp.set(x, 2, z, chance(0.7) ? "yellow_terracotta" : "orange_terracotta");
                }
            }
            bp.fill(8, 1, 18, 8, 1, 19, "black_concrete");
            bp.fill(13, 1, 18, 13, 1, 19, "black_concrete");
            // kabina
            bp.fill(10, 3, 20, 11, 3, 21, "yellow_terracotta");
            // sterta bel siana tuż przed strychem
            for (int x = 9; x <= 12; x++) {
                for (int z = 23; z <= 25; z++) {
                    bp.fill(x, 1, z, x, 4, z, hay);
                }
            }
            // złamana drabina – dolna połowa przy ścianie, reszta leży na ziemi
            bp.set(3, 1, 26, "ladder[facing=east]");
            bp.set(3, 2, 26, "ladder[facing=east]");
            bp.set(4, 1, 24, "spruce_trapdoor[facing=east,half=bottom,open=false]");
            // stare siano pod strychem
            for (int x = 3; x <= 7; x++) {
                for (int z = 29; z <= 39; z++) {
                    int h = rnd.nextInt(3);
                    for (int y = 1; y <= h; y++) {
                        bp.set(x, y, z, hay.next());
                    }
                }
            }
            bp.crate(8, 1, 30, "east", "klepisko");
            bp.crate(13, 1, 36, "west", "klepisko");
        }

        private void loft() {
            for (int x = SX1 + 1; x < SX2; x++) {
                for (int z = LOFT_Z; z < SZ2; z++) {
                    bp.set(x, LOFT_Y, z, chance(0.9) ? "spruce_planks" : "stripped_spruce_wood[axis=z]");
                }
            }
            // słupy pod krawędzią strychu
            for (int x : new int[]{3, 8, 13}) {
                bp.fill(x, 1, LOFT_Z, x, LOFT_Y - 1, LOFT_Z, "stripped_spruce_log[axis=y]");
            }
            // barierka tylko nad starym sianem
            for (int x = 3; x <= 7; x++) {
                bp.set(x, LOFT_Y + 1, LOFT_Z, "spruce_fence");
            }
            // siano i lepsze skrzynie
            for (int x = 10; x <= 12; x++) {
                for (int z = 37; z <= 39; z++) {
                    bp.set(x, LOFT_Y + 1, z, hay.next());
                }
            }
            bp.set(3, LOFT_Y + 1, 39, "hay_block[axis=y]");
            bp.crate(4, LOFT_Y + 1, 34, "east", "strych");
            bp.crate(8, LOFT_Y + 1, 39, "north", "strych");
            bp.crate(13, LOFT_Y + 1, 30, "west", "strych");
            bp.set(6, LOFT_Y + 1, 37, "lantern[hanging=false]");
            // ktoś pilnował okna strychu i już nie zszedł
            f.skeleton(12, LOFT_Y + 1, 34, "south");
        }

        // ------------------------------------------------------------------ dom „kostka”

        // Dom jest opisany w układzie „projektowym” x 40-49, z 10-19 i przesuwany na miejsce (HX1, HZ1).
        private static final int DX1 = 40;
        private static final int DX2 = 49;
        private static final int DZ1 = 10;
        private static final int DZ2 = 19;
        private static final int OX = HX1 - DX1;
        private static final int OZ = HZ1 - DZ1;

        private void hs(int x, int y, int z, String state) {
            bp.set(x + OX, y, z + OZ, state);
        }

        private void hf(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
            bp.fill(x1 + OX, y1, z1 + OZ, x2 + OX, y2, z2 + OZ, state);
        }

        private void hf(int x1, int y1, int z1, int x2, int y2, int z2, Palette palette) {
            bp.fill(x1 + OX, y1, z1 + OZ, x2 + OX, y2, z2 + OZ, palette);
        }

        private void hc(int x, int y, int z, String facing, String pool) {
            bp.crate(x + OX, y, z + OZ, facing, pool);
        }

        private void hd(int x, int y, int z, String type, String facing, String hinge, boolean open) {
            door(x + OX, y, z + OZ, type, facing, hinge, open);
        }

        private void house() {
            for (int y = 1; y <= HOUSE_ROOF_Y - 1; y++) {
                for (int x = DX1; x <= DX2; x++) {
                    hs(x, y, DZ1, plaster.next());
                    hs(x, y, DZ2, plaster.next());
                }
                for (int z = DZ1; z <= DZ2; z++) {
                    hs(DX1, y, z, plaster.next());
                    hs(DX2, y, z, plaster.next());
                }
            }
            for (int x = DX1; x <= DX2; x++) {
                for (int z = DZ1; z <= DZ2; z++) {
                    boolean inner = x > DX1 && x < DX2 && z > DZ1 && z < DZ2;
                    if (inner) {
                        hs(x, 0, z, chance(0.85) ? "spruce_planks" : "dark_oak_planks");
                        hs(x, UPPER_Y, z, chance(0.85) ? "spruce_planks" : "dark_oak_planks");
                    }
                    hs(x, HOUSE_ROOF_Y, z, tarPaper.next());
                    if (!inner) {
                        hs(x, HOUSE_ROOF_Y + 1, z, chance(0.9) ? "gray_concrete" : "air");
                    }
                }
            }
            // okna parteru częściowo zabite deskami
            for (int z : new int[]{11, 12, 17, 18}) {
                groundWindow(DX1, z);
            }
            for (int x : new int[]{42, 43, 46, 47}) {
                groundWindow(x, DZ1);
                groundWindow(x, DZ2);
            }
            groundWindow(DX2, 16);
            groundWindow(DX2, 17);
            // okna piętra
            for (int[] p : new int[][]{{DX1, 12}, {DX1, 17}, {43, DZ2}, {46, DZ2}, {DX2, 14}, {DX2, 17}, {47, DZ1}}) {
                for (int y = 7; y <= 8; y++) {
                    hs(p[0], y, p[1], brokenGlass());
                }
            }
            // ganek z zardzewiałą balustradą i drzwi
            for (int x = 37; x <= 39; x++) {
                for (int z = 13; z <= 15; z++) {
                    hs(x, 0, z, "smooth_stone");
                    hs(x, 1, z, "smooth_stone_slab[type=bottom]");
                }
            }
            hs(37, 2, 13, "iron_bars");
            hs(37, 2, 15, "iron_bars");
            hs(38, 2, 13, "iron_bars");
            hs(39, 2, 13, "iron_bars");
            hs(38, 2, 15, "iron_bars");
            hd(DX1, 1, 14, "spruce_door", "west", "left", true);
            // komin
            hf(47, HOUSE_ROOF_Y + 1, 17, 47, HOUSE_ROOF_Y + 2, 17, "bricks");
            hs(44, HOUSE_ROOF_Y + 1, 12, "lightning_rod[facing=up]");
        }

        private void groundWindow(int x, int z) {
            for (int y = 2; y <= 3; y++) {
                double r = rnd.nextDouble();
                hs(x, y, z, r < 0.45 ? "spruce_planks" : r < 0.55 ? "glass_pane" : r < 0.62 ? "cobweb" : "air");
            }
        }

        private void houseGroundFloor() {
            // ściany działowe w boazerii, wąski korytarz z = 14
            for (int x = DX1 + 1; x < DX2; x++) {
                hf(x, 1, 13, x, 4, 13, x % 3 == 0 ? "stripped_spruce_wood[axis=y]" : "spruce_planks");
                hf(x, 1, 15, x, 4, 15, x % 3 == 0 ? "stripped_spruce_wood[axis=y]" : "spruce_planks");
            }
            hf(43, 1, 13, 43, 2, 13, "air");
            hf(42, 1, 15, 42, 2, 15, "air");
            // schody na piętro zasypane gruzem z zawalonego sufitu
            for (int i = 0; i < 4; i++) {
                int x = 45 + i;
                hs(x, 1 + i, 14, "spruce_stairs[facing=east,half=bottom]");
                for (int y = 1; y < 1 + i; y++) {
                    hs(x, y, 14, "spruce_planks");
                }
                for (int y = 2 + i; y <= UPPER_Y; y++) {
                    hs(x, y, 14, rubble.next());
                }
            }
            hs(44, 1, 14, "cobblestone_slab[type=bottom]");
            hs(44, 4, 14, "cobweb");
            // kuchnia: piec kaflowy, zlew, kuchenka z okapem, przewrócony stół
            hs(47, 1, 11, "furnace[facing=south,lit=false]");
            hs(48, 1, 11, "brown_terracotta");
            hf(47, 2, 11, 48, 3, 11, "white_terracotta");
            hf(47, 4, 11, 48, 4, 11, "brown_terracotta");
            f.sink(42 + OX, 1, 11 + OZ, "north");
            hs(43, 1, 11, "spruce_planks");
            hs(43, 2, 11, "flower_pot");
            hs(45, 1, 12, "spruce_trapdoor[facing=north,half=bottom,open=true]");
            f.stove(44 + OX, 1, 11 + OZ, "south");
            hc(41, 1, 11, "south", "parter");
            // okno zabite deskami i zastawione stołem warsztatowym z płotkiem; pod barykadą jej obrońca
            hf(46, 2, 10, 46, 3, 10, "spruce_planks");
            hs(46, 1, 11, "crafting_table");
            hs(46, 2, 11, "spruce_fence");
            f.skeleton(46 + OX, 1, 12 + OZ, "east");
            hc(48, 1, 12, "west", "lada");
            f.hangingLamp(44 + OX, 4, 12 + OZ);
            // salon: wersalka, meblościanka i rozbity telewizor
            f.bench(44 + OX, 1, 18 + OZ, 4, true, "south", "crimson");
            f.bookshelf(43 + OX, 1, 16 + OZ, 44 + OX, 16 + OZ, 2, "south");
            f.lamp(47 + OX, 1, 16 + OZ);
            f.hangingLamp(45 + OX, 4, 17 + OZ);
            hc(41, 1, 16, "east", "parter");
            hs(45, 1, 16, "spruce_planks");
            hs(45, 2, 16, "black_concrete");
            hs(45, 3, 16, "lightning_rod[facing=up]");
            hs(46, 1, 16, "cobweb");
            for (int x = 43; x <= 46; x++) {
                hs(x, 1, 17, chance(0.7) ? "brown_carpet" : "air");
            }
            hc(41, 1, 18, "east", "parter");
            hc(48, 1, 16, "west", "parter");
            hs(44, 4, 14, "lantern[hanging=true]");
        }

        private void houseUpperFloor() {
            int y = UPPER_Y + 1;
            for (int x = DX1 + 1; x < DX2; x++) {
                hf(x, y, 15, x, HOUSE_ROOF_Y - 1, 15, plaster);
            }
            hf(42, y, 15, 42, y + 1, 15, "air");
            // gruz wokół zawalonej klatki schodowej
            hs(46, y, 14, "cobblestone");
            hs(47, y, 14, "andesite_slab[type=bottom]");
            hs(48, y, 14, "cobblestone");
            hs(48, y + 1, 14, "cobblestone_slab[type=bottom]");
            // sypialnia z cenną skrzynią: łóżko, szafka nocna z lampką, biurko, szafa, chodnik
            hs(42, y, 12, "red_bed[facing=east,part=foot,occupied=false]");
            hs(43, y, 12, "red_bed[facing=east,part=head,occupied=false]");
            hs(44, y, 12, "spruce_slab[type=top]");
            f.lamp(44 + OX, y + 1, 12 + OZ);
            f.desk(46 + OX, y, 11 + OZ, 47 + OX, 11 + OZ, "spruce");
            f.chair(46 + OX, y, 12 + OZ, "south", "spruce");
            f.wardrobe(48 + OX, y, 11 + OZ, "south", "spruce");
            hs(41, y, 11, "chiseled_bookshelf[facing=south]");
            hf(43, y, 13, 45, y, 13, "light_gray_carpet");
            hc(48, y, 13, "west", "pietro");
            hc(41, y, 14, "east", "parter");
            f.hangingLamp(44 + OX, HOUSE_ROOF_Y - 1, 12 + OZ);
            // drugi pokój: łazienka z wanną, sedesem i umywalką
            hs(48, y, 18, "water_cauldron[level=2]");
            hs(47, y, 18, "water_cauldron[level=2]");
            f.toilet(48 + OX, y, 16 + OZ, "east");
            f.sink(45 + OX, y, 16 + OZ, "north");
            hc(41, y, 17, "east", "pietro");
            hc(43, y, 16, "south", "toalety");
            f.hangingLamp(45 + OX, HOUSE_ROOF_Y - 1, 17 + OZ);
            hs(44, HOUSE_ROOF_Y - 1, 13, "cobweb");
            // drzwi balkonowe z wybitą szybą
            hd(44, y, DZ1, "birch_door", "north", "left", true);
        }

        /** Długi, wąski balkon i szopa na węgiel przy zachodniej ścianie domu, po której dachu się na niego wskakuje. */
        private void balconyAndShed() {
            int bz1 = HZ1 - 2;
            for (int x = HX1; x <= HX2 - 1; x++) {
                for (int z = bz1; z < HZ1; z++) {
                    bp.set(x, UPPER_Y, z, "smooth_stone");
                }
            }
            // balustrada; od strony szopy (x = HX1) wyłamana
            for (int x = HX1 + 1; x <= HX2 - 1; x++) {
                if (x != HX1 + 5) {
                    bp.set(x, UPPER_Y + 1, bz1, "iron_bars");
                }
            }
            bp.set(HX2 - 1, UPPER_Y + 1, bz1 + 1, "iron_bars");

            // szopa x 34-37, z 18-24: dach wysoki (y4) przy domu, niski (y3) dalej
            int sx1 = HX1 - 4;
            int sx2 = HX1 - 1;
            int sz1 = HZ1 - 4;
            int sz2 = HZ1 + 2;
            for (int z = sz1; z <= sz2; z++) {
                bp.fill(sx2, 1, z, sx2, 3, z, plaster);
                bp.fill(sx1, 1, z, sx1, 2, z, "spruce_planks");
                for (int x = sx1; x <= sx2; x++) {
                    String roof = chance(0.7) ? "black_terracotta" : "spruce_planks";
                    bp.set(x, x >= sx2 - 1 ? 4 : 3, z, roof);
                }
            }
            for (int z : new int[]{sz1, sz2}) {
                bp.fill(sx1 + 1, 1, z, sx2 - 1, 2, z, "spruce_planks");
                bp.set(sx2 - 1, 3, z, "spruce_planks");
            }
            bp.fill(sx1, 1, sz1 + 3, sx1, 2, sz1 + 4, "air"); // wejście do szopy
            bp.fill(sx1 + 1, 1, sz1 + 1, sx1 + 2, 1, sz1 + 1, "coal_block");
            bp.set(sx1 + 2, 1, sz1 + 2, "coal_block");
            bp.set(sx1 + 2, 2, sz1 + 1, "coal_block");
            bp.crate(sx1 + 1, 1, sz2 - 1, "east", "plac");
            bp.crate(sx1 + 1, 1, sz1 + 2, "east", "zaplecze");
            // stos drewna i pieniek: ziemia → pieniek (y1) → stos (y2) → niski dach szopy (y3) → wysoki (y4) → balkon (y5)
            bp.fill(sx1 - 1, 1, sz1 + 1, sx1 - 1, 2, sz1 + 2, "oak_log[axis=z]");
            bp.set(sx1 - 2, 1, sz1 + 1, "oak_log[axis=y]");
            bp.set(sx1 - 2, 1, sz1 + 2, "oak_log[axis=z]");
        }

        // ------------------------------------------------------------------ paliwo i silosy

        /** Leżąca beka na ropę na betonowych podporach z ręczną pompą. */
        private void fuelTank() {
            for (int x = 4; x <= 13; x++) {
                for (int y = 2; y <= 4; y++) {
                    for (int z = 47; z <= 49; z++) {
                        bp.set(x, y, z, rust.next());
                    }
                }
                bp.set(x, 4, 47, "granite_stairs[facing=south,half=bottom]");
                bp.set(x, 4, 49, "granite_stairs[facing=north,half=bottom]");
                bp.set(x, 2, 47, "granite_stairs[facing=south,half=top]");
                bp.set(x, 2, 49, "granite_stairs[facing=north,half=top]");
            }
            for (int x : new int[]{5, 12}) {
                bp.fill(x, 1, 47, x, 1, 49, "polished_andesite");
            }
            bp.set(9, 5, 48, "lightning_rod[facing=up]");
            // ręczna pompa
            bp.set(15, 1, 48, "iron_block");
            bp.set(15, 2, 48, "lever[face=floor,facing=east,powered=false]");
            bp.set(14, 2, 48, "chain[axis=x]");
            bp.set(15, 0, 49, "black_concrete");
            bp.set(16, 0, 49, "black_concrete");
            bp.set(16, 0, 48, "black_concrete");
            // gęste skupisko beczek
            bp.crate(3, 1, 46, "east", "paliwo");
            bp.crate(8, 1, 45, "south", "paliwo");
            bp.crate(11, 1, 46, "south", "paliwo");
            bp.crate(6, 1, 51, "north", "paliwo");
            bp.crate(10, 1, 51, "north", "paliwo");
            bp.crate(14, 1, 51, "north", "paliwo");
            bp.crate(16, 1, 46, "west", "paliwo");
            bp.set(7, 1, 45, "blue_terracotta");
            bp.set(13, 1, 52, "orange_terracotta");
            bp.set(4, 1, 52, "blue_terracotta");
            tires(17, 52);
        }

        private static double dist(int x, int z, int cx) {
            return Math.sqrt((double) (x - cx) * (x - cx) + (double) (z - SILO_Z) * (z - SILO_Z));
        }

        /** Wysoki silos z blachy ze stożkowym dachem. Na zachodnim drabina w klatce i rzadka skrzynia na szczycie. */
        private void silo(int cx, boolean withLadder) {
            for (int x = cx - 5; x <= cx + 5; x++) {
                for (int z = SILO_Z - 5; z <= SILO_Z + 5; z++) {
                    double d = dist(x, z, cx);
                    if (d <= 4.5) {
                        bp.set(x, 0, z, chance(0.85) ? "smooth_stone" : "cracked_stone_bricks");
                    }
                    if (d > 2.5 && d <= 3.5) {
                        for (int y = 1; y <= SILO_TOP; y++) {
                            // blacha falista: pionowe pasy, co kilka bloków obręcz, rdzawe plamy
                            String sheet = (x + z) % 2 == 0 ? "light_gray_concrete" : "iron_block";
                            if (y % 6 == 0) {
                                sheet = "polished_andesite";
                            } else if (chance(0.08)) {
                                sheet = siloSheet.next();
                            }
                            bp.set(x, y, z, sheet);
                        }
                    }
                    // stożkowy dach
                    double[] radius = {3.5, 2.5, 1.5, 0.5};
                    for (int i = 0; i < radius.length; i++) {
                        if (d <= radius[i]) {
                            bp.set(x, SILO_TOP + 1 + i, z, chance(0.8) ? "iron_block" : chance(0.6) ? "light_gray_concrete" : "orange_terracotta");
                        }
                    }
                }
            }
            bp.set(cx, SILO_TOP + 5, SILO_Z, "lightning_rod[facing=up]");
            if (!withLadder) {
                // rdzawa dziura u podstawy, zasypana resztkami paszy
                bp.set(cx, 1, SILO_Z + 3, "hay_block[axis=y]");
                bp.set(cx, 2, SILO_Z + 3, "iron_bars");
                return;
            }
            // długa drabina w klatce na północnej ścianie
            int lz = SILO_Z - 4;
            for (int y = 1; y <= SILO_TOP + 1; y++) {
                bp.set(cx, y, lz, "ladder[facing=north]");
                if (y >= 3) {
                    bp.set(cx - 1, y, lz - 1, "iron_bars");
                    bp.set(cx, y, lz - 1, "iron_bars");
                    bp.set(cx + 1, y, lz - 1, "iron_bars");
                    bp.set(cx - 1, y, lz, "iron_bars");
                    bp.set(cx + 1, y, lz, "iron_bars");
                }
            }
            bp.crate(cx + 2, SILO_TOP + 3, SILO_Z, "west", "silos");
        }

        // ------------------------------------------------------------------ rekwizyty i opowieść

        /** Drewniany wychodek za domem: sedes, drzwi i kartka „ZAJĘTE”. */
        private void outhouse() {
            int x1 = 42;
            int x2 = 45;
            int z1 = 36;
            int z2 = 39;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean wall = x == x1 || x == x2 || z == z1 || z == z2;
                    if (wall) {
                        bp.fill(x, 1, z, x, 2, z, chance(0.85) ? "spruce_planks" : "stripped_spruce_wood[axis=y]");
                    }
                    bp.set(x, 3, z, chance(0.8) ? "spruce_slab[type=bottom]" : "dark_oak_slab[type=bottom]");
                }
            }
            door(43, 1, z1, "spruce_door", "north", "left", false);
            f.toilet(44, 1, 38, "south");
            bp.crate(43, 1, 38, "east", "toalety");
            bp.set(44, 2, 37, "air");
            bp.sign(44, 2, z1 - 1, "oak_wall_sign[facing=north]", "", "ZAJĘTE", "", "");
        }

        /** Obozowisko dzikiego lokatora pod strychem stodoły, z lampą i dobytkiem. */
        private void squatterCamp() {
            f.camp(11, 1, 33, "north");
            f.hangingLamp(11, LOFT_Y - 1, 34);
            bp.crate(10, 1, 31, "south", "zaplecze");
            bp.sign(8, 2, LOFT_Z + 1, "oak_wall_sign[facing=south]", "NIE RUSZAĆ", "MOICH", "RZECZY!", "- Zbychu");
        }

        /**
         * Ślady dawnego życia: napisy ocalałych na ścianach, zaschnięta krew, porozrzucane kartki
         * i zawalony namiot. Wszystko leży płasko albo na ścianach, więc nie zamyka przejść ani osłon.
         */
        private void lifeSigns() {
            f.traces(HX1 + 1, HZ1 + 1, HX2 - 1, HZ2 - 1, 1, 2, 5);
            f.traces(SX1 + 1, SZ1 + 1, SX2 - 1, LOFT_Z - 1, 1, 3, 2);
            f.graffiti(SX1 + 1, SZ1 + 1, SX2 - 1, SZ2 - 1, 2, "TU SPALIŚMY", "CAŁĄ ZIMĘ", "DZIĘKI, PGR", "");
            f.graffiti(HX1 + 1, HZ1 + 1, HX2 - 1, HZ2 - 1, 2, "ZABRALI", "WSZYSTKO", "NAWET", "FIRANKI");
            f.graffiti(15, 6, 37, 42, 2, "SZUKAM", "BRATA", "PIOTREK K.", "WRÓĆ!");
            f.collapsedTent(0, 42, WIDTH - 1, DEPTH - 3, 1, "brown");
            // bezpańskie zwierzęta: kury na podwórzu, koty w stodole i przy domu
            f.animalSomewhere(16, 8, 36, 40, 1, "chicken");
            f.animalSomewhere(16, 8, 36, 40, 1, "chicken");
            f.animalSomewhere(16, 8, 36, 40, 1, "chicken");
            f.animalSomewhere(SX1 + 1, SZ1 + 1, SX2 - 1, LOFT_Z - 1, 1, "cat");
            f.animalSomewhere(HX1 - 4, HZ1 - 2, HX2 + 2, HZ2 + 3, 1, "cat");
        }

        /** Napisy ocalałych na ścianach. */
        private void graffiti() {
            bp.sign(SX2 + 1, 3, 12, "oak_wall_sign[facing=east]", "STRYCH", "ZAJĘTY", "SPADAJ", "");
            bp.sign(DX1 - 1 + OX, 3, 16 + OZ, "oak_wall_sign[facing=west]", "UWAGA!", "SNAJPER", "NA SILOSIE", "");
        }

        /** Dodatkowe beczki i skrzynki na podwórzu: przy rozrzutniku, bramie, studni, silosie i na przyczepie. */
        private void extraYardCrates() {
            bp.crate(45, 1, 13, "west", "plac");
            bp.crate(34, 1, 9, "south", "plac");
            bp.crate(25, 1, 35, "east", "plac");
            bp.crate(37, 1, 46, "west", "plac");
            bp.crate(20, 2, 22, "east", "plac");
        }

        // ------------------------------------------------------------------ zarośla i zniszczenia

        /** Wysoka trawa i pokrzywy (paprocie) – gęsto przy ścianach, płocie i za stodołą. */
        private void weeds() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 4; z < DEPTH; z++) {
                    String below = bp.get(x, 0, z);
                    if (!below.equals("minecraft:grass_block") && !below.equals("minecraft:podzol")
                            && !below.equals("minecraft:coarse_dirt")) {
                        continue;
                    }
                    if (!bp.isAir(x, 1, z) || !bp.isAir(x, 2, z)) {
                        continue;
                    }
                    boolean dense = z >= 42 || nearSolid(x, z);
                    if (!chance(dense ? 0.55 : 0.18)) {
                        continue;
                    }
                    double r = rnd.nextDouble();
                    if (r < 0.3) {
                        bp.set(x, 1, z, "tall_grass[half=lower]");
                        bp.set(x, 2, z, "tall_grass[half=upper]");
                    } else if (r < 0.45 && dense) {
                        bp.set(x, 1, z, "large_fern[half=lower]");
                        bp.set(x, 2, z, "large_fern[half=upper]");
                    } else if (r < 0.75) {
                        bp.set(x, 1, z, "short_grass");
                    } else {
                        bp.set(x, 1, z, "fern");
                    }
                }
            }
        }

        private boolean nearSolid(int x, int z) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (bp.inBounds(x + dx, 1, z + dz) && !bp.isAir(x + dx, 1, z + dz)
                            && !bp.get(x + dx, 1, z + dz).contains("grass") && !bp.get(x + dx, 1, z + dz).contains("fern")) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void weathering() {
            // pajęczyny w stodole (pod strychem i pod dachem) i w domu
            for (int x = SX1 + 1; x < SX2; x++) {
                for (int z = SZ1 + 1; z < SZ2; z++) {
                    if (z >= LOFT_Z && bp.isAir(x, LOFT_Y - 1, z) && chance(0.06)) {
                        bp.set(x, LOFT_Y - 1, z, "cobweb");
                    }
                }
            }
            for (int x = HX1 + 1; x < HX2; x++) {
                for (int z = HZ1 + 1; z < HZ2; z++) {
                    if (bp.isAir(x, 4, z) && chance(0.06)) {
                        bp.set(x, 4, z, "cobweb");
                    }
                    if (bp.isAir(x, HOUSE_ROOF_Y - 1, z) && chance(0.06)) {
                        bp.set(x, HOUSE_ROOF_Y - 1, z, "cobweb");
                    }
                }
            }
            // odpadający tynk i mech
            for (int i = 0; i < 20; i++) {
                int x = HX1 + rnd.nextInt(HX2 - HX1 + 1);
                int y = 1 + rnd.nextInt(8);
                if (!bp.isAir(x, y, HZ2)) {
                    bp.set(x, y, HZ2, chance(0.5) ? "bricks" : "light_gray_concrete");
                }
            }
            for (int i = 0; i < 16; i++) {
                int z = SZ1 + rnd.nextInt(SZ2 - SZ1 + 1);
                int y = 1 + rnd.nextInt(3);
                if (!bp.isAir(SX1, y, z)) {
                    bp.set(SX1, y, z, chance(0.5) ? "mossy_stone_bricks" : "mossy_cobblestone");
                }
            }
            // pnącza na dziedzińcowej ścianie domu i stodoły
            for (int z = HZ1 + 1; z < HZ2; z += 3) {
                for (int y = 3; y <= 8; y++) {
                    if (chance(0.6) && bp.isAir(HX1 - 1, y, z)) {
                        bp.set(HX1 - 1, y, z, "vine[east=true,north=false,south=false,up=false,west=false]");
                    }
                }
            }
            for (int z = SZ1 + 2; z < SZ2; z += 7) {
                for (int y = 2; y <= 6; y++) {
                    if (chance(0.6) && bp.isAir(SX1 - 1, y, z)) {
                        bp.set(SX1 - 1, y, z, "vine[east=true,north=false,south=false,up=false,west=false]");
                    }
                }
            }
        }

        // ------------------------------------------------------------------ pomocnicze

        private String brokenGlass() {
            double roll = rnd.nextDouble();
            if (roll < 0.4) {
                return "air";
            }
            if (roll < 0.47) {
                return "cobweb";
            }
            return chance(0.2) ? "light_gray_stained_glass_pane" : "glass_pane";
        }

        private void door(int x, int y, int z, String type, String facing, String hinge, boolean open) {
            String props = "facing=" + facing + ",hinge=" + hinge + ",open=" + open + ",powered=false";
            bp.set(x, y, z, type + "[half=lower," + props + "]");
            bp.set(x, y + 1, z, type + "[half=upper," + props + "]");
        }
    }
}
