package pl.axon.structure.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Testy czystej matematyki blokady skrzyni (etap 16 #6).
 *
 * <p>Sprawdzamy wyłącznie logikę czasu – bez Bukkita, bez encji. Dzięki temu
 * odliczanie nad modelem i decyzja „otworzyć czy nie” są w pełni pokryte testami.
 */
class CrateLockMathTest {

    private static final long NOW = 1_700_000_000_000L;

    @Test
    void brakBlokadyDajeUnlocked() {
        assertEquals(CrateLockMath.UNLOCKED, CrateLockMath.unlockAt(NOW, 0));
        assertEquals(CrateLockMath.UNLOCKED, CrateLockMath.unlockAt(NOW, -5));
    }

    @Test
    void blokadaUstawiaTerminWMilisekundach() {
        long at = CrateLockMath.unlockAt(NOW, 10);
        assertEquals(NOW + 10 * 60_000L, at);
    }

    @Test
    void skrzyniaJestZablokowanaPrzedTerminem() {
        long at = CrateLockMath.unlockAt(NOW, 10);
        assertTrue(CrateLockMath.locked(at, NOW));
        assertTrue(CrateLockMath.locked(at, at - 1));
        assertFalse(CrateLockMath.locked(at, at));
        assertFalse(CrateLockMath.locked(at, at + 1));
    }

    @Test
    void unlockedNigdyNieJestZablokowany() {
        assertFalse(CrateLockMath.locked(CrateLockMath.UNLOCKED, NOW));
        assertFalse(CrateLockMath.locked(CrateLockMath.UNLOCKED, 0L));
    }

    @Test
    void pozostalyCzasNieSchodziPonizejZera() {
        long at = CrateLockMath.unlockAt(NOW, 5);
        assertEquals(5 * 60_000L, CrateLockMath.remainingMillis(at, NOW));
        assertEquals(0L, CrateLockMath.remainingMillis(at, at));
        assertEquals(0L, CrateLockMath.remainingMillis(at, at + 60_000L));
    }

    @Test
    void odliczaniePokazujeMinutyISekundy() {
        assertEquals("0:00", CrateLockMath.countdown(0L));
        assertEquals("0:01", CrateLockMath.countdown(1L));
        assertEquals("0:59", CrateLockMath.countdown(59_000L));
        assertEquals("1:00", CrateLockMath.countdown(60_000L));
        assertEquals("9:59", CrateLockMath.countdown(599_000L));
        assertEquals("10:00", CrateLockMath.countdown(600_000L));
    }

    @Test
    void odliczanieZaokraglaWSekundachWGore() {
        // 1 ms do końca to jeszcze „0:01”, a nie „0:00” – gracz nie widzi zera przed czasem.
        assertEquals("0:01", CrateLockMath.countdown(1L));
        assertEquals("0:01", CrateLockMath.countdown(999L));
        assertEquals("0:02", CrateLockMath.countdown(1_001L));
    }

    @Test
    void odliczaniePokazujeGodzinyDlaDlugichBlokad() {
        assertEquals("1:00:00", CrateLockMath.countdown(3_600_000L));
        assertEquals("2:30:05", CrateLockMath.countdown(9_005_000L));
    }

    @Test
    void liniaNadModelemZnikaPoOdlokowaniu() {
        long at = CrateLockMath.unlockAt(NOW, 10);
        assertNull(CrateLockMath.displayLine(at, at));
        assertNull(CrateLockMath.displayLine(at, at + 1));
        assertNull(CrateLockMath.displayLine(CrateLockMath.UNLOCKED, NOW));
    }

    @Test
    void liniaNadModelemZawieraOdliczanie() {
        long at = CrateLockMath.unlockAt(NOW, 10);
        String line = CrateLockMath.displayLine(at, NOW);
        assertEquals("&c&lZABLOKOWANA &710:00", line);
    }

    @Test
    void liniaNadModelemOdswiezaSieZczasem() {
        long at = CrateLockMath.unlockAt(NOW, 10);
        assertEquals("&c&lZABLOKOWANA &79:59", CrateLockMath.displayLine(at, NOW + 1_000L));
        assertEquals("&c&lZABLOKOWANA &70:01", CrateLockMath.displayLine(at, at - 1_000L));
    }

    @Test
    void justUnlockedTylkoRazNaPrzejscie() {
        long at = CrateLockMath.unlockAt(NOW, 10);
        // jeszcze przed terminem – nie
        assertFalse(CrateLockMath.justUnlocked(at, NOW, NOW + 1_000L));
        // dokładnie w terminie – tak
        assertTrue(CrateLockMath.justUnlocked(at, at - 1_000L, at));
        // po terminie, ale poprzedni tick był już po – nie (nie spamujemy ogłoszeń)
        assertFalse(CrateLockMath.justUnlocked(at, at + 1_000L, at + 2_000L));
        // brak blokady – nigdy
        assertFalse(CrateLockMath.justUnlocked(CrateLockMath.UNLOCKED, NOW, NOW + 1_000L));
    }
}
