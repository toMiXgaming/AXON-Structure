package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.Random;

/**
 * Kryjówka czarnego rynku: z wierzchu zrujnowana chałupa na zarośniętym podwórku, pod nią piwnica pasera.
 * Mała struktura rozsiana po mapie, PvP działa normalnie – wyprawa po towar to ryzyko.
 * <p>
 * Plan w lokalnych współrzędnych (y względem gruntu, piwnica y -5..-1):
 * <pre>
 *  powierzchnia: chałupa x 3-11, z 9-16 (zejście drabiną w x 6, z 12), ruiny murków, wóz, stos drewna,
 *                drugie wyjście z piwnicy drabiną w x 20, z 17 (za murkiem)
 *  piwnica x 5-21, z 5-19: lada pasera z 9 (x 9-15), paser za ladą (x 12, z 7), składzik x 16-20, z 13-18
 * </pre>
 */
public final class BlackMarketStructure implements StructureDefinition {

    private static final int WIDTH = 27;
    private static final int DEPTH = 27;
    private static final int G = 6;
    private static final int HEIGHT = G + 9;

    @Override
    public String id() {
        return "czarny-rynek";
    }

    @Override
    public String displayName() {
        return "Kryjówka czarnego rynku";
    }

    @Override
    public String description() {
        return "zrujnowana chałupa z piwnicą pasera, rotujący drogi towar";
    }

    @Override
    public Blueprint build(Random random) {
        return new Builder(random).build();
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);
        private final Palette yard;
        private final Palette soil;
        private final Palette cellarWall;
        private final Palette cellarFloor;
        private final Palette shackWall;
        private final Palette rubble;

        Builder(Random rnd) {
            this.rnd = rnd;
            yard = new Palette(rnd)
                    .add("grass_block", 10).add("coarse_dirt", 4).add("dirt", 2).add("gravel", 1).add("podzol", 1);
            soil = new Palette(rnd)
                    .add("dirt", 8).add("stone", 3).add("gravel", 1).add("coarse_dirt", 1);
            cellarWall = new Palette(rnd)
                    .add("stone_bricks", 6).add("cracked_stone_bricks", 3).add("mossy_stone_bricks", 2).add("cobblestone", 2);
            cellarFloor = new Palette(rnd)
                    .add("spruce_planks", 5).add("cobblestone", 2).add("packed_mud", 1);
            shackWall = new Palette(rnd)
                    .add("spruce_planks", 6).add("stripped_spruce_log[axis=y]", 1).add("dark_oak_planks", 2);
            rubble = new Palette(rnd)
                    .add("cobblestone", 5).add("mossy_cobblestone", 3).add("stone", 1);
        }

        Blueprint build() {
            bp.ground(G);
            ground();
            cellar();
            counter();
            storeroom();
            shafts();
            shack();
            yardCover();
            return bp;
        }

        // ------------------------------------------------------------------ pomocnicze (y względem gruntu)

