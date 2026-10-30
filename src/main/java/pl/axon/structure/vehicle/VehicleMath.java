package pl.axon.structure.vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Czysta geometria wraków: obrys kolizji, podział na części i obrót.
 *
 * <p>Bez Bukkita — wszystko liczy się na intach i double, więc testy jednostkowe pokrywają
 * całą logikę ustawiania wraków bez serwera.
 *
 * <p>Układ współrzędnych: wrak stoi „wzdłuż” osi Z przy obrocie 0 (przód na północ, jak modele
 * z {@code gen_axoncore.py}). {@code length} biegnie po Z, {@code width} po X. Obrót o 90°
 * zamienia osie. Punkt zaczepienia ({@code anchorX}, {@code anchorZ}) to blok, w którym stoi
 * środek modelu — wrak rozciąga się wokół niego symetrycznie.
 */
public final class VehicleMath {

    /** Ile bloków nad gruntem ma górna warstwa barier (dach wraku). */
    public static final int COLLISION_HEIGHT = 2;

    private VehicleMath() {
    }

    /**
     * Bloki kolizji (bariery) dla wraku. Wrak zajmuje prostokąt {@code length × width} obrócony
     * o {@code steps} × 90°, wyśrodkowany na punkcie zaczepienia. Bariery stawiamy w dwóch
     * warstwach nad gruntem, żeby gracz nie wszedł w środek modelu, ale mógł na niego wskoczyć.
     *
     * @param anchorX blok X środka modelu
     * @param groundY blok Y gruntu (bariery startują od {@code groundY + 1})
     * @param anchorZ blok Z środka modelu
     * @param steps   obrót 0–3 × 90°
     * @return lista bloków {@code [x, y, z]} do wypełnienia barierą
     */
    public static List<int[]> collisionBlocks(VehicleModel model, int anchorX, int groundY, int anchorZ, int steps) {
        List<int[]> out = new ArrayList<>();
        int[] size = footprint(model, steps);
        int halfX = size[0] / 2;
        int halfZ = size[1] / 2;
        for (int dy = 1; dy <= COLLISION_HEIGHT; dy++) {
            for (int dx = -halfX; dx <= halfX; dx++) {
                for (int dz = -halfZ; dz <= halfZ; dz++) {
                    out.add(new int[]{anchorX + dx, groundY + dy, anchorZ + dz});
                }
            }
        }
        return out;
    }

    /**
     * Rozmiar obrysu w kratkach po obrocie: {@code [rozmiarX, rozmiarZ]}.
     * Przy obrocie 0/180 długość biegnie po Z, przy 90/270 — po X.
     */
    public static int[] footprint(VehicleModel model, int steps) {
        int length = (int) Math.ceil(model.length());
        int width = (int) Math.ceil(model.width());
        boolean swapped = Math.floorMod(steps, 4) % 2 == 1;
        return swapped ? new int[]{length, width} : new int[]{width, length};
    }

    /**
     * Przesunięcia środków części wraku względem punktu zaczepienia (w kratkach).
     *
     * <p>Duże wraki (Star, Autosan, Polonez, Żuk, Nysa) są dzielone na 2–3 {@code ItemDisplay},
     * żeby każdy element miał własny, mniejszy hitbox i żeby model nie znikał w całości przy
     * jednym uszkodzonym encie. Części leżą wzdłuż długości wraku.
     *
     * @return lista {@code [offsetAlongLength, offsetAcrossWidth]} w kratkach
     */
    public static List<double[]> partOffsets(VehicleModel model) {
        int parts = model.parts();
        List<double[]> out = new ArrayList<>(parts);
        if (parts <= 1) {
            out.add(new double[]{0.0, 0.0});
            return out;
        }
        double span = model.length();
        double step = span / parts;
        for (int i = 0; i < parts; i++) {
            // Środek i-tej części: od -span/2 + step/2 do +span/2 - step/2.
            double along = -span / 2.0 + step * (i + 0.5);
            out.add(new double[]{along, 0.0});
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
