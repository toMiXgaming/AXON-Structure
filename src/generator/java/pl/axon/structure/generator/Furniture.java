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

    /** Stawia jedno skrzydło drzwi (dolną i górną połówkę) – każda struktura ma własne przesunięcia. */
    @FunctionalInterface
    interface DoorSetter {
        void door(int x, int y, int z, String type, String facing, String hinge, boolean open);
    }

    /**
     * Drzwi dwuskrzydłowe, które otwierają się symetrycznie, z zawiasami przy ościeżnicach.
     * <p>
     * W Minecrafcie {@code facing} drzwi to kierunek, w którym patrzy gracz stawiający je (drzwi otwierają się
     * w tę stronę), a {@code hinge} jest liczony z jego punktu widzenia: {@code left} to zawias po lewej ręce.
     * Skrzydło po lewej (w stronę przeciwną do ruchu wskazówek zegara od {@code facing}) dostaje więc
     * {@code hinge=left}, a skrzydło po prawej {@code hinge=right}. Przy tych samych zawiasach albo zamienionych
     * oba skrzydła wchodzą na siebie na środku przejścia.
     * <p>
     * (x, y, z) to skrzydło o mniejszej współrzędnej (zachodnie przy drzwiach w ścianie wzdłuż x, północne
     * przy ścianie wzdłuż z); drugie stoi obok, o jeden blok dalej.
     */
    static void doubleDoor(DoorSetter setter, int x, int y, int z, String type, String facing, boolean open) {
        boolean alongX = facing.equals("north") || facing.equals("south");
        int x2 = alongX ? x + 1 : x;
        int z2 = alongX ? z : z + 1;
        // lewa ręka patrzącego na północ to zachód (mniejsze x), na wschód – północ (mniejsze z);
        // na południe i zachód odwrotnie
        boolean firstIsLeft = facing.equals("north") || facing.equals("east");
        setter.door(x, y, z, type, facing, firstIsLeft ? "left" : "right", open);
        setter.door(x2, y, z2, type, facing, firstIsLeft ? "right" : "left", open);
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

    // ------------------------------------------------------------------ ślady życia

    /**
     * Miejsce zwierzęcia ({@code punkt-zwierze-<typ>}): plugin trzyma tu zwierzę. Znacznik leży w pustej kratce
     * nad podłogą (blok punktu zostaje, więc kratka musi być powietrzem) – trawa w niej i nad nią jest usuwana.
     */
    void animal(int x, int y, int z, String type) {
        for (int dy = 0; dy <= 1; dy++) {
            String s = bp.get(x, y + dy + yOffset, z);
            if (s.contains("grass") && !s.contains("grass_block") || s.contains("fern") || s.contains("moss_carpet")) {
                set(x, y + dy, z, Blueprint.AIR);
            }
        }
        bp.crate(x, y + yOffset, z, "south", "punkt-zwierze-" + type);
    }

    /** Zwierzę w losowej wolnej kratce prostokąta (np. bezpański kot w ruinach). Zwraca, czy się udało. */
    boolean animalSomewhere(int x1, int z1, int x2, int z2, int y, String type) {
        for (int k = 0; k < 200; k++) {
            int x = x1 + rnd.nextInt(x2 - x1 + 1);
            int z = z1 + rnd.nextInt(z2 - z1 + 1);
            if (freeFloor(x, y, z)) {
                animal(x, y, z, type);
                return true;
            }
        }
        return false;
    }

    private boolean isMarker(int x, int y, int z) {
        Blueprint.Pos pos = new Blueprint.Pos(x, y + yOffset, z);
        return bp.spawnPoints().stream().anyMatch(p -> p.pos().equals(pos));
    }

    /** Pusta kratka (i pusto nad nią) na pełnym bloku, bez skrzyni ani znacznika. */
    private boolean freeFloor(int x, int y, int z) {
        int ay = y + yOffset;
        return bp.inBounds(x, ay, z) && bp.isAir(x, ay, z) && bp.isAir(x, ay + 1, z)
                && Weathering.isFullBlock(bp.get(x, ay - 1, z)) && !isMarker(x, y, z) && !isMarker(x, y - 1, z);
    }

    /**
     * Ślady po ludziach na podłodze w prostokącie: zaschnięta krew (kropki redstone) i porozrzucane kartki
     * (biały i szary dywan). Nie blokują przejścia; trafiają tylko na wolną podłogę.
     */
    void traces(int x1, int z1, int x2, int z2, int y, int blood, int papers) {
        for (int k = 0; k < (blood + papers) * 10 && blood + papers > 0; k++) {
            int x = x1 + rnd.nextInt(x2 - x1 + 1);
            int z = z1 + rnd.nextInt(z2 - z1 + 1);
            if (!freeFloor(x, y, z)) {
                continue;
            }
            if (blood > 0 && (papers == 0 || rnd.nextBoolean())) {
                set(x, y, z, "redstone_wire[east=none,north=none,power=0,south=none,west=none]");
                blood--;
            } else {
                set(x, y, z, rnd.nextDouble() < 0.75 ? "white_carpet" : "light_gray_carpet");
                papers--;
            }
        }
    }

    /**
     * Napis ocalałych na ścianie: tabliczka na pierwszej wolnej ścianie w prostokącie (losowo szukanej),
     * czytelna od strony pustej kratki. Zwraca, czy się udało.
     */
    boolean graffiti(int x1, int z1, int x2, int z2, int y, String... lines) {
        String[] faces = {"north", "south", "east", "west"};
        for (int k = 0; k < 200; k++) {
            int x = x1 + rnd.nextInt(x2 - x1 + 1);
            int z = z1 + rnd.nextInt(z2 - z1 + 1);
            int ay = y + yOffset;
            if (!bp.inBounds(x, ay, z) || !bp.isAir(x, ay, z) || isMarker(x, y, z) || isMarker(x, y - 1, z)) {
                continue;
            }
            String face = faces[rnd.nextInt(4)];
            int bx = x - dx(face);
            int bz = z - dz(face);
            int fx = x + dx(face);
            int fz = z + dz(face);
            if (Weathering.isFullBlock(bp.get(bx, ay, bz)) && bp.isAir(fx, ay, fz)) {
                String[] padded = new String[4];
                for (int i = 0; i < 4; i++) {
                    padded[i] = i < lines.length ? lines[i] : "";
                }
                bp.sign(x, ay, z, "dark_oak_wall_sign[facing=" + face + ",waterlogged=false]", padded);
                return true;
            }
        }
        return false;
    }

    /**
     * Zawalony namiot na wolnym kawałku podłogi w prostokącie: płachta (dywany) z jednym zwałem, złamany
     * maszt i zgaszone ognisko obok, jeśli jest miejsce. Zwraca, czy się udało.
     */
    boolean collapsedTent(int x1, int z1, int x2, int z2, int y, String color) {
        for (int k = 0; k < 200; k++) {
            boolean alongX = rnd.nextBoolean();
            int w = alongX ? 4 : 3;
            int d = alongX ? 3 : 4;
            int x0 = x1 + rnd.nextInt(Math.max(1, x2 - x1 - w + 2));
            int z0 = z1 + rnd.nextInt(Math.max(1, z2 - z1 - d + 2));
            boolean ok = x0 + w - 1 <= x2 && z0 + d - 1 <= z2;
            for (int x = x0; ok && x < x0 + w; x++) {
                for (int z = z0; ok && z < z0 + d; z++) {
                    ok = freeFloor(x, y, z);
                }
            }
            if (!ok) {
                continue;
            }
            for (int x = x0; x < x0 + w; x++) {
                for (int z = z0; z < z0 + d; z++) {
                    if (rnd.nextDouble() < 0.85) {
                        set(x, y, z, color + "_carpet");
                    }
                }
            }
            set(x0 + 1, y, z0 + 1, color + "_wool");
            set(x0 + w - 1, y, z0 + d - 1, "lightning_rod[facing=" + (alongX ? "east" : "south") + ",powered=false,waterlogged=false]");
            int cx = alongX ? x0 + w : x0 + 1;
            int cz = alongX ? z0 + 1 : z0 + d;
            if (cx <= x2 && cz <= z2 && freeFloor(cx, y, cz)) {
                set(cx, y, cz, "campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]");
            }
            return true;
        }
        return false;
    }
}
