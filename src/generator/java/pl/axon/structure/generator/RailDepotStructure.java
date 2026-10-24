package pl.axon.structure.generator;

import pl.axon.structure.api.Entrance;
import pl.axon.structure.api.SizeClass;
import pl.axon.structure.core.Blueprint;
import pl.axon.structure.core.Palette;
import pl.axon.structure.core.Traits;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Opuszczona zajezdnia kolejowa: ceglana lokomotywownia na cztery tory, przybudówka z warsztatem i dyspozytornią,
 * nastawnia, zbiornik paliwa, odstawiony wagon i trzy spalinowe lokomotywy (model {@code axon:lokomotywa} z paczki).
 * Tory wychodzą z hali na 25 kratek, coraz bardziej rozkradzione, i urywają się na końcu placu.
 * <p>
 * Plan w lokalnych współrzędnych (y względem gruntu: 0 = podkłady i grunt, 1 = szyny):
 * <pre>
 *  z 0-6    plac od drogi, wjazd z drogi w x 42-46 (z 0, kierunek north), ogrodzenie z bramą
 *  hala     x 3-28, z 6-36, tory w x 8, 13, 18, 23, bramy w ścianie z 36
 *  przybud. x 28-39, z 8-22: parter warsztat i socjalny, piętro dyspozytornia (drabina w x 38, z 21)
 *  tory     poza halą z 37-61 (25 kratek), od z 50 coraz bardziej zniszczone, końce w z 55-60
 *  lok.     tor 8 z 14-27 (w hali), tor 18 z 30-43 (w bramie), tor 23 z 42-55 (na placu)
 *  nastawnia x 31-36, z 40-45; zbiornik paliwa x 43-49, z 24-33; wagon x 37-41, z 48-60
 * </pre>
 */
public final class RailDepotStructure implements StructureDefinition {

    private static final int WIDTH = 52;
    private static final int DEPTH = 66;
    private static final int G = 3;
    private static final int HEIGHT = G + 20;

    static final String LOCO_MODEL = "axon:lokomotywa";
    /** Skala modelu: 3 jednostki modelu na kratkę (gen_lokomotywa.py w QualityArmory-texturepack). */
    static final float LOCO_SCALE = 16f / 3f;
    /** Długość lokomotywy w kratkach. */
    static final int LOCO_LENGTH = 14;

    private static final int[] TRACKS = {8, 13, 18, 23};
    private static final int HALL_X1 = 3;
    private static final int HALL_X2 = 28;
    private static final int HALL_Z1 = 6;
    private static final int HALL_Z2 = 36;
    private static final int WALL_TOP = 9;
    /** Ostatnia kratka torów poza halą (25 kratek od bramy). */
    private static final int TRACK_END = HALL_Z2 + 25;

    @Override
    public String id() {
        return "zajezdnia";
    }

    @Override
    public String displayName() {
        return "Opuszczona zajezdnia kolejowa";
    }

    @Override
    public String description() {
        return "lokomotywownia na cztery tory, trzy porzucone lokomotywy, nastawnia i urwane tory";
    }

    @Override
    public Blueprint build(Random random) {
        Traits traits = new Traits(List.of(new Entrance(44, 0, "north")), true, SizeClass.SREDNIA,
                Set.of("rownina"), Set.of("snieg", "umiarkowany", "suchy"), false, Map.of());
        return new Builder(random).build().traits(traits);
    }

    private static final class Builder {

        private final Random rnd;
        private final Blueprint bp = new Blueprint(WIDTH, HEIGHT, DEPTH);
        private final Palette soil;
        private final Palette lawn;
        private final Palette ballast;
        private final Palette brick;
        private final Palette concrete;
        private final Palette yard;
        private final Palette roof;
        private final Palette rubble;

        Builder(Random rnd) {
            this.rnd = rnd;
            soil = new Palette(rnd).add("dirt", 8).add("stone", 3).add("gravel", 2);
            lawn = new Palette(rnd).add("grass_block", 10).add("coarse_dirt", 3).add("dirt", 1).add("podzol", 1);
            ballast = new Palette(rnd).add("gravel", 10).add("andesite", 2).add("tuff", 2).add("coarse_dirt", 1);
            brick = new Palette(rnd).add("bricks", 16).add("terracotta", 2).add("brown_terracotta", 1).add("granite", 1);
            concrete = new Palette(rnd).add("smooth_stone", 6).add("light_gray_concrete", 4).add("gray_concrete", 1)
                    .add("andesite", 1).add("cobblestone", 1);
            yard = new Palette(rnd).add("gray_concrete", 6).add("light_gray_concrete", 3).add("andesite", 2)
                    .add("cobblestone", 1).add("gravel", 1);
            roof = new Palette(rnd).add("deepslate_tile_slab", 6).add("cobbled_deepslate_slab", 2).add("blackstone_slab", 1);
            rubble = new Palette(rnd).add("cobblestone", 3).add("bricks", 3).add("gravel", 2).add("mossy_cobblestone", 1);
        }

        Blueprint build() {
            bp.ground(G);
            ground();
            frontYard();
            tracks();
            hall();
            hallInterior();
            annex();
            signalBox();
            fuelTank();
            wagon();
            yardDetails();
            locomotive(8, 14, 0);
            locomotive(18, 30, 1);
            locomotive(23, 42, 2);
            guards();
            stories();
            return bp;
        }

