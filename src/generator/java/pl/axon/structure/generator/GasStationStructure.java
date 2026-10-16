package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.Random;

/**
 * Opuszczona stacja benzynowa (Oxum's Gas Station) z Rusta – monument Tier 0.
 * <p>
 * Plan w lokalnych współrzędnych (front na północy, z = 0):
 * <pre>
 *  z 0-2    droga
 *  z 3-17   plac z wiatą (z 5-15), wyspą dystrybutorów (z 9-11), pickupem i wrakiem auta
 *  z 20-44  budynek x 6-38:
 *             sala sklepowa z 21-34 (lada, regały, lodówki),
 *             ściana działowa z 35,
 *             korytarz z 36-37, toalety + pokój z drabiną z 39-43, magazyn z recyklerem x 22-37
 *  z 45-51  podwórko ze śmietnikiem; boczna rampa po zachodniej stronie
 *  dach y 7, attyka y 8, szyld OXUM'S y 9-14
 * </pre>
 */
public final class GasStationStructure implements StructureDefinition {

    private static final int WIDTH = 45;
    private static final int HEIGHT = 16;
    private static final int DEPTH = 52;

    // budynek
    private static final int BX1 = 6;
    private static final int BX2 = 38;
    private static final int BZ1 = 20;
    private static final int BZ2 = 44;
    private static final int ROOF_Y = 7;

    @Override
    public String id() {
        return "stacja";
    }

    @Override
    public String displayName() {
        return "Opuszczona stacja benzynowa";
    }

    @Override
    public String description() {
        return "Oxum's Gas Station, Tier 0, recykler na zapleczu";
    }