        private void set(int x, int y, int z, String state) {
            bp.set(x, y + G, z, state);
        }

        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
            bp.fill(x1, y1 + G, z1, x2, y2 + G, z2, state);
        }

        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Palette palette) {
            bp.fill(x1, y1 + G, z1, x2, y2 + G, z2, palette);
        }

        private void marker(int x, int y, int z, String facing, String pool) {
            bp.crate(x, y + G, z, facing, pool);
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        // ------------------------------------------------------------------ teren

        private void ground() {
            fill(0, -G, 0, WIDTH - 1, -1, DEPTH - 1, soil);
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    String block = yard.next();
                    set(x, 0, z, block);
                    if (block.equals("grass_block") && chance(0.3)) {
                        set(x, 1, z, chance(0.8) ? "short_grass" : chance(0.5) ? "fern" : "dead_bush");
                    }
                }
            }
        }

        // ------------------------------------------------------------------ piwnica

        private void cellar() {
            fill(5, -6, 5, 21, -1, 19, cellarWall);
            fill(6, -5, 6, 20, -5, 18, cellarFloor);
            fill(6, -4, 6, 20, -1, 18, "air");
            // belki stropu
            for (int x = 8; x <= 18; x += 5) {
                fill(x, -1, 6, x, -1, 18, "spruce_log[axis=z]");
            }
            for (int z : new int[]{8, 15}) {
                for (int x : new int[]{9, 17}) {
                    set(x, -1, z, "lantern[hanging=true,waterlogged=false]");
                }
            }
            // pajęczyny i zacieki
            for (int i = 0; i < 6; i++) {
                int x = 6 + rnd.nextInt(15);
                int z = 6 + rnd.nextInt(13);
                if (x != 6 && x != 20) {
                    set(x, -1, z, "cobweb");
                }
            }
        }

        /** Lada pasera: paser stoi za nią, klik w ladę albo w pasera otwiera towar. */
        private void counter() {
            fill(9, -4, 9, 15, -4, 9, "spruce_planks");
            set(9, -3, 9, "spruce_slab[type=bottom,waterlogged=false]");
            set(15, -3, 9, "spruce_slab[type=bottom,waterlogged=false]");
            set(10, -3, 9, "candle[candles=2,lit=true,waterlogged=false]");
            set(14, -3, 9, "lantern[hanging=false,waterlogged=false]");
            // lada pasera: blok-znacznik
            marker(12, -4, 9, "south", "punkt-czarny-rynek-lada");
            set(12, -4, 9, "barrel[facing=up,open=false]");
            // zaplecze pasera: regały z towarem
            fill(9, -4, 6, 15, -3, 6, "barrel[facing=south,open=false]");
            set(12, -2, 6, "chiseled_bookshelf[facing=south,slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=true,slot_5_occupied=false]");
            set(10, -2, 6, "red_wall_banner[facing=south]");
            set(14, -2, 6, "black_wall_banner[facing=south]");
            marker(12, -4, 7, "south", "punkt-czarny-rynek");
            // kącik dla klientów: stół i skrzynki do siedzenia
            set(11, -4, 13, "spruce_fence");
            set(11, -3, 13, "spruce_pressure_plate");
            set(10, -4, 13, "spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
            set(12, -4, 13, "spruce_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
        }

        private void storeroom() {
            // ścianka składziku z przejściem
            for (int z = 13; z <= 18; z++) {
                if (z != 15) {
                    set(16, -4, z, "spruce_planks");
                    set(16, -3, z, "spruce_planks");
                }
                set(16, -2, z, "spruce_slab[type=top,waterlogged=false]");
            }
            marker(18, -4, 14, "west", "czarny-rynek");
            marker(20, -4, 13, "west", "czarny-rynek");
            marker(18, -4, 18, "north", "czarny-rynek");
            set(19, -4, 15, "barrel[facing=up,open=false]");
            set(19, -3, 15, "barrel[facing=up,open=false]");
            set(17, -4, 18, "hay_block[axis=x]");
            // skrzynia za ladą (po stronie pasera)
            marker(14, -4, 7, "south", "czarny-rynek");
        }

        /** Dwa wyjścia drabiną: w chałupie i za murkiem na podwórku. */
        private void shafts() {
            for (int y = -4; y <= 0; y++) {
                set(6, y, 12, "ladder[facing=east,waterlogged=false]");
                set(20, y, 17, "ladder[facing=west,waterlogged=false]");
            }
            set(5, 0, 12, "cobblestone");
            set(21, 0, 17, "cobblestone");
        }

        // ------------------------------------------------------------------ powierzchnia

        private void shack() {
            fill(3, 0, 9, 11, 0, 16, rubble);
            set(6, 0, 12, "ladder[facing=east,waterlogged=false]");
            set(5, 0, 12, "cobblestone");
            for (int x = 3; x <= 11; x++) {
                for (int z = 9; z <= 16; z++) {
                    boolean edge = x == 3 || x == 11 || z == 9 || z == 16;
                    if (!edge) {
                        set(x, 1, z, "air");
                        continue;
                    }
                    int top = 3 - (x > 8 ? 1 : 0) - (chance(0.25) ? 1 : 0);
                    for (int y = 1; y <= top; y++) {
                        boolean corner = (x == 3 || x == 11) && (z == 9 || z == 16);
                        set(x, y, z, corner ? "spruce_log[axis=y]" : shackWall.next());
                    }
                }
            }
            // drzwi i okno-wyłom
            set(7, 1, 16, "air");
            set(7, 2, 16, "air");
            set(3, 2, 12, "air");
            set(3, 2, 13, "air");
            set(11, 1, 11, "air");
            set(11, 2, 11, "air");
            // resztki dachu na zachodniej połowie
            for (int x = 3; x <= 7; x++) {
                for (int z = 9; z <= 16; z++) {
                    if (chance(0.7)) {
                        set(x, 4, z, "spruce_slab[type=bottom,waterlogged=false]");
                    }
                }
            }
            set(9, 1, 14, "barrel[facing=up,open=false]");
            set(5, 1, 10, "crafting_table");
            set(9, 1, 10, "cobweb");
        }

        private void yardCover() {
            // murki z gruzu (osłona przed strzałami)
            wall(14, 3, 19, 3);
            wall(16, 7, 16, 11);
            wall(18, 15, 23, 15);
            wall(2, 20, 7, 20);
            wall(21, 18, 21, 22);
            // wóz bez koła
            fill(13, 1, 20, 16, 1, 22, "spruce_planks");
            fill(13, 2, 20, 13, 2, 22, "spruce_fence");
            set(16, 1, 23, "dark_oak_fence");
            set(12, 1, 20, "air");
            // stos drewna i siano
            fill(2, 1, 4, 5, 1, 5, "spruce_log[axis=x]");
            fill(2, 2, 4, 4, 2, 4, "spruce_log[axis=x]");
            fill(20, 1, 6, 21, 2, 7, "hay_block[axis=y]");
            // krzaki
            for (int[] b : new int[][]{{1, 1}, {24, 3}, {25, 12}, {1, 24}, {24, 24}, {12, 25}, {9, 2}}) {
                set(b[0], 1, b[1], "azalea_leaves[distance=1,persistent=true,waterlogged=false]");
                if (chance(0.5)) {
                    set(b[0], 2, b[1], "azalea_leaves[distance=1,persistent=true,waterlogged=false]");
                }
            }
            // uschnięte drzewo
            fill(23, 1, 9, 23, 5, 9, "spruce_log[axis=y]");
            set(22, 4, 9, "spruce_fence");
            set(24, 5, 9, "spruce_fence");
        }

        private void wall(int x1, int z1, int x2, int z2) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    int h = chance(0.2) ? 1 : 2;
                    for (int y = 1; y <= h; y++) {
                        set(x, y, z, rubble.next());
                    }
                }
            }
        }
    }
}
