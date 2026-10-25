package pl.axon.structure.generator;

import pl.axon.structure.api.Entrance;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;
import pl.axon.structure.core.Traits;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Opuszczona stacja paliw Oilen: zielono-żółta wiata z napisem OILEN, dwie wyspy z dystrybutorami, pylon z cenami,
 * pawilon ze sklepem i zapleczem, myjnia, cysterna i zbiornik gazu na tylnym placu.
 * <p>
 * Plan w lokalnych współrzędnych (front na północy, z = 0):
 * <pre>
 *  z 0       pobocze z krawężnikiem, wjazd x 5-12 i wyjazd x 33-40 (wjazdy dla AxonMapGenerator)
 *  z 1-20    plac: wiata x 4-30 / z 4-16, wyspy x 11-12 i x 22-23, pylon z cenami x 45-47
 *  z 23-39   myjnia x 2-12, przejście z budką telefoniczną x 13-21,
 *            pawilon x 22-44: sala sprzedaży z 24-31, ściana z 32, biuro / WC / magazyn z 33-38
 *  z 40-46   tylny plac: kontener na śmieci (droga na dach), cysterna, ogrodzony zbiornik gazu
 *  z 47      siatka
 *  dach pawilonu y 6, attyka y 7, napis OILEN na wiacie y 8-12
 * </pre>
 * Meble z paczki (kasa, lada, dystrybutor, lodówka, kibel, regał sklepowy, rozdzielnia, kanistry, wózek) stawiane
 * są jako bloki vanilla, które plugin zamienia na modele (sekcja {@code meble.bloki} w configu).
 */
public final class OilenStructure implements StructureDefinition {

    private static final int WIDTH = 48;
    private static final int HEIGHT = 15;
    private static final int DEPTH = 48;

    // pawilon
    private static final int BX1 = 22;
    private static final int BX2 = 44;
    private static final int BZ1 = 23;
    private static final int BZ2 = 39;
    private static final int ROOF_Y = 6;

    // myjnia
    private static final int WX1 = 2;
    private static final int WX2 = 12;

    // wiata
    private static final int CX1 = 4;
    private static final int CX2 = 30;
    private static final int CZ1 = 4;
    private static final int CZ2 = 16;
    private static final int CANOPY_Y = 6;

    // meble z paczki (bloki zamieniane na modele)
    private static final String BARRIER = "barrier";

    @Override
    public String id() {
        return "oilen";
    }

    @Override
    public String displayName() {
        return "Opuszczona stacja paliw Oilen";
    }

    @Override
    public String description() {
        return "stacja paliw z pawilonem, myjnią i cysterną na zapleczu";
    }

    @Override
    public Blueprint build(Random random) {
        Traits defaults = Traits.defaults();
        // wjazd i wyjazd z drogi biegnącej wzdłuż frontu
        Traits traits = new Traits(List.of(new Entrance(8, 0, "north"), new Entrance(37, 0, "north")), true, null,
                defaults.zones(), defaults.climates(), false, Map.of());
        return new Builder(random).build().traits(traits);
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);

        private final Palette asphalt;
        private final Palette outerWall;
        private final Palette innerWall;
        private final Palette roof;
        private final Palette yard;
        private final Palette sidewalk;
        private final Furniture furniture;

        Builder(Random rnd) {
            this.rnd = rnd;
            this.furniture = new Furniture(bp, 0, rnd);
            asphalt = new Palette(rnd)
                    .add("gray_concrete", 12).add("andesite", 3).add("gravel", 2)
                    .add("tuff", 2).add("coarse_dirt", 1).add("cobblestone", 1);
            outerWall = new Palette(rnd)
                    .add("white_terracotta", 10).add("light_gray_terracotta", 2).add("calcite", 2)
                    .add("white_concrete_powder", 1).add("diorite", 1);
            innerWall = new Palette(rnd)
                    .add("white_concrete", 8).add("light_gray_concrete", 2).add("calcite", 1);
            roof = new Palette(rnd)
                    .add("gray_concrete", 6).add("black_concrete_powder", 2).add("gravel", 2).add("andesite", 1);
            yard = new Palette(rnd)
                    .add("grass_block", 10).add("coarse_dirt", 4).add("gravel", 2).add("dirt", 2).add("podzol", 1);
            sidewalk = new Palette(rnd)
                    .add("smooth_stone", 6).add("stone_bricks", 3).add("cracked_stone_bricks", 2).add("andesite", 1);
        }

