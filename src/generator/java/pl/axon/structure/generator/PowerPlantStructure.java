package pl.axon.structure.generator;

import pl.axon.structure.api.Entrance;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;
import pl.axon.structure.core.Traits;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Elektrownia Rybnica: zamknięta elektrownia węglowa z wysokim kominem, który widać z daleka.
 * Średnia struktura: na szczycie komina rzadka skrzynia, w środku szabrownicy.
 * <p>
 * Plan (front z = 0, wjazd z drogi w x 6-10; y = 0 to poziom terenu):
 * <pre>
 *  portiernia x 12-17, z 5-10       budynek socjalny x 24-40, z 6-12 (stołówka, toalety, szatnia, kierownik)
 *  rozdzielnia x 52-80, z 4-16 (trzy trafa, bramki, linie, budka łączników)
 *  hala maszyn x 22-49, z 22-41 (dwa turbozespoły, suwnica, nastawnia na antresoli)
 *  kotłownia x 50-63, z 18-45 (dwa kotły, pomosty y 9 i 18, właz na dach), czopuch do komina
 *  komin: środek x 73, z 31, wysokość 95, drabina od wschodu, pomost na szczycie
 *  chłodnia kominowa: środek x 24, z 63       skład węgla x 42-80, z 50-77, galeria taśmociągu do kotłowni
 * </pre>
 */
public final class PowerPlantStructure implements StructureDefinition {

    private static final int WIDTH = 84;
    private static final int DEPTH = 80;
    private static final int HEIGHT = 99;

    // komin
    private static final int CX = 73;
    private static final int CZ = 31;
    private static final int CHIMNEY_TOP = 94;
    // chłodnia
    private static final int TX = 24;
    private static final int TZ = 63;
    private static final int TOWER_TOP = 30;

    @Override
    public String id() {
        return "elektrownia";
    }

    @Override
    public String displayName() {
        return "Elektrownia Rybnica";
    }

    @Override
    public String description() {
        return "Zamknięta elektrownia węglowa: komin na 95 kratek, hala turbin z nastawnią, kotłownia, chłodnia, rozdzielnia i skład węgla z taśmociągiem";
    }

    @Override
    public Blueprint build(Random random) {
        Traits d = Traits.defaults();
        return new Builder(random).build().traits(new Traits(List.of(new Entrance(8, 0, "north")), d.mapAllowed(),
                d.sizeClass(), Set.of("rownina"), d.climates(), d.safe(), d.tags()));
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);
        private final Furniture f;
        private final Palette yard;
        private final Palette asphalt;
        private final Palette brick;
        private final Palette concrete;
        private final Palette floor;
        private final Palette coal;
        private final Palette tower;

        Builder(Random rnd) {
            this.rnd = rnd;
            this.f = new Furniture(bp, 0, rnd);
            yard = new Palette(rnd)
                    .add("grass_block", 9).add("coarse_dirt", 4).add("gravel", 2).add("dirt", 1);
            asphalt = new Palette(rnd)
                    .add("gray_concrete_powder", 6).add("andesite", 3).add("gravel", 2).add("cobblestone", 1).add("coarse_dirt", 1);
            brick = new Palette(rnd)
                    .add("bricks", 8).add("mud_bricks", 1).add("granite", 1);
            concrete = new Palette(rnd)
                    .add("light_gray_concrete", 7).add("gray_concrete", 2).add("andesite", 1).add("light_gray_concrete_powder", 1);
            floor = new Palette(rnd)
                    .add("smooth_stone", 6).add("polished_andesite", 3).add("gray_concrete", 1).add("cobblestone", 1);
            coal = new Palette(rnd)
                    .add("coal_block", 5).add("black_concrete_powder", 4).add("blackstone", 1).add("gravel", 1);
            tower = new Palette(rnd)
                    .add("light_gray_concrete", 12).add("light_gray_concrete_powder", 2).add("gray_concrete", 1);
        }

        Blueprint build() {
            ground();
            roads();
            fence();
            gatehouse();
            socialBuilding();
            switchyard();
            turbineHall();
            controlRoom();
            boilerHouse();
            flueDuct();
            chimney();
            coolingTower();
            coalYard();
            conveyor();
            yardDetails();
            return bp;
        }

        // ------------------------------------------------------------------ pomocnicze

        private void set(int x, int y, int z, String state) {
            bp.set(x, y, z, state);
        }

