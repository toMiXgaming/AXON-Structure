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
}
