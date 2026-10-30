package pl.axon.structure.api;

/**
 * Punkt pobocza drogi zgłoszony przez AxonMapGenerator.
 *
 * <p>AxonMapGenerator generuje mapę (drogi, rzeki, miasta) i wie, gdzie są pobocza, ale nie zna
 * modeli z paczki AxonCore. Dlatego po wygenerowaniu świata woła
 * {@link AxonStructureApi#setRoadsideContainers(String, java.util.List)} z listą takich punktów,
 * a AXON-Structure sam decyduje, co tam postawić (wrak, beczkę, nic).
 *
 * @param x   blok X punktu na poboczu
 * @param z   blok Z punktu na poboczu
 * @param yaw kierunek patrzenia „wzdłuż drogi” w stopniach (0 = północ, 90 = wschód)
 */
public record RoadsideSpot(int x, int z, float yaw) {

    /** Obrót w krokach po 90° (0–3) — wygodne dla {@code VehicleMath}. */
    public int steps() {
        int steps = Math.round(yaw / 90f);
        return Math.floorMod(steps, 4);
    }
}
