package pl.axon.structure.loot;

import java.util.Random;

/**
 * Czysta matematyka zrzutów zaopatrzenia – bez Bukkita, żeby dała się testować w milisekundach.
 *
 * <p>Zrzut ląduje w losowym punkcie w promieniu od środka (np. od gracza albo od spawnu),
 * ale nigdy bliżej niż {@code minRadius} – inaczej spadałby na głowę temu, kto go wzywa.
 * Kąt jest losowy, promień losowany z pierwiastkiem kwadratowym, żeby punkty rozkładały się
 * równomiernie na kole, a nie gęściej przy środku.
 */
public final class DropMath {

    private DropMath() {
    }

    /**
     * Losuje punkt na kole.
     *
     * @param random    źródło losowości
     * @param centerX   środek X
     * @param centerZ   środek Z
     * @param minRadius najmniejszy promień (w blokach, włącznie)
     * @param maxRadius największy promień (w blokach, włącznie)
     * @return dwuelementowa tablica {@code {x, z}}
     */
    public static int[] pick(Random random, int centerX, int centerZ, int minRadius, int maxRadius) {
        int min = Math.max(0, Math.min(minRadius, maxRadius));
        int max = Math.max(min, maxRadius);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius;
        if (max == min) {
            radius = min;
        } else {
            // sqrt daje równomierne pokrycie koła zamiast skupiska w środku.
            double t = Math.sqrt(random.nextDouble());
            radius = min + t * (max - min);
        }
        int x = centerX + (int) Math.round(Math.cos(angle) * radius);
        int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
        return new int[]{x, z};
    }

    /**
     * Czy punkt mieści się w kole o podanym promieniu (do walidacji po losowaniu).
     */
    public static boolean within(int x, int z, int centerX, int centerZ, int radius) {
        long dx = Math.abs((long) x - centerX);
        long dz = Math.abs((long) z - centerZ);
        long r = Math.max(0, radius);
        // Najpierw porównujemy po osiach – to odsiewa punkty bez ryzyka przepełnienia
        // kwadratu odległości (dx*dx potrafi przekroczyć long dla skrajnych współrzędnych).
        if (dx > r || dz > r) {
            return false;
        }
        return dx * dx + dz * dz <= r * r;
    }
}
