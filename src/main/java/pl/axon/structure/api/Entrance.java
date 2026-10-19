package pl.axon.structure.api;

/**
 * Wjazd do struktury: punkt na krawędzi szablonu, w który wpina się droga.
 *
 * @param x      współrzędna lokalna x (0 = lewa krawędź w układzie szablonu)
 * @param z      współrzędna lokalna z (0 = front struktury)
 * @param facing kierunek, w którym leży droga: {@code north}, {@code east}, {@code south} albo {@code west}
 */
public record Entrance(int x, int z, String facing) {
}
