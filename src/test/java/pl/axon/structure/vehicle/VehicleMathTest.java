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
            assertTrue(model.scale() > 0, model.id());
            assertTrue(model.parts() >= 1 && model.parts() <= VehicleModel.MAX_PARTS, model.id());
            assertFalse(model.lootTable().isBlank(), model.id());
        }
    }

    @Test
    void konstruktorOdrzucaZleWymiary() {
        assertThrows(IllegalArgumentException.class,
                () -> new VehicleModel("", "x", 1, 1, 1, 1, "wrak_auto"));
        assertThrows(IllegalArgumentException.class,
                () -> new VehicleModel("wrak_x", "x", 0, 1, 1, 1, "wrak_auto"));
        assertThrows(IllegalArgumentException.class,
                () -> new VehicleModel("wrak_x", "x", 1, 1, 0, 1, "wrak_auto"));
    }

    @Test
    void konstruktorPrzycinaLiczbeCzesciIDomykaTabele() {
        VehicleModel model = new VehicleModel("wrak_x", "x", 5, 2, 1, 99, "");
        assertEquals(VehicleModel.MAX_PARTS, model.parts());
        assertEquals("wrak_auto", model.lootTable());

        VehicleModel zero = new VehicleModel("wrak_y", "y", 5, 2, 1, 0, "wrak_wojskowy");
        assertEquals(1, zero.parts());
        assertEquals("wrak_wojskowy", zero.lootTable());
    }

    @Test
    void wysokoscSrodkaToPolowaSkali() {
        assertEquals(0.5, FIAT.centerHeight(), 1e-9);
    }

    @Test
    void duzeWrakiSaDzieloneNaCzesci() {
        assertFalse(FIAT.large());
        assertTrue(AUTOSAN.large());
        assertTrue(STAR.large());
    }

    // ------------------------------------------------------------------
    //  Obrys i obrót
    // ------------------------------------------------------------------

    @Test
    void obrysPrzyObrocieZeroMaDlugoscPoZ() {
        int[] size = VehicleMath.footprint(FIAT, 0);
        assertEquals((int) Math.ceil(FIAT.width()), size[0]);
        assertEquals((int) Math.ceil(FIAT.length()), size[1]);
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
    void barierySaWDwochWarstwachNadGruntem() {
        List<int[]> blocks = VehicleMath.collisionBlocks(FIAT, 0, 64, 0, 0);
        assertFalse(blocks.isEmpty());
        for (int[] pos : blocks) {
            assertTrue(pos[1] == 65 || pos[1] == 66, "bariera na y=" + pos[1]);
        }
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

    // ------------------------------------------------------------------
    //  Podział na części
    // ------------------------------------------------------------------

    @Test
    void malyWrakMaJednaCzescWSrodku() {
        List<double[]> offsets = VehicleMath.partOffsets(FIAT);
        assertEquals(1, offsets.size());
        assertEquals(0.0, offsets.get(0)[0], 1e-9);
        assertEquals(0.0, offsets.get(0)[1], 1e-9);
    }

    @Test
    void duzyWrakMaTyleCzesciIleParts() {
        assertEquals(AUTOSAN.parts(), VehicleMath.partOffsets(AUTOSAN).size());
        assertEquals(STAR.parts(), VehicleMath.partOffsets(STAR).size());
    }

    @Test
    void czesciSaRozlozoneRownomiernieWzdluzDlugosci() {
        List<double[]> offsets = VehicleMath.partOffsets(AUTOSAN);
        double span = AUTOSAN.length();
        double step = span / offsets.size();
        for (int i = 0; i < offsets.size(); i++) {
            double expected = -span / 2.0 + step * (i + 0.5);
            assertEquals(expected, offsets.get(i)[0], 1e-9);
        }
    }

    @Test
    void czesciSaSymetryczneWzgledemSrodka() {
        List<double[]> offsets = VehicleMath.partOffsets(AUTOSAN);
        double sum = 0;
        for (double[] offset : offsets) {
            sum += offset[0];
        }
        assertEquals(0.0, sum, 1e-9);
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
