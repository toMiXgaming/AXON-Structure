package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;

import java.util.Random;

/**
 * Kombinat Rolny „Przyszłość” (dawna spółdzielnia produkcyjna) – większa, przemysłowa wersja gospodarstwa.
 * <p>
 * Plan w lokalnych współrzędnych (front na północy, z = 0):
 * <pre>
 *  z 0-3    droga
 *  z 5      siatka na betonowych słupkach, brama x 42-51 z łukiem SKR „PRZYSZŁOŚĆ”
 *  z 6-51   alejka z płyt x 44-49: portiernia, waga samochodowa z budką
 *           po lewej obora x 8-20 / z 16-50 i chlewnia z paszarnią x 25-35 / z 26-47
 *           po prawej baza maszynowa x 56-82 / z 20-48 i magazyn nawozów x 56-68 / z 54-62
 *  z 52-61  suszarnia z kominem na końcu alejki, za nią wieża elewatora x 40-45 / z 66-71
 *  z 50-76  tyły: wieża ciśnień, zbiornik na gnojowicę, dwa krótkie silosy
 *  z 78     tylny płot
 * </pre>
 * Parkour: piętro paszarni (przyczepa → bela → poprzeczka silosu paszowego → daszek → okno),
 * pomost suwnicy (wywrotka → kabina ciągnika → regał → pomost), wieża elewatora
 * (drabina → półpiętro → drzwi serwisowe → zewnętrzny pomost → drabina → maszynownia).
 */
public final class CollectiveFarmStructure implements StructureDefinition {

    private static final int WIDTH = 90;
    private static final int HEIGHT = 32;
    private static final int DEPTH = 80;

    // alejka
    private static final int AX1 = 44;
    private static final int AX2 = 49;

    // obora
    private static final int OX1 = 8;
    private static final int OX2 = 20;
    private static final int OZ1 = 16;
    private static final int OZ2 = 50;

    // chlewnia + paszarnia
    private static final int CX1 = 25;
    private static final int CX2 = 35;
    private static final int CZ1 = 26;
    private static final int PZ1 = 41;
    private static final int PZ2 = 47;

    // baza maszynowa
    private static final int MX1 = 56;
    private static final int MX2 = 82;
    private static final int MZ1 = 20;
    private static final int MZ2 = 48;
    private static final int HALL_ROOF = 11;

    // wieża elewatora
    private static final int TX1 = 40;
    private static final int TX2 = 45;
    private static final int TZ1 = 66;
    private static final int TZ2 = 71;

    @Override
    public String id() {
        return "kombinat";
    }

    @Override
    public String displayName() {
        return "Kombinat rolny Przyszłość";
    }

    @Override
    public String description() {
        return "Dawna spółdzielnia: obora, paszarnia, baza maszynowa z suwnicą, magazyn nawozów i wieża elewatora";
    }

    @Override
    public Blueprint build(Random random) {
        return new Builder(random).build();
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);

        private final Palette grass;
        private final Palette silicate;
        private final Palette block;
        private final Palette eternit;
        private final Palette sheet;
        private final Palette rust;
        private final Palette hay;
        private final Palette sacks;

        Builder(Random rnd) {
            this.rnd = rnd;
            grass = new Palette(rnd)
                    .add("grass_block", 14).add("coarse_dirt", 3).add("dirt", 2).add("podzol", 1).add("mud", 1);
            silicate = new Palette(rnd)
                    .add("calcite", 6).add("white_terracotta", 3).add("polished_diorite", 2).add("diorite", 1);
            // szary pustak
            block = new Palette(rnd)
                    .add("light_gray_concrete", 6).add("stone_bricks", 3).add("cracked_stone_bricks", 2)
                    .add("andesite", 1).add("mossy_stone_bricks", 1);
            eternit = new Palette(rnd)
                    .add("andesite", 7).add("stone", 2).add("cobblestone", 1);
            sheet = new Palette(rnd)
                    .add("light_gray_concrete", 5).add("iron_block", 3).add("andesite", 1).add("orange_terracotta", 1);
            rust = new Palette(rnd)
                    .add("orange_terracotta", 5).add("brown_terracotta", 2).add("terracotta", 2).add("red_terracotta", 1);
            hay = new Palette(rnd)
                    .add("hay_block[axis=y]", 6).add("hay_block[axis=x]", 2).add("brown_wool", 2).add("podzol", 1);
            sacks = new Palette(rnd)
                    .add("white_wool", 5).add("light_gray_wool", 3).add("white_terracotta", 1);
        }

