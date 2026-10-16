package pl.axon.structure.generator;

import pl.axon.structure.core.Blueprint;

import java.util.Random;

/**
 * Meble i rekwizyty, które gracz od razu rozpozna: prawdziwe łóżka, krzesła ze schodków, stoliki, biurka,
 * kuchenki, zlewy, regały, toalety, obozowiska dzikich lokatorów i szkielety. Współrzędna y podawana jest
 * względem {@code yOffset} (np. poziomu gruntu struktury).
 */
final class Furniture {

    private final Blueprint bp;
    private final int yOffset;
    private final Random rnd;

    Furniture(Blueprint bp, int yOffset, Random rnd) {
        this.bp = bp;
        this.yOffset = yOffset;
        this.rnd = rnd;
    }

    private void set(int x, int y, int z, String state) {
        bp.set(x, y + yOffset, z, state);
    }

    static int dx(String facing) {
        return switch (facing) {
            case "east" -> 1;
            case "west" -> -1;
            default -> 0;
        };
    }

    static int dz(String facing) {
        return switch (facing) {
            case "south" -> 1;
            case "north" -> -1;
            default -> 0;
        };
    }

    static String opposite(String facing) {
        return switch (facing) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            default -> "east";
        };
    }

    /** Prawdziwe łóżko: nogi w (x, y, z), poduszka o jeden blok w stronę {@code facing}. */
    void bed(int x, int y, int z, String facing, String color) {
        set(x, y, z, color + "_bed[facing=" + facing + ",occupied=false,part=foot]");
        set(x + dx(facing), y, z + dz(facing), color + "_bed[facing=" + facing + ",occupied=false,part=head]");
    }

    /** Krzesło ze schodków; {@code back} to strona, po której jest oparcie. */
    void chair(int x, int y, int z, String back, String wood) {
        set(x, y, z, wood + "_stairs[facing=" + back + ",half=bottom,shape=straight,waterlogged=false]");
    }

    /** Okrągły stolik: płotek z płytką naciskową jako blatem. */
    void table(int x, int y, int z, String wood) {
        set(x, y, z, wood + "_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
        set(x, y + 1, z, wood + "_pressure_plate[powered=false]");
    }

    /** Biurko lub blat: górne półpłyty od (x1, z1) do (x2, z2). */
    void desk(int x1, int y, int z1, int x2, int z2, String wood) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(x, y, z, wood + "_slab[type=top,waterlogged=false]");
            }
        }
    }

    /** Stół z krzesłami po dwóch stronach (wzdłuż x albo z). */
    void dining(int x, int y, int z, boolean alongX, String wood) {
        if (alongX) {
            desk(x, y, z, x + 1, z, wood);
            chair(x, y, z - 1, "north", wood);
            chair(x + 1, y, z + 1, "south", wood);
            if (rnd.nextBoolean()) {
                chair(x + 1, y, z - 1, "north", wood);
            }
        } else {
            desk(x, y, z, x, z + 1, wood);
            chair(x - 1, y, z, "west", wood);
            chair(x + 1, y, z + 1, "east", wood);
            if (rnd.nextBoolean()) {
                chair(x - 1, y, z + 1, "west", wood);
            }
        }
    }

    /** Kuchenka (wędzarnia) z okapem nad nią. */
    void stove(int x, int y, int z, String facing) {
        set(x, y, z, "smoker[facing=" + facing + ",lit=false]");
        set(x, y + 2, z, "iron_trapdoor[facing=" + facing + ",half=top,open=false,powered=false,waterlogged=false]");
    }

    /** Zlew: kocioł z wodą i dźwignia jako kran na ścianie za nim. */
    void sink(int x, int y, int z, String wallSide) {
        set(x, y, z, "water_cauldron[level=" + (1 + rnd.nextInt(2)) + "]");
        int wx = x + dx(wallSide);
        int wz = z + dz(wallSide);
        if (!bp.isAir(wx, y + 1 + yOffset, wz) && bp.isAir(x, y + 1 + yOffset, z)) {
            set(x, y + 1, z, "tripwire_hook[attached=false,facing=" + opposite(wallSide) + ",powered=false]");
        }
    }

    /** Toaleta: kocioł z klapą (otwarta klapa przy ścianie) i spłuczką. */
    void toilet(int x, int y, int z, String wallSide) {
        set(x, y, z, "cauldron");
        if (bp.isAir(x, y + 1 + yOffset, z)) {
            set(x, y + 1, z, "birch_trapdoor[facing=" + opposite(wallSide) + ",half=bottom,open=true,powered=false,waterlogged=false]");
        }
    }

    /** Regał z książkami lub segregatorami, {@code h} bloków wysokości, od (x1, z1) do (x2, z2). */
    void bookshelf(int x1, int y, int z1, int x2, int z2, int h, String facing) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                for (int dy = 0; dy < h; dy++) {
                    set(x, y + dy, z, rnd.nextDouble() < 0.3
                            ? "chiseled_bookshelf[facing=" + facing + ",slot_0_occupied=" + rnd.nextBoolean()
                            + ",slot_1_occupied=" + rnd.nextBoolean() + ",slot_2_occupied=false,slot_3_occupied="
                            + rnd.nextBoolean() + ",slot_4_occupied=false,slot_5_occupied=" + rnd.nextBoolean() + "]"
                            : "bookshelf");
                }
            }
        }
    }

    /** Szafa: dwa bloki desek z zamkniętymi drzwiczkami (klapy) od frontu. */
    void wardrobe(int x, int y, int z, String front, String wood) {
        set(x, y, z, wood + "_planks");
        set(x, y + 1, z, wood + "_planks");
        int fx = x + dx(front);
        int fz = z + dz(front);
        for (int dy = 0; dy < 2; dy++) {
            if (bp.isAir(fx, y + dy + yOffset, fz)) {
                set(fx, y + dy, fz, wood + "_trapdoor[facing=" + front + ",half=bottom,open=true,powered=false,waterlogged=false]");
            }
        }
    }

    /** Lampka na stole lub podłodze; świeci naprawdę. */
    void lamp(int x, int y, int z) {
        set(x, y, z, rnd.nextBoolean() ? "lantern[hanging=false,waterlogged=false]"
                : "candle[candles=" + (2 + rnd.nextInt(3)) + ",lit=true,waterlogged=false]");
    }

    /** Wisząca, zapalona lampa pod sufitem (sufit w y + 1). */
    void hangingLamp(int x, int y, int z) {
        set(x, y, z, "lantern[hanging=true,waterlogged=false]");
    }

    /** Czaszka szkieletu z kośćmi obok (kość leży na boku). */
    void skeleton(int x, int y, int z, String along) {
        set(x, y, z, "skeleton_skull[powered=false,rotation=" + rnd.nextInt(16) + "]");
        int bx = x + dx(along);
        int bz = z + dz(along);
        if (bp.isAir(bx, y + yOffset, bz)) {
            set(bx, y, bz, "white_carpet");
        }
    }

    /**
     * Obozowisko dzikich lokatorów wokół (x, z): wygaszone ognisko, śpiwór (łóżko), pieniek do siedzenia,
     * zapalone świeczki i garnek. Zajmuje pole 3 × 3 z ogniskiem w środku.
     */
    void camp(int x, int y, int z, String bedFacing) {
        set(x, y, z, "campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]");
        String bedColor = new String[]{"brown", "green", "gray"}[rnd.nextInt(3)];
        int bx = x - dz(bedFacing) * 2;
        int bz = z + dx(bedFacing) * 2;
        bed(bx, y, bz, bedFacing, bedColor);
        set(x + dz(bedFacing) * 2, y, z - dx(bedFacing) * 2, "stripped_oak_log[axis=" + (dx(bedFacing) != 0 ? "x" : "z") + "]");
        set(x + dx(bedFacing) * 2 - dz(bedFacing), y, z + dz(bedFacing) * 2 + dx(bedFacing), "cauldron");
        set(x - dx(bedFacing) * 2, y, z - dz(bedFacing) * 2, "candle[candles=3,lit=true,waterlogged=false]");
    }

    /** Ławka ze schodków wzdłuż x albo z, oparcie po stronie {@code back}. */
    void bench(int x, int y, int z, int len, boolean alongX, String back, String wood) {
        for (int i = 0; i < len; i++) {
            chair(x + (alongX ? i : 0), y, z + (alongX ? 0 : i), back, wood);
        }
    }
}
