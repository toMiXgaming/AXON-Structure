package pl.axon.structure.loot;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy czystej matematyki zrzutów: losowanie punktu w pierścieniu i sprawdzanie zasięgu.
 * Bez Bukkita – wszystko liczy się w milisekundach.
 */
class DropMathTest {

    @Test
    void punktZawszeWewnatrzPierscienia() {
        Random random = new Random(1234L);
        for (int i = 0; i < 5000; i++) {
            int[] point = DropMath.pick(random, 0, 0, 120, 500);
            double distance = Math.hypot(point[0], point[1]);
            assertTrue(distance >= 119.0, "punkt za blisko środka: " + distance);
            assertTrue(distance <= 501.0, "punkt za daleko: " + distance);
        }
    }

    @Test
    void punktJestPrzesunietyWzgledemSrodka() {
        Random random = new Random(99L);
        int[] point = DropMath.pick(random, 1000, -2000, 50, 60);
        assertTrue(Math.abs(point[0] - 1000) <= 61);
        assertTrue(Math.abs(point[1] + 2000) <= 61);
    }

    @Test
    void zerowyPromienDajeDokladnieSrodek() {
        Random random = new Random(7L);
        for (int i = 0; i < 100; i++) {
            int[] point = DropMath.pick(random, 42, -42, 0, 0);
            assertEquals(42, point[0]);
            assertEquals(-42, point[1]);
        }
    }

    @Test
    void minWiekszyOdMaxJestZamieniany() {
        Random random = new Random(5L);
        for (int i = 0; i < 1000; i++) {
            int[] point = DropMath.pick(random, 0, 0, 300, 100);
            double distance = Math.hypot(point[0], point[1]);
            assertTrue(distance >= 99.0, "punkt za blisko środka: " + distance);
            assertTrue(distance <= 301.0, "punkt za daleko: " + distance);
        }
    }

    @Test
    void ujemnePromienieSaPrzycinaneDoZera() {
        Random random = new Random(11L);
        int[] point = DropMath.pick(random, 5, 5, -50, -10);
        assertEquals(5, point[0]);
        assertEquals(5, point[1]);
    }

    @Test
    void rozkladNieSkupiaSieWSrodku() {
        Random random = new Random(2024L);
        int inner = 0;
        int outer = 0;
        for (int i = 0; i < 20000; i++) {
            int[] point = DropMath.pick(random, 0, 0, 0, 100);
            double distance = Math.hypot(point[0], point[1]);
            if (distance < 50) {
                inner++;
            } else {
                outer++;
            }
        }
        // Przy równomiernym pokryciu koła połowa pól leży w promieniu 70.7, więc
        // wewnętrzny krąg r=50 to ~25% próbek. Dopuszczamy szeroki margines.
        double share = inner / (double) (inner + outer);
        assertTrue(share > 0.18 && share < 0.33, "nieoczekiwany rozkład: " + share);
    }

    @Test
    void withinObejmujeBrzeg() {
        assertTrue(DropMath.within(10, 0, 0, 0, 10));
        assertTrue(DropMath.within(0, 0, 0, 0, 10));
        assertTrue(DropMath.within(-10, 0, 0, 0, 10));
    }

    @Test
    void withinOdrzucaPunktyPozaZasiegiem() {
        assertFalse(DropMath.within(11, 0, 0, 0, 10));
        assertFalse(DropMath.within(0, 0, 100, 100, 10));
    }

    @Test
    void withinNiePrzepelniaSieNaDuzeWspolrzedne() {
        // Kwadrat 2^31 przekracza int – test pilnuje, że liczymy na longach.
        assertTrue(DropMath.within(Integer.MAX_VALUE, 0, Integer.MAX_VALUE - 5, 0, 5));
        assertFalse(DropMath.within(Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 0, 5));
    }
}
