package pl.axon.structure.generator;

import pl.axon.structure.api.Entrance;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;
import pl.axon.structure.core.Traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Opuszczona Galeria Kometa – trzypiętrowe centrum handlowe z garażem podziemnym, parkingiem od frontu
 * i rampami rozładunkowymi z tyłu. Galerię zajął gang: dużo strażników ({@code npc-bandyta}), dwóch hersztów
 * ({@code npc-herszt}) i dobry loot (jubiler, kantor, militaria, elektronika, liczarnia w garażu).
 * <p>
 * Struktura ma {@code grunt: 9}. W kodzie wysokości liczone są od gruntu (y = 0 to posadzka parteru,
 * podłoga garażu leży na y -8, stopy na -7).
 * <pre>
 *  z 0-37    parking: wjazd od drogi x 64-75, pylon z logo, przystanek, latarnie, rampa do garażu x 118-123
 *  z 40-113  budynek x 8-131: parter (posadzka y 0), 1 piętro (y 6), 2 piętro (y 12), dach (y 18)
 *            pasaż z 68-85 przez całą długość, atrium x 39-100 / z 72-81 otwarte do świetlika w dachu,
 *            fontanna, drzewo w donicy, ruchome schody, kładka na 2 piętrze, panoramiczna winda,
 *            rząd sklepów od północy (witryny z 67, zaplecza z 45-49, korytarz techniczny z 41-43)
 *            i od południa (witryny z 86, zaplecza z 104-108, korytarz z 110-112),
 *            hol wejściowy x 64-75 na dwie kondygnacje, klatki schodowe x 31-38 (płn.) i x 102-109 (płd.),
 *            zachód x 9-29: Maximarket / Dom Mody / kręgielnia, wschód x 112-130: Sport-Myśliwy / Elektro-Max / kino
 *  z 114-119 droga serwisowa: rampy, naczepa, kontenery; wjazd od wschodu
 *  pod ziemią: garaż x 10-129 / z 41-112, zalany narożnik, liczarnia za kratą (tylko włazem z biura Maximarketu)
 *  dach:      świetlik, maszynownia windy, wentylatory, billboard z napisem KOMETA, miejsce zrzutu
 * </pre>
 */
public final class MallStructure implements StructureDefinition {

    private static final int WIDTH = 140;
    private static final int DEPTH = 120;
    /** Ile warstw leży pod ziemią. */
    private static final int G = 9;
    private static final int HEIGHT = G + 31;
    /** Poziomy posadzek: parter, 1 i 2 piętro. */
    private static final int[] FLOOR = {0, 6, 12};
    private static final int ROOF = 18;
    /** Podłoga garażu. */
    private static final int GB = -8;

    // obrys budynku
    private static final int X1 = 8;
    private static final int X2 = 131;
    private static final int Z1 = 40;
    private static final int Z2 = 113;
    // pasaż
    private static final int CX1 = 31;
    private static final int CX2 = 110;
    private static final int CZ1 = 68;
    private static final int CZ2 = 85;
    // atrium (dziura w stropach 1 i 2 piętra)
    private static final int VX1 = 39;
    private static final int VX2 = 100;
    private static final int VZ1 = 72;
    private static final int VZ2 = 81;
    // kładka na 2 piętrze i szyb windy
    private static final int BRX1 = 76;
    private static final int BRX2 = 83;
    private static final int EX1 = 94;
    private static final int EZ1 = 77;

    @Override
    public String id() {
        return "galeria";
    }

    @Override
    public String displayName() {
        return "Galeria Kometa";
    }

    @Override
    public String description() {
        return "Opuszczone centrum handlowe zajęte przez gang: trzy piętra sklepów, hipermarket, kino, kręgielnia, garaż podziemny z liczarnią i zrzuty na dachu";
    }

    @Override
    public Blueprint build(Random random) {
        Traits d = Traits.defaults();
        Traits traits = new Traits(List.of(new Entrance(69, 0, "north"), new Entrance(WIDTH - 1, 116, "east")),
                d.mapAllowed(), d.sizeClass(), Set.of("rownina"), d.climates(), false, d.tags());
        return new Builder(random).build().traits(traits);
    }

    private enum Kind {
        PHARMACY, MONEY, BAKERY, DRUGSTORE, PRESS, PHOTO, JEWEL, SHOES, OPTICIAN, PHONES, LAUNDRY,
        CLOTHES, TOYS, BOOKS, COMPUTERS, VIDEO, MUSIC, BAGS, TRAVEL,
        FOOD, ARCADE, OFFICE, CAFE, SOLARIUM, GYM, HAIR, LOST_FOUND
    }

    /** Lokal w rzędzie sklepów: ściany x1, x2 (wspólne z sąsiadami), wnętrze x1+1 .. x2-1. */
    private record Unit(int x1, int x2, int f, boolean north) {

        int a() {
            return x1 + 1;
        }

        int b() {
            return x2 - 1;
        }

        int mid() {
            return (x1 + x2) / 2;
        }

        /** Rząd wnętrza: d = 0 przy witrynie, 15 przy ścianie zaplecza. */
        int z(int d) {
            return north ? 66 - d : 87 + d;
        }

        int front() {
            return north ? 67 : 86;
        }

        int back() {
            return north ? 50 : 103;
        }

        /** Rząd zaplecza: d = 0 przy sklepie, 4 przy korytarzu. */
        int br(int d) {
            return north ? 49 - d : 104 + d;
        }

        int corridorWall() {
            return north ? 44 : 109;
        }

        /** Kratka pasażu tuż przed witryną. */
        int outside() {
            return north ? 68 : 85;
        }

        /** Kierunek w stronę pasażu. */
        String out() {
            return north ? "south" : "north";
        }

        /** Kierunek w głąb sklepu. */
        String in() {
            return north ? "north" : "south";
        }

        int zMin() {
            return Math.min(z(0), z(15));
        }

        int zMax() {
            return Math.max(z(0), z(15));
        }
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);
        private final Furniture f;

        private final Palette grass;
        private final Palette asphalt;
        private final Palette rock;
        private final Palette facade;
        private final Palette wall;
        private final Palette tile;
        private final Palette roof;
        private final Palette garageFloor;
        private final Palette garageWall;
        private final Palette car;
        private final Palette rubble;
        private final List<int[]> skylightHoles = new ArrayList<>();

        Builder(Random rnd) {
            this.rnd = rnd;
            this.f = new Furniture(bp, G, rnd);
            grass = new Palette(rnd)
                    .add("grass_block", 12).add("coarse_dirt", 2).add("dirt", 1).add("podzol", 1);
            asphalt = new Palette(rnd)
                    .add("gray_concrete", 10).add("andesite", 2).add("cobblestone", 1).add("tuff", 1);
            rock = new Palette(rnd)
                    .add("stone", 40).add("andesite", 1).add("tuff", 1);
            facade = new Palette(rnd)
                    .add("light_gray_concrete", 14).add("light_gray_terracotta", 1).add("stone", 1)
                    .add("cracked_stone_bricks", 1);
            wall = new Palette(rnd)
                    .add("white_concrete", 10).add("white_terracotta", 2).add("light_gray_concrete", 1).add("calcite", 1);
            tile = new Palette(rnd)
                    .add("polished_diorite", 8).add("calcite", 3).add("diorite", 1).add("polished_andesite", 1)
                    .add("cracked_stone_bricks", 1);
            roof = new Palette(rnd)
                    .add("gray_concrete", 6).add("light_gray_concrete", 3).add("andesite", 2).add("tuff", 1)
                    .add("cobblestone", 1);
            garageFloor = new Palette(rnd)
                    .add("gray_concrete", 8).add("andesite", 2).add("cobblestone", 1).add("tuff", 1);
            garageWall = new Palette(rnd)
                    .add("light_gray_concrete", 6).add("stone_bricks", 2).add("andesite", 1);
            car = new Palette(rnd)
                    .add("light_blue_terracotta", 1).add("red_terracotta", 1).add("brown_terracotta", 1)
                    .add("white_terracotta", 1).add("green_terracotta", 1).add("blue_terracotta", 1)
                    .add("black_terracotta", 1);
            rubble = new Palette(rnd)
                    .add("cobblestone", 4).add("tuff", 2).add("cracked_stone_bricks", 2).add("andesite", 1)
                    .add("gravel", 1);
        }

