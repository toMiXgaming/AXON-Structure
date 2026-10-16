package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.Random;

/**
 * Opuszczony Szpital Powiatowy „Pod Lipami” – pięciokondygnacyjny blok z wielkiej płyty z izbą przyjęć,
 * pawilonem zabiegowym (apteka, blok operacyjny), budynkiem gospodarczym, kaplicą i piwnicami.
 * <p>
 * Struktura ma {@code grunt: 9}. W kodzie wysokości liczone są od gruntu (y = 0 to powierzchnia,
 * piwnice mają stopy na y -4).
 * <pre>
 *  z 0-3     ulica, z 4 ogrodzenie z bramą x 48-55 i budką parkingową
 *  z 5-30    parking z samochodami i karetkami, podjazd z daszkiem z 16-23
 *  z 24-39   izba przyjęć (SOR) x 40-69, przyklejona do głównego bloku
 *  z 40-53   główny blok x 30-79: parter + 4 piętra (podłogi y 0, 5, 10, 15, 20, dach y 25),
 *            korytarz z 46-47, sale z 41-44 i 49-52, klatka zachodnia do 3 piętra, wschodnia do 2,
 *            szyb windy x 50-54 / z 41-45 (kabina utknęła między 2 a 3 piętrem), rusztowanie x 26-29
 *  x 80-83   łącznik na 1 piętrze do pawilonu zabiegowego x 84-103 / z 34-53 (apteka na parterze,
 *            blok operacyjny na piętrze)
 *  z 60-88   budynek gospodarczy (kuchnia, pralnia, kotłownia), zjazd do kostnicy, kaplica, park z altaną
 *  pod ziemią: piwnice pod blokiem (archiwum, magazyn pościeli, rozdzielnia, kostnica) i tunel
 *            do piwnicy budynku gospodarczego z zalanym odcinkiem
 * </pre>
 */
public final class HospitalStructure implements StructureDefinition {

    private static final int WIDTH = 110;
    private static final int DEPTH = 90;
    /** Ile warstw leży pod ziemią. */
    private static final int G = 9;
    private static final int HEIGHT = G + 31;
    /** Poziom stóp w piwnicach. */
    private static final int B = -4;

    @Override
    public String id() {
        return "szpital";
    }

    @Override
    public String displayName() {
        return "Szpital Powiatowy Pod Lipami";
    }

