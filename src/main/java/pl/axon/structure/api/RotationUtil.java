package pl.axon.structure.api;

import pl.axon.structure.core.Placement;
import pl.axon.structure.core.Rotation;

/**
 * Obrót struktur o wielokrotność 90° zgodnie z ruchem wskazówek zegara (patrząc z góry).
 * <p>
 * Działa tak samo jak wklejanie AXON-Structure: współrzędne i stany bloków obracane są tymi samymi funkcjami
 * co w {@code PasteService}. Liczba obrotów spoza zakresu 0–3 jest sprowadzana modulo 4.
 */
public final class RotationUtil {

    private RotationUtil() {
    }

    /** Sprowadza liczbę obrotów do zakresu 0–3. */
    public static int normalize(int steps) {
        return Math.floorMod(steps, 4);
    }

    /** Obraca wektor (x, z) o {@code steps} × 90°. */
    public static int[] rotate(int x, int z, int steps) {
        return Rotation.rotate(x, z, normalize(steps));
    }

    /** Obraca kierunek ({@code north}, {@code east}, …) o {@code steps} × 90°. */
    public static String rotateFacing(String facing, int steps) {
        return Rotation.rotateFacing(facing, normalize(steps));
    }

    /** Obraca właściwości stanu bloku zależne od kierunku ({@code facing}, {@code axis}, płoty, szyby, tablice). */
    public static String rotateState(String state, int steps) {
        return Rotation.rotateState(state, normalize(steps));
    }

    /**
     * Zamienia lokalne współrzędne szablonu na światowe dla punktu (originX, originY, originZ) i obrotu.
     * Warstwa y = 0 szablonu leży na {@code originY}.
     */
    public static int[] toWorld(int originX, int originY, int originZ, int steps, int lx, int ly, int lz) {
        return new Placement("", originX, originY, originZ, normalize(steps)).toWorld(lx, ly, lz);
    }
}