    @Override
    public Blueprint build(Random random) {
        return new Builder(random).build();
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
        private final Palette crates;

        Builder(Random rnd) {
            this.rnd = rnd;
            asphalt = new Palette(rnd)
                    .add("gray_concrete", 14).add("andesite", 3).add("gravel", 2)
                    .add("tuff", 2).add("coarse_dirt", 1).add("cobblestone", 1);
            outerWall = new Palette(rnd)
                    .add("light_gray_concrete", 12).add("stone", 2).add("cracked_stone_bricks", 2)
                    .add("light_gray_concrete_powder", 2).add("andesite", 1);
            innerWall = new Palette(rnd)
                    .add("white_terracotta", 8).add("light_gray_terracotta", 2).add("calcite", 1);
            roof = new Palette(rnd)
                    .add("gray_concrete", 8).add("light_gray_concrete", 3).add("andesite", 2).add("gravel", 1);
            yard = new Palette(rnd)
                    .add("grass_block", 10).add("coarse_dirt", 4).add("gravel", 2).add("dirt", 2).add("dirt_path", 1);
            sidewalk = new Palette(rnd)
                    .add("smooth_stone", 8).add("stone_bricks", 3).add("cracked_stone_bricks", 2).add("andesite", 1);
            crates = new Palette(rnd)
                    .add("spruce_planks", 3).add("stripped_spruce_wood[axis=y]", 2).add("barrel[facing=up,open=false]", 1);
        }

        Blueprint build() {
            ground();
            forecourt();
            canopy();
            pumpIsland();
            pickup();
            sedan();
            priceSign();
            buildingShell();
            shopFloor();
            backRooms();
            recyclerRoom();
            roofTop();
            billboard();
            backYard();
            sideRamp();
            weathering();
            return bp;
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        // ------------------------------------------------------------------ teren

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    String block;
                    if (z <= 2) {
                        // droga z przerywaną linią
                        block = (z == 1 && x % 5 < 3) ? (chance(0.15) ? "gray_concrete" : "yellow_concrete")
                                : (chance(0.85) ? "black_concrete" : "gray_concrete");
                    } else if (z <= 17 && x >= 0 && x <= 44) {
                        block = asphalt.next();
                    } else if (x >= BX1 - 2 && x <= BX2 + 2 && z >= BZ1 - 2 && z <= BZ2 + 1) {
                        block = sidewalk.next();
                    } else {
                        block = yard.next();
                        if (block.equals("grass_block") && chance(0.25)) {
                            bp.set(x, 1, z, chance(0.85) ? "short_grass" : "dead_bush");
                        }
                    }
                    bp.set(x, 0, z, block);
                }
            }
        }

        private void forecourt() {
            // zjazd z drogi – białe pasy
            for (int x = 12; x <= 32; x += 4) {
                bp.set(x, 0, 3, "white_concrete");
                bp.set(x, 0, 4, "white_concrete");
            }
            // betonowe zapory przy drodze
            for (int x = 36; x <= 39; x++) {
                if (x != 38) {
                    bp.set(x, 1, 3, "stone_brick_wall");
                }
            }
            bp.set(28, 1, 17, "stone_brick_wall");
            bp.set(29, 1, 17, "stone_brick_wall");
            // porzucone opony
            tires(1, 13);
            tires(41, 9);
            tires(40, 15);
            bp.set(20, 1, 17, "blackstone_wall");
            // przewrócony znak drogowy
            bp.set(39, 1, 12, "spruce_fence");
            bp.fill(38, 1, 13, 38, 1, 14, "stripped_spruce_wood[axis=z]");
            bp.sign(38, 1, 15, "oak_wall_sign[facing=south]", "", "STOP", "", "");
            // pionowy znak przy wjeździe
            bp.fill(1, 1, 4, 1, 3, 4, "spruce_fence");
            bp.sign(1, 4, 4, "oak_sign[rotation=8]", "OXUM'S", "GAS", "→ 50 m", "");
            // kałuża paliwa
            bp.set(25, 0, 13, "black_concrete");
            bp.set(26, 0, 13, "black_concrete");
            bp.set(26, 0, 14, "black_concrete");
        }

        private void tires(int x, int z) {
            bp.set(x, 1, z, "blackstone_wall");
            if (chance(0.7)) {
                bp.set(x, 2, z, "blackstone_wall");
            }
            bp.set(x + 1, 1, z + 1, "blackstone_wall");
        }

        // ------------------------------------------------------------------ wiata i dystrybutory

        private void canopy() {
            int[] pillarsX = {9, 22, 35};
            int[] pillarsZ = {6, 14};
            for (int px : pillarsX) {
                for (int pz : pillarsZ) {
                    bp.fill(px, 1, pz, px, 5, pz, "polished_andesite");
                    bp.set(px, 1, pz, "smooth_stone");
                }
            }
            for (int x = 8; x <= 36; x++) {
                for (int z = 5; z <= 15; z++) {
                    boolean edge = x == 8 || x == 36 || z == 5 || z == 15;
                    String block = edge ? (chance(0.85) ? "red_concrete" : "red_terracotta")
                            : (chance(0.9) ? "white_concrete" : "light_gray_concrete");
                    if (!edge && chance(0.05)) {
                        block = "air"; // dziury w zadaszeniu
                    }
                    bp.set(x, 6, z, block);
                    if (edge && chance(0.8)) {
                        bp.set(x, 5, z, "white_concrete"); // pas reklamowy pod krawędzią
                    }
                }
            }
            // martwe lampy w suficie wiaty
            for (int x = 12; x <= 32; x += 5) {
                bp.set(x, 6, 8, "redstone_lamp");
                bp.set(x, 6, 12, "redstone_lamp");
            }
            // zawalony narożnik
            for (int x = 31; x <= 36; x++) {
                for (int z = 5; z <= 8; z++) {
                    bp.set(x, 6, z, "air");
                    bp.set(x, 5, z, "air");
                }
            }
            bp.set(35, 6, 6, "polished_andesite");
            bp.fill(33, 4, 5, 33, 5, 5, "red_concrete");
            bp.set(34, 5, 6, "white_concrete");
            bp.set(32, 3, 6, "white_concrete");
            bp.set(33, 2, 7, "white_concrete");
            bp.set(34, 1, 7, "red_concrete");
            bp.set(32, 1, 5, "white_concrete");
            bp.set(31, 1, 6, "light_gray_concrete");
        }

        private void pumpIsland() {
            for (int x = 11; x <= 33; x++) {
                for (int z = 9; z <= 11; z++) {
                    bp.set(x, 1, z, "smooth_stone_slab[type=bottom]");
                }
            }
            pump(14, false);
            pump(22, false);
            pump(30, true);
            // beczki obok dystrybutorów
            bp.crate(16, 1, 8, "north", "paliwo");
            bp.crate(24, 1, 12, "north", "paliwo");
            bp.crate(12, 1, 12, "north", "plac");
            bp.crate(27, 1, 8, "north", "plac");
            bp.crate(33, 1, 12, "north", "plac");
        }

        private void pump(int x, boolean toppled) {
            bp.set(x, 1, 10, "smooth_stone");
            if (toppled) {
                // przewrócony dystrybutor leży na wyspie
                bp.set(x + 1, 2, 10, "white_concrete");
                bp.set(x + 2, 2, 10, "red_terracotta");
                bp.set(x + 3, 2, 10, "white_concrete");
                bp.set(x + 1, 2, 9, "lever[face=wall,facing=north,powered=false]");
                return;
            }
            bp.set(x, 2, 10, "white_concrete");
            bp.set(x, 3, 10, chance(0.5) ? "red_terracotta" : "red_concrete");
            bp.set(x, 4, 10, "smooth_stone_slab[type=bottom]");
            bp.set(x, 3, 9, "black_stained_glass_pane");
            bp.set(x, 2, 9, "lever[face=wall,facing=north,powered=false]");
            bp.set(x, 2, 11, "lever[face=wall,facing=south,powered=false]");
            bp.set(x + 1, 2, 10, "chain[axis=y]");
        }

        // ------------------------------------------------------------------ pojazdy

        /** Pickup przy wiacie; razem ze skrzyniami tworzy drogę na dach wiaty. */
        private void pickup() {
            String body = "red_terracotta";
            // podwozie
            bp.fill(3, 1, 3, 5, 1, 9, body);
            bp.set(3, 1, 4, "black_concrete");
            bp.set(5, 1, 4, "black_concrete");
            bp.set(3, 1, 8, "black_concrete");
            bp.set(5, 1, 8, "black_concrete");
            bp.set(3, 1, 3, "redstone_lamp");
            bp.set(5, 1, 3, "redstone_lamp");
            // kabina
            bp.set(3, 2, 3, "glass_pane");
            bp.set(4, 2, 3, chance(0.5) ? "air" : "glass_pane");
            bp.set(5, 2, 3, "glass_pane");
            bp.set(3, 2, 4, "glass_pane");
            bp.set(5, 2, 4, "air");
            bp.set(4, 2, 5, "spruce_stairs[facing=south,half=bottom]");
            bp.set(3, 2, 5, body);
            bp.set(5, 2, 5, body);
            bp.fill(3, 3, 3, 5, 3, 5, body);
            // paka z beczką
            bp.set(3, 2, 6, body);
            bp.set(5, 2, 6, body);
            bp.fill(3, 2, 7, 3, 2, 9, body);
            bp.fill(5, 2, 7, 5, 2, 9, body);
            bp.crate(4, 2, 7, "north", "plac");
            bp.set(4, 2, 9, "spruce_trapdoor[facing=north,half=bottom,open=true]");
            // skrzynie ustawione w schodki: kabina (3) -> 4 -> 5 -> dach wiaty (6)
            bp.fill(6, 1, 5, 6, 4, 5, crates);
            bp.fill(7, 1, 5, 7, 5, 5, crates);
        }

        private void sedan() {
            String body = chance(0.5) ? "brown_terracotta" : "light_blue_terracotta";
            bp.fill(15, 1, 13, 20, 1, 15, body);
            bp.set(15, 1, 13, "black_concrete");
            bp.set(15, 1, 15, "black_concrete");
            bp.set(20, 1, 13, "black_concrete");
            bp.set(20, 1, 15, "black_concrete");
            bp.fill(16, 2, 13, 19, 2, 13, "glass_pane");
            bp.fill(16, 2, 15, 19, 2, 15, "glass_pane");
            bp.set(17, 2, 13, "air");
            bp.set(16, 2, 14, "glass_pane");
            bp.set(19, 2, 14, "glass_pane");
            bp.set(17, 2, 14, "spruce_stairs[facing=east,half=bottom]");
            bp.fill(16, 3, 13, 19, 3, 15, body);
            bp.set(18, 3, 14, "air"); // zgnieciony dach
        }

        private void priceSign() {
            bp.fill(42, 1, 5, 42, 5, 5, "polished_andesite");
            bp.fill(41, 6, 5, 43, 8, 5, "white_concrete");
            bp.fill(41, 8, 5, 43, 8, 5, "red_concrete");
            bp.set(43, 6, 5, "air");
            bp.sign(42, 7, 4, "oak_wall_sign[facing=north]", "OXUM'S", "Benzyna 95", "4,99 zł/l", "Zamknięte");
        }

        // ------------------------------------------------------------------ budynek

        private void buildingShell() {
            // ściany zewnętrzne
            for (int y = 1; y <= 6; y++) {
                for (int x = BX1; x <= BX2; x++) {
                    bp.set(x, y, BZ1, outerWall.next());
                    bp.set(x, y, BZ2, outerWall.next());
                }
                for (int z = BZ1; z <= BZ2; z++) {
                    bp.set(BX1, y, z, outerWall.next());
                    bp.set(BX2, y, z, outerWall.next());
                }
            }
            // pas cokołu i czerwony pas reklamowy od frontu
            for (int x = BX1; x <= BX2; x++) {
                bp.set(x, 1, BZ1, "polished_andesite");
                bp.set(x, 6, BZ1, chance(0.85) ? "red_concrete" : "red_terracotta");
            }
            // filary frontu i witryny
            int[] pillars = {6, 12, 18, 20, 24, 26, 32, 38};
            for (int x = BX1 + 1; x < BX2; x++) {
                if (contains(pillars, x)) {
                    bp.fill(x, 1, BZ1, x, 5, BZ1, "polished_andesite");
                    continue;
                }
                for (int y = 2; y <= 4; y++) {
                    bp.set(x, y, BZ1, brokenGlass());
                }
            }
            // wejście: jedno skrzydło otwarte, drugie wyrwane
            bp.fill(21, 1, BZ1, 23, 3, BZ1, "air");
            bp.fill(21, 4, BZ1, 23, 4, BZ1, outerWall);
            door(21, 1, BZ1, "dark_oak_door", "north", "left", true);
            bp.set(23, 1, BZ1 - 2, "dark_oak_trapdoor[facing=north,half=bottom,open=false]");
            // boczne okna sali sklepowej
            for (int z = 22; z <= 33; z++) {
                if ((z - 22) % 4 == 3 || (z >= 24 && z <= 27)) {
                    continue;
                }
                for (int y = 2; y <= 4; y++) {
                    bp.set(BX1, y, z, brokenGlass());
                }
            }
            // wąskie, wysokie okna magazynu
            for (int z = 37; z <= 42; z++) {
                if (z % 2 == 1) {
                    bp.set(BX2, 4, z, "iron_bars");
                    bp.set(BX2, 5, z, "iron_bars");
                }
            }
            // podłogi
            for (int x = BX1 + 1; x < BX2; x++) {
                for (int z = BZ1 + 1; z < BZ2; z++) {
                    String floor;
                    if (z <= 34) {
                        floor = (x + z) % 2 == 0 ? "white_concrete" : "light_gray_concrete";
                        if (chance(0.06)) {
                            floor = chance(0.5) ? "light_gray_concrete_powder" : "cracked_stone_bricks";
                        }
                    } else {
                        floor = chance(0.9) ? "polished_andesite" : "andesite";
                    }
                    bp.set(x, 0, z, floor);
                }
            }
            // dach z attyką
            for (int x = BX1; x <= BX2; x++) {
                for (int z = BZ1; z <= BZ2; z++) {
                    bp.set(x, ROOF_Y, z, roof.next());
                    if (x == BX1 || x == BX2 || z == BZ1 || z == BZ2) {
                        bp.set(x, ROOF_Y + 1, z, chance(0.92) ? "light_gray_concrete" : "air");
                    }
                }
            }
            // przerzucone deski z dachu wiaty na dach budynku + przerwa w attyce
            for (int z = 16; z <= 19; z++) {
                bp.set(21, ROOF_Y, z, "spruce_slab[type=bottom]");
                bp.set(22, ROOF_Y, z, z == 18 ? "air" : "spruce_slab[type=bottom]");
            }
            bp.set(21, ROOF_Y + 1, BZ1, "air");
            bp.set(22, ROOF_Y + 1, BZ1, "air");
            // tylne wyjścia: na śmietnik i w stronę bocznej rampy
            door(27, 1, BZ2, "spruce_door", "south", "left", true);
            door(BX1, 1, 37, "spruce_door", "west", "right", false);
        }

        private String brokenGlass() {
            double roll = rnd.nextDouble();
            if (roll < 0.35) {
                return "air";
            }
            if (roll < 0.40) {
                return "cobweb";
            }
            return chance(0.2) ? "light_gray_stained_glass_pane" : "glass_pane";
        }

        private void door(int x, int y, int z, String type, String facing, String hinge, boolean open) {
            String props = "facing=" + facing + ",hinge=" + hinge + ",open=" + open + ",powered=false";
            bp.set(x, y, z, type + "[half=lower," + props + "]");
            bp.set(x, y + 1, z, type + "[half=upper," + props + "]");
        }

        private void wallSegment(int x1, int z1, int x2, int z2) {
            bp.fill(x1, 1, z1, x2, 6, z2, innerWall);
        }

        // ------------------------------------------------------------------ sala sklepowa

        private void shopFloor() {
            // lada kasowa w kształcie L przy lewej ścianie
            for (int z = 22; z <= 30; z++) {
                bp.set(11, 1, z, "polished_andesite");
            }
            for (int x = 7; x <= 10; x++) {
                if (x != 9) {
                    bp.set(x, 1, 30, "polished_andesite");
                }
            }
            bp.set(11, 2, 27, "smithing_table"); // kasa fiskalna
            bp.set(11, 2, 29, "lantern[hanging=false]");
            // zielona karta leży na ladzie, skrzynka z jedzeniem tuż obok lady
            bp.crate(10, 1, 23, "north", "lada");
            bp.frame(11, 2, 24, "up", "karta");
            // telefon na ścianie za ladą
            bp.set(BX1 + 1, 2, 25, "tripwire_hook[facing=east,attached=false,powered=false]");
            bp.set(BX1 + 1, 2, 26, "stone_button[face=wall,facing=east,powered=false]");
            bp.sign(BX1 + 1, 3, 25, "oak_wall_sign[facing=east]", "TELEFON", "Oxum's Gas", "nr 0001", "");
            bp.set(8, 1, 21, "white_terracotta");
            bp.set(8, 2, 21, "flower_pot");

            // regały w alejkach (jeden przewrócony)
            shelfRow(15, 24, 32);
            shelfRow(20, 25, 32);
            fallenShelf(25, 24, 32);
            shelfRow(30, 24, 32);
            // skrzynki w alejkach
            bp.crate(18, 1, 28, "north", "sklep");
            bp.crate(23, 1, 30, "west", "sklep");
            bp.crate(28, 1, 26, "north", "sklep");
            bp.crate(33, 1, 23, "east", "sklep");

            // lodówki wzdłuż prawej ściany
            for (int z = 22; z <= 33; z++) {
                int x = BX2 - 1;
                bp.set(x, 1, z, "white_concrete");
                for (int y = 2; y <= 3; y++) {
                    double roll = rnd.nextDouble();
                    bp.set(x, y, z, roll < 0.25 ? "air" : roll < 0.3 ? "cobweb" : "white_stained_glass");
                }
                bp.set(x, 4, z, "smooth_quartz_slab[type=bottom]");
            }
            bp.crate(BX2 - 2, 1, 25, "west", "lodowki");
            bp.crate(BX2 - 2, 1, 31, "west", "lodowki");

            // oświetlenie i gruz
            for (int x = 12; x <= 34; x += 7) {
                for (int z = 23; z <= 33; z += 5) {
                    bp.set(x, 6, z, chance(0.75) ? "lantern[hanging=true]" : "chain[axis=y]");
                }
            }
        }

        private void shelfRow(int x, int z1, int z2) {
            for (int z = z1; z <= z2; z++) {
                if (chance(0.08)) {
                    continue;
                }
                for (int dx = 0; dx <= 1; dx++) {
                    bp.set(x + dx, 1, z, chance(0.85) ? "exposed_cut_copper" : "weathered_cut_copper");
                    bp.set(x + dx, 2, z, chance(0.8) ? "weathered_copper_grate" : "air");
                    bp.set(x + dx, 3, z, "oxidized_cut_copper_slab[type=top]");
                }
                if (chance(0.15)) {
                    bp.set(x, 4, z, chance(0.5) ? "decorated_pot[facing=north]" : "candle[candles=2,lit=false]");
                }
            }
        }

        private void fallenShelf(int x, int z1, int z2) {
            for (int z = z1; z <= z2; z++) {
                if (chance(0.1)) {
                    continue;
                }
                bp.set(x, 1, z, "weathered_cut_copper_slab[type=bottom]");
                bp.set(x + 1, 1, z, "weathered_copper_grate");
                bp.set(x + 2, 1, z, chance(0.6) ? "oxidized_cut_copper_slab[type=bottom]" : "air");
            }
        }

        // ------------------------------------------------------------------ zaplecze: korytarz, toalety, drabina

        private void backRooms() {
            // ściana działowa sklep / zaplecze
            wallSegment(BX1 + 1, 35, BX2 - 1, 35);
            door(16, 1, 35, "spruce_door", "south", "left", true);
            bp.fill(28, 1, 35, 29, 3, 35, "air"); // wybita dziura do magazynu
            bp.set(30, 1, 35, "cracked_stone_bricks");
            bp.set(27, 3, 35, "air");
            bp.sign(15, 3, 34, "oak_wall_sign[facing=north]", "ZAPLECZE", "Tylko dla", "personelu", "");

            // korytarz z = 36-37, ściana od toalet z = 38
            wallSegment(BX1 + 1, 38, 20, 38);
            wallSegment(21, 36, 21, 43);
            wallSegment(10, 39, 10, 43);
            wallSegment(14, 39, 14, 43);
            door(21, 1, 37, "spruce_door", "east", "left", true);
            bp.sign(20, 3, 36, "oak_wall_sign[facing=west]", "MAGAZYN", "→", "", "");
            door(8, 1, 38, "birch_door", "south", "left", false);
            door(12, 1, 38, "birch_door", "south", "right", true);
            door(17, 1, 38, "spruce_door", "south", "left", true);
            bp.sign(10, 3, 37, "oak_wall_sign[facing=north]", "WC", "", "", "");
            bp.crate(BX1 + 1, 1, 36, "north", "toalety");
            bp.set(15, 6, 36, "lantern[hanging=true]");

            // toalety
            for (int x : new int[]{8, 12}) {
                bp.set(x, 1, 42, "cauldron");
                bp.set(x, 1, 43, "smooth_quartz_stairs[facing=north,half=bottom]");
                bp.set(x, 4, BZ2, "white_stained_glass");
                for (int z = 39; z <= 43; z++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        bp.set(x + dx, 0, z, (dx + z) % 2 == 0 ? "white_concrete" : "light_blue_terracotta");
                    }
                }
            }
            bp.crate(7, 1, 40, "north", "toalety");
            bp.crate(13, 1, 40, "north", "toalety");
            bp.set(11, 5, 41, "cobweb");

            // pokój z drabiną na dach
            for (int y = 1; y <= ROOF_Y; y++) {
                bp.set(18, y, BZ2 - 1, "ladder[facing=north]");
            }
            bp.set(18, ROOF_Y + 1, BZ2 - 1, "iron_trapdoor[facing=north,half=bottom,open=true]");
            bp.crate(15, 1, 43, "east", "zaplecze");
            bp.set(20, 1, 43, "scaffolding[bottom=false,distance=0]");
            bp.set(20, 1, 42, "barrel[facing=up,open=false]");
            bp.set(17, 6, 41, "lantern[hanging=true]");
        }

        // ------------------------------------------------------------------ magazyn z recyklerem

        private void recyclerRoom() {
            recycler(28, 42);
            bp.sign(29, 4, 43, "oak_wall_sign[facing=north]", "RECYKLER", "Złom → części", "", "");
            // regały magazynowe po obu stronach recyklera
            rack(23, 26, 43);
            rack(32, 36, 43);
            bp.crate(24, 2, 43, "north", "magazyn-regal");
            bp.set(25, 2, 43, "barrel[facing=north,open=false]");
            bp.set(24, 4, 43, "decorated_pot[facing=north]");
            bp.set(33, 2, 43, "barrel[facing=north,open=false]");
            bp.set(35, 4, 43, "barrel[facing=north,open=false]");
            bp.crate(34, 2, 43, "north", "magazyn-regal");
            // regał wzdłuż prawej ściany
            for (int z = 37; z <= 41; z++) {
                bp.set(BX2 - 1, 1, z, "spruce_slab[type=top]");
                bp.set(BX2 - 1, 3, z, "spruce_slab[type=top]");
            }
            bp.fill(BX2 - 1, 1, 36, BX2 - 1, 4, 36, "stripped_spruce_log[axis=y]");
            bp.set(BX2 - 1, 2, 38, "barrel[facing=west,open=false]");
            bp.crate(BX2 - 1, 2, 40, "west", "magazyn");
            // beczki i paliwo w magazynie
            bp.crate(23, 1, 37, "north", "magazyn");
            bp.crate(24, 1, 37, "north", "paliwo");
            bp.fill(31, 1, 37, 32, 1, 38, "spruce_slab[type=bottom]"); // paleta
            bp.set(31, 2, 37, "barrel[facing=up,open=false]");
            bp.crate(32, 2, 38, "north", "magazyn");
            for (int x = 24; x <= 36; x += 6) {
                bp.set(x, 6, 39, "chain[axis=y]");
                bp.set(x, 5, 39, "lantern[hanging=true]");
            }
        }

        /** Atrapa recyklera z Rusta: 3 x 2 x 3 maszyneria z lejem, piłą i szlifierką. */
        private void recycler(int x, int z) {
            bp.fill(x, 1, z, x + 2, 1, z + 1, "iron_block");
            bp.set(x + 1, 1, z, "blast_furnace[facing=north,lit=false]");
            bp.set(x, 2, z, "grindstone[face=floor,facing=north]");
            bp.set(x + 1, 2, z, "hopper[facing=down,enabled=true]");
            bp.set(x + 2, 2, z, "stonecutter[facing=north]");
            bp.set(x, 2, z + 1, "iron_block");
            bp.set(x + 1, 2, z + 1, "chiseled_copper");
            bp.set(x + 2, 2, z + 1, "iron_block");
            bp.set(x, 3, z + 1, "lightning_rod[facing=up]");
            bp.set(x + 1, 3, z + 1, "iron_trapdoor[facing=north,half=bottom,open=false]");
            bp.set(x + 2, 3, z + 1, "lever[face=floor,facing=north,powered=false]");
            bp.set(x - 1, 1, z, "yellow_carpet");
            bp.set(x + 3, 1, z, "yellow_carpet");
            bp.set(x - 1, 1, z - 1, "black_carpet");
            bp.set(x + 3, 1, z - 1, "black_carpet");
        }

        private void rack(int x1, int x2, int z) {
            bp.fill(x1, 1, z, x1, 4, z, "stripped_spruce_log[axis=y]");
            bp.fill(x2, 1, z, x2, 4, z, "stripped_spruce_log[axis=y]");
            for (int x = x1 + 1; x < x2; x++) {
                bp.set(x, 1, z, "spruce_slab[type=top]");
                bp.set(x, 3, z, "spruce_slab[type=top]");
            }
        }

        // ------------------------------------------------------------------ dach

        private void roofTop() {
            acUnit(9, 39);
            acUnit(30, 24);
            acUnit(34, 33);
            // wywietrzniki i rury
            for (int[] vent : new int[][]{{14, 30}, {26, 41}, {20, 26}}) {
                bp.set(vent[0], ROOF_Y + 1, vent[1], "copper_grate");
                bp.set(vent[0], ROOF_Y + 2, vent[1], "oxidized_cut_copper_slab[type=bottom]");
            }
            bp.set(12, ROOF_Y + 1, 34, "lightning_rod[facing=up]");
            bp.set(12, ROOF_Y + 2, 34, "lightning_rod[facing=up]");
            bp.set(36, ROOF_Y + 1, 42, "lightning_rod[facing=up]");
            for (int x = 22; x <= 30; x++) {
                bp.set(x, ROOF_Y + 1, 43, "chain[axis=x]");
            }
            // 1-2 skrzynie na dachu
            bp.crate(33, ROOF_Y + 1, 40, "north", "dach");
            bp.crate(12, ROOF_Y + 1, 25, "south", "dach");
            bp.crate(25, ROOF_Y + 1, 31, "north", "dach");
            // osłona snajperska z worków (wełna) przy przedniej krawędzi
            bp.set(16, ROOF_Y + 1, 21, "brown_wool");
            bp.set(17, ROOF_Y + 1, 21, "brown_wool");
            bp.set(28, ROOF_Y + 1, 21, "brown_wool");
        }

        private void acUnit(int x, int z) {
            boolean broken = chance(0.3);
            bp.fill(x, ROOF_Y + 1, z, x + 2, ROOF_Y + 1, z + 1, broken ? "weathered_copper" : "iron_block");
            bp.fill(x, ROOF_Y + 2, z, x + 2, ROOF_Y + 2, z + 1, "daylight_detector[inverted=false,power=0]");
            bp.set(x + 1, ROOF_Y + 1, z + 2, "iron_bars");
        }

        // ------------------------------------------------------------------ szyld OXUM'S

        private static final String[][] FONT = {
                // O
                {"###", "#.#", "#.#", "#.#", "###"},
                // X
                {"#.#", "#.#", ".#.", "#.#", "#.#"},
                // U
                {"#.#", "#.#", "#.#", "#.#", "###"},
                // M
                {"#...#", "##.##", "#.#.#", "#...#", "#...#"},
                // '
                {"#", "#", ".", ".", "."},
                // S
                {"###", "#..", "###", "..#", "###"},
        };

        private void billboard() {
            int totalWidth = -1;
            for (String[] glyph : FONT) {
                totalWidth += glyph[0].length() + 1;
            }
            // tekst czytany od frontu (patrząc na południe) biegnie od dużych x do małych
            int startX = (BX1 + BX2) / 2 + totalWidth / 2;
            int z = BZ1 + 1;
            int topY = ROOF_Y + 7;
            for (int x = startX - totalWidth + 1; x <= startX; x++) {
                if (x == 21 || x == 22) {
                    continue; // przejście z desek na dach
                }
                bp.set(x, ROOF_Y + 2, z, "iron_bars");
            }
            for (int x = startX - totalWidth + 1; x <= startX; x += 4) {
                bp.set(x, ROOF_Y + 1, z, "iron_bars");
            }
            bp.set(startX, ROOF_Y + 1, z, "iron_bars");
            int column = 0;
            for (String[] glyph : FONT) {
                int glyphWidth = glyph[0].length();
                for (int c = 0; c < glyphWidth; c++) {
                    int x = startX - column - c;
                    for (int row = 0; row < glyph.length; row++) {
                        if (glyph[row].charAt(c) != '#') {
                            continue;
                        }
                        int y = topY - row;
                        if (chance(0.12)) {
                            // odpadnięty fragment litery leży na dachu
                            bp.set(x, ROOF_Y + 1, z + 1 + rnd.nextInt(3), "red_concrete");
                            continue;
                        }
                        bp.set(x, y, z, chance(0.9) ? "red_concrete" : "red_terracotta");
                    }
                }
                column += glyphWidth + 1;
            }
        }

        // ------------------------------------------------------------------ podwórko i rampa

        private void backYard() {
            // betonowy plac za budynkiem
            for (int x = 26; x <= 38; x++) {
                for (int z = 45; z <= 49; z++) {
                    if (bp.get(x, 0, z).contains("grass")) {
                        bp.set(x, 1, z, "air");
                    }
                    bp.set(x, 0, z, chance(0.85) ? "smooth_stone" : "gravel");
                }
            }
            // śmietnik przy tylnych drzwiach
            bp.fill(32, 1, 46, 35, 2, 47, "green_terracotta");
            bp.fill(32, 3, 46, 35, 3, 47, "dark_oak_trapdoor[facing=north,half=bottom,open=false]");
            bp.set(34, 3, 46, "dark_oak_trapdoor[facing=north,half=bottom,open=true]");
            bp.set(31, 1, 46, "black_wool");
            bp.set(36, 1, 47, "black_wool");
            bp.set(36, 1, 46, "black_carpet");
            bp.set(31, 1, 48, "composter[level=0]");
            bp.crate(28, 1, 47, "north", "plac");
            bp.crate(37, 1, 49, "north", "paliwo");
            // stara siatka ogrodzenia
            for (int x = 2; x <= 42; x++) {
                if (chance(0.75)) {
                    bp.set(x, 1, 51, "iron_bars");
                    if (chance(0.6)) {
                        bp.set(x, 2, 51, "iron_bars");
                    }
                }
            }
        }

        /** Boczna rampa załadunkowa przy zachodnim wyjściu z korytarza. */
        private void sideRamp() {
            for (int x = 1; x <= 5; x++) {
                for (int z = 33; z <= 43; z++) {
                    bp.set(x, 0, z, "smooth_stone");
                    bp.set(x, 1, z, "air");
                }
            }
            bp.fill(1, 1, 39, 4, 1, 43, "polished_andesite");
            for (int x = 1; x <= 4; x++) {
                bp.set(x, 1, 38, "polished_andesite_slab[type=bottom]");
            }
            for (int z = 39; z <= 43; z++) {
                bp.set(1, 2, z, "iron_bars");
            }
            bp.set(3, 2, 41, "spruce_slab[type=bottom]");
            bp.set(3, 3, 41, "barrel[facing=up,open=false]");
            bp.crate(2, 2, 42, "north", "zaplecze");
        }

        // ------------------------------------------------------------------ zniszczenia

        private void weathering() {
            for (int x = BX1 + 1; x < BX2; x++) {
                for (int z = BZ1 + 1; z < BZ2; z++) {
                    // pajęczyny pod sufitem
                    if (bp.isAir(x, 6, z) && chance(0.035)) {
                        bp.set(x, 6, z, "cobweb");
                    }
                    // gruz na podłodze
                    if (bp.isAir(x, 1, z) && chance(0.025)) {
                        bp.set(x, 1, z, chance(0.5) ? "gray_carpet" : "brown_carpet");
                    }
                }
            }
            // dziury w dachu (omijamy wejście z drabiny)
            for (int i = 0; i < 4; i++) {
                int x = 24 + rnd.nextInt(12);
                int z = 22 + rnd.nextInt(10);
                bp.set(x, ROOF_Y, z, "air");
            }
            // mech i pnącza na ścianach zewnętrznych
            for (int i = 0; i < 18; i++) {
                int x = BX1 + rnd.nextInt(BX2 - BX1 + 1);
                int y = 1 + rnd.nextInt(3);
                if (x == 27) {
                    continue; // tylne drzwi
                }
                bp.set(x, y, BZ2, chance(0.5) ? "mossy_stone_bricks" : "mossy_cobblestone");
            }
            for (int z = 22; z <= 32; z += 5) {
                for (int y = 3; y <= 6; y++) {
                    if (chance(0.7)) {
                        bp.set(BX2 + 1, y, z, "vine[east=false,north=false,south=false,up=false,west=true]");
                    }
                }
            }
        }

        private static boolean contains(int[] values, int value) {
            for (int v : values) {
                if (v == value) {
                    return true;
                }
            }
            return false;
        }
    }
}
