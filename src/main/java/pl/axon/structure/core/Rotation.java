package pl.axon.structure.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Obrót struktury o wielokrotność 90° zgodnie z ruchem wskazówek zegara (patrząc z góry). */
public final class Rotation {

    private static final String[] FACINGS = {"north", "east", "south", "west"};
    private static final Pattern FACING = Pattern.compile("facing=(north|east|south|west)");
    private static final Pattern AXIS = Pattern.compile("axis=([xz])");
    private static final Pattern SIDE_KEY = Pattern.compile("(?<=[\\[,])(north|east|south|west)=");
    private static final Pattern RAIL_SHAPE = Pattern.compile(
            "shape=(north_south|east_west|ascending_(?:north|east|south|west)|(?:north|south)_(?:east|west))");
    private static final Pattern SIGN_ROTATION = Pattern.compile("rotation=(\\d+)");

    private Rotation() {
    }

    /**
     * Liczba obrotów tak, by lokalna oś +z (w głąb struktury) pokrywała się z kierunkiem patrzenia gracza.
     * Yaw 0 = południe, 90 = zachód, 180 = północ, 270 = wschód.
     */
    public static int stepsFromYaw(float yaw) {
        float normalized = ((yaw % 360f) + 360f) % 360f;
        return Math.round(normalized / 90f) % 4;
    }

    /** Obraca wektor (x, z) o {@code steps} * 90°. */
    public static int[] rotate(int x, int z, int steps) {
        int rx = x;
        int rz = z;
        for (int i = 0; i < steps; i++) {
            int tmp = rx;
            rx = -rz;
            rz = tmp;
        }
        return new int[]{rx, rz};
    }

    public static String rotateFacing(String facing, int steps) {
        for (int i = 0; i < FACINGS.length; i++) {
            if (FACINGS[i].equals(facing)) {
                return FACINGS[(i + steps) % 4];
            }
        }
        return facing;
    }

    /** Obraca właściwości stanu bloku zależne od kierunku (facing, axis, rotation). */
    public static String rotateState(String state, int steps) {
        if (steps == 0 || state.indexOf('[') < 0) {
            return state;
        }
        String result = replace(FACING, state, m -> "facing=" + rotateFacing(m.group(1), steps));
        if (steps % 2 == 1) {
            result = replace(AXIS, result, m -> "axis=" + (m.group(1).equals("x") ? "z" : "x"));
        }
        result = replace(SIDE_KEY, result, m -> rotateFacing(m.group(1), steps) + "=");
        result = replace(RAIL_SHAPE, result, m -> "shape=" + rotateRailShape(m.group(1), steps));
        result = replace(SIGN_ROTATION, result,
                m -> "rotation=" + ((Integer.parseInt(m.group(1)) + 4 * steps) % 16));
        return result;
    }

    /** Kształt torów: proste zamieniają oś, wznoszące i zakręty obracają kierunki (zakręt zawsze north/south_east/west). */
    static String rotateRailShape(String shape, int steps) {
        if (shape.equals("north_south") || shape.equals("east_west")) {
            return steps % 2 == 0 ? shape : shape.equals("north_south") ? "east_west" : "north_south";
        }
        if (shape.startsWith("ascending_")) {
            return "ascending_" + rotateFacing(shape.substring("ascending_".length()), steps);
        }
        String[] parts = shape.split("_");
        String a = rotateFacing(parts[0], steps);
        String b = rotateFacing(parts[1], steps);
        // zapis w Minecrafcie: najpierw north albo south
        return (a.equals("north") || a.equals("south")) ? a + "_" + b : b + "_" + a;
    }

    private static String replace(Pattern pattern, String input, java.util.function.Function<Matcher, String> fn) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(fn.apply(matcher)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