        Blueprint build() {
            bp.ground(G);
            terrain();
            parkingLot();
            pylon();
            busStop();
            garage();
            garageRamp();
            shell();
            anchorWalls();
            cores();
            toilets();
            shops();
            hall();
            hypermarket();
            fashion();
            bowling();
            sport();
            electro();
            cinema();
            atrium();
            corridors();
            roofTop();
            docks();
            crates();
            guards();
            stories();
            lifeSigns();
            overgrowth();
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

        /** Punkt w bloku lady / maszyny: znacznik, a w nim widoczny blok (stanowiska GUI). */
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

        private boolean isMarker(int x, int y, int z) {
            Blueprint.Pos pos = new Blueprint.Pos(x, y + G, z);
            return bp.spawnPoints().stream().anyMatch(p -> p.pos().equals(pos));
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        private String pick(String... states) {
            return states[rnd.nextInt(states.length)];
        }

        private String brokenGlass() {
            double roll = rnd.nextDouble();
            if (roll < 0.45) {
                return "air";
            }
            if (roll < 0.5) {
                return "cobweb";
            }
            return chance(0.25) ? "light_blue_stained_glass_pane" : "glass_pane";
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

        private static String trapdoor(String material, String facing, String half, boolean open) {
            return material + "_trapdoor[facing=" + facing + ",half=" + half + ",open=" + open
                    + ",powered=false,waterlogged=false]";
        }

        private static String wallBanner(String color, String facing) {
            return color + "_wall_banner[facing=" + facing + "]";
        }

        private static String banner(String color, int rotation) {
            return color + "_banner[rotation=" + rotation + "]";
        }

        private static String ladder(String facing) {
            return "ladder[facing=" + facing + ",waterlogged=false]";
        }

        // meble z paczki zasobów (meble.bloki w configu): blok vanilla, który plugin podmienia na model

        /** Kasa fiskalna – stawiana na ladzie; przód (klawiatura) w stronę kasjera. */
        private static String kasa(String facing) {
            return "ender_chest[facing=" + facing + ",waterlogged=false]";
        }

        /** Lada sklepowa z szybą i towarem; przód w stronę klienta. */
        private static String lada(String facing) {
            return "jack_o_lantern[facing=" + facing + "]";
        }

        /** Toaleta; zbiornik z tyłu, przód w stronę wejścia do kabiny. */
        private static String kibel(String facing) {
            return "end_portal_frame[eye=false,facing=" + facing + "]";
        }

        /** Regał z puszkami, słoikami i kartonami. */
        private static String regal(String facing) {
            return "bee_nest[facing=" + facing + ",honey_level=0]";
        }

        private static String rozdzielnia(String facing) {
            return "sticky_piston[extended=false,facing=" + facing + "]";
        }

        private static String frytkownica(String facing) {
            return "vault[facing=" + facing + ",ominous=false,vault_state=inactive]";
        }

        private static final String KANISTRY = "respawn_anchor[charges=0]";
        private static final String WOZEK = "lodestone";

        /** Przeszklona lodówka z napojami – dwie kratki, nad nią bariera. */
        private void lodowka(int x, int y, int z, String facing) {
            set(x, y, z, "calibrated_sculk_sensor[facing=" + facing + ",power=0,sculk_sensor_phase=inactive,waterlogged=false]");
            set(x, y + 1, z, "barrier");
        }

        /** Pusta kratka z pustym miejscem nad nią, na pełnym bloku, bez znacznika. */
        private boolean freeFloor(int x, int y, int z) {
            int ay = y + G;
            return bp.inBounds(x, ay, z) && bp.isAir(x, ay, z) && bp.isAir(x, ay + 1, z)
                    && Weathering.isFullBlock(bp.get(x, ay - 1, z)) && !isMarker(x, y, z) && !isMarker(x, y - 1, z)
                    && !isMarker(x, y + 1, z);
        }

        private boolean nextToWall(int x, int y, int z) {
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] o : d) {
                String s = get(x + o[0], y, z + o[1]);
                if (Weathering.isFullBlock(s) || s.contains("bee_nest") || s.contains("jack_o_lantern")) {
                    return true;
                }
            }
            return false;
        }

        /** Skrzynia w losowej wolnej kratce prostokąta, najchętniej przy ścianie albo regale. */
        private boolean crateIn(int x1, int z1, int x2, int z2, int y, String facing, String pool) {
            for (int k = 0; k < 400; k++) {
                int x = Math.min(x1, x2) + rnd.nextInt(Math.abs(x2 - x1) + 1);
                int z = Math.min(z1, z2) + rnd.nextInt(Math.abs(z2 - z1) + 1);
                if (freeFloor(x, y, z) && (k > 300 || nextToWall(x, y, z))) {
                    crate(x, y, z, facing, pool);
                    return true;
                }
            }
            return false;
        }

        /** Strażnik: znacznik w podanym miejscu albo w najbliższej wolnej kratce obok. */
        private void guard(int x, int y, int z, String facing, String kit) {
            for (int r = 0; r <= 5; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) == r && freeFloor(x + dx, y, z + dz)) {
                            crate(x + dx, y, z + dz, facing, "npc-" + kit);
                            return;
                        }
                    }
                }
            }
            throw new IllegalStateException("brak miejsca na strażnika przy " + x + " " + y + " " + z);
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

        /** Rozrzucone kartki, szkło i śmieci na podłodze. */
        private void litter(int x1, int z1, int x2, int z2, int y, int count) {
            for (int i = 0; i < count; i++) {
                int x = x1 + rnd.nextInt(x2 - x1 + 1);
                int z = z1 + rnd.nextInt(z2 - z1 + 1);
                if (freeFloor(x, y, z)) {
                    set(x, y, z, pick("white_carpet", "white_carpet", "light_gray_carpet", "brown_carpet", "gray_carpet"));
                }
            }
        }

        /** Porzucone wózki sklepowe. */
        private void carts(int x1, int z1, int x2, int z2, int y, int count) {
            for (int i = 0; i < count; i++) {
                for (int k = 0; k < 50; k++) {
                    int x = x1 + rnd.nextInt(x2 - x1 + 1);
                    int z = z1 + rnd.nextInt(z2 - z1 + 1);
                    if (freeFloor(x, y, z)) {
                        set(x, y, z, WOZEK);
                        break;
                    }
                }
            }
        }

        /** Osobówka 2 × 4 na poziomie y (osłona), wzdłuż z albo x; czasem spalona. */
        private void sedan(int x, int y, int z, boolean alongZ) {
            boolean burnt = chance(0.15);
            String body = burnt ? "black_concrete" : car.next();
            for (int i = 0; i < 4; i++) {
                for (int w = 0; w < 2; w++) {
                    int px = alongZ ? x + w : x + i;
                    int pz = alongZ ? z + i : z + w;
                    set(px, y, pz, body);
                    if (i == 1 || i == 2) {
                        set(px, y + 1, pz, i == 1 ? (burnt ? "air" : brokenGlass().replace("cobweb", "glass_pane")) : body);
                    }
                }
            }
        }

        // ------------------------------------------------------------------ teren i parking

        private void terrain() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    fill(x, -G, z, x, -4, z, rock);
                    fill(x, -3, z, x, -1, z, "dirt");
                    set(x, 0, z, grass.next());
                }
            }
            // drogi: wjazd od północy, droga serwisowa od wschodu i z tyłu
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    boolean entry = x >= 64 && x <= 75 && z <= 37;
                    boolean lot = x >= 2 && x <= 137 && z >= 2 && z <= 35;
                    boolean service = x >= 133 && x <= 138 && z >= 30;
                    boolean back = z >= 114 && z <= 118 && x >= 4;
                    if (entry || lot || service || back) {
                        set(x, 0, z, chance(0.04) ? "mud" : asphalt.next());
                    }
                    // chodnik przed budynkiem
                    if (z >= 36 && z <= 39 && x >= 4 && x <= 135 && !entry) {
                        set(x, 0, z, chance(0.08) ? "cracked_stone_bricks" : pick("smooth_stone", "smooth_stone", "stone"));
                    }
                }
            }
            // linie wjazdu
            for (int z = 0; z <= 35; z += 3) {
                set(69, 0, z, "white_concrete");
                set(70, 0, z, "white_concrete");
            }
        }

        private void parkingLot() {
            // miejsca parkingowe w rzędach
            int[][] rows = {{5, 10}, {15, 20}, {24, 29}};
            for (int[] r : rows) {
                for (int x = 4; x <= 136; x += 3) {
                    if (x >= 62 && x <= 77 || x >= 115 && x <= 126) {
                        continue;
                    }
                    for (int z = r[0]; z <= r[1]; z++) {
                        if (chance(0.75)) {
                            set(x, 0, z, "white_concrete");
                        }
                    }
                }
            }
            // wysepki z trawą i latarnie między rzędami
            for (int x = 6; x <= 134; x += 16) {
                if (x >= 60 && x <= 78 || x >= 114 && x <= 126) {
                    continue;
                }
                for (int dx = 0; dx < 4; dx++) {
                    set(x + dx, 0, 12, grass.next());
                    set(x + dx, 0, 13, grass.next());
                    if (chance(0.3)) {
                        set(x + dx, 1, 12, pick("short_grass", "fern", "dead_bush"));
                    }
                }
                lampPost(x + 1, 13);
            }
            for (int x = 14; x <= 134; x += 16) {
                if (x >= 60 && x <= 78 || x >= 114 && x <= 126) {
                    continue;
                }
                lampPost(x, 22);
            }
            // samochody
            int[][] cars = {{7, 6, 1}, {13, 16, 1}, {22, 6, 1}, {31, 25, 1}, {40, 16, 1}, {46, 6, 1}, {52, 25, 1},
                    {82, 6, 1}, {88, 25, 1}, {97, 16, 1}, {103, 6, 1}, {109, 25, 1}, {130, 16, 1}, {25, 31, 0},
                    {93, 31, 0}};
            for (int[] c : cars) {
                sedan(c[0], 1, c[1], c[2] == 1);
            }
            // zagroda na wózki przy wejściu i porozrzucane wózki
            for (int x = 50; x <= 56; x++) {
                set(x, 1, 33, "iron_bars");
            }
            set(50, 1, 32, "iron_bars");
            set(56, 1, 32, "iron_bars");
            for (int x = 51; x <= 55; x++) {
                set(x, 1, 32, WOZEK);
            }
            carts(4, 3, 136, 34, 1, 12);
            // tablice
            post(62, 1, 1, 0, "PARKING", "TYLKO DLA", "KLIENTÓW.", "KLIENTÓW BRAK.");
            post(77, 1, 1, 0, "GALERIA", "KOMETA", "PARKING", "BEZPŁATNY");
        }

        private void lampPost(int x, int z) {
            if (chance(0.2)) {
                // przewrócona latarnia leży na ziemi
                for (int i = 0; i < 4; i++) {
                    if (isAir(x + i, 1, z)) {
                        set(x + i, 1, z, "andesite_wall");
                    }
                }
                set(x + 4, 1, z, "redstone_lamp[lit=false]");
                return;
            }
            fill(x, 1, z, x, 5, z, "andesite_wall");
            set(x, 6, z, "smooth_stone");
            set(x + 1, 6, z, slab("smooth_stone", "bottom"));
            set(x + 1, 5, z, "redstone_lamp[lit=false]");
            set(x - 1, 6, z, slab("smooth_stone", "bottom"));
            set(x - 1, 5, z, "redstone_lamp[lit=false]");
        }

        /** Pylon przy wjeździe: dwie nogi i tablica z kometą. */
        private void pylon() {
            fill(55, 1, 3, 55, 13, 4, "light_gray_concrete");
            fill(60, 1, 3, 60, 13, 4, "light_gray_concrete");
            fill(53, 14, 3, 62, 25, 4, "white_concrete");
            fill(53, 14, 3, 62, 14, 4, "orange_terracotta");
            fill(53, 25, 3, 62, 25, 4, "orange_terracotta");
            // kometa (od strony drogi widać ją po lewej, ogon w prawo)
            fill(59, 20, 2, 60, 21, 2, "yellow_concrete");
            int[][] tail = {{58, 20}, {57, 20}, {57, 19}, {56, 19}, {55, 18}, {54, 18}};
            for (int i = 0; i < tail.length; i++) {
                set(tail[i][0], tail[i][1], 2, i < 3 ? "orange_concrete" : "red_concrete");
            }
            set(61, 22, 2, "yellow_concrete");
            sign(58, 17, 2, "north", "GALERIA", "KOMETA", "", "");
            sign(57, 16, 2, "north", "OTWARTE", "9 – 21", "7 DNI", "W TYGODNIU");
            sign(59, 16, 2, "north", "SKLEPY · KINO", "KRĘGLE", "RESTAURACJE", "");
            // brakuje kilku płyt
            for (int i = 0; i < 6; i++) {
                int x = 53 + rnd.nextInt(10);
                int y = 15 + rnd.nextInt(10);
                if (get(x, y, 3).equals("minecraft:white_concrete")) {
                    set(x, y, 3, "air");
                }
            }
        }

        private void busStop() {
            fill(84, 1, 3, 84, 3, 3, "iron_bars");
            fill(90, 1, 3, 90, 3, 3, "iron_bars");
            for (int x = 84; x <= 90; x++) {
                set(x, 4, 2, slab("smooth_stone", "bottom"));
                set(x, 4, 3, slab("smooth_stone", "bottom"));
                if (x > 84 && x < 90) {
                    set(x, 2, 3, brokenGlass());
                    set(x, 3, 3, brokenGlass());
                }
            }
            f.bench(85, 1, 2, 5, true, "south", "spruce");
            set(91, 1, 2, "green_terracotta");
            post(83, 1, 2, 8, "PRZYSTANEK", "GALERIA KOMETA", "LINIA 7", "");
            post(92, 1, 3, 8, "OSTATNI KURS", "BYŁ W MAJU.", "DALEJ CZEKAM.", "– ZDZISIEK");
        }

        // ------------------------------------------------------------------ garaż podziemny

        private void garage() {
            for (int x = 9; x <= 130; x++) {
                for (int z = 40; z <= 113; z++) {
                    boolean edge = x == 9 || x == 130 || z == 40 || z == 113;
                    set(x, GB, z, edge ? garageWall.next() : garageFloor.next());
                    for (int y = GB + 1; y <= -1; y++) {
                        set(x, y, z, edge ? garageWall.next() : "air");
                    }
                }
            }
            // linie miejsc postojowych
            int[][] bays = {{42, 46}, {54, 58}, {61, 65}, {88, 92}, {95, 99}, {107, 111}};
            for (int[] b : bays) {
                for (int x = 12; x <= 127; x += 3) {
                    for (int z = b[0]; z <= b[1]; z++) {
                        if (chance(0.8)) {
                            set(x, GB, z, "white_concrete");
                        }
                    }
                }
            }
            // zalany narożnik: podłoga niżej, woda na poziomie posadzki
            for (int x = 23; x <= 40; x++) {
                for (int z = 92; z <= 112; z++) {
                    set(x, GB - 1, z, "gray_concrete");
                    set(x, GB, z, "water");
                }
            }
            // słupy z żółto-czarną opaską
            for (int x = 17; x <= 127; x += 10) {
                for (int z = 50; z <= 110; z += 10) {
                    if (x <= 22 && z >= 102) {
                        continue;
                    }
                    set(x, GB, z, "gray_concrete");
                    set(x, GB + 1, z, "yellow_concrete");
                    set(x, GB + 2, z, "black_concrete");
                    fill(x, GB + 3, z, x, -1, z, "light_gray_concrete");
                }
            }
            // rury pod stropem
            for (int x = 10; x <= 129; x++) {
                for (int z : new int[]{48, 105}) {
                    if (chance(0.92)) {
                        set(x, -1, z, "lightning_rod[facing=east,powered=false,waterlogged=false]");
                    }
                }
            }
            // samochody
            int[][] cars = {{13, 42, 1}, {22, 42, 1}, {31, 54, 1}, {46, 61, 1}, {55, 42, 1}, {64, 54, 1}, {79, 61, 1},
                    {88, 42, 1}, {100, 54, 1}, {52, 88, 1}, {61, 95, 1}, {73, 107, 1}, {85, 88, 1}, {94, 107, 1},
                    {103, 95, 1}, {124, 88, 1}, {121, 107, 1}, {58, 75, 0}, {84, 78, 0}};
            for (int[] c : cars) {
                sedan(c[0], GB + 1, c[1], c[2] == 1);
            }
            // konwój: furgonetka wbita w słup przy rampie, otwarte tylne drzwi
            fill(110, GB + 1, 49, 112, GB + 3, 56, "gray_concrete");
            fill(110, GB + 2, 49, 112, GB + 2, 49, "black_stained_glass");
            fill(111, GB + 1, 51, 111, GB + 2, 56, "air");
            set(110, GB + 2, 52, "iron_bars");
            set(112, GB + 2, 52, "iron_bars");
            set(111, GB + 1, 55, "brown_carpet");
            crate(111, GB + 1, 53, "south", "galeria-parking");
            sign(110, GB + 2, 57, "south", "KONWÓJ", "NIE", "ZATRZYMUJE", "SIĘ.");
            // obozowisko pod stropem i rozdzielnia
            f.camp(70, GB + 1, 100, "north");
            post(72, GB + 1, 103, 8, "TU ŚPI HENIEK.", "NIE BUDZIĆ,", "BO GRYZIE.", "");
            for (int z = 60; z <= 64; z += 2) {
                set(129, GB + 2, z, rozdzielnia("west"));
            }
            sign(129, GB + 2, 66, "west", "BYŁEM TU", "3 DNI. WODA", "SIĘ PODNOSI.", "– K.");
            sign(124, GB + 3, 41, "south", "PARKING -1", "WYSOKOŚĆ", "MAKS. 2,1 M", "");
            liczarnia();
            garageStairs(44, 82, true);
            garageStairs(104, 72, false);
        }

        /** Liczarnia gotówki za kratą w narożniku garażu; wejście tylko włazem z biura Maximarketu. */
        private void liczarnia() {
            for (int x = 10; x <= 22; x++) {
                fill(x, GB + 1, 102, x, -2, 102, "iron_bars");
                set(x, -1, 102, "light_gray_concrete");
            }
            for (int z = 102; z <= 112; z++) {
                fill(22, GB + 1, z, 22, -2, z, "iron_bars");
                set(22, -1, z, "light_gray_concrete");
            }
            fill(10, GB, 103, 21, GB, 112, "smooth_stone");
            // stoły z maszynami do liczenia, worki, sejf
            f.desk(14, GB + 1, 105, 19, 105, "spruce");
            set(15, GB + 2, 105, "lectern[facing=north,has_book=false,powered=false]");
            set(18, GB + 2, 105, "lectern[facing=north,has_book=true,powered=false]");
            f.chair(15, GB + 1, 106, "south", "spruce");
            fill(19, GB + 1, 110, 21, GB + 2, 112, "iron_block");
            set(20, GB + 1, 110, "air");
            set(20, GB + 2, 110, "air");
            crate(20, GB + 1, 111, "north", "galeria-skarbiec");
            set(20, GB + 1, 112, "iron_block");
            crate(10, GB + 1, 104, "east", "galeria-skarbiec");
            crate(16, GB + 1, 112, "north", "galeria-biuro");
            for (int[] p : new int[][]{{11, 107}, {11, 108}, {13, 110}, {14, 110}, {11, 109}}) {
                set(p[0], GB + 1, p[1], pick("brown_wool", "brown_wool", "light_gray_wool"));
            }
            set(11, GB + 2, 108, "brown_carpet");
            // właz: drabina z biura Maximarketu (x 12, z 112)
            for (int y = GB + 1; y <= 0; y++) {
                set(12, y, 112, ladder("north"));
            }
            post(23, GB + 1, 106, 4, "LICZARNIA", "WSTĘP TYLKO", "Z KONWOJENTEM", "");
            post(16, GB + 1, 100, 8, "KLUCZ MA", "KIEROWNIK.", "KIEROWNIK", "UCIEKŁ.");
            f.hangingLamp(15, -1, 108);
        }

        /**
         * Schody z garażu do pasażu: 7 stopni w garażu i wyjście przez otwór w posadzce parteru.
         * Dolny stopień w (x, zLow); {@code northward} – schody wznoszą się na północ (mniejsze z).
         */
        private void garageStairs(int x, int zLow, boolean northward) {
            int step = northward ? -1 : 1;
            String facing = northward ? "north" : "south";
            for (int i = 0; i < 7; i++) {
                int z = zLow + step * i;
                int ys = GB + 1 + i;
                for (int dx = 0; dx <= 1; dx++) {
                    fill(x + dx, GB + 1, z, x + dx, ys - 1, z, "light_gray_concrete");
                    set(x + dx, ys, z, stairs("stone_brick", facing));
                    fill(x + dx, ys + 1, z, x + dx, 0, z, "air");
                }
                set(x - 1, 1, z, "iron_bars");
                set(x + 2, 1, z, "iron_bars");
            }
            set(x, 1, zLow - step, "iron_bars");
            set(x + 1, 1, zLow - step, "iron_bars");
            set(x - 1, 1, zLow - step, "iron_bars");
            set(x + 2, 1, zLow - step, "iron_bars");
        }

        /** Rampa z parkingu do garażu: co dwie kratki pół bloku w dół, wjazd przez ścianę z 40. */
        private void garageRamp() {
            for (int i = 0; i < 14; i++) {
                int z = 26 + i;
                int depth = (i + 1) / 2;
                boolean half = (i + 1) % 2 == 1;
                for (int x = 118; x <= 123; x++) {
                    int top = half ? -depth - 1 : -depth - 1;
                    fill(x, top + 1, z, x, 0, z, "air");
                    set(x, top, z, half ? slab("smooth_stone", "bottom") : asphalt.next());
                }
                fill(117, GB, z, 117, 0, z, "light_gray_concrete");
                fill(124, GB, z, 124, 0, z, "light_gray_concrete");
                set(117, 1, z, "andesite_wall");
                set(124, 1, z, "andesite_wall");
            }
            fill(118, GB + 1, 40, 123, -3, 40, "air");
            for (int x = 118; x <= 123; x++) {
                set(x, GB, 40, garageFloor.next());
                set(x, -2, 40, x % 2 == 0 ? "yellow_concrete" : "black_concrete");
            }
        }

        // ------------------------------------------------------------------ bryła budynku

        private void shell() {
            // posadzki, stropy i dach
            for (int x = X1 + 1; x < X2; x++) {
                for (int z = Z1 + 1; z < Z2; z++) {
                    set(x, 0, z, tile.next());
                    set(x, 6, z, tile.next());
                    set(x, 12, z, tile.next());
                    set(x, ROOF, z, roof.next());
                }
            }
            // ściany zewnętrzne
            for (int y = 0; y <= ROOF + 1; y++) {
                for (int x = X1; x <= X2; x++) {
                    set(x, y, Z1, facadeBlock(x, y, true));
                    set(x, y, Z2, facadeBlock(x, y, false));
                }
                for (int z = Z1; z <= Z2; z++) {
                    set(X1, y, z, sideBlock(z, y, true));
                    set(X2, y, z, sideBlock(z, y, false));
                }
            }
            for (int x = X1; x <= X2; x++) {
                set(x, ROOF + 2, Z1, slab("smooth_stone", "bottom"));
                set(x, ROOF + 2, Z2, slab("smooth_stone", "bottom"));
            }
            for (int z = Z1; z <= Z2; z++) {
                set(X1, ROOF + 2, z, slab("smooth_stone", "bottom"));
                set(X2, ROOF + 2, z, slab("smooth_stone", "bottom"));
            }
            entrancePortal();
            // wyjścia ewakuacyjne z korytarzy technicznych
            for (int x : new int[]{50, 95}) {
                door(x, 1, Z1, "iron_door", "north", "left", chance(0.5));
                sign(x + 1, 2, Z1 - 1, "north", "WYJŚCIE", "EWAKUACYJNE", "", "");
            }
            for (int x : new int[]{45, 95}) {
                door(x, 1, Z2, "iron_door", "south", "left", true);
            }
        }

        /** Front (północ) i tył: pasy okien korytarzy na piętrach, pomarańczowy pas pod attyką. */
        private String facadeBlock(int x, int y, boolean front) {
            if (y == 0) {
                return "gray_concrete";
            }
            if (y <= 2) {
                return front ? "gray_concrete" : facade.next();
            }
            if (y == 17 || y == ROOF + 1) {
                return y == 17 ? "orange_terracotta" : "light_gray_concrete";
            }
            if (y == 6 || y == 12) {
                return "white_concrete";
            }
            boolean band = (y >= 8 && y <= 10) || (y >= 14 && y <= 16);
            if (band && x > CX1 && x < CX2) {
                if (x % 6 == 0) {
                    return "white_concrete";
                }
                return chance(0.2) ? "air" : "cyan_stained_glass";
            }
            return facade.next();
        }

        /** Ściany boczne (anchory): okna tylko na 1 piętrze, szyld na wysokości 2 piętra. */
        private String sideBlock(int z, int y, boolean west) {
            if (y == 0 || y <= 2) {
                return "gray_concrete";
            }
            if (y == 17 || y == ROOF + 1) {
                return y == 17 ? "orange_terracotta" : "light_gray_concrete";
            }
            if (y == 6 || y == 12) {
                return "white_concrete";
            }
            if (y >= 8 && y <= 10 && z > 44 && z < 110 && z % 8 < 5) {
                return chance(0.25) ? "air" : "cyan_stained_glass";
            }
            return facade.next();
        }

        /** Przeszklony portal wejściowy x 63-76, daszek nad wejściem i logo komety nad nim. */
        private void entrancePortal() {
            for (int y = 1; y <= 11; y++) {
                for (int x = 64; x <= 75; x++) {
                    set(x, y, Z1, y == 6 ? "white_concrete" : brokenGlass().equals("air") ? "air"
                            : "cyan_stained_glass_pane");
                }
                set(63, y, Z1, "white_concrete");
                set(76, y, Z1, "white_concrete");
            }
            fill(67, 1, Z1, 72, 3, Z1, "air");
            // drzwi obrotowe: zostały tylko ramy
            set(66, 1, Z1, "iron_bars");
            set(73, 1, Z1, "iron_bars");
            // daszek
            fill(61, 6, 33, 78, 6, 39, slab("smooth_stone", "top"));
            fill(61, 6, 33, 78, 6, 33, "orange_terracotta");
            fill(61, 1, 33, 61, 5, 33, "white_concrete");
            fill(78, 1, 33, 78, 5, 33, "white_concrete");
            for (int i = 0; i < 6; i++) {
                set(62 + rnd.nextInt(16), 6, 34 + rnd.nextInt(5), "air");
            }
            // logo komety nad daszkiem (patrząc od parkingu: kometa po lewej, ogon w prawo)
            fill(71, 14, Z1, 72, 15, Z1, "yellow_concrete");
            int[][] tail = {{70, 14}, {69, 14}, {69, 13}, {68, 13}, {67, 13}, {66, 12}};
            for (int i = 0; i < tail.length; i++) {
                set(tail[i][0], tail[i][1], Z1, i < 3 ? "orange_concrete" : "red_concrete");
            }
            sign(68, 16, Z1 - 1, "north", "GALERIA", "KOMETA", "", "");
            sign(62, 4, Z1 - 1, "north", "ZAPRASZAMY", "PON – NIEDZ", "9:00 – 21:00", "");
        }

        /** Ściany między pasażem a anchorami (x 30 i x 111): szerokie wejścia i drzwi z korytarzy. */
        private void anchorWalls() {
            for (int x : new int[]{30, 111}) {
                for (int z = Z1 + 1; z < Z2; z++) {
                    for (int y = 1; y < ROOF; y++) {
                        if (y != 6 && y != 12) {
                            set(x, y, z, wall.next());
                        }
                    }
                }
                for (int fl : FLOOR) {
                    fill(x, fl + 1, 70, x, fl + 4, 83, "air");
                    fill(x, fl + 1, 42, x, fl + 2, 42, "air");
                    fill(x, fl + 1, 111, x, fl + 2, 111, "air");
                    set(x, fl + 5, 69, "orange_terracotta");
                    set(x, fl + 5, 84, "orange_terracotta");
                }
            }
        }

        // ------------------------------------------------------------------ klatki schodowe i toalety

        private void cores() {
            core(31, 44);
            core(102, 105);
            sign(32, 2, 49, "south", "KLATKA", "SCHODOWA", "PIĘTRA 1 – 2", "");
        }

        /**
         * Klatka schodowa 8 × 3 (x0..x0+7, rzędy z0+1..z0+3, ściany na z0 i z0+4): bieg w górę na wschód
         * w rzędzie z0+1 (parter – 1 piętro), bieg na zachód w rzędzie z0+3 (1 – 2 piętro), środkowy rząd to ściana.
         * Podesty: x0 na parterze i 2 piętrze, x0+6..x0+7 na 1 piętrze. Drzwi w obu ścianach na każdym podeście.
         */
        private void core(int x0, int z0) {
            for (int y = 1; y < ROOF; y++) {
                for (int x = x0 - 1; x <= x0 + 8; x++) {
                    set(x, y, z0, "light_gray_concrete");
                    set(x, y, z0 + 4, "light_gray_concrete");
                }
                for (int z = z0; z <= z0 + 4; z++) {
                    set(x0 - 1, y, z, "light_gray_concrete");
                    set(x0 + 8, y, z, "light_gray_concrete");
                }
                for (int x = x0; x <= x0 + 7; x++) {
                    for (int z = z0 + 1; z <= z0 + 3; z++) {
                        set(x, y, z, y == 6 || y == 12 ? "smooth_stone" : "air");
                    }
                }
                fill(x0 + 1, y, z0 + 2, x0 + 5, y, z0 + 2, "stone_bricks");
            }
            for (int k = 0; k < 2; k++) {
                int h = 6 * k;
                boolean east = k == 0;
                int row = east ? z0 + 1 : z0 + 3;
                for (int i = 0; i < 5; i++) {
                    int x = east ? x0 + 1 + i : x0 + 5 - i;
                    int ys = h + 1 + i;
                    if (ys - 1 >= h + 1) {
                        fill(x, h + 1, row, x, ys - 1, row, "stone_bricks");
                    }
                    set(x, ys, row, stairs("stone_brick", east ? "east" : "west"));
                }
                fill(x0 + 1, h + 6, row, x0 + 5, h + 6, row, "air");
            }
            for (int k = 0; k < 3; k++) {
                int fl = FLOOR[k];
                int x = k == 1 ? x0 + 7 : x0;
                fill(x, fl + 1, z0, x, fl + 2, z0, "air");
                fill(x, fl + 1, z0 + 4, x, fl + 2, z0 + 4, "air");
                f.hangingLamp(k == 1 ? x0 + 6 : x0 + 1, fl + 5, z0 + 2 + (k == 1 ? -1 : 1));
            }
            cobwebs(x0, z0 + 1, x0 + 7, z0 + 3, 5, 3);
            cobwebs(x0, z0 + 1, x0 + 7, z0 + 3, 17, 3);
        }

        private void toilets() {
            for (int fl : FLOOR) {
                toiletBlock(31, 38, 49, 66, 67, fl, true);
                toiletBlock(102, 109, 87, 104, 86, fl, false);
            }
        }

        /** Toalety między klatką a pasażem: korytarzyk na środku, kabiny po bokach (panie / panowie). */
        private void toiletBlock(int x1, int x2, int za, int zb, int front, int fl, boolean north) {
            int mx = x1 + 3;
            for (int y = fl + 1; y <= fl + 5; y++) {
                for (int x = x1; x <= x2; x++) {
                    set(x, y, front, wall.next());
                }
                for (int z = za; z <= zb; z++) {
                    set(x1 - 1, y, z, wall.next());
                    set(x2 + 1, y, z, wall.next());
                }
            }
            fill(mx, fl + 1, front, mx + 1, fl + 2, front, "air");
            for (int x = x1; x <= x2; x++) {
                for (int z = za; z <= zb; z++) {
                    set(x, fl, z, chance(0.85) ? "white_terracotta" : "cyan_terracotta");
                }
            }
            // kabiny co 3 kratki po obu stronach korytarzyka
            for (int z = za; z <= zb; z++) {
                int rel = Math.abs(z - (north ? zb : za));
                boolean partition = rel % 3 == 2;
                for (int x : new int[]{x1, x1 + 1, x1 + 2, x2 - 2, x2 - 1, x2}) {
                    if (partition) {
                        fill(x, fl + 1, z, x, fl + 3, z, "light_gray_terracotta");
                    }
                }
                if (!partition) {
                    fill(x1 + 2, fl + 1, z, x1 + 2, fl + 3, z, rel % 3 == 0 && chance(0.4) ? "air" : "light_gray_terracotta");
                    fill(x2 - 2, fl + 1, z, x2 - 2, fl + 3, z, rel % 3 == 0 && chance(0.4) ? "air" : "light_gray_terracotta");
                    if (rel % 3 == 1) {
                        set(x1 + 2, fl + 1, z, "air");
                        set(x1 + 2, fl + 2, z, "air");
                        set(x2 - 2, fl + 1, z, "air");
                        set(x2 - 2, fl + 2, z, "air");
                        if (rel > 2) {
                            set(x1, fl + 1, z, kibel("east"));
                            set(x2, fl + 1, z, kibel("west"));
                        }
                    }
                }
            }
            // umywalki przy wejściu
            int sinkZ = north ? zb - 1 : za + 1;
            for (int x : new int[]{x1, x2}) {
                fill(x, fl + 1, sinkZ - 1, x, fl + 3, sinkZ + 1, "air");
                set(x, fl + 1, sinkZ, "cauldron");
                set(x == x1 ? x1 - 1 : x2 + 1, fl + 2, sinkZ, "glass");
            }
            sign(mx - 1, fl + 3, front + (north ? 1 : -1), north ? "south" : "north", "WC", "PANIE  PANOWIE", "", "");
            cobwebs(x1, za, x2, zb, fl + 3, 4);
        }

        // ------------------------------------------------------------------ sklepy

        private void shops() {
            String sk = "galeria-sklep";
            String je = "galeria-jedzenie";
            String el = "galeria-elektronika";
            // parter
            shop(new Unit(39, 51, 0, true), Kind.PHARMACY, "galeria-apteka", "green", 0, "APTEKA", "POD KOMETĄ");
            shop(new Unit(51, 63, 0, true), Kind.MONEY, "galeria-jubiler", "yellow", 1, "KANTOR", "KUPNO", "SPRZEDAŻ");
            shop(new Unit(76, 88, 0, true), Kind.BAKERY, je, "brown", 0, "PIEKARNIA", "U JANKA");
            shop(new Unit(88, 100, 0, true), Kind.DRUGSTORE, sk, "pink", 1, "DROGERIA", "PERFUMERIA");
            shop(new Unit(100, 111, 0, true), Kind.PRESS, sk, "blue", 0, "PRASA", "TYTOŃ", "ZDRAPKI");
            shop(new Unit(30, 42, 0, false), Kind.PHOTO, el, "red", 0, "FOTO-EXPRESS", "ZDJĘCIA W 1 H");
            shop(new Unit(42, 54, 0, false), Kind.JEWEL, "galeria-jubiler", "yellow", 2, "JUBILER", "ZŁOTY RÓG");
            shop(new Unit(54, 66, 0, false), Kind.SHOES, sk, "brown", 0, "OBUWIE", "SKÓRA I ZAMSZ");
            shop(new Unit(66, 78, 0, false), Kind.OPTICIAN, sk, "cyan", 0, "OPTYK", "OKULARY", "OD RĘKI");
            shop(new Unit(78, 90, 0, false), Kind.PHONES, el, "blue", 1, "TELEFONY", "KOMÓRKOWE");
            shop(new Unit(90, 101, 0, false), Kind.LAUNDRY, sk, "light_blue", 0, "PRALNIA", "CHEMICZNA");
            // 1 piętro
            shop(new Unit(39, 51, 6, true), Kind.CLOTHES, sk, "magenta", 0, "MODA DAMSKA", "ELEGANCJA");
            shop(new Unit(51, 63, 6, true), Kind.CLOTHES, sk, "blue", 1, "DŻINSY", "PROSTO Z USA");
            shop(new Unit(76, 88, 6, true), Kind.TOYS, sk, "orange", 0, "ZABAWKI", "MIŚ");
            shop(new Unit(88, 100, 6, true), Kind.BOOKS, sk, "green", 0, "KSIĘGARNIA", "PODRĘCZNIKI");
            shop(new Unit(100, 111, 6, true), Kind.CLOTHES, sk, "gray", 0, "KOSZULE", "I KRAWATY");
            shop(new Unit(30, 42, 6, false), Kind.COMPUTERS, el, "cyan", 0, "KOMPUTERY", "BAJT");
            shop(new Unit(42, 54, 6, false), Kind.VIDEO, sk, "red", 0, "VIDEO HIT", "WYPOŻYCZALNIA");
            shop(new Unit(54, 66, 6, false), Kind.MUSIC, sk, "purple", 1, "MUZYKA", "KASETY · PŁYTY");
            shop(new Unit(66, 78, 6, false), Kind.SHOES, sk, "lime", 0, "BUTY", "SPORTOWE");
            shop(new Unit(78, 90, 6, false), Kind.BAGS, sk, "brown", 0, "TORBY", "I WALIZKI");
            shop(new Unit(90, 101, 6, false), Kind.TRAVEL, sk, "yellow", 0, "BIURO PODRÓŻY", "EGIPT", "TUNEZJA");
            // 2 piętro: strefa restauracji, salon gier, administracja
            shop(new Unit(39, 51, 12, true), Kind.FOOD, je, "red", 0, "BAR CHIŃSKI", "SMAK ORIENTU");
            shop(new Unit(51, 63, 12, true), Kind.FOOD, je, "green", 0, "PIZZA", "U WŁOCHA");
            shop(new Unit(63, 76, 12, true), Kind.ARCADE, sk, "purple", 0, "AUTOMATY", "SALON GIER");
            shop(new Unit(76, 88, 12, true), Kind.FOOD, je, "orange", 0, "KEBAB", "U ALEGO");
            shop(new Unit(88, 100, 12, true), Kind.FOOD, je, "pink", 0, "LODY", "WŁOSKIE");
            shop(new Unit(100, 111, 12, true), Kind.OFFICE, "galeria-biuro", "gray", 1, "ADMINISTRACJA", "OCHRONA");
            shop(new Unit(30, 42, 12, false), Kind.CAFE, je, "brown", 0, "KAWIARNIA", "ESPRESSO");
            shop(new Unit(42, 54, 12, false), Kind.SOLARIUM, sk, "purple", 0, "SOLARIUM", "SŁOŃCE");
            shop(new Unit(54, 66, 12, false), Kind.GYM, "galeria-zaplecze", "black", 0, "SIŁOWNIA", "FITNESS CLUB");
            shop(new Unit(66, 78, 12, false), Kind.FOOD, je, "white", 0, "BAR MLECZNY", "POD KOMETĄ");
            shop(new Unit(78, 90, 12, false), Kind.HAIR, sk, "pink", 0, "FRYZJER", "DAMSKI · MĘSKI");
            shop(new Unit(90, 101, 12, false), Kind.LOST_FOUND, "galeria-zaplecze", "gray", 0, "BIURO RZECZY", "ZNALEZIONYCH");
        }

        /**
         * Lokal: ściany, zaplecze z drzwiami na korytarz techniczny, witryna (szyby w większości wybite),
         * szyld nad witryną od strony pasażu i wnętrze zależne od rodzaju. {@code shutter}: 0 – bez rolety,
         * 1 – roleta opuszczona do połowy, 2 – roleta zamknięta (wejście tylko przez zaplecze).
         */
        private void shop(Unit u, Kind kind, String pool, String accent, int shutter, String... name) {
            int fl = u.f();
            int cw = u.corridorWall();
            int zLo = Math.min(cw, u.front());
            int zHi = Math.max(cw, u.front());
            for (int y = fl + 1; y <= fl + 5; y++) {
                for (int z = zLo; z <= zHi; z++) {
                    set(u.x1(), y, z, wall.next());
                    set(u.x2(), y, z, wall.next());
                }
                for (int x = u.x1(); x <= u.x2(); x++) {
                    set(x, y, cw, wall.next());
                    set(x, y, u.back(), wall.next());
                }
                for (int x = u.a(); x <= u.b(); x++) {
                    for (int z = zLo + 1; z < zHi; z++) {
                        if (z != u.back()) {
                            set(x, y, z, "air");
                        }
                    }
                }
            }
            // posadzka
            Palette floor = shopFloor(kind);
            for (int x = u.a(); x <= u.b(); x++) {
                for (int z = zLo + 1; z < zHi; z++) {
                    if (z != u.back()) {
                        set(x, fl, z, floor.next());
                    }
                }
            }
            // przejścia: sklep – zaplecze i zaplecze – korytarz
            fill(u.b() - 1, fl + 1, u.back(), u.b() - 1, fl + 2, u.back(), "air");
            if (chance(0.6)) {
                door(u.b() - 1, fl + 1, u.back(), "spruce_door", u.in(), "left", true);
            }
            door(u.a() + 1, fl + 1, cw, "iron_door", u.in(), "right", chance(0.7));
            // witryna
            int f1 = u.front();
            int dm = u.mid();
            for (int x = u.a(); x <= u.b(); x++) {
                for (int y = fl + 1; y <= fl + 4; y++) {
                    set(x, y, f1, kind == Kind.FOOD && y == fl + 1 ? lada(u.out()) : kind == Kind.FOOD ? "air" : brokenGlass());
                }
                set(x, fl + 5, f1, accent + "_concrete");
            }
            set(u.x1(), fl + 5, f1, "white_concrete");
            set(u.x2(), fl + 5, f1, "white_concrete");
            if (kind == Kind.FOOD) {
                set(u.a(), fl + 1, f1, "air");
            } else {
                fill(dm, fl + 1, f1, dm + 1, fl + 3, f1, "air");
            }
            if (shutter == 1) {
                fill(u.a(), fl + 3, f1, u.b(), fl + 4, f1, "light_gray_concrete");
            } else if (shutter == 2) {
                fill(u.a(), fl + 1, f1, u.b(), fl + 4, f1, "light_gray_concrete");
                for (int x = u.a(); x <= u.b(); x += 2) {
                    set(x, fl + 1, f1, "polished_andesite");
                }
            }
            sign(dm, fl + 5, u.outside(), u.out(), name);
            // zaplecze
            backroom(u, kind);
            // wnętrze
            switch (kind) {
                case PHARMACY -> pharmacy(u, pool);
                case MONEY -> moneyChange(u, pool);
                case BAKERY -> bakery(u, pool);
                case DRUGSTORE, PRESS, TOYS, BOOKS, VIDEO, MUSIC, BAGS, OPTICIAN -> shelves(u, kind, pool);
                case PHOTO, PHONES, COMPUTERS -> electronics(u, kind, pool);
                case JEWEL -> jeweler(u, pool);
                case SHOES -> shoes(u, pool);
                case LAUNDRY -> laundry(u, pool);
                case CLOTHES -> clothes(u, pool);
                case TRAVEL -> travel(u, pool);
                case FOOD -> foodStall(u, pool, name[0]);
                case ARCADE -> arcade(u, pool);
                case OFFICE -> office(u, pool);
                case CAFE -> cafe(u, pool);
                case SOLARIUM -> solarium(u, pool);
                case GYM -> gym(u, pool);
                case HAIR -> hairdresser(u, pool);
                case LOST_FOUND -> lostAndFound(u, pool);
            }
            litter(u.a(), u.zMin(), u.b(), u.zMax(), fl + 1, 4);
            cobwebs(u.a(), u.zMin(), u.b(), u.zMax(), fl + 5, 3);
        }

        private Palette shopFloor(Kind kind) {
            return switch (kind) {
                case CLOTHES, BOOKS, JEWEL, TRAVEL, CAFE, OFFICE, SHOES -> new Palette(rnd)
                        .add("stripped_oak_wood[axis=x]", 6).add("oak_planks", 3).add("stripped_spruce_wood[axis=x]", 1);
                case ARCADE, VIDEO, MUSIC, GYM -> new Palette(rnd)
                        .add("gray_concrete", 5).add("blue_terracotta", 2).add("black_concrete", 1);
                case FOOD, BAKERY, PHARMACY, HAIR, SOLARIUM -> new Palette(rnd)
                        .add("white_terracotta", 5).add("light_gray_terracotta", 2).add("cracked_stone_bricks", 1);
                default -> new Palette(rnd)
                        .add("light_gray_terracotta", 6).add("white_terracotta", 3).add("cracked_stone_bricks", 1);
            };
        }

        /** Zaplecze: regały przy ścianie korytarza, kartony, czasem posłanie i skrzynia. */
        private void backroom(Unit u, Kind kind) {
            int fl = u.f();
            for (int x = u.a() + 2; x <= u.b(); x++) {
                if (chance(0.7)) {
                    set(x, fl + 1, u.br(4), regal(u.out()));
                    if (chance(0.6)) {
                        set(x, fl + 2, u.br(4), regal(u.out()));
                    }
                }
            }
            for (int i = 0; i < 3; i++) {
                int x = u.a() + rnd.nextInt(u.b() - u.a() - 1);
                int z = u.br(1 + rnd.nextInt(2));
                if (isAir(x, fl + 1, z) && x != u.b() - 1) {
                    set(x, fl + 1, z, pick("barrel[facing=up,open=false]", "brown_wool", "brown_wool", "mushroom_stem"));
                }
            }
            if (chance(0.25)) {
                set(u.a(), fl + 1, u.br(2), rozdzielnia(u.out().equals("south") ? "east" : "east"));
            }
            if (chance(0.15)) {
                f.bed(u.a(), fl + 1, u.br(1), u.in(), pick("brown", "gray", "green"));
            }
            String pool = switch (kind) {
                case PHONES, PHOTO, COMPUTERS -> "galeria-elektronika";
                case FOOD, BAKERY, CAFE -> "galeria-jedzenie";
                case JEWEL, MONEY, OFFICE -> "galeria-biuro";
                default -> "galeria-zaplecze";
            };
            crateIn(u.a(), u.br(0), u.b() - 2, u.br(3), fl + 1, u.out(), pool);
        }

        /** Lada z kasą przy wejściu (po prawej od drzwi patrząc z pasażu). */
        private void counterWithTill(Unit u, int x1, int x2, int d) {
            int fl = u.f();
            for (int x = x1; x <= x2; x++) {
                set(x, fl + 1, u.z(d), lada(u.out()));
            }
            set(x1 + (x2 - x1) / 2, fl + 2, u.z(d), kasa(u.in()));
        }

        private void pharmacy(Unit u, String pool) {
            int fl = u.f();
            // lada przez całą szerokość z przejściem przy ścianie
            for (int x = u.a() + 1; x <= u.b(); x++) {
                set(x, fl + 1, u.z(4), lada(u.out()));
            }
            set(u.a() + 3, fl + 2, u.z(4), kasa(u.in()));
            set(u.b() - 2, fl + 2, u.z(4), kasa(u.in()));
            // regały z lekami za ladą i przy witrynie
            for (int d = 6; d <= 15; d++) {
                set(u.a(), fl + 1, u.z(d), regal(u.north() ? "east" : "east"));
                set(u.a(), fl + 2, u.z(d), regal("east"));
                set(u.b(), fl + 1, u.z(d), regal("west"));
                set(u.b(), fl + 2, u.z(d), regal("west"));
            }
            for (int x = u.a() + 2; x <= u.b() - 2; x++) {
                set(x, fl + 1, u.z(15), regal(u.out()));
                set(x, fl + 2, u.z(15), regal(u.out()));
                set(x, fl + 1, u.z(10), chance(0.85) ? regal(u.out()) : "air");
            }
            for (int d = 1; d <= 2; d++) {
                set(u.a(), fl + 1, u.z(d), "white_concrete");
                set(u.a(), fl + 2, u.z(d), "potted_dead_bush");
            }
            // zielony krzyż na ścianie zaplecza
            int cx = u.mid();
            int bz = u.back();
            set(cx, fl + 3, bz, "lime_concrete");
            set(cx, fl + 2, bz, "lime_concrete");
            set(cx, fl + 4, bz, "lime_concrete");
            set(cx - 1, fl + 3, bz, "lime_concrete");
            set(cx + 1, fl + 3, bz, "lime_concrete");
            crateIn(u.a() + 1, u.z(5), u.b() - 1, u.z(9), fl + 1, u.out(), pool);
            crateIn(u.a() + 1, u.z(11), u.b() - 1, u.z(14), fl + 1, u.out(), pool);
        }

        private void moneyChange(Unit u, String pool) {
            int fl = u.f();
            // okienka kasowe za kratą przez całą szerokość
            for (int x = u.a(); x <= u.b(); x++) {
                set(x, fl + 1, u.z(4), lada(u.out()));
                set(x, fl + 2, u.z(4), x % 3 == 0 ? "iron_bars" : "glass_pane");
                set(x, fl + 3, u.z(4), "iron_bars");
                fill(x, fl + 4, u.z(4), x, fl + 5, u.z(4), "white_concrete");
            }
            set(u.b(), fl + 1, u.z(4), "air");
            set(u.b(), fl + 2, u.z(4), "air");
            door(u.b(), fl + 1, u.z(4), "iron_door", u.in(), "left", true);
            set(u.a() + 2, fl + 2, u.z(4), kasa(u.in()));
            set(u.a() + 6, fl + 2, u.z(4), kasa(u.in()));
            // za kratą: biurka, sejf
            f.desk(u.b() - 4, fl + 1, u.z(7), u.b() - 2, u.z(7), "spruce");
            set(u.b() - 3, fl + 2, u.z(7), "lectern[facing=" + u.out() + ",has_book=false,powered=false]");
            f.chair(u.b() - 3, fl + 1, u.z(8), u.in(), "spruce");
            fill(u.a(), fl + 1, u.z(13), u.a() + 2, fl + 3, u.z(15), "iron_block");
            fill(u.a() + 1, fl + 1, u.z(13), u.a() + 1, fl + 2, u.z(14), "air");
            crate(u.a() + 1, fl + 1, u.z(14), u.out(), pool);
            crateIn(u.a() + 3, u.z(9), u.b() - 2, u.z(15), fl + 1, u.out(), pool);
            // tablica kursów w poczekalni
            sign(u.a(), fl + 3, u.z(1), "east", "USD   3,95", "DEM   2,24", "ATS   0,32", "FRF   0,67");
            sign(u.a(), fl + 2, u.z(2), "east", "KURS DOLARA:", "BEZCENNY.", "", "– KASJERKA");
        }

        private void bakery(Unit u, String pool) {
            int fl = u.f();
            for (int x = u.a() + 1; x <= u.b(); x++) {
                set(x, fl + 1, u.z(3), lada(u.out()));
            }
            set(u.a() + 4, fl + 2, u.z(3), kasa(u.in()));
            // półki z pieczywem za ladą i piec
            for (int x = u.a(); x <= u.b(); x++) {
                set(x, fl + 1, u.z(6), chance(0.7) ? regal(u.out()) : "spruce_planks");
                set(x, fl + 2, u.z(6), chance(0.4) ? "hay_block" : regal(u.out()));
            }
            set(u.a(), fl + 1, u.z(6), "air");
            set(u.a(), fl + 2, u.z(6), "air");
            for (int x = u.a() + 1; x <= u.b() - 1; x += 3) {
                f.stove(x, fl + 1, u.z(15), u.out());
            }
            f.desk(u.a() + 2, fl + 1, u.z(11), u.b() - 2, u.z(11), "birch");
            f.sink(u.b(), fl + 1, u.z(13), "east");
            crateIn(u.a(), u.z(7), u.b(), u.z(14), fl + 1, u.out(), pool);
        }

        /** Sklep z regałami: drogeria, prasa, zabawki, księgarnia, wideo, muzyka, torby, optyk. */
        private void shelves(Unit u, Kind kind, String pool) {
            int fl = u.f();
            boolean books = kind == Kind.BOOKS || kind == Kind.VIDEO || kind == Kind.MUSIC;
            for (int d = 3; d <= 14; d++) {
                for (int dy = 1; dy <= (books ? 3 : 2); dy++) {
                    if (books) {
                        f.bookshelf(u.a(), fl + dy, u.z(d), u.a(), u.z(d), 1, "east");
                        f.bookshelf(u.b(), fl + dy, u.z(d), u.b(), u.z(d), 1, "west");
                    } else {
                        set(u.a(), fl + dy, u.z(d), regal("east"));
                        set(u.b(), fl + dy, u.z(d), regal("west"));
                    }
                }
            }
            // wyspy z towarem
            for (int d : new int[]{6, 7, 11, 12}) {
                for (int x = u.a() + 2; x <= u.b() - 2; x++) {
                    if (chance(0.1)) {
                        continue;
                    }
                    if (kind == Kind.TOYS && chance(0.5)) {
                        set(x, fl + 1, u.z(d), pick("red_wool", "yellow_wool", "lime_wool", "light_blue_wool", "pink_wool"));
                    } else if (kind == Kind.BAGS && chance(0.5)) {
                        set(x, fl + 1, u.z(d), pick("brown_wool", "black_wool", "brown_terracotta"));
                    } else if (kind == Kind.OPTICIAN) {
                        set(x, fl + 1, u.z(d), d % 2 == 0 ? "smooth_quartz" : "air");
                        if (d % 2 == 0) {
                            set(x, fl + 2, u.z(d), "glass");
                        }
                    } else if (books) {
                        f.bookshelf(x, fl + 1, u.z(d), x, u.z(d), 2, d % 2 == 0 ? u.out() : u.in());
                    } else {
                        set(x, fl + 1, u.z(d), regal(d % 2 == 0 ? u.out() : u.in()));
                    }
                }
            }
            // przewrócony regał
            if (chance(0.5)) {
                int x = u.a() + 2 + rnd.nextInt(Math.max(1, u.b() - u.a() - 3));
                set(x, fl + 1, u.z(9), regal(u.out()));
                set(x + 1, fl + 1, u.z(9), regal(u.in()));
            }
            counterWithTill(u, u.b() - 3, u.b() - 1, 1);
            if (kind == Kind.OPTICIAN) {
                for (int x = u.a(); x <= u.a() + 2; x++) {
                    set(x, fl + 2, u.z(15), "glass_pane");
                }
                f.chair(u.a() + 1, fl + 1, u.z(14), u.out(), "spruce");
            }
            if (kind == Kind.PRESS) {
                for (int d = 1; d <= 2; d++) {
                    set(u.a(), fl + 1, u.z(d), "white_concrete");
                    set(u.a(), fl + 2, u.z(d), "white_carpet");
                }
            }
            crateIn(u.a() + 1, u.z(3), u.b() - 1, u.z(9), fl + 1, u.out(), pool);
            crateIn(u.a() + 1, u.z(10), u.b() - 1, u.z(15), fl + 1, u.out(), pool);
        }

        /** Foto, telefony, komputery: lady przy ścianach, ściana ekranów, biurka z komputerami. */
        private void electronics(Unit u, Kind kind, String pool) {
            int fl = u.f();
            for (int d = 3; d <= 12; d++) {
                set(u.a(), fl + 1, u.z(d), lada("east"));
                set(u.b(), fl + 1, u.z(d), lada("west"));
            }
            // ekrany na ścianie zaplecza
            for (int x = u.a(); x <= u.b(); x++) {
                if (x == u.b() - 1) {
                    continue;
                }
                for (int y = fl + 2; y <= fl + 4; y++) {
                    boolean frame = (x - u.a()) % 3 == 0 || y == fl + 4;
                    set(x, y, u.z(15), frame ? "black_concrete"
                            : pick("black_stained_glass", "gray_stained_glass", "light_blue_stained_glass"));
                }
                set(x, fl + 1, u.z(15), "black_concrete");
            }
            if (kind == Kind.COMPUTERS) {
                for (int d : new int[]{6, 10}) {
                    for (int x = u.a() + 2; x <= u.b() - 2; x += 2) {
                        set(x, fl + 1, u.z(d), "lectern[facing=" + u.in() + ",has_book=" + chance(0.5) + ",powered=false]");
                        f.chair(x, fl + 1, u.z(d - 1), u.out(), "birch");
                    }
                }
            } else if (kind == Kind.PHONES) {
                for (int x = u.a() + 2; x <= u.b() - 2; x++) {
                    set(x, fl + 1, u.z(8), "smooth_quartz");
                    set(x, fl + 2, u.z(8), chance(0.7) ? "glass" : "air");
                }
                sign(u.a(), fl + 2, u.z(13), "east", "ZASIĘGU NIE MA.", "NIE PYTAĆ.", "", "");
            } else {
                // fotobudka i automat do zdjęć
                fill(u.a() + 2, fl + 1, u.z(9), u.a() + 3, fl + 3, u.z(11), "black_concrete");
                fill(u.a() + 2, fl + 1, u.z(10), u.a() + 2, fl + 2, u.z(10), "air");
                set(u.a() + 3, fl + 1, u.z(10), "white_carpet");
                set(u.b() - 3, fl + 1, u.z(10), rozdzielnia(u.out()));
            }
            counterWithTill(u, u.a() + 2, u.a() + 4, 2);
            crateIn(u.a() + 1, u.z(3), u.b() - 1, u.z(14), fl + 1, u.out(), pool);
            crateIn(u.a() + 1, u.z(3), u.b() - 1, u.z(14), fl + 1, u.out(), pool);
        }

        /** Jubiler: gabloty przy ścianach i na środku, sejf w rogu. Roleta zamknięta – wchodzi się z zaplecza. */
        private void jeweler(Unit u, String pool) {
            int fl = u.f();
            for (int d = 2; d <= 13; d++) {
                for (int x : new int[]{u.a(), u.b()}) {
                    set(x, fl + 1, u.z(d), "smooth_quartz");
                    set(x, fl + 2, u.z(d), chance(0.6) ? "glass" : "air");
                }
            }
            for (int x = u.a() + 3; x <= u.b() - 3; x++) {
                for (int d = 7; d <= 8; d++) {
                    set(x, fl + 1, u.z(d), "smooth_quartz");
                    set(x, fl + 2, u.z(d), chance(0.5) ? "glass" : "air");
                }
            }
            counterWithTill(u, u.a() + 3, u.b() - 3, 12);
            // sejf
            fill(u.a(), fl + 1, u.z(14), u.a() + 2, fl + 3, u.z(15), "iron_block");
            set(u.a() + 1, fl + 1, u.z(14), "air");
            set(u.a() + 1, fl + 2, u.z(14), "air");
            crate(u.a() + 1, fl + 1, u.z(14), u.out(), pool);
            crateIn(u.a() + 1, u.z(2), u.b() - 1, u.z(6), fl + 1, u.out(), pool);
            crateIn(u.a() + 1, u.z(9), u.b() - 1, u.z(13), fl + 1, u.out(), pool);
        }

        private void shoes(Unit u, String pool) {
            int fl = u.f();
            for (int d = 2; d <= 15; d++) {
                for (int x : new int[]{u.a(), u.b()}) {
                    set(x, fl + 1, u.z(d), "spruce_planks");
                    set(x, fl + 2, u.z(d), slab("spruce", "top"));
                    set(x, fl + 3, u.z(d), slab("spruce", "top"));
                }
                if (d % 3 == 0) {
                    set(u.a(), fl + 2, u.z(d), "brown_carpet");
                }
            }
            f.bench(u.a() + 3, fl + 1, u.z(8), u.b() - u.a() - 5, true, u.in(), "spruce");
            f.bench(u.a() + 3, fl + 1, u.z(11), u.b() - u.a() - 5, true, u.out(), "spruce");
            for (int x = u.a() + 2; x <= u.a() + 3; x++) {
                set(x, fl + 2, u.z(15), "glass_pane");
                set(x, fl + 3, u.z(15), "glass_pane");
            }
            counterWithTill(u, u.b() - 3, u.b() - 1, 2);
            crateIn(u.a() + 1, u.z(3), u.b() - 1, u.z(15), fl + 1, u.out(), pool);
        }

        private void laundry(Unit u, String pool) {
            int fl = u.f();
            for (int d = 4; d <= 15; d++) {
                for (int x : new int[]{u.a(), u.b()}) {
                    set(x, fl + 1, u.z(d), "white_concrete");
                    set(x, fl + 2, u.z(d), d % 2 == 0 ? "white_concrete" : "air");
                }
            }
            // wieszaki z ubraniami w foliach
            for (int x = u.a() + 2; x <= u.b() - 2; x++) {
                set(x, fl + 3, u.z(9), "chain[axis=x,waterlogged=false]");
                if (chance(0.7)) {
                    set(x, fl + 2, u.z(9), wallBanner(pick("white", "light_gray", "black", "blue"), u.out()));
                }
            }
            fill(u.a() + 1, fl + 1, u.z(9), u.a() + 1, fl + 3, u.z(9), "iron_bars");
            fill(u.b() - 1, fl + 1, u.z(9), u.b() - 1, fl + 3, u.z(9), "iron_bars");
            counterWithTill(u, u.a() + 2, u.b() - 2, 2);
            crateIn(u.a() + 1, u.z(4), u.b() - 1, u.z(15), fl + 1, u.out(), pool);
        }

        /** Odzież: wieszaki z ubraniami (stojące chorągwie), przymierzalnie z lustrami, lada. */
        private void clothes(Unit u, String pool) {
            int fl = u.f();
            String[] colors = {"red", "blue", "black", "white", "pink", "light_blue", "gray", "brown", "purple", "green"};
            for (int d : new int[]{4, 7, 10}) {
                for (int x = u.a() + 1; x <= u.b() - 1; x++) {
                    if ((x - u.a()) % 4 == 0 || chance(0.15)) {
                        continue;
                    }
                    set(x, fl + 1, u.z(d), banner(colors[rnd.nextInt(colors.length)], u.north() ? 0 : 8));
                }
            }
            for (int d = 2; d <= 11; d++) {
                if (chance(0.7)) {
                    set(u.a(), fl + 2, u.z(d), wallBanner(colors[rnd.nextInt(colors.length)], "east"));
                }
            }
            // przymierzalnie
            for (int i = 0; i < 2; i++) {
                int x = u.a() + i * 3;
                fill(x + 2, fl + 1, u.z(13), x + 2, fl + 3, u.z(15), "white_concrete");
                set(x, fl + 2, u.z(15), "glass");
                set(x + 1, fl + 1, u.z(13), "red_carpet");
            }
            counterWithTill(u, u.b() - 3, u.b() - 1, 1);
            crateIn(u.a() + 1, u.z(2), u.b() - 1, u.z(11), fl + 1, u.out(), pool);
            crateIn(u.a(), u.z(12), u.b(), u.z(15), fl + 1, u.out(), pool);
        }

        private void travel(Unit u, String pool) {
            int fl = u.f();
            for (int x = u.a() + 1; x <= u.b() - 1; x += 3) {
                set(x, fl + 1, u.z(6), "lectern[facing=" + u.out() + ",has_book=true,powered=false]");
                f.chair(x, fl + 1, u.z(7), u.in(), "birch");
                f.chair(x, fl + 1, u.z(4), u.out(), "birch");
            }
            for (int d = 2; d <= 14; d += 2) {
                set(u.a(), fl + 2, u.z(d), wallBanner(pick("light_blue", "yellow", "cyan", "orange"), "east"));
                set(u.b(), fl + 2, u.z(d), wallBanner(pick("light_blue", "yellow", "cyan", "orange"), "west"));
            }
            set(u.a() + 1, fl + 1, u.z(12), "potted_dead_bush");
            set(u.b() - 1, fl + 1, u.z(12), "potted_dead_bush");
            sign(u.mid(), fl + 2, u.z(15), u.out(), "EGIPT 7 DNI", "ALL INCLUSIVE.", "WYLOT:", "ODWOŁANY.");
            crateIn(u.a() + 1, u.z(8), u.b() - 1, u.z(15), fl + 1, u.out(), pool);
        }

        /** Stoisko w strefie restauracji: lada od strony pasażu, kuchnia z frytkownicą i lodówką. */
        private void foodStall(Unit u, String pool, String name) {
            int fl = u.f();
            set(u.a() + 3, fl + 2, u.front(), kasa(u.in()));
            for (int x = u.a(); x <= u.b(); x++) {
                if (x <= u.b() - 3) {
                    set(x, fl + 1, u.z(13), "smooth_stone");
                }
                if (x != u.b() - 1) {
                    set(x, fl + 1, u.z(15), "white_concrete");
                }
            }
            set(u.a() + 1, fl + 1, u.z(15), frytkownica(u.out()));
            set(u.a() + 2, fl + 1, u.z(15), frytkownica(u.out()));
            f.stove(u.a() + 4, fl + 1, u.z(15), u.out());
            f.stove(u.a() + 5, fl + 1, u.z(15), u.out());
            f.sink(u.a() + 7, fl + 1, u.z(15), u.in());
            lodowka(u.b(), fl + 1, u.z(12), "west");
            lodowka(u.b(), fl + 1, u.z(11), "west");
            set(u.a() + 2, fl + 2, u.z(13), "lantern[hanging=false,waterlogged=false]");
            f.desk(u.a(), fl + 1, u.z(6), u.a() + 3, u.z(6), "birch");
            // stoliki przed stoiskiem, na krawędzi pasażu
            int tz = u.north() ? 69 : 84;
            for (int x = u.a() + 1; x <= u.b() - 1; x += 5) {
                if (isAir(x, fl + 1, tz) && isAir(x - 1, fl + 1, tz) && isAir(x + 1, fl + 1, tz)) {
                    f.table(x, fl + 1, tz, "birch");
                    f.chair(x - 1, fl + 1, tz, "west", "birch");
                    f.chair(x + 1, fl + 1, tz, "east", "birch");
                }
            }
            if (name.equals("BAR MLECZNY")) {
                sign(u.mid(), fl + 4, u.z(15), u.out(), "ZAPIEKANKA", "3 ZŁ.", "ZAPIEKANEK", "BRAK.");
            } else if (name.equals("KEBAB")) {
                sign(u.mid(), fl + 4, u.z(15), u.out(), "KEBAB DUŻY", "KEBAB MAŁY", "KEBABU BRAK", "– ALI");
            }
            crateIn(u.a(), u.z(7), u.b() - 1, u.z(14), fl + 1, u.out(), pool);
        }

        /** Salon gier: rzędy automatów, trzy z nich to stanowiska gry (punkt-automat). */
        private void arcade(Unit u, String pool) {
            int fl = u.f();
            String[] screens = {"lime_stained_glass", "magenta_stained_glass", "cyan_stained_glass", "yellow_stained_glass",
                    "red_stained_glass", "blue_stained_glass"};
            int slots = 0;
            for (int d : new int[]{3, 7, 11}) {
                for (int x = u.a() + 1; x <= u.b() - 1; x += 2) {
                    String face = d == 7 ? u.out() : u.in();
                    if (d == 7 && slots < 3 && x >= u.a() + 3) {
                        counterMarker(x, fl + 1, u.z(d), face, "punkt-automat", "dispenser[facing=" + face + ",triggered=false]");
                        slots++;
                    } else {
                        set(x, fl + 1, u.z(d), "note_block[instrument=bit,note=" + rnd.nextInt(24) + ",powered=false]");
                    }
                    set(x, fl + 2, u.z(d), chance(0.15) ? "black_stained_glass" : screens[rnd.nextInt(screens.length)]);
                    set(x, fl + 3, u.z(d), pick("black_concrete", "purple_concrete", "red_concrete"));
                }
            }
            // stół do cymbergaja i siedzenia samochodówek
            fill(u.a() + 2, fl + 1, u.z(14), u.a() + 4, fl + 1, u.z(15), "white_concrete");
            fill(u.a() + 2, fl + 2, u.z(14), u.a() + 4, fl + 2, u.z(15), "lime_carpet");
            f.chair(u.b() - 2, fl + 1, u.z(14), u.in(), "dark_oak");
            f.chair(u.b() - 4, fl + 1, u.z(14), u.in(), "dark_oak");
            set(u.b() - 2, fl + 1, u.z(15), "black_concrete");
            set(u.b() - 4, fl + 1, u.z(15), "black_concrete");
            // rozmieniarka
            counterWithTill(u, u.a(), u.a() + 1, 1);
            sign(u.b(), fl + 2, u.z(5), "west", "REKORD:", "AAA  99999", "NIE DO", "POBICIA.");
            crateIn(u.a(), u.z(2), u.b(), u.z(15), fl + 1, u.out(), pool);
        }

        /** Administracja i ochrona: monitoring, biurka, kanapa herszta, drabina na dach w zapleczu. */
        private void office(Unit u, String pool) {
            int fl = u.f();
            for (int x = u.a(); x <= u.b(); x++) {
                if (x == u.b() - 1) {
                    continue;
                }
                set(x, fl + 1, u.z(15), "black_concrete");
                for (int y = fl + 2; y <= fl + 4; y++) {
                    set(x, y, u.z(15), (x - u.a()) % 2 == 0 && y < fl + 4 ? pick("gray_stained_glass", "black_stained_glass",
                            "light_gray_stained_glass") : "black_concrete");
                }
            }
            f.desk(u.a() + 1, fl + 1, u.z(13), u.b() - 3, u.z(13), "dark_oak");
            for (int x = u.a() + 2; x <= u.b() - 3; x += 3) {
                set(x, fl + 2, u.z(13), "lectern[facing=" + u.in() + ",has_book=false,powered=false]");
                f.chair(x, fl + 1, u.z(12), u.out(), "dark_oak");
            }
            // kanapa i biurko herszta
            f.bench(u.a() + 1, fl + 1, u.z(5), 4, true, u.out(), "dark_oak");
            f.table(u.a() + 3, fl + 1, u.z(7), "dark_oak");
            f.desk(u.b() - 3, fl + 1, u.z(8), u.b() - 1, u.z(8), "dark_oak");
            f.chair(u.b() - 2, fl + 1, u.z(9), u.out(), "dark_oak");
            set(u.b() - 1, fl + 2, u.z(8), "candle[candles=3,lit=true,waterlogged=false]");
            // barykada w wejściu
            for (int x = u.a(); x <= u.b(); x++) {
                if (x < u.mid() - 1 || x > u.mid() + 2) {
                    set(x, fl + 1, u.z(1), pick("barrel[facing=up,open=false]", "stripped_spruce_log[axis=x]", KANISTRY));
                }
            }
            crate(u.b() - 2, fl + 1, u.z(10), u.out(), pool);
            crateIn(u.a(), u.z(2), u.b(), u.z(11), fl + 1, u.out(), pool);
            // drabina na dach w zapleczu (przy ścianie korytarza)
            int lx = u.a() + 4;
            for (int y = fl + 1; y <= ROOF; y++) {
                set(lx, y, u.br(4), ladder(u.out()));
            }
        }

        private void cafe(Unit u, String pool) {
            int fl = u.f();
            for (int x = u.a(); x <= u.b(); x++) {
                set(x, fl + 1, u.z(13), lada(u.out()));
            }
            set(u.a(), fl + 1, u.z(13), "air");
            set(u.a() + 4, fl + 2, u.z(13), kasa(u.in()));
            f.stove(u.b() - 3, fl + 1, u.z(15), u.out());
            lodowka(u.b(), fl + 1, u.z(15), u.out());
            for (int d : new int[]{3, 7, 10}) {
                for (int x = u.a() + 1; x <= u.b() - 1; x += 4) {
                    f.table(x, fl + 1, u.z(d), "spruce");
                    f.chair(x - 1, fl + 1, u.z(d), "west", "spruce");
                    if (x + 1 <= u.b()) {
                        f.chair(x + 1, fl + 1, u.z(d), "east", "spruce");
                    }
                }
            }
            crateIn(u.a(), u.z(14), u.b(), u.z(15), fl + 1, u.out(), pool);
        }

        private void solarium(Unit u, String pool) {
            int fl = u.f();
            for (int i = 0; i < 3; i++) {
                int x = u.a() + i * 4;
                if (x + 3 > u.b() + 1) {
                    break;
                }
                fill(x + 3, fl + 1, u.z(9), x + 3, fl + 3, u.z(15), "white_concrete");
                set(x + 1, fl + 1, u.z(13), "white_concrete");
                set(x + 1, fl + 1, u.z(14), "white_concrete");
                set(x + 1, fl + 2, u.z(13), "purple_stained_glass");
                set(x + 1, fl + 2, u.z(14), "purple_stained_glass");
            }
            counterWithTill(u, u.a() + 1, u.a() + 3, 3);
            sign(u.b(), fl + 2, u.z(4), "west", "OPALANIE", "GRATIS.", "PRĄDU BRAK.", "");
            crateIn(u.a(), u.z(4), u.b(), u.z(15), fl + 1, u.out(), pool);
        }

        private void gym(Unit u, String pool) {
            int fl = u.f();
            for (int d = 3; d <= 14; d++) {
                set(u.b(), fl + 2, u.z(d), "glass");
            }
            for (int d : new int[]{4, 8, 12}) {
                f.bench(u.a() + 2, fl + 1, u.z(d), 2, true, u.in(), "dark_oak");
                set(u.a() + 5, fl + 1, u.z(d), "anvil[facing=east]");
                set(u.a() + 7, fl + 1, u.z(d), "iron_block");
                set(u.a() + 7, fl + 2, u.z(d), "chain[axis=y,waterlogged=false]");
            }
            // bieżnie
            for (int x = u.a() + 1; x <= u.b() - 2; x += 3) {
                set(x, fl + 1, u.z(15), "black_concrete");
                set(x, fl + 1, u.z(14), "black_carpet");
                set(x, fl + 2, u.z(15), "iron_bars");
            }
            counterWithTill(u, u.a(), u.a() + 2, 1);
            crateIn(u.a(), u.z(2), u.b(), u.z(15), fl + 1, u.out(), pool);
        }

        private void hairdresser(Unit u, String pool) {
            int fl = u.f();
            for (int d = 3; d <= 13; d += 3) {
                f.chair(u.a() + 1, fl + 1, u.z(d), "west", "dark_oak");
                set(u.a(), fl + 2, u.z(d), "glass");
                set(u.a(), fl + 3, u.z(d), "glass");
                set(u.b(), fl + 1, u.z(d), "cauldron");
            }
            f.bench(u.a() + 3, fl + 1, u.z(4), 3, true, u.in(), "birch");
            counterWithTill(u, u.b() - 3, u.b() - 1, 1);
            crateIn(u.a(), u.z(2), u.b(), u.z(14), fl + 1, u.out(), pool);
        }

        private void lostAndFound(Unit u, String pool) {
            int fl = u.f();
            for (int x = u.a(); x <= u.b(); x++) {
                set(x, fl + 1, u.z(4), lada(u.out()));
            }
            set(u.a(), fl + 1, u.z(4), "air");
            set(u.mid(), fl + 2, u.z(4), kasa(u.in()));
            for (int d = 7; d <= 15; d += 2) {
                for (int x = u.a() + 1; x <= u.b() - 2; x++) {
                    set(x, fl + 1, u.z(d), chance(0.8) ? regal(u.out()) : "barrel[facing=up,open=false]");
                    if (chance(0.5)) {
                        set(x, fl + 2, u.z(d), pick("end_rod[facing=up]", "brown_wool", "lantern[hanging=false,waterlogged=false]",
                                "player_head[rotation=" + rnd.nextInt(16) + "]", "red_carpet", "flower_pot"));
                    }
                }
            }
            sign(u.a(), fl + 2, u.z(2), "east", "ZNALEZIONO:", "SZTUCZNĄ", "SZCZĘKĘ.", "ODBIÓR W PIĄTEK");
            crateIn(u.a(), u.z(5), u.b(), u.z(15), fl + 1, u.out(), pool);
            crateIn(u.a(), u.z(5), u.b(), u.z(15), fl + 1, u.out(), pool);
        }

        // ------------------------------------------------------------------ hol wejściowy

        private void hall() {
            // parter x 64-75 / z 41-67, na dwie kondygnacje
            fill(64, 1, 41, 75, 11, 67, "air");
            for (int x = 64; x <= 75; x++) {
                for (int z = 41; z <= 67; z++) {
                    set(x, 0, z, (x + z) % 2 == 0 ? "polished_diorite" : "polished_andesite");
                }
            }
            for (int y = 1; y <= 11; y++) {
                for (int z = 41; z <= 67; z++) {
                    if (y != 6 || z < 44) {
                        set(63, y, z, wall.next());
                        set(76, y, z, wall.next());
                    }
                }
            }
            // strop 1 piętra nad holem wycięty, korytarz 1 piętra przechodzi po kładce z 41-43 za szybą
            fill(64, 6, 44, 75, 6, 67, "air");
            for (int x = 64; x <= 75; x++) {
                for (int y = 7; y <= 11; y++) {
                    set(x, y, 44, y == 8 || y == 9 ? (chance(0.4) ? "air" : "glass_pane") : wall.next());
                }
                set(x, 6, 44, "white_concrete");
                set(x, 7, 68, chance(0.85) ? "glass_pane" : "air");
            }
            fill(64, 1, 44, 75, 5, 44, "air");
            fill(64, 6, 41, 75, 6, 43, tile);
            fill(63, 7, 42, 63, 8, 42, "air");
            fill(76, 7, 42, 76, 8, 42, "air");
            // przejścia do korytarza technicznego z holu
            fill(63, 1, 42, 63, 2, 42, "air");
            fill(76, 1, 42, 76, 2, 42, "air");
            // wiatrołap: druga szklana ściana
            for (int x = 64; x <= 75; x++) {
                for (int y = 1; y <= 4; y++) {
                    set(x, y, 46, x >= 67 && x <= 72 && y <= 3 ? "air" : brokenGlass());
                }
                set(x, 5, 46, "white_concrete");
            }
            // informacja
            for (int x = 68; x <= 71; x++) {
                set(x, 1, 54, lada("north"));
                set(x, 1, 57, lada("south"));
            }
            set(68, 1, 55, lada("west"));
            set(68, 1, 56, lada("west"));
            set(71, 1, 55, lada("east"));
            set(70, 2, 54, kasa("south"));
            fill(69, 5, 55, 70, 5, 56, "white_concrete");
            sign(69, 5, 54, "north", "INFORMACJA", "", "", "");
            post(69, 2, 57, 0, "INFORMACJA", "NIECZYNNA.", "NIC NIE WIEMY.", "");
            // tablica z planem galerii
            sign(64, 3, 60, "east", "PARTER:", "MAXIMARKET", "APTEKA · KANTOR", "SPORT-MYŚLIWY");
            sign(64, 3, 61, "east", "1 PIĘTRO:", "DOM MODY", "ELEKTRO-MAX", "WIDEO · MUZYKA");
            sign(64, 3, 62, "east", "2 PIĘTRO:", "KINO · KRĘGLE", "RESTAURACJE", "SALON GIER");
            // bankomaty
            for (int z : new int[]{58, 61}) {
                set(75, 1, z, "iron_block");
                set(75, 2, z, "black_stained_glass");
                set(75, 3, z, "blue_concrete");
                set(74, 2, z, "stone_button[face=wall,facing=west,powered=false]");
            }
            // ławki i donice
            f.bench(65, 1, 49, 3, false, "west", "spruce");
            f.bench(74, 1, 49, 3, false, "east", "spruce");
            for (int[] p : new int[][]{{65, 64}, {74, 64}, {65, 53}, {74, 53}}) {
                set(p[0], 1, p[1], "bricks");
                set(p[0], 2, p[1], pick("dead_bush", "potted_dead_bush", "fern"));
            }
            // konik na monety dla dzieci
            set(72, 1, 64, "red_concrete");
            set(72, 2, 64, stairs("red_nether_brick", "north"));
            set(72, 1, 65, "red_concrete");
            carts(64, 47, 75, 66, 1, 3);
        }

        // ------------------------------------------------------------------ anchory

        /** Maximarket na parterze zachodniego skrzydła: regały, lodówki, kasy, zaplecze z biurem i włazem do liczarni. */
        private void hypermarket() {
            for (int x = 9; x <= 29; x++) {
                for (int z = 41; z <= 112; z++) {
                    set(x, 0, z, (x / 2 + z / 2) % 2 == 0 ? "white_terracotta" : "light_gray_terracotta");
                }
            }
            // kasy: co 4 kratki, taśma i kasa
            for (int[] range : new int[][]{{44, 64}, {89, 101}}) {
                for (int z = range[0]; z <= range[1]; z += 4) {
                    set(24, 1, z, "smooth_stone");
                    set(24, 2, z, "black_carpet");
                    set(25, 1, z, "smooth_stone");
                    set(25, 2, z, "black_carpet");
                    set(26, 1, z, lada("north"));
                    set(26, 2, z, kasa("south"));
                    if (chance(0.5)) {
                        set(27, 1, z + 1, WOZEK);
                    }
                }
            }
            // regały
            for (int[] range : new int[][]{{44, 64}, {89, 101}}) {
                for (int x : new int[]{12, 13, 16, 17, 20, 21}) {
                    String face = x % 4 == 0 ? "west" : "east";
                    for (int z = range[0]; z <= range[1]; z++) {
                        if (chance(0.07)) {
                            set(x, 1, z, pick("barrel[facing=east,open=true]", "brown_carpet", "air"));
                            continue;
                        }
                        set(x, 1, z, regal(face));
                        set(x, 2, z, chance(0.85) ? regal(face) : "air");
                    }
                }
            }
            // lodówki przy zachodniej ścianie
            for (int[] range : new int[][]{{44, 64}, {89, 101}}) {
                for (int z = range[0]; z <= range[1]; z++) {
                    if (chance(0.85)) {
                        lodowka(9, 1, z, "east");
                    }
                }
            }
            // stoiska z warzywami przy wejściu
            String[] produce = {"melon", "pumpkin", "hay_block", "dried_kelp_block", "brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]"};
            for (int x = 11; x <= 20; x += 4) {
                for (int z = 68; z <= 84; z += 5) {
                    String p = produce[rnd.nextInt(produce.length)];
                    for (int dx = 0; dx <= 1; dx++) {
                        for (int dz = 0; dz <= 1; dz++) {
                            set(x + dx, 1, z + dz, chance(0.8) ? p : "spruce_slab[type=bottom,waterlogged=false]");
                        }
                    }
                }
            }
            // lada mięsna przy północnej ścianie
            for (int x = 11; x <= 22; x++) {
                set(x, 1, 43, lada("south"));
                if (x % 3 == 0) {
                    lodowka(x, 1, 41, "south");
                }
            }
            set(16, 2, 43, kasa("north"));
            sign(15, 3, 41, "south", "MIĘSO", "WYDAJEMY", "OD 6:00.", "KOLEJKA OD 4:00");
            // zaplecze: biuro kierownika z włazem i magazyn z rampą
            for (int y = 1; y <= 5; y++) {
                for (int x = 9; x <= 29; x++) {
                    set(x, y, 103, wall.next());
                }
                for (int z = 104; z <= 112; z++) {
                    set(17, y, z, wall.next());
                }
            }
            door(14, 1, 103, "spruce_door", "south", "left", true);
            fill(24, 1, 103, 26, 3, 103, "air");
            door(17, 1, 109, "spruce_door", "west", "left", false);
            f.desk(13, 1, 106, 15, 106, "spruce");
            set(14, 2, 106, "lectern[facing=north,has_book=true,powered=false]");
            f.chair(14, 1, 107, "south", "spruce");
            fill(9, 1, 104, 10, 2, 105, "iron_block");
            set(10, 1, 108, rozdzielnia("east"));
            crate(9, 1, 110, "east", "galeria-biuro");
            set(12, 0, 112, ladder("north"));
            set(12, 1, 111, "air");
            for (int x = 19; x <= 28; x += 3) {
                for (int z = 105; z <= 111; z += 3) {
                    set(x, 1, z, pick("barrel[facing=up,open=false]", "brown_wool", "mushroom_stem", KANISTRY));
                    if (chance(0.5)) {
                        set(x, 2, z, pick("barrel[facing=up,open=false]", "brown_wool"));
                    }
                }
            }
            crate(28, 1, 104, "west", "galeria-zaplecze");
            crate(19, 1, 112, "north", "galeria-zaplecze");
            crate(25, 1, 108, "north", "galeria-zaplecze");
            // rampa w ścianie południowej
            fill(20, 1, Z2, 24, 2, Z2, "air");
            fill(20, 3, Z2, 24, 3, Z2, "light_gray_concrete");
            carts(10, 44, 29, 101, 1, 10);
            litter(10, 44, 29, 101, 1, 25);
            f.traces(10, 66, 29, 87, 1, 3, 3);
            sign(31, 5, 76, "east", "MAXIMARKET", "WSZYSTKO", "POD JEDNYM", "DACHEM");
            post(23, 1, 67, 4, "PROMOCJA!", "WSZYSTKO", "–100%", "");
        }

        /** Dom Mody na 1 piętrze zachodniego skrzydła. */
        private void fashion() {
            int fl = 6;
            String[] colors = {"red", "blue", "black", "white", "pink", "light_blue", "gray", "brown", "purple", "green", "yellow"};
            for (int x = 9; x <= 29; x++) {
                for (int z = 41; z <= 112; z++) {
                    set(x, fl, z, chance(0.9) ? "stripped_oak_wood[axis=x]" : "oak_planks");
                }
            }
            for (int[] range : new int[][]{{43, 66}, {88, 101}}) {
                for (int x = 12; x <= 26; x += 3) {
                    for (int z = range[0]; z <= range[1]; z++) {
                        if (z % 6 == 0 || chance(0.12)) {
                            continue;
                        }
                        set(x, fl + 1, z, banner(colors[rnd.nextInt(colors.length)], 4));
                    }
                }
            }
            // przymierzalnie przy zachodniej ścianie
            for (int z = 70; z <= 84; z += 3) {
                fill(9, fl + 1, z, 11, fl + 3, z, "white_concrete");
                set(9, fl + 2, z + 1, "glass");
                set(11, fl + 1, z + 1, "red_carpet");
            }
            // kasy
            for (int z = 70; z <= 82; z += 6) {
                set(27, fl + 1, z, lada("east"));
                set(27, fl + 1, z + 1, lada("east"));
                set(27, fl + 2, z, kasa("west"));
            }
            // zaplecze
            for (int y = fl + 1; y <= fl + 5; y++) {
                for (int x = 9; x <= 29; x++) {
                    set(x, y, 103, wall.next());
                }
            }
            fill(20, fl + 1, 103, 21, fl + 2, 103, "air");
            for (int x = 10; x <= 28; x += 2) {
                set(x, fl + 1, 111, regal("north"));
                set(x, fl + 2, 111, regal("north"));
            }
            crate(12, fl + 1, 105, "south", "galeria-zaplecze");
            crate(26, fl + 1, 108, "south", "galeria-zaplecze");
            sign(31, fl + 5, 76, "east", "DOM MODY", "ELEGANT", "", "");
            litter(9, 41, 29, 101, fl + 1, 20);
        }

        /** Kręgielnia na 2 piętrze zachodniego skrzydła: 5 torów, bar z wypożyczalnią butów, maszynownia. */
        private void bowling() {
            int fl = 12;
            for (int x = 9; x <= 29; x++) {
                for (int z = 41; z <= 112; z++) {
                    set(x, fl, z, "gray_concrete");
                }
            }
            for (int lane = 0; lane < 5; lane++) {
                int x = 11 + lane * 3;
                for (int z = 50; z <= 92; z++) {
                    set(x, fl, z, "birch_planks");
                    set(x + 1, fl, z, "birch_planks");
                    set(x + 2, fl, z, slab("polished_blackstone", "bottom"));
                }
                // kręgle: białe świeczki
                set(x, fl + 1, 52, "white_candle[candles=4,lit=false,waterlogged=false]");
                set(x + 1, fl + 1, 52, "white_candle[candles=3,lit=false,waterlogged=false]");
                if (chance(0.6)) {
                    set(x, fl + 1, 54, "white_candle[candles=" + (1 + rnd.nextInt(3)) + ",lit=false,waterlogged=false]");
                }
                // zwrotnica kul i ekran wyników
                set(x + 2, fl + 1, 95, "black_concrete");
                set(x + 2, fl + 2, 95, "coal_block");
                set(x, fl + 4, 94, "black_stained_glass");
                set(x + 1, fl + 4, 94, "black_stained_glass");
            }
            set(10, fl, 50, "gray_concrete");
            // maszynownia za torami
            for (int x = 9; x <= 29; x++) {
                for (int y = fl + 1; y <= fl + 5; y++) {
                    set(x, y, 49, y >= fl + 3 || x > 26 || x < 11 ? "black_concrete" : "air");
                }
            }
            fill(27, fl + 1, 49, 27, fl + 2, 49, "air");
            for (int x = 10; x <= 26; x += 3) {
                set(x, fl + 1, 44, rozdzielnia("south"));
                set(x, fl + 3, 46, "chain[axis=x,waterlogged=false]");
            }
            crate(12, fl + 1, 42, "south", "galeria-zaplecze");
            crate(24, fl + 1, 42, "south", "galeria-zaplecze");
            // siedziska graczy
            for (int lane = 0; lane < 5; lane++) {
                int x = 11 + lane * 3;
                f.chair(x, fl + 1, 98, "south", "dark_oak");
                f.chair(x + 1, fl + 1, 98, "south", "dark_oak");
            }
            // bar i wypożyczalnia butów
            for (int x = 10; x <= 27; x++) {
                set(x, fl + 1, 103, lada("north"));
            }
            set(28, fl + 1, 103, "air");
            set(14, fl + 2, 103, kasa("south"));
            set(23, fl + 2, 103, kasa("south"));
            f.bookshelf(10, fl + 1, 107, 18, 107, 3, "north");
            for (int x = 20; x <= 27; x++) {
                set(x, fl + 1, 108, "smooth_stone");
                if (x % 3 == 0) {
                    lodowka(x, fl + 1, 110, "north");
                }
            }
            f.stove(26, fl + 1, 108, "north");
            for (int x = 12; x <= 26; x += 3) {
                f.table(x, fl + 1, 100, "dark_oak");
            }
            crate(15, fl + 1, 105, "south", "galeria-jedzenie");
            crate(24, fl + 1, 106, "south", "galeria-jedzenie");
            sign(31, fl + 5, 76, "east", "KRĘGLE", "STRIKE", "", "");
            sign(9, fl + 2, 93, "east", "TOR 3 – REKORD:", "212 PKT, MAREK.", "MAREK, ODDAJ", "WRESZCIE KULĘ!");
        }

        /** Sport-Myśliwy na parterze wschodniego skrzydła: strzelnica, klatka z bronią, namioty, kajaki. */
        private void sport() {
            int fl = 0;
            for (int x = 112; x <= 130; x++) {
                for (int z = 41; z <= 112; z++) {
                    set(x, fl, z, chance(0.9) ? "green_terracotta" : "cracked_stone_bricks");
                }
            }
            // strzelnica łucznicza przy północnej ścianie
            for (int x = 114; x <= 128; x += 4) {
                set(x, 2, 41, "target[power=0]");
                set(x, 1, 41, "hay_block[axis=y]");
                set(x, 3, 41, "hay_block[axis=y]");
            }
            for (int x = 112; x <= 130; x++) {
                if (x % 5 != 0) {
                    set(x, 1, 56, "spruce_fence");
                }
            }
            // lada sklepu: stanowisko sklepu galerii w ladzie
            for (int z = 72; z <= 81; z++) {
                set(121, 1, z, lada("west"));
            }
            counterMarker(121, 1, 76, "west", "punkt-sklep-galeria", lada("west"));
            set(121, 2, 74, kasa("east"));
            for (int z = 70; z <= 83; z++) {
                set(130, 2, z, z % 2 == 0 ? "lightning_rod[facing=north,powered=false,waterlogged=false]" : "air");
                set(130, 3, z, z % 3 == 0 ? "lightning_rod[facing=south,powered=false,waterlogged=false]" : "air");
                set(130, 1, z, "spruce_planks");
            }
            // klatka z bronią: krata, stalowe drzwi z przyciskiem
            for (int y = 1; y <= 5; y++) {
                for (int z = 88; z <= 100; z++) {
                    set(122, y, z, "iron_bars");
                }
                for (int x = 122; x <= 130; x++) {
                    set(x, y, 88, "iron_bars");
                    set(x, y, 100, "iron_bars");
                }
            }
            fill(122, 1, 93, 122, 5, 93, "iron_block");
            fill(122, 3, 94, 122, 5, 94, "iron_block");
            door(122, 1, 94, "iron_door", "east", "left", false);
            set(121, 2, 93, "stone_button[face=wall,facing=west,powered=false]");
            for (int z = 90; z <= 98; z += 2) {
                set(130, 2, z, "lightning_rod[facing=north,powered=false,waterlogged=false]");
                set(130, 3, z, "lightning_rod[facing=north,powered=false,waterlogged=false]");
            }
            crate(128, 1, 90, "west", "galeria-militaria");
            crate(128, 1, 98, "west", "galeria-militaria");
            set(126, 1, 94, KANISTRY);
            // kajaki na wschodniej ścianie i namioty wystawowe
            for (int z = 58; z <= 64; z++) {
                set(130, 3, z, "orange_concrete");
                set(130, 5, z, "lime_concrete");
            }
            f.collapsedTent(113, 59, 120, 68, 1, "green");
            f.collapsedTent(113, 86, 120, 100, 1, "brown");
            // stojaki z odzieżą moro
            for (int x = 114; x <= 120; x += 2) {
                set(x, 1, 102, banner(pick("green", "brown", "lime"), 0));
            }
            // zaplecze
            for (int y = 1; y <= 5; y++) {
                for (int x = 112; x <= 130; x++) {
                    set(x, y, 104, wall.next());
                }
            }
            fill(114, 1, 104, 115, 2, 104, "air");
            for (int x = 113; x <= 129; x += 2) {
                set(x, 1, 112, regal("north"));
                set(x, 2, 112, regal("north"));
            }
            set(127, 1, 106, KANISTRY);
            set(128, 1, 106, KANISTRY);
            sign(130, 2, 106, "west", "KTO UKRADŁ", "MOJE", "KANISTRY?!", "– BOGDAN");
            crate(120, 1, 108, "south", "galeria-zaplecze");
            crate(125, 1, 110, "north", "galeria-militaria");
            sign(110, 5, 76, "west", "SPORT", "MYŚLIWY", "WĘDKARSTWO", "");
            litter(112, 41, 130, 103, 1, 15);
        }

        /** Elektro-Max na 1 piętrze wschodniego skrzydła: ściana telewizorów, AGD, komputery, kasy. */
        private void electro() {
            int fl = 6;
            for (int x = 112; x <= 130; x++) {
                for (int z = 41; z <= 112; z++) {
                    set(x, fl, z, chance(0.9) ? "light_gray_terracotta" : "cracked_stone_bricks");
                }
            }
            // ściana telewizorów
            for (int z = 56; z <= 98; z++) {
                for (int y = fl + 2; y <= fl + 4; y++) {
                    set(130, y, z, z % 3 == 0 ? "black_concrete" : pick("black_stained_glass", "gray_stained_glass",
                            "light_blue_stained_glass", "black_concrete"));
                }
                set(130, fl + 1, z, "black_concrete");
            }
            // pralki i lodówki
            for (int z = 44; z <= 54; z += 2) {
                for (int x = 113; x <= 125; x++) {
                    if (x % 4 == 0) {
                        continue;
                    }
                    if (z <= 48) {
                        set(x, fl + 1, z, "white_concrete");
                        set(x, fl + 2, z, "white_concrete");
                    } else {
                        set(x, fl + 1, z, "quartz_block");
                        set(x, fl + 2, z, chance(0.3) ? "white_carpet" : "air");
                    }
                }
            }
            // biurka z komputerami
            for (int z = 88; z <= 100; z += 4) {
                for (int x = 114; x <= 126; x += 3) {
                    set(x, fl + 1, z, "lectern[facing=west,has_book=" + chance(0.5) + ",powered=false]");
                }
            }
            // kasy przy wejściu
            for (int z = 66; z <= 86; z += 5) {
                if (z >= 70 && z <= 83) {
                    continue;
                }
                set(114, fl + 1, z, lada("west"));
                set(115, fl + 1, z, lada("west"));
                set(114, fl + 2, z, kasa("east"));
            }
            // kartony z towarem
            for (int i = 0; i < 14; i++) {
                int x = 113 + rnd.nextInt(17);
                int z = 58 + rnd.nextInt(28);
                if (isAir(x, fl + 1, z) && (z < 70 || z > 83 || x > 118)) {
                    set(x, fl + 1, z, pick("mushroom_stem", "brown_wool", "barrel[facing=up,open=false]"));
                }
            }
            // zaplecze
            for (int y = fl + 1; y <= fl + 5; y++) {
                for (int x = 112; x <= 130; x++) {
                    set(x, y, 104, wall.next());
                }
            }
            fill(120, fl + 1, 104, 121, fl + 2, 104, "air");
            for (int x = 113; x <= 129; x++) {
                set(x, fl + 1, 112, x % 2 == 0 ? regal("north") : "mushroom_stem");
            }
            crate(116, fl + 1, 108, "north", "galeria-elektronika");
            crate(127, fl + 1, 107, "north", "galeria-zaplecze");
            sign(110, fl + 5, 76, "west", "ELEKTRO-MAX", "RTV · AGD", "KOMPUTERY", "");
            sign(129, fl + 2, 77, "west", "KTO WYNIÓSŁ", "TELEWIZOR,", "NIECH ODDA", "CHOĆ PILOTA.");
            litter(112, 41, 130, 103, fl + 1, 15);
        }

        /** Kino na 2 piętrze wschodniego skrzydła: hol z kasą biletową i barem, dwie sale ze schodkowymi rzędami. */
        private void cinema() {
            int fl = 12;
            for (int x = 112; x <= 130; x++) {
                for (int z = 41; z <= 112; z++) {
                    set(x, fl, z, z >= 68 && z <= 85 ? (chance(0.9) ? "red_terracotta" : "black_terracotta") : "black_concrete");
                }
            }
            for (int y = fl + 1; y <= fl + 5; y++) {
                for (int x = 112; x <= 130; x++) {
                    set(x, y, 67, wall.next());
                    set(x, y, 86, wall.next());
                }
            }
            for (int x : new int[]{114, 127}) {
                fill(x, fl + 1, 67, x + 1, fl + 2, 67, "air");
                fill(x, fl + 1, 86, x + 1, fl + 2, 86, "air");
            }
            // kasa biletowa i bar
            for (int x = 120; x <= 128; x++) {
                set(x, fl + 1, 70, lada("south"));
                set(x, fl + 1, 83, lada("north"));
            }
            set(122, fl + 2, 70, kasa("north"));
            set(126, fl + 2, 70, kasa("north"));
            set(124, fl + 2, 83, kasa("south"));
            set(127, fl + 1, 85, "red_concrete");
            set(127, fl + 2, 85, "yellow_stained_glass");
            for (int x = 119; x <= 122; x++) {
                lodowka(x, fl + 1, 85, "north");
            }
            counterMarker(129, fl + 1, 78, "west", "punkt-sklep-automat-napoje",
                    "calibrated_sculk_sensor[facing=west,power=0,sculk_sensor_phase=inactive,waterlogged=false]");
            set(129, fl + 2, 78, "barrier");
            for (int z = 72; z <= 81; z += 3) {
                set(130, fl + 2, z, wallBanner(pick("red", "black", "yellow", "blue"), "west"));
            }
            f.bench(113, fl + 1, 74, 6, false, "west", "dark_oak");
            sign(121, fl + 3, 68, "south", "DZIŚ:", "SEANS", "ODWOŁANY.", "NA ZAWSZE.");
            sign(110, fl + 5, 76, "west", "KINO", "KOMETA", "2 SALE", "");
            // sala 1: ekran od północy, rzędy wznoszą się na południe
            fill(114, fl + 2, 41, 128, fl + 5, 41, "white_concrete");
            for (int r = 0; r < 8; r++) {
                int z = 50 + 2 * r;
                int h = r / 2;
                if (h > 0) {
                    fill(113, fl + 1, z, 129, fl + h, z + 1, "black_concrete");
                }
                for (int x = 114; x <= 128; x++) {
                    if (x != 121 && chance(0.9)) {
                        set(x, fl + 1 + h, z + 1, stairs("crimson", "south"));
                    }
                }
            }
            // sala 2: ekran od południa, rzędy wznoszą się na północ
            fill(114, fl + 2, 112, 128, fl + 5, 112, "white_concrete");
            for (int r = 0; r < 8; r++) {
                int z = 103 - 2 * r;
                int h = r / 2;
                if (h > 0) {
                    fill(113, fl + 1, z - 1, 129, fl + h, z, "black_concrete");
                }
                for (int x = 114; x <= 128; x++) {
                    if (x != 121 && chance(0.9)) {
                        set(x, fl + 1 + h, z - 1, stairs("crimson", "north"));
                    }
                }
            }
            // dziura w ekranie sali 2
            fill(119, fl + 3, 112, 121, fl + 4, 112, "air");
            crate(121, fl + 1, 47, "south", "galeria-sklep");
            crate(113, fl + 1, 106, "north", "galeria-sklep");
            crate(125, fl + 1, 84, "north", "galeria-jedzenie");
            crate(118, fl + 1, 71, "south", "galeria-sklep");
        }

        // ------------------------------------------------------------------ pasaż i atrium

        private void atrium() {
            // dziura w stropach 1 i 2 piętra
            for (int fl : new int[]{6, 12}) {
                fill(VX1, fl, VZ1, VX2, fl, VZ2, "air");
            }
            // kładka na 2 piętrze
            fill(BRX1, 12, VZ1, BRX2, 12, VZ2, tile);
            fill(79, 12, 77, 80, 12, VZ2, "air");
            // balustrady
            for (int fl : new int[]{6, 12}) {
                for (int x = VX1; x <= VX2; x++) {
                    set(x, fl + 1, VZ1 - 1, chance(0.88) ? "glass_pane" : "air");
                    set(x, fl + 1, VZ2 + 1, chance(0.88) ? "glass_pane" : "air");
                }
                for (int z = VZ1; z <= VZ2; z++) {
                    set(VX1 - 1, fl + 1, z, "glass_pane");
                    set(VX2 + 1, fl + 1, z, "glass_pane");
                }
            }
            for (int z = VZ1; z <= VZ2; z++) {
                set(BRX1, 13, z, "glass_pane");
                set(BRX2, 13, z, "glass_pane");
            }
            for (int x = BRX1 + 1; x <= BRX2 - 1; x++) {
                set(x, 13, VZ1 - 1, "air");
                set(x, 13, VZ2 + 1, "air");
            }
            // ruchome schody parter – 1 piętro (x 60-61, w górę na północ)
            escalator(60, 77, 0);
            // ruchome schody 1 – 2 piętro (x 79-80, z balkonu południowego na kładkę)
            escalator(79, 82, 6);
            fountain();
            tree();
            elevator();
            kiosks();
            collapse();
            // ławki i kosze na parterze pasażu
            for (int x = 34; x <= 108; x += 12) {
                if (isAir(x, 1, 69) && isAir(x + 1, 1, 69) && isAir(x + 2, 1, 69)) {
                    f.bench(x, 1, 69, 3, true, "north", "spruce");
                }
                if (isAir(x, 1, 84) && isAir(x + 1, 1, 84) && isAir(x + 2, 1, 84)) {
                    f.bench(x, 1, 84, 3, true, "south", "spruce");
                }
            }
            for (int fl : FLOOR) {
                litter(CX1, CZ1, CX2, CZ2, fl + 1, 30);
                carts(CX1, CZ1, CX2, CZ2, fl + 1, fl == 0 ? 6 : 2);
            }
            // pasy oświetlenia pod stropami balkonów
            for (int fl : new int[]{5, 11, 17}) {
                for (int x = CX1 + 2; x <= CX2 - 2; x += 6) {
                    for (int z : new int[]{CZ1 + 1, CZ2 - 1}) {
                        if (isAir(x, fl, z)) {
                            set(x, fl + 1, z, "redstone_lamp[lit=false]");
                        }
                    }
                }
            }
        }

        /**
         * Zatrzymane ruchome schody (x0, x0+1): dolny stopień w (zLow, fl+1), pięć stopni na północ,
         * wyjście na strop fl+6. Pod stopniami kratownica, po bokach balustrady.
         */
        private void escalator(int x0, int zLow, int fl) {
            for (int i = 0; i < 5; i++) {
                int z = zLow - 1 - i;
                int ys = fl + 1 + i;
                for (int dx = 0; dx <= 1; dx++) {
                    set(x0 + dx, ys, z, stairs("polished_andesite", "north"));
                    set(x0 + dx, ys - 1, z, fl == 0 && i == 0 ? tile.next() : "polished_andesite");
                    fill(x0 + dx, ys + 1, z, x0 + dx, ys + 3, z, "air");
                }
                if (ys + 1 < fl + 6) {
                    set(x0 - 1, ys + 1, z, "glass_pane");
                    set(x0 + 2, ys + 1, z, "glass_pane");
                }
                set(x0 - 1, ys, z, "smooth_quartz");
                set(x0 + 2, ys, z, "smooth_quartz");
            }
            for (int dx = 0; dx <= 1; dx++) {
                set(x0 + dx, fl + 6, zLow - 6, tile.next());
                set(x0 + dx, fl + 7, zLow - 6, "air");
                set(x0 + dx, fl + 1, zLow, fl == 0 ? "iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]" : "air");
            }
            if (fl == 6) {
                set(x0, 7, zLow, "air");
                set(x0 + 1, 7, zLow, "air");
            }
        }

        private void fountain() {
            for (int x = 66; x <= 73; x++) {
                for (int z = 74; z <= 79; z++) {
                    boolean rim = x == 66 || x == 73 || z == 74 || z == 79;
                    set(x, 1, z, rim ? "polished_andesite" : "water");
                    if (!rim && chance(0.15)) {
                        set(x, 2, z, "lily_pad");
                    }
                }
            }
            // kometa na postumencie
            fill(69, 1, 76, 70, 3, 77, "smooth_quartz");
            set(69, 4, 76, "yellow_concrete");
            set(70, 4, 77, "orange_concrete");
            set(70, 4, 76, "yellow_concrete");
            set(69, 4, 77, "red_concrete");
            set(70, 5, 76, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            post(68, 1, 73, 8, "NA SZCZĘŚCIE", "WRZUĆ MONETĘ.", "SZCZĘŚCIA", "NIE BYŁO.");
        }

        /** Drzewo, które przerosło donicę i sięga 2 piętra. */
        private void tree() {
            for (int x = 86; x <= 90; x++) {
                for (int z = 74; z <= 78; z++) {
                    boolean rim = x == 86 || x == 90 || z == 74 || z == 78;
                    set(x, 1, z, rim ? "bricks" : "rooted_dirt");
                    if (!rim && chance(0.5)) {
                        set(x, 2, z, pick("short_grass", "fern", "short_grass"));
                    }
                }
            }
            fill(88, 1, 76, 88, 14, 76, "oak_log[axis=y]");
            for (int y = 8; y <= 16; y++) {
                int r = y <= 9 ? 2 : y <= 14 ? 3 : 2;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dz * dz > r * r + 1 || chance(0.2)) {
                            continue;
                        }
                        int x = 88 + dx;
                        int z = 76 + dz;
                        if (x >= VX1 && x <= VX2 && z >= VZ1 && z <= VZ2 && isAir(x, y, z)) {
                            set(x, y, z, "oak_leaves[distance=1,persistent=true,waterlogged=false]");
                        }
                    }
                }
            }
            set(87, 10, 76, "oak_log[axis=x]");
            set(89, 12, 76, "oak_log[axis=x]");
        }

        /** Panoramiczna winda w atrium: szklany szyb z drabiną serwisową, kabina utknęła między 1 a 2 piętrem. */
        private void elevator() {
            int x1 = EX1;
            int x2 = EX1 + 4;
            int z1 = EZ1;
            int z2 = EZ1 + 4;
            for (int y = 1; y <= ROOF; y++) {
                for (int x = x1; x <= x2; x++) {
                    for (int z = z1; z <= z2; z++) {
                        boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                        boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                        if (edge) {
                            set(x, y, z, corner ? "light_gray_concrete" : y == ROOF ? "light_gray_concrete"
                                    : chance(0.15) ? "air" : "glass");
                        } else {
                            set(x, y, z, "air");
                        }
                    }
                }
            }
            for (int fl : FLOOR) {
                fill(x1 + 2, fl + 1, z2, x1 + 2, fl + 2, z2, "air");
            }
            for (int y = 1; y <= ROOF + 1; y++) {
                set(x1 + 2, y, z1 + 1, ladder("south"));
            }
            // kabina
            for (int x = x1 + 1; x <= x2 - 1; x++) {
                for (int z = z1 + 1; z <= z2 - 1; z++) {
                    if (x != x1 + 2 || z != z1 + 1) {
                        set(x, 9, z, "iron_block");
                        set(x, 12, z, "iron_block");
                    }
                }
            }
            set(x1 + 1, 10, z2 - 1, "skeleton_skull[powered=false,rotation=5]");
            set(x1 + 3, 10, z2 - 1, "white_carpet");
            set(x1 + 3, 3, z2, "glass");
            sign(x1 + 3, 3, z2 + 1, "south", "WINDA", "NIECZYNNA", "OD WTORKU.", "– ADMINISTRACJA");
            // maszynownia na dachu
            for (int y = ROOF + 1; y <= ROOF + 4; y++) {
                for (int x = x1; x <= x2; x++) {
                    for (int z = z1; z <= z2; z++) {
                        boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                        set(x, y, z, y == ROOF + 4 ? "light_gray_concrete" : edge ? "light_gray_concrete" : "air");
                    }
                }
            }
            for (int x = x1 + 1; x <= x2 - 1; x++) {
                for (int z = z1 + 1; z <= z2 - 1; z++) {
                    if (x != x1 + 2 || z != z1 + 1) {
                        set(x, ROOF, z, "iron_block");
                    }
                }
            }
            set(x1 + 2, ROOF + 1, z1 + 1, ladder("south"));
            set(x1 + 1, ROOF + 1, z1 + 1, "iron_block");
            set(x1 + 1, ROOF + 2, z1 + 1, "chain[axis=y,waterlogged=false]");
            set(x1 + 3, ROOF + 1, z1 + 1, rozdzielnia("south"));
            fill(x1 + 2, ROOF + 1, z2, x1 + 2, ROOF + 2, z2, "air");
            crate(x1 + 3, ROOF + 1, z2 - 1, "west", "galeria-dach");
        }

        /** Wyspy handlowe na parterze pasażu. */
        private void kiosks() {
            // dorabianie kluczy
            for (int x = 50; x <= 54; x++) {
                set(x, 1, 77, lada("north"));
                set(x, 1, 79, lada("south"));
            }
            set(50, 1, 78, lada("west"));
            set(54, 1, 78, "air");
            set(52, 2, 77, "grindstone[face=floor,facing=north]");
            set(51, 2, 79, kasa("north"));
            fill(50, 4, 77, 54, 4, 79, slab("smooth_stone", "bottom"));
            fill(50, 2, 77, 50, 3, 77, "iron_bars");
            fill(54, 2, 79, 54, 3, 79, "iron_bars");
            post(52, 1, 76, 8, "KLUCZE", "OBCASY", "PIECZĄTKI", "");
            crate(53, 1, 78, "west", "galeria-sklep");
            // perfumy
            for (int x = 80; x <= 83; x++) {
                set(x, 1, 70, lada("north"));
            }
            set(81, 2, 70, kasa("south"));
            set(82, 2, 70, "glass");
            post(79, 1, 70, 8, "PERFUMY", "Z PARYŻA", "(PRAWIE)", "");
        }

        /** Zawalony fragment stropu 2 piętra na balkonie południowym i gruz na 1 piętrze. */
        private void collapse() {
            for (int x = 86; x <= 91; x++) {
                for (int z = 82; z <= 85; z++) {
                    if (x > 86 && x < 91 || chance(0.5)) {
                        set(x, 12, z, "air");
                        set(x, 13, z, "air");
                    }
                }
            }
            for (int i = 0; i < 18; i++) {
                int x = 85 + rnd.nextInt(8);
                int z = 82 + rnd.nextInt(4);
                if (isAir(x, 7, z)) {
                    set(x, 7, z, chance(0.6) ? rubble.next() : slab("cobblestone", "bottom"));
                }
            }
            set(88, 8, 84, "cobblestone");
        }

        /** Korytarze techniczne: rozdzielnie na ścianach, rury, kilka skrzyń. */
        private void corridors() {
            for (int fl : FLOOR) {
                for (int x = 33; x <= 108; x += 9) {
                    if (fl == 0 && x >= 63 && x <= 76) {
                        continue;
                    }
                    if (isAir(x, fl + 1, 41)) {
                        set(x, fl + 2, 41, rozdzielnia("south"));
                    }
                    if (isAir(x, fl + 2, 112)) {
                        set(x, fl + 2, 112, rozdzielnia("north"));
                    }
                }
                for (int x = 31; x <= 110; x++) {
                    if (isAir(x, fl + 5, 42)) {
                        set(x, fl + 5, 42, "lightning_rod[facing=east,powered=false,waterlogged=false]");
                    }
                    if (isAir(x, fl + 5, 111)) {
                        set(x, fl + 5, 111, "lightning_rod[facing=east,powered=false,waterlogged=false]");
                    }
                }
                f.traces(31, 41, 110, 43, fl + 1, 1, 1);
                f.traces(31, 110, 110, 112, fl + 1, 1, 1);
                cobwebs(31, 41, 110, 43, fl + 4, 6);
                cobwebs(31, 110, 110, 112, fl + 4, 6);
            }
            crateIn(31, 41, 62, 43, 1, "south", "galeria-zaplecze");
            crateIn(77, 110, 110, 112, 7, "north", "galeria-zaplecze");
            crateIn(31, 41, 110, 43, 13, "south", "galeria-zaplecze");
        }

        // ------------------------------------------------------------------ dach

        private void roofTop() {
            // świetlik nad atrium
            for (int x = VX1 - 2; x <= VX2 + 2; x++) {
                for (int z = VZ1 - 2; z <= VZ2 + 2; z++) {
                    if (x >= EX1 && x <= EX1 + 4 && z >= EZ1 && z <= EZ1 + 4) {
                        continue;
                    }
                    boolean frame = x % 6 == 0 || z == VZ1 - 2 || z == VZ2 + 2 || x == VX1 - 2 || x == VX2 + 2;
                    if (frame) {
                        set(x, ROOF, z, "light_gray_concrete");
                        if (chance(0.12) && isAir(x, ROOF - 1, z)) {
                            set(x, ROOF - 1, z, "weeping_vines");
                        }
                    } else if (chance(0.14) || x >= 50 && x <= 55 && z >= 74 && z <= 78) {
                        set(x, ROOF, z, "air");
                        skylightHoles.add(new int[]{x, z});
                    } else {
                        set(x, ROOF, z, chance(0.3) ? "light_blue_stained_glass" : "glass");
                    }
                }
            }
            // szkło i gruz pod zawalonym fragmentem świetlika
            for (int i = 0; i < 14; i++) {
                int x = 49 + rnd.nextInt(8);
                int z = 73 + rnd.nextInt(7);
                if (freeFloor(x, 1, z)) {
                    set(x, 1, z, pick("glass_pane", "light_gray_concrete", slab("smooth_stone", "bottom"), "glass_pane"));
                }
            }
            // wentylatory
            int[][] units = {{20, 50}, {20, 95}, {115, 50}, {115, 95}, {40, 100}, {100, 48}, {60, 100}, {85, 48}};
            for (int[] u : units) {
                fill(u[0], ROOF + 1, u[1], u[0] + 2, ROOF + 2, u[1] + 2, "light_gray_concrete");
                fill(u[0], ROOF + 3, u[1], u[0] + 2, ROOF + 3, u[1] + 2, "iron_bars");
                set(u[0] + 1, ROOF + 3, u[1] + 1, "iron_block");
                for (int x = u[0] + 3; x <= u[0] + 6 && x < X2; x++) {
                    set(x, ROOF + 1, u[1] + 1, "polished_andesite_wall");
                }
            }
            // anteny i talerz
            fill(125, ROOF + 1, 108, 125, ROOF + 6, 108, "iron_bars");
            set(125, ROOF + 7, 108, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            set(120, ROOF + 1, 108, "iron_bars");
            set(120, ROOF + 2, 108, trapdoor("iron", "south", "bottom", true));
            billboard();
            // miejsce zrzutu: czerwony krzyż na dachu
            for (int i = -2; i <= 2; i++) {
                set(47 + i, ROOF, 55 + i, "red_concrete");
                set(47 + i, ROOF, 55 - i, "red_concrete");
            }
            crate(47, ROOF + 1, 55, "north", "zrzut");
            sign(44, ROOF + 1, 52, "south", "TU LĄDUJĄ", "PACZKI.", "KTO PIERWSZY,", "TEN BIERZE.");
            set(44, ROOF, 52, "light_gray_concrete");
            set(44, ROOF + 1, 51, "light_gray_concrete");
            crateIn(10, 41, 129, 112, ROOF + 1, "north", "galeria-dach");
            f.collapsedTent(100, 95, 125, 110, ROOF + 1, "gray");
            // drabina z rampy rozładunkowej na dach
            for (int y = 6; y <= ROOF + 1; y++) {
                set(76, y, Z2 + 1, ladder("south"));
            }
        }

        /** Billboard KOMETA na krawędzi dachu od strony parkingu, z pomostem serwisowym. */
        private void billboard() {
            int z = Z1 + 2;
            for (int x : new int[]{53, 70, 87}) {
                fill(x, ROOF + 1, z, x, ROOF + 2, z + 1, "light_gray_concrete");
            }
            fill(52, ROOF + 3, z, 88, ROOF + 9, z, "blue_concrete");
            fill(52, ROOF + 3, z, 88, ROOF + 3, z, "orange_terracotta");
            fill(52, ROOF + 9, z, 88, ROOF + 9, z, "orange_terracotta");
            // napis KOMETA pikselami 3 × 5 (od strony parkingu czyta się od lewej, czyli od większego x)
            String[][] glyphs = {
                    {"101", "110", "100", "110", "101"},
                    {"111", "101", "101", "101", "111"},
                    {"101", "111", "101", "101", "101"},
                    {"111", "100", "110", "100", "111"},
                    {"111", "010", "010", "010", "010"},
                    {"010", "101", "111", "101", "101"}};
            int start = 81;
            for (String[] g : glyphs) {
                for (int row = 0; row < 5; row++) {
                    for (int c = 0; c < 3; c++) {
                        if (g[row].charAt(c) == '1') {
                            set(start - c, ROOF + 8 - row, z, "white_concrete");
                        }
                    }
                }
                start -= 4;
            }
            // kometa
            fill(84, ROOF + 5, z, 85, ROOF + 6, z, "yellow_concrete");
            set(86, ROOF + 6, z, "orange_concrete");
            set(87, ROOF + 7, z, "red_concrete");
            set(86, ROOF + 7, z, "orange_concrete");
            // ubytki w tablicy
            for (int i = 0; i < 12; i++) {
                int x = 52 + rnd.nextInt(37);
                int y = ROOF + 3 + rnd.nextInt(7);
                if (get(x, y, z).equals("minecraft:blue_concrete")) {
                    set(x, y, z, "air");
                }
            }
            // pomost za tablicą
            fill(52, ROOF + 2, z + 1, 88, ROOF + 2, z + 1, slab("smooth_stone", "top"));
            for (int x = 52; x <= 88; x += 2) {
                set(x, ROOF + 3, z + 2, "iron_bars");
            }
            fill(52, ROOF + 2, z + 2, 88, ROOF + 2, z + 2, slab("smooth_stone", "top"));
            for (int y = ROOF + 1; y <= ROOF + 2; y++) {
                set(51, y, z + 1, ladder("west"));
            }
            set(52, ROOF + 1, z + 1, "light_gray_concrete");
            crate(70, ROOF + 3, z + 1, "south", "galeria-dach");
        }

        // ------------------------------------------------------------------ tył: rampy

        private void docks() {
            // daszek nad rampą centralną
            fill(64, 5, Z2 + 1, 78, 5, Z2 + 3, slab("smooth_stone", "top"));
            fill(68, 1, Z2, 73, 2, Z2, "air");
            fill(68, 3, Z2, 73, 3, Z2, "light_gray_concrete");
            for (int x = 68; x <= 73; x++) {
                set(x, 0, Z2 + 1, x % 2 == 0 ? "yellow_concrete" : "black_concrete");
            }
            // naczepa z otwartym tyłem i kabina ciągnika
            fill(60, 1, Z2 + 3, 72, 4, Z2 + 5, "white_concrete");
            fill(60, 1, Z2 + 4, 71, 3, Z2 + 4, "air");
            fill(60, 0, Z2 + 3, 72, 0, Z2 + 5, "gray_concrete");
            fill(73, 1, Z2 + 3, 75, 3, Z2 + 5, "red_concrete");
            fill(75, 2, Z2 + 3, 75, 2, Z2 + 5, "black_stained_glass");
            set(73, 1, Z2 + 4, "black_concrete");
            crate(65, 1, Z2 + 4, "west", "galeria-zaplecze");
            crate(70, 1, Z2 + 4, "west", "galeria-zaplecze");
            sign(66, 3, Z2 + 6, "south", "TRANSPORT", "KRAJOWY", "I ZAGRANICZNY", "");
            // kontenery na śmieci i beczki
            for (int x = 82; x <= 87; x += 3) {
                fill(x, 1, Z2 + 1, x + 1, 1, Z2 + 2, "green_terracotta");
                fill(x, 2, Z2 + 1, x + 1, 2, Z2 + 2, trapdoor("iron", "south", "bottom", false));
            }
            crate(80, 1, Z2 + 1, "south", "plac");
            crate(89, 1, Z2 + 1, "south", "plac");
            crate(30, 1, Z2 + 1, "south", "plac");
            crate(12, 1, Z2 + 1, "south", "paliwo");
            set(13, 1, Z2 + 1, KANISTRY);
            // wózek widłowy przy rampie Maximarketu
            set(26, 1, Z2 + 2, "yellow_concrete");
            set(27, 1, Z2 + 2, "yellow_concrete");
            fill(25, 1, Z2 + 2, 25, 3, Z2 + 2, "iron_bars");
            set(27, 2, Z2 + 2, stairs("blackstone", "west"));
            for (int x = 15; x <= 19; x++) {
                set(x, 1, Z2 + 1, chance(0.6) ? "barrel[facing=up,open=false]" : slab("spruce", "bottom"));
            }
        }

        // ------------------------------------------------------------------ skrzynie, strażnicy, historie

        /** Skrzynie poza sklepami: pasaż, parking, rampy. */
        private void crates() {
            crate(9, 1, 60, "east", "lodowki");
            crate(9, 1, 94, "east", "lodowki");
            crate(9, 1, 50, "east", "lodowki");
            for (int i = 0; i < 8; i++) {
                crateIn(10, 44, 23, 101, 1, "east", "galeria-hipermarket");
            }
            for (int i = 0; i < 4; i++) {
                crateIn(9, 43, 29, 101, 7, "east", "galeria-sklep");
            }
            for (int i = 0; i < 3; i++) {
                crateIn(9, 93, 29, 101, 13, "north", "galeria-sklep");
            }
            for (int i = 0; i < 3; i++) {
                crateIn(112, 57, 129, 86, 1, "west", "galeria-sklep");
            }
            crateIn(123, 89, 129, 99, 1, "west", "galeria-militaria");
            for (int i = 0; i < 4; i++) {
                crateIn(112, 56, 129, 101, 7, "west", "galeria-elektronika");
            }
            // pasaż
            crateIn(CX1, CZ1, CX2, CZ1 + 2, 1, "south", "galeria-sklep");
            crateIn(CX1, CZ2 - 2, CX2, CZ2, 1, "north", "galeria-sklep");
            crateIn(CX1, CZ1, CX2, CZ1 + 3, 7, "south", "galeria-sklep");
            crateIn(CX1, CZ2 - 3, CX2, CZ2, 13, "north", "galeria-sklep");
            // parking i garaż
            crate(36, 1, 30, "north", "plac");
            crate(100, 1, 12, "north", "plac");
            crate(127, 1, 34, "north", "paliwo");
            crate(13, 1, 36, "north", "plac");
            for (int i = 0; i < 6; i++) {
                crateIn(41, 41, 129, 101, GB + 1, "north", "galeria-parking");
            }
            crate(124, GB + 1, 45, "west", "paliwo");
            crate(127, GB + 1, 75, "west", "paliwo");
        }

        private void guards() {
            String b = "bandyta";
            // parter
            guard(69, 1, 60, "north", b);
            guard(40, 1, 70, "east", b);
            guard(102, 1, 83, "west", b);
            guard(19, 1, 76, "east", b);
            guard(117, 1, 80, "west", b);
            guard(126, 1, 94, "west", b);
            // garaż
            guard(60, GB + 1, 70, "north", b);
            guard(106, GB + 1, 60, "west", b);
            guard(16, GB + 1, 107, "north", "herszt");
            // 1 piętro
            guard(50, 7, 69, "south", b);
            guard(90, 7, 84, "north", b);
            guard(118, 7, 70, "west", b);
            guard(18, 7, 95, "east", b);
            // 2 piętro
            guard(78, 13, 74, "south", b);
            guard(118, 13, 77, "west", b);
            guard(121, 13, 58, "south", b);
            guard(24, 13, 96, "north", b);
            guard(105, 13, 57, "south", "herszt");
            guard(104, 13, 47, "south", b);
            // dach i obejście
            guard(56, ROOF + 1, 100, "north", b);
            guard(112, ROOF + 1, 62, "west", b);
            guard(86, 1, Z2 + 4, "south", b);
            guard(40, 1, 22, "north", b);
        }

        /** Tabliczki z anegdotami w miejscach, gdzie na pewno jest ściana. */
        private void stories() {
            f.graffiti(64, 47, 75, 66, 2, "TERYTORIUM", "KOMETY.", "KTO WEJDZIE,", "TEN PŁACI.");
            f.graffiti(43, 85, 53, 85, 2, "WSZYSTKO", "WYNIEŚLI.", "ZOSTAŁA", "KRATA.");
            f.graffiti(101, 68, 110, 68, 15, "OCHRONA", "PRZEJMUJE", "TEREN.", "– GANG KOMETY");
            f.graffiti(31, 41, 110, 43, 8, "KTO ZABRAŁ", "WÓZEK NR 12,", "NIECH ODDA.", "– OCHRONA");
            f.graffiti(102, 87, 109, 104, 8, "PAPIER", "PRZYNIEŚ", "SWÓJ.", "");
            f.graffiti(43, 87, 53, 102, 8, "ODDAWAĆ", "PRZEWINIĘTE!", "KARA 2 ZŁ", "ZA DOBĘ.");
            f.graffiti(40, 51, 50, 66, 2, "APTEKA", "OKRADZIONA", "TRZY RAZY.", "WIĘCEJ NIE MA.");
            f.graffiti(12, 41, 128, 101, GB + 2, "ZAJĘTE.", "TU MIESZKA", "ROMEK.", "");
            f.graffiti(102, 87, 109, 104, 14, "ZAJĘTE", "OD TRZECH LAT.", "", "");
        }

        private void lifeSigns() {
            f.traces(CX1, CZ1, CX2, CZ2, 1, 6, 6);
            f.traces(CX1, CZ1, CX2, CZ2, 7, 3, 4);
            f.traces(CX1, CZ1, CX2, CZ2, 13, 3, 4);
            f.traces(10, 44, 29, 101, 1, 3, 2);
            f.traces(112, 57, 129, 101, 1, 2, 3);
            f.traces(112, 87, 129, 101, 13, 2, 2);
            f.traces(12, 41, 128, 101, GB + 1, 4, 3);
            f.collapsedTent(41, 41, 128, 101, GB + 1, "blue");
            f.animalSomewhere(CX1, CZ1, CX2, CZ2, 1, "cat");
            f.animalSomewhere(12, 41, 128, 101, GB + 1, "cat");
        }

        /** Natura wchodzi przez dziury w świetliku: mech, trawa i paprocie na posadzce parteru. */
        private void overgrowth() {
            for (int[] h : skylightHoles) {
                int x = h[0];
                int z = h[1];
                if (x < VX1 || x > VX2 || z < VZ1 || z > VZ2) {
                    continue;
                }
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (!chance(0.5)) {
                            continue;
                        }
                        String below = get(x + dx, 0, z + dz);
                        if (isAir(x + dx, 1, z + dz) && (below.contains("diorite") || below.contains("calcite")
                                || below.contains("andesite"))) {
                            set(x + dx, 0, z + dz, chance(0.6) ? "moss_block" : "grass_block");
                            if (chance(0.5)) {
                                set(x + dx, 1, z + dz, pick("short_grass", "fern", "short_grass", "dandelion"));
                            }
                        }
                    }
                }
            }
        }
    }
}
