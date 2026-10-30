package pl.axon.structure.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy czystej logiki kart dostępu (etap 16 #7).
 *
 * <p>Sprawdzamy trzy rzeczy, które decydują o balansie rozgrywki:
 * <ul>
 *   <li>poziomy kart (niebieska 1, czerwona 2, zielona 3),</li>
 *   <li>która karta otwiera jaki wymóg (karta nie może być słabsza niż zamek),</li>
 *   <li>łączenie tylko niebieska + czerwona (żadnego mnożenia kart).</li>
 * </ul>
 */
class AccessCardMathTest {

    // ------------------------------------------------------------------
    //  Poziomy
    // ------------------------------------------------------------------

    @Test
    void niebieskaMaPoziomPierwszy() {
        assertEquals(1, AccessCardMath.level(AccessCardMath.BLUE));
    }

    @Test
    void czerwonaMaPoziomDrugi() {
        assertEquals(2, AccessCardMath.level(AccessCardMath.RED));
    }

    @Test
    void zielonaMaPoziomTrzeci() {
        assertEquals(3, AccessCardMath.level(AccessCardMath.GREEN));
    }

    @Test
    void nieznanaKartaMaPoziomZero() {
        assertEquals(0, AccessCardMath.level("fioletowa"));
        assertEquals(0, AccessCardMath.level(null));
        assertEquals(0, AccessCardMath.level(""));
    }

    @Test
    void poziomIgnorujeWielkoscLiterIOdstępy() {
        assertEquals(1, AccessCardMath.level("  NIEBIESKA  "));
        assertEquals(3, AccessCardMath.level("Zielona"));
    }

    @Test
    void znaneKartySaRozpoznawane() {
        assertTrue(AccessCardMath.known(AccessCardMath.BLUE));
        assertTrue(AccessCardMath.known(AccessCardMath.RED));
        assertTrue(AccessCardMath.known(AccessCardMath.GREEN));
        assertFalse(AccessCardMath.known("brak"));
        assertFalse(AccessCardMath.known(null));
    }

    // ------------------------------------------------------------------
    //  Otwieranie skrzyń
    // ------------------------------------------------------------------

    @Test
    void skrzyniaBezWymoguNiePotrzebujeKarty() {
        assertTrue(AccessCardMath.opens(0, 0));
        assertTrue(AccessCardMath.opens(1, 0));
        assertTrue(AccessCardMath.opens(3, 0));
    }

    @Test
    void kartaMusiBycCoNajmniejTakMocnaJakZamek() {
        assertTrue(AccessCardMath.opens(1, 1));
        assertFalse(AccessCardMath.opens(1, 2));
        assertFalse(AccessCardMath.opens(1, 3));

        assertTrue(AccessCardMath.opens(2, 1));
        assertTrue(AccessCardMath.opens(2, 2));
        assertFalse(AccessCardMath.opens(2, 3));

        assertTrue(AccessCardMath.opens(3, 1));
        assertTrue(AccessCardMath.opens(3, 2));
        assertTrue(AccessCardMath.opens(3, 3));
    }

    @Test
    void zielonaOtwieraSkrzynieElitarne() {
        assertTrue(AccessCardMath.opens(AccessCardMath.GREEN, 3));
    }

    @Test
    void czerwonaNieOtwieraSkrzyniElitarnej() {
        assertFalse(AccessCardMath.opens(AccessCardMath.RED, 3));
    }

    @Test
    void niebieskaNieOtwieraSkrzyniWojskowej() {
        assertFalse(AccessCardMath.opens(AccessCardMath.BLUE, 2));
    }

    // ------------------------------------------------------------------
    //  Łączenie
    // ------------------------------------------------------------------

    @Test
    void niebieskaICzerwonaDajaZielona() {
        assertEquals(AccessCardMath.GREEN,
                AccessCardMath.combine(AccessCardMath.BLUE, AccessCardMath.RED));
    }

    @Test
    void kolejnoscNieMaZnaczenia() {
        assertEquals(AccessCardMath.GREEN,
                AccessCardMath.combine(AccessCardMath.RED, AccessCardMath.BLUE));
    }

    @Test
    void dwieNiebieskieNieDajaZielonej() {
        assertNull(AccessCardMath.combine(AccessCardMath.BLUE, AccessCardMath.BLUE));
        assertFalse(AccessCardMath.canCombine(AccessCardMath.BLUE, AccessCardMath.BLUE));
    }

    @Test
    void dwieCzerwoneNieDajaZielonej() {
        assertNull(AccessCardMath.combine(AccessCardMath.RED, AccessCardMath.RED));
        assertFalse(AccessCardMath.canCombine(AccessCardMath.RED, AccessCardMath.RED));
    }

    @Test
    void zielonaNieLaczySieZNiczym() {
        assertNull(AccessCardMath.combine(AccessCardMath.GREEN, AccessCardMath.BLUE));
        assertNull(AccessCardMath.combine(AccessCardMath.GREEN, AccessCardMath.RED));
        assertNull(AccessCardMath.combine(AccessCardMath.GREEN, AccessCardMath.GREEN));
    }

    @Test
    void pusteWartosciNieLaczaSie() {
        assertNull(AccessCardMath.combine(null, AccessCardMath.BLUE));
        assertNull(AccessCardMath.combine(AccessCardMath.RED, null));
        assertNull(AccessCardMath.combine(null, null));
        assertNull(AccessCardMath.combine("", ""));
    }

    @Test
    void laczenieIgnorujeWielkoscLiter() {
        assertEquals(AccessCardMath.GREEN,
                AccessCardMath.combine(" NIEBIESKA ", "czerwona"));
    }

    // ------------------------------------------------------------------
    //  Prezentacja
    // ------------------------------------------------------------------

    @Test
    void nazwySaKolorowe() {
        assertTrue(AccessCardMath.displayName(AccessCardMath.BLUE).startsWith("&9"));
        assertTrue(AccessCardMath.displayName(AccessCardMath.RED).startsWith("&c"));
        assertTrue(AccessCardMath.displayName(AccessCardMath.GREEN).startsWith("&a"));
    }

    @Test
    void nieznanaKartaMaNeutralnaNazwe() {
        assertEquals("&7Karta dostępu", AccessCardMath.displayName("fioletowa"));
        assertEquals("&7", AccessCardMath.color("fioletowa"));
    }

    @Test
    void liniaPoziomuPokazujeCyfre() {
        assertEquals("&7Poziom dostępu: &f1", AccessCardMath.levelLine(AccessCardMath.BLUE));
        assertEquals("&7Poziom dostępu: &f2", AccessCardMath.levelLine(AccessCardMath.RED));
        assertEquals("&7Poziom dostępu: &f3", AccessCardMath.levelLine(AccessCardMath.GREEN));
        assertEquals("&7Poziom dostępu: ?", AccessCardMath.levelLine("fioletowa"));
    }
}
