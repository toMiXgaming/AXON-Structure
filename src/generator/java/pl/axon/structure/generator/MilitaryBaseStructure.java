package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.Random;

/**
 * Opuszczona Jednostka Wojskowa „JW 2137” – duży garnizon z trzema strefami i dwupoziomowym schronem.
 * <p>
 * Struktura ma {@code grunt: 21}. W kodzie wysokości liczone są od gruntu (y = 0 to powierzchnia,
 * y &lt; 0 to podziemie: poziom 1 schronu ma stopy na y -9, poziom 2 na y -19).
 * <pre>
 *  z 0-3     droga, z 5 płot z prefabrykatów, brama x 70-79, droga główna x 72-77
 *  z 6-50    strefa koszarowa: wartownia, plac apelowy z trybuną i T-34, sztab, dwa bloki koszar
 *            z łącznikiem, stołówka, izba chorych
 *  z 52      siatka z bramą
 *  z 54-88   strefa techniczna: garaże ze strychem, wiata, dźwig, warsztaty z kanałami, stacja paliw,
 *            kotłownia z kominem, dwa magazyny
 *  z 90      siatka z bramą
 *  z 92-125  strefa bojowa: magazyn uzbrojenia (trzy bunkry za drutem), lądowisko z wrakiem Mi-8,
 *            hangar, wieża kontroli lotów, strzelnica, tor przeszkód, las z wylotem szybu awaryjnego
 *  rogi      cztery wieże wartownicze
 *  pod ziemią: schron pod strefą bojową (x 39-90 / z 98-123), wejścia z grzybka, z hangaru,
 *            wyjście awaryjne w lesie
 * </pre>
 */
public final class MilitaryBaseStructure implements StructureDefinition {

    private static final int WIDTH = 150;
    private static final int DEPTH = 130;
    /** Ile warstw leży pod ziemią. */
    private static final int G = 21;
    private static final int HEIGHT = G + 34;

    /** Poziom stóp na poziomach schronu. */
    private static final int L1 = -9;
    private static final int L2 = -19;

    @Override
    public String id() {
        return "jednostka";
    }

    @Override
    public String displayName() {
        return "Jednostka wojskowa JW 2137";
    }