    @Override
    public String description() {
        return "Opuszczony szpital: oddziały na pięciu kondygnacjach, izba przyjęć, apteka, blok operacyjny, lądowisko na dachu i piwnice z kostnicą";
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
        private final Palette white;
        private final Palette lino;
        private final Palette brick;
        private final Palette rubble;
        private final Palette rock;
        private final Palette car;

        Builder(Random rnd) {
            this.rnd = rnd;
            grass = new Palette(rnd)
                    .add("grass_block", 12).add("coarse_dirt", 2).add("dirt", 1).add("podzol", 1);
            asphalt = new Palette(rnd)
                    .add("gray_concrete", 8).add("andesite", 2).add("cobblestone", 1).add("cracked_stone_bricks", 1);
            white = new Palette(rnd)
                    .add("white_concrete", 8).add("white_terracotta", 3).add("light_gray_concrete", 1)
                    .add("cracked_stone_bricks", 1);
            lino = new Palette(rnd)
                    .add("white_terracotta", 5).add("light_gray_terracotta", 3).add("cyan_terracotta", 1)
                    .add("cracked_stone_bricks", 1);
            brick = new Palette(rnd)
                    .add("bricks", 14).add("terracotta", 2).add("mud_bricks", 1);
            rubble = new Palette(rnd)
                    .add("cobblestone", 4).add("tuff", 2).add("cracked_stone_bricks", 2).add("andesite", 1);
            rock = new Palette(rnd)
                    .add("stone", 40).add("andesite", 1).add("tuff", 1);
            car = new Palette(rnd)
                    .add("light_blue_terracotta", 1).add("red_terracotta", 1).add("brown_terracotta", 1)
                    .add("white_terracotta", 1).add("green_terracotta", 1);
        }

        Blueprint build() {
            bp.ground(G);
            ground();
            perimeter();
            gate();
            parking();
            mainBlock();
            emergency();
            canopyAndAmbulance();
            pavilion();
            link();
            scaffold();
            roof();
            serviceBuilding();
            chapel();
            park();
            basement();
            crates();
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
            return chance(0.2) ? "light_blue_stained_glass_pane" : "glass_pane";
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

        private void cobwebs(int x1, int z1, int x2, int z2, int y, int count) {
            for (int i = 0; i < count; i++) {
                int x = x1 + rnd.nextInt(x2 - x1 + 1);
                int z = z1 + rnd.nextInt(z2 - z1 + 1);
                if (isAir(x, y, z) && !isAir(x, y + 1, z)) {
                    set(x, y, z, "cobweb");
                }
            }
        }

        /** Łóżko szpitalne na dwóch blokach wzdłuż z (z, z + dz), poduszka po stronie z. */
        private void bed(int x, int y, int z, int dz, String wool) {
            set(x, y, z, wool);
            set(x, y, z + dz, wool);
            set(x, y + 1, z, "white_carpet");
        }

        /**
         * Klatka schodowa 6 × 4 (x0..x0+5, z0..z0+3): biegi po 4 stopnie na kondygnację 5 bloków,
         * na zmianę na zachód (rząd z0+1, podest x0) i na wschód (rząd z0+3, podest x0+5).
         */
        private void stairCore(int x0, int z0, int base, int flights) {
            for (int k = 0; k < flights; k++) {
                int h = base + 5 * k;
                boolean west = k % 2 == 0;
                int row = west ? z0 + 1 : z0 + 3;
                for (int i = 0; i < 4; i++) {
                    int x = west ? x0 + 4 - i : x0 + 1 + i;
                    for (int y = h + 1; y < h + 1 + i; y++) {
                        set(x, y, row, "stone_bricks");
                    }
                    set(x, h + 1 + i, row, stairs("stone_brick", west ? "west" : "east"));
                }
                fill(x0 + 1, h + 5, row, x0 + 4, h + 5, row, "air");
            }
        }

        private static boolean isRock(String state) {
            return switch (state) {
                case "minecraft:stone", "minecraft:andesite", "minecraft:tuff", "minecraft:dirt" -> true;
                default -> false;
            };
        }

        /** Wykuwa pomieszczenie (stopy y1..y2), daje podłogę y1-1 i obudowuje ziemię dookoła betonem. */
        private void room(int x1, int y1, int z1, int x2, int y2, int z2) {
            fill(x1, y1, z1, x2, y2, z2, "air");
            fill(x1, y1 - 1, z1, x2, y1 - 1, z2, "smooth_stone");
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                for (int y = y1 - 1; y <= y2 + 1; y++) {
                    for (int z = z1 - 1; z <= z2 + 1; z++) {
                        if (isRock(get(x, y, z))) {
                            set(x, y, z, chance(0.85) ? "light_gray_concrete" : "white_terracotta");
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ teren, ogrodzenie, parking

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    fill(x, -G, z, x, -4, z, rock);
                    fill(x, -3, z, x, -1, z, "dirt");
                    set(x, 0, z, z <= 3 ? asphalt.next() : grass.next());
                }
            }
            // podjazd od bramy, parking i droga do pawilonu
            for (int x = 6; x <= 104; x++) {
                for (int z = 5; z <= 33; z++) {
                    boolean road = x >= 48 && x <= 59 && z <= 23;
                    boolean lot = x >= 6 && x <= 44 && z >= 8 && z <= 31;
                    boolean side = x >= 70 && x <= 104 && z >= 16 && z <= 26;
                    boolean toPavilion = x >= 84 && x <= 100 && z >= 26 && z <= 33;
                    if (road || lot || side || toPavilion) {
                        set(x, 0, z, chance(0.05) ? "mud" : asphalt.next());
                    }
                }
            }
            for (int x = 8; x <= 44; x += 4) {
                for (int z : new int[]{8, 9, 10, 11, 21, 22, 23, 24}) {
                    if (chance(0.7)) {
                        set(x, 0, z, "white_concrete");
                    }
                }
            }
            // droga techniczna wzdłuż wschodniej ściany do zjazdu przy kostnicy
            for (int x = 104; x <= 107; x++) {
                for (int z = 5; z <= 66; z++) {
                    set(x, 0, z, chance(0.6) ? "gravel" : "coarse_dirt");
                }
            }
            for (int x = 68; x <= 103; x++) {
                for (int z = 63; z <= 66; z++) {
                    set(x, 0, z, chance(0.6) ? "gravel" : "coarse_dirt");
                }
            }
            // ścieżki w parku
            for (int z = 54; z <= 87; z++) {
                for (int x = 52; x <= 53; x++) {
                    set(x, 0, z, chance(0.7) ? "gravel" : "coarse_dirt");
                }
            }
            for (int x = 30; x <= 100; x++) {
                for (int z = 85; z <= 86; z++) {
                    set(x, 0, z, chance(0.7) ? "gravel" : "coarse_dirt");
                }
            }
        }

        /** Murek z cegły z kratą na wierzchu i słupkami; brama od ulicy i kilka wyrw. */
        private void perimeter() {
            for (int x = 0; x < WIDTH; x++) {
                fencePost(x, 4, x);
                fencePost(x, DEPTH - 1, x);
            }
            for (int z = 4; z < DEPTH; z++) {
                fencePost(0, z, z);
                fencePost(WIDTH - 1, z, z);
            }
            fill(48, 1, 4, 59, 3, 4, "air");
            fill(104, 1, 4, 107, 3, 4, "air");
            int[][] gaps = {{20, 4, 22, 4}, {0, 40, 0, 42}, {WIDTH - 1, 70, WIDTH - 1, 72}, {30, DEPTH - 1, 32, DEPTH - 1},
                    {80, DEPTH - 1, 81, DEPTH - 1}};
            for (int[] g : gaps) {
                fill(g[0], 1, g[1], g[2], 3, g[3], "air");
                fill(g[0], 1, g[1], g[2], 1, g[3], chance(0.5) ? "cobblestone" : "air");
            }
        }

        private void fencePost(int x, int z, int pos) {
            if (pos % 6 == 0) {
                fill(x, 1, z, x, 3, z, brick);
                set(x, 4, z, "stone_brick_slab[type=bottom]");
            } else {
                set(x, 1, z, brick.next());
                set(x, 2, z, chance(0.85) ? "iron_bars" : "air");
            }
        }

        private void gate() {
            // budka parkingowa x 43-46 / z 5-8
            walls(43, 5, 46, 8, 1, 2, white);
            fill(43, 3, 5, 46, 3, 8, "smooth_stone_slab[type=bottom]");
            set(44, 2, 8, brokenGlass());
            set(45, 2, 8, brokenGlass());
            set(46, 2, 6, brokenGlass());
            fill(43, 1, 7, 43, 2, 7, "air");
            set(45, 1, 6, "spruce_slab[type=top]");
            // szlaban: słupek i opuszczone ramię
            fill(47, 1, 6, 47, 2, 6, "red_concrete");
            for (int x = 48; x <= 52; x++) {
                set(x, 1, 6, x % 2 == 0 ? "white_concrete" : "red_concrete");
            }
            sign(47, 1, 5, "spruce_wall_sign[facing=north]", "SZPITAL", "POWIATOWY", "WJAZD DLA", "KARETEK");
        }

        /** Osobówka 2 × 4 (osłona), wzdłuż z albo x. */
        private void sedan(int x, int z, boolean alongZ) {
            String body = car.next();
            for (int i = 0; i < 4; i++) {
                for (int w = 0; w < 2; w++) {
                    int px = alongZ ? x + w : x + i;
                    int pz = alongZ ? z + i : z + w;
                    set(px, 1, pz, body);
                    if (i == 1 || i == 2) {
                        set(px, 2, pz, i == 1 ? "glass_pane" : body);
                    }
                }
            }
        }

        /** Stojąca karetka 3 × 7 wzdłuż z: zderzak z0, kabina z0+1..z0+2, skrzynia z0+3..z0+6 z otwartym tyłem. */
        private void ambulance(int x, int z0) {
            fill(x, 1, z0, x + 2, 1, z0, "polished_blackstone");
            for (int z = z0 + 1; z <= z0 + 2; z++) {
                fill(x, 1, z, x + 2, 2, z, "white_concrete");
            }
            fill(x, 2, z0 + 1, x + 2, 2, z0 + 1, "light_blue_stained_glass");
            for (int z = z0 + 3; z <= z0 + 6; z++) {
                fill(x, 1, z, x + 2, 3, z, "white_concrete");
                set(x, 2, z, "red_concrete");
                set(x + 2, 2, z, "red_concrete");
            }
            fill(x + 1, 1, z0 + 3, x + 1, 2, z0 + 6, "air");
            set(x + 1, 1, z0 + 4, "white_carpet");
            set(x, 4, z0 + 3, "blue_stained_glass");
            set(x + 2, 4, z0 + 3, "blue_stained_glass");
        }

        private void parking() {
            int[][] cars = {{9, 12, 1}, {17, 12, 1}, {29, 13, 1}, {13, 21, 1}, {25, 22, 1}, {37, 21, 1}, {20, 27, 0}, {34, 28, 0}};
            for (int[] c : cars) {
                sedan(c[0], c[1], c[2] == 1);
            }
            // druga karetka na parkingu (tył do alejki)
            ambulance(40, 11);
            // budka parkingowa i kosze
            set(8, 1, 30, "green_terracotta");
            set(44, 1, 31, "green_terracotta");
        }

        // ------------------------------------------------------------------ główny blok

        private static final int[] PARTITIONS = {37, 43, 49, 55, 61, 67, 72};
        private static final int[] PARTITIONS_GROUND = {37, 43, 56, 61, 67, 72};

        private void mainBlock() {
            int x1 = 30;
            int x2 = 79;
            int z1 = 40;
            int z2 = 53;
            walls(x1, z1, x2, z2, 1, 24, white);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean inside = x > x1 && x < x2 && z > z1 && z < z2;
                    set(x, 0, z, inside ? lino.next() : "smooth_stone");
                    for (int k = 1; k <= 4; k++) {
                        set(x, 5 * k, z, inside ? lino.next() : "light_gray_concrete");
                    }
                    set(x, 25, z, chance(0.85) ? "smooth_stone" : "andesite");
                }
            }
            for (int k = 0; k <= 4; k++) {
                floorLayout(k);
            }
            stairCore(31, 41, 0, 3);
            stairCore(73, 41, 0, 2);
            // zawalony bieg między 3 a 4 piętrem
            set(33, 16, 44, rubble.next());
            set(34, 16, 44, rubble.next());
            set(34, 17, 44, rubble.next());
            set(35, 16, 43, rubble.next());
            liftShaft();
            // wejścia na parterze: szczyty i tył holu
            fill(x1, 1, 46, x1, 2, 47, "air");
            fill(x2, 1, 46, x2, 2, 47, "air");
            fill(48, 1, z2, 51, 3, z2, "air");
            // pomieszczenie techniczne nad klatką zachodnią z drabiną na dach
            for (int y = 21; y <= 25; y++) {
                set(31, y, 41, "ladder[facing=south,waterlogged=false]");
            }
            sign(53, 3, 54, "spruce_wall_sign[facing=south]", "", "SZPITAL", "POWIATOWY", "");
        }

        private void floorLayout(int k) {
            int h = 5 * k;
            int[] parts = k == 0 ? PARTITIONS_GROUND : PARTITIONS;
            // ściany korytarza
            for (int x = 31; x <= 78; x++) {
                if (k == 0 && x >= 44 && x <= 55) {
                    continue;
                }
                fill(x, h + 1, 45, x, h + 4, 45, white);
                fill(x, h + 1, 48, x, h + 4, 48, white);
            }
            for (int px : parts) {
                fill(px, h + 1, 41, px, h + 4, 44, white);
                fill(px, h + 1, 49, px, h + 4, 52, white);
            }
            if (k == 0) {
                // hol: przejścia z korytarzy
                fill(43, 1, 46, 43, 3, 47, "air");
                fill(56, 1, 46, 56, 3, 47, "air");
                fill(43, 1, 45, 43, 4, 48, "air");
                fill(56, 1, 45, 56, 4, 48, "air");
                fill(43, 1, 41, 43, 4, 44, white);
                fill(43, 1, 49, 43, 4, 52, white);
                fill(56, 1, 41, 56, 4, 44, white);
                fill(56, 1, 49, 56, 4, 52, white);
                // przejście do izby przyjęć
                fill(48, 1, 40, 51, 3, 40, "air");
            }
            // segmenty sal: [a+1, b-1]
            int prev = 30;
            int[] bounds = new int[parts.length + 1];
            System.arraycopy(parts, 0, bounds, 0, parts.length);
            bounds[parts.length] = 79;
            for (int b : bounds) {
                int a = prev;
                prev = b;
                int xa = a + 1;
                int xb = b - 1;
                boolean hall = k == 0 && xa == 44;
                if (hall) {
                    continue;
                }
                int mid = (xa + xb) / 2;
                boolean westStair = xa == 31 && k <= 3;
                boolean eastStair = xa == 73 && k <= 2;
                boolean shaft = xa == 50;
                // drzwi z korytarza
                if (westStair || eastStair) {
                    fill(xa, h + 1, 45, xa, h + 2, 45, "air");
                    fill(xb, h + 1, 45, xb, h + 2, 45, "air");
                } else if (!shaft) {
                    fill(mid - 1, h + 1, 45, mid - 1, h + 2, 45, "air");
                }
                fill(mid - 1, h + 1, 48, mid - 1, h + 2, 48, "air");
                // okna od frontu i od tyłu
                if (!shaft) {
                    for (int x = mid - 1; x <= mid + 1; x += 2) {
                        set(x, h + 2, 40, brokenGlass());
                        set(x, h + 3, 40, brokenGlass());
                    }
                }
                for (int x = mid - 1; x <= mid + 1; x += 2) {
                    set(x, h + 2, 53, brokenGlass());
                    set(x, h + 3, 53, brokenGlass());
                }
                // wyposażenie sal
                if (!westStair && !eastStair && !shaft && k >= 1 && k <= 3) {
                    String wool = k == 3 ? pick("light_blue_wool", "pink_wool", "yellow_wool", "lime_wool") : "white_wool";
                    bed(xa, h + 1, 41, 1, wool);
                    if (xb - xa >= 4) {
                        bed(xb, h + 1, 41, 1, chance(0.5) ? wool : "white_wool");
                    }
                }
                if (k >= 1 && k <= 3) {
                    String wool = k == 3 ? pick("light_blue_wool", "pink_wool", "yellow_wool", "lime_wool") : "white_wool";
                    bed(xa, h + 1, 52, -1, wool);
                    if (xb - xa >= 4) {
                        if (chance(0.6)) {
                            bed(xb, h + 1, 52, -1, wool);
                        } else {
                            // przewrócone łóżko na środku sali
                            set(xb - 1, h + 1, 50, "iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
                            set(xb, h + 1, 50, "white_wool");
                        }
                    }
                    if (chance(0.5)) {
                        set(xb, h + 1, 49, "iron_bars");
                    }
                }
                if (k == 0 && !westStair && !eastStair) {
                    // gabinety na parterze: kozetka i szafka
                    set(xa, 1, 41, "white_wool");
                    set(xb, 1, 52, "light_gray_terracotta");
                }
                if (k == 4 && !shaft) {
                    // biura: biurko, regał
                    fill(xa + 1, h + 1, 51, xa + 2, h + 1, 51, "dark_oak_slab[type=top]");
                    set(xb, h + 1, 52, "bookshelf");
                    set(xb, h + 2, 52, "bookshelf");
                    if (!westStair) {
                        fill(xa + 1, h + 1, 42, xa + 2, h + 1, 42, "dark_oak_slab[type=top]");
                    }
                }
            }
            // osłony na korytarzu: wózki, łóżka, ławki, na zmianę przy jednej ścianie
            for (int x = 34; x <= 76; x += 5) {
                if (k == 0 && x >= 43 && x <= 56) {
                    continue;
                }
                int z = (x / 5) % 2 == 0 ? 46 : 47;
                int wall = z == 46 ? 45 : 48;
                if (!isAir(x, h + 2, wall) || !isAir(x + 1, h + 2, wall)) {
                    // przy drzwiach nie stawiamy przeszkody
                }
                if (isAir(x, h + 1, wall) || isAir(x + 1, h + 1, wall) || isAir(x - 1, h + 1, wall)) {
                    continue;
                }
                double r = rnd.nextDouble();
                if (r < 0.4) {
                    set(x, h + 1, z, "white_wool");
                    set(x + 1, h + 1, z, "white_wool");
                } else if (r < 0.7) {
                    set(x, h + 1, z, "iron_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]");
                } else {
                    set(x, h + 1, z, stairs("spruce", z == 46 ? "south" : "north"));
                }
            }
            cobwebs(31, 41, 78, 52, h + 4, 14);
        }

        private String pick(String... options) {
            return options[rnd.nextInt(options.length)];
        }

        /** Szyb windy x 50-54 / z 41-45: kabina utknęła y 11-15, drzwi wyważone tylko na 3 i 4 piętrze. */
        private void liftShaft() {
            for (int y = 1; y <= 24; y++) {
                for (int x = 50; x <= 54; x++) {
                    for (int z = 41; z <= 45; z++) {
                        boolean wall = x == 50 || x == 54 || z == 41 || z == 45;
                        set(x, y, z, wall ? (z == 41 ? white.next() : "light_gray_concrete") : "air");
                    }
                }
            }
            for (int k = 1; k <= 4; k++) {
                fill(51, 5 * k, 42, 53, 5 * k, 44, "air");
            }
            for (int k = 0; k <= 2; k++) {
                fill(52, 5 * k + 1, 45, 53, 5 * k + 2, 45, "iron_block");
            }
            fill(52, 16, 45, 53, 17, 45, "air");
            fill(52, 21, 45, 53, 22, 45, "air");
            set(51, 18, 45, "iron_block");
            // kabina
            fill(51, 11, 42, 53, 15, 44, "iron_block");
            fill(52, 12, 43, 52, 14, 43, "air");
            set(52, 0, 43, "black_concrete");
            for (int y = 16; y <= 21; y++) {
                set(53, y, 44, "ladder[facing=west,waterlogged=false]");
            }
            fill(52, 16, 42, 52, 24, 42, "chain[axis=y]");
        }

        // ------------------------------------------------------------------ izba przyjęć

        private void emergency() {
            int x1 = 40;
            int x2 = 69;
            int z1 = 24;
            int z2 = 39;
            walls(x1, z1, x2, z2, 1, 4, white);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 5, z, chance(0.85) ? "smooth_stone" : "andesite");
                    if (x > x1 && x < x2 && z > z1) {
                        set(x, 0, z, lino.next());
                    }
                }
            }
            // ściana z39 zostaje tylko po bokach: izba przechodzi w hol głównego bloku
            fill(x1 + 1, 1, z2, x2 - 1, 4, z2, "air");
            fill(51, 1, z1, 56, 3, z1, "air");
            fill(x1, 1, 35, x1, 2, 36, "air");
            fill(x2, 1, 35, x2, 2, 36, "air");
            for (int x = 42; x <= 66; x += 4) {
                if (x < 50 || x > 57) {
                    set(x, 2, z1, brokenGlass());
                    set(x, 3, z1, brokenGlass());
                }
            }
            // gabinet zabiegowy x 41-48 / z 25-31 i pokój lekarza x 61-68 / z 25-31
            fill(49, 1, 25, 49, 4, 31, white);
            fill(41, 1, 32, 49, 4, 32, white);
            fill(60, 1, 25, 60, 4, 31, white);
            fill(60, 1, 32, 68, 4, 32, white);
            fill(49, 1, 28, 49, 2, 28, "air");
            fill(60, 1, 28, 60, 2, 28, "air");
            fill(43, 1, 26, 46, 1, 26, "white_terracotta");
            set(41, 1, 30, "white_wool");
            set(41, 1, 31, "white_wool");
            set(47, 2, 25, "lantern[hanging=false,waterlogged=false]");
            set(47, 1, 25, "light_gray_terracotta");
            set(62, 1, 26, "dark_oak_slab[type=top]");
            set(63, 1, 26, "dark_oak_slab[type=top]");
            set(66, 1, 30, "white_wool");
            set(66, 1, 31, "white_wool");
            // rejestracja: lada z okienkiem
            for (int x = 51; x <= 58; x++) {
                set(x, 1, 30, "quartz_slab[type=top]");
            }
            fill(54, 1, 30, 55, 1, 30, "air");
            set(51, 2, 30, "glass_pane");
            set(58, 2, 30, "glass_pane");
            // poczekalnia: rzędy krzesełek
            for (int z = 34; z <= 36; z += 2) {
                for (int x = 51; x <= 58; x++) {
                    if (x != 54 && x != 55) {
                        set(x, 1, z, stairs("spruce", "north"));
                    }
                }
            }
            // sala obserwacyjna z łóżkami za parawanami (po obu stronach)
            for (int z = 34; z <= 38; z += 2) {
                bed(42, 1, z, 0, "white_wool");
                set(43, 1, z, "white_stained_glass_pane");
                set(43, 2, z, "white_stained_glass_pane");
                set(67, 1, z, "white_wool");
                set(66, 1, z, "white_stained_glass_pane");
                set(66, 2, z, "white_stained_glass_pane");
            }
            cobwebs(41, 25, 68, 38, 4, 10);
        }

        /** Daszek podjazdu x 48-59 / z 16-23 (dach y 4) i karetka przed nim – wejście na dach izby przyjęć. */
        private void canopyAndAmbulance() {
            for (int x = 48; x <= 59; x++) {
                for (int z = 16; z <= 23; z++) {
                    set(x, 4, z, chance(0.85) ? "light_gray_concrete" : "iron_block");
                }
            }
            for (int[] p : new int[][]{{48, 16}, {59, 16}, {48, 20}, {59, 20}}) {
                fill(p[0], 1, p[1], p[0], 3, p[1], "quartz_pillar[axis=y]");
            }
            sign(53, 4, 15, "spruce_wall_sign[facing=north]", "IZBA", "PRZYJĘĆ", "SOR", "");
            sign(54, 4, 15, "spruce_wall_sign[facing=north]", "", "WJAZD", "KARETEK", "");
            ambulance(52, 8);
        }

        // ------------------------------------------------------------------ pawilon zabiegowy i łącznik

        private void pavilion() {
            int x1 = 84;
            int x2 = 103;
            int z1 = 34;
            int z2 = 53;
            walls(x1, z1, x2, z2, 1, 9, white);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean inside = x > x1 && x < x2 && z > z1 && z < z2;
                    set(x, 0, z, inside ? lino.next() : "smooth_stone");
                    set(x, 5, z, inside ? lino.next() : "light_gray_concrete");
                    set(x, 10, z, chance(0.85) ? "smooth_stone" : "andesite");
                    if (!inside) {
                        set(x, 11, z, "stone_brick_slab[type=top]");
                    }
                }
            }
            // ---- parter: apteka x 85-93 / z 35-43, przychodnia x 85-93 / z 45-52, korytarz x 95-102
            fill(94, 1, 35, 94, 4, 52, white);
            fill(85, 1, 44, 93, 4, 44, white);
            fill(95, 1, z1, 97, 3, z1, "air");
            fill(x2, 1, 47, x2, 2, 48, "air");
            fill(94, 1, 48, 94, 2, 48, "air");
            set(94, 2, 37, "iron_bars");
            set(95, 1, 37, "quartz_slab[type=top]");
            door(94, 1, 41, "iron_door", "east", "left", false);
            sign(95, 3, 40, "spruce_wall_sign[facing=east]", "", "APTEKA", "SZPITALNA", "");
            // regały apteki
            for (int x = 86; x <= 92; x += 2) {
                for (int y = 1; y <= 3; y++) {
                    set(x, y, 40, y == 2 ? "white_terracotta" : "birch_slab[type=top]");
                }
            }
            for (int x = 89; x <= 93; x++) {
                set(x, 1, 35, "birch_planks");
                set(x, 2, 35, chance(0.5) ? "flower_pot" : "air");
            }
            // przychodnia
            set(86, 1, 46, "white_wool");
            set(86, 1, 47, "white_wool");
            fill(89, 1, 51, 90, 1, 51, "dark_oak_slab[type=top]");
            // zasypana klatka schodowa pawilonu
            walls(97, 49, 102, 52, 1, 4, white);
            fill(98, 1, 50, 101, 4, 51, rubble);
            fill(97, 1, 49, 97, 2, 49, rubble);
            // okna parteru (wysoko, małe) i piętra
            for (int x = 86; x <= 101; x += 3) {
                set(x, 3, z2, brokenGlass());
                set(x, 7, z1, brokenGlass());
                set(x, 7, z2, brokenGlass());
            }
            for (int z = 36; z <= 51; z += 3) {
                set(x2, 3, z, brokenGlass());
                set(x2, 7, z, brokenGlass());
            }

            // ---- piętro: blok operacyjny (wejście tylko łącznikiem), korytarz z 45-48
            fill(85, 6, 44, 102, 9, 44, white);
            fill(85, 6, 49, 102, 9, 49, white);
            fill(89, 6, 35, 89, 9, 43, white);
            fill(95, 6, 35, 95, 9, 43, white);
            fill(96, 6, 50, 96, 9, 52, white);
            fill(x1, 6, 46, x1, 7, 47, "air");
            fill(87, 6, 44, 87, 7, 44, "air");
            fill(92, 6, 44, 92, 7, 44, "air");
            fill(99, 6, 44, 99, 7, 44, "air");
            fill(90, 6, 49, 90, 7, 49, "air");
            fill(99, 6, 49, 99, 7, 49, "air");
            // śluza z umywalkami
            for (int x = 86; x <= 90; x += 2) {
                set(x, 6, 45, "cauldron");
                set(x, 7, 45, "lightning_rod[facing=down]");
            }
            // sale operacyjne: stół i lampa
            for (int sx : new int[]{92, 98}) {
                fill(sx, 6, 39, sx + 1, 6, 40, "smooth_quartz");
                set(sx, 9, 39, "sea_lantern");
                set(sx + 1, 9, 40, "chain[axis=y]");
                set(sx - 1, 6, 37, "iron_bars");
                set(sx + 2, 6, 42, "lightning_rod[facing=up]");
            }
            // sala pooperacyjna
            for (int x = 86; x <= 94; x += 3) {
                bed(x, 6, 52, -1, "white_wool");
            }
            // okno ewakuacyjne na wschodzie: tylko wyjście (skok na ziemię)
            fill(x2, 6, 50, x2, 7, 50, "air");
            cobwebs(85, 35, 102, 52, 9, 12);

            // ---- kanał wentylacyjny z dachu do zaplecza apteki (88, *, 38)
            fill(87, 6, 37, 89, 9, 39, "iron_block");
            fill(88, 1, 37, 88, 9, 37, "iron_block");
            for (int y = 1; y <= 10; y++) {
                set(88, y, 38, "ladder[facing=south,waterlogged=false]");
            }
            set(87, 11, 38, "iron_bars");
            set(89, 11, 38, "iron_bars");
            // świetliki i klimatyzatory na dachu
            fill(95, 10, 40, 98, 10, 42, "glass");
            fill(92, 11, 48, 93, 11, 49, "iron_block");

            // ---- trasa na dach: kosz, przewrócona karetka, daszek wejścia, klimatyzatory
            for (int x = 90; x <= 96; x++) {
                for (int z = 30; z <= 33; z++) {
                    set(x, 4, z, chance(0.85) ? "light_gray_concrete" : "iron_block");
                }
            }
            fill(90, 1, 30, 90, 3, 30, "quartz_pillar[axis=y]");
            fill(96, 1, 30, 96, 3, 30, "quartz_pillar[axis=y]");
            // przewrócona karetka na boku: skrzynia x 86-88 / z 27-30 (y 1-3), kabina z 31-32 (y 1-2)
            for (int z = 27; z <= 30; z++) {
                fill(86, 1, z, 88, 3, z, "white_concrete");
                set(87, 3, z, "red_concrete");
                set(87, 1, z, "red_concrete");
            }
            fill(86, 1, 31, 88, 2, 32, "white_concrete");
            set(86, 2, 32, "light_blue_stained_glass");
            set(85, 1, 32, "green_terracotta");
            set(89, 1, 27, "blue_stained_glass");
            int[][] ac = {{98, 5, 33}, {100, 6, 33}, {102, 7, 33}, {104, 8, 33}, {104, 9, 35}, {104, 10, 37}};
            for (int[] a : ac) {
                set(a[0], a[1], a[2], "iron_block");
                set(a[0], a[1] - 1, a[2], "iron_bars");
            }
        }

        /** Przeszklony łącznik na 1 piętrze między głównym blokiem (x 79) a pawilonem (x 84). */
        private void link() {
            for (int x = 79; x <= 84; x++) {
                for (int z = 45; z <= 48; z++) {
                    if (x >= 80 && x <= 83) {
                        set(x, 5, z, "light_gray_concrete");
                        set(x, 9, z, "smooth_stone");
                    }
                }
            }
            for (int x = 80; x <= 83; x++) {
                for (int y = 6; y <= 8; y++) {
                    set(x, y, 45, y == 8 ? "white_concrete" : brokenGlass());
                    set(x, y, 48, y == 8 ? "white_concrete" : brokenGlass());
                }
            }
            fill(79, 6, 46, 79, 7, 47, "air");
            fill(80, 1, 45, 80, 4, 45, "quartz_pillar[axis=y]");
            fill(83, 1, 48, 83, 4, 48, "quartz_pillar[axis=y]");
        }

        // ------------------------------------------------------------------ rusztowanie i dach

        /** Rusztowanie remontowe przy zachodnim szczycie: pomosty co 4 bloki, drabiny na zmianę, dziury w deskach. */
        private void scaffold() {
            int[] decks = {4, 8, 12, 16, 20, 24};
            for (int z : new int[]{42, 46, 51}) {
                fill(26, 1, z, 26, 25, z, "spruce_log[axis=y]");
            }
            for (int d = 0; d < decks.length; d++) {
                int y = decks[d];
                for (int x = 26; x <= 29; x++) {
                    for (int z = 42; z <= 51; z++) {
                        if (x == 26 && (z == 42 || z == 46 || z == 51)) {
                            continue;
                        }
                        set(x, y, z, "spruce_planks");
                    }
                }
                for (int z = 43; z <= 50; z++) {
                    if (z != 46) {
                        set(26, y + 1, z, "spruce_fence");
                    }
                }
            }
            int[][] holes = {{8, 46, 47}, {12, 45, 46}, {16, 47, 48}, {20, 45, 45}, {20, 48, 48}};
            for (int[] hl : holes) {
                fill(27, hl[0], hl[1], 29, hl[0], hl[2], "air");
                for (int z = hl[1]; z <= hl[2]; z++) {
                    set(26, hl[0], z, "air");
                    set(26, hl[0] + 1, z, "air");
                }
            }
            for (int d = 0; d < decks.length; d++) {
                int top = decks[d];
                int z = d % 2 == 0 ? 42 : 51;
                for (int y = top - 3; y <= top; y++) {
                    set(29, y, z, "ladder[facing=west,waterlogged=false]");
                }
            }
            // worki z gruzem i wiadra na pomostach
            set(28, 5, 49, "brown_wool");
            set(27, 13, 50, "cauldron");
            set(28, 21, 43, "brown_wool");
        }

        private void roof() {
            // attyka (bez zachodniej krawędzi przy rusztowaniu)
            for (int x = 30; x <= 79; x++) {
                set(x, 26, 40, "stone_brick_slab[type=bottom]");
                set(x, 26, 53, "stone_brick_slab[type=bottom]");
            }
            for (int z = 40; z <= 53; z++) {
                set(79, 26, z, "stone_brick_slab[type=bottom]");
                if (z <= 41 || z >= 52) {
                    set(30, 26, z, "stone_brick_slab[type=bottom]");
                }
            }
            // maszynownia windy nad szybem
            for (int y = 26; y <= 28; y++) {
                for (int x = 50; x <= 54; x++) {
                    for (int z = 41; z <= 45; z++) {
                        set(x, y, z, x == 50 || x == 54 || z == 41 || z == 45 ? "light_gray_concrete" : "air");
                    }
                }
            }
            fill(50, 29, 41, 54, 29, 45, "smooth_stone");
            set(52, 30, 43, "lightning_rod[facing=up]");
            // lądowisko: okrąg r 5 wokół (66, 46.5) z literą H
            for (int x = 60; x <= 72; x++) {
                for (int z = 41; z <= 52; z++) {
                    double d = Math.hypot(x - 66, z - 46.5);
                    if (d <= 5.6) {
                        set(x, 25, z, d >= 4.6 ? "yellow_concrete" : "light_gray_concrete");
                    }
                }
            }
            for (int z = 44; z <= 49; z++) {
                set(64, 25, z, "white_concrete");
                set(68, 25, z, "white_concrete");
            }
            for (int x = 65; x <= 67; x++) {
                set(x, 25, 46, "white_concrete");
                set(x, 25, 47, "white_concrete");
            }
            // przewrócona skrzynia transportowa i wywietrzniki (osłony)
            fill(69, 26, 49, 70, 26, 50, "spruce_planks");
            set(70, 27, 50, "spruce_slab[type=bottom]");
            for (int[] v : new int[][]{{36, 44}, {42, 50}, {58, 49}, {75, 43}, {45, 42}}) {
                set(v[0], 26, v[1], "iron_block");
                set(v[0], 27, v[1], "iron_bars");
            }
            set(31, 26, 41, "air");
        }

        // ------------------------------------------------------------------ tył: budynek gospodarczy, kaplica, park

        private void serviceBuilding() {
            int x1 = 14;
            int x2 = 43;
            int z1 = 62;
            int z2 = 77;
            walls(x1, z1, x2, z2, 1, 5, brick);
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean inside = x > x1 && x < x2 && z > z1 && z < z2;
                    set(x, 6, z, chance(0.85) ? "smooth_stone" : "andesite");
                    if (inside) {
                        set(x, 0, z, chance(0.8) ? "smooth_stone" : "cracked_stone_bricks");
                    }
                }
            }
            fill(26, 1, z1 + 1, 26, 5, z2 - 1, brick);
            fill(34, 1, z1 + 1, 34, 5, z2 - 1, brick);
            fill(26, 1, 69, 26, 2, 70, "air");
            fill(34, 1, 69, 34, 2, 70, "air");
            for (int x : new int[]{19, 30, 38}) {
                fill(x, 1, z1, x + 1, 3, z1, "air");
            }
            fill(x1, 1, 69, x1, 2, 70, "air");
            fill(x2, 1, 73, x2, 2, 74, "air");
            for (int x = 16; x <= 41; x += 3) {
                set(x, 3, z1, brokenGlass());
                set(x, 3, z2, brokenGlass());
            }
            // kuchnia: kotły warzelne i stoły
            for (int x = 16; x <= 22; x += 3) {
                set(x, 1, 66, "cauldron");
                set(x, 1, 67, "iron_block");
            }
            fill(16, 1, 71, 23, 1, 71, "smooth_stone_slab[type=top]");
            set(18, 2, 71, "smoker[facing=south,lit=false]");
            // pralnia: bębny pralek
            for (int x = 28; x <= 32; x += 2) {
                set(x, 1, 64, "iron_block");
                set(x, 2, 64, "observer[facing=south,powered=false]");
            }
            fill(28, 1, 70, 32, 1, 71, "white_wool");
            // kotłownia: kocioł i komin
            fill(37, 1, 70, 41, 3, 75, "iron_block");
            set(39, 2, 69, "furnace[facing=north,lit=false]");
            fill(44, 1, 70, 45, 16, 71, brick);
            set(44, 17, 70, "lightning_rod[facing=up]");
            cobwebs(15, 63, 42, 76, 5, 10);
        }

        private void chapel() {
            int x1 = 86;
            int x2 = 95;
            int z1 = 70;
            int z2 = 81;
            walls(x1, z1, x2, z2, 1, 4, white);
            for (int x = x1 + 1; x < x2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    set(x, 0, z, "spruce_planks");
                }
            }
            for (int z = z1; z <= z2; z++) {
                for (int i = 0; i <= 4; i++) {
                    set(x1 + i, 5 + i, z, "dark_oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
                    set(x2 - i, 5 + i, z, "dark_oak_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
                }
            }
            for (int z : new int[]{z1, z2}) {
                for (int i = 1; i <= 3; i++) {
                    fill(x1 + i, 5, z, x2 - i, 4 + i, z, white);
                }
            }
            fill(90, 1, z1, 91, 3, z1, "air");
            fill(90, 10, z1, 90, 12, z1, "dark_oak_fence");
            set(89, 11, z1, "dark_oak_fence");
            set(91, 11, z1, "dark_oak_fence");
            for (int z = 73; z <= 78; z += 2) {
                for (int x = 87; x <= 94; x++) {
                    if (x != 90 && x != 91) {
                        set(x, 1, z, stairs("spruce", "south"));
                    }
                }
            }
            set(90, 1, 80, "quartz_slab[type=top]");
            set(91, 1, 80, "quartz_slab[type=top]");
            set(90, 2, 80, "candle[candles=3,lit=false,waterlogged=false]");
            windowsZ(x1, 72, 79, 3, 2, 3);
            windowsZ(x2, 72, 79, 3, 2, 3);
        }

        private void windowsZ(int x, int z1, int z2, int step, int... ys) {
            for (int z = z1; z <= z2; z += step) {
                for (int y : ys) {
                    set(x, y, z, brokenGlass());
                }
            }
        }

        private void park() {
            int[][] trees = {{8, 50}, {14, 56}, {6, 84}, {14, 84}, {24, 83}, {46, 60}, {58, 58}, {60, 70}, {46, 74},
                    {66, 84}, {74, 76}, {100, 60}, {104, 74}, {104, 84}, {8, 40}, {16, 44}, {92, 9}, {100, 12}};
            for (int[] t : trees) {
                tree(t[0], t[1]);
            }
            // altana x 96-101 / z 84-87? (w parku za kaplicą)
            int ax = 76;
            int az = 82;
            for (int[] p : new int[][]{{ax, az}, {ax + 4, az}, {ax, az + 4}, {ax + 4, az + 4}}) {
                fill(p[0], 1, p[1], p[0], 3, p[1], "spruce_fence");
            }
            for (int x = ax - 1; x <= ax + 5; x++) {
                for (int z = az - 1; z <= az + 5; z++) {
                    set(x, 4, z, "spruce_slab[type=bottom]");
                }
            }
            for (int x = ax + 1; x <= ax + 3; x++) {
                set(x, 1, az + 2, "spruce_slab[type=bottom]");
            }
            // ławki przy ścieżkach
            for (int z : new int[]{58, 66, 74}) {
                set(54, 1, z, stairs("spruce", "east"));
                set(54, 1, z + 1, stairs("spruce", "east"));
            }
            for (int x : new int[]{36, 62, 96}) {
                set(x, 1, 79, stairs("spruce", "south"));
                set(x + 1, 1, 79, stairs("spruce", "south"));
            }
            // wózek inwalidzki i kontenery na odpady
            set(56, 1, 56, "iron_trapdoor[facing=east,half=bottom,open=true,powered=false,waterlogged=false]");
            fill(46, 1, 66, 47, 2, 67, "green_terracotta");
            fill(98, 1, 64, 99, 2, 64, "yellow_terracotta");
        }

        private void tree(int x, int z) {
            int trunk = 5 + rnd.nextInt(2);
            for (int y = 1; y <= trunk; y++) {
                set(x, y, z, "oak_log[axis=y]");
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = trunk - 1; y <= trunk + 1; y++) {
                        int r = y > trunk ? 1 : 2;
                        if (Math.abs(dx) <= r && Math.abs(dz) <= r && !(Math.abs(dx) == 2 && Math.abs(dz) == 2)
                                && isAir(x + dx, y, z + dz)) {
                            set(x + dx, y, z + dz, "oak_leaves[distance=1,persistent=true,waterlogged=false]");
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ piwnice

        private void basement() {
            // korytarz pod głównym blokiem
            room(31, B, 46, 78, B + 2, 47);
            for (int x = 34; x <= 76; x += 6) {
                set(x, B + 2, 46, "chain[axis=x]");
                set(x + 1, B + 2, 46, "chain[axis=x]");
            }
            for (int x = 36; x <= 76; x += 10) {
                set(x, B + 2, 47, "lantern[hanging=true,waterlogged=false]");
            }
            // zejście z parteru klatki zachodniej (rząd z 44)
            room(31, B, 41, 36, B + 3, 44);
            for (int i = 0; i < 4; i++) {
                int x = 35 - i;
                int y = -1 - i;
                set(x, y, 44, stairs("stone_brick", "east"));
                fill(x, y + 1, 44, x, 0, 44, "air");
                for (int s = B - 1; s < y; s++) {
                    set(x, s, 44, "stone_bricks");
                }
            }
            fill(31, B, 45, 31, B + 1, 45, "air");
            // archiwum, magazyn pościeli, rozdzielnia, kostnica
            room(44, B, 49, 55, B + 2, 52);
            fill(50, B, 48, 50, B + 1, 48, "air");
            for (int x = 45; x <= 54; x += 3) {
                fill(x, B, 50, x, B + 2, 50, "bookshelf");
                fill(x, B, 51, x, B + 1, 51, "brown_wool");
            }
            room(57, B, 49, 64, B + 2, 52);
            fill(60, B, 48, 60, B + 1, 48, "air");
            fill(58, B, 50, 59, B + 1, 50, "white_wool");
            fill(62, B, 51, 63, B, 51, "white_wool");
            room(66, B, 41, 72, B + 2, 44);
            fill(69, B, 45, 69, B + 1, 45, "air");
            for (int x = 67; x <= 71; x += 2) {
                fill(x, B, 41, x, B + 1, 41, "light_gray_terracotta");
                set(x, B + 1, 42, "lever[face=wall,facing=south,powered=false]");
            }
            // kostnica x 66-78 / z 49-52: szuflady chłodnicze w ścianie i stoły sekcyjne
            room(66, B, 49, 78, B + 2, 52);
            fill(70, B, 48, 70, B + 1, 48, "air");
            for (int x = 67; x <= 77; x += 2) {
                set(x, B + 1, 52, "iron_block");
                set(x, B, 52, "iron_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]");
            }
            fill(69, B, 50, 70, B, 50, "smooth_quartz");
            fill(74, B, 50, 75, B, 50, "smooth_quartz");
            set(72, B + 2, 50, "lantern[hanging=true,waterlogged=false]");
            set(70, B + 1, 50, "white_carpet");

            // zjazd dla karawanów od tyłu do kostnicy x 70-73
            room(70, B, 54, 73, B + 2, 58);
            fill(71, B, 53, 72, B + 1, 53, "air");
            for (int i = 0; i < 4; i++) {
                int z = 59 + i;
                int y = B + i;
                room(70, y + 1, z, 73, 0, z);
                fill(70, y + 1, z, 73, 3, z, "air");
                fill(70, y, z, 73, y, z, stairs("stone_brick", "south"));
                for (int s = B - 1; s < y; s++) {
                    fill(70, s, z, 73, s, z, "stone_bricks");
                }
            }
            for (int z = 59; z <= 63; z++) {
                fill(69, B, z, 69, 1, z, "light_gray_concrete");
                fill(74, B, z, 74, 1, z, "light_gray_concrete");
            }
            fill(69, 1, 54, 74, 1, 58, "light_gray_concrete");
            fill(70, 1, 55, 73, 1, 57, "grass_block");

            // tunel do budynku gospodarczego x 38-39, zalany odcinek z 54-57
            room(38, B, 48, 39, B + 2, 63);
            room(38, B - 1, 54, 39, B + 2, 57);
            fill(38, B - 1, 54, 39, B - 1, 57, "water");
            for (int z = 50; z <= 62; z += 4) {
                set(38, B + 2, z, "chain[axis=z]");
            }
            fill(38, B, 48, 39, B + 1, 48, "air");
            // piwnica budynku gospodarczego i schody do kotłowni (rząd z 66)
            room(34, B, 64, 41, B + 2, 69);
            for (int i = 0; i < 4; i++) {
                int x = 36 + i;
                int y = B + i;
                set(x, y, 66, stairs("stone_brick", "east"));
                fill(x, y + 1, 66, x, 3, 66, "air");
            }
            fill(37, 1, 66, 39, 3, 66, "air");
            set(35, B, 68, "iron_block");
            set(35, B + 1, 68, "iron_block");
        }

        // ------------------------------------------------------------------ skrzynie

        private void crates() {
            // brama, parking, karetki
            crate(45, 1, 7, "north", "plac");
            crate(22, 1, 17, "east", "plac");
            crate(43, 1, 27, "north", "plac");
            crate(53, 1, 12, "south", "izba-chorych");
            crate(41, 1, 15, "south", "izba-chorych");
            // izba przyjęć
            crate(42, 1, 26, "south", "izba-chorych");
            crate(48, 1, 30, "west", "izba-chorych");
            crate(56, 1, 29, "north", "parter");
            crate(67, 1, 26, "south", "parter");
            crate(67, 1, 37, "west", "izba-chorych");
            // główny blok: parter
            crate(40, 1, 41, "south", "izba-chorych");
            crate(40, 1, 52, "north", "parter");
            crate(58, 1, 41, "south", "parter");
            crate(64, 1, 52, "north", "izba-chorych");
            crate(33, 1, 52, "north", "parter");
            // 1 piętro – interna
            crate(46, 6, 41, "south", "izba-chorych");
            crate(33, 6, 52, "north", "izba-chorych");
            crate(52, 6, 52, "north", "parter");
            crate(64, 6, 41, "south", "izba-chorych");
            // 2 piętro – chirurgia
            crate(40, 11, 52, "north", "izba-chorych");
            crate(58, 11, 41, "south", "parter");
            crate(69, 11, 52, "north", "izba-chorych");
            crate(46, 11, 41, "south", "parter");
            // 3 piętro – pediatria
            crate(46, 16, 52, "north", "parter");
            crate(64, 16, 41, "south", "izba-chorych");
            crate(75, 16, 52, "north", "parter");
            // 4 piętro – dyrekcja (szyb windy albo właz z dachu)
            crate(58, 21, 52, "north", "dyrekcja");
            crate(58, 21, 41, "south", "parter");
            crate(64, 21, 52, "north", "parter");
            // dach – lądowisko
            crate(66, 26, 50, "north", "ladowisko");
            // pawilon: apteka (kanał wentylacyjny z dachu), przychodnia, blok operacyjny (łącznik)
            crate(86, 1, 35, "south", "apteka");
            crate(92, 1, 36, "south", "izba-chorych");
            crate(86, 1, 43, "north", "izba-chorych");
            crate(92, 1, 52, "north", "izba-chorych");
            crate(93, 6, 35, "south", "blok-operacyjny");
            crate(101, 6, 35, "south", "blok-operacyjny");
            crate(86, 6, 43, "north", "blok-operacyjny");
            crate(95, 6, 52, "north", "izba-chorych");
            // tył
            crate(17, 1, 64, "south", "lada");
            crate(24, 1, 75, "north", "lada");
            crate(31, 1, 75, "north", "parter");
            crate(42, 1, 64, "west", "parter");
            crate(46, 1, 72, "east", "plac");
            crate(76, 1, 65, "north", "plac");
            crate(93, 1, 72, "south", "parter");
            // piwnice
            crate(46, B, 52, "north", "parter");
            crate(63, B, 49, "south", "izba-chorych");
            crate(77, B, 50, "west", "kostnica");
            crate(71, B, 43, "south", "parter");
            crate(40, B, 69, "north", "parter");
        }

        private void weeds() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 5; z < DEPTH; z++) {
                    String below = get(x, 0, z);
                    if (!below.equals("minecraft:grass_block") && !below.equals("minecraft:podzol")
                            && !below.equals("minecraft:coarse_dirt")) {
                        continue;
                    }
                    if (!isAir(x, 1, z) || !isAir(x, 2, z) || isCrate(x, 1, z) || !chance(0.16)) {
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