        private boolean isAir(int x, int y, int z) {
            return bp.isAir(x, y, z);
        }

        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
            bp.fill(x1, y1, z1, x2, y2, z2, state);
        }

        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Palette palette) {
            bp.fill(x1, y1, z1, x2, y2, z2, palette);
        }

        private void crate(int x, int y, int z, String facing, String pool) {
            bp.crate(x, y, z, facing, pool);
        }

        private void sign(int x, int y, int z, String facing, String... lines) {
            bp.sign(x, y, z, "spruce_wall_sign[facing=" + facing + ",waterlogged=false]", lines);
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
            return chance(0.25) ? "gray_stained_glass_pane" : "glass_pane";
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

        /** Rząd okien w ścianie wzdłuż x (z stałe) albo wzdłuż z (x stałe), co {@code step} kratek. */
        private void windows(int fixed, boolean alongX, int from, int to, int y1, int y2, int step) {
            for (int i = from; i <= to; i += step) {
                for (int y = y1; y <= y2; y++) {
                    if (alongX) {
                        set(i, y, fixed, brokenGlass());
                    } else {
                        set(fixed, y, i, brokenGlass());
                    }
                }
            }
        }

        private static boolean inDisk(int dx, int dz, double r) {
            return dx * dx + dz * dz <= (r + 0.5) * (r + 0.5);
        }

        /** Kopiec (węgla, gruzu) o promieniu r i wysokości h. */
        private void mound(int cx, int cz, int r, int h, Palette palette) {
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)) / (r + 0.5);
                    if (d > 1) {
                        continue;
                    }
                    int top = (int) Math.round(h * (1 - d * d) + rnd.nextDouble() * 0.6);
                    for (int y = 1; y <= top; y++) {
                        set(x, y, z, palette.next());
                    }
                }
            }
        }

        // ------------------------------------------------------------------ teren, drogi, płot

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    String block = yard.next();
                    set(x, 0, z, block);
                    if (block.equals("grass_block") && chance(0.25)) {
                        set(x, 1, z, chance(0.8) ? "short_grass" : chance(0.6) ? "dead_bush" : "fern");
                    }
                }
            }
        }

        private void road(int x1, int z1, int x2, int z2) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 0, z, asphalt.next());
                    set(x, 1, z, chance(0.04) ? "short_grass" : "air");
                }
            }
        }

        private void roads() {
            // wjazd z drogi i główna droga wzdłuż zachodniej strony
            road(6, 0, 10, 48);
            // do bramy hali maszyn
            road(11, 29, 21, 33);
            // przed budynkiem socjalnym do rozdzielni
            road(11, 13, 51, 15);
            // tylna droga: chłodnia, skład węgla, kotłownia
            road(11, 46, 80, 48);
            // pas przy bramie: biało-czerwone pasy na wjeździe
            for (int x = 6; x <= 10; x++) {
                set(x, 0, 1, x % 2 == 0 ? "white_concrete" : "red_concrete");
            }
        }

        private void fence() {
            for (int x = 1; x <= 82; x++) {
                if (x < 6 || x > 10) {
                    fencePost(x, 4, x % 5 == 0);
                }
                fencePost(x, 78, x % 5 == 0);
            }
            for (int z = 5; z <= 77; z++) {
                fencePost(1, z, z % 5 == 0);
                fencePost(82, z, z % 5 == 0);
            }
            // wyrwy w płocie
            for (int[] gap : new int[][]{{1, 40}, {1, 41}, {30, 78}, {31, 78}, {82, 60}, {82, 61}, {82, 62}, {47, 4}}) {
                fill(gap[0], 1, gap[1], gap[0], 3, gap[1], "air");
            }
            // słupki bramy i szlaban leżący na ziemi
            fill(5, 1, 4, 5, 3, 4, "stone_bricks");
            fill(11, 1, 4, 11, 3, 4, "stone_bricks");
            set(11, 4, 4, "lantern[hanging=false,waterlogged=false]");
            for (int x = 6; x <= 10; x++) {
                set(x, 1, 3, x % 2 == 0 ? "red_carpet" : "white_carpet");
            }
            sign(5, 2, 3, "north", "ELEKTROWNIA", "RYBNICA", "", "");
            sign(11, 2, 3, "north", "PRZEPUSTKI", "POKAZYWAĆ", "BEZ PYTANIA", "");
        }

        private void fencePost(int x, int z, boolean post) {
            if (post) {
                fill(x, 1, z, x, 2, z, "stone_bricks");
                set(x, 3, z, chance(0.8) ? "iron_bars" : "air");
            } else {
                fill(x, 1, z, x, 2, z, "andesite_wall");
                if (chance(0.5)) {
                    set(x, 3, z, "iron_bars");
                }
            }
        }

        // ------------------------------------------------------------------ portiernia

        private void gatehouse() {
            Palette wall = new Palette(rnd).add("white_terracotta", 6).add("light_gray_concrete", 2).add("white_concrete", 1);
            fill(12, 0, 5, 17, 0, 10, floor);
            fill(13, 1, 6, 16, 3, 9, "air");
            walls(12, 5, 17, 10, 1, 3, wall);
            fill(12, 4, 5, 17, 4, 10, "smooth_stone_slab[type=bottom,waterlogged=false]");
            // okno na wjazd i na plac
            fill(12, 2, 6, 12, 2, 9, "air");
            for (int z = 6; z <= 9; z++) {
                set(12, 2, z, brokenGlass());
            }
            set(14, 2, 5, brokenGlass());
            set(15, 2, 5, brokenGlass());
            door(15, 1, 10, "iron_door", "north", "left", true);
            // biurko portiera
            f.desk(13, 1, 7, 13, 9, "spruce");
            f.chair(14, 1, 8, "east", "spruce");
            set(13, 2, 9, "lantern[hanging=false,waterlogged=false]");
            crate(16, 1, 6, "west", "parter");
            sign(14, 2, 11, "south", "PORTIER", "WYSZEDŁ", "WRÓCI", "");
            bp.crate(16, 1, 9, "west", "npc-szabrownik");
        }

        // ------------------------------------------------------------------ budynek socjalny

        private void socialBuilding() {
            int x1 = 24;
            int x2 = 40;
            int z1 = 6;
            int z2 = 12;
            Palette wall = new Palette(rnd).add("white_terracotta", 5).add("light_gray_concrete", 2).add("smooth_stone", 1);
            fill(x1, 0, z1, x2, 0, z2, floor);
            fill(x1 + 1, 1, z1 + 1, x2 - 1, 9, z2 - 1, "air");
            walls(x1, z1, x2, z2, 1, 9, wall);
            // niebieski pas między piętrami
            for (int x = x1; x <= x2; x++) {
                set(x, 5, z1, "light_blue_terracotta");
                set(x, 5, z2, "light_blue_terracotta");
            }
            for (int z = z1; z <= z2; z++) {
                set(x1, 5, z, "light_blue_terracotta");
                set(x2, 5, z, "light_blue_terracotta");
            }
            // strop i dach (z dziurą w dachu)
            fill(x1 + 1, 5, z1 + 1, x2 - 1, 5, z2 - 1, floor);
            fill(x1, 10, z1, x2, 10, z2, "gray_concrete");
            fill(26, 10, 8, 27, 10, 9, "air");
            set(26, 6, 8, "cobblestone");
            // okna
            windows(z1, true, 26, 38, 2, 3, 3);
            windows(z2, true, 26, 38, 7, 8, 3);
            windows(z1, true, 26, 38, 7, 8, 3);
            windows(z2, true, 27, 27, 2, 3, 1);
            windows(z2, true, 37, 38, 2, 3, 1);
            // wejście od drogi
            door(32, 1, z2, "iron_door", "south", "left", true);
            sign(31, 3, z2 + 1, "south", "STOŁÓWKA", "", "", "");
            // ścianki działowe z przejściem
            fill(35, 1, z1 + 1, 35, 4, z2 - 1, "white_terracotta");
            fill(35, 6, z1 + 1, 35, 9, z2 - 1, "white_terracotta");
            fill(35, 1, 8, 35, 2, 8, "air");
            fill(35, 6, 8, 35, 7, 8, "air");
            // schody na piętro (ostatni stopień w poziomie stropu) i otwór w stropie
            for (int i = 0; i < 5; i++) {
                set(29 + i, 1 + i, 11, "stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
                if (i > 0) {
                    fill(29 + i, 1, 11, 29 + i, i, 11, "stone_bricks");
                }
            }
            fill(29, 5, 11, 32, 5, 11, "air");
            for (int x = 29; x <= 32; x++) {
                set(x, 6, 10, "iron_bars");
            }

            // parter: stołówka z ladą
            fill(25, 1, 7, 29, 1, 7, "polished_andesite");
            for (int x = 25; x <= 29; x++) {
                set(x, 2, 7, x == 27 ? "air" : "smooth_stone_slab[type=bottom,waterlogged=false]");
            }
            crate(27, 2, 7, "south", "lada");
            crate(25, 1, 8, "east", "lodowki");
            f.stove(29, 1, 8, "south");
            f.dining(26, 1, 10, true, "spruce");
            f.dining(31, 1, 9, false, "spruce");
            set(34, 1, 7, "respawn_anchor[charges=0]");
            sign(25, 3, 8, "east", "KAWA SIĘ", "SKOŃCZYŁA", "RAZEM", "Z PRĄDEM");
            bp.crate(30, 1, 8, "south", "npc-szabrownik");
            // toalety
            set(37, 1, 7, "end_portal_frame[eye=false,facing=south]");
            set(39, 1, 7, "end_portal_frame[eye=false,facing=south]");
            f.sink(37, 1, 11, "south");
            f.sink(39, 1, 11, "south");
            crate(39, 1, 9, "west", "toalety");
            sign(38, 2, 7, "south", "SPŁUCZKA", "NIE DZIAŁA", "OD ZAWSZE", "");

            // piętro: szatnia z szafkami
            for (int x = 25; x <= 33; x += 2) {
                f.wardrobe(x, 6, 7, "south", "spruce");
            }
            crate(34, 6, 7, "south", "parter");
            f.bench(26, 6, 9, 4, true, "south", "spruce");
            f.bed(26, 6, 11, "east", "brown");
            set(27, 6, 10, "campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]");
            set(25, 6, 11, "respawn_anchor[charges=0]");
            sign(25, 7, 10, "east", "TU ŚPI", "MIETEK", "NIE BUDZIĆ", "BO GRYZIE");
            // gabinet kierownika
            f.desk(37, 6, 10, 39, 10, "dark_oak");
            f.chair(38, 6, 9, "north", "dark_oak");
            f.bookshelf(39, 6, 7, 39, 8, 2, "west");
            crate(36, 6, 11, "north", "pietro");
            sign(36, 7, 7, "south", "PREMIA", "KWARTALNA", "WSTRZYMANA", "– KIEROWNIK");
        }

        // ------------------------------------------------------------------ rozdzielnia

        private void switchyard() {
            int x1 = 52;
            int x2 = 80;
            int z1 = 5;
            int z2 = 17;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 0, z, chance(0.8) ? "gravel" : "andesite");
                    set(x, 1, z, "air");
                }
            }
            // ogrodzenie z siatki, wejście od drogi
            for (int x = x1; x <= x2; x++) {
                set(x, 1, z2, "iron_bars");
                set(x, 2, z2, "iron_bars");
            }
            for (int z = z1; z <= z2; z++) {
                if (z < 13 || z > 15) {
                    set(x1, 1, z, "iron_bars");
                    set(x1, 2, z, "iron_bars");
                }
            }
            set(66, 1, z2, "air");
            set(66, 2, z2, "air");
            fill(x1, 1, 12, x1, 3, 12, "stone_bricks");
            fill(x1, 1, 16, x1, 3, 16, "stone_bricks");
            sign(x1 - 1, 2, 12, "west", "NIE DOTYKAĆ", "PRĄD ZABIJA", "", "");
            sign(x1 - 1, 2, 16, "west", "PRĄDU NIE MA", "OD DAWNA,", "SPRAWDZAŁEM", "– HENIEK");

            transformer(56, 8, false);
            transformer(63, 8, false);
            transformer(70, 8, true);
            // bramki liniowe z trzema fazami
            for (int x : new int[]{54, 78}) {
                fill(x, 1, 7, x, 10, 7, "iron_bars");
                fill(x, 1, 13, x, 10, 13, "iron_bars");
                fill(x, 11, 7, x, 11, 13, "iron_bars");
                for (int z = 8; z <= 12; z += 2) {
                    set(x, 10, z, "lightning_rod[facing=down,powered=false,waterlogged=false]");
                }
            }
            for (int z = 8; z <= 12; z += 2) {
                for (int x = 55; x <= 77; x++) {
                    if (!(z == 12 && x >= 70 && x <= 73)) {
                        set(x, 9, z, "chain[axis=x,waterlogged=false]");
                    }
                }
            }
            // zerwany przewód zwisa do ziemi
            fill(73, 2, 12, 73, 8, 12, "chain[axis=y,waterlogged=false]");
            crate(60, 1, 6, "south", "paliwo");
            crate(67, 1, 6, "south", "paliwo");
            set(61, 1, 6, "respawn_anchor[charges=0]");

            // budka łączników
            Palette wall = new Palette(rnd).add("bricks", 5).add("light_gray_concrete", 1);
            walls(58, 13, 64, 16, 1, 4, wall);
            fill(58, 5, 13, 64, 5, 16, "smooth_stone_slab[type=bottom,waterlogged=false]");
            fill(59, 0, 14, 63, 0, 15, floor);
            door(61, 1, 13, "iron_door", "north", "left", true);
            set(63, 2, 13, brokenGlass());
            for (int x = 60; x <= 63; x++) {
                set(x, 1, 15, "sticky_piston[extended=false,facing=north]");
            }
            crate(59, 1, 14, "east", "magazyn-regal");
            sign(61, 3, 12, "north", "KTO UKRADŁ", "MIEDŹ Z", "TRAFA NR 3", "ZNAJDĘ CIĘ");
            bp.crate(66, 1, 14, "north", "npc-szabrownik");
        }

        /** Transformator 3 × 3 z żebrami chłodnicy i izolatorami; obdarty z miedzi stoi otwarty. */
        private void transformer(int x, int z, boolean stripped) {
            fill(x - 1, 0, z - 1, x + 3, 0, z + 3, "smooth_stone");
            for (int y = 1; y <= 3; y++) {
                for (int dx = 0; dx <= 2; dx++) {
                    for (int dz = 0; dz <= 2; dz++) {
                        boolean hole = stripped && (dx == 1 || y == 3) && dz < 2 && chance(0.7);
                        set(x + dx, y, z + dz, hole ? "air" : y == 3 ? "light_gray_concrete" : "gray_concrete");
                    }
                }
            }
            for (int dz = 0; dz <= 2; dz++) {
                set(x - 1, 1, z + dz, "iron_bars");
                set(x - 1, 2, z + dz, "iron_bars");
                if (!stripped) {
                    set(x + 3, 1, z + dz, "iron_bars");
                    set(x + 3, 2, z + dz, "iron_bars");
                }
            }
            if (stripped) {
                set(x + 1, 1, z + 1, "exposed_copper");
                set(x + 3, 1, z + 4, "weathered_cut_copper_slab[type=bottom,waterlogged=false]");
                set(x + 4, 1, z + 3, "copper_bulb[lit=false,powered=false]");
                return;
            }
            for (int dz = 0; dz <= 2; dz++) {
                set(x + 1, 4, z + dz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            }
            set(x, 4, z + 2, "smooth_stone_slab[type=bottom,waterlogged=false]");
            set(x + 2, 4, z + 2, "smooth_stone_slab[type=bottom,waterlogged=false]");
        }

        // ------------------------------------------------------------------ hala maszyn

        private void turbineHall() {
            int x1 = 22;
            int x2 = 49;
            int z1 = 22;
            int z2 = 41;
            fill(x1, 0, z1, x2, 0, z2, floor);
            fill(x1 + 1, 1, z1 + 1, x2 - 1, 14, z2 - 1, "air");
            walls(x1, z1, x2, z2, 1, 3, brick);
            walls(x1, z1, x2, z2, 4, 11, concrete);
            walls(x1, z1, x2, z2, 12, 14, concrete);
            // pilastry i wysokie okna
            for (int x = x1 + 2; x < x2 - 1; x += 4) {
                for (int y = 4; y <= 11; y++) {
                    for (int dx = 0; dx <= 2; dx++) {
                        set(x + dx, y, z1, brokenGlass());
                        set(x + dx, y, z2, brokenGlass());
                    }
                }
            }
            // dach z świetlikami i zawaloną częścią
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean skylight = (x - x1) % 6 == 3 && z > z1 + 1 && z < z2 - 1;
                    boolean collapsed = x >= 28 && x <= 33 && z >= 32 && z <= 36 && chance(0.85);
                    set(x, 15, z, collapsed ? "air" : skylight ? brokenGlassBlock() : chance(0.06) ? "gray_concrete_powder" : "gray_concrete");
                }
            }
            for (int i = 0; i < 18; i++) {
                int x = 28 + rnd.nextInt(6);
                int z = 32 + rnd.nextInt(5);
                if (isAir(x, 1, z) || bp.get(x, 1, z).contains("grass")) {
                    set(x, 1, z, chance(0.5) ? "gray_concrete" : "cobblestone");
                }
            }
            // brama od drogi (otwarta, bez skrzydeł) i drzwi do kotłowni
            fill(x1, 1, 29, x1, 6, 33, "air");
            fill(x1, 7, 29, x1, 7, 33, "yellow_concrete");
            sign(x1 - 1, 7, 28, "west", "HALA", "MASZYN", "", "");

            turbine(26);
            turbine(31);
            // chodnik pod suwnicą wzdłuż północnej ściany z drabiną
            fill(x1 + 1, 10, z1 + 1, x2 - 1, 10, z1 + 2, "polished_andesite");
            for (int x = x1 + 1; x <= x2 - 1; x++) {
                if (x != 48) {
                    set(x, 11, z1 + 3, "iron_bars");
                }
            }
            for (int y = 1; y <= 11; y++) {
                set(48, y, z1 + 3, "ladder[facing=west,waterlogged=false]");
            }
            // tory suwnicy i suwnica z hakiem
            fill(x1 + 1, 11, z1 + 1, x2 - 1, 11, z1 + 1, "yellow_terracotta");
            fill(x1 + 1, 11, z2 - 1, x2 - 1, 11, z2 - 1, "yellow_terracotta");
            // most suwnicy między torami, wskakuje się na niego z chodnika
            fill(40, 11, z1 + 2, 40, 11, z2 - 2, "yellow_concrete");
            set(40, 11, 31, "iron_block");
            fill(40, 8, 31, 40, 10, 31, "chain[axis=y,waterlogged=false]");
            set(40, 7, 31, "lightning_rod[facing=down,powered=false,waterlogged=false]");
            crate(40, 12, 29, "west", "suwnica");
            // tablice i resztki po zmianie
            sign(x2 - 1, 3, 35, "west", "BHP", "DNI BEZ", "WYPADKU:", "0");
            sign(x1 + 1, 3, 25, "east", "BLOK 2", "NIE RUSZAĆ", "BO WYBUCHNIE", "– INŻ. KOWALIK");
            crate(24, 1, 24, "east", "plac");
            crate(46, 1, 39, "west", "plac");
            set(47, 1, 39, "respawn_anchor[charges=0]");
            set(45, 1, 24, "sticky_piston[extended=false,facing=south]");
            set(46, 1, 24, "sticky_piston[extended=false,facing=south]");
            f.skeleton(43, 1, 29, "east");
            bp.crate(34, 1, 30, "east", "npc-szabrownik");
        }

        private String brokenGlassBlock() {
            return chance(0.5) ? "air" : chance(0.6) ? "glass" : "tinted_glass";
        }

        /** Turbozespół wzdłuż x: fundament, zielony kadłub turbiny i jasny generator; z0 to pierwszy rząd. */
        private void turbine(int z0) {
            fill(25, 1, z0, 42, 1, z0 + 3, "smooth_stone");
            for (int x = 26; x <= 41; x++) {
                boolean generator = x >= 37;
                String body = generator ? "light_gray_concrete" : "green_terracotta";
                for (int dz = 0; dz <= 3; dz++) {
                    set(x, 2, z0 + dz, body);
                    set(x, 3, z0 + dz, body);
                }
                set(x, 4, z0 + 1, generator ? "iron_block" : body);
                set(x, 4, z0 + 2, generator ? "iron_block" : body);
                if (x % 3 == 0 && !generator) {
                    set(x, 4, z0, "smooth_stone_slab[type=bottom,waterlogged=false]");
                    set(x, 4, z0 + 3, "smooth_stone_slab[type=bottom,waterlogged=false]");
                }
            }
            // wymontowany wirnik: dziura w kadłubie
            if (z0 == 31) {
                fill(30, 3, 32, 33, 4, 33, "air");
                fill(30, 2, 32, 33, 2, 33, "black_concrete");
            }
            // rury parowe z kotłowni
            fill(42, 5, z0 + 1, 48, 5, z0 + 1, "light_gray_terracotta");
            fill(42, 2, z0 + 1, 42, 4, z0 + 1, "light_gray_terracotta");
        }

        // ------------------------------------------------------------------ nastawnia

        private void controlRoom() {
            // antresola nad warsztatem przy południowej ścianie hali
            fill(23, 5, 36, 37, 5, 40, "polished_andesite");
            for (int x = 23; x <= 36; x++) {
                set(x, 6, 36, "white_terracotta");
                set(x, 7, 36, brokenGlass());
                set(x, 8, 36, brokenGlass());
                set(x, 9, 36, "white_terracotta");
            }
            fill(36, 6, 37, 36, 9, 40, "white_terracotta");
            fill(23, 9, 37, 35, 9, 40, "smooth_stone_slab[type=top,waterlogged=false]");
            door(36, 6, 38, "iron_door", "west", "left", true);
            // schody z hali
            for (int i = 0; i < 5; i++) {
                int x = 41 - i;
                set(x, 1 + i, 38, "stone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
                set(x, 1 + i, 39, "stone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
                if (i > 0) {
                    fill(x, 1, 38, x, i, 39, "stone_bricks");
                }
            }
            fill(37, 6, 37, 37, 6, 37, "iron_bars");
            // pulpity sterownicze
            for (int x = 24; x <= 34; x++) {
                set(x, 6, 37, (x % 4 == 0) ? "redstone_lamp[lit=false]" : "cyan_terracotta");
                if (x % 2 == 1) {
                    set(x, 7, 37, "lever[face=floor,facing=south,powered=" + chance(0.5) + "]");
                }
            }
            for (int x = 24; x <= 34; x += 2) {
                set(x, 6, 40, "sticky_piston[extended=false,facing=north]");
            }
            // radiostacja dyspozytora na pulpicie (stanowisko GUI: namiar na najbliższy zrzut)
            bp.crate(29, 6, 37, "south", "punkt-radio");
            set(29, 6, 37, "cyan_terracotta");
            set(29, 7, 37, "lightning_rod[facing=up,powered=false,waterlogged=false]");
            f.chair(27, 6, 38, "south", "spruce");
            f.chair(31, 6, 38, "south", "spruce");
            crate(25, 6, 39, "north", "maszynownia");
            crate(33, 6, 39, "north", "maszynownia");
            sign(35, 7, 39, "west", "OSTATNI", "WYCHODZI", "GASI ŚWIATŁO", "");
            sign(35, 7, 37, "west", "WYŁĄCZYŁEM.", "NIE BYŁO", "KOMU ZAPALIĆ", "");
            // warsztat pod antresolą
            for (int x = 24; x <= 34; x += 3) {
                f.desk(x, 1, 40, x + 1, 40, "spruce");
            }
            crate(26, 1, 37, "south", "magazyn-regal");
            crate(32, 1, 37, "south", "magazyn-regal");
            set(23, 1, 40, "anvil[facing=north]");
            set(23, 1, 37, "grindstone[face=floor,facing=north]");
            f.hangingLamp(29, 4, 38);
        }

        // ------------------------------------------------------------------ kotłownia

        private void boilerHouse() {
            int x1 = 50;
            int x2 = 63;
            int z1 = 18;
            int z2 = 45;
            Palette wall = new Palette(rnd).add("gray_concrete", 8).add("andesite", 1).add("gray_concrete_powder", 1);
            fill(x1, 0, z1, x2, 0, z2, floor);
            fill(x1 + 1, 1, z1 + 1, x2 - 1, 26, z2 - 1, "air");
            walls(x1, z1, x2, z2, 1, 26, wall);
            walls(x1, z1, x2, z2, 1, 4, brick);
            walls(x1, z1, x2, z2, 26, 26, concrete);
            fill(x1, 27, z1, x2, 27, z2, "gray_concrete");
            for (int y = 12; y <= 24; y += 9) {
                windows(z1, true, 52, 61, y, y + 2, 3);
                windows(x2, false, 21, 42, y, y + 2, 3);
            }
            // przejście do hali maszyn
            fill(49, 1, 31, 50, 3, 32, "air");
            // brama od tylnej drogi
            fill(53, 1, z2, 55, 4, z2, "air");
            sign(56, 3, z2 + 1, "south", "KOTŁOWNIA", "", "", "");

            boiler(21);
            boiler(36);
            // pomosty z kraty wokół kotłów, kładka między nimi
            for (int y : new int[]{9, 18}) {
                for (int x = x1 + 1; x <= x2 - 1; x++) {
                    for (int z = z1 + 1; z <= z2 - 1; z++) {
                        boolean ring = x <= 52 || x >= 61 || z <= 20 || z >= 43;
                        boolean bridge = z >= 30 && z <= 33;
                        if ((ring || bridge) && isAir(x, y, z)) {
                            set(x, y, z, "waxed_weathered_copper_grate[waterlogged=false]");
                        }
                    }
                }
                for (int x = 53; x <= 60; x++) {
                    set(x, y + 1, 30, "iron_bars");
                    set(x, y + 1, 33, "iron_bars");
                }
            }
            // dziura w pomoście (spadło coś ciężkiego)
            fill(57, 9, 43, 59, 9, 44, "air");
            // drabina przez oba pomosty na dach
            for (int y = 1; y <= 27; y++) {
                set(62, y, 31, "ladder[facing=west,waterlogged=false]");
            }
            crate(51, 1, 44, "east", "magazyn");
            crate(51, 1, 19, "east", "plac");
            crate(52, 10, 44, "east", "zaplecze");
            crate(61, 19, 19, "west", "suwnica");
            crate(55, 28, 25, "south", "dach");
            set(51, 10, 19, "respawn_anchor[charges=0]");
            sign(61, 11, 44, "north", "KTO UKRADŁ", "MOJE", "KANISTRY?", "– STASIEK");
            sign(54, 20, 28, "south", "KOCIOŁ OP-230", "CIŚNIENIE:", "NIE MA", "");
            bp.crate(57, 19, 31, "east", "npc-szabrownik");
            // hałda węgla przy palenisku
            mound(56, 31, 2, 2, coal);
        }

        /** Kocioł parowy x 53-60, z z0..z0+6: obmurze z pasami, palenisko z drzwiczkami. */
        private void boiler(int z0) {
            Palette shell = new Palette(rnd).add("polished_blackstone", 4).add("gray_terracotta", 3).add("blackstone", 1);
            for (int y = 1; y <= 22; y++) {
                for (int x = 53; x <= 60; x++) {
                    for (int z = z0; z <= z0 + 6; z++) {
                        boolean edge = x == 53 || x == 60 || z == z0 || z == z0 + 6;
                        if (!edge) {
                            continue;
                        }
                        set(x, y, z, y % 6 == 0 ? "polished_blackstone_bricks" : shell.next());
                    }
                }
            }
            fill(54, 22, z0 + 1, 59, 22, z0 + 5, "polished_blackstone_slab[type=bottom,waterlogged=false]");
            // palenisko od strony kładki
            int front = z0 < 30 ? z0 + 6 : z0;
            for (int x = 55; x <= 58; x++) {
                set(x, 1, front, "blast_furnace[facing=" + (z0 < 30 ? "south" : "north") + ",lit=false]");
            }
            fill(54, 1, z0 + 1, 59, 2, z0 + 5, "magma_block");
            fill(54, 3, z0 + 1, 59, 21, z0 + 5, "air");
            // rury do hali
            fill(51, 14, z0 + 3, 52, 14, z0 + 3, "light_gray_terracotta");
        }

        // ------------------------------------------------------------------ czopuch i komin

        private void flueDuct() {
            fill(64, 20, 30, 68, 22, 32, "gray_concrete");
            fill(64, 21, 31, 69, 21, 31, "air");
            for (int x = 65; x <= 67; x += 2) {
                fill(x, 1, 31, x, 19, 31, "iron_bars");
            }
        }

        private void chimney() {
            for (int y = 1; y <= CHIMNEY_TOP; y++) {
                int r = radius(y);
                boolean band = y >= 70 && ((y - 70) / 4) % 2 == 0;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (!inDisk(dx, dz, r) || inDisk(dx, dz, r - 1)) {
                            continue;
                        }
                        String block = y >= 70 ? (band ? "red_concrete" : "white_concrete") : brick.next();
                        if (y >= 92 && chance(0.15)) {
                            block = "black_concrete";
                        }
                        set(CX + dx, y, CZ + dz, block);
                    }
                }
                // sadza na dnie i wnętrze
                for (int dx = -r + 1; dx <= r - 1; dx++) {
                    for (int dz = -r + 1; dz <= r - 1; dz++) {
                        if (inDisk(dx, dz, r - 1)) {
                            set(CX + dx, y, CZ + dz, "air");
                        }
                    }
                }
            }
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (inDisk(dx, dz, 4)) {
                        set(CX + dx, 0, CZ + dz, chance(0.6) ? "black_concrete_powder" : "coal_block");
                    }
                }
            }
            // wlot czopucha
            fill(CX - 5, 21, CZ, CX - 4, 21, CZ, "air");
            // wejście od południa
            fill(CX, 1, CZ + 5, CX, 2, CZ + 5, "air");
            sign(CX + 1, 2, CZ + 6, "south", "WEJŚCIE NA", "KOMIN TYLKO", "Z UPRAWNIENIEM", "");
            // drabina od wschodu, przesuwa się na każdym zwężeniu
            for (int y = 1; y <= CHIMNEY_TOP - 4; y++) {
                set(CX + radius(y) + 1, y, CZ, "ladder[facing=east,waterlogged=false]");
            }
            sign(CX + 6, 3, CZ + 1, "east", "95 METRÓW", "NIE PATRZ", "W DÓŁ", "");
            sign(CX + 5, 35, CZ + 1, "east", "TU ZAWRÓCIŁ", "ZBYSZEK", "", "");
            // pomost na szczycie z barierką
            int py = CHIMNEY_TOP - 4;
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    if (!inDisk(dx, dz, 5) || inDisk(dx, dz, 3)) {
                        continue;
                    }
                    if (dx == 4 && dz == 0) {
                        continue;
                    }
                    set(CX + dx, py, CZ + dz, "polished_andesite");
                    if (!inDisk(dx, dz, 4)) {
                        set(CX + dx, py + 1, CZ + dz, "iron_bars");
                    }
                }
            }
            set(CX + 4, py, CZ, "ladder[facing=east,waterlogged=false]");
            set(CX + 5, py + 1, CZ, "air");
            crate(CX - 4, py + 1, CZ, "west", "komin");
            // światła ostrzegawcze i odgromniki na koronie
            for (int[] p : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
                set(CX + p[0], CHIMNEY_TOP + 1, CZ + p[1], "lightning_rod[facing=up,powered=false,waterlogged=false]");
                if (p[0] == 3) {
                    continue; // nad wyjściem z drabiny
                }
                set(CX + p[0] * 4 / 3, py + 2, CZ + p[1] * 4 / 3, "redstone_lamp[lit=false]");
            }
        }

        private static int radius(int y) {
            return y <= 30 ? 5 : y <= 65 ? 4 : 3;
        }

        // ------------------------------------------------------------------ chłodnia kominowa

        private void coolingTower() {
            for (int y = 0; y <= TOWER_TOP; y++) {
                double t = (y - 18) / 18.0;
                double r = 8 + 4 * t * t;
                int ri = (int) Math.ceil(r) + 1;
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        int x = TX + dx;
                        int z = TZ + dz;
                        if (y == 0) {
                            if (d <= r) {
                                set(x, 0, z, d < 4 && chance(0.5) ? "water" : chance(0.5) ? "mud" : "packed_mud");
                            }
                            continue;
                        }
                        if (d > r + 0.5 || d <= r - 0.5) {
                            if (d <= r - 0.5) {
                                set(x, y, z, "air");
                            }
                            continue;
                        }
                        double angle = Math.toDegrees(Math.atan2(dz, dx));
                        boolean leg = Math.floorMod(Math.round(angle / 15), 2) == 0;
                        if (y <= 3 && !leg) {
                            set(x, y, z, "air");
                            continue;
                        }
                        // wyłom w koronie od północnego wschodu
                        boolean breach = angle > -80 && angle < -20 && y > 21 + Math.abs(angle + 50) / 6 + rnd.nextInt(2);
                        if (breach) {
                            continue;
                        }
                        set(x, y, z, y <= 3 ? "gray_concrete" : y == TOWER_TOP ? "smooth_stone" : tower.next());
                    }
                }
            }
            // gruz z wyłomu
            mound(TX + 7, TZ - 7, 2, 1, new Palette(rnd).add("light_gray_concrete", 3).add("gravel", 2));
            // obozowisko w środku
            f.camp(TX - 4, 1, TZ + 4, "south");
            crate(TX - 6, 1, TZ + 2, "east", "zaplecze");
            crate(TX + 2, 1, TZ + 6, "north", "plac");
            set(TX + 3, 1, TZ + 6, "respawn_anchor[charges=0]");
            set(TX, 0, TZ, "water");
            set(TX, 1, TZ, "lily_pad");
            fill(TX - 8, 1, TZ + 3, TX - 8, 2, TZ + 3, "stone_bricks");
            bp.crate(TX - 2, 1, TZ + 2, "east", "npc-szabrownik");
            sign(TX - 7, 2, TZ + 3, "east", "ECHO TU", "ODPOWIADA", "SZYBCIEJ", "NIŻ ŻONA");
        }

        // ------------------------------------------------------------------ skład węgla i taśmociąg

        private void coalYard() {
            for (int x = 42; x <= 80; x++) {
                for (int z = 50; z <= 77; z++) {
                    if (chance(0.7)) {
                        set(x, 0, z, chance(0.6) ? "black_concrete_powder" : chance(0.5) ? "gravel" : "coarse_dirt");
                        set(x, 1, z, "air");
                    }
                }
            }
            mound(48, 58, 5, 4, coal);
            mound(71, 66, 7, 6, coal);
            mound(47, 72, 4, 3, coal);
            // lej przyjęciowy pod taśmociągiem
            fill(55, 0, 73, 59, 0, 76, "gray_concrete");
            fill(56, 1, 74, 58, 1, 75, "hopper[enabled=true,facing=down]");
            // spychacz bez gąsienicy
            fill(64, 1, 55, 67, 1, 57, "yellow_concrete");
            fill(64, 2, 55, 65, 3, 57, "yellow_terracotta");
            set(64, 2, 56, "glass_pane");
            fill(68, 1, 55, 68, 2, 57, "iron_block");
            fill(64, 0, 54, 67, 0, 54, "black_concrete");
            crate(66, 2, 56, "north", "paliwo");
            set(63, 1, 58, "respawn_anchor[charges=0]");
            crate(62, 1, 58, "west", "plac");
            crate(77, 1, 52, "west", "plac");
            sign(55, 2, 77, "north", "WĘGIEL", "LICZONY!", "KAŻDĄ BRYŁĘ", "– KIEROWNIK");
            bp.crate(61, 1, 63, "north", "npc-szabrownik");
        }

        /** Kryta galeria taśmociągu od leja (z 73) do kotłowni (z 45, pomost y 18), da się nią wejść. */
        private void conveyor() {
            for (int z = 45; z <= 72; z++) {
                int y = galleryFloor(z);
                boolean stair = z < 72 && galleryFloor(z + 1) < y;
                for (int x = 56; x <= 58; x++) {
                    String block;
                    if (stair) {
                        block = (x == 57 ? "blackstone_stairs" : "andesite_stairs") + "[facing=north,half=bottom,shape=straight,waterlogged=false]";
                    } else {
                        block = x == 57 ? (chance(0.2) ? "coal_block" : "black_concrete") : "polished_andesite";
                    }
                    set(x, y, z, block);
                    fill(x, y + 1, z, x, y + 3, z, "air");
                }
                if (z == 45) {
                    continue;
                }
                for (int x : new int[]{55, 59}) {
                    set(x, y, z, "gray_concrete");
                    for (int dy = 1; dy <= 3; dy++) {
                        set(x, y + dy, z, dy == 2 && z % 3 == 0 ? brokenGlass() : "light_gray_concrete");
                    }
                }
                if (!(z >= 60 && z <= 62)) {
                    fill(55, y + 4, z, 59, y + 4, z, "gray_concrete");
                }
                // podpory
                if (z % 6 == 2 && y > 2) {
                    fill(55, 1, z, 55, y - 1, z, "gray_concrete");
                    fill(59, 1, z, 59, y - 1, z, "gray_concrete");
                }
            }
        }

        private static int galleryFloor(int z) {
            return Math.min(18, (int) Math.round(18 - (z - 46) * 17.0 / 26.0));
        }

        // ------------------------------------------------------------------ drobiazgi na placu

        private void yardDetails() {
            // maluch bez kół przy drodze
            fill(44, 1, 17, 45, 1, 19, "red_concrete");
            set(44, 2, 18, "glass_pane");
            set(45, 2, 18, "glass_pane");
            set(44, 2, 19, "red_concrete");
            set(45, 2, 19, "red_terracotta");
            // latarnie przy drogach
            for (int[] p : new int[][]{{11, 20}, {11, 36}, {20, 45}, {40, 45}, {64, 45}}) {
                fill(p[0], 1, p[1], p[0], 5, p[1], "andesite_wall");
                set(p[0], 6, p[1], "smooth_stone_slab[type=bottom,waterlogged=false]");
                if (chance(0.6)) {
                    set(p[0], 5, p[1] + 1, "lantern[hanging=true,waterlogged=false]");
                    set(p[0], 6, p[1] + 1, "smooth_stone_slab[type=bottom,waterlogged=false]");
                }
            }
            // zbiornik oleju przy rozdzielni
            fill(44, 1, 6, 49, 3, 9, "white_concrete");
            fill(44, 1, 6, 44, 3, 6, "air");
            fill(49, 3, 9, 49, 3, 9, "air");
            fill(44, 4, 7, 49, 4, 8, "white_concrete");
            crate(47, 1, 10, "south", "paliwo");
            set(46, 1, 10, "respawn_anchor[charges=0]");
            // beczki i złom przy hali
            crate(19, 1, 36, "east", "plac");
            crate(65, 1, 40, "east", "plac");
            mound(16, 40, 2, 1, new Palette(rnd).add("iron_bars", 1).add("gravel", 2).add("cobblestone", 1));
        }
    }
}
