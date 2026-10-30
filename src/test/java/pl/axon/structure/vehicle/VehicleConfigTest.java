package pl.axon.structure.vehicle;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleConfigTest {

    private static VehicleConfig load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return VehicleConfig.load(config.getConfigurationSection("wraki"));
    }

    @Test
    void brakSekcjiDajeDomyslne() {
        VehicleConfig config = VehicleConfig.load(null);
        assertTrue(config.enabled());
        assertEquals(35.0, config.chancePercent(), 1e-9);
        assertEquals(24, config.minSpacing());
        assertTrue(config.barriers());
        assertTrue(config.loot());
        assertEquals("wrak", config.lootPool());
        assertTrue(config.weights().containsKey(VehicleConfig.NONE));
        assertTrue(config.errors().isEmpty());
    }

    @Test
    void domyslneWagiZnajaWszystkieWraki() {
        VehicleConfig config = VehicleConfig.defaults();
        for (VehicleModel model : VehicleModel.ALL) {
            assertTrue(config.weights().containsKey(model.id()), "brak wagi dla " + model.id());
        }
    }

    @Test
    void wczytujeWszystkieKlucze() throws InvalidConfigurationException {
        VehicleConfig config = load("""
                wraki:
                  wlaczone: false
                  swiaty: [world, world_nether]
                  szansa-procent: 12.5
                  min-odstep: 40
                  bariery: false
                  loot: false
                  pula-lootu: wrak_wojskowy
                  modele:
                    wrak_fiat_126p: 5
                    brak: 1
                """);
        assertFalse(config.enabled());
        assertEquals(2, config.worlds().size());
        assertEquals(12.5, config.chancePercent(), 1e-9);
        assertEquals(40, config.minSpacing());
        assertFalse(config.barriers());
        assertFalse(config.loot());
        assertEquals("wrak_wojskowy", config.lootPool());
        assertEquals(5, config.weights().get("wrak_fiat_126p"));
        assertEquals(1, config.weights().get(VehicleConfig.NONE));
        assertTrue(config.errors().isEmpty());
    }

    @Test
    void przycinaSzanseDoZakresu() throws InvalidConfigurationException {
        VehicleConfig tooHigh = load("wraki:\n  szansa-procent: 500\n");
        assertEquals(100.0, tooHigh.chancePercent(), 1e-9);
        assertFalse(tooHigh.errors().isEmpty());

        VehicleConfig negative = load("wraki:\n  szansa-procent: -10\n");
        assertEquals(0.0, negative.chancePercent(), 1e-9);
    }

    @Test
    void przycinaOdstepDoZakresu() throws InvalidConfigurationException {
        VehicleConfig tooSmall = load("wraki:\n  min-odstep: 1\n");
        assertEquals(4, tooSmall.minSpacing());
        assertFalse(tooSmall.errors().isEmpty());

        VehicleConfig tooBig = load("wraki:\n  min-odstep: 99999\n");
        assertEquals(512, tooBig.minSpacing());
    }

    @Test
    void nieznanyModelTrafiaDoBledow() throws InvalidConfigurationException {
        VehicleConfig config = load("""
                wraki:
                  modele:
                    wrak_fiat_126p: 5
                    wrak_ciezarowka: 3
                """);
        assertEquals(1, config.weights().size());
        assertTrue(config.weights().containsKey("wrak_fiat_126p"));
        assertFalse(config.errors().isEmpty());
        assertTrue(config.errors().get(0).contains("wrak_ciezarowka"));
    }

    @Test
    void wagaZeroLubUjemnaJestOdrzucana() throws InvalidConfigurationException {
        VehicleConfig config = load("""
                wraki:
                  modele:
                    wrak_fiat_126p: 0
                    wrak_polonez: -5
                    wrak_zuk: 3
                """);
        assertEquals(1, config.weights().size());
        assertTrue(config.weights().containsKey("wrak_zuk"));
        assertEquals(2, config.errors().size());
    }

    @Test
    void pustaListaModeliWracaDoDomyslnych() throws InvalidConfigurationException {
        VehicleConfig config = load("wraki:\n  modele: {}\n");
        assertTrue(config.weights().containsKey("wrak_fiat_126p"));
        assertTrue(config.weights().containsKey(VehicleConfig.NONE));
        assertFalse(config.errors().isEmpty());
    }

    @Test
    void brakListyModeliDajeWszystkieWraki() throws InvalidConfigurationException {
        VehicleConfig config = load("wraki:\n  wlaczone: true\n");
        for (VehicleModel model : VehicleModel.ALL) {
            assertTrue(config.weights().containsKey(model.id()));
        }
    }

    @Test
    void pusteSwiatyDajaDomyslnySwiat() throws InvalidConfigurationException {
        VehicleConfig config = load("wraki:\n  swiaty: []\n");
        assertEquals(1, config.worlds().size());
        assertEquals("world", config.worlds().get(0));
    }

    @Test
    void inWorldSprawdzaListeIWlaczenie() throws InvalidConfigurationException {
        VehicleConfig config = load("wraki:\n  swiaty: [world]\n");
        assertTrue(config.inWorld("world"));
        assertFalse(config.inWorld("world_nether"));
        assertFalse(config.inWorld(null));

        VehicleConfig off = load("wraki:\n  wlaczone: false\n  swiaty: [world]\n");
        assertFalse(off.inWorld("world"));
    }

    @Test
    void rollZwracaTylkoModeleZListy() throws InvalidConfigurationException {
        VehicleConfig config = load("""
                wraki:
                  modele:
                    wrak_fiat_126p: 10
                """);
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            VehicleModel model = config.roll(random);
            assertNotNull(model);
            assertEquals("wrak_fiat_126p", model.id());
        }
    }

    @Test
    void rollZwracaNullDlaBraku() throws InvalidConfigurationException {
        VehicleConfig config = load("""
                wraki:
                  modele:
                    brak: 10
                """);
        Random random = new Random(7);
        for (int i = 0; i < 100; i++) {
            assertNull(config.roll(random));
        }
    }

    @Test
    void rollRozkladaWagiProporcjonalnie() throws InvalidConfigurationException {
        VehicleConfig config = load("""
                wraki:
                  modele:
                    wrak_fiat_126p: 75
                    brak: 25
                """);
        Random random = new Random(1234);
        int fiat = 0;
        int none = 0;
        for (int i = 0; i < 20_000; i++) {
            if (config.roll(random) == null) {
                none++;
            } else {
                fiat++;
            }
        }
        double ratio = fiat / (double) (fiat + none);
        assertEquals(0.75, ratio, 0.02);
    }

    @Test
    void rollNieWywalaSieNaPustychWagach() {
        VehicleConfig config = VehicleConfig.load(null);
        assertNotNull(config.roll(new Random(1)));
        assertNull(config.roll(null));
    }
}
