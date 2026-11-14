package pl.axon.structure.vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Czysta geometria wraków: bariery, hitboxy bagażnika i obrót.
 *
 * <p>Bez Bukkita — wszystko liczy się na intach i double, więc testy jednostkowe pokrywają
 * całą logikę ustawiania wraków bez serwera.
 *
 * <p>Układ współrzędnych: wrak stoi „wzdłuż” osi Z przy obrocie 0 (przód na północ, jak modele
 * z {@code gen_axoncore.py}). {@code length} biegnie po Z, {@code width} po X. Obrót o 90°
 * zamienia osie. Punkt zaczepienia ({@code anchorX}, {@code anchorZ}) to blok, w którego środku stoi
 * środek modelu — wrak rozciąga się wokół niego symetrycznie.
 */
public final class VehicleMath {

    /** Ile warstw barier miała stara wersja (2 warstwy, obrys zaokrąglony w górę i poszerzony). */
    public static final int LEGACY_COLLISION_HEIGHT = 2;

    /** Hitbox bagażnika jest minimalnie większy od barier, żeby klik trafiał w niego, a nie w barierę. */
    public static final double HITBOX_MARGIN = 0.05;

    private VehicleMath() {
    }

    /**
     * Bloki kolizji (bariery) dla wraku. Bariera stoi w bloku, którego środek leży w obrysie modelu
     * ({@code length × width} obrócone o {@code steps} × 90°, wyśrodkowane na środku bloku zaczepienia),
     * w tylu warstwach nad gruntem, ile wynosi zaokrąglona wysokość modelu. Dzięki temu bariery nie
     * wystają poza wrak, a na niski wrak (Fiat, Polonez, WSK) da się wskoczyć.
     *
     * @param anchorX blok X środka modelu
     * @param groundY blok Y gruntu (bariery startują od {@code groundY + 1})
     * @param anchorZ blok Z środka modelu
     * @param steps   obrót 0–3 × 90°
     * @return lista bloków {@code [x, y, z]} do wypełnienia barierą
     */
    public static List<int[]> collisionBlocks(VehicleModel model, int anchorX, int groundY, int anchorZ, int steps) {
        int[] half = halfBlocks(model, steps);
        return box(anchorX, groundY, anchorZ, half[0], half[1], collisionLayers(model));
    }

    /**
     * Bariery ze starej wersji (obrys {@code ceil(wymiar) / 2} w każdą stronę, 2 warstwy). Były szersze
     * i dłuższe niż model; przy ponownym stawianiu wraku sprzątamy te, które wypadają poza nowy obrys.
     */
    public static List<int[]> legacyCollisionBlocks(VehicleModel model, int anchorX, int groundY, int anchorZ,
                                                    int steps) {
        int length = (int) Math.ceil(model.length());
        int width = (int) Math.ceil(model.width());
        int[] size = swapped(steps) ? new int[]{length, width} : new int[]{width, length};
        return box(anchorX, groundY, anchorZ, size[0] / 2, size[1] / 2, LEGACY_COLLISION_HEIGHT);
    }

    /** Liczba warstw barier: zaokrąglona wysokość modelu, co najmniej 1. */
    public static int collisionLayers(VehicleModel model) {
        return Math.max(1, (int) Math.round(model.height()));
    }

    /**
     * Ile bloków barier w każdą stronę od zaczepienia: {@code [poX, poZ]}. Blok {@code i} (jego środek
     * jest {@code i} kratek od środka wraku) dostaje barierę, gdy {@code |i| <= wymiar / 2}.
     */
    public static int[] halfBlocks(VehicleModel model, int steps) {
        int halfLength = (int) Math.floor(model.length() / 2.0 + 1e-9);
        int halfWidth = (int) Math.floor(model.width() / 2.0 + 1e-9);
        return swapped(steps) ? new int[]{halfLength, halfWidth} : new int[]{halfWidth, halfLength};
    }

