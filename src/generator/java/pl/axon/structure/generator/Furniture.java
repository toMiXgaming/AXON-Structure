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
     * Ślady po ludziach na podłodze w prostokącie, ułożone w sceny, które od razu wiadomo, co znaczą:
     * {@code blood} to liczba plam krwi (smuga zaschniętej krwi ciągnąca się po podłodze, czasem kończąca się
     * czaszką), {@code papers} to liczba porzuconych stosów papierów (pulpit z otwartą księgą, wybebeszona
     * beczka albo teczka, a wokół rozrzucone kartki). Trafiają tylko na wolną podłogę i nie blokują przejść.
     */
    void traces(int x1, int z1, int x2, int z2, int y, int blood, int papers) {
        int bloodScenes = (blood + 1) / 2;
        int paperScenes = (papers + 1) / 2;
        for (int k = 0; k < 300 && bloodScenes + paperScenes > 0; k++) {
            int x = x1 + rnd.nextInt(x2 - x1 + 1);
            int z = z1 + rnd.nextInt(z2 - z1 + 1);
            if (!freeFloor(x, y, z)) {
                continue;
            }
            if (bloodScenes > 0 && (paperScenes == 0 || rnd.nextBoolean())) {
                if (bloodTrail(x, y, z, x1, z1, x2, z2)) {
                    bloodScenes--;
                }
            } else if (paperPile(x, y, z, x1, z1, x2, z2)) {
                paperScenes--;
            }
        }
    }

    private boolean inRect(int x, int z, int x1, int z1, int x2, int z2) {
        return x >= x1 && x <= x2 && z >= z1 && z <= z2;
    }

    /** Smuga krwi: 4–7 połączonych kratek zaschniętej krwi (jak ciągnięte ciało), na końcu kałuża albo czaszka. */
    private boolean bloodTrail(int x, int y, int z, int x1, int z1, int x2, int z2) {
        String[] dirs = {"north", "south", "east", "west"};
        String dir = dirs[rnd.nextInt(4)];
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        cells.add(new int[]{x, z});
        int cx = x;
        int cz = z;
        int len = 4 + rnd.nextInt(4);
        for (int i = 1; i < len; i++) {
            String d = rnd.nextDouble() < 0.7 ? dir : dirs[rnd.nextInt(4)];
            int nx = cx + dx(d);
            int nz = cz + dz(d);
            boolean taken = false;
            for (int[] c : cells) {
                taken |= c[0] == nx && c[1] == nz;
            }
            if (taken || !inRect(nx, nz, x1, z1, x2, z2) || !freeFloor(nx, y, nz)) {
                continue;
            }
            cells.add(new int[]{nx, nz});
            cx = nx;
            cz = nz;
        }
        if (cells.size() < 3) {
            return false;
        }
        // Kałuża na początku smugi: dodatkowe kratki wokół pierwszej.
        int[] first = cells.get(0);
        for (String d : dirs) {
            int nx = first[0] + dx(d);
            int nz = first[1] + dz(d);
            if (rnd.nextBoolean() && inRect(nx, nz, x1, z1, x2, z2) && freeFloor(nx, y, nz)
                    && cells.stream().noneMatch(c -> c[0] == nx && c[1] == nz)) {
                cells.add(new int[]{nx, nz});
            }
        }
        for (int[] c : cells) {
            StringBuilder state = new StringBuilder("redstone_wire[");
            for (String d : new String[]{"east", "north", "south", "west"}) {
                int nx = c[0] + dx(d);
                int nz = c[1] + dz(d);
                boolean link = cells.stream().anyMatch(o -> o[0] == nx && o[1] == nz);
                state.append(d).append('=').append(link ? "side" : "none").append(',');
                if (d.equals("north")) {
                    state.append("power=0,");
                }
            }
            state.setLength(state.length() - 1);
            set(c[0], y, c[1], state.append(']').toString());
        }
        // Na końcu smugi: czaszka (tu skończył) albo nic (ktoś go zabrał).
        if (rnd.nextDouble() < 0.5) {
            int nx = cx + dx(dir);
            int nz = cz + dz(dir);
            if (inRect(nx, nz, x1, z1, x2, z2) && freeFloor(nx, y, nz)) {
                set(nx, y, nz, "skeleton_skull[powered=false,rotation=" + rnd.nextInt(16) + "]");
            }
        }
        return true;
    }

    /** Porzucone papiery: pulpit z otwartą księgą, wywrócona beczka albo teczka, a wokół 3–6 rozrzuconych kartek. */
    private boolean paperPile(int x, int y, int z, int x1, int z1, int x2, int z2) {
        String[] faces = {"north", "south", "east", "west"};
        String face = faces[rnd.nextInt(4)];
        double r = rnd.nextDouble();
        if (r < 0.45) {
            set(x, y, z, "lectern[facing=" + face + ",has_book=true,powered=false]");
        } else if (r < 0.75) {
            set(x, y, z, "barrel[facing=" + face + ",open=true]");
        } else {
            set(x, y, z, "chiseled_bookshelf[facing=" + face + ",slot_0_occupied=true,slot_1_occupied=false,"
                    + "slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=false,slot_5_occupied=true]");
        }
        int sheets = 3 + rnd.nextInt(4);
        for (int k = 0; k < 30 && sheets > 0; k++) {
            int nx = x + rnd.nextInt(5) - 2;
            int nz = z + rnd.nextInt(5) - 2;
            if ((nx != x || nz != z) && inRect(nx, nz, x1, z1, x2, z2) && freeFloor(nx, y, nz)) {
                set(nx, y, nz, rnd.nextDouble() < 0.7 ? "white_carpet" : "light_gray_carpet");
                sheets--;
            }
        }
        return true;
    }

    private static boolean paintable(String state) {
        String n = state.replace("minecraft:", "");
        return Weathering.isFullBlock(state) && !(n.contains("ore") || n.contains("barrel") || n.contains("shulker")
                || n.contains("lamp") || n.contains("glowstone") || n.contains("tnt") || n.contains("spawner")
                || n.contains("furnace") || n.contains("smoker") || n.contains("table") || n.contains("bookshelf")
                || n.contains("dispenser") || n.contains("dropper") || n.contains("observer") || n.contains("piston")
                || n.contains("jukebox") || n.contains("note_block") || n.contains("beehive") || n.contains("crafter"));
    }

    /**
     * Napis ocalałych na ścianie: tabliczka z tekstem na wysokości oczu, a wokół niej na ścianie farba w sprayu
     * (kilka pomalowanych bloków ściany, z zaciekami w dół), żeby z daleka było widać, że ktoś tu coś napisał.
     * Ściana musi być pełna i grubsza niż jeden blok tam, gdzie leży farba, żeby nie przebiła na drugą stronę.
     * Zwraca, czy się udało.
     */
    boolean graffiti(int x1, int z1, int x2, int z2, int y, String... lines) {
        String[] faces = {"north", "south", "east", "west"};
        String[] paints = {"red_terracotta", "black_concrete", "red_concrete", "white_concrete"};
        // Najpierw szukamy ściany, na której zmieści się cały napis; jak nie ma, wystarczy sama tabliczka.
        for (int k = 0; k < 600; k++) {
            int minPaint = k < 400 ? 5 : 0;
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
            if (!Weathering.isFullBlock(bp.get(bx, ay, bz)) || !bp.isAir(fx, ay, fz)) {
                continue;
            }
            // Pas ściany 5 × 3 wokół tabliczki (wzdłuż ściany: u, w pionie: v).
            int ux = dz(face) != 0 ? 1 : 0;
            int uz = dx(face) != 0 ? 1 : 0;
            String paint = paints[rnd.nextInt(paints.length)];
            java.util.List<int[]> spray = new java.util.ArrayList<>();
            for (int u = -2; u <= 2; u++) {
                for (int v = -1; v <= 1; v++) {
                    boolean stroke = v == 1 || v == 0 && Math.abs(u) == 2 || v == -1 && rnd.nextDouble() < 0.35
                            || u == 0 && v == 0;
                    if (!stroke) {
                        continue;
                    }
                    int wx = bx + u * ux;
                    int wz = bz + u * uz;
                    int wy = ay + v;
                    int frontX = wx + dx(face);
                    int frontZ = wz + dz(face);
                    int backX = wx - dx(face);
                    int backZ = wz - dz(face);
                    if (bp.inBounds(wx, wy, wz) && paintable(bp.get(wx, wy, wz)) && bp.inBounds(frontX, wy, frontZ)
                            && bp.isAir(frontX, wy, frontZ) && bp.inBounds(backX, wy, backZ)
                            && !bp.isAir(backX, wy, backZ)) {
                        spray.add(new int[]{wx, wy, wz});
                    }
                }
            }
            if (spray.size() < minPaint) {
                continue;
            }
            for (int[] c : spray) {
                bp.set(c[0], c[1], c[2], paint);
            }
            String[] padded = new String[4];
            for (int i = 0; i < 4; i++) {
                padded[i] = i < lines.length ? lines[i] : "";
            }
            bp.sign(x, ay, z, "dark_oak_wall_sign[facing=" + face + ",waterlogged=false]", padded);
            return true;
        }
        return false;
    }

    /**
     * Na pół zawalony namiot 3 × 4 na wolnym kawałku podłogi: od wejścia jeszcze stoi (maszt z płótnem na górze,
     * płótno opada po bokach), dalej płachta sflaczała do ziemi, z tyłu złamany maszt. Obok zgaszone ognisko
     * z pieńkiem do siedzenia i porzucony plecak (beczka). Zwraca, czy się udało.
     */
    boolean collapsedTent(int x1, int z1, int x2, int z2, int y, String color) {
        for (int k = 0; k < 300; k++) {
            boolean alongX = rnd.nextBoolean();
            // Namiot leży wzdłuż osi a (4 kratki), w poprzek b (3 kratki); przed wejściem 2 kratki na ognisko.
            int w = alongX ? 6 : 3;
            int d = alongX ? 3 : 6;
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
            // (a, b) -> (x, z): a = 0..3 namiot od wejścia, a = 4..5 przedpole; b = 0..2, środek b = 1.
            java.util.function.BiFunction<Integer, Integer, int[]> at = (a, b) ->
                    alongX ? new int[]{x0 + a, z0 + b} : new int[]{x0 + b, z0 + a};
            String along = alongX ? "x" : "z";
            String side0 = alongX ? "north" : "west";
            String side2 = alongX ? "south" : "east";
            // Wejście: maszt (płot) i płótno na nim, po bokach płótno opadające do ziemi.
            int[] c = at.apply(3, 1);
            set(c[0], y, c[1], "oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]");
            set(c[0], y + 1, c[1], color + "_wool");
            for (int b : new int[]{0, 2}) {
                int[] s = at.apply(3, b);
                set(s[0], y, s[1], color + "_wool");
            }
            // Środek: płótno zapadnięte, ale jeszcze trzyma kształt (blok na środku, boki płasko).
            int[] m = at.apply(2, 1);
            set(m[0], y, m[1], color + "_wool");
            for (int b : new int[]{0, 2}) {
                int[] s = at.apply(2, b);
                set(s[0], y, s[1], color + "_carpet");
            }
            // Tył: płachta całkiem na ziemi, z dziurami, i złamany maszt leżący w poprzek.
            for (int a = 0; a <= 1; a++) {
                for (int b = 0; b <= 2; b++) {
                    int[] s = at.apply(a, b);
                    if (a == 0 && b == 1) {
                        set(s[0], y, s[1], "lightning_rod[facing=" + side2 + ",powered=false,waterlogged=false]");
                    } else if (rnd.nextDouble() < 0.85) {
                        set(s[0], y, s[1], color + "_carpet");
                    }
                }
            }
            // Przedpole: zgaszone ognisko, pieniek do siedzenia i porzucony plecak.
            int[] fire = at.apply(5, 1);
            set(fire[0], y, fire[1], "campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]");
            int[] log = at.apply(5, rnd.nextBoolean() ? 0 : 2);
            set(log[0], y, log[1], "stripped_spruce_log[axis=" + along + "]");
            int[] pack = at.apply(4, rnd.nextBoolean() ? 0 : 2);
            set(pack[0], y, pack[1], "barrel[facing=" + (rnd.nextBoolean() ? side0 : "up") + ",open=false]");
            return true;
        }
        return false;
    }
}