        // ------------------------------------------------------------------ pomocnicze (y względem gruntu)

        private void set(int x, int y, int z, String state) {
            bp.set(x, y + G, z, state);
        }

        private String get(int x, int y, int z) {
            return bp.get(x, y + G, z);
        }

        private boolean air(int x, int y, int z) {
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

        private void sign(int x, int y, int z, String facing, String... lines) {
            bp.sign(x, y + G, z, "spruce_wall_sign[facing=" + facing + ",waterlogged=false]", lines);
        }

        private boolean chance(double p) {
            return rnd.nextDouble() < p;
        }

        private static boolean isTrack(int x) {
            for (int t : TRACKS) {
                if (Math.abs(x - t) <= 1) {
                    return true;
                }
            }
            return false;
        }

        private void weeds(int x, int z) {
            if (air(x, 1, z) && get(x, 0, z).endsWith("grass_block")) {
                double r = rnd.nextDouble();
                set(x, 1, z, r < 0.6 ? "short_grass" : r < 0.8 ? "fern" : r < 0.9 ? "dead_bush" : "tall_grass[half=lower]");
                if (get(x, 1, z).startsWith("minecraft:tall_grass")) {
                    set(x, 2, z, "tall_grass[half=upper]");
                }
            }
        }

        // ------------------------------------------------------------------ teren i plac

        private void ground() {
            fill(0, -G, 0, WIDTH - 1, -1, DEPTH - 1, soil);
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    set(x, 0, z, lawn.next());
                }
            }
        }

