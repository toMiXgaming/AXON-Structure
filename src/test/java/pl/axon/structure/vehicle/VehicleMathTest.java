package pl.axon.structure.vehicle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleMathTest {

    private static final VehicleModel FIAT = VehicleModel.byId("wrak_fiat_126p");
    private static final VehicleModel AUTOSAN = VehicleModel.byId("wrak_autosan");
    private static final VehicleModel STAR = VehicleModel.byId("wrak_star_266");

    // ------------------------------------------------------------------
    //  Katalog modeli
    // ------------------------------------------------------------------

    @Test
    void katalogMaWszystkieWrakiZPaczki() {
        assertEquals(8, VehicleModel.ALL.size());
        for (String id : List.of("wrak_fiat_126p", "wrak_polonez", "wrak_zuk", "wrak_nysa",
                "wrak_star_266", "wrak_autosan", "wrak_ursus", "wrak_wsk")) {
            assertNotNull(VehicleModel.byId(id), "brak modelu " + id);
        }
    }

    @Test
    void byIdIgnorujeWielkoscLiterIOdstępy() {
        assertNotNull(VehicleModel.byId("  WRAK_FIAT_126P "));
        assertNull(VehicleModel.byId("wrak_ciezarowka"));
        assertNull(VehicleModel.byId(null));
    }

    @Test
    void wymiaryWrakowSaDodatnie() {
        for (VehicleModel model : VehicleModel.ALL) {
            assertTrue(model.length() > 0, model.id());
            assertTrue(model.width() > 0, model.id());
            assertTrue(model.height() > 0, model.id());
            assertTrue(model.scale() > 0, model.id());
            assertFalse(model.lootTable().isBlank(), model.id());
        }
    }

    @Test
    void skalaZgadzaSieZVehiclesPy() {
        // vehicles.py: f = min(1.6, 46 / L[dm]), scale = round(1.6 / f, 3)
        for (VehicleModel model : VehicleModel.ALL) {
            double lengthDm = model.length() * 10.0;
            double f = Math.min(1.6, 46.0 / lengthDm);
            double expected = Math.round(1.6 / f * 1000.0) / 1000.0;
            assertEquals(expected, model.scale(), 1e-9, model.id());
        }
        // Autosan ma 10 m, nie 3 (bez skali był wielkości Fiata).
        assertTrue(AUTOSAN.scale() > 3.0);
    }

    @Test
    void konstruktorOdrzucaZleWymiary() {
        assertThrows(IllegalArgumentException.class,
                () -> new VehicleModel("", "x", 1, 1, 1, 1, "wrak_auto"));
        assertThrows(IllegalArgumentException.class,
                () -> new VehicleModel("wrak_x", "x", 0, 1, 1, 1, "wrak_auto"));
        assertThrows(IllegalArgumentException.class,
                () -> new VehicleModel("wrak_x", "x", 1, 1, 0, 1, "wrak_auto"));
        assertThrows(IllegalArgumentException.class,
                () -> new VehicleModel("wrak_x", "x", 1, 1, 1, 0, "wrak_auto"));
    }

    @Test
    void konstruktorDomykaTabele() {
        assertEquals("wrak_auto", new VehicleModel("wrak_x", "x", 5, 2, 1, 1, "").lootTable());
        assertEquals("wrak_wojskowy", new VehicleModel("wrak_y", "y", 5, 2, 1, 1, "wrak_wojskowy").lootTable());
    }

    @Test
    void wysokoscSrodkaToPolowaSkali() {
        assertEquals(FIAT.scale() / 2.0, FIAT.centerHeight(), 1e-9);
        assertEquals(AUTOSAN.scale() / 2.0, AUTOSAN.centerHeight(), 1e-9);
    }

    // ------------------------------------------------------------------
    //  Obrys i obrót
    // ------------------------------------------------------------------

    @Test
    void obrysPrzyObrocieZeroMaDlugoscPoZ() {
        int[] size = VehicleMath.footprint(FIAT, 0);
        assertEquals(1, size[0]); // 1.4 m szerokości → 1 blok
        assertEquals(3, size[1]); // 3.1 m długości → 3 bloki
    }

    @Test
    void obrysPrzyObrocie90ZamieniaOsie() {
        int[] zero = VehicleMath.footprint(AUTOSAN, 0);
        int[] ninety = VehicleMath.footprint(AUTOSAN, 1);
        assertEquals(zero[0], ninety[1]);
        assertEquals(zero[1], ninety[0]);
    }

    @Test
    void obrotUjemnyTraktowanyJakModulo() {
        assertEquals(VehicleMath.footprint(AUTOSAN, 1)[0], VehicleMath.footprint(AUTOSAN, -3)[0]);
        assertEquals(VehicleMath.footprint(AUTOSAN, 0)[0], VehicleMath.footprint(AUTOSAN, 4)[0]);
    }

    @Test
    void obrysNieWystajePozaModelWiecejNizPolBloku() {
        for (VehicleModel model : VehicleModel.ALL) {
            int[] size = VehicleMath.footprint(model, 0);
            assertTrue(size[0] - model.width() <= 1.0 + 1e-9, model.id());
            assertTrue(size[1] - model.length() <= 1.0 + 1e-9, model.id());
            assertTrue(size[1] <= Math.ceil(model.length()) + 1, model.id());
        }
    }

    @Test
    void rotateOffsetObracaWzdluzOsi() {
        // 0°: długość po Z
        double[] zero = VehicleMath.rotateOffset(2.0, 0.0, 0);
        assertEquals(0.0, zero[0], 1e-9);
        assertEquals(2.0, zero[1], 1e-9);
        // 90°: długość po X
        double[] ninety = VehicleMath.rotateOffset(2.0, 0.0, 1);
        assertEquals(2.0, ninety[0], 1e-9);
        assertEquals(0.0, ninety[1], 1e-9);
        // 180°: długość po -Z
        double[] oneEighty = VehicleMath.rotateOffset(2.0, 0.0, 2);
        assertEquals(0.0, oneEighty[0], 1e-9);
        assertEquals(-2.0, oneEighty[1], 1e-9);
        // 270°: długość po -X
        double[] twoSeventy = VehicleMath.rotateOffset(2.0, 0.0, 3);
        assertEquals(-2.0, twoSeventy[0], 1e-9);
        assertEquals(0.0, twoSeventy[1], 1e-9);
    }

    // ------------------------------------------------------------------
    //  Bariery
    // ------------------------------------------------------------------

    @Test
    void niskiWrakMaJednaWarstweBarier() {
        // Fiat ma 1.3 m wysokości: jedna warstwa, da się na niego wskoczyć.
        for (int[] pos : VehicleMath.collisionBlocks(FIAT, 0, 64, 0, 0)) {
            assertEquals(65, pos[1]);
        }
        assertEquals(3, VehicleMath.collisionBlocks(FIAT, 0, 64, 0, 0).size());
    }

    @Test
    void wysokiWrakMaBarieryDoDachu() {
        assertEquals(3, VehicleMath.collisionLayers(AUTOSAN));
        assertEquals(3, VehicleMath.collisionLayers(STAR));
        int maxY = 0;
        for (int[] pos : VehicleMath.collisionBlocks(AUTOSAN, 0, 64, 0, 0)) {
            maxY = Math.max(maxY, pos[1]);
        }
        assertEquals(67, maxY);
    }

    @Test
    void barieryNieWychodzaPozaObrys() {
        int[] size = VehicleMath.footprint(FIAT, 0);
        int halfX = size[0] / 2;
        int halfZ = size[1] / 2;
        for (int[] pos : VehicleMath.collisionBlocks(FIAT, 10, 64, 20, 0)) {
            assertTrue(Math.abs(pos[0] - 10) <= halfX);
            assertTrue(Math.abs(pos[2] - 20) <= halfZ);
        }
    }

    @Test
    void barierySaSymetryczneWzgledemZaczepienia() {
        List<int[]> blocks = VehicleMath.collisionBlocks(FIAT, 0, 64, 0, 0);
        int sumX = 0;
        int sumZ = 0;
        for (int[] pos : blocks) {
            sumX += pos[0];
            sumZ += pos[2];
        }
        assertEquals(0, sumX);
        assertEquals(0, sumZ);
    }

    @Test
    void stareBarieryObejmujaNowe() {
        // Sprzątanie starych barier nie może ruszyć nowych: nowe są podzbiorem starego obrysu (Fiat).
        List<int[]> legacy = VehicleMath.legacyCollisionBlocks(FIAT, 0, 64, 0, 0);
        assertEquals(3 * 5 * 2, legacy.size());
        for (int[] pos : VehicleMath.collisionBlocks(FIAT, 0, 64, 0, 0)) {
            assertTrue(legacy.stream().anyMatch(p -> p[0] == pos[0] && p[1] == pos[1] && p[2] == pos[2]));
        }
    }

    // ------------------------------------------------------------------
    //  Hitboxy bagażnika
    // ------------------------------------------------------------------

    @Test
    void hitboxyPokrywajaCalyWrak() {
        for (VehicleModel model : VehicleModel.ALL) {
            List<double[]> boxes = VehicleMath.hitboxes(model);
            assertFalse(boxes.isEmpty(), model.id());
            int[] size = VehicleMath.footprint(model, 0);
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (double[] box : boxes) {
                min = Math.min(min, box[0] - box[1] / 2.0);
                max = Math.max(max, box[0] + box[1] / 2.0);
                // szerokość i wysokość obejmują model i bariery
                assertTrue(box[1] > model.width() && box[1] > size[0], model.id());
                assertTrue(box[2] > model.height() && box[2] > VehicleMath.collisionLayers(model), model.id());
            }
            double needed = Math.max(model.length(), size[1]) / 2.0;
            assertTrue(-min > needed && max > needed, model.id());
        }
    }

    @Test
    void hitboxySaSymetryczne() {
        double sum = 0;
        for (double[] box : VehicleMath.hitboxes(AUTOSAN)) {
            sum += box[0];
        }
        assertEquals(0.0, sum, 1e-9);
        assertTrue(VehicleMath.hitboxes(AUTOSAN).size() >= 3);
    }

    // ------------------------------------------------------------------
    //  Dopasowanie do terenu i promień
    // ------------------------------------------------------------------

    @Test
    void fitsAkceptujeWrakWDuzejPrzestrzeni() {
        assertTrue(VehicleMath.fits(FIAT, 0, 0, 0, -10, 10, -10, 10));
    }

    @Test
    void fitsOdrzucaWrakWychodzacyZaGranice() {
        assertFalse(VehicleMath.fits(AUTOSAN, 0, 0, 0, -2, 2, -2, 2));
    }

    @Test
    void fitsUwzgledniaObrot() {
        // Autosan 10×2.5: przy 0° potrzebuje 10 po Z, przy 90° 10 po X.
        assertFalse(VehicleMath.fits(AUTOSAN, 0, 0, 0, -5, 5, -2, 2));
        assertTrue(VehicleMath.fits(AUTOSAN, 0, 0, 1, -5, 5, -2, 2));
    }

    @Test
    void promienJestWiekszyOdPolowyDlugosci() {
        assertTrue(VehicleMath.radius(AUTOSAN) > AUTOSAN.length() / 2.0);
        assertTrue(VehicleMath.radius(FIAT) > 0);
    }
}