    /** Rozmiar barier w blokach po obrocie: {@code [rozmiarX, rozmiarZ]}. */
    public static int[] footprint(VehicleModel model, int steps) {
        int[] half = halfBlocks(model, steps);
        return new int[]{2 * half[0] + 1, 2 * half[1] + 1};
    }

    private static List<int[]> box(int anchorX, int groundY, int anchorZ, int halfX, int halfZ, int layers) {
        List<int[]> out = new ArrayList<>();
        for (int dy = 1; dy <= layers; dy++) {
            for (int dx = -halfX; dx <= halfX; dx++) {
                for (int dz = -halfZ; dz <= halfZ; dz++) {
                    out.add(new int[]{anchorX + dx, groundY + dy, anchorZ + dz});
                }
            }
        }
        return out;
    }

    private static boolean swapped(int steps) {
        return Math.floorMod(steps, 4) % 2 == 1;
    }

    /**
     * Hitboxy bagażnika ({@code Interaction}) pokrywające cały wrak. {@code Interaction} ma kwadratową
     * podstawę, więc długi wrak dostaje kilka kwadratów wzdłuż długości. Każdy obejmuje i model, i bariery
     * (z małym zapasem), żeby klik w dowolne miejsce wraku otwierał bagażnik, a nie trafiał w barierę.
     *
     * @return lista {@code [offsetWzdłuż, bok, wysokość]} w kratkach; offset liczony od środka wraku
     *         wzdłuż jego długości (do obrócenia przez {@link #rotateOffset})
     */
    public static List<double[]> hitboxes(VehicleModel model) {
        int[] size = footprint(model, 0);
        double side = Math.max(model.width(), size[0]) + 2 * HITBOX_MARGIN;
        double length = Math.max(model.length(), size[1]) + 2 * HITBOX_MARGIN;
        double height = Math.max(model.height(), collisionLayers(model)) + HITBOX_MARGIN;
        if (length <= side) {
            return List.of(new double[]{0.0, side, height});
        }
        int count = (int) Math.ceil(length / side - 1e-9);
        double span = length - side;
        List<double[]> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(new double[]{-span / 2.0 + span * i / (count - 1), side, height});
        }
        return out;
    }

    /**
     * Zamienia przesunięcie „wzdłuż/wszerz” wraku na przesunięcie w świecie po obrocie.
     *
     * @param along offset wzdłuż długości (oś Z przy obrocie 0)
     * @param across offset w poprzek (oś X przy obrocie 0)
     * @return {@code [dx, dz]} w świecie
     */
    public static double[] rotateOffset(double along, double across, int steps) {
        return switch (Math.floorMod(steps, 4)) {
            case 1 -> new double[]{along, -across};   // 90°: długość na X
            case 2 -> new double[]{-across, -along};  // 180°
            case 3 -> new double[]{-along, across};   // 270°
            default -> new double[]{across, along};   // 0°
        };
    }

    /**
     * Czy wrak zmieści się w kawałku terenu: sprawdza, czy obrys nie wychodzi poza podany
     * prostokąt (np. pobocze drogi). Używane przy wyborze miejsca, żeby wrak nie wchodził w jezdnię.
     *
     * @param minX najmniejszy dozwolony blok X
     * @param maxX największy dozwolony blok X
     * @param minZ najmniejszy dozwolony blok Z
     * @param maxZ największy dozwolony blok Z
     */
    public static boolean fits(VehicleModel model, int anchorX, int anchorZ, int steps,
                               int minX, int maxX, int minZ, int maxZ) {
        int[] size = footprint(model, steps);
        int halfX = size[0] / 2;
        int halfZ = size[1] / 2;
        return anchorX - halfX >= minX && anchorX + halfX <= maxX
                && anchorZ - halfZ >= minZ && anchorZ + halfZ <= maxZ;
    }

    /**
     * Największy promień obrysu wraku (kratki) — do sprawdzania odległości od innych obiektów
     * i do czyszczenia starych encji wokół punktu zaczepienia.
     */
    public static double radius(VehicleModel model) {
        return Math.hypot(model.length(), model.width()) / 2.0;
    }
}