        private void frontYard() {
            // utwardzony plac przed halą i przy przybudówce
            for (int x = 1; x < WIDTH - 1; x++) {
                for (int z = 0; z <= 7; z++) {
                    if (chance(0.92)) {
                        set(x, 0, z, yard.next());
                    }
                }
            }
            for (int x = 40; x <= 50; x++) {
                for (int z = 8; z <= 20; z++) {
                    if (chance(0.85)) {
                        set(x, 0, z, yard.next());
                    }
                }
            }
            // dojazd od drogi: asfalt z pasem
            for (int x = 42; x <= 46; x++) {
                for (int z = 0; z <= 6; z++) {
                    set(x, 0, z, chance(0.1) ? "gray_concrete" : "black_concrete");
                }
            }
            // ogrodzenie: słupki i kraty, brama wyrwana
            for (int x = 0; x < WIDTH; x++) {
                if (x >= 41 && x <= 47) {
                    continue;
                }
                if (x % 4 == 0) {
                    fill(x, 1, 1, x, 2, 1, "stone_brick_wall");
                } else if (chance(0.75)) {
                    set(x, 1, 1, "iron_bars");
                    if (chance(0.7)) {
                        set(x, 2, 1, "iron_bars");
                    }
                }
            }
            fill(40, 1, 1, 40, 3, 1, "stone_bricks");
            fill(48, 1, 1, 48, 3, 1, "stone_bricks");
            // skrzydło bramy leży na ziemi
            for (int x = 42; x <= 44; x++) {
                set(x, 1, 3, "iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
            }
            fill(47, 1, 2, 47, 2, 2, "iron_bars");
            for (int z = 2; z < DEPTH - 1; z += 1) {
                if (z % 4 == 1) {
                    fill(WIDTH - 1, 1, z, WIDTH - 1, 2, z, "stone_brick_wall");
                } else if (chance(0.45)) {
                    set(WIDTH - 1, 1, z, "iron_bars");
                }
            }
            // wrak malucha na parkingu
            int cx = 44;
            int cz = 11;
            fill(cx, 1, cz, cx + 1, 1, cz + 3, "red_terracotta");
            fill(cx, 2, cz + 1, cx + 1, 2, cz + 2, "glass_pane");
            set(cx, 2, cz + 1, "air");
            fill(cx, 3, cz + 1, cx + 1, 3, cz + 2, "red_terracotta");
            set(cx, 2, cz, "red_terracotta");
            set(cx + 1, 2, cz + 3, "red_terracotta");
            set(cx - 1, 1, cz + 3, "black_concrete");
            crate(cx + 1, 2, cz + 1, "west", "plac");
        }

        // ------------------------------------------------------------------ tory

        /** Zniszczenie toru w kratce z: 0 do z 50, potem rośnie do 1 na końcu toru. */
        private double damage(int z, int end) {
            if (z < 50) {
                return 0.0;
            }
            return Math.min(1.0, (z - 50) / (double) Math.max(1, end - 50));
        }

        private void tracks() {
            int[] ends = {55, 58, TRACK_END - 1, 57};
            for (int i = 0; i < TRACKS.length; i++) {
                int t = TRACKS[i];
                int end = ends[i];
                // podsypka poza halą
                for (int z = HALL_Z2; z <= Math.min(DEPTH - 1, end + 3); z++) {
                    double dmg = damage(z, end);
                    for (int x = t - 2; x <= t + 2; x++) {
                        if (z > end && chance(0.5 + 0.1 * (z - end))) {
                            continue;
                        }
                        set(x, 0, z, chance(dmg * 0.4) ? lawn.next() : ballast.next());
                    }
                }
                for (int z = HALL_Z1 + 2; z <= end; z++) {
                    boolean inside = z < HALL_Z2;
                    double dmg = damage(z, end);
                    // podkłady co drugą kratkę, część przesunięta albo ukradziona
                    if (z % 2 == 0 && !chance(dmg * 0.5)) {
                        String sleeper = chance(0.3) ? "spruce_log" : "stripped_spruce_log";
                        boolean skew = chance(dmg * 0.5);
                        fill(t - 1, 0, z, t + 1, 0, z, sleeper + "[axis=" + (skew ? "z" : "x") + "]");
                    } else if (inside) {
                        fill(t - 1, 0, z, t + 1, 0, z, "polished_andesite");
                    }
                    if (!chance(dmg * 0.7)) {
                        set(t, 1, z, "rail[shape=north_south,waterlogged=false]");
                    }
                }
            }
            // tor 13: szyna wygięta do góry na samym końcu
            set(13, 1, 59, "rail[shape=ascending_south,waterlogged=false]");
            set(13, 1, 60, "gravel");
            // tor 18: podmyty nasyp, za nim rów z wodą
            for (int x = 14; x <= 22; x++) {
                for (int z = TRACK_END + 1; z <= TRACK_END + 3; z++) {
                    int depth = (x == 14 || x == 22 || z == TRACK_END + 3) ? 1 : 2;
                    fill(x, 1 - depth, z, x, 1, z, "air");
                    set(x, -depth, z, chance(0.5) ? "mud" : "gravel");
                    if (depth == 2 && chance(0.6)) {
                        set(x, -1, z, "water[level=0]");
                    }
                }
            }
            set(17, 0, TRACK_END + 2, "stripped_spruce_log[axis=x]");
            set(19, -1, TRACK_END + 1, "stripped_spruce_log[axis=z]");
            // tor 8: kopiec ziemi z krzakami, na którym tor się kończy
            for (int x = 6; x <= 10; x++) {
                set(x, 1, 56, chance(0.6) ? "coarse_dirt" : "dirt");
                if (chance(0.5)) {
                    set(x, 2, 56, "azalea_leaves[distance=1,persistent=true,waterlogged=false]");
                }
            }
            // luźne szyny i stos podkładów
            for (int[] r : new int[][]{{11, 54}, {15, 57}, {26, 52}, {20, 56}}) {
                set(r[0], 1, r[1], "rail[shape=east_west,waterlogged=false]");
            }
            fill(27, 1, 44, 29, 1, 49, "stripped_spruce_log[axis=z]");
            fill(27, 2, 45, 28, 2, 48, "stripped_spruce_log[axis=z]");
            set(29, 2, 46, "spruce_log[axis=z]");
            // trawa i samosiejki między torami
            for (int x = 1; x < WIDTH - 1; x++) {
                for (int z = HALL_Z2 + 1; z < DEPTH - 1; z++) {
                    if (!isTrack(x) && chance(0.35)) {
                        weeds(x, z);
                    } else if (isTrack(x) && z > 50 && chance(0.2)) {
                        weeds(x, z);
                    }
                }
            }
            set(15, 1, 52, "azalea_leaves[distance=1,persistent=true,waterlogged=false]");
            set(16, 1, 53, "azalea_leaves[distance=1,persistent=true,waterlogged=false]");
            tree(10, 61, 4);
            tree(25, 63, 6);
            tree(3, 48, 5);
        }

        private void tree(int x, int z, int h) {
            set(x, 0, z, "grass_block");
            fill(x, 1, z, x, h, z, "birch_log[axis=y]");
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = h - 1; dy <= h + 1; dy++) {
                        int r = Math.abs(dx) + Math.abs(dz) + (dy == h + 1 ? 2 : 0);
                        if (r <= 3 && air(x + dx, dy, z + dz) && chance(0.85) && bp.inBounds(x + dx, dy + G, z + dz)) {
                            set(x + dx, dy, z + dz, "birch_leaves[distance=1,persistent=true,waterlogged=false]");
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------ hala lokomotywowni

        private int roofY(int x) {
            int dist = Math.min(x - HALL_X1, HALL_X2 - x);
            return 2 * (WALL_TOP + 1) + dist;   // w połówkach bloku
        }

        private void hall() {
            // posadzka
            for (int x = HALL_X1 + 1; x < HALL_X2; x++) {
                for (int z = HALL_Z1 + 1; z < HALL_Z2; z++) {
                    if (!isTrack(x)) {
                        set(x, 0, z, chance(0.06) ? "black_concrete_powder" : concrete.next());
                    }
                }
            }
            // ściany
            for (int y = 1; y <= WALL_TOP; y++) {
                for (int x = HALL_X1; x <= HALL_X2; x++) {
                    set(x, y, HALL_Z1, y == 1 ? "stone_bricks" : brick.next());
                    set(x, y, HALL_Z2, y == 1 ? "stone_bricks" : brick.next());
                }
                for (int z = HALL_Z1; z <= HALL_Z2; z++) {
                    set(HALL_X1, y, z, y == 1 ? "stone_bricks" : brick.next());
                    set(HALL_X2, y, z, y == 1 ? "stone_bricks" : brick.next());
                }
            }
            // gzyms i szczyty pod dachem
            for (int x = HALL_X1; x <= HALL_X2; x++) {
                set(x, WALL_TOP, HALL_Z1, "smooth_stone");
                set(x, WALL_TOP, HALL_Z2, "smooth_stone");
                int top = roofY(x) / 2 - 1;
                for (int y = WALL_TOP + 1; y <= top; y++) {
                    set(x, y, HALL_Z1, brick.next());
                    set(x, y, HALL_Z2, brick.next());
                }
            }
            for (int z = HALL_Z1; z <= HALL_Z2; z++) {
                set(HALL_X1, WALL_TOP, z, "smooth_stone");
                set(HALL_X2, WALL_TOP, z, "smooth_stone");
            }
            // okna w długich ścianach: wysokie, większość szyb wybita
            for (int z = HALL_Z1 + 3; z < HALL_Z2 - 2; z += 5) {
                for (int x : new int[]{HALL_X1, HALL_X2}) {
                    for (int y = 3; y <= 7; y++) {
                        for (int dz = 0; dz <= 1; dz++) {
                            double r = rnd.nextDouble();
                            set(x, y, z + dz, r < 0.35 ? "glass_pane" : r < 0.45 ? "iron_bars" : "air");
                        }
                    }
                    set(x, 8, z, "brick_slab[type=top,waterlogged=false]");
                    set(x, 8, z + 1, "brick_slab[type=top,waterlogged=false]");
                }
            }
            // okna w szczycie od placu
            for (int x : new int[]{7, 11, 20, 24}) {
                fill(x, 4, HALL_Z1, x + 1, 7, HALL_Z1, chance(0.5) ? "air" : "glass_pane");
            }
            fill(14, 11, HALL_Z1, 17, 12, HALL_Z1, "air");
            fill(14, 11, HALL_Z2, 17, 12, HALL_Z2, "iron_bars");
            // drzwi dla ludzi od placu, jedno skrzydło wyrwane
            set(15, 1, HALL_Z1, "dark_oak_door[facing=south,half=lower,hinge=left,open=true,powered=false]");
            set(15, 2, HALL_Z1, "dark_oak_door[facing=south,half=upper,hinge=left,open=true,powered=false]");
            set(16, 1, HALL_Z1, "air");
            set(16, 2, HALL_Z1, "air");
            set(16, 1, HALL_Z1 - 2, "dark_oak_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
            // bramy dla lokomotyw w ścianie od torów
            for (int t : TRACKS) {
                fill(t - 1, 1, HALL_Z2, t + 1, 6, HALL_Z2, "air");
                fill(t - 2, 7, HALL_Z2, t + 2, 7, HALL_Z2, "stone_bricks");
                set(t - 2, 1, HALL_Z2, "stone_bricks");
                set(t + 2, 1, HALL_Z2, "stone_bricks");
            }
            // skrzydła bram: przy torze 13 jedno wisi zamknięte, przy 23 leży na ziemi
            fill(12, 1, HALL_Z2, 12, 6, HALL_Z2, "spruce_planks");
            set(12, 3, HALL_Z2, "air");
            fill(25, 1, HALL_Z2 + 1, 25, 1, HALL_Z2 + 3, "spruce_slab[type=bottom,waterlogged=false]");
            fill(26, 1, HALL_Z2 + 1, 26, 1, HALL_Z2 + 3, "spruce_slab[type=bottom,waterlogged=false]");
            // wyłom w narożniku
            for (int y = 6; y <= WALL_TOP; y++) {
                for (int z = 29; z <= 33 - (WALL_TOP - y); z++) {
                    set(HALL_X1, y, z, "air");
                }
            }
            fill(1, 1, 30, 2, 1, 33, rubble);
            set(2, 2, 31, "bricks");
            // dach dwuspadowy z dziurami i zawalonym kawałkiem
            for (int x = HALL_X1 - 1; x <= HALL_X2 + 1; x++) {
                int xc = Math.max(HALL_X1, Math.min(HALL_X2, x));
                int half = roofY(xc) - (x != xc ? 1 : 0);
                for (int z = HALL_Z1 - 1; z <= HALL_Z2 + 1; z++) {
                    boolean collapsed = x >= 11 && x <= 18 && z >= 21 && z <= 27;
                    if (collapsed || chance(0.08)) {
                        continue;
                    }
                    String slab = roof.next();
                    set(x, half / 2, z, slab + "[type=" + (half % 2 == 0 ? "bottom" : "top") + ",waterlogged=false]");
                }
            }
            // gruz pod zawalonym dachem
            for (int x = 10; x <= 17; x++) {
                for (int z = 20; z <= 28; z++) {
                    if (x >= 12 && x <= 14) {
                        if (chance(0.4)) {
                            set(x, 1, z, chance(0.5) ? "cobbled_deepslate_slab[type=bottom,waterlogged=false]" : rubble.next());
                        }
                    } else if (chance(0.25) && air(x, 1, z) && !isTrack(x)) {
                        set(x, 1, z, "deepslate_tile_slab[type=bottom,waterlogged=false]");
                    }
                }
            }
            fill(11, 1, 23, 11, 2, 24, rubble);
        }

        private void hallInterior() {
            // kozły oporowe na końcu torów
            for (int t : TRACKS) {
                fill(t - 1, 1, HALL_Z1 + 1, t + 1, 1, HALL_Z1 + 1, "spruce_planks");
                set(t, 2, HALL_Z1 + 1, "red_terracotta");
            }
            // ściągi dachu z łańcuchami
            for (int z = 12; z < HALL_Z2; z += 6) {
                fill(HALL_X1 + 1, WALL_TOP, z, HALL_X2 - 1, WALL_TOP, z, "stripped_dark_oak_log[axis=x]");
                if (z == 24) {
                    for (int x = 11; x <= 18; x++) {
                        set(x, WALL_TOP, z, chance(0.5) ? "air" : get(x, WALL_TOP, z));
                    }
                }
            }
            // suwnica nad torami: belka na wspornikach, hak na łańcuchu
            fill(HALL_X1 + 1, 8, 9, HALL_X1 + 1, 8, 33, "polished_blackstone_bricks");
            fill(HALL_X2 - 1, 8, 9, HALL_X2 - 1, 8, 33, "polished_blackstone_bricks");
            fill(HALL_X1 + 2, 8, 17, HALL_X2 - 2, 8, 17, "yellow_concrete");
            fill(HALL_X1 + 2, 8, 18, HALL_X2 - 2, 8, 18, "yellow_concrete");
            for (int y = 5; y <= 7; y++) {
                set(13, y, 17, "chain[axis=y,waterlogged=false]");
            }
            set(13, 4, 17, "anvil[facing=north]");
            // warsztat wzdłuż zachodniej ściany
            set(4, 1, 9, "crafting_table");
            set(4, 1, 10, "grindstone[face=floor,facing=east]");
            set(4, 1, 11, "crafting_table");
            crate(4, 1, 12, "east", "lokomotywownia");
            set(4, 1, 14, "barrel[facing=up,open=false]");
            set(4, 2, 14, "barrel[facing=up,open=false]");
            set(4, 1, 15, "respawn_anchor[charges=0]");
            crate(5, 1, 20, "east", "lokomotywownia");
            set(4, 1, 22, "sticky_piston[extended=false,facing=east]");
            set(4, 1, 26, "composter[level=3]");
            crate(4, 1, 31, "east", "lokomotywownia");
            for (int z = 16; z <= 19; z++) {
                set(4, 3, z, "chain[axis=z,waterlogged=false]");
            }
            // między torami 13 i 18: wózek z narzędziami, palety, olej na posadzce
            crate(15, 1, 10, "west", "lokomotywownia");
            set(16, 1, 10, "barrel[facing=up,open=false]");
            set(15, 1, 11, "spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
            set(16, 1, 11, "spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
            set(15, 2, 11, "spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
            for (int z = 14; z <= 18; z++) {
                set(11, 0, z, "black_concrete_powder");
            }
            // tor 23 w hali: wózek z kołami (zestaw kołowy) i skrzynie
            set(23, 1, 12, "grindstone[face=floor,facing=north]");
            set(23, 1, 13, "grindstone[face=floor,facing=north]");
            crate(26, 1, 9, "west", "lokomotywownia");
            crate(26, 1, 27, "west", "lokomotywownia");
            set(26, 1, 28, "respawn_anchor[charges=0]");
            set(27, 1, 20, "sticky_piston[extended=false,facing=west]");
            // przejście do przybudówki
            fill(HALL_X2, 1, 14, HALL_X2, 2, 14, "air");
            // pajęczyny pod dachem
            for (int i = 0; i < 14; i++) {
                int x = HALL_X1 + 1 + rnd.nextInt(HALL_X2 - HALL_X1 - 1);
                int z = HALL_Z1 + 1 + rnd.nextInt(HALL_Z2 - HALL_Z1 - 1);
                if (air(x, WALL_TOP - 1, z)) {
                    set(x, WALL_TOP - 1, z, "cobweb");
                }
            }
        }

        // ------------------------------------------------------------------ przybudówka: warsztat, socjalny, dyspozytornia

        private void annex() {
            int x1 = HALL_X2;
            int x2 = 39;
            int z1 = 8;
            int z2 = 22;
            for (int y = 1; y <= 10; y++) {
                for (int x = x1 + 1; x <= x2; x++) {
                    set(x, y, z1, y == 1 ? "stone_bricks" : brick.next());
                    set(x, y, z2, y == 1 ? "stone_bricks" : brick.next());
                }
                for (int z = z1; z <= z2; z++) {
                    set(x2, y, z, y == 1 ? "stone_bricks" : brick.next());
                    if (y > WALL_TOP) {
                        set(x1, y, z, brick.next());
                    }
                }
            }
            fill(x1 + 1, 0, z1 + 1, x2 - 1, 0, z2 - 1, "polished_andesite");
            // strop i dach
            fill(x1 + 1, 5, z1 + 1, x2 - 1, 5, z2 - 1, "spruce_planks");
            for (int x = x1; x <= x2 + 1; x++) {
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    if (!chance(0.06)) {
                        set(x, 11, z, "smooth_stone_slab[type=bottom,waterlogged=false]");
                    }
                }
            }
            // okna obu kondygnacji
            for (int z = z1 + 2; z < z2 - 1; z += 4) {
                for (int y : new int[]{2, 7}) {
                    set(x2, y, z, chance(0.5) ? "glass_pane" : "air");
                    set(x2, y + 1, z, chance(0.5) ? "glass_pane" : "air");
                    set(x2, y, z + 1, chance(0.5) ? "glass_pane" : "air");
                    set(x2, y + 1, z + 1, chance(0.5) ? "glass_pane" : "air");
                }
            }
            for (int x = 31; x <= 36; x += 5) {
                fill(x, 2, z1, x + 1, 3, z1, "glass_pane");
                fill(x, 7, z1, x + 1, 8, z1, "air");
            }
            // wejście od parkingu
            set(34, 1, z1, "iron_door[facing=south,half=lower,hinge=right,open=true,powered=false]");
            set(34, 2, z1, "iron_door[facing=south,half=upper,hinge=right,open=true,powered=false]");
            // parter: ścianka z drzwiami między warsztatem a socjalnym
            fill(x1 + 1, 1, 15, x2 - 1, 4, 15, "white_terracotta");
            fill(33, 1, 15, 33, 2, 15, "air");
            // warsztat (z 9-14)
            set(29, 1, 9, "crafting_table");
            set(30, 1, 9, "grindstone[face=floor,facing=south]");
            set(31, 1, 9, "crafting_table");
            crate(32, 1, 9, "south", "warsztat-kolejowy");
            set(38, 1, 9, "sticky_piston[extended=false,facing=west]");
            set(38, 2, 9, "sticky_piston[extended=false,facing=west]");
            crate(38, 1, 13, "west", "warsztat-kolejowy");
            set(37, 1, 13, "respawn_anchor[charges=0]");
            set(37, 1, 11, "anvil[facing=east]");
            // skup złomu: lada ze stanowiskiem GUI (punkt-sklep-zajezdnia), za nią waga i skrzynki
            set(32, 1, 11, "smooth_stone_slab[type=top,waterlogged=false]");
            bp.crate(33, 1 + G, 11, "north", "punkt-sklep-zajezdnia");
            set(33, 1, 11, "smooth_stone_slab[type=top,waterlogged=false]");
            set(34, 1, 11, "smooth_stone_slab[type=top,waterlogged=false]");
            set(33, 1, 12, "heavy_weighted_pressure_plate[power=0]");
            bp.sign(34, 2 + G, 11, "oak_sign[rotation=8,waterlogged=false]", "SKUP ZŁOMU", "KOLOROWY", "I CZARNY", "WAGA UCZCIWA");
            for (int x = 29; x <= 32; x++) {
                set(x, 3, 9, "chain[axis=x,waterlogged=false]");
            }
            // socjalny (z 16-21): stół, kuchenka, lodówka, kibel za ścianką
            set(31, 1, 18, "spruce_fence");
            set(31, 2, 18, "spruce_pressure_plate[powered=false]");
            set(30, 1, 18, "spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
            set(32, 1, 18, "spruce_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
            set(29, 1, 21, "smoker[facing=east,lit=false]");
            set(29, 1, 20, "cauldron");
            crate(29, 1, 16, "east", "lodowki");
            crate(34, 1, 21, "north", "zaplecze");
            fill(36, 1, 16, 36, 4, 21, "white_terracotta");
            set(36, 1, 18, "air");
            set(36, 2, 18, "air");
            set(37, 1, 17, "end_portal_frame[eye=false,facing=west]");
            crate(38, 1, 20, "west", "toalety");
            // drabina na piętro
            for (int y = 1; y <= 5; y++) {
                set(35, y, 21, "ladder[facing=north,waterlogged=false]");
            }
            // piętro: dyspozytornia
            set(30, 6, 10, "lectern[facing=south,has_book=false,powered=false]");
            set(33, 6, 10, "lectern[facing=south,has_book=false,powered=false]");
            set(29, 6, 13, "chiseled_bookshelf[facing=east,slot_0_occupied=true,slot_1_occupied=false,slot_2_occupied=true,"
                    + "slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=false]");
            set(29, 6, 14, "bookshelf");
            crate(38, 6, 10, "west", "dyspozytornia");
            crate(30, 6, 20, "east", "dyspozytornia");
            set(37, 6, 16, "red_bed[facing=south,occupied=false,part=head]");
            set(37, 6, 15, "red_bed[facing=south,occupied=false,part=foot]");
            set(33, 6, 19, "furnace[facing=north,lit=false]");
            set(34, 6, 13, "observer[facing=south,powered=false]");
            set(29, 6, 18, "composter[level=5]");
        }

        // ------------------------------------------------------------------ nastawnia

        private void signalBox() {
            int x1 = 31;
            int x2 = 36;
            int z1 = 40;
            int z2 = 45;
            for (int y = 1; y <= 9; y++) {
                for (int x = x1; x <= x2; x++) {
                    for (int z = z1; z <= z2; z++) {
                        boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                        boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                        if (!edge) {
                            set(x, y, z, y == 5 ? "spruce_planks" : "air");
                        } else if (y <= 5) {
                            set(x, y, z, y == 1 ? "stone_bricks" : brick.next());
                        } else if (y <= 8) {
                            set(x, y, z, corner ? "dark_oak_log[axis=y]" : y == 6 ? "dark_oak_planks"
                                    : chance(0.4) ? "glass_pane" : "air");
                        }
                    }
                }
            }
            fill(x1 + 1, 0, z1 + 1, x2 - 1, 0, z2 - 1, "polished_andesite");
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    set(x, 9, z, "dark_oak_slab[type=bottom,waterlogged=false]");
                }
            }
            set(33, 10, 42, "brick_wall");
            set(33, 1, z1, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
            set(33, 2, z1, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
            set(x1, 3, 42, "air");
            for (int y = 1; y <= 5; y++) {
                set(35, y, 44, "ladder[facing=north,waterlogged=false]");
            }
            // parter: przekaźniki, beczki
            set(32, 1, 44, "sticky_piston[extended=false,facing=east]");
            set(32, 2, 44, "sticky_piston[extended=false,facing=east]");
            crate(34, 1, 41, "south", "nastawnia");
            // piętro: rząd dźwigni zwrotnic, pulpit
            for (int x = 32; x <= 35; x++) {
                set(x, 6, 41, "polished_andesite");
                set(x, 7, 41, "lever[face=floor,facing=south,powered=" + (x % 2 == 0) + "]");
            }
            set(32, 6, 44, "lectern[facing=east,has_book=true,powered=false]");
            crate(34, 6, 43, "west", "nastawnia");
            set(33, 6, 43, "observer[facing=north,powered=false]");
        }

        // ------------------------------------------------------------------ zbiornik paliwa i dystrybutor

        private void fuelTank() {
            int cx = 46;
            int cy = 4;
            for (int z = 24; z <= 33; z++) {
                for (int x = cx - 3; x <= cx + 3; x++) {
                    for (int y = cy - 3; y <= cy + 3; y++) {
                        int dx = x - cx;
                        int dy = y - cy;
                        if (dx * dx + dy * dy <= 10) {
                            boolean end = z == 24 || z == 33;
                            set(x, y, z, chance(0.15) ? "orange_terracotta" : end ? "light_gray_concrete" : "white_terracotta");
                        }
                    }
                }
            }
            for (int z : new int[]{25, 32}) {
                fill(cx - 2, 1, z, cx + 2, 1, z, "stone_bricks");
            }
            for (int y = 1; y <= 7; y++) {
                set(cx + 4, y, 28, "ladder[facing=east,waterlogged=false]");
            }
            set(cx + 4, 1, 28, "air");
            fill(cx + 4, 1, 27, cx + 4, 1, 29, "iron_bars");
            set(cx, 8, 28, "iron_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]");
            // rura do dystrybutora przy torze 23
            fill(28, 1, 30, cx - 4, 1, 30, "smooth_stone_slab[type=bottom,waterlogged=false]");
            crate(42, 1, 26, "east", "paliwo");
            crate(42, 1, 31, "east", "paliwo");
            set(41, 1, 26, "respawn_anchor[charges=0]");
            // dystrybutor oleju napędowego dla lokomotyw (model 2 kratki, nad nim bariera)
            set(26, 1, 39, "damaged_anvil[facing=west]");
            set(26, 2, 39, "barrier");
            crate(27, 1, 40, "west", "paliwo");
        }

        // ------------------------------------------------------------------ odstawiony wagon

        private void wagon() {
            int x1 = 37;
            int x2 = 41;
            int z1 = 48;
            int z2 = 60;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    set(x, 1, z, "spruce_planks");
                    boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                    for (int y = 2; y <= 4; y++) {
                        set(x, y, z, edge ? (chance(0.12) ? "orange_terracotta" : "brown_terracotta") : "air");
                    }
                    set(x, 5, z, (x == x1 || x == x2) ? "brown_terracotta"
                            : "dark_oak_slab[type=" + (x == 39 ? "top" : "bottom") + ",waterlogged=false]");
                }
            }
            // podwozie na podkładach
            for (int z : new int[]{49, 59}) {
                fill(x1, 0, z, x2, 0, z, "stripped_spruce_log[axis=x]");
            }
            // drzwi przesuwne odsunięte, druga strona dziura po szlifierce
            fill(x1, 2, 53, x1, 4, 55, "air");
            fill(x1 - 1, 2, 56, x1 - 1, 4, 57, "spruce_planks");
            fill(x2, 2, 52, x2, 3, 53, "air");
            set(x1 - 1, 1, 54, "spruce_slab[type=bottom,waterlogged=false]");
            // lokator: legowisko, ognisko, skrzynie
            set(39, 2, 50, "brown_bed[facing=north,occupied=false,part=head]");
            set(39, 2, 51, "brown_bed[facing=north,occupied=false,part=foot]");
            set(40, 2, 55, "campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]");
            set(40, 2, 59, "barrel[facing=up,open=false]");
            crate(38, 2, 49, "south", "wagon");
            crate(40, 2, 49, "south", "wagon");
            crate(38, 2, 59, "north", "wagon");
            set(39, 2, 57, "composter[level=2]");
        }

        // ------------------------------------------------------------------ drobiazgi na placu

        private void yardDetails() {
            // żuraw wodny przy torach
            fill(20, 1, 47, 20, 4, 47, "polished_blackstone_wall");
            set(19, 4, 47, "polished_blackstone_wall");
            set(18, 4, 47, "dark_oak_trapdoor[facing=east,half=top,open=false,powered=false,waterlogged=false]");
            // semafor kształtowy z opuszczonym ramieniem
            fill(26, 1, 58, 26, 6, 58, "iron_bars");
            set(26, 6, 57, "red_wool");
            set(26, 7, 58, "lantern[hanging=false,waterlogged=false]");
            // beczki i opony przy hali
            set(30, 1, 33, "barrel[facing=up,open=false]");
            set(30, 1, 34, "barrel[facing=up,open=false]");
            crate(31, 1, 34, "west", "plac");
            set(2, 1, 12, "black_concrete");
            set(2, 2, 12, "black_carpet");
            set(1, 1, 20, "barrel[facing=up,open=false]");
        }

        // ------------------------------------------------------------------ lokomotywy

        /**
         * Lokomotywa z modelu {@code axon:lokomotywa} na torze w kolumnie {@code track}, od kratki {@code zs}
         * (krótka maska) przez 14 kratek. Kolizję dają bariery, środek kabiny jest pusty (wejście bokiem po stopniu).
         */
        void locomotive(int track, int zs, int variant) {
            for (int z = zs; z < zs + LOCO_LENGTH; z++) {
                int lz = z - zs;
                boolean cab = lz >= 3 && lz <= 5;
                for (int x = track - 1; x <= track + 1; x++) {
                    set(x, 2, z, "barrier");
                    if (!cab) {
                        set(x, 3, z, "barrier");
                        set(x, 4, z, "barrier");
                    } else {
                        boolean wall = x != track && lz != 4;
                        set(x, 3, z, wall ? "barrier" : "air");
                        set(x, 4, z, wall ? "barrier" : "air");
                        set(x, 5, z, "barrier");
                    }
                    if (x != track) {
                        set(x, 1, z, "air");
                    }
                }
                if (!get(track, 1, z).contains("rail")) {
                    set(track, 1, z, "rail[shape=north_south,waterlogged=false]");
                }
            }
            // stopnie pod drzwiami kabiny
            set(track - 1, 1, zs + 4, "barrier");
            set(track + 1, 1, zs + 4, "barrier");
            crate(track, 3, zs + 3, "south", "kabina");
            bp.prop(new Blueprint.PropSpec(track + 0.5, G + 1 + 2.5, zs + 7.0, LOCO_MODEL, LOCO_SCALE, 0f));
        }

        // ------------------------------------------------------------------ strażnicy i zrzut

        private void guards() {
            crate(10, 1, 30, "south", "npc-szabrownik");
            crate(20, 1, 12, "north", "npc-szabrownik");
            crate(35, 1, 19, "west", "npc-szabrownik");
            crate(39, 2, 54, "west", "npc-szabrownik");
            crate(16, 1, 55, "north", "npc-szabrownik");
            crate(33, 6, 42, "south", "npc-zolnierz");
            crate(32, 6, 17, "west", "npc-zolnierz");
            // zrzut spada na otwarty plac między nastawnią a zbiornikiem
            crate(44, 1, 41, "north", "zrzut");
        }

        // ------------------------------------------------------------------ tabliczki

        private void stories() {
            sign(15, 4, HALL_Z1 - 1, "north", "ZAKŁAD TABORU", "KOLEJOWEGO", "LOKOMOTYWOWNIA", "NR 2");
            sign(40, 2, 0, "north", "WSTĘP", "WZBRONIONY", "PSY NIE GRYZĄ", "(PSÓW NIE MA)");
            sign(14, 3, HALL_Z1 + 1, "south", "DNI BEZ", "WYPADKU:", "0", "ZENEK, WIEMY");
            sign(4, 3, 13, "east", "KLUCZ 32", "WISI TU", "JAK NIE WISI", "TO KTO?");
            sign(HALL_X2 - 1, 3, 12, "west", "LOK NIE JEDZIE", "NIE PYTAĆ", "CZEMU", "");
            sign(32, 3, 16, "south", "KAWA 5000 ZŁ", "HERBATA 3000", "NA ZESZYT", "NIE DAJEMY");
            sign(37, 3, 21, "north", "KTO WYPIŁ", "MOJĄ HERBATĘ", "Z TERMOSU", "TEN WIE. R.");
            sign(36, 7, 21, "north", "DYŻUR NOCNY:", "MIETEK", "MIETEK", "MIETEK");
            sign(38, 7, 16, "west", "ŁÓŻKO HENIA", "NIE SIADAĆ", "NAWET JAK", "GO NIE MA");
            sign(34, 3, 39, "north", "ROZKŁAD:", "6:40 ODWOŁANY", "14:10 ODWOŁANY", "RESZTA TEŻ");
            sign(46, 2, 23, "north", "ZAKAZ PALENIA", "PALIWA I TAK", "NIE MA", "KIEROWNIK");
            sign(36, 3, 51, "west", "TU MIESZKA", "WIESIEK", "PUKAĆ", "DWA RAZY");
            sign(20, 2, 46, "north", "KTO UKRADŁ", "SZYNY", "NIECH ODDA", "CHOCIAŻ JEDNĄ");
            sign(26, 2, 57, "north", "DALEJ", "NIE JEDZIE", "ZŁOMIARZE", "BYLI PIERWSI");
            // tabliczka na słupie semafora
            set(26, 2, 58, "iron_bars");
        }
    }
}