        Blueprint build() {
            ground();
            roadside();
            canopy();
            pumpIslands();
            cars();
            pylon();
            letters();
            shopShell();
            salesHall();
            backRooms();
            shopRoof();
            carWash();
            alley();
            backLot();
            tanker();
            gasTank();
            decay();
            lifeSigns();
            return bp;
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        private void door(int x, int y, int z, String type, String facing, String hinge, boolean open) {
            String props = "facing=" + facing + ",hinge=" + hinge + ",open=" + open + ",powered=false";
            bp.set(x, y, z, type + "[half=lower," + props + "]");
            bp.set(x, y + 1, z, type + "[half=upper," + props + "]");
        }

        private String brokenGlass() {
            double roll = rnd.nextDouble();
            if (roll < 0.35) {
                return "air";
            }
            if (roll < 0.40) {
                return "cobweb";
            }
            return chance(0.25) ? "light_gray_stained_glass_pane" : "glass_pane";
        }

        // ------------------------------------------------------------------ meble z paczki

        /** Dystrybutor paliwa: model na 2 kratki (nad nim bariera). */
        private void pump(int x, int z, String facing) {
            bp.set(x, 1, z, "damaged_anvil[facing=" + facing + "]");
            bp.set(x, 2, z, BARRIER);
        }

        /** Przeszklona lodówka z napojami: model na 2 kratki (nad nią bariera). */
        private void fridge(int x, int z, String facing) {
            bp.set(x, 1, z, "calibrated_sculk_sensor[facing=" + facing + ",power=0,sculk_sensor_phase=inactive,waterlogged=false]");
            bp.set(x, 2, z, BARRIER);
        }

        private void counter(int x, int z, String facing) {
            bp.set(x, 1, z, "jack_o_lantern[facing=" + facing + "]");
        }

        private void till(int x, int y, int z, String facing) {
            bp.set(x, y, z, "ender_chest[facing=" + facing + ",waterlogged=false]");
        }

        private void shopShelf(int x, int z, String facing) {
            bp.set(x, 1, z, "bee_nest[facing=" + facing + ",honey_level=0]");
        }

        private void wc(int x, int z, String facing) {
            bp.set(x, 1, z, "end_portal_frame[eye=false,facing=" + facing + "]");
        }

        private void fuseBox(int x, int y, int z, String facing) {
            bp.set(x, y, z, "sticky_piston[extended=false,facing=" + facing + "]");
        }

        private void canisters(int x, int y, int z) {
            bp.set(x, y, z, "respawn_anchor[charges=0]");
        }

        private void cart(int x, int z) {
            bp.set(x, 1, z, "lodestone");
        }

        private void fryer(int x, int z, String facing) {
            bp.set(x, 1, z, "vault[facing=" + facing + ",ominous=false,vault_state=inactive]");
        }

        // ------------------------------------------------------------------ teren i pobocze

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    String block;
                    boolean lot = x >= 2 && x <= 44 && (z >= 1 && z <= 20 || z >= 40 && z <= 46);
                    boolean lane = x >= 13 && x <= 21 && z >= 21 && z <= 39;
                    if (lot || lane) {
                        block = asphalt.next();
                    } else if (x >= BX1 - 1 && x <= BX2 + 1 && z >= 21 && z <= BZ2) {
                        block = sidewalk.next();
                    } else {
                        block = yard.next();
                        if (block.equals("grass_block") && chance(0.3)) {
                            bp.set(x, 1, z, chance(0.8) ? "short_grass" : chance(0.5) ? "dead_bush" : "tall_grass[half=lower]");
                            if (bp.get(x, 1, z).contains("tall_grass")) {
                                bp.set(x, 2, z, "tall_grass[half=upper]");
                            }
                        }
                    }
                    bp.set(x, 0, z, block);
                }
            }
            // spękania asfaltu, przez które przebija trawa
            for (int i = 0; i < 40; i++) {
                int x = 2 + rnd.nextInt(43);
                int z = 1 + rnd.nextInt(20);
                bp.set(x, 0, z, chance(0.6) ? "grass_block" : "moss_block");
                if (chance(0.5)) {
                    bp.set(x, 1, z, "short_grass");
                }
            }
        }

        /** Krawężnik wzdłuż drogi z dwoma zjazdami (wjazd i wyjazd), strzałki na asfalcie i znaki. */
        private void roadside() {
            for (int x = 0; x < WIDTH; x++) {
                boolean driveway = x >= 5 && x <= 12 || x >= 33 && x <= 40;
                if (driveway) {
                    bp.set(x, 0, 0, asphalt.next());
                    bp.set(x, 1, 0, "air");
                } else {
                    bp.set(x, 0, 0, "smooth_stone");
                    bp.set(x, 1, 0, chance(0.85) ? "smooth_stone_slab[type=bottom]" : "air");
                }
            }
            // białe strzałki: wjazd w głąb, wyjazd do drogi
            for (int z = 2; z <= 4; z++) {
                bp.set(8, 0, z, "white_concrete");
                bp.set(37, 0, z, "white_concrete");
            }
            bp.set(7, 0, 4, "white_concrete");
            bp.set(9, 0, 4, "white_concrete");
            bp.set(36, 0, 2, "white_concrete");
            bp.set(38, 0, 2, "white_concrete");
            // znaki przy zjazdach
            bp.fill(4, 1, 1, 4, 2, 1, "spruce_fence");
            bp.sign(4, 3, 1, "oak_sign[rotation=8]", "WJAZD", "→", "", "");
            bp.fill(41, 1, 1, 41, 2, 1, "spruce_fence");
            bp.sign(41, 3, 1, "oak_sign[rotation=8]", "WYJAZD", "←", "uwaga", "na tiry");
            // betonowe donice z zeschniętymi krzakami przy krawężniku
            for (int x : new int[]{1, 14, 31, 43}) {
                bp.set(x, 1, 1, "stone_bricks");
                bp.set(x, 2, 1, chance(0.6) ? "dead_bush" : "air");
            }
            bp.set(22, 1, 1, "stone_bricks");
            bp.set(22, 2, 1, "dead_bush");
            bp.set(23, 1, 1, "stone_bricks");
        }

        // ------------------------------------------------------------------ wiata

        private void canopy() {
            int[][] pillars = {{11, 7}, {11, 13}, {23, 7}, {23, 13}};
            for (int[] p : pillars) {
                bp.set(p[0], 1, p[1], "green_concrete");
                bp.fill(p[0], 2, p[1], p[0], CANOPY_Y - 1, p[1], "white_concrete");
                bp.set(p[0], 3, p[1], "yellow_concrete");
            }
            for (int x = CX1; x <= CX2; x++) {
                for (int z = CZ1; z <= CZ2; z++) {
                    boolean edge = x == CX1 || x == CX2 || z == CZ1 || z == CZ2;
                    if (edge) {
                        // dwupiętrowa blenda: żółty pas pod zielonym
                        bp.set(x, CANOPY_Y, z, chance(0.9) ? "yellow_concrete" : "yellow_terracotta");
                        bp.set(x, CANOPY_Y + 1, z, chance(0.9) ? "green_concrete" : "green_terracotta");
                    } else {
                        bp.set(x, CANOPY_Y, z, chance(0.92) ? "white_concrete" : "light_gray_concrete");
                        bp.set(x, CANOPY_Y + 1, z, chance(0.8) ? "light_gray_concrete" : "gravel");
                    }
                }
            }
            // świetlówki w suficie wiaty (część pobita)
            for (int x = 7; x <= 28; x += 7) {
                for (int z = 7; z <= 13; z += 6) {
                    bp.set(x, CANOPY_Y, z, chance(0.6) ? "sea_lantern" : "redstone_lamp[lit=false]");
                }
            }
            // narożnik wiaty od zachodu oberwał: blacha zwisa do ziemi
            for (int x = CX1; x <= CX1 + 4; x++) {
                for (int z = CZ2 - 3; z <= CZ2; z++) {
                    bp.set(x, CANOPY_Y, z, "air");
                    bp.set(x, CANOPY_Y + 1, z, "air");
                }
            }
            bp.set(CX1 + 4, CANOPY_Y - 1, CZ2 - 2, "white_concrete");
            bp.set(CX1 + 3, CANOPY_Y - 2, CZ2 - 2, "yellow_concrete");
            bp.set(CX1 + 2, CANOPY_Y - 3, CZ2 - 1, "green_concrete");
            bp.set(CX1 + 2, CANOPY_Y - 4, CZ2 - 1, "white_concrete");
            bp.set(CX1 + 1, 1, CZ2, "green_concrete");
            bp.set(CX1, 1, CZ2 - 1, "white_concrete");
            bp.set(CX1 + 1, 1, CZ2 - 1, "light_gray_concrete");
            // kilka dziur w dachu wiaty
            for (int i = 0; i < 6; i++) {
                int x = CX1 + 6 + rnd.nextInt(CX2 - CX1 - 7);
                int z = CZ1 + 1 + rnd.nextInt(CZ2 - CZ1 - 1);
                bp.set(x, CANOPY_Y, z, "air");
                bp.set(x, CANOPY_Y + 1, z, "air");
            }
            bp.sign(11, 3, 6, "oak_wall_sign[facing=north]", "NIE PALIĆ", "PRZY", "DYSTRYBUTORZE", "dotyczy też Cię");
        }

        private void pumpIslands() {
            for (int x : new int[]{11, 23}) {
                for (int z = 6; z <= 14; z++) {
                    if (bp.isAir(x, 1, z)) {
                        bp.set(x, 1, z, "smooth_stone_slab[type=bottom]");
                    }
                    bp.set(x + (x == 11 ? 1 : -1), 1, z, "smooth_stone_slab[type=bottom]");
                }
                // odbojniki w biało-czerwone pasy na końcach wysp
                bp.set(x, 1, 5, "red_concrete");
                bp.set(x, 2, 5, "white_concrete");
                bp.set(x, 1, 15, "red_concrete");
            }
            // trzy dystrybutory stoją, czwarty ktoś wyrwał
            pump(12, 9, "east");
            pump(12, 11, "east");
            pump(22, 9, "west");
            bp.set(22, 1, 11, "smooth_stone");
            bp.set(22, 2, 11, "chain[axis=y]");
            bp.set(21, 0, 11, "black_concrete");
            bp.set(21, 0, 12, "black_concrete");
            bp.set(20, 0, 12, "black_concrete");
            // kosze przy wyspach
            bp.set(12, 1, 13, "composter[level=0]");
            bp.set(22, 1, 7, "composter[level=0]");
            canisters(24, 1, 10);
            // beczki i kanistry przy wyspach
            bp.crate(13, 1, 6, "north", "paliwo");
            bp.crate(21, 1, 14, "north", "paliwo");
            bp.crate(24, 1, 14, "east", "plac");
            bp.crate(10, 1, 9, "west", "plac");
        }

        // ------------------------------------------------------------------ samochody

        private void cars() {
            // mały samochodzik między wyspami, drzwi otwarte, bagażnik z przodu otwarty
            String small = chance(0.5) ? "light_blue_terracotta" : "yellow_terracotta";
            bp.fill(15, 1, 8, 17, 1, 11, small);
            bp.set(15, 1, 8, "black_concrete");
            bp.set(17, 1, 8, "black_concrete");
            bp.set(15, 1, 11, "black_concrete");
            bp.set(17, 1, 11, "black_concrete");
            bp.set(16, 2, 8, "glass_pane");
            bp.set(15, 2, 9, "glass_pane");
            bp.set(17, 2, 9, "air");
            bp.set(16, 2, 9, "spruce_stairs[facing=south,half=bottom]");
            bp.set(16, 2, 10, "spruce_stairs[facing=south,half=bottom]");
            bp.set(15, 2, 10, "glass_pane");
            bp.set(17, 2, 10, "glass_pane");
            bp.fill(15, 3, 9, 17, 3, 10, small);
            bp.set(18, 1, 9, "spruce_trapdoor[facing=east,half=bottom,open=true]"); // drzwi kierowcy
            bp.crate(16, 2, 11, "north", "plac");
            canisters(14, 1, 11);

            // dłuższe auto za drugą wyspą, wybita szyba, bagażnik otwarty
            String sedan = chance(0.5) ? "brown_terracotta" : "white_terracotta";
            bp.fill(26, 1, 6, 28, 1, 11, sedan);
            bp.set(26, 1, 6, "black_concrete");
            bp.set(28, 1, 6, "black_concrete");
            bp.set(26, 1, 11, "black_concrete");
            bp.set(28, 1, 11, "black_concrete");
            bp.fill(26, 2, 7, 28, 2, 7, "glass_pane");
            bp.set(27, 2, 7, "air");
            bp.set(26, 2, 8, "glass_pane");
            bp.set(28, 2, 8, "air");
            bp.set(26, 2, 9, "glass_pane");
            bp.set(28, 2, 9, "glass_pane");
            bp.set(27, 2, 8, "spruce_stairs[facing=south,half=bottom]");
            bp.fill(26, 3, 8, 28, 3, 9, sedan);
            bp.set(27, 3, 8, "air");
            bp.crate(27, 2, 10, "south", "plac");
            bp.set(27, 2, 11, "spruce_trapdoor[facing=south,half=top,open=true]");

            // porzucony wózek sklepowy na placu
            cart(33, 14);
            cart(19, 18);
        }

        /** Pylon przy wyjeździe z tablicą cen (część cyfr wypadła). */
        private void pylon() {
            bp.fill(45, 1, 2, 45, 5, 2, "light_gray_concrete");
            bp.fill(47, 1, 2, 47, 5, 2, "light_gray_concrete");
            bp.fill(45, 6, 2, 47, 12, 2, "green_concrete");
            bp.fill(45, 12, 2, 47, 12, 2, "yellow_concrete");
            bp.set(46, 13, 2, "lightning_rod[facing=up]");
            bp.sign(46, 11, 1, "oak_wall_sign[facing=north]", "OILEN", "stacja paliw", "czynne", "6 - 22");
            bp.sign(45, 9, 1, "oak_wall_sign[facing=north]", "Pb 95", "2,87", "", "");
            bp.sign(47, 9, 1, "oak_wall_sign[facing=north]", "Pb 98", "3,", "", "");
            bp.sign(45, 7, 1, "oak_wall_sign[facing=north]", "ON", "2,61", "", "");
            bp.sign(47, 7, 1, "oak_wall_sign[facing=north]", "LPG", "--,--", "", "");
            bp.set(46, 8, 1, "air");
        }

        // ------------------------------------------------------------------ napis OILEN na wiacie

        private static final String[][] FONT = {
                // O
                {"###", "#.#", "#.#", "#.#", "###"},
                // I
                {"#", "#", "#", "#", "#"},
                // L
                {"#..", "#..", "#..", "#..", "###"},
                // E
                {"###", "#..", "###", "#..", "###"},
                // N
                {"#..#", "##.#", "#.##", "#..#", "#..#"},
        };

        private void letters() {
            int totalWidth = -1;
            for (String[] glyph : FONT) {
                totalWidth += glyph[0].length() + 1;
            }
            // tekst czytany od frontu (patrząc na południe) biegnie od dużych x do małych
            int startX = (CX1 + CX2) / 2 + totalWidth / 2;
            int z = CZ1;
            int topY = CANOPY_Y + 6;
            int column = 0;
            for (int g = 0; g < FONT.length; g++) {
                String[] glyph = FONT[g];
                int glyphWidth = glyph[0].length();
                for (int c = 0; c < glyphWidth; c++) {
                    int x = startX - column - c;
                    for (int row = 0; row < glyph.length; row++) {
                        if (glyph[row].charAt(c) != '#') {
                            continue;
                        }
                        int y = topY - row;
                        // z litery L odpadła podstawa (leży na dachu wiaty), z pozostałych co dziesiąty kawałek
                        if (g == 2 && row == glyph.length - 1 || chance(0.08)) {
                            bp.set(x - 1 + rnd.nextInt(3), CANOPY_Y + 2, z + 2 + rnd.nextInt(3), "yellow_concrete");
                            continue;
                        }
                        bp.set(x, y, z, chance(0.9) ? "yellow_concrete" : "yellow_terracotta");
                    }
                }
                column += glyphWidth + 1;
            }
        }

        // ------------------------------------------------------------------ pawilon

        private void shopShell() {
            for (int y = 1; y <= ROOF_Y - 1; y++) {
                for (int x = BX1; x <= BX2; x++) {
                    bp.set(x, y, BZ1, outerWall.next());
                    bp.set(x, y, BZ2, outerWall.next());
                }
                for (int z = BZ1; z <= BZ2; z++) {
                    bp.set(BX1, y, z, outerWall.next());
                    bp.set(BX2, y, z, outerWall.next());
                }
            }
            // zielony cokół dookoła, żółto-zielona blenda nad witryną
            for (int x = BX1; x <= BX2; x++) {
                bp.set(x, 1, BZ1, "green_terracotta");
                bp.set(x, 1, BZ2, "green_terracotta");
                bp.set(x, 5, BZ1, chance(0.9) ? "yellow_concrete" : "yellow_terracotta");
            }
            for (int z = BZ1; z <= BZ2; z++) {
                bp.set(BX1, 1, z, "green_terracotta");
                bp.set(BX2, 1, z, "green_terracotta");
            }
            // witryna: szyby między słupkami co 4 kratki, wejście x 32-33
            for (int x = BX1 + 1; x < BX2; x++) {
                if ((x - BX1) % 4 == 0 || x == 32 || x == 33) {
                    continue;
                }
                for (int y = 2; y <= 4; y++) {
                    // krata antywłamaniowa została tylko na wschodniej części witryny
                    bp.set(x, y, BZ1, x >= 36 && chance(0.85) ? "iron_bars" : brokenGlass());
                }
            }
            bp.fill(32, 1, BZ1, 33, 4, BZ1, "air");
            Furniture.doubleDoor(this::door, 32, 1, BZ1, "birch_door", "north", true);
            bp.set(32, 4, BZ1, "white_stained_glass");
            bp.set(33, 4, BZ1, "white_stained_glass");
            bp.sign(34, 3, BZ1 - 1, "oak_wall_sign[facing=north]", "ZAMKNIĘTE", "do odwołania", "", "");
            // okna boczne
            for (int z = 25; z <= 30; z++) {
                if (z == 27 || z == 28) {
                    continue;
                }
                for (int y = 2; y <= 3; y++) {
                    bp.set(BX2, y, z, brokenGlass());
                    bp.set(BX1, y, z, "iron_bars");
                }
            }
            // podłogi: lastryko w sali, beton na zapleczu
            for (int x = BX1 + 1; x < BX2; x++) {
                for (int z = BZ1 + 1; z < BZ2; z++) {
                    String floor;
                    if (z <= 31) {
                        floor = chance(0.8) ? "polished_diorite" : chance(0.5) ? "diorite" : "cracked_stone_bricks";
                    } else {
                        floor = chance(0.85) ? "smooth_stone" : "andesite";
                    }
                    bp.set(x, 0, z, floor);
                }
            }
            // dach z attyką
            for (int x = BX1; x <= BX2; x++) {
                for (int z = BZ1; z <= BZ2; z++) {
                    boolean edge = x == BX1 || x == BX2 || z == BZ1 || z == BZ2;
                    bp.set(x, ROOF_Y, z, edge && z == BZ1 ? "green_concrete" : roof.next());
                    if (edge) {
                        bp.set(x, ROOF_Y + 1, z, chance(0.92) ? (z == BZ1 ? "green_concrete" : "light_gray_concrete") : "air");
                    }
                }
            }
            // tylne drzwi na plac i wejście dla dostawców
            door(40, 1, BZ2, "spruce_door", "south", "left", true);
            door(BX1, 1, 35, "iron_door", "west", "right", false);
        }

        private void salesHall() {
            // lada kasowa wzdłuż z, sprzedawca od zachodu, przejście za ladę od strony zaplecza (z 31)
            for (int z = 25; z <= 30; z++) {
                counter(28, z, "east");
            }
            // punkt sklepu stacji (GUI) w ladzie między kasami
            bp.crate(28, 1, 28, "east", "punkt-sklep-stacja");
            till(28, 2, 26, "west");
            till(28, 2, 29, "west");
            bp.sign(28, 2, 27, "oak_sign[rotation=4]", "KLUCZ DO WC", "U KASJERA", "kasjer uciekł.", "klucz też.");
            // regał z papierosami za ladą
            furniture.bookshelf(BX1 + 1, 1, 25, BX1 + 1, 28, 2, "east");
            fuseBox(BX1 + 1, 2, 30, "east");
            bp.crate(27, 1, 25, "east", "lada");
            bp.crate(24, 1, 30, "north", "lada");
            bp.set(26, 1, 29, "spruce_stairs[facing=west,half=bottom]"); // stołek kasjera

            // regały sklepowe: dwa rzędy plecami do siebie, w drugim rzędzie dziura po przewróconym regale
            for (int z = 26; z <= 30; z++) {
                shopShelf(32, z, "west");
                shopShelf(33, z, "east");
                if (z != 28 && z != 29) {
                    shopShelf(36, z, "west");
                    shopShelf(37, z, "east");
                }
            }
            bp.set(36, 1, 28, "brown_carpet");
            bp.set(37, 1, 29, "brown_carpet");
            cart(35, 30);
            bp.crate(34, 1, 27, "north", "sklep");
            bp.crate(31, 1, 29, "west", "sklep");
            bp.crate(38, 1, 30, "east", "sklep");
            bp.crate(35, 1, 25, "north", "sklep");
            bp.crate(30, 1, 24, "south", "sklep");

            // lodówki z napojami pod wschodnią ścianą
            for (int z = 24; z <= 30; z++) {
                if (z == 27) {
                    bp.set(BX2 - 1, 1, z, "white_concrete"); // wyrwana lodówka
                    continue;
                }
                fridge(BX2 - 1, z, "west");
            }
            // automat z napojami za lodówkami (punkt sklepu GUI w automacie)
            bp.crate(BX2 - 1, 1, 31, "west", "punkt-sklep-automat-napoje");
            bp.set(BX2 - 1, 1, 31, "dispenser[facing=west,triggered=false]");
            bp.set(BX2 - 1, 2, 31, "red_stained_glass");
            bp.set(BX2 - 1, 3, 31, "red_concrete");
            bp.crate(BX2 - 2, 1, 25, "west", "lodowki");
            bp.crate(BX2 - 2, 1, 27, "west", "lodowki");
            bp.crate(BX2 - 2, 1, 30, "west", "lodowki");

            // kącik z hot-dogami przy witrynie
            fryer(38, 24, "south");
            bp.set(39, 1, 24, "smoker[facing=south,lit=false]");
            bp.sign(39, 2, 24, "oak_sign[rotation=8]", "HOT-DOG 2,50", "z musztardą", "(musztardy", "brak)");
            furniture.table(40, 1, 26, "birch");
            furniture.chair(40, 1, 27, "south", "birch");

            for (int x = 25; x <= 41; x += 8) {
                for (int z = 25; z <= 30; z += 5) {
                    bp.set(x, ROOF_Y - 1, z, chance(0.75) ? "lantern[hanging=true]" : "chain[axis=y]");
                }
            }
        }

        private void backRooms() {
            // ściana między salą a zapleczem, drzwi do biura, WC i szeroki przejazd do magazynu
            bp.fill(BX1 + 1, 1, 32, BX2 - 1, ROOF_Y - 1, 32, innerWall);
            door(26, 1, 32, "spruce_door", "south", "left", true);
            door(31, 1, 32, "birch_door", "south", "right", false);
            bp.fill(39, 1, 32, 40, 3, 32, "air");
            bp.sign(26, 3, 31, "oak_wall_sign[facing=north]", "BIURO", "", "", "");
            bp.sign(31, 3, 31, "oak_wall_sign[facing=north]", "WC", "dla klientów", "", "");
            bp.sign(41, 3, 31, "oak_wall_sign[facing=north]", "MAGAZYN", "wstęp", "wzbroniony", "");
            bp.fill(29, 1, 33, 29, ROOF_Y - 1, BZ2 - 1, innerWall);
            bp.fill(34, 1, 33, 34, ROOF_Y - 1, BZ2 - 1, innerWall);

            // biuro: biurko z komputerem, szafa, sejf
            bp.set(24, 1, 37, "lectern[facing=east,has_book=false,powered=false]");
            furniture.chair(25, 1, 37, "east", "spruce");
            furniture.desk(BX1 + 1, 1, 33, BX1 + 1, 34, "spruce");
            furniture.lamp(BX1 + 1, 2, 34);
            furniture.wardrobe(28, 1, 34, "west", "spruce");
            bp.fill(28, 1, 37, 28, 2, 38, "iron_block"); // sejf
            bp.set(27, 2, 38, "iron_trapdoor[facing=west,half=bottom,open=true]");
            bp.crate(27, 1, 38, "west", "zaplecze");
            bp.sign(27, 3, 38, "oak_wall_sign[facing=north]", "SEJF PUSTY.", "NAPRAWDĘ.", "- kierownik", "");
            bp.crate(25, 1, 33, "south", "parter");
            furniture.hangingLamp(26, ROOF_Y - 1, 35);

            // WC: kibel, umywalka i lustro
            wc(31, 38, "north");
            wc(33, 38, "north");
            bp.fill(32, 1, 36, 32, 3, 38, "white_concrete"); // ścianka między kabinami
            furniture.sink(30, 1, 33, "north");
            bp.crate(33, 1, 34, "north", "toalety");
            bp.crate(30, 1, 36, "west", "toalety");
            for (int x = 30; x <= 33; x++) {
                for (int z = 33; z <= 38; z++) {
                    bp.set(x, 0, z, (x + z) % 2 == 0 ? "white_concrete" : "light_blue_terracotta");
                }
            }
            bp.set(31, 4, BZ2, "white_stained_glass");
            furniture.hangingLamp(31, ROOF_Y - 1, 35);

            // magazyn: regały, palety, rozdzielnia, warsztat
            for (int x = 35; x <= 43; x++) {
                if (x == 40) {
                    continue; // tylne drzwi
                }
                bp.set(x, 1, 38, "spruce_slab[type=top]");
                bp.set(x, 3, 38, "spruce_slab[type=top]");
            }
            bp.fill(35, 1, 38, 35, 4, 38, "stripped_spruce_log[axis=y]");
            bp.fill(39, 1, 38, 39, 4, 38, "stripped_spruce_log[axis=y]");
            bp.fill(43, 1, 38, 43, 4, 38, "stripped_spruce_log[axis=y]");
            bp.crate(37, 2, 38, "north", "magazyn-regal");
            bp.crate(42, 2, 38, "north", "magazyn-regal");
            bp.set(36, 2, 38, "barrel[facing=north,open=false]");
            canisters(41, 2, 38);
            fuseBox(BX2 - 1, 2, 33, "west");
            bp.set(BX2 - 1, 1, 34, "crafting_table");
            bp.set(BX2 - 1, 1, 35, "grindstone[face=floor,facing=west]");
            bp.fill(36, 1, 34, 37, 1, 35, "spruce_slab[type=bottom]"); // paleta
            bp.crate(36, 2, 34, "north", "magazyn");
            canisters(37, 2, 35);
            bp.crate(41, 1, 35, "north", "magazyn");
            bp.crate(35, 1, 33, "south", "parter");
            for (int x = 37; x <= 42; x += 5) {
                furniture.hangingLamp(x, ROOF_Y - 1, 36);
            }
        }

        private void shopRoof() {
            // klimatyzatory, wywietrzniki, maszt anteny
            acUnit(24, 25);
            acUnit(40, 34);
            for (int[] vent : new int[][]{{30, 29}, {36, 37}}) {
                bp.set(vent[0], ROOF_Y + 1, vent[1], "copper_grate");
                bp.set(vent[0], ROOF_Y + 2, vent[1], "oxidized_cut_copper_slab[type=bottom]");
            }
            bp.fill(43, ROOF_Y + 1, 38, 43, ROOF_Y + 5, 38, "iron_bars");
            bp.set(43, ROOF_Y + 6, 38, "lightning_rod[facing=up]");
            // wejście: drabina na tylnej ścianie, dolne szczeble urwane – wskakuje się z kontenera na śmieci
            for (int y = 3; y <= ROOF_Y; y++) {
                bp.set(25, y, BZ2 + 1, "ladder[facing=south]");
            }
            bp.set(25, ROOF_Y + 1, BZ2, "air");
            // obozowisko na dachu
            furniture.camp(36, ROOF_Y + 1, 28, "east");
            bp.set(34, ROOF_Y + 1, 26, "spruce_fence");
            bp.sign(34, ROOF_Y + 2, 26, "oak_sign[rotation=0]", "TU ŚPI ZBYCHU", "NIE RUSZAĆ", "(piwa też nie)", "");
            bp.crate(31, ROOF_Y + 1, 35, "north", "dach");
            bp.crate(42, ROOF_Y + 1, 25, "west", "dach");
            // osłona z worków przy krawędzi od frontu
            for (int x = 28; x <= 30; x++) {
                bp.set(x, ROOF_Y + 1, BZ1 + 1, "brown_wool");
            }
            bp.crate(33, ROOF_Y + 1, 32, "south", "npc-szabrownik");
        }

        private void acUnit(int x, int z) {
            boolean broken = chance(0.3);
            bp.fill(x, ROOF_Y + 1, z, x + 1, ROOF_Y + 1, z + 1, broken ? "weathered_copper" : "iron_block");
            bp.fill(x, ROOF_Y + 2, z, x + 1, ROOF_Y + 2, z + 1, "daylight_detector[inverted=false,power=0]");
        }

        // ------------------------------------------------------------------ myjnia

        private void carWash() {
            for (int z = BZ1; z <= BZ2; z++) {
                bp.fill(WX1, 1, z, WX1, 5, z, outerWall);
                bp.fill(WX2, 1, z, WX2, 5, z, outerWall);
                bp.set(WX1, 1, z, "green_terracotta");
                bp.set(WX2, 1, z, "green_terracotta");
                for (int x = WX1; x <= WX2; x++) {
                    bp.set(x, 6, z, roof.next());
                    if (x > WX1 && x < WX2) {
                        bp.set(x, 0, z, x == 7 ? "iron_trapdoor[facing=north,half=top,open=false]" : "smooth_stone");
                    }
                }
            }
            // zielono-żółte blendy nad wjazdem i wyjazdem
            for (int x = WX1; x <= WX2; x++) {
                bp.set(x, 5, BZ1, "yellow_concrete");
                bp.set(x, 6, BZ1, "green_concrete");
                bp.set(x, 5, BZ2, "yellow_concrete");
                bp.set(x, 6, BZ2, "green_concrete");
            }
            bp.sign(7, 4, BZ1, "oak_wall_sign[facing=north]", "MYJNIA", "NIECZYNNA", "od wtorku.", "którego?");
            bp.set(7, 4, BZ1 + 1, "yellow_concrete");
            // bramownica ze szczotkami
            bp.fill(3, 1, 31, 3, 5, 31, "yellow_concrete");
            bp.fill(11, 1, 31, 11, 5, 31, "yellow_concrete");
            bp.fill(4, 5, 31, 10, 5, 31, "yellow_concrete");
            bp.fill(4, 1, 31, 4, 4, 31, "blue_wool");
            bp.fill(10, 1, 30, 10, 3, 30, "blue_wool"); // szczotka urwana i przechylona
            bp.fill(5, 4, 31, 9, 4, 31, chance(0.5) ? "blue_wool" : "light_blue_wool");
            bp.set(7, 4, 31, "air");
            fuseBox(WX1 + 1, 2, 34, "east");
            // samochód w myjni, przed bramownicą
            String car = chance(0.5) ? "red_terracotta" : "green_terracotta";
            bp.fill(6, 1, 25, 8, 1, 29, car);
            bp.set(6, 1, 25, "black_concrete");
            bp.set(8, 1, 25, "black_concrete");
            bp.set(6, 1, 29, "black_concrete");
            bp.set(8, 1, 29, "black_concrete");
            bp.fill(6, 2, 26, 8, 2, 26, "glass_pane");
            bp.set(6, 2, 27, "glass_pane");
            bp.set(8, 2, 27, "air");
            bp.set(7, 2, 27, "spruce_stairs[facing=south,half=bottom]");
            bp.fill(6, 3, 27, 8, 3, 28, car);
            bp.crate(7, 2, 28, "south", "plac");
            bp.crate(10, 1, 36, "west", "zaplecze");
            canisters(3, 1, 37);
            bp.crate(3, 1, 25, "east", "plac");
            // drabina na dach myjni od strony przejścia, bez najniższego szczebla (wskakuje się z beczki)
            for (int y = 2; y <= 6; y++) {
                bp.set(WX2 + 1, y, 36, "ladder[facing=east]");
            }
            bp.set(WX2 + 2, 1, 36, "barrel[facing=up,open=false]");
            // na dachu myjni
            bp.crate(5, 7, 33, "north", "dach");
            bp.crate(8, 7, 27, "north", "npc-szabrownik");
            bp.fill(4, 7, 24, 5, 7, 24, "brown_wool");
        }

        // ------------------------------------------------------------------ przejście między myjnią a pawilonem

        private void alley() {
            // budka telefoniczna na karty
            bp.fill(18, 1, 25, 20, 3, 27, "glass_pane");
            bp.fill(19, 1, 26, 19, 2, 26, "air");
            bp.fill(19, 1, 25, 19, 2, 25, "air"); // wybite drzwi od frontu
            bp.fill(18, 4, 25, 20, 4, 27, "orange_concrete");
            bp.set(18, 1, 25, "orange_concrete");
            bp.set(20, 1, 25, "orange_concrete");
            bp.set(19, 2, 27, "orange_concrete");
            bp.set(19, 2, 26, "tripwire_hook[facing=north,attached=false,powered=false]");
            bp.sign(19, 3, 26, "oak_wall_sign[facing=north]", "TELEFON", "NA KARTY", "kart brak.", "sygnału też.");
            // kompresor do pompowania kół
            bp.set(15, 1, 24, "light_gray_concrete");
            bp.set(14, 1, 24, "tripwire_hook[facing=west,attached=false,powered=false]");
            bp.sign(15, 2, 23, "oak_wall_sign[facing=north]", "POWIETRZE", "WODA", "", "");
            bp.set(15, 2, 24, "light_gray_concrete");
            // stos opon i palet przy myjni
            bp.set(14, 1, 29, "blackstone_wall");
            bp.set(14, 2, 29, "blackstone_wall");
            bp.set(14, 1, 30, "blackstone_wall");
            bp.fill(20, 1, 33, 21, 1, 34, "spruce_slab[type=bottom]");
            bp.crate(21, 2, 33, "east", "plac");
            cart(17, 31);
            bp.crate(16, 1, 38, "north", "plac");
        }

        // ------------------------------------------------------------------ tylny plac

        private void backLot() {
            // kontener na śmieci pod drabiną na dach pawilonu
            bp.fill(24, 1, 41, 26, 2, 42, "green_terracotta");
            bp.fill(24, 3, 41, 26, 3, 42, "dark_oak_trapdoor[facing=north,half=bottom,open=false]");
            bp.set(26, 3, 42, "air");
            bp.set(23, 1, 41, "black_wool");
            bp.set(27, 1, 42, "black_carpet");
            bp.set(28, 1, 41, "composter[level=0]");
            // siatka z dziurą od zachodu
            for (int x = 0; x < WIDTH; x++) {
                if (x >= 4 && x <= 6) {
                    continue;
                }
                bp.set(x, 1, DEPTH - 1, "iron_bars");
                if (chance(0.7)) {
                    bp.set(x, 2, DEPTH - 1, "iron_bars");
                }
            }
            bp.crate(2, 1, 44, "east", "plac");
            bp.crate(32, 1, 41, "north", "plac");
            canisters(10, 1, 41);
            // pusta paleta i pretensje
            bp.fill(11, 1, 43, 12, 1, 44, "spruce_slab[type=bottom]");
            bp.fill(13, 1, 43, 13, 2, 43, "spruce_fence");
            bp.sign(13, 3, 43, "oak_sign[rotation=8]", "KTO UKRADŁ", "MOJE KANISTRY?!", "oddaj, nic", "nie powiem. H.");
            bp.crate(20, 1, 41, "south", "npc-szabrownik");
        }

        /** Cysterna przy tylnym placu: zbiornik x 15-29, kabina x 31-33, drabinka z tyłu na górę zbiornika. */
        private void tanker() {
            int z1 = 43;
            int z2 = 45;
            for (int x = 15; x <= 33; x++) {
                bp.set(x, 1, 44, "gray_concrete");
            }
            for (int x : new int[]{16, 17, 27, 28, 32}) {
                bp.set(x, 1, z1, "black_concrete");
                bp.set(x, 1, z2, "black_concrete");
            }
            for (int x = 15; x <= 29; x++) {
                bp.set(x, 2, z1, "light_gray_concrete");
                bp.set(x, 3, z1, "light_gray_concrete");
                bp.set(x, 2, z2, "light_gray_concrete");
                bp.set(x, 3, z2, "light_gray_concrete");
                bp.fill(x, 2, 44, x, 4, 44, "light_gray_concrete");
                bp.set(x, 4, z1, "smooth_stone_slab[type=bottom]");
                bp.set(x, 4, z2, "smooth_stone_slab[type=bottom]");
                if (x % 5 == 0) {
                    bp.set(x, 3, z1, "green_concrete");
                    bp.set(x, 3, z2, "green_concrete");
                }
            }
            bp.set(22, 5, 44, "iron_trapdoor[facing=north,half=bottom,open=false]");
            // kabina
            bp.fill(31, 1, z1, 33, 3, z2, "green_terracotta");
            bp.fill(31, 2, z1 + 1, 32, 2, z2 - 1, "air");
            bp.fill(33, 2, z1, 33, 2, z2, "glass_pane");
            bp.set(33, 2, 44, "air");
            bp.set(31, 2, z1, "glass_pane");
            bp.set(32, 2, 44, "spruce_stairs[facing=west,half=bottom]");
            bp.fill(31, 4, z1, 33, 4, z2, "smooth_stone_slab[type=bottom]");
            // drabinka z tyłu na zbiornik, skrzynia na górze
            for (int y = 2; y <= 4; y++) {
                bp.set(14, y, 44, "ladder[facing=west]");
            }
            bp.crate(26, 5, 44, "north", "paliwo");
            canisters(19, 5, 44);
            bp.crate(30, 1, 46, "north", "paliwo");
            bp.sign(15, 3, 46, "oak_wall_sign[facing=south]", "TANKOWANIE", "NA ZESZYT", "SKOŃCZONE!", "- Mirek");
            // wyciek
            for (int[] p : new int[][]{{21, 46}, {22, 46}, {22, 42}, {23, 42}}) {
                bp.set(p[0], 0, p[1], "black_concrete");
            }
        }

        /** Ogrodzony zbiornik gazu na tylnym placu. */
        private void gasTank() {
            for (int x = 37; x <= 45; x++) {
                for (int z = 40; z <= 46; z++) {
                    boolean edge = x == 37 || x == 45 || z == 40 || z == 46;
                    bp.set(x, 0, z, "gravel");
                    if (edge && !(z == 40 && x == 41)) {
                        bp.set(x, 1, z, "iron_bars");
                        bp.set(x, 2, z, "iron_bars");
                    }
                }
            }
            bp.set(41, 1, 40, "iron_door[half=lower,facing=north,hinge=left,open=true,powered=false]");
            bp.set(41, 2, 40, "iron_door[half=upper,facing=north,hinge=left,open=true,powered=false]");
            bp.set(39, 1, 43, "stone_brick_wall");
            bp.set(43, 1, 43, "stone_brick_wall");
            bp.set(39, 1, 44, "stone_brick_wall");
            bp.set(43, 1, 44, "stone_brick_wall");
            bp.fill(39, 2, 43, 43, 3, 44, "white_concrete");
            bp.fill(38, 2, 43, 38, 3, 44, "smooth_quartz");
            bp.fill(44, 2, 43, 44, 3, 44, "smooth_quartz");
            bp.set(41, 4, 43, "lightning_rod[facing=up]");
            bp.sign(42, 2, 40, "oak_wall_sign[facing=north]", "GAZ!", "NIE PALIĆ", "serio.", "- szef");
            bp.set(42, 2, 41, "white_concrete");
            bp.crate(40, 1, 45, "north", "paliwo");
        }

        // ------------------------------------------------------------------ zniszczenia i ślady życia

        private void decay() {
            for (int x = BX1 + 1; x < BX2; x++) {
                for (int z = BZ1 + 1; z < BZ2; z++) {
                    if (bp.isAir(x, ROOF_Y - 1, z) && chance(0.035)) {
                        bp.set(x, ROOF_Y - 1, z, "cobweb");
                    }
                    if (bp.isAir(x, 1, z) && chance(0.02)) {
                        bp.set(x, 1, z, chance(0.5) ? "gray_carpet" : "white_carpet");
                    }
                }
            }
            // dziury w dachu sali (omijamy drabinę i obozowisko)
            for (int i = 0; i < 3; i++) {
                bp.set(26 + rnd.nextInt(5), ROOF_Y, 28 + rnd.nextInt(3), "air");
            }
            // pnącza na zachodniej ścianie myjni i tylnej pawilonu
            for (int z = 25; z <= 37; z += 3) {
                for (int y = 2; y <= 5; y++) {
                    if (chance(0.7)) {
                        bp.set(WX1 - 1, y, z, "vine[east=true,north=false,south=false,up=false,west=false]");
                    }
                }
            }
            for (int x = 29; x <= 43; x += 4) {
                for (int y = 3; y <= 5; y++) {
                    if (chance(0.6) && bp.isAir(x, y, BZ2 + 1)) {
                        bp.set(x, y, BZ2 + 1, "vine[east=false,north=true,south=false,up=false,west=false]");
                    }
                }
            }
        }

        private void lifeSigns() {
            furniture.traces(BX1 + 1, BZ1 + 1, BX2 - 1, 31, 1, 3, 4);
            furniture.traces(35, 33, BX2 - 1, BZ2 - 1, 1, 1, 2);
            furniture.traces(2, 1, 44, 20, 1, 2, 0);
            furniture.graffiti(BX1 + 1, BZ1 + 1, BX2 - 1, 31, 2, "DZIEŃ 112", "BENZYNY", "NIE MA", "- K.");
            furniture.graffiti(WX1 + 1, BZ1 + 1, WX2 - 1, BZ2 - 1, 2, "OAZA ŚWIT", "ZA LASEM", "NA POŁUDNIE", "");
            furniture.collapsedTent(2, 40, 12, 46, 1, "green");
            furniture.skeleton(30, 1, 31, "east");
            furniture.skeleton(9, 1, 34, "north");
        }
    }
}
