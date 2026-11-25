package pl.axon.structure.api;

/**
 * Punkt na pojemnik przy drodze (beczka) do {@link AxonStructureApi#setRoadsideContainers}. Wysokość liczy
 * AXON-Structure przy stawianiu: pojemnik staje na terenie.
 *
 * @param x   blok x
 * @param z   blok z
 * @param yaw kierunek, w który patrzy przód pojemnika, w stopniach jak w Minecrafcie (0 = południe, 90 = zachód);
 *            zwykle w stronę drogi
 */
public record RoadsideSpot(int x, int z, float yaw) {

    /**
     * Obrót „wzdłuż drogi” w krokach po 90° (0–3) dla wraków: przód pojemnika patrzy na drogę,
     * więc droga biegnie prostopadle (yaw + 90°).
     */
    public int steps() {
        return Math.floorMod(Math.round((yaw + 90f) / 90f), 4);
    }
}