        Blueprint build() {
            ground();
            fence();
            gate();
            gatehouse();
            weighbridge();
            cowshed();
            piggery();
            feedLoft();
            machineHall();
            hallParkour();
            fertilizerStore();
            dryer();
            elevatorTower();
            shortSilo(31, 73);
            shortSilo(54, 73);
            slurryTank();
            waterTower();
            weeds();
            weathering();
            return bp;
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        // ------------------------------------------------------------------ teren, płot, brama

        private void ground() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    String b;
                    if (z <= 3) {
                        b = chance(0.8) ? "gray_concrete" : chance(0.6) ? "andesite" : "cobblestone";
                        if (chance(0.03)) {
                            b = "mud";
                        }
                    } else {
                        b = grass.next();
                    }
                    bp.set(x, 0, z, b);
                }
            }
            for (int x = AX1 - 2; x <= AX2 + 2; x++) {
                bp.set(x, 0, 4, chance(0.7) ? "gravel" : "coarse_dirt");
            }
            // alejka z popękanych płyt
            pave(AX1, 6, AX2, 51);
            // dojazdy do hali i magazynu
            pave(AX2 + 1, 30, MX1 - 1, 38);
            pave(AX2 + 1, 56, 55, 60);
            // betonowe opaski wokół budynków
            pave(MX1 - 1, MZ1 - 1, MX2 + 1, MZ1 - 1);
        }

        private void pave(int x1, int z1, int x2, int z2) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean joint = x % 3 == 0 || z % 3 == 0;
                    String b;
                    if (chance(0.08)) {
                        b = "mud";
                    } else if (joint && chance(0.35)) {
                        b = "grass_block";
                    } else {
                        b = chance(0.75) ? "smooth_stone" : "cracked_stone_bricks";
                    }
                    bp.set(x, 0, z, b);
                }
            }
        }

        /** Siatka na betonowych słupkach (2 bloki, nie da się na nią wskoczyć), miejscami przewrócona. */
        private void fence() {
            for (int x = 1; x <= WIDTH - 2; x++) {
                if (x < 42 || x > 51) {
                    fencePiece(x, 5, x);
                }
                fencePiece(x, DEPTH - 2, x);
            }
            for (int z = 6; z <= DEPTH - 3; z++) {
                fencePiece(1, z, z);
                fencePiece(WIDTH - 2, z, z);
            }
            for (int[] gap : new int[][]{{14, 5}, {70, 5}, {30, DEPTH - 2}, {62, DEPTH - 2}}) {
                for (int x = gap[0]; x < gap[0] + 3; x++) {
                    bp.fill(x, 1, gap[1], x, 2, gap[1], "air");
                }
            }
            for (int z = 40; z < 43; z++) {
                bp.fill(WIDTH - 2, 1, z, WIDTH - 2, 2, z, "air");
            }
        }

        private void fencePiece(int x, int z, int pos) {
            if (pos % 4 == 0) {
                bp.fill(x, 1, z, x, 2, z, "polished_andesite");
            } else {
                bp.set(x, 1, z, "iron_bars");
                bp.set(x, 2, z, chance(0.85) ? "iron_bars" : "air");
            }
        }

        private void gate() {
            // stalowy łuk z wyblakłym napisem
            bp.fill(42, 1, 5, 42, 8, 5, "iron_block");
            bp.fill(51, 1, 5, 51, 8, 5, "iron_block");
            for (int x = 43; x <= 50; x++) {
                bp.set(x, 8, 5, chance(0.85) ? "light_gray_concrete" : "orange_terracotta");
            }
            bp.sign(45, 8, 4, "oak_wall_sign[facing=north]", "SKR", "", "", "");
            bp.sign(47, 8, 4, "oak_wall_sign[facing=north]", "„PRZY ZŁ ŚĆ”", "", "", "");
            // jedno skrzydło leży na ziemi, drugie otwarte
            for (int x = 43; x <= 46; x++) {
                bp.set(x, 1, 7, "iron_trapdoor[facing=north,half=bottom,open=false]");
            }
            for (int z = 6; z <= 9; z++) {
                bp.set(52, 1, z, "iron_bars");
                bp.set(52, 2, z, "iron_bars");
            }
        }

        private void gatehouse() {
            int x1 = 36;
            int x2 = 40;
            int z1 = 7;
            int z2 = 11;
            for (int y = 1; y <= 3; y++) {
                for (int x = x1; x <= x2; x++) {
                    bp.set(x, y, z1, block.next());
                    bp.set(x, y, z2, block.next());
                }
                for (int z = z1; z <= z2; z++) {
                    bp.set(x1, y, z, block.next());
                    bp.set(x2, y, z, block.next());
                }
            }
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    bp.set(x, 4, z, chance(0.8) ? "black_concrete" : "gray_concrete");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        bp.set(x, 0, z, "smooth_stone");
                    }
                }
            }
            door(x2, 1, 9, "spruce_door", "east", "left", true);
            bp.set(38, 2, z1, brokenGlass());
            bp.set(x2, 2, 10, brokenGlass());
            bp.set(37, 1, 8, "spruce_slab[type=top]");
            bp.set(38, 1, 8, "spruce_slab[type=top]");
            bp.set(37, 2, 8, "lantern[hanging=false]");
            bp.set(39, 1, 9, "spruce_stairs[facing=west,half=bottom]");
            bp.set(x1 + 1, 2, 9, "tripwire_hook[facing=east,attached=false,powered=false]");
            bp.crate(37, 1, 10, "east", "parter");
            // szlaban w pozycji pionowej
            bp.set(41, 1, 11, "polished_andesite");
            for (int y = 2; y <= 5; y++) {
                bp.set(41, y, 11, y % 2 == 0 ? "red_concrete" : "white_concrete");
            }
        }

        private void weighbridge() {
            for (int x = AX1; x <= AX2; x++) {
                for (int z = 12; z <= 18; z++) {
                    boolean rim = x == AX1 || x == AX2 || z == 12 || z == 18;
                    bp.set(x, 0, z, rim ? "polished_andesite" : chance(0.85) ? "iron_block" : "orange_terracotta");
                }
            }
            // budka wagowego
            for (int x = 52; x <= 54; x++) {
                for (int z = 13; z <= 15; z++) {
                    bp.fill(x, 1, z, x, 3, z, block);
                    bp.set(x, 4, z, "gray_concrete");
                }
            }
            bp.set(53, 1, 14, "air");
            bp.set(53, 2, 14, "air");
            bp.set(52, 2, 14, brokenGlass());
            door(53, 1, 15, "spruce_door", "south", "left", true);
            bp.crate(43, 1, 12, "east", "plac");
            bp.crate(50, 1, 17, "west", "plac");
            bp.crate(43, 1, 18, "east", "plac");
            bp.crate(50, 1, 11, "west", "plac");
            bp.set(51, 1, 18, "blue_terracotta");
            tires(42, 15);
        }

        // ------------------------------------------------------------------ obora

        private static int cowRoofY(int x) {
            return 5 + Math.min(x - OX1, OX2 - x);
        }

        private void cowshed() {
            for (int y = 1; y <= 4; y++) {
                Palette wall = y <= 1 ? block : silicate;
                for (int x = OX1; x <= OX2; x++) {
                    bp.set(x, y, OZ1, wall.next());
                    bp.set(x, y, OZ2, wall.next());
                }
                for (int z = OZ1; z <= OZ2; z++) {
                    bp.set(OX1, y, z, wall.next());
                    bp.set(OX2, y, z, wall.next());
                }
            }
            for (int x = OX1 + 1; x < OX2; x++) {
                for (int y = 5; y < cowRoofY(x); y++) {
                    bp.set(x, y, OZ1, silicate.next());
                    bp.set(x, y, OZ2, silicate.next());
                }
            }
            int ridge = (OX1 + OX2) / 2;
            for (int x = OX1 - 1; x <= OX2 + 1; x++) {
                int y = x < OX1 ? 4 : x > OX2 ? 4 : cowRoofY(x);
                for (int z = OZ1 - 1; z <= OZ2 + 1; z++) {
                    String b;
                    if (x == ridge) {
                        b = z % 5 == 0 ? "glass" : eternit.next(); // świetlik na kalenicy
                    } else {
                        String stair = chance(0.75) ? "andesite_stairs" : chance(0.7) ? "stone_stairs" : "cobblestone_stairs";
                        b = stair + "[facing=" + (x < ridge ? "east" : "west") + ",half=bottom]";
                    }
                    bp.set(x, y, z, b);
                }
            }
            // zapadnięty fragment dachu
            for (int x = 15; x <= 18; x++) {
                for (int z = 30; z <= 33; z++) {
                    bp.set(x, cowRoofY(x), z, z % 2 == 0 ? "stripped_spruce_wood[axis=y]" : "air");
                }
            }
            // okienka pod dachem
            for (int z = OZ1 + 2; z < OZ2; z += 4) {
                bp.set(OX1, 3, z, "iron_bars");
                bp.set(OX2, 3, z, "iron_bars");
            }
            // posadzka i korytarz paszowy
            for (int x = OX1 + 1; x < OX2; x++) {
                for (int z = OZ1 + 1; z < OZ2; z++) {
                    boolean corridor = x >= 13 && x <= 15;
                    bp.set(x, 0, z, corridor ? (chance(0.8) ? "smooth_stone" : "cracked_stone_bricks")
                            : (chance(0.6) ? "packed_mud" : chance(0.5) ? "mud" : "coarse_dirt"));
                }
            }
            // wejście od frontu, wybita dziura w tylnym szczycie
            bp.fill(13, 1, OZ1, 15, 3, OZ1, "air");
            bp.fill(13, 1, OZ2, 15, 2, OZ2, "air");
            bp.set(15, 3, OZ2, "air");
            bp.set(12, 1, OZ2 + 1, "cobblestone");
            bp.set(16, 1, OZ2 + 1, "cracked_stone_bricks");
            // boksy z krat po obu stronach korytarza
            for (int z = OZ1 + 1; z < OZ2; z += 3) {
                boolean end = z == OZ1 + 1 || z + 3 >= OZ2;
                for (int side = 0; side < 2; side++) {
                    int front = side == 0 ? 12 : 16;
                    int wallX = side == 0 ? OX1 + 1 : OX2 - 1;
                    for (int y = 1; y <= 2; y++) {
                        for (int dz = 0; dz < 3; dz++) {
                            if (z + dz < OZ2) {
                                bp.set(front, y, z + dz, "iron_bars");
                            }
                        }
                        if (z + 3 < OZ2) {
                            int from = Math.min(front, wallX);
                            int to = Math.max(front, wallX);
                            for (int x = from; x <= to; x++) {
                                bp.set(x, y, z + 3, "iron_bars");
                            }
                        }
                    }
                    // furtka boksu: otwarta w końcowych boksach, w pozostałych czasem
                    if (end || chance(0.6)) {
                        bp.fill(front, 1, z + 1, front, 2, z + 1, "air");
                    }
                    bp.set(wallX, 1, z + 1, chance(0.7) ? "cauldron" : "spruce_slab[type=bottom]");
                }
            }
            bp.crate(OX1 + 1, 1, OZ1 + 2, "east", "klepisko");
            bp.crate(OX2 - 1, 1, OZ1 + 3, "west", "klepisko");
            bp.crate(OX1 + 1, 1, OZ2 - 2, "east", "klepisko");
            bp.crate(OX2 - 1, 1, OZ2 - 2, "west", "klepisko");
            for (int z = OZ1 + 4; z < OZ2; z += 8) {
                bp.set(14, cowRoofY(14) - 1, z, "lantern[hanging=true]");
            }
            for (int z = OZ1 + 3; z < OZ2; z += 6) {
                if (bp.isAir(14, 1, z)) {
                    bp.set(14, 1, z, chance(0.5) ? "hay_block[axis=z]" : "brown_carpet");
                }
            }
        }

        // ------------------------------------------------------------------ chlewnia i paszarnia

        private static int pigRoofY(int x) {
            return 5 + Math.min(x - CX1, CX2 - x);
        }

        private void piggery() {
            for (int y = 1; y <= 4; y++) {
                for (int x = CX1; x <= CX2; x++) {
                    bp.set(x, y, CZ1, block.next());
                }
                for (int z = CZ1; z < PZ1; z++) {
                    bp.set(CX1, y, z, block.next());
                    bp.set(CX2, y, z, block.next());
                }
            }
            for (int x = CX1 + 1; x < CX2; x++) {
                for (int y = 5; y < pigRoofY(x); y++) {
                    bp.set(x, y, CZ1, block.next());
                }
            }
            int ridge = (CX1 + CX2) / 2;
            for (int x = CX1 - 1; x <= CX2 + 1; x++) {
                int y = x < CX1 || x > CX2 ? 4 : pigRoofY(x);
                for (int z = CZ1 - 1; z < PZ1; z++) {
                    String b = x == ridge ? eternit.next()
                            : (chance(0.8) ? "andesite_stairs" : "stone_stairs") + "[facing=" + (x < ridge ? "east" : "west") + ",half=bottom]";
                    bp.set(x, y, z, b);
                }
            }
            for (int x = CX1 + 1; x < CX2; x++) {
                for (int z = CZ1 + 1; z < PZ1; z++) {
                    bp.set(x, 0, z, x == 30 ? "mud" : chance(0.7) ? "smooth_stone" : "packed_mud");
                }
            }
            door(30, 1, CZ1, "spruce_door", "north", "left", true);
            for (int z = CZ1 + 2; z < PZ1; z += 4) {
                bp.set(CX1, 3, z, brokenGlass());
                bp.set(CX2, 3, z, brokenGlass());
            }
            // kojce z niskich murków po obu stronach kanału na gnojowicę
            for (int z = CZ1 + 4; z < PZ1; z += 4) {
                for (int x = CX1 + 1; x <= 28; x++) {
                    bp.set(x, 1, z, "stone_bricks");
                }
                for (int x = 32; x < CX2; x++) {
                    bp.set(x, 1, z, "stone_bricks");
                }
            }
            bp.fill(29, 1, CZ1 + 1, 29, 1, PZ1 - 1, "stone_bricks");
            bp.fill(31, 1, CZ1 + 1, 31, 1, PZ1 - 1, "stone_bricks");
            bp.set(29, 1, 32, "air");
            bp.set(31, 1, 36, "air");
            bp.set(29, 1, 38, "air");
            bp.set(31, 1, 28, "air");
            bp.crate(26, 1, 28, "east", "parter");
            bp.crate(34, 1, 38, "west", "parter");
            for (int[] p : new int[][]{{27, 33}, {33, 31}, {26, 37}}) {
                bp.set(p[0], 1, p[1], "brown_wool");
            }
            bp.set(30, pigRoofY(30) - 1, 33, "lantern[hanging=true]");
        }

        /**
         * Paszarnia: piętro tylko z zewnątrz. Każdy krok to +1: przyczepa (y1) → bela (y2)
         * → poprzeczka silosu paszowego (y3) → daszek nad wejściem (y4) → okno na piętrze (podłoga y5).
         */
        private void feedLoft() {
            int top = 9;
            for (int y = 1; y <= top; y++) {
                for (int x = CX1; x <= CX2; x++) {
                    bp.set(x, y, PZ1, block.next());
                    bp.set(x, y, PZ2, block.next());
                }
                for (int z = PZ1; z <= PZ2; z++) {
                    bp.set(CX1, y, z, block.next());
                    bp.set(CX2, y, z, block.next());
                }
            }
            for (int x = CX1; x <= CX2; x++) {
                for (int z = PZ1; z <= PZ2; z++) {
                    bp.set(x, top + 1, z, chance(0.8) ? "black_concrete" : "gray_concrete");
                    if (x > CX1 && x < CX2 && z > PZ1 && z < PZ2) {
                        bp.set(x, 0, z, "smooth_stone");
                        bp.set(x, 5, z, chance(0.9) ? "spruce_planks" : "dark_oak_planks");
                    }
                }
            }
            // przejście z chlewni i wejście od alejki
            door(30, 1, PZ1, "spruce_door", "north", "left", true);
            door(CX2, 1, 44, "spruce_door", "east", "left", true);
            // zżarte przez rdzę schody: zostały tylko łańcuchy
            for (int i = 0; i < 4; i++) {
                bp.set(27 + i, 1 + i, 43, "chain[axis=y]");
            }
            bp.set(28, 1, 46, "hopper[facing=down,enabled=true]");
            bp.fill(32, 1, 46, 34, 1, 46, sacks);
            bp.set(33, 2, 46, "white_wool");
            // piętro: worki paszy i lepsze skrzynie
            bp.fill(26, 6, 46, 29, 6, 46, sacks);
            bp.set(27, 7, 46, "light_gray_wool");
            bp.set(31, 6, 42, "hopper[facing=down,enabled=true]");
            bp.crate(26, 6, 42, "east", "strych");
            bp.crate(34, 6, 46, "west", "strych");
            bp.set(30, 9, 44, "lantern[hanging=true]");
            for (int[] w : new int[][]{{CX1, 43}, {CX1, 45}}) {
                bp.set(w[0], 7, w[1], brokenGlass());
                bp.set(w[0], 8, w[1], brokenGlass());
            }
            bp.set(28, 7, PZ2, brokenGlass());
            bp.set(32, 7, PZ2, brokenGlass());
            // wybite okno nad daszkiem – wejście na piętro
            bp.fill(CX2, 6, 44, CX2, 7, 44, "air");
            // daszek nad wejściem
            bp.fill(36, 4, 43, 37, 4, 45, "dark_oak_planks");
            // silos paszowy na nóżkach: lej y8-13, nóżki do y7, poprzeczka y3
            for (int[] leg : new int[][]{{38, 42}, {41, 42}, {38, 46}, {41, 46}}) {
                bp.fill(leg[0], 1, leg[1], leg[0], 7, leg[1], "polished_andesite_wall");
            }
            bp.fill(38, 3, 43, 38, 3, 45, "iron_block");
            bp.fill(39, 7, 43, 40, 7, 45, "orange_terracotta");
            bp.fill(38, 8, 42, 41, 13, 46, "light_gray_concrete");
            bp.fill(39, 8, 43, 40, 13, 45, "iron_block");
            bp.set(39, 14, 44, "lightning_rod[facing=up]");
            // przyczepa pod silosem z jedną belą
            bp.fill(39, 1, 43, 40, 1, 45, "dark_oak_planks");
            bp.set(39, 2, 44, "hay_block[axis=y]");
            bp.set(40, 2, 45, "brown_carpet");
        }

        // ------------------------------------------------------------------ baza maszynowa

        private void machineHall() {
            for (int y = 1; y < HALL_ROOF; y++) {
                for (int x = MX1; x <= MX2; x++) {
                    bp.set(x, y, MZ1, hallSheet(x, y));
                    bp.set(x, y, MZ2, hallSheet(x, y));
                }
                for (int z = MZ1; z <= MZ2; z++) {
                    bp.set(MX1, y, z, hallSheet(z, y));
                    bp.set(MX2, y, z, hallSheet(z, y));
                }
            }
            for (int x = MX1; x <= MX2; x++) {
                for (int z = MZ1; z <= MZ2; z++) {
                    bp.set(x, HALL_ROOF, z, (x + z) % 7 == 0 ? "glass" : (x % 2 == 0 ? "light_gray_concrete" : "iron_block"));
                    if (x > MX1 && x < MX2 && z > MZ1 && z < MZ2) {
                        bp.set(x, 0, z, chance(0.85) ? "polished_andesite" : chance(0.5) ? "black_concrete" : "cracked_stone_bricks");
                    }
                }
            }
            // dziury w dachu
            for (int x = 62; x <= 65; x++) {
                for (int z = 40; z <= 42; z++) {
                    bp.set(x, HALL_ROOF, z, "air");
                }
            }
            // wysokie okna
            for (int x = MX1 + 3; x < MX2; x += 4) {
                bp.set(x, 8, MZ1, "iron_bars");
                bp.set(x, 9, MZ1, "iron_bars");
            }
            // wielkie wrota od alejki, jedno skrzydło zasunięte w połowie
            bp.fill(MX1, 1, 28, MX1, 7, 40, "air");
            for (int z = 26; z <= 33; z++) {
                for (int y = 1; y <= 7; y++) {
                    bp.set(MX1 - 1, y, z, y % 3 == 0 ? "iron_block" : "light_gray_concrete");
                }
            }
            for (int z = 25; z <= 41; z++) {
                bp.set(MX1 - 1, 8, z, "chain[axis=z]");
            }
            // tylna ściana bez blachy: sam szkielet z dwuteowników
            for (int x = MX1 + 1; x <= 68; x++) {
                for (int y = 1; y < HALL_ROOF; y++) {
                    boolean column = (x - MX1) % 4 == 0;
                    bp.set(x, y, MZ2, column ? "iron_block" : (y == 6 || y == 10) ? "iron_bars" : "air");
                }
            }
            // warsztat przy północnej ścianie
            for (int x = MX1 + 1; x <= 70; x++) {
                bp.set(x, 1, MZ1 + 1, "spruce_slab[type=top]");
            }
            for (int x = MX1 + 2; x <= 70; x += 4) {
                bp.set(x, 2, MZ1 + 1, chance(0.5) ? "grindstone[face=floor,facing=south]" : "chipped_anvil[facing=east]");
            }
            bp.set(64, 2, MZ1 + 1, "lantern[hanging=false]");
            bp.crate(MX1 + 1, 1, MZ1 + 2, "south", "klepisko");
            bp.crate(70, 1, MZ1 + 2, "south", "klepisko");
            // kanał serwisowy: wykop do gruntu (y0), krawężnik y1 z barierką y2 – wejście i wyjście tylko od strony drabinki
            for (int z = 28; z <= 38; z++) {
                bp.set(66, 0, z, "air");
                bp.set(67, 0, z, "air");
                for (int x : new int[]{65, 68}) {
                    bp.set(x, 1, z, "yellow_concrete");
                    bp.set(x, 2, z, "iron_bars");
                }
            }
            for (int x = 65; x <= 68; x++) {
                bp.set(x, 1, 27, "yellow_concrete");
                bp.set(x, 2, 27, "iron_bars");
            }
            bp.set(66, 1, 39, "yellow_concrete");
            bp.set(67, 1, 39, "yellow_concrete");
            bp.set(66, 0, 38, "ladder[facing=north]");
            bp.set(66, 1, 38, "ladder[facing=north]");
            bp.crate(67, 0, 29, "west", "klepisko");
            bp.crate(66, 0, 34, "east", "klepisko");
            // ciągnik bez kabiny
            tractor(59, 26, false);
            // prasa do słomy
            bp.fill(60, 1, 41, 63, 2, 44, "red_terracotta");
            bp.fill(61, 3, 42, 62, 3, 43, "red_concrete");
            bp.set(64, 1, 42, "hay_block[axis=x]");
            bp.set(59, 1, 42, "black_concrete");
            bp.set(59, 1, 43, "black_concrete");
            // paliwo przy maszynach
            bp.crate(MX1 + 1, 1, 45, "east", "paliwo");
            bp.crate(MX1 + 2, 1, 46, "north", "paliwo");
            bp.crate(71, 1, 46, "north", "paliwo");
            bp.set(72, 1, 46, "blue_terracotta");
            for (int x = 60; x <= 76; x += 8) {
                bp.set(x, HALL_ROOF - 1, 30, "lantern[hanging=true]");
            }
        }

        private String hallSheet(int along, int y) {
            if (y == 5 || y == 10) {
                return "iron_block";
            }
            if (chance(0.06)) {
                return rust.next();
            }
            return along % 2 == 0 ? "light_gray_concrete" : "andesite";
        }

        /**
         * Pomost suwnicy: wywrotka z podniesioną skrzynią (na górze y4) → przez kratkę kabina ciągnika (y5)
         * → przez kratkę górna półka regału (y6) → pomost (y7).
         */
        private void hallParkour() {
            // wywrotka: podwozie i skrzynia uniesiona przodem
            bp.fill(73, 1, 22, 74, 1, 29, "gray_concrete");
            bp.set(72, 1, 23, "black_concrete");
            bp.set(75, 1, 23, "black_concrete");
            bp.set(72, 1, 28, "black_concrete");
            bp.set(75, 1, 28, "black_concrete");
            int[][] box = {{29, 1}, {28, 2}, {27, 3}, {26, 4}, {25, 4}};
            for (int[] b : box) {
                int z = b[0];
                for (int x = 73; x <= 74; x++) {
                    bp.fill(x, 1, z, x, b[1], z, rust);
                }
            }
            bp.fill(73, 2, 23, 73, 3, 24, "chain[axis=y]");
            // ciągnik z kabiną obok
            tractor(76, 22, true);
            // regał przy wschodniej ścianie: słupy, półki y3 i y6
            bp.fill(80, 1, 22, 80, 6, 22, "stripped_spruce_log[axis=y]");
            bp.fill(81, 1, 22, 81, 6, 22, "stripped_spruce_log[axis=y]");
            bp.fill(80, 1, 26, 80, 6, 26, "stripped_spruce_log[axis=y]");
            bp.fill(81, 1, 26, 81, 6, 26, "stripped_spruce_log[axis=y]");
            for (int z = 23; z <= 25; z++) {
                bp.set(80, 3, z, "spruce_planks");
                bp.set(81, 3, z, "spruce_planks");
                bp.set(80, 6, z, "spruce_planks");
                bp.set(81, 6, z, "spruce_planks");
            }
            bp.set(81, 4, 24, "barrel[facing=up,open=false]");
            bp.set(80, 1, 24, "barrel[facing=up,open=false]");
            // pomost wzdłuż ściany i belka suwnicy pod dachem
            for (int z = 27; z <= 44; z++) {
                bp.set(80, 7, z, "iron_block");
                bp.set(81, 7, z, "iron_block");
                bp.set(79, 8, z, "iron_bars");
            }
            for (int x = MX1 + 1; x < MX2; x++) {
                bp.set(x, 10, 40, "yellow_terracotta");
            }
            bp.fill(69, 6, 40, 69, 9, 40, "chain[axis=y]");
            bp.set(69, 5, 40, "iron_trapdoor[facing=north,half=bottom,open=false]");
            bp.crate(81, 8, 33, "west", "suwnica");
            bp.crate(81, 8, 42, "west", "suwnica");
        }

        /** Ciągnik wzdłuż osi z, przodem na północ (z = z0). Kabina ma dach na y5. */
        private void tractor(int x0, int z0, boolean cab) {
            bp.set(x0, 1, z0 + 1, "black_concrete");
            bp.set(x0 + 2, 1, z0 + 1, "black_concrete");
            bp.fill(x0, 1, z0 + 3, x0, 2, z0 + 4, "black_concrete");
            bp.fill(x0 + 2, 1, z0 + 3, x0 + 2, 2, z0 + 4, "black_concrete");
            bp.fill(x0 + 1, 1, z0, x0 + 1, 2, z0 + 2, "red_concrete");
            bp.fill(x0 + 1, 1, z0 + 3, x0 + 1, 1, z0 + 4, "gray_concrete");
            bp.set(x0 + 1, 2, z0 + 4, "spruce_stairs[facing=south,half=bottom]");
            bp.set(x0 + 1, 3, z0 + 1, "lightning_rod[facing=up]");
            if (!cab) {
                return;
            }
            for (int[] p : new int[][]{{x0, z0 + 1}, {x0 + 2, z0 + 1}, {x0, z0 + 3}, {x0 + 2, z0 + 3}}) {
                bp.fill(p[0], 3, p[1], p[0], 4, p[1], "iron_bars");
            }
            bp.fill(x0, 5, z0 + 1, x0 + 2, 5, z0 + 3, "red_concrete");
        }

        // ------------------------------------------------------------------ magazyn nawozów

        private void fertilizerStore() {
            int x1 = MX1;
            int x2 = 68;
            int z1 = 54;
            int z2 = 62;
            for (int y = 1; y <= 5; y++) {
                for (int x = x1; x <= x2; x++) {
                    bp.set(x, y, z1, chance(0.9) ? "gray_concrete" : "cracked_stone_bricks");
                    bp.set(x, y, z2, chance(0.9) ? "gray_concrete" : "cracked_stone_bricks");
                }
                for (int z = z1; z <= z2; z++) {
                    bp.set(x1, y, z, chance(0.9) ? "gray_concrete" : "cracked_stone_bricks");
                    bp.set(x2, y, z, chance(0.9) ? "gray_concrete" : "cracked_stone_bricks");
                }
            }
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    bp.set(x, 6, z, "gray_concrete");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        bp.set(x, 0, z, "smooth_stone");
                    }
                }
            }
            // rampa i stalowe drzwi z tablicą
            for (int z : new int[]{56, 57, 59, 60}) {
                bp.set(x1 - 1, 1, z, "smooth_stone_slab[type=bottom]");
                bp.set(x1 - 2, 1, z, "smooth_stone_slab[type=bottom]");
            }
            door(x1, 1, 58, "iron_door", "west", "left", true);
            bp.sign(x1 - 1, 3, 58, "oak_wall_sign[facing=west]", "UWAGA!", "ŚRODKI", "TRUJĄCE", "");
            // worki na paletach
            for (int x = 58; x <= 66; x += 4) {
                for (int z = 55; z <= 56; z++) {
                    bp.set(x, 1, z, "spruce_slab[type=bottom]");
                    bp.set(x + 1, 1, z, "spruce_slab[type=bottom]");
                }
                bp.fill(x, 2, 55, x + 1, 2 + rnd.nextInt(2), 55, sacks);
            }
            // pęknięte beczki z chemikaliami i rozlana kałuża
            bp.set(66, 1, 61, "lime_terracotta");
            bp.set(67, 1, 61, "yellow_terracotta");
            bp.set(67, 2, 61, "lime_terracotta");
            bp.set(65, 1, 61, "green_terracotta");
            for (int x = 61; x <= 65; x++) {
                for (int z = 58; z <= 61; z++) {
                    if (chance(0.6)) {
                        bp.set(x, 0, z, chance(0.5) ? "lime_concrete" : "green_concrete");
                    }
                }
            }
            bp.crate(57, 1, 61, "east", "magazyn-nawozow");
            bp.crate(67, 1, 57, "west", "magazyn-nawozow");
            bp.crate(60, 1, 57, "north", "magazyn-nawozow");
            bp.set(62, 5, 58, "lantern[hanging=true]");
        }

        // ------------------------------------------------------------------ suszarnia i elewator

        private void dryer() {
            int x1 = 36;
            int x2 = 53;
            int z1 = 52;
            int z2 = 61;
            for (int y = 1; y <= 6; y++) {
                Palette wall = y <= 2 ? block : null;
                for (int x = x1; x <= x2; x++) {
                    bp.set(x, y, z1, wall != null ? wall.next() : brick());
                    bp.set(x, y, z2, wall != null ? wall.next() : brick());
                }
                for (int z = z1; z <= z2; z++) {
                    bp.set(x1, y, z, wall != null ? wall.next() : brick());
                    bp.set(x2, y, z, wall != null ? wall.next() : brick());
                }
            }
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    bp.set(x, 7, z, chance(0.85) ? "gray_concrete" : "black_concrete");
                    if (x > x1 && x < x2 && z > z1 && z < z2) {
                        bp.set(x, 0, z, chance(0.8) ? "smooth_stone" : "packed_mud");
                    }
                }
            }
            bp.fill(45, 1, z1, 47, 4, z1, "air");
            // komin z czerwonej cegły
            bp.fill(51, 1, 58, 52, 22, 59, "bricks");
            // zasypy, dmuchawy i sterty ziarna
            for (int x = 39; x <= 49; x += 5) {
                bp.set(x, 1, 58, "polished_andesite");
                bp.set(x, 2, 58, "hopper[facing=down,enabled=true]");
                bp.set(x, 3, 58, "hopper[facing=down,enabled=true]");
            }
            bp.set(38, 1, 54, "iron_block");
            bp.set(38, 2, 54, "grindstone[face=floor,facing=north]");
            bp.set(50, 1, 54, "iron_block");
            for (int x = 41; x <= 46; x++) {
                for (int z = 59; z <= 60; z++) {
                    if (chance(0.6)) {
                        bp.set(x, 1, z, chance(0.5) ? "hay_block[axis=y]" : "brown_carpet");
                    }
                }
            }
            bp.crate(37, 1, 60, "east", "parter");
            for (int x = 40; x <= 50; x += 5) {
                bp.set(x, 6, 56, "lantern[hanging=true]");
            }
        }

        private String brick() {
            return chance(0.8) ? "bricks" : chance(0.5) ? "light_gray_concrete" : "cracked_stone_bricks";
        }

        /**
         * Wieża elewatora (blacha, ok. 30 bloków). W środku drabiny z półpiętrami y8, y16, y24 i maszynownia
         * z dwiema rzadkimi skrzyniami. Drugi bieg jest zerwany – z półpiętra y8 wychodzi się drzwiami
         * serwisowymi na zewnętrzny pomost, obchodzi wieżę, wchodzi po drabinie na zachodniej ścianie
         * i wraca do środka drzwiami na y17.
         */
        private void elevatorTower() {
            int roof = 29;
            for (int y = 1; y < roof; y++) {
                for (int x = TX1; x <= TX2; x++) {
                    bp.set(x, y, TZ1, towerSheet(x, y));
                    bp.set(x, y, TZ2, towerSheet(x, y));
                }
                for (int z = TZ1; z <= TZ2; z++) {
                    bp.set(TX1, y, z, towerSheet(z, y));
                    bp.set(TX2, y, z, towerSheet(z, y));
                }
            }
            for (int x = TX1; x <= TX2; x++) {
                for (int z = TZ1; z <= TZ2; z++) {
                    bp.set(x, roof, z, "iron_block");
                    if (x > TX1 && x < TX2 && z > TZ1 && z < TZ2) {
                        bp.set(x, 0, z, "smooth_stone");
                        for (int floor : new int[]{8, 16, 24}) {
                            bp.set(x, floor, z, chance(0.85) ? "iron_block" : "polished_andesite");
                        }
                    }
                }
            }
            bp.set(42, roof + 1, 68, "lightning_rod[facing=up]");
            bp.fill(42, 1, TZ1, 42, 2, TZ1, "air");
            // bieg 1: parter → półpiętro y8
            for (int y = 1; y <= 8; y++) {
                bp.set(TX1 + 1, y, 68, "ladder[facing=east]");
            }
            // bieg 2: zerwany w połowie, półpiętro y16 nieosiągalne od środka
            for (int y = 9; y <= 11; y++) {
                bp.set(TX2 - 1, y, 70, "ladder[facing=west]");
            }
            bp.set(TX2 - 1, 9, 69, "cobblestone_slab[type=bottom]");
            // drzwi serwisowe na zewnętrzny pomost
            bp.fill(TX2, 9, 68, TX2, 10, 68, "air");
            for (int x = TX1 - 1; x <= TX2 + 1; x++) {
                for (int z = TZ1 - 1; z <= TZ2 + 1; z++) {
                    boolean ring = x == TX1 - 1 || x == TX2 + 1 || z == TZ1 - 1 || z == TZ2 + 1;
                    if (ring) {
                        bp.set(x, 8, z, chance(0.9) ? "iron_block" : "waxed_exposed_cut_copper");
                    }
                }
            }
            // drabina bez klatki na zachodniej ścianie i mały podest y16 z drzwiami na y17
            for (int y = 9; y <= 16; y++) {
                bp.set(TX1 - 1, y, 69, "ladder[facing=west]");
            }
            bp.set(TX1 - 1, 16, 68, "iron_block");
            bp.set(TX1 - 1, 16, 67, "iron_block");
            bp.fill(TX1, 17, 68, TX1, 18, 68, "air");
            // bieg 3: y16 → y24
            for (int y = 17; y <= 24; y++) {
                bp.set(TX2 - 1, y, 67, "ladder[facing=west]");
            }
            // maszynownia y25-28 z oknami na cztery strony
            for (int i = 1; i <= 4; i++) {
                for (int y = 26; y <= 27; y++) {
                    bp.set(TX1 + i, y, TZ1, brokenGlass());
                    bp.set(TX1 + i, y, TZ2, brokenGlass());
                    bp.set(TX1, y, TZ1 + i, brokenGlass());
                    bp.set(TX2, y, TZ1 + i, brokenGlass());
                }
            }
            bp.set(43, 25, 69, "iron_block");
            bp.set(43, 26, 69, "grindstone[face=floor,facing=north]");
            bp.set(42, 28, 69, "lantern[hanging=true]");
            bp.crate(TX1 + 1, 25, 70, "east", "elewator");
            bp.crate(TX1 + 1, 25, 67, "east", "elewator");
            // rury transportowe do silosów
            for (int x = 35; x < TX1; x++) {
                bp.set(x, 20, 69, "chain[axis=x]");
            }
            for (int x = TX2 + 1; x <= 51; x++) {
                bp.set(x, 20, 69, "chain[axis=x]");
            }
        }

        private String towerSheet(int along, int y) {
            if (y % 8 == 0) {
                return "polished_andesite";
            }
            if (chance(0.07)) {
                return rust.next();
            }
            return along % 2 == 0 ? "light_gray_concrete" : "iron_block";
        }

        private void shortSilo(int cx, int cz) {
            int top = 14;
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = cz - 4; z <= cz + 4; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (d <= 4.5 && bp.inBounds(x, 0, z)) {
                        bp.set(x, 0, z, "smooth_stone");
                    }
                    if (d > 2.5 && d <= 3.5) {
                        for (int y = 1; y <= top; y++) {
                            bp.set(x, y, z, y % 6 == 0 ? "polished_andesite"
                                    : chance(0.08) ? rust.next() : (x + z) % 2 == 0 ? "light_gray_concrete" : "iron_block");
                        }
                    }
                    double[] radius = {3.5, 2.5, 1.5, 0.5};
                    for (int i = 0; i < radius.length; i++) {
                        if (d <= radius[i]) {
                            bp.set(x, top + 1 + i, z, chance(0.8) ? "iron_block" : "light_gray_concrete");
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ tyły

        /** Otwarty zbiornik na gnojowicę: ściany y1-3, w środku muł i woda, wyjście tylko po drabince. */
        private void slurryTank() {
            int cx = 12;
            int cz = 64;
            for (int x = cx - 7; x <= cx + 7; x++) {
                for (int z = cz - 7; z <= cz + 7; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (d > 5.5 && d <= 6.5) {
                        bp.fill(x, 1, z, x, 3, z, chance(0.85) ? "gray_concrete" : "cracked_stone_bricks");
                    } else if (d <= 5.5) {
                        bp.set(x, 0, z, chance(0.55) ? "mud" : "water");
                    }
                }
            }
            for (int y = 1; y <= 3; y++) {
                bp.set(cx - 5, y, cz, "ladder[facing=east]");
            }
            // schodki na krawędź przy pompie
            bp.set(cx + 8, 1, cz, "smooth_stone");
            bp.fill(cx + 7, 1, cz, cx + 7, 2, cz, "smooth_stone");
            bp.fill(cx + 5, 1, cz, cx + 5, 3, cz, "chain[axis=y]");
            bp.crate(cx, 1, cz - 8, "north", "plac");
            bp.crate(cx + 7, 1, cz + 4, "west", "plac");
            bp.crate(cx - 7, 1, cz - 5, "east", "plac");
            bp.crate(cx + 3, 1, cz + 8, "north", "plac");
        }

        /** Wieża ciśnień: kratownica na nóżkach, drabina bez klatki, pomost y16 z lepszą skrzynią, zbiornik y19-24. */
        private void waterTower() {
            int cx = 26;
            int cz = 54;
            for (int[] leg : new int[][]{{24, 52}, {28, 52}, {24, 56}, {28, 56}}) {
                bp.fill(leg[0], 1, leg[1], leg[0], 15, leg[1], "iron_block");
            }
            for (int y = 5; y <= 15; y += 5) {
                for (int i = 25; i <= 27; i++) {
                    bp.set(i, y, 52, "iron_bars");
                    bp.set(i, y, 56, "iron_bars");
                    bp.set(24, y, i + 28, "iron_bars");
                    bp.set(28, y, i + 28, "iron_bars");
                }
            }
            for (int x = 23; x <= 29; x++) {
                for (int z = 51; z <= 57; z++) {
                    bp.set(x, 16, z, chance(0.85) ? "iron_block" : "polished_andesite");
                }
            }
            // drabina od strony wschodniej – z daleka od dachów obory i chlewni
            for (int y = 1; y <= 16; y++) {
                bp.set(29, y, 52, "ladder[facing=east]");
            }
            for (int x = cx - 3; x <= cx + 3; x++) {
                for (int z = cz - 3; z <= cz + 3; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (d <= 3.2) {
                        for (int y = 19; y <= 24; y++) {
                            boolean shell = d > 2.2 || y == 19;
                            if (shell) {
                                bp.set(x, y, z, chance(0.85) ? "light_gray_concrete" : rust.next());
                            }
                        }
                    }
                    if (d <= 2.5) {
                        bp.set(x, 25, z, "iron_block");
                    }
                    if (d <= 1.2) {
                        bp.set(x, 26, z, "iron_block");
                    }
                }
            }
            bp.fill(26, 17, 54, 26, 18, 54, "iron_block");
            bp.crate(28, 17, 57, "west", "wieza");
        }

        // ------------------------------------------------------------------ zarośla i zniszczenia

        private void weeds() {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 6; z < DEPTH; z++) {
                    String below = bp.get(x, 0, z);
                    if (!below.equals("minecraft:grass_block") && !below.equals("minecraft:podzol")
                            && !below.equals("minecraft:coarse_dirt")) {
                        continue;
                    }
                    if (!bp.isAir(x, 1, z) || !bp.isAir(x, 2, z)) {
                        continue;
                    }
                    boolean dense = nearSolid(x, z);
                    if (!chance(dense ? 0.5 : 0.16)) {
                        continue;
                    }
                    double r = rnd.nextDouble();
                    if (r < 0.3) {
                        bp.set(x, 1, z, "tall_grass[half=lower]");
                        bp.set(x, 2, z, "tall_grass[half=upper]");
                    } else if (r < 0.42 && dense) {
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
                    if (!bp.inBounds(x + dx, 1, z + dz)) {
                        continue;
                    }
                    String s = bp.get(x + dx, 1, z + dz);
                    if (!s.equals(Blueprint.AIR) && !s.contains("grass") && !s.contains("fern")) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void weathering() {
            for (int x = OX1 + 1; x < OX2; x++) {
                for (int z = OZ1 + 1; z < OZ2; z++) {
                    if (bp.isAir(x, 4, z) && chance(0.04)) {
                        bp.set(x, 4, z, "cobweb");
                    }
                }
            }
            for (int x = MX1 + 1; x < MX2; x++) {
                for (int z = MZ1 + 1; z < MZ2; z++) {
                    if (bp.isAir(x, HALL_ROOF - 1, z) && chance(0.02)) {
                        bp.set(x, HALL_ROOF - 1, z, "cobweb");
                    }
                }
            }
            for (int z = OZ1 + 2; z < OZ2; z += 5) {
                for (int y = 2; y <= 4; y++) {
                    if (chance(0.6) && bp.isAir(OX2 + 1, y, z)) {
                        bp.set(OX2 + 1, y, z, "vine[east=false,north=false,south=false,up=false,west=true]");
                    }
                }
            }
            for (int i = 0; i < 20; i++) {
                int z = OZ1 + rnd.nextInt(OZ2 - OZ1 + 1);
                int y = 1 + rnd.nextInt(2);
                if (!bp.isAir(OX1, y, z) && !bp.get(OX1, y, z).contains("bars")) {
                    bp.set(OX1, y, z, chance(0.5) ? "mossy_stone_bricks" : "mossy_cobblestone");
                }
            }
        }

        // ------------------------------------------------------------------ pomocnicze

        private void tires(int x, int z) {
            bp.set(x, 1, z, "blackstone_wall");
            if (chance(0.6)) {
                bp.set(x, 2, z, "blackstone_wall");
            }
        }

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
