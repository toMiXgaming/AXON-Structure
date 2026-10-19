package pl.axon.structure.core;

import org.junit.jupiter.api.Test;
import pl.axon.structure.api.Entrance;
import pl.axon.structure.api.SizeClass;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureFileTest {

    private static final String BODY = """
            paleta:
            0 minecraft:air
            1 minecraft:stone
            warstwy:
            0 0: 1*6
            """;

    private static String file(String headerExtra) {
        return "# AXON-Structure v1\n"
                + "id: test\n"
                + "nazwa: Test\n"
                + "opis: \n"
                + "rozmiar: 6 2 6\n"
                + headerExtra
                + BODY;
    }

    private static Blueprint read(String text) throws IOException {
        return StructureFile.read(new StringReader(text));
    }

    @Test
    void staryNaglowekDostajeWartosciDomyslne() throws IOException {
        Blueprint bp = read(file(""));
        assertEquals(Traits.defaults(), bp.traits());
        assertNull(bp.traits().sizeClass());
        assertTrue(bp.traits().mapAllowed());
    }

    @Test
    void nowePolaNaglowkaSaCzytane() throws IOException {
        Blueprint bp = read(file("""
                grunt: 21
                wjazdy: 0 2 west; 5 0 north
                rozmiar-klasa: duza
                mapa: nie
                strefa: rownina,las
                klimat: snieg
                bezpieczna: tak
                autor: Tomek
                """));
        Traits t = bp.traits();
        assertEquals(21, t.groundLevel());
        assertEquals(List.of(new Entrance(0, 2, "west"), new Entrance(5, 0, "north")), t.entrances());
        assertEquals(SizeClass.DUZA, t.sizeClass());
        assertFalse(t.mapAllowed());
        assertEquals(List.of("rownina", "las"), List.copyOf(t.zones()));
        assertEquals(List.of("snieg"), List.copyOf(t.climates()));
        assertTrue(t.safe());
        assertEquals(Map.of("autor", "Tomek"), t.tags());
    }

    @Test
    void staraStrefaBezpiecznaToSrodekIBezpieczna() throws IOException {
        Traits t = read(file("strefa: bezpieczna\n")).traits();
        assertEquals(List.of("srodek"), List.copyOf(t.zones()));
        assertTrue(t.safe());
    }

    @Test
    void blednaWartoscRzucaWyjatekZNazwaPola() {
        IOException mapa = assertThrows(IOException.class, () -> read(file("mapa: moze\n")));
        assertTrue(mapa.getMessage().contains("mapa"));
        assertThrows(IOException.class, () -> read(file("strefa: pustynia\n")));
        assertThrows(IOException.class, () -> read(file("klimat: tropik\n")));
        assertThrows(IOException.class, () -> read(file("rozmiar-klasa: ogromna\n")));
        assertThrows(IOException.class, () -> read(file("wjazdy: 9 0 north\n")), "poza szablonem");
        assertThrows(IOException.class, () -> read(file("wjazdy: 1 1 up\n")), "zły kierunek");
        assertThrows(IOException.class, () -> read(file("grunt: -3\n")));
    }

    @Test
    void zapisIOdczytDajaTeSameNaglowkiIBloki() throws IOException {
        Blueprint bp = read(file("""
                grunt: 21
                wjazdy: 0 2 west
                mapa: nie
                bezpieczna: tak
                autor: Tomek
                """));
        StringWriter out = new StringWriter();
        StructureFile.write(bp, out);
        String written = out.toString();
        assertTrue(written.contains("grunt: 21"));
        assertTrue(written.contains("mapa: nie"));
        assertTrue(written.contains("autor: Tomek"));
        assertFalse(written.contains("klimat:"), "domyślny klimat nie powinien trafić do pliku");

        Blueprint again = read(written);
        assertEquals(bp.traits(), again.traits());
        assertEquals(bp.get(0, 0, 0), again.get(0, 0, 0));
        assertEquals("minecraft:stone", again.get(3, 0, 0));
    }
}