    @Override
    public String description() {
        return "Opuszczony garnizon: koszary, sztab, park maszyn, kotłownia, magazyn uzbrojenia, lądowisko i dwupoziomowy schron";
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
        private final Palette apron;
        private final Palette olive;
        private final Palette plaster;
        private final Palette hospital;
        private final Palette brick;
        private final Palette sheet;
        private final Palette army;
        private final Palette rubble;
        private final Palette rock;
        private final Furniture f;

        Builder(Random rnd) {
            this.rnd = rnd;
            f = new Furniture(bp, G, rnd);
            grass = new Palette(rnd)
                    .add("grass_block", 12).add("coarse_dirt", 3).add("dirt", 2).add("podzol", 1);
            asphalt = new Palette(rnd)
                    .add("gray_concrete", 7).add("andesite", 2).add("cobblestone", 1).add("cracked_stone_bricks", 1);
            apron = new Palette(rnd)
                    .add("smooth_stone", 7).add("light_gray_concrete", 3).add("cracked_stone_bricks", 1);
            olive = new Palette(rnd)
                    .add("green_terracotta", 9).add("mossy_stone_bricks", 1).add("terracotta", 1);
            plaster = new Palette(rnd)
                    .add("light_gray_concrete", 7).add("white_terracotta", 2).add("cracked_stone_bricks", 1);
            hospital = new Palette(rnd)
                    .add("white_concrete", 6).add("white_terracotta", 2).add("light_gray_concrete", 1);
            brick = new Palette(rnd)
                    .add("bricks", 14).add("terracotta", 2).add("mud_bricks", 1);
            sheet = new Palette(rnd)
                    .add("light_gray_concrete", 5).add("iron_block", 3).add("orange_terracotta", 1).add("andesite", 1);
            army = new Palette(rnd)
                    .add("green_concrete", 5).add("green_terracotta", 2).add("moss_block", 1);
            rubble = new Palette(rnd)
                    .add("cobblestone", 4).add("tuff", 2).add("cracked_stone_bricks", 2).add("andesite", 1);
            rock = new Palette(rnd)
                    .add("stone", 40).add("andesite", 1).add("tuff", 1);
        }

        Blueprint build() {
            bp.ground(G);
            ground();
            perimeter();
            innerFences();
            gateArea();
            guardhouse();
            paradeGround();
            headquarters();
            barracks(8, false);
            barracks(30, true);
            barracksLink();
            canteen();
            infirmary();
            garages();
            parking();
            crane();
            workshops();
            fuelStation();
            boilerHouse();
            foodStore();
            equipmentStore();
            armory();
            helipad();
            hangar();
            flightTower();
            shootingRange();
            obstacleCourse();
            watchtower(2, 13, 16, "east", true, 6, 9);
            watchtower(143, 7, 8, null, true, 0, -1);
            watchtower(3, 121, 8, null, false, 0, -1);
            watchtower(143, 121, 8, "west", false, 3, 6);
            berm();
            forest();
            bunker();
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

        private boolean isCrate(int x, int y, int z) {
            Blueprint.Pos pos = new Blueprint.Pos(x, y + G, z);
            return bp.spawnPoints().stream().anyMatch(p -> p.pos().equals(pos));
        }

        /** Piętrowa prycza: dwa prawdziwe łóżka jedno nad drugim, nogi w (x, z), wezgłowie w stronę {@code facing}. */
        private void bunk(int x, int y, int z, String facing, String color) {
            f.bed(x, y, z, facing, color);
            f.bed(x, y + 1, z, facing, color);
        }

        private String armyBed() {
            return new String[]{"green", "green", "brown", "gray"}[rnd.nextInt(4)];
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

        private static String stairs(String material, String facing) {
            return material + "_stairs[facing=" + facing + ",half=bottom,shape=straight,waterlogged=false]";
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

        private void slab(int x1, int z1, int x2, int z2, int y, Palette palette) {
            fill(x1, y, z1, x2, y, z2, palette);
        }

        /** Okna co {@code step} bloków na ścianie z = const. */
        private void windowsX(int z, int x1, int x2, int step, int... ys) {
            for (int x = x1; x <= x2; x += step) {
                for (int y : ys) {
                    set(x, y, z, brokenGlass());
                }
            }
        }

        private void windowsZ(int x, int z1, int z2, int step, int... ys) {
            for (int z = z1; z <= z2; z += step) {
                for (int y : ys) {
                    set(x, y, z, brokenGlass());
                }
            }
        }

        private void cobwebs(int x1, int z1, int x2, int z2, int y, int count) {
            for (int i = 0; i < count; i++) {
                int x = x1 + rnd.nextInt(x2 - x1 + 1);
                int z = z1 + rnd.nextInt(z2 - z1 + 1);
                if (isAir(x, y, z)) {
                    set(x, y, z, "cobweb");
                }
            }
        }

        /**
         * Klatka schodowa 6 × 4 (x0..x0+5, z0..z0+3): biegi po 4 stopnie na kondygnację 5 bloków,
         * na zmianę na zachód (rząd z0+1, podest x0) i na wschód (rząd z0+3, podest x0+5).
         */
        private void stairCore(int x0, int z0, int base, int flights, String material, String support) {
            for (int k = 0; k < flights; k++) {
                int h = base + 5 * k;
                boolean west = k % 2 == 0;
                int row = west ? z0 + 1 : z0 + 3;
                for (int i = 0; i < 4; i++) {
                    int x = west ? x0 + 4 - i : x0 + 1 + i;
                    for (int y = h + 1; y < h + 1 + i; y++) {
                        set(x, y, row, support);
                    }
                    set(x, h + 1 + i, row, stairs(material, west ? "west" : "east"));
                }
                fill(x0 + 1, h + 5, row, x0 + 4, h + 5, row, "air");
            }
        }

        // ------------------------------------------------------------------ teren, płoty, drogi

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    fill(x, -G, z, x, -4, z, rock);
                    fill(x, -3, z, x, -1, z, "dirt");
                    set(x, 0, z, z <= 3 ? asphalt.next() : grass.next());
                }
            }
            // droga główna od bramy do hangaru
            for (int x = 72; x <= 77; x++) {
                for (int z = 4; z <= 93; z++) {
                    set(x, 0, z, chance(0.06) ? "mud" : asphalt.next());
                }
            }
            // droga patrolowa wzdłuż płotu
            for (int x = 2; x <= WIDTH - 3; x++) {
                for (int z : new int[]{6, 7, DEPTH - 5, DEPTH - 4}) {
                    set(x, 0, z, chance(0.6) ? "gravel" : "coarse_dirt");
                }
            }
            for (int z = 6; z <= DEPTH - 4; z++) {
                for (int x : new int[]{2, 3, WIDTH - 4, WIDTH - 3}) {
                    set(x, 0, z, chance(0.6) ? "gravel" : "coarse_dirt");
                }
            }
        }

        /** Płot z betonowych płyt (3 bloki) ze słupkami i drutem kolczastym, w kilku miejscach wyłamany. */
        private void perimeter() {
            for (int x = 1; x <= WIDTH - 2; x++) {
                if (x < 70 || x > 79) {
                    panel(x, 5, x);
                }
                panel(x, DEPTH - 3, x);
            }
            for (int z = 6; z <= DEPTH - 4; z++) {
                panel(1, z, z);
                panel(WIDTH - 2, z, z);
            }
            for (int[] gap : new int[][]{{30, 5}, {118, DEPTH - 3}}) {
                fill(gap[0], 1, gap[1], gap[0] + 1, 4, gap[1], "air");
                set(gap[0], 1, gap[1] + 1, rubble.next());
            }
            fill(WIDTH - 2, 1, 70, WIDTH - 2, 4, 71, "air");
        }

        private void panel(int x, int z, int pos) {
            if (pos % 4 == 0) {
                fill(x, 1, z, x, 4, z, "polished_andesite");
            } else {
                fill(x, 1, z, x, 3, z, chance(0.85) ? "smooth_stone" : "light_gray_concrete");
                if (chance(0.5)) {
                    set(x, 4, z, "cobweb");
                }
            }
        }

        /** Siatki między strefami z bramami na drodze głównej. */
        private void innerFences() {
            for (int z : new int[]{52, 90}) {
                for (int x = 4; x <= WIDTH - 5; x++) {
                    if (x >= 71 && x <= 78) {
                        continue;
                    }
                    if (x % 5 == 0) {
                        fill(x, 1, z, x, 3, z, "polished_andesite");
                    } else {
                        set(x, 1, z, "iron_bars");
                        set(x, 2, z, "iron_bars");
                        if (chance(0.3)) {
                            set(x, 3, z, "cobweb");
                        }
                    }
                }
                // otwarte skrzydła bramy
                fill(70, 1, z + 1, 70, 2, z + 4, "iron_bars");
                fill(79, 1, z + 1, 79, 2, z + 4, "iron_bars");
            }
            fill(22, 1, 52, 23, 3, 52, "air");
            fill(118, 1, 52, 119, 3, 52, "air");
            fill(43, 1, 90, 44, 3, 90, "air");
        }

        // ------------------------------------------------------------------ strefa koszarowa

        private void gateArea() {
            fill(69, 1, 5, 69, 5, 5, "light_gray_concrete");
            fill(80, 1, 5, 80, 5, 5, "light_gray_concrete");
            sign(69, 3, 4, "spruce_wall_sign[facing=north]", "JEDNOSTKA", "WOJSKOWA", "2137", "");
            sign(80, 3, 4, "spruce_wall_sign[facing=north]", "TEREN", "WOJSKOWY", "WSTĘP", "WZBRONIONY");
            // jedno skrzydło otwarte, drugie wyrwane i leży na drodze
            fill(68, 1, 6, 68, 3, 9, "green_concrete");
            set(68, 2, 7, "red_concrete");
            set(68, 2, 8, "red_concrete");
            for (int x = 74; x <= 78; x++) {
                for (int z = 1; z <= 3; z++) {
                    set(x, 1, z, "green_carpet");
                }
            }
            set(76, 1, 2, "red_carpet");
            // zapory przeciwczołgowe w zygzak
            fill(70, 1, 9, 74, 2, 9, "smooth_stone");
            fill(75, 1, 12, 79, 2, 12, "smooth_stone");
            fill(70, 1, 15, 74, 2, 15, "smooth_stone");
            // podniesiony szlaban
            for (int y = 1; y <= 6; y++) {
                set(71, y, 18, y % 2 == 0 ? "red_concrete" : "white_concrete");
            }
            crate(70, 1, 17, "east", "plac");
            crate(79, 1, 19, "west", "plac");
            crate(81, 1, 6, "south", "plac");
            crate(79, 1, 10, "west", "plac");
        }

        private void guardhouse() {
            int x1 = 82;
            int x2 = 98;
            int z1 = 7;
            int z2 = 18;
            walls(x1, z1, x2, z2, 1, 4, olive);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 5, z, "smooth_stone");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, "smooth_stone");
                    }
                }
            }
            // korytarz z 13, pokoje z 8-11 i z 15-17
            fill(x1 + 1, 1, 12, x2 - 1, 4, 12, olive);
            fill(x1 + 1, 1, 14, x2 - 1, 4, 14, olive);
            fill(88, 1, 8, 88, 4, 11, olive);
            fill(91, 1, 15, 91, 4, 17, olive);
            fill(95, 1, 15, 95, 4, 17, olive);
            fill(x1, 1, 13, x1, 2, 13, "air");
            door(x1, 1, 13, "spruce_door", "west", "left", true);
            fill(85, 1, 12, 85, 2, 12, "air");
            fill(93, 1, 12, 93, 2, 12, "air");
            fill(86, 1, 14, 86, 2, 14, "air");
            // cele: kraty od korytarza, drzwi zamknięte, do jednej dziura ze ściany pokoju warty
            fill(92, 1, 14, 97, 3, 14, "iron_bars");
            door(93, 1, 14, "iron_door", "south", "left", false);
            door(96, 1, 14, "iron_door", "south", "left", false);
            fill(91, 1, 16, 91, 2, 16, "air");
            set(91, 1, 15, rubble.next());
            // okienko na bramę i okna
            set(x1, 2, 9, "glass_pane");
            windowsX(z1, 84, 96, 3, 2, 3);
            windowsX(z2, 84, 89, 3, 2);
            set(98, 2, 16, "iron_bars");
            set(98, 2, 9, brokenGlass());
            // mapa na ścianie w pokoju odpraw
            for (int z = 8; z <= 11; z++) {
                for (int y = 2; y <= 3; y++) {
                    set(x2, y, z, chance(0.2) ? "lime_terracotta" : chance(0.15) ? "red_terracotta" : "white_terracotta");
                }
            }
            // dyżurka
            fill(84, 1, 8, 86, 1, 8, "dark_oak_slab[type=top]");
            set(84, 2, 8, "lever[face=floor,facing=south,powered=false]");
            set(87, 3, 8, "lantern[hanging=false,waterlogged=false]");
            f.chair(85, 1, 9, "south", "spruce");
            f.lamp(86, 2, 8);
            crate(83, 1, 9, "east", "parter");
            crate(87, 1, 10, "west", "parter");
            // pokój odpraw: stół i krzesła
            fill(91, 1, 9, 95, 1, 10, "dark_oak_slab[type=top]");
            for (int x = 91; x <= 95; x += 2) {
                set(x, 1, 8, stairs("spruce", "north"));
                set(x, 1, 11, stairs("spruce", "south"));
            }
            f.lamp(93, 2, 10);
            crate(97, 1, 11, "west", "parter");
            // pokój warty: piętrowe prycze i stolik
            bunk(87, 1, 17, "east", "green");
            bunk(89, 1, 17, "east", "green");
            f.table(83, 1, 15, "spruce");
            f.chair(84, 1, 15, "east", "spruce");
            crate(84, 1, 17, "east", "parter");
            // cela A: prycza; cela B (zamknięta): kibel i ktoś, kogo zapomnieli wypuścić
            f.bed(93, 1, 17, "west", "white");
            crate(94, 1, 17, "north", "parter");
            f.toilet(96, 1, 17, "south");
            f.skeleton(97, 1, 16, "south");
            sign(90, 3, 13, "spruce_wall_sign[facing=south]", "KLUCZE OD CEL", "WYRZUCIŁEM", "DO STUDNI", "– KAPRAL");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 4, 10);
        }

        private void paradeGround() {
            int x1 = 46;
            int x2 = 68;
            int z1 = 18;
            int z2 = 46;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 0, z, apron.next());
                }
            }
            for (int z = 22; z <= 40; z += 6) {
                for (int x = 50; x <= 64; x++) {
                    if (chance(0.85)) {
                        set(x, 0, z, "white_concrete");
                    }
                }
            }
            // trybuna honorowa ze stopniami i daszkiem
            for (int z = 24; z <= 40; z++) {
                set(45, 1, z, "spruce_planks");
                fill(44, 1, z, 44, 1, z, "smooth_stone");
                set(44, 2, z, "spruce_planks");
                fill(43, 1, z, 43, 2, z, "smooth_stone");
                set(43, 3, z, "spruce_planks");
                fill(42, 1, z, 42, 3, z, "smooth_stone");
                set(42, 4, z, "spruce_planks");
                fill(41, 1, z, 41, 4, z, "smooth_stone");
            }
            for (int z = 24; z <= 40; z += 8) {
                fill(41, 5, z, 41, 7, z, "polished_andesite");
            }
            for (int x = 41; x <= 45; x++) {
                for (int z = 23; z <= 41; z++) {
                    set(x, 8, z, "smooth_stone_slab[type=bottom]");
                }
            }
            fill(42, 6, 31, 42, 7, 33, "red_concrete");
            set(42, 6, 32, "white_concrete");
            // maszt flagowy
            fill(56, 1, 23, 58, 1, 25, "stone_brick_slab[type=bottom]");
            fill(57, 2, 24, 57, 15, 24, "iron_bars");
            set(57, 16, 24, "lightning_rod[facing=up]");
            // pomnik T-34 na cokole (z trybuny da się wskoczyć na cokół)
            fill(46, 1, 38, 52, 2, 44, "polished_andesite");
            for (int x = 47; x <= 51; x++) {
                for (int z = 39; z <= 43; z++) {
                    set(x, 3, z, z == 39 || z == 43 ? "gray_concrete" : army.next());
                }
            }
            for (int x = 48; x <= 50; x++) {
                for (int z = 40; z <= 42; z++) {
                    set(x, 4, z, army.next());
                }
            }
            crate(49, 4, 41, "east", "parter");
            for (int x = 51; x <= 55; x++) {
                set(x, 4, 41, "iron_bars");
            }
            sign(49, 2, 45, "oak_wall_sign[facing=south]", "CHWAŁA", "BOHATEROM", "", "");
            // tablice z hasłami
            for (int[] board : new int[][]{{50, 18}, {60, 18}}) {
                set(board[0], 1, board[1], "spruce_fence");
                set(board[0] + 4, 1, board[1], "spruce_fence");
                fill(board[0], 2, board[1], board[0] + 4, 3, board[1], "white_terracotta");
            }
            sign(52, 3, 19, "oak_wall_sign[facing=south]", "SŁUŻYMY", "OJCZYŹNIE", "", "");
            sign(62, 3, 19, "oak_wall_sign[facing=south]", "ŻOŁNIERZ", "–", "OBYWATEL", "");
            crate(47, 1, 25, "east", "plac");
            crate(58, 1, 26, "north", "plac");
            crate(66, 1, 20, "west", "plac");
            crate(64, 1, 44, "north", "plac");
        }

        private void headquarters() {
            int x1 = 84;
            int x2 = 104;
            int z1 = 24;
            int z2 = 38;
            walls(x1, z1, x2, z2, 1, 9, plaster);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean inside = x > x1 && x < x2 && z > z1 && z < z2;
                    set(x, 10, z, "smooth_stone");
                    if (inside) {
                        set(x, 0, z, chance(0.85) ? "polished_andesite" : "cracked_stone_bricks");
                        set(x, 5, z, chance(0.9) ? "spruce_planks" : "dark_oak_planks");
                    } else {
                        set(x, 5, z, "stone_bricks");
                        set(x, 11, z, "stone_brick_slab[type=bottom]");
                    }
                }
            }
            // portyk z kolumnami i godło
            for (int z = 27; z <= 35; z += 2) {
                fill(82, 1, z, 82, 7, z, "quartz_pillar[axis=y]");
            }
            for (int x = 81; x <= 83; x++) {
                for (int z = 26; z <= 36; z++) {
                    set(x, 8, z, "smooth_quartz");
                }
            }
            fill(81, 0, 27, 83, 0, 35, "polished_andesite");
            fill(x1, 1, 30, x1, 3, 32, "air");
            fill(x1, 7, 30, x1, 9, 32, "red_concrete");
            set(x1, 8, 31, "white_concrete");
            windowsZ(x1, 26, 36, 3, 2, 3, 7, 8);
            windowsX(z1, 87, 102, 3, 2, 3, 7, 8);
            windowsZ(x2, 26, 31, 3, 2, 3, 7, 8);
            // parter: hol, sala odpraw, kancelaria tajna za kratą
            fill(90, 1, z1 + 1, 90, 4, z2 - 1, plaster);
            fill(90, 1, 28, 90, 2, 28, "air");
            fill(90, 1, 33, 90, 3, 37, "iron_bars");
            fill(91, 1, 32, x2 - 1, 4, 32, plaster);
            for (int x = 87; x <= 101; x += 3) {
                set(x, 2, z2, "iron_bars");
                set(x, 3, z2, "iron_bars");
            }
            windowsZ(x2, 34, 36, 2, 2);
            for (int x = 95; x <= 101; x += 2) {
                for (int z = 26; z <= 30; z += 2) {
                    set(x, 1, z, stairs("spruce", "east"));
                }
            }
            for (int z = 26; z <= 30; z++) {
                for (int y = 2; y <= 3; y++) {
                    set(x2, y, z, chance(0.25) ? "lime_terracotta" : chance(0.15) ? "red_terracotta" : "white_terracotta");
                }
            }
            set(87, 1, 26, "spruce_planks");
            set(87, 2, 26, "glass");
            set(88, 1, 26, "spruce_planks");
            set(88, 2, 26, "glass");
            set(103, 1, 28, "lectern[facing=west,has_book=false,powered=false]");
            crate(91, 1, 25, "east", "parter");
            crate(89, 1, 37, "north", "parter");
            // schody na piętro w holu
            for (int i = 0; i < 4; i++) {
                for (int x = 85; x <= 86; x++) {
                    for (int y = 1; y < 1 + i; y++) {
                        set(x, y, 35 - i, "stone_bricks");
                    }
                    set(x, 1 + i, 35 - i, stairs("stone_brick", "north"));
                }
            }
            fill(85, 5, 32, 86, 5, 35, "air");
            // piętro: sekretariat, gabinet dowódcy, archiwum
            fill(90, 6, z1 + 1, 90, 9, z2 - 1, plaster);
            fill(90, 6, 27, 90, 7, 27, "air");
            fill(90, 6, 35, 90, 7, 35, "air");
            fill(96, 6, z1 + 1, 96, 9, 31, plaster);
            fill(96, 6, 28, 96, 7, 28, "air");
            fill(91, 6, 32, x2 - 1, 9, 32, plaster);
            fill(99, 6, 26, 102, 6, 27, "dark_oak_slab[type=top]");
            set(100, 6, 29, stairs("dark_oak", "north"));
            f.lamp(99, 7, 26);
            set(102, 7, 26, "potted_fern");
            f.bookshelf(97, 6, 30, 97, 31, 2, "east");
            for (int x = 99; x <= 101; x++) {
                for (int z = 30; z <= 31; z++) {
                    set(x, 6, z, "red_carpet");
                }
            }
            crate(102, 6, 30, "west", "sztab");
            // sekretariat: biurko z krzesłem i regał z segregatorami
            fill(92, 6, 29, 94, 6, 29, "spruce_slab[type=top]");
            f.chair(93, 6, 30, "south", "spruce");
            f.lamp(94, 7, 29);
            f.bookshelf(91, 6, 31, 92, 31, 2, "north");
            crate(92, 6, 26, "east", "parter");
            // archiwum
            for (int x = 99; x <= 103; x++) {
                set(x, 6, 37, "bookshelf");
                set(x, 7, 37, "bookshelf");
            }
            f.bookshelf(98, 6, 33, 101, 33, 2, "south");
            crate(102, 6, 34, "west", "parter");
            crate(91, 6, 37, "east", "parter");
            // zawalony fragment stropu archiwum nad kancelarią i gruz pod nim
            fill(94, 5, 35, 97, 5, 36, "air");
            fill(98, 1, 36, 98, 1, 36, rubble);
            fill(97, 1, 36, 97, 2, 36, rubble);
            fill(96, 1, 36, 96, 3, 36, rubble);
            fill(95, 1, 36, 95, 4, 36, rubble);
            fill(94, 1, 36, 94, 4, 36, rubble);
            crate(101, 1, 34, "west", "sztab");
            set(103, 1, 37, "iron_block");
            set(103, 2, 37, "iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 9, 12);
        }

        /** Blok koszar x 8-38 / z zs..zs+14, trzy kondygnacje (y 0, 5, 10), dach y 15. */
        private void barracks(int zs, boolean south) {
            int x1 = 8;
            int x2 = 38;
            int z1 = zs;
            int z2 = zs + 14;
            walls(x1, z1, x2, z2, 1, 14, olive);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean inside = x > x1 && x < x2 && z > z1 && z < z2;
                    set(x, 15, z, "smooth_stone");
                    if (inside) {
                        set(x, 0, z, "smooth_stone");
                        set(x, 5, z, "polished_andesite");
                        set(x, 10, z, "polished_andesite");
                    } else {
                        set(x, 16, z, "stone_brick_slab[type=bottom]");
                        set(x, 5, z, "stone_bricks");
                        set(x, 10, z, "stone_bricks");
                    }
                }
            }
            int wallN = zs + 5;
            int wallS = zs + 9;
            for (int h : new int[]{0, 5, 10}) {
                fill(x1 + 1, h + 1, wallN, x2 - 1, h + 4, wallN, olive);
                fill(x1 + 1, h + 1, wallS, x2 - 1, h + 4, wallS, olive);
                for (int px : new int[]{15, 22, 29}) {
                    fill(px, h + 1, z1 + 1, px, h + 4, wallN - 1, olive);
                    fill(px, h + 1, wallS + 1, px, h + 4, z2 - 1, olive);
                }
                for (int dx : new int[]{18, 25, 33}) {
                    fill(dx, h + 1, wallN, dx, h + 2, wallN, "air");
                    fill(dx, h + 1, wallS, dx, h + 2, wallS, "air");
                }
                // wejścia na końcach korytarza
                fill(x1, h + 1, zs + 7, x1, h + 2, zs + 7, h == 0 ? "air" : "glass_pane");
                fill(x2, h + 1, zs + 7, x2, h + 2, zs + 7, h == 0 ? "air" : "glass_pane");
                windowsX(z1, 10, 36, 3, h + 2, h + 3);
                windowsX(z2, 10, 36, 3, h + 2, h + 3);
            }
            // klatka schodowa x 9-14 w pasie północnych pokoi
            stairCore(9, z1 + 1, 0, south ? 2 : 3, "stone_brick", "stone_bricks");
            fill(13, 1, wallN, 13, 2, wallN, "air");
            fill(9, 6, wallN, 9, 7, wallN, "air");
            fill(14, 11, wallN, 14, 12, wallN, "air");
            if (!south) {
                // nadbudówka wyjścia na dach
                walls(8, z1, 15, wallN, 16, 18, olive);
                for (int x = 8; x <= 15; x++) {
                    for (int z = z1; z <= wallN; z++) {
                        set(x, 19, z, "smooth_stone");
                    }
                }
                fill(9, 16, wallN, 9, 17, wallN, "air");
                set(12, 20, z1 + 2, "lightning_rod[facing=up]");
                fill(30, 16, z1 + 3, 31, 18, z1 + 4, "iron_block");
                fill(34, 16, z2 - 3, 34, 19, z2 - 3, "iron_bars");
            }
            crate(18, 1, z2 - 1, "north", "parter");
            crate(25, 6, z2 - 1, "north", "parter");
            crate(34, 11, z1 + 1, "south", "parter");
            crate(17, 11, z2 - 1, "north", "parter");
            // sale żołnierskie: piętrowe prycze wezgłowiem do ściany zewnętrznej, szafki, skrzynka w kącie;
            // na piętrach pierwsza północna sala to kancelaria kompanii
            int[][] rooms = {{16, 21}, {23, 28}, {30, 37}};
            int[][] bunkXs = {{16, 18, 21}, {23, 25, 28}, {30, 32, 34, 37}};
            int[] lockers = {20, 27, 36};
            for (int h : new int[]{0, 5, 10}) {
                for (int r = 0; r < rooms.length; r++) {
                    for (boolean north : new boolean[]{true, false}) {
                        int head = north ? z1 + 1 : z2 - 1;
                        int foot = north ? z1 + 2 : z2 - 2;
                        int lane = north ? wallN - 1 : wallS + 1;
                        boolean office = r == 0 && north && h > 0;
                        boolean washroom = !south && h == 5 && r == 2 && !north;
                        boolean dayroom = south && h == 0 && !north;
                        boolean link = h == 5 && r == 1 && south == north;
                        if (washroom || dayroom) {
                            continue;
                        }
                        if (office) {
                            f.desk(19, h + 1, head, 21, head, "spruce");
                            f.chair(20, h + 1, foot, "south", "spruce");
                            f.lamp(21, h + 2, head);
                            f.bookshelf(16, h + 1, head, 17, head, 2, "south");
                        } else {
                            String color = armyBed();
                            for (int x : bunkXs[r]) {
                                if (link && x >= 24 && x <= 26 || isCrate(x, h + 1, head) || isCrate(x, h + 1, foot)) {
                                    continue;
                                }
                                bunk(x, h + 1, foot, north ? "north" : "south", color);
                            }
                            if (!north) {
                                f.wardrobe(lockers[r], h + 1, head, "north", "spruce");
                            }
                        }
                        if ((h / 5 + r + (north ? 0 : 1)) % 2 == 0) {
                            crate(office ? 16 : rooms[r][1], h + 1, lane, north ? "south" : "north", "parter");
                        }
                    }
                }
            }
            if (!south) {
                // umywalnia na piętrze: umywalki, dwa kible, prysznice pod sufitem
                for (int x = 30; x <= 34; x += 2) {
                    f.sink(x, 6, z2 - 1, "south");
                    set(x, 8, z2 - 2, "lightning_rod[facing=down]");
                }
                f.toilet(37, 6, z2 - 1, "east");
                f.toilet(37, 6, z2 - 3, "east");
                crate(30, 6, wallS + 1, "south", "toalety");
                sign(20, 3, zs + 6, "spruce_wall_sign[facing=south]", "KTO ZAJUMAŁ", "MOJE BUTY", "TEN NIECH", "UWAŻA");
            } else {
                // świetlica na parterze (bez ścianki między dwiema salami)
                fill(22, 1, wallS + 1, 22, 4, z2 - 1, "air");
                fill(16, 1, wallS + 1, 28, 2, z2 - 1, "air");
                fill(16, 1, z2 - 1, 17, 2, z2 - 1, "black_concrete");
                set(16, 3, z2 - 1, "gray_concrete");
                fill(22, 1, wallS + 2, 24, 1, wallS + 3, "spruce_slab[type=top]");
                for (int x = 22; x <= 24; x++) {
                    set(x, 2, wallS + 2, "green_carpet");
                    set(x, 2, wallS + 3, "green_carpet");
                }
                f.chair(21, 1, wallS + 2, "west", "spruce");
                f.chair(25, 1, wallS + 3, "east", "spruce");
                f.chair(23, 1, wallS + 1, "north", "spruce");
                // kanapa przed telewizorem i regał
                f.bench(16, 1, wallS + 2, 3, true, "north", "spruce");
                f.bookshelf(27, 1, z2 - 1, 28, z2 - 1, 2, "north");
                f.hangingLamp(23, 4, wallS + 2);
                crate(28, 1, wallS + 1, "south", "parter");
                // magazynek mundurowy
                fill(30, 1, wallS + 1, 37, 2, z2 - 1, "air");
                for (int x = 31; x <= 37; x += 2) {
                    set(x, 1, z2 - 1, "spruce_planks");
                    set(x, 2, z2 - 1, chance(0.5) ? "green_wool" : "brown_wool");
                    set(x, 3, z2 - 1, "spruce_slab[type=bottom]");
                }
                crate(33, 1, wallS + 1, "south", "strych");
            }
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 4, 8);
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 14, 8);
        }

        private void barracksLink() {
            for (int[] p : new int[][]{{23, 23}, {27, 23}, {23, 29}, {27, 29}}) {
                fill(p[0], 1, p[1], p[0], 4, p[1], "polished_andesite");
            }
            for (int x = 23; x <= 27; x++) {
                for (int z = 23; z <= 29; z++) {
                    set(x, 5, z, "polished_andesite");
                    set(x, 10, z, "smooth_stone");
                }
            }
            for (int z = 23; z <= 29; z++) {
                for (int y = 6; y <= 9; y++) {
                    set(23, y, z, y == 7 && z % 2 == 0 ? brokenGlass() : olive.next());
                    set(27, y, z, y == 7 && z % 2 == 1 ? brokenGlass() : olive.next());
                }
            }
            fill(24, 6, 22, 26, 7, 22, "air");
            fill(24, 6, 30, 26, 7, 30, "air");
            // przejście przez północne sale bloku B i południowe bloku A
            fill(24, 6, 18, 26, 7, 21, "air");
            fill(24, 6, 31, 26, 7, 34, "air");
            // barykada: jedno przejście z trzech zastawione, obok ktoś, kto jej bronił
            set(24, 6, 22, "crafting_table");
            set(24, 7, 22, "spruce_fence");
            f.skeleton(24, 6, 23, "south");
        }

        private void canteen() {
            int x1 = 108;
            int x2 = 132;
            int z1 = 9;
            int z2 = 21;
            walls(x1, z1, x2, z2, 1, 5, olive);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 6, z, "smooth_stone");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, chance(0.8) ? "white_terracotta" : "light_gray_terracotta");
                    }
                }
            }
            fill(125, 1, z1 + 1, 125, 5, z2 - 1, olive);
            fill(126, 1, 15, x2 - 1, 5, 15, olive);
            fill(x1, 1, 14, x1, 2, 15, "air");
            fill(125, 1, 12, 125, 2, 12, "air");
            fill(128, 1, 15, 128, 2, 15, "air");
            windowsX(z1, 110, 124, 3, 2, 3);
            windowsX(z2, 110, 124, 3, 2, 3);
            // długie stoły z ławami z obu stron, na stołach świeczki i lampy
            for (int z = 11; z <= 19; z += 4) {
                fill(111, 1, z, 122, 1, z, "dark_oak_slab[type=top]");
                f.bench(111, 1, z - 1, 5, true, "north", "spruce");
                f.bench(117, 1, z - 1, 6, true, "north", "spruce");
                f.bench(111, 1, z + 1, 6, true, "south", "spruce");
                f.bench(118, 1, z + 1, 5, true, "south", "spruce");
                f.lamp(112 + rnd.nextInt(9), 2, z);
            }
            f.hangingLamp(116, 5, 13);
            f.hangingLamp(116, 5, 17);
            crate(124, 1, 10, "west", "lada");
            crate(124, 1, 20, "west", "parter");
            sign(109, 3, 11, "spruce_wall_sign[facing=east]", "MIELONKA", "TRZECI ROK", "Z RZĘDU", "");
            // kuchnia: kuchenki z okapami i zlewy
            for (int x = 127; x <= 130; x++) {
                if (x % 2 == 0) {
                    f.sink(x, 1, 10, "north");
                } else {
                    f.stove(x, 1, 10, "south");
                }
            }
            fill(127, 1, 13, 130, 1, 13, "smooth_stone_slab[type=top]");
            crate(131, 1, 12, "west", "lada");
            crate(126, 1, 10, "east", "lada");
            // chłodnia z hakami
            for (int x = 127; x <= 130; x += 3) {
                fill(x, 4, 18, x, 5, 18, "chain[axis=y]");
            }
            fill(126, 1, 20, 131, 1, 20, "packed_ice");
            crate(131, 1, 17, "west", "lada");
            crate(126, 1, 18, "east", "zaplecze");
            crate(110, 1, 20, "east", "parter");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 5, 8);
        }

        private void infirmary() {
            int x1 = 108;
            int x2 = 126;
            int z1 = 28;
            int z2 = 42;
            walls(x1, z1, x2, z2, 1, 5, hospital);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 6, z, "smooth_stone");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, chance(0.85) ? "white_concrete" : "light_gray_concrete");
                    }
                }
            }
            fill(x1 + 1, 1, 34, x2 - 1, 5, 34, hospital);
            fill(x1 + 1, 1, 36, x2 - 1, 5, 36, hospital);
            fill(115, 1, z1 + 1, 115, 5, 33, hospital);
            fill(121, 1, 37, 121, 5, z2 - 1, hospital);
            fill(x1, 1, 35, x1, 2, 35, "air");
            fill(112, 1, 34, 112, 2, 34, "air");
            fill(120, 1, 34, 120, 2, 34, "air");
            fill(112, 1, 36, 112, 2, 36, "air");
            door(123, 1, 36, "spruce_door", "south", "left", true);
            // czerwony krzyż na ścianie frontowej
            fill(x1, 3, 31, x1, 3, 33, "red_concrete");
            set(x1, 2, 32, "red_concrete");
            set(x1, 4, 32, "red_concrete");
            windowsX(z2, 110, 124, 3, 2, 3);
            windowsX(z1, 110, 124, 3, 3);
            // gabinet, apteka z ladą, sala z łóżkami, izolatka
            f.bed(111, 1, 32, "west", "white");
            f.desk(113, 1, 29, 114, 29, "birch");
            f.chair(113, 1, 30, "south", "birch");
            f.lamp(114, 2, 29);
            crate(110, 1, 30, "east", "izba-chorych");
            fill(117, 1, 31, 124, 1, 31, "quartz_slab[type=top]");
            f.lamp(120, 2, 31);
            set(124, 1, 32, "brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]");
            crate(118, 1, 29, "south", "izba-chorych");
            crate(123, 1, 29, "south", "izba-chorych");
            for (int x = 110; x <= 119; x += 3) {
                f.bed(x, 1, 40, "south", "white");
                f.bed(x, 1, 38, "north", "white");
            }
            crate(115, 1, 41, "north", "izba-chorych");
            crate(117, 1, 37, "south", "parter");
            f.bed(124, 1, 40, "south", "white");
            set(124, 2, 39, "iron_bars");
            f.skeleton(122, 1, 41, "north");
            sign(116, 3, 35, "spruce_wall_sign[facing=south]", "LEKÓW BRAK", "LEKARZA BRAK", "NADZIEI BRAK", "");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 5, 6);
        }

        // ------------------------------------------------------------------ strefa techniczna

        /** Pięć boksów garażowych x 6-46 / z 56-66 z niskim strychem nad nimi (podłoga y5, dach y8). */
        private void garages() {
            int x1 = 6;
            int x2 = 46;
            int z1 = 56;
            int z2 = 66;
            walls(x1, z1, x2, z2, 1, 7, olive);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 5, z, "polished_andesite");
                    set(x, 8, z, chance(0.9) ? "smooth_stone" : "andesite");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, chance(0.85) ? "smooth_stone" : "cracked_stone_bricks");
                    }
                }
            }
            for (int px : new int[]{14, 22, 30, 38}) {
                fill(px, 1, z1 + 1, px, 4, z2 - 1, olive);
            }
            // bramy boksów
            fill(8, 1, z2, 12, 3, z2, "air");
            fill(24, 1, z2, 28, 3, z2, "air");
            fill(32, 1, z2, 36, 2, z2, "air");
            fill(32, 3, z2, 36, 3, z2, "iron_block");
            fill(40, 1, z2, 44, 3, z2, "air");
            fill(16, 1, z2, 20, 3, z2, "iron_block");
            // wyrwana krata wentylacyjna strychu nad BTR-em
            fill(37, 6, z2, 38, 7, z2, "air");
            set(39, 6, z2 + 1, "iron_bars");
            windowsX(z1, 10, 42, 8, 6);
            crate(8, 6, 58, "east", "strych");
            crate(44, 6, 64, "west", "strych");
            set(26, 6, 60, "spruce_planks");
            set(27, 6, 60, "brown_wool");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 7, 10);
            // boks 1: gazik, boks 3: stół warsztatowy, boks 4: opony, boks 5: kanał
            uaz(8, 59, false);
            fill(24, 1, 57, 28, 1, 57, "spruce_slab[type=top]");
            set(25, 2, 57, "grindstone[face=floor,facing=south]");
            set(29, 1, 57, "crafting_table");
            crate(24, 1, 58, "east", "klepisko");
            crate(13, 1, 57, "west", "klepisko");
            crate(37, 1, 57, "west", "klepisko");
            crate(45, 1, 57, "west", "klepisko");
            for (int z = 58; z <= 62; z += 2) {
                set(31, 1, z, "blackstone_wall");
                set(31, 2, z, "blackstone_wall");
            }
            fill(41, -2, 58, 43, 0, 64, "air");
            fill(41, -3, 58, 43, -3, 64, "smooth_stone");
            for (int z = 57; z <= 65; z++) {
                for (int y = -3; y <= 0; y++) {
                    set(40, y, z, "gray_concrete");
                    set(44, y, z, "gray_concrete");
                }
            }
            for (int x = 40; x <= 44; x++) {
                for (int y = -3; y <= 0; y++) {
                    set(x, y, 57, "gray_concrete");
                    set(x, y, 65, "gray_concrete");
                }
            }
            for (int y = -2; y <= 0; y++) {
                set(42, y, 64, "ladder[facing=north,waterlogged=false]");
            }
            crate(42, -2, 58, "south", "klepisko");
            // tunel z kanału do warsztatów
            fill(44, -2, 61, 52, -1, 61, "air");
            set(48, -1, 61, "lantern[hanging=true,waterlogged=false]");
            set(48, 0, 61, "gray_concrete");

            // Star przed boksami: zderzak, maska, kabina, plandeka
            int sx = 24;
            for (int z = 69; z <= 71; z++) {
                set(sx, 1, z, "polished_blackstone");
                fill(sx + 1, 1, z, sx + 2, 2, z, army);
                fill(sx + 3, 1, z, sx + 4, 3, z, army);
                for (int x = sx + 5; x <= sx + 10; x++) {
                    set(x, 1, z, "spruce_planks");
                    boolean side = z == 69 || z == 71;
                    set(x, 2, z, side ? "green_wool" : "air");
                    set(x, 3, z, side ? "green_wool" : "air");
                    set(x, 4, z, "green_wool");
                }
            }
            set(sx + 3, 2, 70, "glass_pane");
            set(sx + 3, 2, 69, "black_concrete");
            set(sx + 3, 2, 71, "black_concrete");
            // BTR na klockach przy ścianie garażu (dach y5 pod kratą wentylacyjną)
            for (int x = 35; x <= 41; x++) {
                for (int z = 67; z <= 71; z++) {
                    boolean corner = (x == 35 || x == 41) && (z == 67 || z == 71);
                    set(x, 1, z, corner ? "spruce_planks" : "air");
                    fill(x, 2, z, x, 5, z, army);
                }
            }
            fill(39, 6, 69, 40, 6, 70, army);
            set(41, 6, 70, "iron_bars");
            set(38, 3, 72, "black_concrete");
        }

        private void uaz(int x, int z, boolean alongZ) {
            int dx = alongZ ? 0 : 1;
            int dz = alongZ ? 1 : 0;
            for (int i = 0; i < 4; i++) {
                int cx = x + dx * i;
                int cz = z + dz * i;
                for (int w = 0; w < 2; w++) {
                    int px = cx + dz * w;
                    int pz = cz + dx * w;
                    set(px, 1, pz, army.next());
                    if (i >= 1 && i <= 2) {
                        set(px, 2, pz, i == 1 ? "glass_pane" : army.next());
                    }
                }
            }
        }

        /** Wiata postojowa x 6-46 / z 74-86 (dach y4), pod nią pojazdy; na dach wchodzi się po paletach. */
        private void parking() {
            for (int x = 6; x <= 46; x++) {
                for (int z = 74; z <= 86; z++) {
                    set(x, 4, z, chance(0.8) ? "light_gray_concrete" : "iron_block");
                }
            }
            for (int x = 6; x <= 46; x += 8) {
                fill(x, 1, 74, x, 3, 74, "polished_andesite");
                fill(x, 1, 86, x, 3, 86, "polished_andesite");
            }
            set(18, 4, 80, "air");
            set(19, 4, 80, "air");
            set(34, 4, 76, "air");
            uaz(9, 77, false);
            uaz(16, 81, false);
            // drugi Star bez plandeki
            for (int z = 77; z <= 79; z++) {
                set(24, 1, z, "polished_blackstone");
                fill(25, 1, z, 26, 2, z, army);
                fill(27, 1, z, 28, 3, z, army);
                fill(29, 1, z, 33, 1, z, "spruce_planks");
            }
            set(29, 2, 77, "spruce_fence");
            set(33, 2, 79, "spruce_fence");
            // BWP
            for (int x = 36; x <= 43; x++) {
                for (int z = 79; z <= 83; z++) {
                    fill(x, 1, z, x, z == 79 || z == 83 ? 1 : 2, z, z == 79 || z == 83 ? "gray_concrete" : army.next());
                }
            }
            fill(39, 3, 80, 40, 3, 82, army);
            for (int x = 34; x <= 38; x++) {
                set(x, 3, 81, "iron_bars");
            }
            // palety przy wiacie
            set(10, 1, 73, "spruce_planks");
            fill(11, 1, 73, 11, 2, 73, "spruce_planks");
            fill(12, 1, 73, 12, 3, 73, "spruce_planks");
            crate(8, 1, 75, "south", "plac");
            crate(44, 1, 85, "north", "plac");
        }

        /** Dźwig na podwoziu ciężarówki: z dachu wiaty na kabinę, po wysięgniku nad placem do kosza. */
        private void crane() {
            for (int x = 48; x <= 60; x++) {
                for (int z = 78; z <= 80; z++) {
                    set(x, 1, z, x % 3 == 0 && z != 79 ? "black_concrete" : "gray_concrete");
                }
            }
            for (int x = 48; x <= 50; x++) {
                for (int z = 78; z <= 80; z++) {
                    fill(x, 2, z, x, 5, z, "yellow_terracotta");
                }
            }
            set(48, 3, 79, "glass_pane");
            set(48, 4, 79, "glass_pane");
            fill(54, 2, 78, 57, 4, 80, "yellow_terracotta");
            set(55, 5, 79, "iron_block");
            for (int x = 51; x <= 65; x++) {
                set(x, 6, 79, x % 2 == 0 ? "yellow_concrete" : "iron_block");
            }
            // kosz zawieszony pod końcem wysięgnika
            for (int x = 64; x <= 68; x++) {
                for (int z = 77; z <= 81; z++) {
                    set(x, 3, z, "iron_block");
                    if (x == 64 || x == 68 || z == 77 || z == 81) {
                        set(x, 4, z, "iron_bars");
                    }
                }
            }
            set(65, 5, 79, "chain[axis=y]");
            crate(67, 4, 80, "west", "dzwig");
            crate(70, 1, 71, "north", "plac");
            for (int x = 51; x <= 70; x++) {
                for (int z = 74; z <= 88; z++) {
                    if (isAir(x, 1, z) && get(x, 0, z).contains("grass")) {
                        set(x, 0, z, asphalt.next());
                    }
                }
            }
        }

        private void workshops() {
            int x1 = 50;
            int x2 = 68;
            int z1 = 56;
            int z2 = 70;
            walls(x1, z1, x2, z2, 1, 7, sheet);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 8, z, chance(0.85) ? "light_gray_concrete" : "iron_block");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, chance(0.8) ? "smooth_stone" : "polished_andesite");
                    }
                }
            }
            fill(x2, 1, 60, x2, 4, 64, "air");
            windowsX(z1, 52, 66, 3, 5, 6);
            windowsX(z2, 52, 66, 3, 5, 6);
            // kanały naprawcze (drabinki na końcu, pierwszy połączony tunelem z garażami)
            for (int px : new int[]{53, 61}) {
                fill(px, -2, 59, px + 2, 0, 67, "air");
                fill(px, -3, 59, px + 2, -3, 67, "smooth_stone");
                for (int z = 58; z <= 68; z++) {
                    for (int y = -3; y <= -1; y++) {
                        if (!(px == 53 && z == 61 && y >= -2)) {
                            set(px - 1, y, z, "gray_concrete");
                        }
                        set(px + 3, y, z, "gray_concrete");
                    }
                }
                for (int x = px; x <= px + 2; x++) {
                    for (int y = -3; y <= -1; y++) {
                        set(x, y, 58, "gray_concrete");
                        set(x, y, 68, "gray_concrete");
                    }
                }
                for (int y = -2; y <= 0; y++) {
                    set(px + 1, y, 67, "ladder[facing=north,waterlogged=false]");
                }
                crate(px + 1, -2, 59, "south", "klepisko");
            }
            // stanowiska ślusarskie i suwnica pod dachem
            set(52, 1, 57, "anvil[facing=east]");
            set(54, 1, 57, "smithing_table");
            set(56, 1, 57, "grindstone[face=floor,facing=south]");
            fill(58, 1, 57, 64, 1, 57, "spruce_slab[type=top]");
            crate(66, 1, 57, "west", "klepisko");
            set(58, 1, 69, "crafting_table");
            set(59, 1, 69, "loom[facing=north]");
            crate(51, 1, 63, "east", "klepisko");
            crate(67, 1, 68, "west", "zaplecze");
            for (int x = x1 + 1; x < x2; x++) {
                set(x, 6, z1 + 1, "iron_block");
                set(x, 6, z2 - 1, "iron_block");
            }
            for (int z = z1 + 2; z < z2 - 1; z++) {
                set(58, 6, z, "yellow_concrete");
            }
            fill(58, 3, 64, 58, 5, 64, "chain[axis=y]");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 7, 10);
        }

        private void fuelStation() {
            for (int x = 80; x <= 94; x++) {
                for (int z = 56; z <= 68; z++) {
                    set(x, 0, z, apron.next());
                }
            }
            for (int x = 82; x <= 92; x++) {
                for (int z = 58; z <= 66; z++) {
                    set(x, 5, z, chance(0.85) ? "white_concrete" : "light_gray_concrete");
                }
            }
            for (int[] p : new int[][]{{82, 58}, {92, 58}, {82, 66}, {92, 66}}) {
                fill(p[0], 1, p[1], p[0], 4, p[1], "polished_andesite");
            }
            for (int x : new int[]{86, 89}) {
                set(x, 1, 62, "iron_block");
                set(x, 2, 62, "red_concrete");
                set(x, 2, 63, "tripwire_hook[facing=south,attached=false,powered=false]");
            }
            for (int x = 84; x <= 90; x += 3) {
                set(x, 0, 57, "iron_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]");
            }
            walls(95, 57, 99, 61, 1, 3, olive);
            fill(95, 4, 57, 99, 4, 61, "smooth_stone");
            fill(95, 1, 59, 95, 2, 59, "air");
            set(97, 2, 57, "glass_pane");
            crate(84, 1, 64, "east", "paliwo");
            crate(90, 1, 64, "west", "paliwo");
            crate(80, 1, 57, "east", "paliwo");
            crate(94, 1, 67, "west", "paliwo");
            crate(98, 1, 60, "west", "klepisko");
            f.desk(98, 1, 58, 98, 58, "spruce");
            f.lamp(98, 2, 58);
        }

        /** Kotłownia z drabiną na dach, rura ciepłownicza do pomostu przy kominie, klamry urwane w połowie. */
        private void boilerHouse() {
            int x1 = 108;
            int x2 = 126;
            int z1 = 56;
            int z2 = 70;
            int roof = 14;
            walls(x1, z1, x2, z2, 1, roof - 1, brick);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, roof, z, chance(0.85) ? "smooth_stone" : "andesite");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, chance(0.7) ? "stone_bricks" : "black_concrete");
                    }
                }
            }
            fill(116, 1, z2, 117, 3, z2, "air");
            windowsX(z1, 111, 123, 4, 4, 5, 10, 11);
            windowsX(z2, 111, 123, 4, 4, 5, 10, 11);
            // kotły
            for (int bx : new int[]{112, 119}) {
                for (int x = bx; x <= bx + 4; x++) {
                    for (int z = 59; z <= 66; z++) {
                        for (int y = 1; y <= 5; y++) {
                            if (Math.hypot(x - (bx + 2), y - 2.5) <= 2.6) {
                                set(x, y, z, z == 59 ? "polished_blackstone" : chance(0.85) ? "iron_block" : "orange_terracotta");
                            }
                        }
                    }
                }
                set(bx + 2, 2, 58, "furnace[facing=north,lit=false]");
            }
            // drabina na dach
            for (int y = 1; y <= roof; y++) {
                set(x2 - 1, y, z1 + 1, "ladder[facing=west,waterlogged=false]");
            }
            crate(110, 1, 68, "east", "parter");
            crate(124, 1, 68, "west", "parter");
            // kącik palaczy
            f.table(109, 1, 62, "spruce");
            f.chair(109, 1, 61, "north", "spruce");
            crate(109, 1, 57, "south", "zaplecze");
            crate(103, 1, 71, "north", "plac");
            // hałda węgla pod zachodnią ścianą
            for (int x = 101; x < x1; x++) {
                for (int z = 57; z <= 67; z++) {
                    double d = Math.hypot((x - 105) * 1.2, z - 62);
                    int h = (int) Math.round(3.4 - d * 0.6);
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, chance(0.6) ? "coal_block" : "black_concrete");
                    }
                }
            }
            // rura ciepłownicza z dachu do pomostu przy kominie
            for (int x = x2 + 1; x <= 132; x++) {
                set(x, roof, 61, chance(0.8) ? "light_gray_wool" : "iron_block");
            }
            for (int x : new int[]{129, 132}) {
                fill(x, 1, 61, x, roof - 1, 61, "andesite_wall");
            }
            // komin x 134-137 / z 60-63, pusty w środku
            int top = 29;
            for (int x = 134; x <= 137; x++) {
                for (int z = 60; z <= 63; z++) {
                    boolean shell = x == 134 || x == 137 || z == 60 || z == 63;
                    if (shell) {
                        fill(x, 1, z, x, top, z, brick);
                        if (chance(0.5)) {
                            set(x, top - 1, z, "black_concrete");
                        }
                    }
                }
            }
            // pomost w połowie i na szczycie
            for (int x = 133; x <= 138; x++) {
                for (int z = 59; z <= 64; z++) {
                    boolean ring = x == 133 || x == 138 || z == 59 || z == 64;
                    if (ring) {
                        set(x, 15, z, "iron_block");
                    }
                    boolean hole = x >= 135 && x <= 136 && z >= 61 && z <= 62;
                    if (!hole) {
                        set(x, top + 1, z, chance(0.85) ? "iron_block" : "polished_andesite");
                    }
                }
            }
            // klamry: dół do y9, potem urwane do pomostu, od pomostu do szczytu
            for (int y = 1; y <= top + 1; y++) {
                if (y <= 9 || y >= 16) {
                    set(135, y, 59, "ladder[facing=north,waterlogged=false]");
                }
            }
            crate(137, top + 2, 64, "west", "komin");
            set(133, top + 2, 59, "lightning_rod[facing=up]");
        }

        private void foodStore() {
            int x1 = 80;
            int x2 = 100;
            int z1 = 74;
            int z2 = 88;
            walls(x1, z1, x2, z2, 1, 6, sheet);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 7, z, chance(0.85) ? "light_gray_concrete" : "iron_block");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, "smooth_stone");
                    }
                }
            }
            fill(x1, 1, 80, x1, 3, 82, "air");
            windowsX(z1, 83, 97, 4, 5);
            for (int z : new int[]{77, 81, 85}) {
                for (int x = 83; x <= 97; x++) {
                    set(x, 1, z, "spruce_planks");
                    set(x, 2, z, chance(0.5) ? "brown_wool" : "air");
                    set(x, 3, z, "spruce_slab[type=bottom]");
                }
            }
            crate(84, 1, 79, "south", "lada");
            crate(96, 1, 83, "north", "lada");
            crate(90, 1, 87, "north", "parter");
            crate(82, 1, 75, "south", "magazyn");
            crate(99, 1, 79, "west", "lada");
            crate(82, 1, 86, "east", "magazyn");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 6, 8);
        }

        /**
         * Magazyn sprzętu: dwa rzędy regałów z kładką na górze (y6). Na regał wchodzi się po stopniach
         * z palet, jedna sekcja jest przewrócona (2 bloki do przeskoczenia).
         */
        private void equipmentStore() {
            int x1 = 106;
            int x2 = 138;
            int z1 = 74;
            int z2 = 88;
            walls(x1, z1, x2, z2, 1, 9, sheet);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 10, z, chance(0.85) ? "light_gray_concrete" : "iron_block");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        set(x, 0, z, "smooth_stone");
                    }
                }
            }
            fill(x1, 1, 80, x1, 3, 82, "air");
            windowsX(z1, 110, 134, 4, 8);
            windowsX(z2, 110, 134, 4, 8);
            for (int z : new int[]{77, 84}) {
                for (int x = 112; x <= 134; x++) {
                    if (x == 124 || x == 125) {
                        continue;
                    }
                    for (int y = 2; y <= 6; y += 2) {
                        set(x, y, z, "spruce_slab[type=top]");
                        set(x, y, z + 1, "spruce_slab[type=top]");
                    }
                    if (x % 6 == 0) {
                        for (int y = 1; y <= 5; y += 2) {
                            set(x, y, z, "spruce_fence");
                            set(x, y, z + 1, "spruce_fence");
                        }
                    } else {
                        for (int y = 1; y <= 5; y += 2) {
                            if (chance(0.35)) {
                                set(x, y, z, chance(0.5) ? "green_wool" : "brown_wool");
                            }
                        }
                    }
                }
            }
            // stopnie z palet na pierwszy rząd
            for (int i = 0; i < 5; i++) {
                int x = 107 + i;
                fill(x, 1, 77, x, 1 + i, 78, "spruce_planks");
            }
            fill(112, 1, 77, 112, 5, 78, "spruce_planks");
            // przewrócona sekcja leży na podłodze
            fill(123, 1, 80, 126, 1, 81, "spruce_planks");
            set(124, 2, 80, "green_wool");
            // kładka na drugi rząd
            fill(130, 6, 79, 131, 6, 83, "iron_block");
            crate(134, 7, 78, "west", "strych");
            crate(113, 7, 84, "east", "strych");
            crate(110, 1, 86, "east", "parter");
            crate(137, 1, 75, "west", "magazyn");
            crate(137, 1, 87, "west", "magazyn");
            crate(118, 1, 81, "north", "magazyn");
            cobwebs(x1 + 1, z1 + 1, x2 - 1, z2 - 1, 9, 10);
        }

        // ------------------------------------------------------------------ strefa bojowa

        /** Ogrodzony magazyn uzbrojenia: trzy bunkry obsypane ziemią, jedno wejście przez wyrwę w siatce. */
        private void armory() {
            int fx1 = 9;
            int fx2 = 36;
            int fz1 = 93;
            int fz2 = 122;
            int[][] mounds = new int[WIDTH][DEPTH];
            int[][] boxes = {{15, 99, 21, 104}, {26, 99, 32, 104}, {18, 111, 28, 117}};
            for (int[] b : boxes) {
                for (int x = b[0] - 5; x <= b[2] + 5; x++) {
                    for (int z = b[1] - 5; z <= b[3] + 5; z++) {
                        if (x <= fx1 || x >= fx2 || z <= fz1 || z >= fz2) {
                            continue;
                        }
                        int dx = Math.max(0, Math.max(b[0] - x, x - b[2]));
                        int dz = Math.max(0, Math.max(b[1] - z, z - b[3]));
                        int h = 6 - Math.max(dx, dz);
                        mounds[x][z] = Math.max(mounds[x][z], h);
                    }
                }
            }
            for (int x = fx1 + 1; x < fx2; x++) {
                for (int z = fz1 + 1; z < fz2; z++) {
                    int h = mounds[x][z];
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, y == h ? (chance(0.8) ? "grass_block" : "coarse_dirt") : "dirt");
                    }
                }
            }
            for (int b = 0; b < boxes.length; b++) {
                int[] box = boxes[b];
                walls(box[0], box[1], box[2], box[3], 1, 4, plaster);
                for (int x = box[0]; x <= box[2]; x++) {
                    for (int z = box[1]; z <= box[3]; z++) {
                        set(x, 5, z, "gray_concrete");
                        if (x > box[0] && x < box[2] && z > box[1] && z < box[3]) {
                            fill(x, 1, z, x, 4, z, "air");
                            set(x, 0, z, "smooth_stone");
                        }
                    }
                }
            }
            // wejścia do dwóch pierwszych bunkrów od południa, wycięte w nasypie
            for (int dxDoor : new int[]{18, 29}) {
                fill(dxDoor - 1, 1, 105, dxDoor + 1, 6, 109, "air");
                for (int z = 105; z <= 109; z++) {
                    set(dxDoor - 1, 0, z, "gravel");
                    set(dxDoor, 0, z, "gravel");
                    set(dxDoor + 1, 0, z, "gravel");
                }
                fill(dxDoor - 2, 1, 105, dxDoor - 2, 3, 109, "smooth_stone");
                fill(dxDoor + 2, 1, 105, dxDoor + 2, 3, 109, "smooth_stone");
                door(dxDoor, 1, 104, "iron_door", "south", "left", true);
            }
            // trzeci bunkier zawalony: drzwi zasypane, wejście przez dziurę w dachu z drabiną
            fill(22, 1, 109, 24, 3, 110, rubble);
            // przekop łączący wyrwę w siatce z wejściami do bunkrów
            fill(17, 1, 107, 35, 6, 108, "air");
            for (int x = 17; x <= 35; x++) {
                set(x, 0, 107, "gravel");
                set(x, 0, 108, chance(0.5) ? "gravel" : "coarse_dirt");
            }
            fill(22, 5, 114, 23, 6, 115, "air");
            fill(21, 1, 114, 21, 5, 114, "cobblestone");
            for (int y = 1; y <= 6; y++) {
                set(22, y, 114, "ladder[facing=east,waterlogged=false]");
            }
            fill(25, 1, 115, 27, 2, 116, rubble);
            // regały i skrzynie z bronią
            for (int[] box : boxes) {
                for (int x = box[0] + 1; x < box[2]; x += 2) {
                    set(x, 1, box[1] + 1, "dark_oak_planks");
                    set(x, 2, box[1] + 1, chance(0.5) ? "green_terracotta" : "iron_bars");
                }
            }
            crate(16, 1, 102, "east", "magazyn-broni");
            crate(20, 1, 102, "west", "magazyn-broni");
            crate(27, 1, 102, "east", "magazyn-broni");
            crate(31, 1, 102, "west", "magazyn-broni");
            crate(19, 1, 116, "east", "magazyn-broni");
            crate(26, 1, 113, "west", "magazyn-broni");
            // ogrodzenie: siatka 3 bloki, drut na górze, jedna wyrwa od wschodu
            for (int x = fx1; x <= fx2; x++) {
                compoundFence(x, fz1, x);
                compoundFence(x, fz2, x);
            }
            for (int z = fz1; z <= fz2; z++) {
                compoundFence(fx1, z, z);
                compoundFence(fx2, z, z);
            }
            fill(fx2, 1, 107, fx2, 4, 108, "air");
            sign(fx2 + 1, 2, 106, "spruce_wall_sign[facing=east]", "MAGAZYN", "UZBROJENIA", "", "");
            set(fx2, 2, 106, "polished_andesite");
            // wieżyczka strażnicza w rogu ogrodzenia
            for (int[] leg : new int[][]{{33, 95}, {35, 95}, {33, 97}, {35, 97}}) {
                fill(leg[0], 1, leg[1], leg[0], 5, leg[1], "spruce_log[axis=y]");
            }
            fill(33, 6, 95, 35, 6, 97, "spruce_planks");
            for (int y = 1; y <= 6; y++) {
                set(34, y, 98, "ladder[facing=south,waterlogged=false]");
            }
            fill(34, 1, 97, 34, 5, 97, "spruce_log[axis=y]");
            set(33, 7, 95, "spruce_fence");
            set(35, 7, 95, "spruce_fence");
            fill(33, 8, 95, 35, 8, 97, "spruce_slab[type=bottom]");
            fill(33, 8, 95, 33, 8, 95, "spruce_planks");
        }

        private void compoundFence(int x, int z, int pos) {
            if (pos % 4 == 0) {
                fill(x, 1, z, x, 4, z, "polished_andesite");
            } else {
                fill(x, 1, z, x, 3, z, "iron_bars");
                if (chance(0.6)) {
                    set(x, 4, z, "cobweb");
                }
            }
        }

        /** Lądowisko x 48-68 / z 92-112 z wrakiem Mi-8 przechylonym na bok. */
        private void helipad() {
            int cx = 58;
            int cz = 102;
            for (int x = 48; x <= 68; x++) {
                for (int z = 92; z <= 112; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    String b = apron.next();
                    if (Math.abs(d - 8) < 0.6) {
                        b = "yellow_concrete";
                    }
                    boolean hBar = (x == 55 || x == 61) && z >= 98 && z <= 106;
                    boolean hCross = z == 102 && x >= 55 && x <= 61;
                    if (hBar || hCross) {
                        b = "white_concrete";
                    }
                    set(x, 0, z, b);
                }
            }
            // kadłub przechylony: im dalej na południe, tym wyżej
            for (int x = 52; x <= 63; x++) {
                for (int z = 94; z <= 97; z++) {
                    int h = z - 93;
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, x <= 53 && y >= 2 && z < 97 ? "glass" : army.next());
                    }
                }
            }
            crate(53, 4, 97, "north", "wrak");
            for (int x = 64; x <= 70; x++) {
                set(x, 2, 96, army.next());
                set(x, 3, 96, x == 70 ? army.next() : "air");
            }
            set(70, 4, 96, "iron_bars");
            // łopaty wirnika leżą obok
            for (int i = 0; i < 7; i++) {
                set(55 + i, 1, 99, "iron_bars");
                set(58, 1, 92 + i, "iron_bars");
            }
            crate(50, 1, 108, "east", "plac");
            crate(66, 1, 94, "west", "plac");
        }

        /** Łukowy hangar x 76-92 / z 94-118 ze śmigłowcem bez wirnika i szybem windy do schronu. */
        private void hangar() {
            int cx = 84;
            int z1 = 94;
            int z2 = 118;
            for (int x = 76; x <= 92; x++) {
                int dx = Math.abs(x - cx);
                int outer = (int) Math.floor(Math.sqrt(8.5 * 8.5 - dx * dx));
                int inner = dx <= 7 ? (int) Math.floor(Math.sqrt(7.5 * 7.5 - dx * dx)) : 0;
                for (int z = z1; z <= z2; z++) {
                    for (int y = inner + 1; y <= outer; y++) {
                        set(x, y, z, (z - z1) % 6 == 0 ? "polished_andesite" : sheet.next());
                    }
                    if (z == z2) {
                        for (int y = 1; y <= inner; y++) {
                            set(x, y, z, sheet.next());
                        }
                    }
                    if (x > 76 && x < 92) {
                        set(x, 0, z, apron.next());
                    }
                }
            }
            fill(83, 1, z2, 85, 2, z2, "air");
            // śmigłowiec bez wirnika i rusztowanie
            for (int x = 79; x <= 89; x++) {
                for (int z = 99; z <= 102; z++) {
                    fill(x, 1, z, x, x <= 80 ? 2 : 3, z, x <= 80 && z > 99 && z < 102 ? "glass" : army.next());
                }
            }
            for (int x = 90; x <= 91; x++) {
                set(x, 2, 100, army.next());
            }
            for (int y = 1; y <= 4; y++) {
                set(80, y, 104, "spruce_fence");
                set(83, y, 104, "spruce_fence");
            }
            fill(80, 4, 103, 83, 4, 104, "spruce_slab[type=bottom]");
            crate(90, 1, 96, "west", "klepisko");
            // barierka wokół szybu windy (przejście przy drabinie)
            for (int x = 85; x <= 89; x++) {
                set(x, 1, 109, "iron_bars");
                set(x, 1, 113, "iron_bars");
            }
            for (int z = 109; z <= 113; z++) {
                set(85, 1, z, "iron_bars");
                set(89, 1, z, "iron_bars");
            }
            set(89, 1, 111, "air");
            // obozowisko dzikich lokatorów w tylnym kącie hangaru
            f.camp(79, 1, 114, "north");
            crate(78, 1, 117, "east", "zaplecze");
        }

        /** Wieża kontroli lotów x 46-53 / z 114-121: schody do połowy, dalej drabina z balkonu. */
        private void flightTower() {
            int x1 = 46;
            int x2 = 53;
            int z1 = 114;
            int z2 = 121;
            int cabin = 20;
            walls(x1, z1, x2, z2, 1, cabin, plaster);
            for (int x = x1 + 1; x < x2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 0, z, "smooth_stone");
                    set(x, 5, z, "polished_andesite");
                    set(x, 10, z, "polished_andesite");
                    set(x, cabin, z, "polished_andesite");
                }
            }
            stairCore(x1 + 1, z1 + 1, 0, 2, "stone_brick", "stone_bricks");
            fill(51, 1, z1, 51, 2, z1, "air");
            windowsZ(x1, 116, 119, 3, 2, 7, 12, 16);
            windowsX(z2, 48, 51, 3, 3, 8, 14);
            // zawalone wyżej: gruz na podeście y10
            set(48, 11, 120, rubble.next());
            set(49, 11, 119, rubble.next());
            // balkon i zewnętrzna drabina
            fill(x2, 11, 116, x2, 12, 117, "air");
            for (int x = x2 + 1; x <= x2 + 3; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 10, z, "iron_block");
                    if (x == x2 + 3 || z == z1 || z == z2) {
                        set(x, 11, z, "iron_bars");
                    }
                }
            }
            for (int y = 11; y <= cabin; y++) {
                set(x2 + 1, y, 119, "ladder[facing=east,waterlogged=false]");
            }
            // kabina z oknami na cztery strony
            for (int y = cabin + 1; y <= cabin + 3; y++) {
                for (int x = x1; x <= x2; x++) {
                    for (int z = z1; z <= z2; z++) {
                        boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                        boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                        if (corner) {
                            set(x, y, z, "iron_block");
                        } else if (edge) {
                            set(x, y, z, y == cabin + 1 ? plaster.next() : brokenGlass());
                        }
                    }
                }
            }
            fill(x2, cabin + 1, 119, x2, cabin + 2, 119, "air");
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, cabin + 4, z, "smooth_stone");
                }
            }
            set(49, cabin + 5, 117, "lightning_rod[facing=up]");
            set(50, cabin + 5, 118, "iron_bars");
            fill(48, cabin + 1, 115, 51, cabin + 1, 115, "polished_blackstone_slab[type=top]");
            set(50, cabin + 2, 115, "lever[face=floor,facing=south,powered=false]");
            crate(48, cabin + 1, 120, "east", "wieza-lotow");
            crate(49, 6, 120, "east", "parter");
        }

        private void shootingRange() {
            for (int x = 96; x <= 144; x++) {
                for (int z = 93; z <= 106; z++) {
                    set(x, 0, z, chance(0.6) ? "coarse_dirt" : "sand");
                }
            }
            // wały ziemne
            for (int x = 101; x <= 140; x++) {
                for (int z : new int[]{93, 94, 105, 106}) {
                    int h = z == 93 || z == 106 ? 2 : 3;
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, y == h ? "grass_block" : "dirt");
                    }
                }
            }
            // kulochwyt za tarczami
            for (int x = 141; x <= 145; x++) {
                for (int z = 93; z <= 106; z++) {
                    int h = Math.min(6, 2 + (x - 141));
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, y == h ? "grass_block" : "dirt");
                    }
                }
            }
            for (int z = 96; z <= 103; z += 2) {
                set(138, 1, z, "spruce_fence");
                set(138, 2, z, "target");
            }
            // stanowiska strzeleckie pod daszkiem
            for (int x = 96; x <= 100; x++) {
                for (int z = 95; z <= 104; z++) {
                    set(x, 4, z, "spruce_slab[type=bottom]");
                }
            }
            for (int[] p : new int[][]{{96, 95}, {100, 95}, {96, 104}, {100, 104}}) {
                fill(p[0], 1, p[1], p[0], 3, p[1], "spruce_log[axis=y]");
            }
            for (int z = 95; z <= 104; z += 2) {
                set(101, 1, z, "packed_mud");
            }
            crate(98, 1, 96, "east", "plac");
            crate(98, 1, 103, "east", "plac");
            // budka instruktora
            walls(96, 108, 100, 112, 1, 3, olive);
            fill(96, 4, 108, 100, 4, 112, "smooth_stone");
            fill(100, 1, 110, 100, 2, 110, "air");
            set(98, 2, 108, "glass_pane");
            crate(97, 1, 111, "east", "parter");
            f.desk(97, 1, 109, 98, 109, "spruce");
            f.lamp(97, 2, 109);
        }

        /**
         * Tor przeszkód: stopień, ścianka, równoważnia nad rowem z wodą, słupki coraz wyżej
         * i wieża desantowa z lepszą skrzynią.
         */
        private void obstacleCourse() {
            for (int x = 102; x <= 140; x++) {
                for (int z = 109; z <= 121; z++) {
                    set(x, 0, z, chance(0.7) ? "coarse_dirt" : "sand");
                }
            }
            // opony
            for (int x = 104; x <= 110; x += 2) {
                for (int z = 111; z <= 113; z += 2) {
                    set(x, 1, z, "blackstone_wall");
                }
            }
            // siatka do czołgania
            for (int x = 104; x <= 110; x++) {
                for (int z = 118; z <= 120; z++) {
                    set(x, 2, z, "cobweb");
                }
                set(x, 1, 117, "spruce_fence");
            }
            set(113, 1, 115, "spruce_planks");
            fill(114, 1, 113, 115, 2, 117, "spruce_planks");
            // rów z wodą i równoważnia
            for (int x = 117; x <= 124; x++) {
                for (int z = 112; z <= 118; z++) {
                    set(x, 0, z, "water");
                    set(x, -1, z, "water");
                }
            }
            for (int x = 116; x <= 125; x++) {
                set(x, 2, 115, "spruce_log[axis=x]");
            }
            set(116, 1, 115, "spruce_log[axis=y]");
            set(125, 1, 115, "spruce_log[axis=y]");
            // słupki coraz wyżej
            int[][] posts = {{126, 3}, {128, 4}, {130, 5}, {131, 6}};
            for (int[] p : posts) {
                fill(p[0], 1, 115, p[0], p[1], 115, "spruce_log[axis=y]");
            }
            // wieża desantowa
            for (int[] leg : new int[][]{{132, 113}, {136, 113}, {132, 117}, {136, 117}}) {
                fill(leg[0], 1, leg[1], leg[0], 6, leg[1], "spruce_log[axis=y]");
            }
            for (int x = 132; x <= 136; x++) {
                for (int z = 113; z <= 117; z++) {
                    set(x, 7, z, "spruce_planks");
                    if ((x == 136 || z == 113 || z == 117) && !(x == 136 && z == 115)) {
                        set(x, 8, z, "spruce_fence");
                    }
                }
            }
            set(137, 7, 115, "spruce_planks");
            fill(137, 1, 115, 137, 6, 115, "chain[axis=y]");
            crate(134, 8, 114, "south", "wieza");
            crate(103, 1, 109, "south", "plac");
        }

        /**
         * Wieża wartownicza 4 × 4 na palach. Drabina od strony {@code ladderSouth ? południe : północ},
         * szczeble missFrom..missTo brakują. {@code openSide} = strona bez barierki (wejście z dachu albo wału).
         */
        private void watchtower(int x1, int z1, int floor, String openSide, boolean ladderSouth, int missFrom, int missTo) {
            int x2 = x1 + 3;
            int z2 = z1 + 3;
            for (int[] leg : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) {
                fill(leg[0], 1, leg[1], leg[0], floor - 1, leg[1], "spruce_log[axis=y]");
                fill(leg[0], floor + 1, leg[1], leg[0], floor + 2, leg[1], "spruce_log[axis=y]");
            }
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, floor, z, "spruce_planks");
                    set(x, floor + 3, z, "spruce_slab[type=bottom]");
                }
            }
            int ladderX = x1 + 1;
            int ladderZ = ladderSouth ? z2 + 1 : z1 - 1;
            int postZ = ladderSouth ? z2 : z1;
            for (int x = x1 + 1; x < x2; x++) {
                if (!"north".equals(openSide) && !(x == ladderX && !ladderSouth)) {
                    set(x, floor + 1, z1, "spruce_fence");
                }
                if (!"south".equals(openSide) && !(x == ladderX && ladderSouth)) {
                    set(x, floor + 1, z2, "spruce_fence");
                }
            }
            for (int z = z1 + 1; z < z2; z++) {
                if (!"west".equals(openSide)) {
                    set(x1, floor + 1, z, "spruce_fence");
                }
                if (!"east".equals(openSide)) {
                    set(x2, floor + 1, z, "spruce_fence");
                }
            }
            fill(ladderX, 1, postZ, ladderX, floor - 1, postZ, "spruce_log[axis=y]");
            String facing = ladderSouth ? "south" : "north";
            for (int y = 1; y <= floor; y++) {
                if (y < missFrom || y > missTo) {
                    set(ladderX, y, ladderZ, "ladder[facing=" + facing + ",waterlogged=false]");
                }
            }
            set(x1, floor + 4, z1, "redstone_lamp[lit=false]");
            crate(x1 + 2, floor + 1, ladderSouth ? z1 + 1 : z2 - 1, "south", "wieze-wartownicze");
        }

        /** Wał ziemny przy południowym płocie: łagodnie wznosi się do wieży w południowo-wschodnim rogu. */
        private void berm() {
            for (int x = 128; x <= 141; x++) {
                int h = (x - 127) / 2;
                for (int z = 121; z <= 124; z++) {
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, y == h ? "grass_block" : "dirt");
                    }
                }
            }
        }

        private void forest() {
            int[][] trees = {{78, 121}, {81, 124}, {93, 120}, {94, 124}, {60, 122}, {65, 124}, {104, 124}, {40, 124}};
            for (int[] t : trees) {
                tree(t[0], t[1]);
            }
            crate(99, 1, 121, "north", "plac");
        }

        private void tree(int x, int z) {
            int trunk = 5 + rnd.nextInt(2);
            for (int y = 1; y <= trunk; y++) {
                set(x, y, z, "birch_log[axis=y]");
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = trunk - 1; y <= trunk + 1; y++) {
                        int r = y > trunk ? 1 : 2;
                        if (Math.abs(dx) <= r && Math.abs(dz) <= r && !(Math.abs(dx) == 2 && Math.abs(dz) == 2)
                                && isAir(x + dx, y, z + dz)) {
                            set(x + dx, y, z + dz, "birch_leaves[distance=1,persistent=true,waterlogged=false]");
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ schron dowodzenia

        private static boolean isRock(String state) {
            return switch (state) {
                case "minecraft:stone", "minecraft:andesite", "minecraft:tuff", "minecraft:dirt" -> true;
                default -> false;
            };
        }

        /** Wykuwa pomieszczenie (stopy y1..y2), daje podłogę y1-1 i obudowuje skałę dookoła betonem. */
        private void room(int x1, int y1, int z1, int x2, int y2, int z2) {
            fill(x1, y1, z1, x2, y2, z2, "air");
            fill(x1, y1 - 1, z1, x2, y1 - 1, z2, "smooth_stone");
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                for (int y = y1 - 1; y <= y2 + 1; y++) {
                    for (int z = z1 - 1; z <= z2 + 1; z++) {
                        if (isRock(get(x, y, z))) {
                            set(x, y, z, chance(0.9) ? "gray_concrete" : "light_gray_concrete");
                        }
                    }
                }
            }
        }

        private void bunker() {
            // grzybek wejścia głównego
            walls(39, 108, 45, 114, 1, 3, plaster);
            fill(39, 4, 108, 45, 4, 114, "gray_concrete");
            fill(41, 5, 110, 43, 5, 112, "gray_concrete");
            fill(40, 0, 109, 44, 0, 113, "smooth_stone");
            fill(39, 1, 111, 39, 2, 111, "air");
            door(39, 1, 110, "iron_door", "west", "left", true);
            door(39, 1, 112, "iron_door", "west", "right", true);
            fill(39, 1, 110, 39, 2, 112, "air");
            door(38, 1, 110, "iron_door", "west", "right", true);
            fill(38, 1, 110, 38, 2, 110, "air");
            set(39, 3, 111, "gray_concrete");
            sign(39, 3, 113, "spruce_wall_sign[facing=west]", "SCHRON", "DOWODZENIA", "", "");
            set(39, 3, 113, "air");
            sign(38, 3, 112, "spruce_wall_sign[facing=west]", "SCHRON", "DOWODZENIA", "", "");
            set(38, 3, 112, "air");
            // schody w dół do poziomu 1
            for (int i = 0; i <= 9; i++) {
                int x = 41 + i;
                int step = -1 - i;
                room(x, step + 1, 110, x, Math.min(step + 3, 0), 112);
                for (int z = 110; z <= 112; z++) {
                    set(x, step, z, i == 9 ? "smooth_stone" : stairs("stone_brick", "west"));
                }
            }
            fill(41, 1, 110, 44, 3, 112, "air");
            f.hangingLamp(46, -3, 112);
            // śluza z dwiema parami pancernych drzwi
            room(51, L1, 110, 56, L1 + 2, 112);
            for (int sx : new int[]{52, 55}) {
                fill(sx, L1, 110, sx, L1 + 2, 112, "gray_concrete");
                door(sx, L1, 110, "iron_door", "east", "left", true);
                door(sx, L1, 111, "iron_door", "east", "right", true);
            }
            f.hangingLamp(53, L1 + 2, 111);
            // korytarz poziomu 1
            room(57, L1, 110, 88, L1 + 2, 112);
            for (int x = 57; x <= 88; x++) {
                set(x, L1 + 2, 110, "chain[axis=x]");
            }
            for (int x = 62; x <= 86; x += 8) {
                set(x, L1 + 2, 111, "lantern[hanging=true,waterlogged=false]");
            }
            // klatka schodowa na poziom 2 (środkowy bieg zawalony, zostaje drabina)
            room(57, L2, 98, 63, L1 + 3, 108);
            fill(57, L1 - 1, 106, 63, L1 - 1, 108, "smooth_stone");
            fill(60, L1, 109, 60, L1 + 1, 109, "air");
            for (int i = 0; i <= 5; i++) {
                set(60, -11 - i, 105 - i, stairs("stone_brick", "south"));
            }
            fill(58, -17, 98, 63, -17, 99, "smooth_stone");
            for (int y = L2; y <= -16; y++) {
                set(57, y, 98, "ladder[facing=east,waterlogged=false]");
            }
            for (int[] r : new int[][]{{59, 104}, {61, 102}, {62, 106}, {58, 103}}) {
                set(r[0], L2, r[1], rubble.next());
            }
            for (int x = 59; x <= 63; x++) {
                for (int z = 100; z <= 107; z++) {
                    if (isAir(x, L2, z) && chance(0.5)) {
                        set(x, L2 - 1, z, "mud");
                    }
                }
            }
            fill(60, L2, 109, 60, L2 + 1, 109, "air");
            set(63, L1, 106, "lantern[hanging=false,waterlogged=false]");
            set(63, L2, 107, "lantern[hanging=false,waterlogged=false]");
            f.skeleton(62, L2, 101, "north");
            // sala dowodzenia
            room(65, L1, 98, 75, L1 + 3, 108);
            fill(70, L1, 109, 70, L1 + 1, 109, "air");
            for (int x = 68; x <= 72; x++) {
                for (int z = 101; z <= 105; z++) {
                    set(x, L1, z, chance(0.3) ? "lime_terracotta" : "green_terracotta");
                }
            }
            for (int x = 66; x <= 74; x += 2) {
                set(x, L1, 98, "observer[facing=south,powered=false]");
                set(x + 1, L1, 98, "lever[face=floor,facing=south,powered=false]");
            }
            set(74, L1, 104, "note_block");
            set(74, L1 + 1, 104, "lightning_rod[facing=up]");
            set(70, L1 + 3, 103, "lantern[hanging=true,waterlogged=false]");
            f.hangingLamp(67, L1 + 3, 100);
            f.hangingLamp(73, L1 + 3, 106);
            f.chair(68, L1, 99, "south", "spruce");
            f.chair(72, L1, 99, "south", "spruce");
            crate(66, L1, 100, "east", "schron-1");
            crate(74, L1, 107, "west", "schron-1");
            // pokój szyfranta
            room(77, L1, 100, 83, L1 + 2, 108);
            fill(80, L1, 109, 80, L1 + 1, 109, "air");
            fill(78, L1, 101, 80, L1, 101, "dark_oak_slab[type=top]");
            set(79, L1, 102, stairs("dark_oak", "north"));
            crate(82, L1, 101, "west", "schron-1");
            f.lamp(78, L1 + 1, 101);
            f.hangingLamp(80, L1 + 2, 105);
            f.bookshelf(83, L1, 103, 83, 105, 2, "west");
            crate(78, L1, 107, "east", "zaplecze");
            // kwatery
            room(60, L1, 114, 72, L1 + 2, 122);
            fill(66, L1, 113, 66, L1 + 1, 113, "air");
            for (int x = 61; x <= 71; x += 2) {
                bunk(x, L1, 121, "south", armyBed());
            }
            f.table(68, L1, 116, "spruce");
            f.chair(67, L1, 116, "west", "spruce");
            f.hangingLamp(63, L1 + 2, 117);
            f.hangingLamp(69, L1 + 2, 119);
            crate(61, L1, 116, "east", "parter");
            crate(71, L1, 118, "west", "parter");
            // maszynownia z agregatem i szybem wentylacyjnym do skarbca
            room(75, L1, 114, 84, L1 + 3, 122);
            fill(79, L1, 113, 79, L1 + 1, 113, "air");
            fill(76, L1, 116, 79, L1 + 1, 119, "iron_block");
            set(77, L1 + 2, 117, "observer[facing=up,powered=false]");
            for (int z = 115; z <= 121; z += 2) {
                set(82, L1, z, "cauldron");
                set(82, L1 + 1, z, "iron_bars");
            }
            fill(84, L2 + 4, 121, 84, L1 - 1, 121, "air");
            for (int y = L2; y <= L1 - 1; y++) {
                set(84, y, 121, "ladder[facing=west,waterlogged=false]");
            }
            set(84, L1 + 3, 121, "lantern[hanging=true,waterlogged=false]");
            f.hangingLamp(80, L1 + 3, 115);
            crate(76, L1, 121, "east", "zaplecze");
            // szyb windy z hangaru do końca korytarza
            fill(86, L1 + 3, 110, 88, 0, 112, "air");
            for (int y = L1 + 3; y <= -1; y++) {
                for (int x = 85; x <= 89; x++) {
                    for (int z = 109; z <= 113; z++) {
                        if (isRock(get(x, y, z))) {
                            set(x, y, z, "gray_concrete");
                        }
                    }
                }
            }
            for (int y = L1; y <= 0; y++) {
                set(88, y, 111, "ladder[facing=west,waterlogged=false]");
            }

            // poziom 2: korytarz, zbrojownia za kratą, skarbiec za zamkniętymi drzwiami
            room(57, L2, 110, 90, L2 + 2, 112);
            for (int x = 57; x <= 90; x++) {
                set(x, L2 + 2, 112, "chain[axis=x]");
            }
            set(65, L2 + 2, 111, "lantern[hanging=true,waterlogged=false]");
            f.hangingLamp(74, L2 + 2, 111);
            f.hangingLamp(83, L2 + 2, 111);
            crate(90, L2, 110, "west", "zaplecze");
            sign(59, L2 + 1, 110, "spruce_wall_sign[facing=south]", "ZAWRÓĆ", "PÓKI", "JESZCZE", "MOŻESZ");
            room(65, L2, 98, 75, L2 + 2, 108);
            fill(65, L2, 109, 75, L2 + 2, 109, "iron_bars");
            fill(70, L2, 109, 70, L2 + 1, 109, "air");
            for (int x = 66; x <= 74; x += 2) {
                set(x, L2, 104, "dark_oak_planks");
                set(x, L2 + 1, 104, "iron_bars");
            }
            crate(66, L2, 99, "south", "zbrojownia");
            crate(70, L2, 99, "south", "zbrojownia");
            crate(74, L2, 99, "south", "zbrojownia");
            f.hangingLamp(70, L2 + 2, 106);
            room(75, L2, 114, 84, L2 + 3, 122);
            fill(84, L2, 121, 84, L1 - 1, 121, "air");
            for (int y = L2; y <= L1 - 1; y++) {
                set(84, y, 121, "ladder[facing=west,waterlogged=false]");
            }
            door(79, L2, 113, "iron_door", "north", "left", false);
            door(80, L2, 113, "iron_door", "north", "right", false);
            for (int z = 115; z <= 121; z += 2) {
                set(75, L2 + 1, z, "iron_trapdoor[facing=east,half=top,open=false,powered=false,waterlogged=false]");
            }
            fill(78, L2, 118, 80, L2, 118, "yellow_terracotta");
            crate(76, L2, 121, "east", "skarbiec");
            crate(76, L2, 115, "east", "skarbiec");
            f.hangingLamp(80, L2 + 3, 116);
            // zalany tunel kablowy do szybu awaryjnego
            room(87, L2, 113, 89, L2 + 2, 122);
            for (int z = 114; z <= 121; z++) {
                for (int x = 87; x <= 89; x++) {
                    set(x, L2 - 1, z, "water");
                }
                set(89, L2 + 2, z, "chain[axis=z]");
            }
            // szyb awaryjny: drabina urwana u dołu (tylko wyjście), wylot w betonowym kominku w lesie
            fill(87, L2 + 3, 122, 87, 4, 122, "air");
            for (int y = L2 + 3; y <= 3; y++) {
                set(87, y, 122, "ladder[facing=north,waterlogged=false]");
            }
            for (int x = 86; x <= 88; x++) {
                for (int z = 121; z <= 123; z++) {
                    if (x != 87 || z != 122) {
                        fill(x, 1, z, x, 4, z, "gray_concrete");
                    }
                    set(x, 5, z, "gray_concrete");
                }
            }
            fill(86, 3, 122, 86, 4, 122, "air");
            for (int y = L2 + 3; y <= -1; y++) {
                for (int x = 86; x <= 88; x++) {
                    for (int z = 121; z <= 123; z++) {
                        if (isRock(get(x, y, z))) {
                            set(x, y, z, "gray_concrete");
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ zarośla

        private void weeds() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 6; z < DEPTH; z++) {
                    String below = get(x, 0, z);
                    if (!below.equals("minecraft:grass_block") && !below.equals("minecraft:podzol")
                            && !below.equals("minecraft:coarse_dirt")) {
                        continue;
                    }
                    if (!isAir(x, 1, z) || !isAir(x, 2, z) || isCrate(x, 1, z) || !chance(0.15)) {
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
